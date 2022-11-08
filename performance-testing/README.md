# Performance Testing
This folder contains scripts to measure performance of our distributed cache memory system.
Performance testing provides a metric to compare the performance of different implementation decisions.

Overall there are 3 different time metrics:
1. Backend Timing: Runtime of only the distributed backend.
2. Client Timing: Runtime of the client API calls and the distributed backend.
3. Performance Script Timing: Runtime of the entire performance script itself.

Each of these time metrics are important for various reasons. They allow us to determine
if bottlenecks are caused by client API calls or the backend. Further, it is important the 
performance testing script itself run efficiently, especially when testing multiple 
different implementations. In this case, the performance testing script supports parallelization.

## Run Performance Testing
Usage: perfTest.py [options]

Options:  
 -h, --help: 
 show this help message and exit
 
 -p, --parallel: 
Run Performance Tests in Parallel. By default, performance tests run in serial.  

-b, --time_backend: 
Prints Backend Runtime.

-c, --time_client: 
Prints Client Runtime. Defaults to client runtime if no other timing options are specified.

-s, --time_script: 
Prints Performance Script Runtime. 


