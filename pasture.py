import sys
import io
import time

import requests # pip install requests
import boto3 # pip install boto3[crt]
from fabric import Connection # pip install fabric
from invoke import Responder
# from ilogue.fexpect import expect, expecting, run  # pip install fexpect

# USAGE: python3 pasture.py <mode> <number of nodes> [-s]

# TODO: automatic teardown
# TODO: set up aws configs and permissions

cache_port = 7070

system = "aws"

# aws_configure_prompts = {
#     'AWS Access Key ID [None]:': '',
#     'AWS Secret Access Key [None]:': '',
#     'Default region name [None]:': '',
#     'Default output format [None]:': ''
# }

def get_vpc_and_subnet(ec2, zone):
    default_vpc = None
    for vpc in ec2.vpcs.all():
        if vpc.is_default:
            default_vpc = vpc

    if default_vpc is None:
        return None, None

    for subnet in default_vpc.subnets.all():
        if subnet.availability_zone == zone:
            return default_vpc.id, subnet.id

    return default_vpc.id, None

def connect_retry(host, user, key):
    while True:
        try:
            c = Connection(
                host=host,
                user=user,
                connect_kwargs={
                    "key_filename": key,
                    "timeout": 5,
                    "banner_timeout": 5,
                    "auth_timeout": 5
                },
            )
            c.open()
            return c
        except Exception as e:
            print(f"Exception while connecting to host {host}: {e}")
            time.sleep(5)


def test_node(node):
    success = True
    try:
        print("Retry")
        requests.get(f"http://{node}:{cache_port}", timeout=5)
    except:
        success = False
    return success

def wait_node(node):
    print(f"Waiting for {node}")
    while not test_node(node):
        time.sleep(5)

