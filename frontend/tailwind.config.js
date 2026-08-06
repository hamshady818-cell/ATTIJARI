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
          'secondary-hover': 'var(--color-secondary-hover)',
          'secondary-light': 'var(--color-secondary-light)',
          bg: 'var(--color-bg-main)',
          surface: 'var(--color-bg-surface)',
          alt: 'var(--color-bg-alt)',
          border: 'var(--color-border)',
          'border-dark': 'var(--color-border-dark)',
          text: 'var(--color-text-main)',
          muted: 'var(--color-text-muted)',
          // Status colors
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
        xs: '4px',
        sm: '6px',
        DEFAULT: '8px',
        md: '8px',
        lg: '12px',
        xl: '16px',
        '2xl': '20px',
        full: '9999px'
      },
      boxShadow: {
        flat: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
        crisp: '0 0 0 1px var(--color-border), 0 2px 4px 0 rgba(0, 0, 0, 0.03)',
        card: '0 2px 8px 0 rgba(0, 0, 0, 0.04), 0 0 0 1px var(--color-border)',
        popover: '0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04), 0 0 0 1px var(--color-border)',
        modal: '0 25px 50px -12px rgba(0, 0, 0, 0.15), 0 0 0 1px var(--color-border)'
      }
    },
  },
  plugins: [],
}
