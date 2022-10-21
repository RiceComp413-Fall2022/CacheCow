import React from 'react';
import { Dashboard } from './components/Dashboard';
import { useState } from 'react';
import { useEffect } from 'react';
import axios from 'axios';
axios.defaults.baseURL = 'http://localhost:7070/';
axios.defaults.withCredentials = true;



export function App() {
  return (
    <div>
      <FetchFull/>
    </div>
    // <div id='chart'>
    //   <Dashboard />
    // </div>
  );
}


function FetchFull() {
  const [error, setError] = useState(null);
  const [isLoaded, setIsLoaded] = useState(false);
  const [items, setItems] = useState(null);

  // Note: the empty deps array [] means
  // this useEffect will run once
  // similar to componentDidMount()
  useEffect(() => {
    axios.get("/node-info")
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