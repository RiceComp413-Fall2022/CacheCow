import React from 'react';
import { Dashboard } from './components/Dashboard';
import { useState } from 'react';
import { useEffect } from 'react';
import { Chart } from 'react-chartjs-2';
import axios from 'axios';
axios.defaults.withCredentials = true;



export function App() {
  return (
    <div>
      <FetchNodeInfo nodeId = {0} />
      <FetchNodeInfo nodeId = {1} />
    </div>
  );
}


function FetchNodeInfo(props) {
  const [error, setError] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [items, setItems] = useState(null);

  const url = "http://localhost:707" + props.nodeId + "/node-info";

  console.log(url)

  // Note: the empty deps array [] means
  // this useEffect will run once
  // similar to componentDidMount()
  useEffect(() => {
    axios.get(url)
      .then((result) => {
          setItems(result.data)
          console.log(result.data)
        })
      .catch((error) => {
        console.error(error);
      });
  }, [])

  if (items != null) {


    return (
      <div>
        <ul>
          {"NodeID: " + items.nodeId}
        </ul>
        <ul>
          {"\nCache info: " + items.cacheInfo.totalKeys + " keys, " + items.cacheInfo.kvBytes + " byte size of all keys and values"}
        </ul>
      </div>
    );
  }
  return (
    <ul>
      Response was not received
    </ul>
  )

}