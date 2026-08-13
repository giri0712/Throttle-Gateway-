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
  const [error, setError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(null);

  useEffect(() => {
    // Fetch metrics from ThrottleGate service
    const fetchMetrics = async () => {
      try {
        const response = await axios.get('http://localhost:8080/actuator/metrics/throttlegate.requests');
        setMetrics(response.data);
        setError(null); // Clear error on successful fetch
        setLastUpdate(new Date());

        // Update chart data - limit to last 30 data points to prevent memory buildup
        const now = new Date();
        setChartData(prev => {
          const MAX_DATA_POINTS = 30;
          const newLabels = [...prev.labels, now.toLocaleTimeString()];
          const newData = [...prev.datasets[0].data, metrics.requestsPerSecond];

          // Keep only the last MAX_DATA_POINTS entries
          const trimmedLabels = newLabels.slice(-MAX_DATA_POINTS);
          const trimmedData = newData.slice(-MAX_DATA_POINTS);

          return {
            labels: trimmedLabels,
            datasets: [{
              ...prev.datasets[0],
              data: trimmedData
            }]
          };
        });
      } catch (error) {
        console.error('Error fetching metrics:', error);
        setError('Failed to fetch metrics from ThrottleGate service. Please ensure the service is running on http://localhost:8080');
        setLastUpdate(new Date());
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

      {error && (
        <div className="error-alert">
          <p>{error}</p>
          <p>Last update attempt: {lastUpdate ? lastUpdate.toLocaleTimeString() : 'Never'}</p>
        </div>
      )}

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