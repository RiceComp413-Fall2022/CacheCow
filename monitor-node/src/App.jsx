import React from 'react';
import { useState } from 'react';
import { useEffect } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  PointElement,
  LineElement,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import { Box, Grid } from '@mui/material';
import nodes from './nodes.txt';
import axios from 'axios';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

export function App() {
  let [nodeDnss, setNodeDnss] = useState([]);

  useEffect(() => {
    fetch(nodes)
      .then((row) => row.text())
      .then((text) => {
        setNodeDnss(text.split('\n'));
      });
  }, []);

  return (
    <div tyle={{ padding: '20%' }}>
      <DisplayNodeCharts links={nodeDnss} global={true} />
    </div>
  );
}

function getLocalCacheInfo(links, setNodeInfos) {
  axios
    .all(
      links.map((link) => axios.get('http://' + link + '/v1/local-cache-info'))
    )
    .then(
      axios.spread(function (...res) {
        setNodeInfos(res.map((sres) => sres.data));
      })
    )
    .catch((error) => {
      console.error(error);
    });
}

function getGlobalCacheInfo(link, setNodeInfos, setNodeNames) {
  axios
    .get('http://' + link + '/v1/global-cache-info')
    .then((res) => {
      setNodeInfos(res.data);
      setNodeNames(res.data.map((res) => res.hostName));
    })
    .catch((error) => {
      console.error(error);
    });
}

function DisplayNodeCharts(props) {
  let [nodeInfos, setNodeInfos] = useState([]);
  let [nodeNames, setNodeNames] = useState(props.links);

  // Get the initial cache info
  useEffect(() => {
    if (props.links.length > 0) {
      props.global
        ? getGlobalCacheInfo(props.links[0], setNodeInfos, setNodeNames)
        : getLocalCacheInfo(props.links, setNodeInfos);
    }
  }, [props.links, props.global]);

  // Get the updated cache info
  useEffect(() => {
    let interval = setInterval(() => {
      if (nodeNames.length > 0) {
        props.global
          ? getGlobalCacheInfo(nodeNames[0], setNodeInfos, setNodeNames)
          : getLocalCacheInfo(nodeNames, setNodeInfos);
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [nodeNames, props.global]);

  let memUsageData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map((nodeInfo) => nodeInfo.memUsage.usage),
      },
    ],
  };

  let keyCountData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map((nodeInfo) => nodeInfo.cacheInfo.totalKeys),
      },
    ],
  };

  let keyValBytesData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map((nodeInfo) => nodeInfo.cacheInfo.memorySize),
      },
    ],
  };

  let receiverInfoData = {
    labels: nodeNames,
    datasets: [
      {
        label: 'Store Attempts',
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.receiverUsageInfo.storeAttempts
        ),
      },
      {
        label: 'Store Successes',
        backgroundColor: '#E1FFB1',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.receiverUsageInfo.storeSuccesses
        ),
      },
      {
        label: 'Fetch Attempts',
        backgroundColor: '#CDFCF6',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.receiverUsageInfo.fetchAttempts
        ),
      },
      {
        label: 'Fetch Successes',
        backgroundColor: '#BCE29E',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.receiverUsageInfo.fetchSuccesses
        ),
      },
      {
        label: 'Invalid Requests',
        backgroundColor: '#FF7D7D',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.receiverUsageInfo.invalidRequests
        ),
      },
    ],
  };

  let senderInfoData = {
    labels: nodeNames,
    datasets: [
      {
        label: 'Store Attempts',
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.senderUsageInfo.storeAttempts
        ),
      },
      {
        label: 'Store Successes',
        backgroundColor: '#E1FFB1',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.senderUsageInfo.storeSuccesses
        ),
      },
      {
        label: 'Fetch Attempts',
        backgroundColor: '#CDFCF6',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.senderUsageInfo.fetchAttempts
        ),
      },
      {
        label: 'Fetch Successes',
        backgroundColor: '#BCE29E',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(
          (nodeInfo) => nodeInfo.senderUsageInfo.fetchSuccesses
        ),
      },
    ],
  };

  let clientRequestTimingData = {
    labels: nodeNames,
    datasets: [
      {
        label: 'ClearTiming',
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.clientRequestTiming.clearTiming)
      },
      {
        label: 'FetchTiming',
        backgroundColor: '#E1FFB1',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.clientRequestTiming.fetchTiming)
      },
      {
        label: 'RemoveTiming',
        backgroundColor: '#CDFCF6',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.clientRequestTiming.removeTiming)
      },
      {
        label: 'StoreTiming',
        backgroundColor: '#BCE29E',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.clientRequestTiming.storeTiming)
      }
    ]
  };

  let serverRequestTimingData = {
    labels: nodeNames,
    datasets: [
      {
        label: 'ClearTiming',
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.serverRequestTiming.clearTiming)
      },
      {
        label: 'FetchTiming',
        backgroundColor: '#E1FFB1',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.serverRequestTiming.fetchTiming)
      },
      {
        label: 'RemoveTiming',
        backgroundColor: '#CDFCF6',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.serverRequestTiming.removeTiming)
      },
      {
        label: 'StoreTiming',
        backgroundColor: '#BCE29E',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.serverRequestTiming.storeTiming)
      }
    ]
  };


  return (
    <Grid
      container
      spacing={2}
      alignItems='center'
      justifyContent='center'
      paddingTop='5%'
      paddingBottom='5%'
      paddingRight='20px'
      paddingLeft='20px'
    >
      <Grid item xs={3} md={4}>
        <BasicBarChart
          data={memUsageData}
          backgroundColor='white'
          chartTitle='Memory Usage'
          displayLegend={false}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart
          data={keyCountData}
          backgroundColor='white'
          chartTitle='Key Count'
          displayLegend={false}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart
          data={keyValBytesData}
          backgroundColor='white'
          chartTitle='Bytes Stored'
          displayLegend={false}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart 
          data = {receiverInfoData}
          backgroundColor = 'white'
          chartTitle = 'Receiver Info'
          displayLegend = {true}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart 
          data = {senderInfoData}
          backgroundColor = 'white'
          chartTitle = 'Sender Info'
          displayLegend = {true}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart 
          data = {clientRequestTimingData}
          backgroundColor = 'white'
          chartTitle = 'Client Request Timing'
          displayLegend = {true}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart 
          data = {serverRequestTimingData}
          backgroundColor = 'white'
          chartTitle = 'Server Request Timing'
          displayLegend = {true}
        />
      </Grid>
    </Grid>
  );
}

function BasicBarChart(props) {
  return (
    <div>
      <Box
        borderRadius='3%'
        backgroundColor={props.backgroundColor}
        style={{ justifyContent: 'center' }}
      >
        <Bar
          style={{ margin: '10px' }}
          data={props.data}
          options={{
            responsiveness: true,
            plugins: {
              title: {
                display: true,
                text: props.chartTitle,
              },
              legend: {
                display: props.displayLegend,
              },
            },
          }}
        />
      </Box>
    </div>
  );
}
