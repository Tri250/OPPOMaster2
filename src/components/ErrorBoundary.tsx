import React, { Component, ErrorInfo, ReactNode } from 'react';
import { tokens } from '../styles/designTokens';
import { AlertTriangle, RotateCcw } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

/**
 * 全局错误边界
 * 捕获子组件渲染异常，避免整个应用白屏，提升 Android 端稳定性。
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div
          className="min-h-screen flex flex-col items-center justify-center px-lg"
          style={{ background: tokens.colors.background, fontFamily: tokens.typography.fontFamily }}
        >
          <div
            className="w-full max-w-sm p-6 rounded-2xl border backdrop-blur-glass"
            style={{
              background: tokens.colors.glass,
              borderColor: tokens.colors.glassBorder,
              boxShadow: tokens.shadows.glass,
            }}
          >
            <div
              className="w-14 h-14 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ background: `${tokens.colors.error}20` }}
            >
              <AlertTriangle size={28} style={{ color: tokens.colors.error }} />
            </div>
            <h2 className="text-h2 font-bold text-master-text-primary text-center mb-2">
              出错了
            </h2>
            <p className="text-body text-master-text-secondary text-center mb-2">
              应用遇到意外问题，请尝试恢复。
            </p>
            {this.state.error && (
              <p className="text-small text-master-text-muted text-center mb-6 break-all">
                {this.state.error.message}
              </p>
            )}
            <button
              onClick={this.handleReset}
              className="w-full py-3 rounded-xl font-semibold text-body flex items-center justify-center gap-2 transition-all duration-normal active:scale-95"
              style={{
                background: tokens.colors.accent,
                color: tokens.colors.textPrimary,
                transitionTimingFunction: tokens.animation.easing.spring,
              }}
            >
              <RotateCcw size={18} />
              重新加载
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
