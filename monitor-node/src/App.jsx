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
    <div>
      <DisplayNodeCharts nodeCount = {3} />
    </div>
  );
}


function DisplayNodeCharts(props) {
  const [nodeInfos, setNodeInfos] = useState([]);

  let nodeNames = [];

  let links = []
  for (let i = 0; i < props.nodeCount; i++) {
    // TODO: will need to change this URL in the future
    const url = "http://localhost:707" + i + "/node-info";
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
  
  const memUsageData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: 'rgba(180,80,80,1)',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 2,
        data: nodeInfos.map(nodeInfo => nodeInfo.memUsage.usage)
      }
    ]
  };

  const keyCountData = {
    labels: nodeNames,
    datasets: [
      {
        backgroundColor: 'rgba(100,100,200,1)',
        borderColor: 'rgba(0,0,0,1)',
        borderWidth: 2,
        data: nodeInfos.map(nodeInfo => nodeInfo.cacheInfo.totalKeys)
      }
    ]
  };

  return (
    <Grid container spacing={2} alignItems='center' justifyContent='center' paddingTop='5%'>
      <Grid item>
        <BasicBubbleChart 
          data = {memUsageData}
          backgroundColor = 'rgba(255,100,100,1)'
          chartTitle = 'Memory Usage'
        />
      </Grid>
      <Grid item>
        <BasicBubbleChart 
          data = {keyCountData}
          backgroundColor = 'rgba(180,200,230,1)'
          chartTitle = 'Key Count'
        />
      </Grid>
    </Grid>
  );

}

function BasicBubbleChart(props) {
  return (
  <Box borderRadius = '10%' backgroundColor = {props.backgroundColor}>
    <Bar
      borderWidth = '10%'
      data = {props.data}
      options = {{
        responsiveness: true,
        plugins: {
          title:{
            display: true,
            text: props.chartTitle, 
            fontSize: 20
          },
          legend:{
            display: false
          }
        }
      }}
    />
  </Box>
  )
}
