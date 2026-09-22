import React from 'react';
import { Line } from 'react-chartjs-2';
import '../chartSetup';

const tooltipStyle = {
  backgroundColor: '#141426',
  borderColor: '#3a3a54',
  borderWidth: 1,
  padding: 10,
  cornerRadius: 0,
  titleColor: '#f5f4f9',
  titleFont: { family: '"IBM Plex Mono", monospace', weight: 700, size: 11 },
  bodyColor: '#b4adc8',
  bodyFont: { family: '"IBM Plex Mono", monospace', size: 11 },
  displayColors: false,
  caretSize: 0
};

export default function RpsLineChart({ labels, data }) {
  const chartData = {
    labels,
    datasets: [
      {
        label: 'Requests / second',
        data,
        borderColor: '#8fb0ff',
        borderWidth: 2,
        fill: true,
        backgroundColor: (context) => {
          const { ctx, chartArea } = context.chart;
          if (!chartArea) return 'rgba(143, 176, 255, 0.06)';
          const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          gradient.addColorStop(0, 'rgba(143, 176, 255, 0.25)');
          gradient.addColorStop(1, 'rgba(143, 176, 255, 0.01)');
          return gradient;
        },
        pointRadius: 0,
        pointHoverRadius: 3,
        pointHoverBackgroundColor: '#8fb0ff',
        pointHoverBorderColor: '#080810',
        pointHoverBorderWidth: 2,
        tension: 0.25
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
        ticks: { color: '#7a738f', maxTicksLimit: 8, maxRotation: 0, font: { family: '"IBM Plex Mono", monospace', size: 10 } }
      },
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(42, 42, 61, 0.6)' },
        border: { display: false },
        ticks: { color: '#7a738f', precision: 0, font: { family: '"IBM Plex Mono", monospace', size: 10 } }
      }
    }
  };

  return <Line data={chartData} options={options} />;
}
