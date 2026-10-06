# ThrottleGate Admin Dashboard

A real-time monitoring dashboard for the ThrottleGate rate limiting service — modern dark UI, live metrics, and interactive visualizations.

## Features

- **Live stat cards** — requests/second, allowed/denied counts, and allow ratio with a pulsing live indicator
- **Throughput chart** — gradient area chart of requests per second over the last 30 samples
- **Allow vs. deny breakdown** — doughnut chart with lifetime distribution and a center total
- **Auto-refresh** every 5 seconds with connection-loss banner and manual retry
- **Responsive dark theme** built with Tailwind CSS, glass-morphism cards, and staggered entrance animations

## Tech Stack

- React 18 + Vite (@vitejs/plugin-react)
- Tailwind CSS v3 (via PostCSS)
- chart.js v4 + react-chartjs-2

## Getting Started

### Prerequisites
- Node.js 18+ (Vite 6 requirement)

### Installation

```bash
cd throttle-gate-dashboard
npm install
```

### Running the Dashboard

1. Make sure the ThrottleGate service is running (default: `http://localhost:8080`)
2. Start the dashboard:
   ```bash
   npm run dev
   ```
3. Open your browser to `http://localhost:3000`

> The Vite dev server proxies `/api` and `/v1` calls to `http://localhost:8080` (see `vite.config.js`), so no CORS setup is needed while developing.

### API Endpoint Configuration

The dashboard reads the metrics endpoint from `VITE_API_URL` (defaults to `http://localhost:8080`). Vite only exposes env vars prefixed with `VITE_` to client code:

```bash
VITE_API_URL=http://my-host:8080 npm run dev
```

Endpoint used:

- `GET {API_BASE_URL}/api/metrics/throttlegate.requests` — returns `requestsPerSecond`, `allowedCount`, `deniedCount`, and `allowRatio` (`API_BASE_URL` = `VITE_API_URL` or the default)

## Customization

- **Refresh rate** — change the `POLL_OPTIONS` list in `src/App.jsx` (default 5 s, selectable in the header)
- **Chart history** — change `MAX_DATA_POINTS` in `src/App.jsx` (default 30 samples)
- **Theme** — Tailwind config lives in `tailwind.config.cjs`; base styles in `src/index.css`

## Building for Production

```bash
npm run build
```

This creates a production-ready build in the `build` directory (`build.outDir` in `vite.config.js`). Point the build at a different backend with `VITE_API_URL`:

```bash
VITE_API_URL=https://api.your-host:8080 npm run build
```
