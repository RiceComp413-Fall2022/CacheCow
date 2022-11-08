#!/usr/bin/env python
# This python script runs various performance tests for the CacheCow distributed
# memory cache. It provides a standardized way to measure the performance of
# different implementations.

from functools import reduce
import json
from multiprocessing import Pool
from optparse import OptionParser
import requests

from perfTests import *

# Types of perfTestFunc: serial or parallel
def serialPerfTest(function, data, reduction=lambda a, b: a + b):
    return reduce(reduction, map(function, data))

def parallelPerfTest(function, data, reduction=lambda a, b: a + b):
    with Pool() as pool:
      return reduce(reduction, pool.map(function, data))


if __name__ == "__main__":
    # Argument Parsing
    parser = OptionParser(usage="perfTest.py -u node_url [options]")
    parser.add_option("-u", "--url",
                type="string",
                dest="url",
                help="Node URL for sending HTTP requests.")
    parser.add_option("-p", "--parallel",
                action = "store_true",
                default=False,
                dest="parallel",
                help="Run Performance Tests in Parallel. " +
                "By default, performance tests run in serial.")
    parser.add_option("-b", "--time_backend",
                action = "store_true",
                default=False,
                dest="time_backend",
                help="Prints Backend Runtime.")
    parser.add_option("-c", "--time_client",
                action = "store_true",
                default=False,
                dest="time_client",
                help="Prints Client Runtime. " +
                "Defaults to client runtime if no other timing options are specified.")
    parser.add_option("-s", "--time_script",
                action = "store_true",
                default=False,
                dest="time_script",
                help="Prints Performance Script Runtime.")
    (options, args) = parser.parse_args()

    if not options.url:
        raise Exception("URL Argument is necessary for performance testing.")
    if not options.time_backend and not options.time_script:
        options.time_client = True

    if options.time_script:
        script_time = time.perf_counter()

    # Run performance tests
    perfTestFunc = serialPerfTest
    if options.parallel:
        perfTestFunc = parallelPerfTest
    client_time = PerfTest.store_and_fetch_test(options.url, perfTestFunc=perfTestFunc)

    # Print Performance Time Metrics
    if options.time_backend:
        fetched_data = json.loads(requests.get(url=f'http://{options.url}/v1/node-info').content)
        request_timing = fetched_data['totalRequestTiming']
        store_timing = request_timing['storeTiming']
        fetch_timing = request_timing['fetchTiming']
        total_request_time = store_timing + fetch_timing
        print("Total Backend Timing: ", total_request_time, "(store: ", store_timing, "fetch: ", fetch_timing, ")")
    if options.time_client:
        print("Total Client Timing: ", client_time)
    if options.time_script:
        print("Performance Script Timing: ", time.perf_counter() - script_time)
