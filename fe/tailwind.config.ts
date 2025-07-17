import type { Config } from "tailwindcss";

export default {
    content: [
        "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
        "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
        "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    ],
    theme: {
        extend: {
            colors: {
                'deep-charcoal': '#1a1a1a',
                'slate-gray': '#2d3748',
                'luxury-gold': '#f6d55c',
                'warm-gray': '#718096',
                'pure-white': '#ffffff',
                'off-white': '#f7fafc',
                'success': '#48bb78',
                'warning': '#ed8936',
                'error': '#f56565',
                'info': '#4299e1',
                background: "hsl(var(--background))",
                foreground: "hsl(var(--foreground))",
                card: {
                    DEFAULT: "hsl(var(--card))",
                    foreground: "hsl(var(--card-foreground))",
                },
                popover: {
                    DEFAULT: "hsl(var(--popover))",
                    foreground: "hsl(var(--popover-foreground))",
                },
                primary: {
                    DEFAULT: "hsl(var(--primary))",
                    foreground: "hsl(var(--primary-foreground))",
                },
                secondary: {
                    DEFAULT: "hsl(var(--secondary))",
                    foreground: "hsl(var(--secondary-foreground))",
                },
                muted: {
                    DEFAULT: "hsl(var(--muted))",
                    foreground: "hsl(var(--muted-foreground))",
                },
                accent: {
                    DEFAULT: "hsl(var(--accent))",
                    foreground: "hsl(var(--accent-foreground))",
                },
                destructive: {
                    DEFAULT: "hsl(var(--destructive))",
                    foreground: "hsl(var(--destructive-foreground))",
                },
                border: "hsl(var(--border))",
                input: "hsl(var(--input))",
                ring: "hsl(var(--ring))",
                chart: {
                    "1": "hsl(var(--chart-1))",
                    "2": "hsl(var(--chart-2))",
                    "3": "hsl(var(--chart-3))",
                    "4": "hsl(var(--chart-4))",
                    "5": "hsl(var(--chart-5))",
                },
            },
            fontFamily: {
                'sans': ['Inter', 'Segoe UI', 'sans-serif'],
            },
            boxShadow: {
                'subtle': '0 1px 3px rgba(0,0,0,0.12)',
                'medium': '0 4px 6px rgba(0,0,0,0.07)',
                'strong': '0 10px 15px rgba(0,0,0,0.1)',
                'luxury': '0 8px 25px rgba(246, 213, 92, 0.3)',
                'xs': '0 1px 2px 0 rgb(0 0 0 / 0.05)',
            },
            borderRadius: {
                lg: "var(--radius)",
                md: "calc(var(--radius) - 2px)",
                sm: "calc(var(--radius) - 4px)",
            },
            animation: {
                'fade-in': 'fadeIn 0.6s ease-out',
                'pulse-glow': 'pulse-glow 2s infinite',
            },
        },
    },
    plugins: [require("tailwindcss-animate")],
} satisfies Config;