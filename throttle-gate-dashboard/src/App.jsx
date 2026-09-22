import React, { useState, useEffect, useCallback, useMemo } from 'react';
import axios from 'axios';
import MetricCard from './components/MetricCard';
import RpsLineChart from './components/RpsLineChart';
import AllowDenyChart from './components/AllowDenyChart';
import './index.css';

// Vite uses import.meta.env instead of process.env
// Variables must be prefixed with VITE_ to be exposed to client code
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const METRICS_URL = `${API_BASE_URL}/api/metrics/throttlegate.requests`;
const MAX_DATA_POINTS = 30;
const POLL_OPTIONS = [2, 5, 10, 30]; // seconds — selectable in the header

const BOOT_LINES = [
  'THROTTLEGATE v1.0.0 — TRAFFIC CONTROL CONSOLE',
  'MOUNTING REDIS/VALKEY STATE ......... OK',
  'LOADING LUA SCRIPTS (ATOMIC) ........ OK',
  'ALGORITHM: TOKEN-BUCKET [STRATEGY] .. OK',
  'LINKING METRICS ENDPOINT ............ OK',
  'CONSOLE READY. HUNT. THROTTLE. DEFEND.',
];

const BOOT_STEP_MS = 260;

const initialMetrics = {
  requestsPerSecond: 0,
  allowedCount: 0,
  deniedCount: 0,
  allowRatio: 0,
};

const initialChartData = {
  labels: [],
  requestsPerSecond: [],
};

/* Derives traffic pressure level from the live allow ratio — pure cosplay, real data:
   healthy traffic keeps pressure low, brutal throttling pushes it toward critical. */
function pressureFor(allowRatio, hasData) {
  if (!hasData) return { level: 5, label: 'STANDBY', cls: 'status-amber' };
  if (allowRatio >= 99) return { level: 5, label: 'ALL CLEAR', cls: 'status-green' };
  if (allowRatio >= 90) return { level: 4, label: 'NOMINAL', cls: 'status-green' };
  if (allowRatio >= 75) return { level: 3, label: 'ELEVATED', cls: 'status-blue' };
  if (allowRatio >= 50) return { level: 2, label: 'HEAVY THROTTLE', cls: 'status-amber' };
  return { level: 1, label: 'CRITICAL', cls: 'status-red' };
}

function BootSequence({ onDone }) {
  const [shown, setShown] = useState(0);

  useEffect(() => {
    if (shown >= BOOT_LINES.length) {
      const t = setTimeout(onDone, 420);
      return () => clearTimeout(t);
    }
    const t = setTimeout(() => setShown((s) => s + 1), BOOT_STEP_MS);
    return () => clearTimeout(t);
  }, [shown, onDone]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-void">
      <div className="w-full max-w-xl px-6">
        <div className="mb-6 flex items-end justify-between">
          <h1 className="display-heading text-5xl leading-none">THROTTLE<span className="text-sig-blue">GATE</span></h1>
          <p className="section-label">EST. 2026</p>
        </div>
        <div className="tactical-panel p-5">
          {BOOT_LINES.slice(0, shown).map((line, i) => (
            <p key={line} className="font-mono text-xs text-muted">
              <span className="text-sig-green">&gt;</span> {line}
              {i === shown - 1 && shown < BOOT_LINES.length && (
                <span className="ml-1 inline-block h-3 w-2 translate-y-px animate-blink bg-sig-green" />
              )}
            </p>
          ))}
          {shown === 0 && <p className="font-mono text-xs text-faint">booting…</p>}
        </div>
      </div>
    </div>
  );
}

