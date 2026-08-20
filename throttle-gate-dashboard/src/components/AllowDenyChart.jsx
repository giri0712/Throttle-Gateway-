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
        backgroundColor: ['#10b981', '#f43f5e'],
        borderColor: '#0f172a',
        borderWidth: 3,
        hoverOffset: 6
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
        backgroundColor: '#0f172a',
        borderColor: 'rgba(148, 163, 184, 0.2)',
        borderWidth: 1,
        padding: 10,
        titleColor: '#e2e8f0',
        titleFont: { family: 'Inter', weight: 600 },
        bodyColor: '#94a3b8',
        bodyFont: { family: 'Inter' },
        caretSize: 6,
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
