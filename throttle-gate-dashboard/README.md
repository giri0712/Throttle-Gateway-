# ThrottleGate Admin Dashboard

A real-time monitoring dashboard for the ThrottleGate rate limiting service.

## Features

- Real-time metrics display (requests/second, allowed/denied requests, allow ratio)
- Interactive chart showing requests per second over time
- Automatic data refresh every 5 seconds
- Responsive design

## Getting Started

### Prerequisites
- Node.js 14+
- npm or yarn

### Installation

1. Clone the repository
2. Navigate to the dashboard directory:
   ```bash
   cd throttle-gate-dashboard
   ```
3. Install dependencies:
   ```bash
   npm install
   ```

### Running the Dashboard

1. Make sure the ThrottleGate service is running on `http://localhost:8080`
2. Start the dashboard:
   ```bash
   npm start
   ```
3. Open your browser to `http://localhost:3000`

## API Endpoints Used

The dashboard expects the following endpoints to be available from the ThrottleGate service:

- `GET http://localhost:8080/actuator/metrics/throttlegate.requests` - Returns current metrics

Note: You'll need to configure Spring Boot Actuator in your ThrottleGate service to expose these metrics.

## Customization

To change the refresh rate, modify the interval in `src/App.js`:
```javascript
const interval = setInterval(fetchMetrics, 5000); // Change 5000 to desired milliseconds
```

## Building for Production

```bash
npm run build
```

This will create a production-ready build in the `build` directory.