function App() {
  const [metrics, setMetrics] = useState(initialMetrics);
  const [chartData, setChartData] = useState(initialChartData);
  const [error, setError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState(null);
  const [booted, setBooted] = useState(false);

  // Console control state
  const [paused, setPaused] = useState(false);
  const [pollSecs, setPollSecs] = useState(5);
  const [refreshing, setRefreshing] = useState(false);
  const [eventLog, setEventLog] = useState([]); // rolling ops log of console actions

  const logEvent = useCallback((msg) => {
    const entry = `${new Date().toLocaleTimeString()}  ${msg}`;
    setEventLog((prev) => [entry, ...prev].slice(0, 4));
  }, []);

  const fetchMetrics = useCallback(
    async ({ manual = false } = {}) => {
      if (manual) setRefreshing(true);
      try {
        const response = await axios.get(METRICS_URL, { timeout: 4000 });
        const m = response.data || {};

        setMetrics({
          requestsPerSecond: m.requestsPerSecond ?? 0,
          allowedCount: m.allowedCount ?? 0,
          deniedCount: m.deniedCount ?? 0,
          allowRatio: m.allowRatio ?? 0,
        });
        setError(null);
        setLastUpdate(new Date());

        setChartData((prev) => {
          const labels = [...prev.labels, new Date().toLocaleTimeString()].slice(-MAX_DATA_POINTS);
          const requestsPerSecond = [...prev.requestsPerSecond, m.requestsPerSecond ?? 0].slice(-MAX_DATA_POINTS);
          return { labels, requestsPerSecond };
        });
        if (manual) logEvent('MANUAL PULSE — METRICS SYNCED');
      } catch (err) {
        console.error('Error fetching metrics:', err);
        setError(`UPLINK FAILED — ${API_BASE_URL} UNREACHABLE. IS THE SERVICE RUNNING?`);
        setLastUpdate(new Date());
        if (manual) logEvent('MANUAL PULSE — UPLINK FAILED');
      } finally {
        if (manual) setRefreshing(false);
      }
    },
    [logEvent]
  );

  // Polling loop — pauses when paused, restarts when pollSecs changes
  useEffect(() => {
    if (!booted || paused) return undefined;
    fetchMetrics();
    const interval = setInterval(fetchMetrics, pollSecs * 1000);
    return () => clearInterval(interval);
  }, [booted, paused, pollSecs, fetchMetrics]);

  // Console actions
  const handleTogglePause = useCallback(() => {
    setPaused((p) => {
      logEvent(p ? 'LIVE FEED RESUMED' : 'LIVE FEED PAUSED');
      return !p;
    });
  }, [logEvent]);

  const handleCyclePoll = useCallback(() => {
    setPollSecs((s) => {
      const next = POLL_OPTIONS[(POLL_OPTIONS.indexOf(s) + 1) % POLL_OPTIONS.length];
      logEvent(`POLL INTERVAL SET TO ${next}S`);
      return next;
    });
  }, [logEvent]);

  const handleRefresh = useCallback(() => {
    fetchMetrics({ manual: true });
  }, [fetchMetrics]);

  const handleClear = useCallback(() => {
    setChartData(initialChartData);
    logEvent('THROUGHPUT BUFFER PURGED');
  }, [logEvent]);

  const { requestsPerSecond, allowedCount, deniedCount, allowRatio } = metrics;
  const totalRequests = allowedCount + deniedCount;
  const isLive = !error && lastUpdate !== null;
  const pressure = pressureFor(allowRatio, totalRequests > 0);

  const pollLabel = useMemo(() => `POLL ${pollSecs}S`, [pollSecs]);

  if (!booted) return <BootSequence onDone={() => setBooted(true)} />;

  return (
    <div className="scanline-overlay noise-overlay scanline-bar min-h-screen animate-flicker text-muted">
      {/* Top status bar */}
      <header className="sticky top-0 z-20 border-b border-edge bg-void/85 backdrop-blur-xl">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center border border-sig-blue/60 text-sig-blue">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" className="h-4 w-4">
                <path d="M13 2 3 14h7l-1 8 10-12h-7l1-8z" />
              </svg>
            </div>
            <div>
              <h1 className="display-heading text-2xl leading-none">
                THROTTLE<span className="text-sig-blue">GATE</span>
              </h1>
              <p className="section-label mt-0.5">TRAFFIC CONTROL // ADMIN CONSOLE</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={handleTogglePause}
              title={paused ? 'Resume live polling' : 'Pause live polling'}
              className={`console-btn ${paused ? 'status-amber' : 'status-green'}`}
            >
              <span className="status-pill-dot" />
              {paused ? 'Paused' : 'Live'}
            </button>
            <button
              type="button"
              onClick={handleCyclePoll}
              title="Cycle poll interval (2s / 5s / 10s / 30s)"
              className="console-btn status-blue"
            >
              <span className="status-pill-dot" />
              {pollLabel}
            </button>
            <button
              type="button"
              onClick={handleRefresh}
              disabled={refreshing}
              title="Fetch metrics now"
              className="console-btn status-blue"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className={`h-3 w-3 ${refreshing ? 'animate-spin' : ''}`}>
                <path d="M21 12a9 9 0 1 1-2.64-6.36" />
                <path d="M21 3v6h-6" />
              </svg>
              {refreshing ? 'Syncing' : 'Sync'}
            </button>
            <div className="hidden text-right sm:block">
              <p className="section-label">LAST SYNC</p>
              <p className="font-mono text-xs font-medium tabular-nums text-cream">
                {lastUpdate ? lastUpdate.toLocaleTimeString() : '—'}
              </p>
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        {/* Traffic pressure strip */}
        <section className="tactical-panel mb-4 flex flex-wrap items-center justify-between gap-3 px-4 py-3 animate-fade-in-up">
          <div className="flex items-center gap-3">
            <span className="section-label">PRESSURE</span>
            <span className="display-heading text-4xl leading-none text-sig-red">{pressure.level}</span>
            <span className={`status-pill ${pressure.cls}`}>
              <span className="status-pill-dot" />
              {pressure.label}
            </span>
            {paused && (
              <span className="status-pill status-amber">
                <span className="status-pill-dot" />
                FEED PAUSED
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            {[5, 4, 3, 2, 1].map((lvl) => (
              <span
                key={lvl}
                aria-hidden="true"
                className={`h-2 w-8 border ${
                  lvl === pressure.level
                    ? lvl <= 2
                      ? 'border-sig-red bg-sig-red/70'
                      : lvl === 3
                        ? 'border-sig-blue bg-sig-blue/70'
                        : 'border-sig-green bg-sig-green/70'
                    : 'border-edge bg-transparent'
                }`}
              />
            ))}
          </div>
        </section>

        {/* Error banner */}
        {error && (
          <div className="tactical-panel mb-4 flex flex-wrap items-center gap-3 border-sig-red/40 px-4 py-3 animate-fade-in-up">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5 shrink-0 text-sig-red">
              <circle cx="12" cy="12" r="10" />
              <path d="M12 8v4M12 16h.01" />
            </svg>
            <div className="font-mono text-xs">
              <p className="font-bold uppercase tracking-wider2 text-sig-red">// CONNECTION LOST</p>
              <p className="mt-0.5 text-cream">{error}</p>
            </div>
            <button
              type="button"
              onClick={handleRefresh}
              className="console-btn status-red ml-auto"
            >
              Re-establish uplink
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
            accent="sig-blue"
            delay={0}
          />
          <MetricCard
            label="Allowed requests"
            value={allowedCount.toLocaleString()}
            sub={`${totalRequests.toLocaleString()} total processed`}
            icon="check"
            accent="green"
            delay={60}
          />
          <MetricCard
            label="Denied requests"
            value={deniedCount.toLocaleString()}
            sub="Returned 429 Too Many Requests"
            icon="denied"
            accent="red"
            delay={120}
          />
          <MetricCard
            label="Allow ratio"
            value={`${allowRatio.toFixed(1)}%`}
            sub="Allowed / total requests"
            icon="ratio"
            accent="cream"
            delay={180}
          />
        </section>

        {/* Charts */}
        <section className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
          <div className="tactical-panel animate-fade-in-up lg:col-span-2" style={{ animationDelay: '120ms' }}>
            <div className="tactical-panel-header">
              <div>
                <h2 className="font-mono text-xs font-bold uppercase tracking-wider2 text-cream">THROUGHPUT</h2>
                <p className="font-mono text-[10px] text-faint">requests per second — last {MAX_DATA_POINTS} samples</p>
              </div>
              <button
                type="button"
                onClick={handleClear}
                disabled={chartData.labels.length === 0}
                title="Clear the throughput buffer"
                className="console-btn status-amber"
              >
                Purge buffer
              </button>
            </div>
            <div className="h-72 p-4">
              <RpsLineChart labels={chartData.labels} data={chartData.requestsPerSecond} />
            </div>
          </div>

          <div className="tactical-panel animate-fade-in-up" style={{ animationDelay: '180ms' }}>
            <div className="tactical-panel-header">
              <div>
                <h2 className="font-mono text-xs font-bold uppercase tracking-wider2 text-cream">ALLOW VS. DENY</h2>
                <p className="font-mono text-[10px] text-faint">lifetime distribution</p>
              </div>
            </div>
            <div className="p-4">
              <div className="relative h-56">
                <AllowDenyChart allowed={allowedCount} denied={deniedCount} />
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
                  <p className="font-mono text-2xl font-medium tabular-nums text-paper">
                    {totalRequests.toLocaleString()}
                  </p>
                  <p className="section-label mt-1">TOTAL REQUESTS</p>
                </div>
              </div>
              <div className="mt-4 flex items-center justify-around border-t border-edge pt-4">
                <div className="flex items-center gap-2">
                  <span className="h-2.5 w-2.5 bg-sig-green" />
                  <div>
                    <p className="font-mono text-sm font-medium tabular-nums text-paper">{allowedCount.toLocaleString()}</p>
                    <p className="font-mono text-[10px] text-faint">ALLOWED</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className="h-2.5 w-2.5 bg-sig-red" />
                  <div>
                    <p className="font-mono text-sm font-medium tabular-nums text-paper">{deniedCount.toLocaleString()}</p>
                    <p className="font-mono text-[10px] text-faint">DENIED</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Ops log */}
        <section className="tactical-panel mt-4 animate-fade-in-up px-4 py-3" style={{ animationDelay: '240ms' }}>
          <div className="flex items-center justify-between">
            <p className="section-label">CONSOLE OPS LOG</p>
            <p className="font-mono text-[10px] text-faint/70">last {eventLog.length} actions</p>
          </div>
          <div className="mt-2 min-h-[3.75rem] font-mono text-[11px] leading-relaxed text-faint">
            {eventLog.length === 0 ? (
              <p>&gt; awaiting operator input…</p>
            ) : (
              eventLog.map((entry) => (
                <p key={entry}>
                  <span className="text-sig-green">&gt;</span> {entry}
                </p>
              ))
            )}
          </div>
        </section>

        {/* Footer */}
        <footer className="mt-8 flex flex-col items-center gap-1 border-t border-edge pt-5 text-center">
          <p className="font-mono text-[10px] text-faint/70">
            AUTO-REFRESH {paused ? 'PAUSED' : `${pollSecs}S`}
          </p>
          <p className="display-heading mt-1 text-xl text-faint">HUNT. THROTTLE. DEFEND.</p>
        </footer>
      </main>
    </div>
  );
}

export default App;
