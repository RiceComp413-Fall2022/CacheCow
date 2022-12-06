#!/usr/bin/env python
# Long-tailed distribution test for our memory cache. This is a standard test to
# measure the effectiveness of our distributed memory cache. We assume querying
# the graph takes "query_time" seconds, and our distributed memory cache takes
# however long it takes. The "query_time" should be slower than our distributed
# memory cache for max speedup.

from optparse import OptionParser
import numpy as np
import matplotlib.pyplot as plt
import requests
import time

# Constants
TRIALS = 3 # TODO: Implement this.
NUM_INITIAL_STORE = 1500 # Should be larger than the max cache size to test cache effectiveness, and speed.
FETCH_PROPORTION = 1
NUM_REQUESTS = 500

STORE_TIMEOUT = 0.5
FETCH_TIMEOUT = 2

MU, SIGMA = 2, 1

if __name__ == "__main__":

    """Argument Parsing"""
    parser = OptionParser(usage="perfTest.py -u node_url [options]")
    parser.add_option("--url",
                type="string",
                dest="url",
                help="Node URL for sending HTTP requests.")
    (options, args) = parser.parse_args()

    """Fetch and Store"""
    def fetch(data):
        key, version = str(data), 1 # Unused version
        try:
            fetched_data = requests.get(url=f'http://{options.url}/v1/blobs/{key}/{version}', timeout=FETCH_TIMEOUT)
            value = float(fetched_data.content.decode('ascii'))
            return value == f(key)
        except:
            return False

    def store(data):
        key, version = str(data), 1 # Unused version
        value = f(key)
        try:
            requests.post(url=f'http://{options.url}/v1/blobs/{key}/{version}',
                      data = str(value).encode('ascii'), timeout=STORE_TIMEOUT)
            return True
        except:
            return False


    """Initialize Test"""
    np.random.seed(seed=0)

    initial_store_data = np.random.lognormal(MU, SIGMA, NUM_INITIAL_STORE).astype(int)
    request_data = np.random.lognormal(MU, SIGMA, NUM_REQUESTS).astype(int)
    request_types = np.random.binomial(n=1, p=FETCH_PROPORTION, size=NUM_REQUESTS) # 1: fetch, 0: store

    def f(key):
        """Some function s.t. value = f(key).

        Assumptions: key is a valid floating point value.
        """
        return float(key)

    fetch_total = (request_types == 1).sum()
    fetch_hits = 0

    store_total = NUM_INITIAL_STORE + NUM_REQUESTS - fetch_total
    store_success = 0

    """Run Tests"""
    # Start time
    start_time = time.perf_counter()

    # Store NUM_INITIAL_STORE values
    for data in initial_store_data:
        store_success += store(data)

    # Fetch and store NUM_REQUESTS values. Number is dependent on FETCH_PROPORTION.
    for request_type, data in zip(request_types, request_data):
        if request_type == 0:
            store(data)
        else:
            fetch_hits += fetch(data) # True = 1; False = 0

    # End time
    print("Time: ", time.perf_counter() - start_time)
    print("Fetch Hit Rate: ", fetch_hits / fetch_total)
    print("Store Success Rate: ", store_success / store_total if store_total > 0 else 1.0)

    stored_data = list(initial_store_data) + [data for index, data in enumerate(request_data) if request_types[index] == 0]
    fetched_data = [data for index, data in enumerate(request_data) if request_types[index] == 1]

    possible_hit = set(stored_data).intersection(set(fetched_data))
    all_data = set(stored_data).union(set(fetched_data))
    print("Jaccard Index (store vs. fetch data): ", len(possible_hit) / len(all_data))

    possible_hit = [data for data in fetched_data if data in set(stored_data)]
    print('Maximum Hit "Possible" Rate: ', len(possible_hit) / fetch_total)

    # Plot Data
    # plt.hist(request_data, bins=100, color='cyan', alpha=0.5)
    # plt.hist(initial_store_data, bins=100, color='green', alpha=0.5)
    # plt.title("Log-Normal Data")
    #
    # plt.title("Log-Normal Data")
    # plt.show()
