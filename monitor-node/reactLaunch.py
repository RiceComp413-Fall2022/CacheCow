from re import sub
import subprocess
import os

subprocess.call(['npm', 'start', '--prefix', os.path.dirname(__file__)])