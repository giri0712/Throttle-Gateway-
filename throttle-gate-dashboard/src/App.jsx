import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import MetricCard from './components/MetricCard';
import RpsLineChart from './components/RpsLineChart';
import AllowDenyChart from './components/AllowDenyChart';
import './index.css';

// Vite uses import.meta.env instead of process.env
// Variables must be prefixed with VITE_ to be exposed to client code
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const METRICS_URL = `${API_BASE_URL}/api/metrics/throttlegate.requests`;
const REFRESH_INTERVAL_MS = 5000;
const MAX_DATA_POINTS = 30;

const initialMetrics = {
  requestsPerSecond: 0,
  allowedCount: 0,
  deniedCount: 0,
  allowRatio: 0
};

const initialChartData = {
  labels: [],
  requestsPerSecond: []
};

function App() {
  const [metrics, setMetrics] = useState(initialMetrics);
  const [chartData, setChartData] = useState(initialChartData);
  const [error, setError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(null);

  const fetchMetrics = useCallback(async () => {
    try {
      const response = await axios.get(METRICS_URL);
      const m = response.data || {};

      setMetrics({
        requestsPerSecond: m.requestsPerSecond ?? 0,
        allowedCount: m.allowedCount ?? 0,
        deniedCount: m.deniedCount ?? 0,
        allowRatio: m.allowRatio ?? 0
      });
      setError(null);
      setLastUpdate(new Date());

      setChartData((prev) => {
        const labels = [...prev.labels, new Date().toLocaleTimeString()].slice(-MAX_DATA_POINTS);
        const requestsPerSecond = [...prev.requestsPerSecond, m.requestsPerSecond ?? 0].slice(-MAX_DATA_POINTS);
        return { labels, requestsPerSecond };
      });
    } catch (err) {
      console.error('Error fetching metrics:', err);
      setError(`Failed to reach ThrottleGate at ${API_BASE_URL}. Is the service running?`);
      setLastUpdate(new Date());
    }
  }, []);

  useEffect(() => {
    fetchMetrics();
    const interval = setInterval(fetchMetrics, REFRESH_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [fetchMetrics]);

  const { requestsPerSecond, allowedCount, deniedCount, allowRatio } = metrics;
  const totalRequests = allowedCount + deniedCount;
  const isLive = !error && lastUpdate !== null;

  return (
    <div className="min-h-screen text-slate-100">
      {/* Header */}
      <header className="sticky top-0 z-20 border-b border-slate-800/80 bg-slate-950/70 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-400 to-violet-500 shadow-lg shadow-cyan-500/20">
              <svg viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
                <path d="M13 2 3 14h7l-1 8 10-12h-7l1-8z" />
              </svg>
            </div>
            <div>
              <h1 className="text-sm font-bold leading-tight tracking-tight text-slate-50 sm:text-base">
                ThrottleGate
              </h1>
              <p className="text-[11px] font-medium uppercase tracking-widest text-slate-500">
                Admin Dashboard
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden items-center gap-2 rounded-full border border-slate-800 bg-slate-900/60 px-3 py-1.5 sm:flex">
              <span className={`relative flex h-2 w-2 ${isLive ? '' : 'opacity-40'}`}>
                {isLive && <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />}
                <span className={`relative inline-flex h-2 w-2 rounded-full ${isLive ? 'bg-emerald-400' : 'bg-slate-500'}`} />
              </span>
              <span className="text-xs font-semibold text-slate-300">{isLive ? 'Live' : 'Offline'}</span>
            </div>
            <div className="text-right">
              <p className="text-[11px] text-slate-500">Updated</p>
              <p className="font-mono text-xs font-semibold text-slate-300">
                {lastUpdate ? lastUpdate.toLocaleTimeString() : '—'}
              </p>
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {/* Error banner */}
        {error && (
          <div className="mb-6 flex items-center gap-3 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 animate-fade-in-up">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5 shrink-0 text-rose-400">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 8v4M12 16h.01" />
            </svg>
            <div className="text-sm">
              <p className="font-semibold text-rose-300">Connection lost</p>
              <p className="text-rose-200/70">{error}</p>
            </div>
            <button
              onClick={fetchMetrics}
              className="ml-auto rounded-lg border border-rose-400/40 px-3 py-1.5 text-xs font-semibold text-rose-300 transition hover:bg-rose-500/20"
            >
              Retry now
            </button>
          </div>
        )}

        {/* Stat cards */}
        <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard
            label="Requests / second"
            value={requestsPerSecond.toFixed(2)}
            sub="Rolling 1s window"
            icon="activity"
            accent="text-cyan-400"
            delay={0}
          />
          <MetricCard
            label="Allowed requests"
            value={allowedCount.toLocaleString()}
            sub={`${totalRequests.toLocaleString()} total`}
            icon="check"
            accent="text-emerald-400"
            delay={60}
          />
          <MetricCard
            label="Denied requests"
            value={deniedCount.toLocaleString()}
            sub="Returned 429 Too Many Requests"
            icon="denied"
            accent="text-rose-400"
            delay={120}
          />
          <MetricCard
            label="Allow ratio"
            value={`${allowRatio.toFixed(1)}%`}
            sub="Allowed / total requests"
            icon="ratio"
            accent="text-violet-400"
            delay={180}
          />
        </section>

        {/* Charts */}
        <section className="mt-6 grid grid-cols-1 gap-4 lg:grid-cols-3">
          <div className="glass-card p-5 lg:col-span-2 animate-fade-in-up" style={{ animationDelay: '120ms' }}>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 className="text-sm font-bold text-slate-200">Throughput</h2>
                <p className="text-xs text-slate-500">Requests per second over time (last {MAX_DATA_POINTS} samples)</p>
              </div>
              <div className="flex items-center gap-2 text-xs font-medium text-slate-400">
                <span className="h-2 w-2 rounded-full bg-cyan-400" />
                Requests / sec
              </div>
            </div>
            <div className="h-72">
              <RpsLineChart labels={chartData.labels} data={chartData.requestsPerSecond} />
            </div>
          </div>

          <div className="glass-card p-5 animate-fade-in-up" style={{ animationDelay: '180ms' }}>
            <div className="mb-4">
              <h2 className="text-sm font-bold text-slate-200">Allow vs. Deny</h2>
              <p className="text-xs text-slate-500">Lifetime distribution</p>
            </div>
            <div className="relative h-56">
              <AllowDenyChart allowed={allowedCount} denied={deniedCount} />
              <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                <p className="font-mono text-2xl font-semibold tabular-nums text-slate-100">
                  {totalRequests.toLocaleString()}
                </p>
                <p className="text-[11px] uppercase tracking-wider text-slate-500">Total requests</p>
              </div>
            </div>
            <div className="mt-4 flex items-center justify-around border-t border-slate-800 pt-4 text-sm">
              <div className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-400" />
                <div>
                  <p className="font-mono text-sm font-semibold text-slate-200">{allowedCount.toLocaleString()}</p>
                  <p className="text-[11px] text-slate-500">Allowed</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className="h-2.5 w-2.5 rounded-full bg-rose-500" />
                <div>
                  <p className="font-mono text-sm font-semibold text-slate-200">{deniedCount.toLocaleString()}</p>
                  <p className="text-[11px] text-slate-500">Denied</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Footer */}
        <footer className="mt-10 flex flex-col items-center gap-1 border-t border-slate-800/60 pt-6 text-center">
          <p className="text-xs text-slate-500">
            Polling <code className="rounded bg-slate-800/80 px-1.5 py-0.5 font-mono text-[11px] text-cyan-300">{METRICS_URL}</code>
          </p>
          <p className="text-[11px] text-slate-600">
            Auto-refreshes every {REFRESH_INTERVAL_MS / 1000}s
            {API_BASE_URL !== 'http://localhost:8080' && ` · API base: ${API_BASE_URL}`}
          </p>
        </footer>
      </main>
    </div>
  );
}

export default App;
