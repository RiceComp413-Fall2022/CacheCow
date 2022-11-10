# This python script contains test datasets for running performance tests.

class TestData:
    """Stores client API data.
    """
    def __init__(self, node_url, key, version, value=None):
        self.node_url = node_url
        self.key = key
        self.version = version
        self.value = value

    def unpack(self):
        return self.node_url, self.key, self.version, self.value

    def __repr__(self):
        return f'(URL={self.node_url}, key={self.key}, version={self.version}, value={self.value})'

class TestDatasets:
    """Static generative datasets for performance tests.
    """

    def generate_store_data(url, keys, versions, values):
        return [TestData(url, key, version, value) for key, version, value in zip(keys, versions, values)]

    def generate_fetch_data(url, keys, versions):
        return [TestData(url, key, version) for key, version in zip(keys, versions)]
