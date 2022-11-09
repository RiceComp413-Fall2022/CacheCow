#!/usr/bin/env python
# Floyd Warshall dynamic programming algorithm. This is a standard test to
# measure the effectiveness of our distributed memory cache. We assume querying
# the graph takes "query_time" seconds, and our distributed memory cache takes
# however long it takes. The "query_time" should be slower than our distributed
# memory cache for max speedup.

import numpy as np
from optparse import OptionParser
import random
import requests
import time
from tqdm import tqdm

class FloydWarshall:
    """This class generates a graph and runs the Floyd Warshall algorithm.

    This class generates a standardized graph. It also performs cache queries
    and handles cache misses. It implements algorithm timing as well.
    """

    def __init__(self, num_nodes, cache_url, query_time, prob_edge=0.3, max_weight=10, seed=50):
        """
        num_nodes: the number of nodes in the graph
        cache_url: url for the cache
        query_time: latency (in seconds) to query the graph.
        prob_edge: probability of an edge between two nodes
        max_weight: maximum edge weight
        seed: PRNG seed for consistency across graph datasets
        """
        self.num_nodes = num_nodes
        self.cache_url = cache_url
        self.query_time = query_time
        self.prob_edge = prob_edge
        self.max_weight = max_weight
        self.seed = seed
        self.graph = None

        self.query_hit, self.query_miss = 0, 0
        self.update_success, self.update_failure = 0, 0

        self.start_time = None # Algorithm start time
        self.elapsed_time = None # Total time spent in algorithm. Must stop clock.
        self.database_time = None # Time spent querying database.

    def generate_graph(self):
        """Generates a graph for the Floyd Warshall algorithm.
        Edges are generated randomly based on the provided seed.
        """
        np.random.seed(self.seed)
        graph=np.zeros(shape=(self.num_nodes, self.num_nodes))
        for i in range(self.num_nodes):
            for j in range(self.num_nodes):
                if i == j:
                    continue
                if np.random.random_sample() < self.prob_edge:
                    graph[i, j] = np.random.randint(self.max_weight+1) # Random edge weight
                else:
                    graph[i, j] = np.inf # No edge
        self.graph = graph

    def run(self):
        """Floyd Warshall Algorithm.
        """

        # Generate graph data
        self.generate_graph()

        # Start time
        self.start_clock()

        for k in tqdm(range(self.num_nodes)):
            for i in range(self.num_nodes):
                dist_i_k = self.query_graph(i, k)
                for j in range(self.num_nodes):
                    dist_i_j = self.query_graph(i, j)
                    dist_k_j = self.query_graph(k, j)

                    dist_i_j = min(dist_i_j, # k not in path from i to j
                                     dist_i_k + dist_k_j) # k in path from i to j

                    self.update_graph(i, j, dist_i_j)
                    #self.update_graph(k, j, dist_k_j)

        # End time
        self.stop_clock()

        self.print_statistics()

        return self.get_runtime()

    def query_graph(self, i, j):
        # Query cache
        key = f'{i}->{j}'
        version = 1 # Unused version
        fetched_data = requests.get(url=f'http://{self.cache_url}/v1/blobs/{key}/{version}')
        try:
            value = float(fetched_data.content.decode('ascii'))
            self.query_hit += 1
            return value
        except:
            print(fetched_data.status_code, fetched_data.content)
            self.query_miss += 1
            self.database_time += self.query_time
            return self.graph[i, j]

    def update_graph(self, i, j, value):
        # Store data into cache
        key = f'{i}->{j}'
        version = 1 # Unused version
        try:
            requests.post(url=f'http://{self.cache_url}/v1/blobs/{key}/{version}',
                      data = str(value).encode('ascii'))
            self.update_success += 1
        except:
            self.update_failure += 1

        # Store data into storage
        self.database_time += self.query_time
        self.graph[i, j] = value

    def start_clock(self):
        self.start_time = time.perf_counter()
        self.database_time = 0

    def stop_clock(self):
        assert self.start_time != None
        self.elapsed_time = time.perf_counter() - self.start_time

    def get_runtime(self):
        assert self.elapsed_time != None and self.database_time != None
        return self.elapsed_time + self.database_time

    def validate(self):
        raise Exception("Not yet implemented.")

    def print_statistics(self):
        print(f'Query Hit Rate: {self.query_hit / (self.query_miss + self.query_hit)} ' +
              f'(Hits: {self.query_hit}, Misses: {self.query_miss})')
        print(f'Update Success Rate: {self.update_success / (self.update_failure + self.update_success)} ' +
              f'(Number of Updates: {self.update_failure + self.update_success})')
        print(f'Total Time: {self.elapsed_time + self.database_time} sec '+
              f'(Database Time: {self.database_time} sec, ' +
              f'Non-Database Time: {self.elapsed_time} sec)')
        print(self.graph)



if __name__ == "__main__":
    # Argument Parsing
    parser = OptionParser(usage="floydWarshall.py -u node_url [options]")
    parser.add_option("--url",
                type="string",
                dest="url",
                help="Node URL for sending HTTP requests.")
    parser.add_option("-d",
                type="float",
                default=0.3,
                dest="query_time",
                help="Database Average Query Time.")
    parser.add_option("-n",
                type="int",
                default=10,
                dest="num_nodes",
                help="Number of nodes in graph.")
    parser.add_option("-p",
                type="float",
                default=0.3,
                dest="prob_edge",
                help="Probability of an edge between two nodes.")
    parser.add_option("-w",
                type="int",
                default=10,
                dest="max_weight",
                help="Maximum edge weight.")
    parser.add_option("-s",
                type="int",
                default=10,
                dest="seed",
                help="Seed.")
    (options, args) = parser.parse_args()

    if not options.url:
        raise Exception("URL Argument is necessary for performance testing.")


    floydWarshall = FloydWarshall(num_nodes=options.num_nodes,
                                  cache_url=options.url,
                                  query_time=options.query_time,
                                  prob_edge=options.prob_edge,
                                  max_weight=options.max_weight,
                                  seed=options.seed)
    floydWarshall.run()
