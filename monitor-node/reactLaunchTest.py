from re import sub
import subprocess
import os


node_dns = ['localhost:7070', 'localhost:7071', 'localhost:7072']


mock_dns_list = "\n".join(x for x in node_dns)
print(mock_dns_list)
mock_node_dns_file = os.path.dirname(__file__) + '/src/nodes.txt'

with open(mock_node_dns_file, 'w') as nodeList:
    nodeList.write(mock_dns_list)
    nodeList.truncate()





# subprocess.call(['npm', 'start', '--prefix', os.path.dirname(__file__)])