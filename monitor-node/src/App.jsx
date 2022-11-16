import React from 'react';
import { Dashboard } from './components/Dashboard';
import { useState } from 'react';
import { useEffect } from 'react';
import { Bar } from 'react-chartjs-2';
import { Box, Grid } from '@mui/material';
import nodes from './nodes.txt'
import axios from 'axios';



export function App() {
  let [nodeDnss, setNodeDnss] = useState([])

  useEffect(() => {
    fetch(nodes)
    .then(row => row.text())
    .then(text => {
      setNodeDnss(text.split("\n"))
    });
  }, [])
  
  return (
    <div tyle = {{padding: '20%'}}>
      <DisplayNodeCharts links = {nodeDnss}/>
    </div>
  );
}


function DisplayNodeCharts(props) {
  let [nodeInfos, setNodeInfos] = useState([]);
  let nodeNames = [];
  props.links.map(link => nodeNames.push(link))


  useEffect(() => {
    let interval = setInterval(() => {
      axios.all(props.links.map(link => axios.get('http://' + link + '/v1/node-info')))
      .then(axios.spread(function(...res) {
        setNodeInfos(res.map(sres => sres.data));
        console.log(nodeInfos)
      }))
      .catch((error) => {
        console.error(error);
      });
    }, 1000);
    return () => clearInterval(interval);
  });

  let memUsageData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.memUsage.usage)
      }
    ]
  };

  let keyCountData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.cacheInfo.totalKeys)
      }
    ]
  };

  let keyValBytesData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.cacheInfo.kvBytes)
      }
    ]
  };

  let receiverInfoData = {
    labels: nodeNames,
    datasets: [
      {
        label: 'StoreAttempts',
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.receiverUsageInfo.storeAttempts)
      },
      {
        label: 'StoreSuccesses',
        backgroundColor: '#E1FFB1',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.receiverUsageInfo.storeSuccesses)
      },
      {
        label: 'FetchAttempts',
        backgroundColor: '#CDFCF6',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.receiverUsageInfo.fetchAttempts)
      },
      {
        label: 'FetchSuccesses',
        backgroundColor: '#BCE29E',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.receiverUsageInfo.fetchSuccesses)
      },
      {
        label: 'InvalidRequests',
        backgroundColor: '#FF7D7D',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.receiverUsageInfo.invalidRequests)
      }
    ]
  };

  let senderInfoData = {
    labels: nodeNames,
    datasets: [
      {
        label: 'StoreAttempts',
        backgroundColor: '#98A8F8',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.senderUsageInfo.storeAttempts)
      },
      {
        label: 'StoreSuccesses',
        backgroundColor: '#E1FFB1',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.senderUsageInfo.storeSuccesses)
      },
      {
        label: 'FetchAttempts',
        backgroundColor: '#CDFCF6',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.senderUsageInfo.fetchAttempts)
      },
      {
        label: 'FetchSuccesses',
        backgroundColor: '#BCE29E',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 0,
        data: nodeInfos.map(nodeInfo => nodeInfo.senderUsageInfo.fetchSuccesses)
      }
    ]
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
    <Grid container spacing={2} alignItems='center' justifyContent='center' paddingTop='5%' paddingBottom='5%' paddingRight='20px' paddingLeft='20px'>
      <Grid item xs={3} md={4}>
        <BasicBarChart 
          data = {memUsageData}
          backgroundColor = 'white'
          chartTitle = 'Memory Usage'
          displayLegend = {false}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart 
          data = {keyCountData}
          backgroundColor = 'white'
          chartTitle = 'Key Count'
          displayLegend = {false}
        />
      </Grid>
      <Grid item xs={3} md={4}>
        <BasicBarChart 
          data = {keyValBytesData}
          backgroundColor = 'white'
          chartTitle = 'Bytes Stored'
          displayLegend = {false}
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
    <Box borderRadius = '3%' backgroundColor = {props.backgroundColor} style = {{justifyContent: 'center'}} >
      <Bar 
        style = {{margin: '10px'}}
        data = {props.data}
        options = {{
          responsiveness: true,
          plugins: {
            title:{
              display: true,
              text: props.chartTitle, 
            },
            legend:{
              display: props.displayLegend
            }
          }
        }}
      />
    </Box>
  </div>
  )
}
