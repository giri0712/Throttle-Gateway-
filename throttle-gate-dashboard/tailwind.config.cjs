/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        // RECON 2026 font stack: Bebas Neue display, IBM Plex Mono terminal,
        // Michroma wide techno labels, Geist Mono fallback
        sans: ['"Geist Mono"', '"IBM Plex Mono"', 'Consolas', 'monospace'],
        mono: ['"IBM Plex Mono"', '"Geist Mono"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'Consolas', 'monospace'],
        display: ['"Bebas Neue"', '"Arial Black"', 'Impact', 'sans-serif'],
        techno: ['Michroma', '"Arial Black"', 'sans-serif'],
      },
      colors: {
        // Exact palette lifted from reconhq.tech
        void: '#080810',      // page background
        dark: '#0d0d18',      // deep panels
        panel: '#141426',     // card surfaces
        edge: '#2a2a3d',      // dim borders
        soft: '#3a3a54',      // active borders
        faint: '#7a738f',     // dim text
        muted: '#b4adc8',     // body text
        cream: '#d8d3e6',     // secondary headings
        paper: '#f5f4f9',     // primary headings
        sig: {
          green: '#22c55e',   // allowed / live
          red: '#f25555',     // denied / 429
          blue: '#8fb0ff',    // info / accent
        },
      },
      letterSpacing: {
        wider2: '0.2em',
      },
      keyframes: {
        'fade-in-up': {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'pulse-soft': {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.35' },
        },
        blink: {
          '0%, 49%': { opacity: '1' },
          '50%, 100%': { opacity: '0' },
        },
        flicker: {
          '0%, 100%': { opacity: '1' },
          '92%': { opacity: '1' },
          '93%': { opacity: '0.6' },
          '94%': { opacity: '1' },
          '97%': { opacity: '0.8' },
          '98%': { opacity: '1' },
        },
        'scanline-sweep': {
          '0%': { transform: 'translateY(-4px)' },
          '100%': { transform: 'translateY(100vh)' },
        },
      },
      animation: {
        'fade-in-up': 'fade-in-up 0.4s ease-out both',
        'pulse-soft': 'pulse-soft 2s ease-in-out infinite',
        blink: 'blink 1s step-end infinite',
        flicker: 'flicker 6s linear infinite',
        'scanline-sweep': 'scanline-sweep 6s linear infinite',
      },
    },
  },
  plugins: [],
};
