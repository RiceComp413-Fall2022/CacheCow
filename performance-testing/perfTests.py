# This python script contains various performance tests to run. These
# performance tests are metrics to analyze the performance of different
# distributed memory cache implementations.

import requests
import time

from perfDatasets import *

class PerfTest:
    """Provides static performance test functions.

    Core functions are basic client API functions. The core functions used in
    composition to create full-scale tests. The full-scale tests are used to
    measure the performance of different implementations. Full-scale tests can
    run core functions in parallel to speed up execution.
    """

    # The following provide core test functions.
    def store_key_test(data):
        node_url, key, version, value = data.unpack()

        # Start time
        start_time = time.perf_counter()

        # Store Data
        requests.post(url=f'http://{node_url}/v1/blobs/{key}/{version}',
                      data = str(value).encode('ascii'))

        # End time
        return time.perf_counter() - start_time

    def fetch_key_test(data):
        node_url, key, version, value = data.unpack()

        # Start time
        start_time = time.perf_counter()

        # Fetch Data
        fetched_data = requests.get(url=f'http://{node_url}/v1/blobs/{key}/{version}').content

        # End time
        end_time = time.perf_counter() - start_time

        # Validate fetch
        assert fetched_data.decode('ascii') == str(value)

        return end_time


    # The following provide full-scale tests based on our core test functions.
    # The parameter perfTestFunc determines whether the full-scale test should
    # be run in serial or in parallel.
    def store_and_fetch_test(url, perfTestFunc):
        store_data = TestDatasets.generate_store_data(url,
                                                keys=range(10),
                                                versions=range(10),
                                                values=range(10))
        total_time = perfTestFunc(PerfTest.store_key_test, store_data)
        return total_time + perfTestFunc(PerfTest.fetch_key_test, store_data)

    def store_duplicate_key_test(url, perfTestFunc):
        raise Exception("Unimplemened.")

    def eviction_test(url, perfTestFunc):
        raise Exception("Unimplemened.")

    def memory_overflow_test(url, perfTestFunc, max_capacity):
        raise Exception("Unimplemened.")