def launch_cluster(num_nodes, scaleable_param):
    ec2 = boto3.resource('ec2')

    vpc_id, subnet_id = get_vpc_and_subnet(ec2, 'us-east-1b')

    security_group = ec2.create_security_group(
        GroupName='cachecow-security',
        Description='Security group for CacheCow',
        VpcId=vpc_id,
    )

    security_group.authorize_ingress(
        IpPermissions=[
                {
                    'IpProtocol': 'tcp',
                    'FromPort': cache_port,
                    'ToPort': cache_port,
                    'IpRanges': [
                        {
                            'CidrIp': '0.0.0.0/0'
                        }
                    ]
                },
                {
                    'IpProtocol': 'tcp',
                    'FromPort': 3000,
                    'ToPort': 3000,
                    'IpRanges': [
                        {
                            'CidrIp': '0.0.0.0/0'
                        }
                    ]
                },
                {
                    'IpProtocol': 'tcp',
                    'FromPort': 22,
                    'ToPort': 22,
                    'IpRanges': [
                        {
                            'CidrIp': '0.0.0.0/0'
                        }
                    ]
                }
            ]
    )

    instances = ec2.create_instances(
        # ImageId='ami-079466db464206b00', # Custom AMI with dependencies preinstalled
        ImageId='ami-09d3b3274b6c5d4aa', # Amazon Linux 2 Kernel 5.10 AMI 2.0.20221004.0 x86_64 HVM gp2
        InstanceType='t3.medium', # 2 vCPU, 4 GiB memory
        KeyName='CacheCow', # keypair for authentication
        MaxCount=num_nodes,
        MinCount=num_nodes,
        Placement={
            'AvailabilityZone': 'us-east-1b'
        },
        SecurityGroups=[
            'cachecow-security', # security group for firewall
        ],
        TagSpecifications=[
            {
                'ResourceType': 'instance',
                'Tags': [
                    {
                        'Key': 'Name',
                        'Value': 'CacheCow Node' # instance name
                    },
                ]
            },
        ],
        BlockDeviceMappings=[
            {
                'DeviceName': '/dev/xvda',
                'Ebs': {
                    'VolumeSize': 16,
                }
            },
        ]
    )

    node_dns = []
    for instance in instances:
        instance.wait_until_running()
        instance.load()
        node_dns.append(instance.public_dns_name)
        print(f"Node {instance.public_dns_name} finished loading")

    node_dns_f = io.StringIO("\n".join(x + f":{cache_port}" for x in node_dns))

    for i, node in enumerate(node_dns):
        c = connect_retry(node, "ec2-user", "CacheCow.pem")

        print("ACTION: Cloning git repo")
        c.run("sudo yum update -y")
        # c.run("sudo yum install git -y")
        # c.run("sudo amazon-linux-extras install java-openjdk11 -y")
        c.run("sudo yum install git java-11-amazon-corretto-headless tmux -y")

        c.run("git clone https://github.com/RiceComp413-Fall2022/CacheCow")
        # c.run("cd CacheCow/cache-node/ && git switch ec2-support")
        c.run("cd CacheCow/cache-node/ && git switch Autoscale-POC")

        c.put(node_dns_f, remote='CacheCow/cache-node/nodes.txt')

        # TODO: Verify new stuff as working
        print("ACTION: Setting Up AWS")
        c.put("CacheCow.pem", remote="CacheCow/CacheCow.pem")
        c.put("rootkey.csv", remote="CacheCow/rootkey.csv")

        access_id = c.run("awk -F: '{$1 = substr($1, index($1, \"=\") + 1, 100)} NR==1{print $1}' CacheCow/rootkey.csv").stdout.strip()
        access_secret = c.run("awk -F: '{$1 = substr($1, index($1, \"=\") + 1, 100)} NR==2{print $1}' CacheCow/rootkey.csv").stdout.strip()
        print(f"Captured key id {access_id} and access key {access_secret}")

        aws_watchers = [
            Responder(pattern=r'[.]*ID[.]*', response=access_id + "\n"),
            Responder(pattern=r'[.]*Secret[.]*', response=access_secret + "\n"),
            Responder(pattern=r'[.]*region[.]*', response="us-east-1b\n"),
            Responder(pattern=r'[.]*output[.]*', response="\n")
        ]

        # aws_configure_prompts['AWS Access Key ID [None]:'] = c.run("awk -F: '{$1 = substr($1, index($1, \"=\") + 1, 100)} NR==1{print $1}' rootkey.csv").stdout
        # aws_configure_prompts['AWS Secret Access Key [None]:'] = c.run("awk -F: '{$1 = substr($1, index($1, \"=\") + 1, 100)} NR==2{print $1}' rootkey.csv").stdout
        # print(f"Captured key id {aws_configure_prompts['AWS Access Key ID [None]:']} and access key {aws_configure_prompts['AWS Secret Access Key [None]:']}")

        c.run("curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip")
        c.run("unzip awscliv2.zip")
        c.run("sudo ./aws/install")

        print("ACTION: Configuring AWS")
        c.run("aws configure", pty=True, watchers=aws_watchers)
        # with settings(prompts=aws_configure_prompts):
        #     c.run("aws configure")
        c.run("pip3 install requests boto3 fabric")

        print("ACTION: Starting services")
        c.run(f"tmux new-session -d \"cd CacheCow/cache-node/ && ./gradlew run --args '{system} {i} {cache_port} {scaleable_param}'\"", asynchronous=True)

        c.run("curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.34.0/install.sh | bash")
        c.run(". ~/.nvm/nvm.sh")
        c.run("nvm install 16")

        c.put(node_dns_f, remote='CacheCow/monitor-node/src/nodes.txt') # TODO: use the same file

        c.run("tmux new-session -d \"cd CacheCow/monitor-node/ && . ~/.nvm/nvm.sh && npm install && npm start\"", asynchronous=True)

        c.close()

    elb = boto3.client('elbv2')

    target_group = elb.create_target_group(
        Name='cachecow-nodes',
        Protocol='TCP',
        Port=cache_port,
        TargetType='instance',
        VpcId=vpc_id
    )

    target_group_arn = target_group['TargetGroups'][0]['TargetGroupArn']

    targets = elb.register_targets(
        TargetGroupArn=target_group_arn,
        Targets=[{'Id': x.id, 'Port': cache_port} for x in instances]
    )

    balancer = elb.create_load_balancer(
        Name='cachecow-balancer',
        Subnets=[
            subnet_id
        ],
        Scheme='internet-facing',
        Type='network',
        IpAddressType='ipv4'
    )

    balancer_arn = balancer['LoadBalancers'][0]['LoadBalancerArn']

    listener = elb.create_listener(
        LoadBalancerArn=balancer_arn,
        Protocol='TCP',
        Port=cache_port,
        DefaultActions=[
            {
                'Type': 'forward',
                'TargetGroupArn': target_group_arn,
                'ForwardConfig': {
                    'TargetGroups': [
                        {
                            'TargetGroupArn': target_group_arn
                        },
                    ]
                }
            },
        ],
    )

    elb.get_waiter('load_balancer_available').wait(LoadBalancerArns=[balancer_arn])

    elb_dns = elb.describe_load_balancers(LoadBalancerArns=[balancer_arn])['LoadBalancers'][0]['DNSName']

    for node in node_dns:
        wait_node(node)

    wait_node(elb_dns)

