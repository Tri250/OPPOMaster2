import type { Config } from 'tailwindcss';
import { colors, typography, spacing, radius, shadows, animation } from './src/styles/designTokens';

const config: Config = {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    container: {
      center: true,
    },
    extend: {
      colors: {
        master: {
          bg: colors.background,
          surface: colors.surface,
          elevated: colors.elevated,
          glass: colors.glass,
          'glass-strong': colors.glassStrong,
          'glass-border': colors.glassBorder,
          'glass-border-hover': colors.glassBorderHover,
          accent: colors.accent,
          'accent-light': colors.accentLight,
          'accent-dark': colors.accentDark,
          'text-primary': colors.textPrimary,
          'text-secondary': colors.textSecondary,
          'text-tertiary': colors.textTertiary,
          'text-muted': colors.textMuted,
        },
      },
      fontFamily: {
        sans: [typography.fontFamily],
      },
      fontSize: {
        hero: typography.sizes.hero,
        h1: typography.sizes.h1,
        h2: typography.sizes.h2,
        h3: typography.sizes.h3,
        body: typography.sizes.body,
        small: typography.sizes.small,
        xs: typography.sizes.xs,
        micro: typography.sizes.micro,
      },
      fontWeight: {
        regular: typography.weights.regular,
        medium: typography.weights.medium,
        semibold: typography.weights.semibold,
        bold: typography.weights.bold,
      },
      lineHeight: {
        tight: typography.lineHeights.tight,
        relaxed: typography.lineHeights.relaxed,
      },
      spacing: {
        xs: spacing.xs,
        sm: spacing.sm,
        md: spacing.md,
        lg: spacing.lg,
        xl: spacing.xl,
        xxl: spacing.xxl,
        xxxl: spacing.xxxl,
      },
      borderRadius: {
        sm: radius.sm,
        md: radius.md,
        lg: radius.lg,
        xl: radius.xl,
        xxl: radius.xxl,
      },
      boxShadow: {
        soft: shadows.soft,
        medium: shadows.medium,
        glass: shadows.glass,
        glow: shadows.glow,
        'inner-highlight': shadows.innerHighlight,
      },
      transitionDuration: {
        fast: animation.duration.fast,
        normal: animation.duration.normal,
        slow: animation.duration.slow,
        slower: animation.duration.slower,
      },
      transitionTimingFunction: {
        spring: animation.easing.spring,
        smooth: animation.easing.smooth,
        'ease-out-custom': animation.easing.easeOut,
      },
      backdropBlur: {
        glass: '20px',
      },
      keyframes: {
        'fade-in-up': {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(0.96)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
      },
      animation: {
        'fade-in-up': 'fade-in-up 0.35s cubic-bezier(0.22, 1, 0.36, 1) forwards',
        'scale-in': 'scale-in 0.3s cubic-bezier(0.22, 1, 0.36, 1) forwards',
        shimmer: 'shimmer 2.5s linear infinite',
      },
    },
  },
  plugins: [],
};

export default config;
