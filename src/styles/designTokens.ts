/**
 * OMaster ColorOS 16 设计令牌
 * 统一 UI/UX 风格：液态玻璃、摄影优先、移动优先
 */

export const colors = {
  // 背景
  background: '#0A0A0A',
  surface: '#1A1A1A',
  elevated: '#222222',

  // 玻璃态
  glass: 'rgba(255, 255, 255, 0.08)',
  glassStrong: 'rgba(255, 255, 255, 0.12)',
  glassBorder: 'rgba(255, 255, 255, 0.10)',
  glassBorderHover: 'rgba(255, 255, 255, 0.18)',

  // 强调色（哈苏橙）
  accent: '#FF6B35',
  accentLight: '#FF8C42',
  accentDark: '#E55A2B',
  accentGlow: 'rgba(255, 107, 53, 0.35)',

  // 文字
  textPrimary: '#FFFFFF',
  textSecondary: 'rgba(255, 255, 255, 0.72)',
  textTertiary: 'rgba(255, 255, 255, 0.45)',
  textMuted: 'rgba(255, 255, 255, 0.30)',

  // 状态
  success: '#4CAF50',
  error: '#EF4444',
  warning: '#F59E0B',
} as const;

export const typography = {
  fontFamily:
    '-apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Helvetica, Arial, sans-serif',
  sizes: {
    hero: '28px',
    h1: '22px',
    h2: '18px',
    h3: '16px',
    body: '14px',
    small: '12px',
    xs: '10px',
    micro: '9px',
  },
  weights: {
    regular: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
  },
  lineHeights: {
    tight: 1.2,
    normal: 1.5,
    relaxed: 1.65,
  },
} as const;

export const spacing = {
  xs: '4px',
  sm: '8px',
  md: '12px',
  lg: '16px',
  xl: '20px',
  xxl: '24px',
  xxxl: '32px',
} as const;

export const radius = {
  sm: '8px',
  md: '12px',
  lg: '16px',
  xl: '20px',
  xxl: '24px',
  full: '9999px',
} as const;

export const shadows = {
  soft: '0 4px 24px rgba(0, 0, 0, 0.25)',
  medium: '0 8px 32px rgba(0, 0, 0, 0.35)',
  glass: '0 8px 32px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.08)',
  glow: `0 0 24px ${colors.accentGlow}`,
  innerHighlight: 'inset 0 1px 0 rgba(255, 255, 255, 0.10)',
} as const;

export const animation = {
  duration: {
    fast: '150ms',
    normal: '250ms',
    slow: '350ms',
    slower: '500ms',
  },
  easing: {
    default: 'cubic-bezier(0.4, 0, 0.2, 1)',
    spring: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
    smooth: 'cubic-bezier(0.22, 1, 0.36, 1)',
    easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
  },
} as const;

export const zIndex = {
  base: 0,
  frame: 10,
  nav: 40,
  modal: 50,
  overlay: 60,
} as const;

/** 导出完整令牌对象，便于一次性引用 */
export const tokens = {
  colors,
  typography,
  spacing,
  radius,
  shadows,
  animation,
  zIndex,
} as const;

export default tokens;
