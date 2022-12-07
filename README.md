# CacheCow

![CacheCow](https://github.com/RiceComp413-Fall2022/CacheCow/actions/workflows/ci.yml/badge.svg)

Helpful Links: [Google Drive](https://drive.google.com/drive/u/0/folders/14yXw_k74cJNPWMZYnpjWMDIvtp1FH8T9), [Design Document](https://docs.google.com/document/d/1lT3F6lsjmoETbyx3xtu-MgMmEzld4PASVc9IrKrWGbw/)

# File Structure

Todo

# Set Up

1. Create a new directory

```sh
mkdir CacheCow
```

2. Clone the repository into the new directory

```sh
git clone git@github.com:RiceComp413-Fall2022/CacheCow.git CacheCow
```

## Running Locally

1. Download [Node.js](https://nodejs.org/en/download/) if it's not already installed

2. Set up the monitoring node

```sh
# Navigate to monitoring node directory
cd monitoring-node

# Install packages
npm install
```

3. Launch a local cache cluster

```sh
# Navigate to cache node directory
cd cache-node

# Run 3 node cluster locally
./gradlew run --args 'local 0 7070'
./gradlew run --args 'local 1 7071'
./gradlew run --args 'local 2 7072'
```

4. Check that the nodes are running

```sh
# Ping a cache node
curl -X GET "localhost:7070/v1/hello-world"
```

5. Monitor the cache

- Open [localhost:3000](http://localhost:3000) in any browser

## Running on AWS

1. Download [Python 3](https://www.python.org/downloads/) if not already installed

2. Download the [AWS Command Line Interface](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)

3. Set up an AWS key pair

- Navigate to `EC2 Dashboard` on AWS Management Console
- Create a new key pair
- Download the pair and save the file `rootkey.csv` to the root CacheCow directory

4. Configure the AWS CLI

```sh
aws configure

# Respond to the prompts using your credentials in rootkey.csv
AWS Access Key ID []: <your-access-key>
AWS Secret Access Key []: <your-secret-key>
Default Region Name []: us-east-1
Default Output Format []: <enter>
```

5. Install Python packages

```sh
pip3 install requests "boto3[crt]" fabric

# Note: if boto3 install fails try this
pip3 install --upgrade pip
```

6. Launch a cluster on AWS

```sh
python3 pasture.py create 2
```

7. Delete the cluster

```sh
python3 pasture.py delete 2
```

# Sending Requests

Todo

# Performance Testing

There are multiple performance tests. Here, we will run long-tailed.py which uses a heavy-tailed lognormal distribution to simulate cache-aside performance. The test is best performed against a cache which can hold a maximum of 100 keys. The distribution parameters generate 995 keys, 408 of which are unique. This ensures that the cache handles eviction appropriately.

1. Move into the performance-testing directory

```sh
cd CacheCow/performance-testing
```

2. Run the test script: long-tailed.py

```sh
python3 long-tailed.py --url <domain name>
```

For example,

```sh
python3 long-tailed.py --url localhost:7070
```
