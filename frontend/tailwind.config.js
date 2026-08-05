/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          primary: 'var(--color-primary)',
          'primary-hover': 'var(--color-primary-hover)',
          'primary-light': 'var(--color-primary-light)',
          secondary: 'var(--color-secondary)',
          bg: 'var(--color-bg-main)',
          surface: 'var(--color-bg-surface)',
          alt: 'var(--color-bg-alt)',
          border: 'var(--color-border)',
          'border-dark': 'var(--color-border-dark)',
          text: 'var(--color-text-main)',
          muted: 'var(--color-text-muted)',
          // Status colors without blue/gradients
          draft: 'var(--color-status-draft)',
          published: 'var(--color-status-published)',
          archived: 'var(--color-status-archived)',
          trashed: 'var(--color-status-trashed)',
          locked: 'var(--color-status-locked)'
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'Consolas', 'monospace']
      },
      borderRadius: {
        none: '0px',
        sm: '2px',
        DEFAULT: '4px',
        md: '6px',
        lg: '8px'
      },
      boxShadow: {
        flat: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        crisp: '0 0 0 1px var(--color-border)',
        popover: '0 4px 12px 0 rgba(0, 0, 0, 0.08), 0 0 0 1px var(--color-border)'
      }
    },
  },
  plugins: [],
}
