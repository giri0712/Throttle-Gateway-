import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Bar } from 'react-chartjs-2';
import './App.css';

function App() {
  const [metrics, setMetrics] = useState({
    requestsPerSecond: 0,
    allowedCount: 0,
    deniedCount: 0,
    allowRatio: 0
  });
  const [chartData, setChartData] = useState({
    labels: [],
    datasets: [{
      label: 'Requests per Second',
      data: [],
      backgroundColor: 'rgba(75, 192, 192, 0.2)',
      borderColor: 'rgba(75, 192, 192, 1)',
      borderWidth: 1
    }]
  });

  useEffect(() => {
    // Fetch metrics from ThrottleGate service
    const fetchMetrics = async () => {
      try {
        const response = await axios.get('http://localhost:8080/actuator/metrics/throttlegate.requests');
        setMetrics(response.data);

        // Update chart data
        const now = new Date();
        setChartData(prev => ({
          labels: [...prev.labels, now.toLocaleTimeString()],
          datasets: [{
            ...prev.datasets[0],
            data: [...prev.datasets[0].data, metrics.requestsPerSecond]
          }]
        }));
      } catch (error) {
        console.error('Error fetching metrics:', error);
      }
    };

    const interval = setInterval(fetchMetrics, 5000); // Fetch every 5 seconds
    fetchMetrics(); // Initial fetch

    return () => clearInterval(interval);
  }, [metrics]);

  return (
    <div className="App">
      <header className="App-header">
        <h1>ThrottleGate Admin Dashboard</h1>
        <p>Real-time API Rate Limiting Monitoring</p>
      </header>

      <main>
        <div className="metrics-panel">
          <div className="metric-card">
            <h2>Requests/Second</h2>
            <p className="metric-value">{metrics.requestsPerSecond.toFixed(2)}</p>
          </div>

          <div className="metric-card">
            <h2>Allowed Requests</h2>
            <p className="metric-value">{metrics.allowedCount}</p>
          </div>

          <div className="metric-card">
            <h2>Denied Requests</h2>
            <p className="metric-value">{metrics.deniedCount}</p>
          </div>

          <div className="metric-card">
            <h2>Allow Ratio</h2>
            <p className="metric-value">{metrics.allowRatio.toFixed(2)}%</p>
          </div>
        </div>

        <div className="chart-container">
          <h2>Requests per Second Over Time</h2>
          <Bar
            data={chartData}
            options={{
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                y: {
                  beginAtZero: true
                }
              }
            }}
          />
        </div>
      </main>
    </div>
  );
}

export default App;