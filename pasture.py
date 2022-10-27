import sys
import io
import time

import requests # pip install requests
import boto3 # pip install boto3[crt]
from fabric import Connection # pip install fabric

port = 7070

num_nodes = int(sys.argv[1])

ec2 = boto3.resource('ec2')

instances = ec2.create_instances(
    ImageId='ami-09d3b3274b6c5d4aa', # Amazon Linux 2 Kernel 5.10 AMI 2.0.20221004.0 x86_64 HVM gp2
    InstanceType='t3.medium', # 2 vCPU, 4 GiB memory
    KeyName='CacheCow', # keypair for authentication
    MaxCount=num_nodes,
    MinCount=num_nodes,
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

print(node_dns)

node_dns_f = io.StringIO("\n".join(x + f":{port}" for x in node_dns))

for i, node in enumerate(node_dns):
    c = Connection(
        host=node,
        user="ec2-user",
        connect_kwargs={
            "key_filename": "CacheCow.pem",
        },
    )

    c.run("sudo yum update -y")
    # c.run("sudo yum install git -y")
    # c.run("sudo amazon-linux-extras install java-openjdk11 -y")
    c.run("sudo yum install git java-11-amazon-corretto-headless tmux -y")

    c.run("git clone https://github.com/RiceComp413-Fall2022/CacheCow")
    c.run("cd CacheCow/cache-node/ && git switch ec2-support")

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

for node in node_dns:
    print(f"Waiting for node {node}")
    while not test_node(node):
        time.sleep(5)