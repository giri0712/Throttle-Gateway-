import React from 'react';

const ICONS = {
  activity: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
      <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
    </svg>
  ),
  check: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
      <circle cx="12" cy="12" r="10" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  ),
  denied: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
      <circle cx="12" cy="12" r="10" />
      <path d="m4.9 4.9 14.2 14.2" />
    </svg>
  ),
  ratio: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5">
      <line x1="19" x2="5" y1="5" y2="19" />
      <circle cx="6.5" cy="6.5" r="2.5" />
      <circle cx="17.5" cy="17.5" r="2.5" />
    </svg>
  )
};

const ACCENTS = {
  blue: {
    text: 'text-sig-blue',
    border: 'border-sig-blue/50',
    bar: 'bg-sig-blue',
  },
  green: {
    text: 'text-sig-green',
    border: 'border-sig-green/50',
    bar: 'bg-sig-green',
  },
  red: {
    text: 'text-sig-red',
    border: 'border-sig-red/50',
    bar: 'bg-sig-red',
  },
  cream: {
    text: 'text-cream',
    border: 'border-cream/40',
    bar: 'bg-cream',
  },
};

export default function MetricCard({ label, value, sub, icon = 'activity', accent = 'blue', delay = 0 }) {
  const a = ACCENTS[accent] || ACCENTS.blue;

  return (
    <div
      className="tactical-panel group relative overflow-hidden p-4 transition duration-300 hover:border-soft hover:bg-panel animate-fade-in-up"
      style={{ animationDelay: `${delay}ms` }}
    >
      {/* Top signal bar — brightens on hover */}
      <span className={`absolute inset-x-0 top-0 h-0.5 opacity-40 transition duration-300 group-hover:opacity-100 ${a.bar}`} />

      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="section-label">{label}</p>
          <p className="stat-value mt-2">{value}</p>
          {sub && <p className="mt-1 font-mono text-[10px] text-faint">{sub}</p>}
        </div>
        <div className={`shrink-0 border bg-dark/60 p-2 ${a.border} ${a.text}`}>
          {ICONS[icon] || ICONS.activity}
        </div>
      </div>
    </div>
  );
}
