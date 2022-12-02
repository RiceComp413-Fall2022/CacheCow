import sys
import io
import time

import requests # pip install requests
import boto3 # pip install "boto3[crt]"
from fabric import Connection # pip install fabric
from invoke import Responder

<<<<<<< HEAD
# USAGE: python3 pasture.py <mode> <number of nodes> [-s]

MAX_NODES = 5

CACHE_PORT = 7070

SSH_USER = "ec2-user"

SSH_CREDS = "CacheCow.pem"

AWS_CREDS = "rootkey.csv"

"""
Placeholder
"""
=======
# TODO: automatic teardown

port = 7070

num_nodes = int(sys.argv[1])

ec2 = boto3.resource('ec2')

>>>>>>> master
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

<<<<<<< HEAD

"""
Placeholder
"""
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
=======
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
                'FromPort': 7070,
                'ToPort': 7070,
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
>>>>>>> master
                },
            )
            c.open()
            return c
        except Exception as e:
            print(f"Exception while connecting to host {host}: {e}")
            time.sleep(5)


"""
Placeholder
"""
def test_node(node):
    success = True
    try:
        requests.get(f"http://{node}:{CACHE_PORT}", timeout=5)
    except:
        success = False
    return success


<<<<<<< HEAD
"""
Placeholder
"""
def wait_node(node):
    print(f"Waiting for {node}")
    while not test_node(node):
        time.sleep(5)