def scale_cluster(num_nodes):
    print("HERE 1")
    ec2 = boto3.resource('ec2')

    print("HERE 2")
    vpc_id, subnet_id = get_vpc_and_subnet(ec2, 'us-east-1b')

    all_node_dns = []
    new_node_dns = []
    instance_count = 0

    for instance in ec2.instances.all():
        all_node_dns.append(instance.public_dns_name)
        instance_count += 1

    print(f"Existing instance count is {instance_count}")

    new_instances = ec2.create_instances(
        # ImageId='ami-079466db464206b00', # Custom AMI with dependencies preinstalled
        ImageId='ami-09d3b3274b6c5d4aa', # Amazon Linux 2 Kernel 5.10 AMI 2.0.20221004.0 x86_64 HVM gp2
        InstanceType='t3.medium', # 2 vCPU, 4 GiB memory
        KeyName='CacheCow', # keypair for authentication
        MaxCount=num_nodes,
        MinCount=num_nodes,
        Placement={
            'AvailabilityZone': 'us-east-1b'
        },
        SecurityGroups=[
            'cachecow-security', # security group for firewall
        ],
        TagSpecifications=[
            {
                'ResourceType': 'instance',
                'Tags': [
                    {
                        'Key': 'Name',
                        'Value': 'CacheCow Node' # instance name
                    },
                ]
            },
        ],
        BlockDeviceMappings=[
            {
                'DeviceName': '/dev/xvda',
                'Ebs': {
                    'VolumeSize': 16,
                }
            },
        ]
    )

    for instance in new_instances:
        instance.wait_until_running()
        instance.load()
        all_node_dns.append(instance.public_dns_name)
        new_node_dns.append(instance.public_dns_name)

    node_dns_f = io.StringIO("\n".join(x + f":{cache_port}" for x in all_node_dns))

    for i, node in enumerate(new_node_dns):
        c = connect_retry(node, "ec2-user", "CacheCow.pem")

        c.run("sudo yum update -y")
        # c.run("sudo yum install git -y")
        # c.run("sudo amazon-linux-extras install java-openjdk11 -y")
        c.run("sudo yum install git java-11-amazon-corretto-headless tmux -y")

        c.run("git clone https://github.com/RiceComp413-Fall2022/CacheCow")
        # c.run("cd CacheCow/cache-node/ && git switch ec2-support")
        c.run("cd CacheCow/cache-node/ && git switch Autoscale-POC")

        c.put(node_dns_f, remote='CacheCow/cache-node/nodes.txt')

        c.run(f"tmux new-session -d \"cd CacheCow/cache-node/ && ./gradlew run --args '{system} {i + instance_count} {cache_port} -s -n'\"", asynchronous=True)

        c.run("curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.34.0/install.sh | bash")
        c.run(". ~/.nvm/nvm.sh")
        c.run("nvm install 16")

        c.put(node_dns_f, remote='CacheCow/monitor-node/src/nodes.txt') # TODO: use the same file

        c.run("tmux new-session -d \"cd CacheCow/monitor-node/ && . ~/.nvm/nvm.sh && npm install && npm start\"", asynchronous=True)

        c.close()

    for node in new_node_dns:
        wait_node(node)

# Program entry point
mode = sys.argv[1]
num_nodes = int(sys.argv[2])

scaleable_param = ""
if len(sys.argv) == 4 and sys.argv[3] == "-s":
    scaleable_param = "-s"

if (mode == "create"):
    launch_cluster(num_nodes, scaleable_param)
elif (mode == "add"):
    scale_cluster(num_nodes)
else:
    print(f"Launch mode {mode} not recognized, only 'create' and 'add' supported")
