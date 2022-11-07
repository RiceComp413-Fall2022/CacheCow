from re import sub
import subprocess
import os


node_dns = ['localhost:7070', 'localhost:7071', 'localhost:7072']


mock_dns_list = "\n".join(x for x in node_dns)
mock_node_dns_file = os.path.dirname(__file__) + '/nodes.txt'

with open(mock_node_dns_file, 'w') as nodeList:
    nodeList.write()
    nodeList.truncate()



# subprocess.call(['npm', 'start', '--prefix', os.path.dirname(__file__)])