"""
Placeholder
"""
def create_instances(ec2, num_nodes):
    return ec2.create_instances(
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
=======
node_dns_f = io.StringIO("\n".join(x + f":{port}" for x in node_dns))

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

for i, node in enumerate(node_dns):
    c = connect_retry(node, "ec2-user", "CacheCow.pem")
>>>>>>> master


"""
Placeholder
"""
def setup_services(c, id, node_dns_f, scaleable, new_node):
    print("ACTION: Cloning Repo")

    c.run("sudo yum update -y")
    # c.run("sudo yum install git -y")
    # c.run("sudo amazon-linux-extras install java-openjdk11 -y")
    c.run("sudo yum install git java-11-amazon-corretto-headless tmux -y")

    c.run("git clone https://github.com/RiceComp413-Fall2022/CacheCow")
    # c.run("cd CacheCow/cache-node/ && git switch ec2-support")
    c.run("cd CacheCow/cache-node/ && git switch Autoscale-POC")

    if (scaleable):
        print("ACTION: Setting Up AWS")

        c.put(SSH_CREDS, remote=f"CacheCow/{SSH_CREDS}")

        remote_credentials = f"CacheCow/{AWS_CREDS}"
        c.put(AWS_CREDS, remote=remote_credentials)

        access_id = c.run("awk -F: '{$1 = substr($1, index($1, \"=\") + 1, 100)} NR==1{print $1}' " + remote_credentials).stdout.strip()
        access_secret = c.run("awk -F: '{$1 = substr($1, index($1, \"=\") + 1, 100)} NR==2{print $1}' " + remote_credentials).stdout.strip()

        aws_watchers = [
            Responder(pattern=r'[.]*ID[.]*', response=access_id + "\n"),
            Responder(pattern=r'[.]*Secret[.]*', response=access_secret + "\n"),
            Responder(pattern=r'[.]*region[.]*', response="us-east-1\n"),
            Responder(pattern=r'[.]*output[.]*', response="\n")
        ]
        c.run("curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o awscliv2.zip")
        c.run("unzip awscliv2.zip")
        c.run("sudo ./aws/install")

        print("ACTION: Configuring AWS")
        c.run("aws configure", pty=True, watchers=aws_watchers)
        c.run("pip3 install requests boto3 fabric")

    print("ACTION: Starting services")

    c.put(node_dns_f, remote='CacheCow/cache-node/nodes.txt')

    scaleable_str = "-s" if scaleable else ""
    new_str = "-n" if new_node else ""
    c.run(f"tmux new-session -d \"cd CacheCow/cache-node/ && ./gradlew run --args 'aws {id} {CACHE_PORT} {scaleable_str} {new_str}'\"", asynchronous=True)

    c.run("curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.34.0/install.sh | bash")
    c.run(". ~/.nvm/nvm.sh")
    c.run("nvm install 16")
<<<<<<< HEAD

    c.put(node_dns_f, remote='CacheCow/monitor-node/src/nodes.txt') # TODO: use the same file
    c.run("tmux new-session -d \"cd CacheCow/monitor-node/ && . ~/.nvm/nvm.sh && npm install && npm start\"", asynchronous=True)


"""
Placeholder
"""
def launch_cluster(num_nodes, scaleable):

    if num_nodes > MAX_NODES:
        sys.stderr.write(f"Attempted to create {num_nodes} nodes, max is {MAX_NODES}\n")
        return

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
                    'FromPort': CACHE_PORT,
                    'ToPort': CACHE_PORT,
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


    instances = create_instances(ec2, num_nodes)

    node_dns = []
    for instance in instances:
        instance.wait_until_running()
        instance.load()
        node_dns.append(instance.public_dns_name)
        print(f"Node {instance.public_dns_name} finished loading")

    node_dns_f = io.StringIO("\n".join(x + f":{CACHE_PORT}" for x in node_dns))

    for i, node in enumerate(node_dns):
        c = connect_retry(node, SSH_USER, SSH_CREDS)

        setup_services(c, i, node_dns_f, scaleable, False)

        c.close()

    elb = boto3.client('elbv2')

    target_group = elb.create_target_group(
        Name='cachecow-nodes',
        Protocol='TCP',
        Port=CACHE_PORT,
        TargetType='instance',
        VpcId=vpc_id
    )

    target_group_arn = target_group['TargetGroups'][0]['TargetGroupArn']

    targets = elb.register_targets(
        TargetGroupArn=target_group_arn,
        Targets=[{'Id': x.id, 'Port': CACHE_PORT} for x in instances]
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
        Port=CACHE_PORT,
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


"""
Placeholder
"""
def scale_cluster(num_nodes):
    ec2 = boto3.resource('ec2')
    vpc_id, subnet_id = get_vpc_and_subnet(ec2, 'us-east-1b')

    all_node_dns = []
    new_node_dns = []
    instance_count = 0

    for instance in ec2.instances.all():
        if instance.state['Name'] == 'running':
            all_node_dns.append(instance.public_dns_name)
            instance_count += 1

    if instance_count + num_nodes > MAX_NODES:
        sys.stderr.write(f"Attempted to scale cluster to {instance_count + num_nodes} nodes, max is {MAX_NODES}\n")
        return

    print(f"Existing instance count is {instance_count}")

    new_instances = create_instances(ec2, num_nodes)

    for instance in new_instances:
        instance.wait_until_running()
        instance.load()
        all_node_dns.append(instance.public_dns_name)
        new_node_dns.append(instance.public_dns_name)

    node_dns_f = io.StringIO("\n".join(x + f":{CACHE_PORT}" for x in all_node_dns))

    for i, node in enumerate(new_node_dns):
        c = connect_retry(node, SSH_USER, SSH_CREDS)

        id = i + instance_count

        setup_services(c, id, node_dns_f, True, True)

        c.close()

    for node in new_node_dns:
        wait_node(node)


def teardown_cluster():
    elb = boto3.client('elbv2')
    balancer_arn = elb.describe_load_balancers(Names=['cachecow-balancer'])['LoadBalancers'][0]['LoadBalancerArn']
    elb.delete_load_balancer(LoadBalancerArn=balancer_arn)
    target_group_arn = elb.describe_target_groups(Names=['cachecow-nodes'])['TargetGroups'][0]['TargetGroupArn']
    elb.delete_target_group(TargetGroupArn=target_group_arn)
    ec2 = boto3.resource('ec2')
    instances = list(ec2.instances.filter(Filters=[{'Name': 'tag:Name', 'Values': ['CacheCow Node']}]))
    for instance in instances:
        instance.terminate()
    for instance in instances:
        instance.wait_until_terminated()
    security_group = list(ec2.security_groups.filter(Filters=[{'Name': 'group-name', 'Values': ['cachecow-security']}]))[0]
    security_group.delete()

# Program entry point
if __name__ == "__main__":
    mode = sys.argv[1]
    num_nodes = int(sys.argv[2])

    scaleable = False
    if len(sys.argv) == 4 and sys.argv[3] == "-s":
        scaleable = True

    if (mode == "create"):
        launch_cluster(num_nodes, scaleable)
    elif (mode == "add"):
        scale_cluster(num_nodes)
    elif (mode == "delete"):
        teardown_cluster()
    else:
        # TODO: Add better help message
        print(f"Launch mode {mode} not recognized, only 'create' and 'add' supported")
=======

    c.put(node_dns_f, remote='CacheCow/monitor-node/src/nodes.txt') # TODO: use the same file

    c.run("tmux new-session -d \"cd CacheCow/monitor-node/ && . ~/.nvm/nvm.sh && npm install && npm start\"", asynchronous=True)

    c.close()

elb = boto3.client('elbv2')

target_group = elb.create_target_group(
    Name='cachecow-nodes',
    Protocol='TCP',
    Port=7070,
    TargetType='instance',
    VpcId=vpc_id
)

target_group_arn = target_group['TargetGroups'][0]['TargetGroupArn']

targets = elb.register_targets(
    TargetGroupArn=target_group_arn,
    Targets=[{'Id': x.id, 'Port': 7070} for x in instances]
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
    Port=7070,
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

def test_node(node):
    success = True
    try:
        requests.get(f"http://{node}:{port}", timeout=5)
    except:
        success = False
    return success

def wait_node(node):
    print(f"Waiting for {node}")
    while not test_node(node):
        time.sleep(5)

for node in node_dns:
    wait_node(node)

wait_node(elb_dns)
>>>>>>> master
