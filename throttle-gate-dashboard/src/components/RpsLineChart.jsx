import React from 'react';
import { Line } from 'react-chartjs-2';
import '../chartSetup';

const tooltipStyle = {
  backgroundColor: '#0f172a',
  borderColor: 'rgba(148, 163, 184, 0.2)',
  borderWidth: 1,
  padding: 10,
  titleColor: '#e2e8f0',
  titleFont: { family: 'Inter', weight: 600 },
  bodyColor: '#94a3b8',
  bodyFont: { family: 'Inter' },
  displayColors: false,
  caretSize: 6
};

export default function RpsLineChart({ labels, data }) {
  const chartData = {
    labels,
    datasets: [
      {
        label: 'Requests / second',
        data,
        borderColor: '#22d3ee',
        borderWidth: 2,
        fill: true,
        backgroundColor: (context) => {
          const { ctx, chartArea } = context.chart;
          if (!chartArea) return 'rgba(34, 211, 238, 0.08)';
          const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          gradient.addColorStop(0, 'rgba(34, 211, 238, 0.28)');
          gradient.addColorStop(1, 'rgba(34, 211, 238, 0.01)');
          return gradient;
        },
        pointRadius: 0,
        pointHoverRadius: 4,
        pointHoverBackgroundColor: '#22d3ee',
        pointHoverBorderColor: '#0f172a',
        pointHoverBorderWidth: 2,
        tension: 0.35
      }
    ]
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 400 },
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        ...tooltipStyle,
        callbacks: {
          label: (ctx) => ` ${ctx.parsed.y.toFixed(2)} req/s`
        }
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: '#64748b', maxTicksLimit: 8, maxRotation: 0, font: { family: 'Inter', size: 11 } }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(148, 163, 184, 0.07)' },
        border: { display: false },
        ticks: { color: '#64748b', precision: 0, font: { family: 'Inter', size: 11 } }
      }
    }
  };

  return <Line data={chartData} options={options} />;
}
