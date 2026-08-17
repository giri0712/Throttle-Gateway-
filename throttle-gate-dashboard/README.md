# ThrottleGate Admin Dashboard

A real-time monitoring dashboard for the ThrottleGate rate limiting service — modern dark UI, live metrics, and interactive visualizations.

## Features

- **Live stat cards** — requests/second, allowed/denied counts, and allow ratio with a pulsing live indicator
- **Throughput chart** — gradient area chart of requests per second over the last 30 samples
- **Allow vs. deny breakdown** — doughnut chart with lifetime distribution and a center total
- **Auto-refresh** every 5 seconds with connection-loss banner and manual retry
- **Responsive dark theme** built with Tailwind CSS, glass-morphism cards, and staggered entrance animations

## Tech Stack

- React 18 + Create React App (react-scripts)
- Tailwind CSS v3 (via PostCSS)
- chart.js v4 + react-chartjs-2

## Getting Started

### Prerequisites
- Node.js 14+

### Installation

```bash
cd throttle-gate-dashboard
npm install
```

### Running the Dashboard

1. Make sure the ThrottleGate service is running (default: `http://localhost:8080`)
2. Start the dashboard:
   ```bash
   npm start
   ```
3. Open your browser to `http://localhost:3000`

### API Endpoint Configuration

The dashboard reads the metrics endpoint from `REACT_APP_API_URL` (defaults to `http://localhost:8080`):

```bash
REACT_APP_API_URL=http://my-host:8080 npm start
```

Endpoint used:

- `GET {REACT_APP_API_URL}/api/metrics/throttlegate.requests` — returns `requestsPerSecond`, `allowedCount`, `deniedCount`, and `allowRatio`

## Customization

- **Refresh rate** — change `REFRESH_INTERVAL_MS` in `src/App.js` (default 5000 ms)
- **Chart history** — change `MAX_DATA_POINTS` in `src/App.js` (default 30 samples)
- **Theme** — Tailwind config lives in `tailwind.config.js`; base styles in `src/index.css`

## Building for Production

```bash
npm run build
```

This creates a production-ready build in the `build` directory.
