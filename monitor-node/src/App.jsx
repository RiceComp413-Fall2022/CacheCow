import React from 'react';
import { Dashboard } from './components/Dashboard';
import { useState } from 'react';
import { useEffect } from 'react';
import { Bar } from 'react-chartjs-2';
import { Box, Grid } from '@mui/material';
import axios from 'axios';
axios.defaults.withCredentials = true;



export function App() {
  return (
    <div tyle = {{padding: '20%'}}>
      <DisplayNodeCharts nodeCount = {3}/>
    </div>
  );
}


function DisplayNodeCharts(props) {
  const [nodeInfos, setNodeInfos] = useState([]);

  let nodeNames = [];

  let links = []
  for (let i = 0; i < props.nodeCount; i++) {
    // TODO: will need to change this URL in the future
    const url = "http://localhost:707" + i + "/v1/node-info";
    links.push(url);
    nodeNames.push('Node' + i);
  } 

  console.log("fetching with axios");
  useEffect(() => {
    axios.all(links.map(link => axios.get(link)))
      .then(axios.spread(function(...res) {
        console.log(res);
        setNodeInfos(res.map(res => res.data));
      }))
      .catch((error) => {
        console.error(error);
      });
  }, [])
  

  if (nodeInfos != []) {
    const memUsageData = {
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
  
    const keyCountData = {
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

    const keyValBytesData = {
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

    const receiverInfoData = {
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

    const senderInfoData = {
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
        <Grid item xs={3} md={5}>
          <BasicBarChart 
            data = {receiverInfoData}
            backgroundColor = 'white'
            chartTitle = 'Receiver Info'
            displayLegend = {true}
          />
        </Grid>
        <Grid item xs={3} md={5}>
          <BasicBarChart 
            data = {senderInfoData}
            backgroundColor = 'white'
            chartTitle = 'Sender Info'
            displayLegend = {true}
          />
        </Grid>
      </Grid>
    );
  }
  else {
    return <text>Loading...</text>
  }
 

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
