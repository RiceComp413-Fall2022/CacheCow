#!/usr/bin/env python
# This python script runs various performance tests for the CacheCow distributed
# memory cache. It provides a standardized way to measure the performance of
# different implementations.

from functools import reduce
import json
import matplotlib.pyplot as plt
from multiprocessing import Pool
from optparse import OptionParser
import pandas as pd
import requests
import seaborn as sns
from statistics import mean

from floydWarshall import *
from perfTests import *

# Types of perfTestFunc: serial or parallel
def serialPerfTest(function, data, reduction=lambda a, b: a + b):
    return reduce(reduction, map(function, data))

def parallelPerfTest(function, data, reduction=lambda a, b: a + b):
    with Pool() as pool:
      return reduce(reduction, pool.map(function, data))



def get_backend_timing(node_url):
    """Fetches request timing data from the backend.
    """
    fetched_data = json.loads(requests.get(url=f'http://{node_url}/v1/node-info').content)
    request_timing = fetched_data['totalRequestTiming']
    store_timing = request_timing['storeTiming']
    fetch_timing = request_timing['fetchTiming']
    return store_timing, fetch_timing

def runPerfTest(options):
    # Initialize timing for multiple trials
    timing_data = {}
    if options.time_backend:
        timing_data["Backend Times"] = []
    if options.time_client:
        timing_data["Client Times"] = []
    if options.time_script:
        timing_data["Script Times"] = []

    # Run trials
    for trial in range(options.trials):
        # Calculate Initial Timing
        if options.time_backend:
            store_start_timing, fetch_start_timing = get_backend_timing(options.url)
        if options.time_script:
            script_start_time = time.perf_counter()

        # Run performance tests
        perfTestFunc = serialPerfTest
        if options.parallel:
            perfTestFunc = parallelPerfTest
        client_time = options.test(options.url, perfTestFunc=perfTestFunc)

        # Print Performance Time Metrics
        if options.time_backend:
            store_end_timing, fetch_end_timing = get_backend_timing(options.url)
            store_timing = store_end_timing - store_start_timing
            fetch_timing = fetch_end_timing - fetch_start_timing
            total_request_time = store_timing + fetch_timing
            timing_data["Backend Times"].append(total_request_time)
        if options.time_client:
            timing_data["Client Times"].append(client_time)
        if options.time_script:
            timing_data["Script Times"].append(time.perf_counter() - script_start_time)

    timing_df = pd.DataFrame(timing_data)

    # Print average timing
    if options.time_backend:
        print("Average Backend Timing: ", mean(timing_data["Backend Times"]))
    if options.time_client:
        print("Average Client Timing: ", mean(timing_data["Client Times"]))
    if options.time_script:
        print("Average Performance Script Timing: ", mean(timing_data["Script Times"]))

    timing_df["HTTP Times"] = timing_df["Client Times"] - timing_df["Backend Times"]

    # Graph
    if options.graph:
        sns.lineplot(data=timing_df, markers=True)
        plt.xlabel("Trials")
        plt.ylabel("Time (in seconds)")
        if options.parallel:
            plt.title("Timing per Trial (in Parallel)")
        else:
            plt.title("Timing per Trial (in Serial)")
        # ax.xaxis.get_major_locator().set_params(integer=True)
        plt.show()


if __name__ == "__main__":
    # Argument Parsing
    parser = OptionParser(usage="perfTest.py -u node_url [options]")
    parser.add_option("--url",
                type="string",
                dest="url",
                help="Node URL for sending HTTP requests.")
    parser.add_option("--test",
                type="string",
                default="store_and_fetch_test",
                dest="test_name",
                help="Performance test to run. Default is 'store_and_fetch_test'.")
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
    parser.add_option("-p", "--parallel",
                action = "store_true",
                default=False,
                dest="parallel",
                help="Run Performance Tests in Parallel. " +
                "By default, performance tests run in serial.")
    parser.add_option("--cap",
                type="int",
                dest="max_capacity",
                help="Maximum cache capacity.")
    parser.add_option("--trials",
                type="int",
                default=1,
                dest="trials",
                help="Number of performance test trials. These are run " +
                "back-to-back. The cache state is not reset in between trials." +
                " This is helpful to analyze how the cache works over time.")
    parser.add_option("-g", "--graph",
                action = "store_true",
                default=False,
                dest="graph",
                help="Graphs Performance Timing.")
    (options, args) = parser.parse_args()

    if not options.url:
        raise Exception("URL Argument is necessary for performance testing.")
    if not options.time_backend and not options.time_script:
        options.time_client = True
    print("Performing Test: ", options.test_name)
    options.test = PerfTest.load_test(options.test_name)

    runPerfTest(options)
