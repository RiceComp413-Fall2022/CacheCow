import sys
import io
import time
import subprocess
import os

import requests # pip install requests
import boto3 # pip install boto3[crt]
from fabric import Connection # pip install fabric
from paramiko.ssh_exception import NoValidConnectionsError

# TODO: dynamically create VPC (with subnet, etc.), keypair, security group
# TODO: automatic teardown

port = 7070

num_nodes = int(sys.argv[1])

ec2 = boto3.resource('ec2')

instances = ec2.create_instances(
    ImageId='ami-079466db464206b00', # Custom AMI with dependencies preinstalled
    # ImageId='ami-09d3b3274b6c5d4aa', # Amazon Linux 2 Kernel 5.10 AMI 2.0.20221004.0 x86_64 HVM gp2
    InstanceType='t3.medium', # 2 vCPU, 4 GiB memory
    KeyName='CacheCow', # keypair for authentication
    MaxCount=num_nodes,
    MinCount=num_nodes,
    Placement={
        'AvailabilityZone': 'us-east-1b'
    },
    SecurityGroups=[
        'cachecow', # security group for firewall
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

node_dns_str = "\n".join(x + f":{port}" for x in node_dns)
node_dns_f = io.StringIO(node_dns_str)

node_dns_real_f = os.path.dirname(__file__) + 'monitor-node/src/nodes.txt'

with open(node_dns_real_f, 'w') as nodeList:
    nodeList.write(node_dns_str)
    nodeList.truncate()

def connect_retry(host, user, key):
    while True:
        try:
            c = Connection(
                host=host,
                user=user,
                connect_kwargs={
                    "key_filename": key,
                },
            )
            c.open()
            return c
        except (NoValidConnectionsError, ConnectionResetError):
            pass
    time.sleep(5)

for i, node in enumerate(node_dns):
    c = connect_retry(node, "ec2-user", "CacheCow.pem")

    c.run("sudo yum update -y")
    # c.run("sudo yum install git -y")
    # c.run("sudo amazon-linux-extras install java-openjdk11 -y")
    c.run("sudo yum install git java-11-amazon-corretto-headless tmux -y")

    c.run("git clone https://github.com/RiceComp413-Fall2022/CacheCow")
    # c.run("cd CacheCow/cache-node/ && git switch ec2-support")

    c.put(node_dns_f, remote='CacheCow/cache-node/nodes.txt')

    c.run(f"tmux new-session -d \"cd CacheCow/cache-node/ && ./gradlew run --args '{i} {port}'\"", asynchronous=True)

    c.close()

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

#start the react app here, so it can read nodes.txt
subprocess.call(['npm', 'start', '--prefix', os.path.dirname(__file__) + '/monitor-node'])

elb = boto3.client('elbv2')

target_group = elb.create_target_group(
    Name='cachecow-nodes',
    Protocol='TCP',
    Port=7070,
    TargetType='instance',
    VpcId='vpc-0b0b55be375992f99'
)

target_group_arn = target_group['TargetGroups'][0]['TargetGroupArn']

targets = elb.register_targets(
    TargetGroupArn=target_group_arn,
    Targets=[{'Id': x.id, 'Port': 7070} for x in instances]
)

balancer = elb.create_load_balancer(
    Name='cachecow-balancer',
    Subnets=[
        'subnet-0f380160653779f61'
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

wait_node(elb_dns)