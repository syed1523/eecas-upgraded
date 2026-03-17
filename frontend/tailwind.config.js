/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                dark: {
                    base: '#080B12',
                    surface: '#121721',
                    highlight: '#1A1F2E',
                },
                neon: {
                    blue: '#00f3ff',     // Cyan/Electric Blue
                    purple: '#bd00ff',   // Deep Violet/Purple
                }
            },
            fontFamily: {
                sans: ['Inter', 'sans-serif'],
            },
            backgroundImage: {
                'mesh-gradient': 'radial-gradient(circle at 50% 50%, #121721 0%, #080B12 100%)',
                'glass-gradient': 'linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05))',
                'primary-gradient': 'linear-gradient(135deg, #00f3ff 0%, #bd00ff 100%)',
            },
            boxShadow: {
                'neon': '0 0 10px rgba(0, 243, 255, 0.5), 0 0 20px rgba(0, 243, 255, 0.3)',
                'glass': '0 4px 30px rgba(0, 0, 0, 0.1)',
            },
            backdropBlur: {
                xs: '2px',
            }
        },
    },
    plugins: [],
}
