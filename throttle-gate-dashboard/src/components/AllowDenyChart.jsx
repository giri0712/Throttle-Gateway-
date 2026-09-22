import React from 'react';
import { Doughnut } from 'react-chartjs-2';
import '../chartSetup';

export default function AllowDenyChart({ allowed, denied }) {
  const total = allowed + denied;

  const chartData = {
    labels: ['Allowed', 'Denied'],
    datasets: [
      {
        data: [allowed, denied],
        backgroundColor: ['#22c55e', '#f25555'],
        borderColor: '#141426',
        borderWidth: 3,
        hoverOffset: 4
      }
    ]
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    animation: { duration: 400 },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#141426',
        borderColor: '#3a3a54',
        borderWidth: 1,
        padding: 10,
        cornerRadius: 0,
        titleColor: '#f5f4f9',
        titleFont: { family: '"IBM Plex Mono", monospace', weight: 700, size: 11 },
        bodyColor: '#b4adc8',
        bodyFont: { family: '"IBM Plex Mono", monospace', size: 11 },
        caretSize: 0,
        callbacks: {
          label: (ctx) => {
            const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : '0.0';
            return ` ${ctx.label}: ${ctx.parsed.toLocaleString()} (${pct}%)`;
          }
        }
      }
    }
  };

  return <Doughnut data={chartData} options={options} />;
}
