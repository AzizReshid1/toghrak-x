/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#0b4bb3',
          dark:    '#093d96',
        },
      },
    },
  },
  plugins: [],
};
