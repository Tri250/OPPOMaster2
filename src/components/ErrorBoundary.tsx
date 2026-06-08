import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  resetErrorBoundary = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div
          className="flex flex-col items-center justify-center h-full px-8 animate-liquid-fade"
          style={{ background: 'var(--color-bg-primary)' }}
        >
          <div
            className="w-20 h-20 rounded-3xl flex items-center justify-center mb-6"
            style={{
              background: 'rgba(255, 107, 53, 0.15)',
              border: '1px solid rgba(255, 107, 53, 0.2)'
            }}
          >
            <AlertTriangle size={40} style={{ color: '#FF6B35' }} />
          </div>

          <h2
            className="font-bold text-xl mb-3"
            style={{ color: 'var(--color-text-primary)' }}
          >
            页面出错了
          </h2>

          <p
            className="text-sm text-center mb-2"
            style={{ color: 'var(--color-text-secondary)' }}
          >
            {this.state.error?.message || '发生了未知错误，请稍后重试'}
          </p>

          <p
            className="text-xs text-center mb-8"
            style={{ color: 'var(--color-text-tertiary)' }}
          >
            请尝试刷新页面或返回上一页
          </p>

          <button
            onClick={this.resetErrorBoundary}
            className="flex items-center gap-2 px-6 py-3 rounded-xl font-semibold text-sm transition-all active:scale-95"
            style={{
              background: '#FF6B35',
              color: '#FFFFFF'
            }}
          >
            <RefreshCw size={16} />
            重新加载
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
