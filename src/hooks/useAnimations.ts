/**
 * 哈苏大师动画系统
 * 统一管理所有动画效果
 */

import { useEffect, useState, useRef, useCallback } from 'react';

// 哈苏品牌色
export const HASSELBLAD_ORANGE = '#FF6B35';

/**
 * 首次加载动画Hook
 * @param delay 延迟时间(ms)
 * @param duration 持续时间(ms)
 */
export function useFadeInUp(delay: number = 0, duration: number = 400) {
  const [isVisible, setIsVisible] = useState(false);
  const [hasAnimated, setHasAnimated] = useState(false);

  useEffect(() => {
    if (hasAnimated) return;
    
    const timer = setTimeout(() => {
      setIsVisible(true);
      setHasAnimated(true);
    }, delay);

    return () => clearTimeout(timer);
  }, [delay, hasAnimated]);

  return {
    isVisible,
    style: {
      opacity: isVisible ? 1 : 0,
      transform: isVisible ? 'translateY(0)' : 'translateY(20px)',
      transition: `opacity ${duration}ms ease-out, transform ${duration}ms ease-out`,
    },
  };
}

/**
 * 交错动画Hook - 用于列表项依次动画
 * @param index 项目索引
 * @param baseDelay 基础延迟(ms)
 * @param staggerDelay 每项延迟(ms)
 */
export function useStaggeredFadeIn(index: number, baseDelay: number = 0, staggerDelay: number = 50) {
  const delay = baseDelay + index * staggerDelay;
  return useFadeInUp(delay, 400);
}

/**
 * 滚动可视区动画Hook
 * @param threshold 可视阈值(0-1)
 */
export function useScrollReveal(threshold: number = 0.1) {
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.unobserve(element);
        }
      },
      { threshold }
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, [threshold]);

  return {
    ref,
    isVisible,
    style: {
      opacity: isVisible ? 1 : 0,
      transform: isVisible ? 'translateY(0)' : 'translateY(20px)',
      transition: 'opacity 400ms ease-out, transform 400ms ease-out',
    },
  };
}

/**
 * 心形弹跳动画Hook
 */
export function useHeartBounce() {
  const [isAnimating, setIsAnimating] = useState(false);
  const [showFlash, setShowFlash] = useState(false);

  const trigger = useCallback(() => {
    setIsAnimating(true);
    setShowFlash(true);
    
    setTimeout(() => {
      setIsAnimating(false);
    }, 300);
    
    setTimeout(() => {
      setShowFlash(false);
    }, 200);
  }, []);

  return {
    isAnimating,
    showFlash,
    trigger,
    style: {
      transform: isAnimating ? 'scale(1.3)' : 'scale(1)',
      transition: 'transform 150ms ease-out',
    },
  };
}

/**
 * Tab指示器动画Hook
 */
export function useTabIndicator(tabCount: number) {
  const [indicatorPosition, setIndicatorPosition] = useState(0);
  const [indicatorWidth, setIndicatorWidth] = useState(0);
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);

  const updateIndicator = useCallback((index: number) => {
    const tab = tabRefs.current[index];
    if (tab) {
      const rect = tab.getBoundingClientRect();
      const parentRect = tab.parentElement?.getBoundingClientRect();
      if (parentRect) {
        setIndicatorPosition(tab.offsetLeft + 8);
        setIndicatorWidth(rect.width - 16);
      }
    }
  }, []);

  useEffect(() => {
    updateIndicator(0);
  }, [updateIndicator]);

  return {
    tabRefs,
    indicatorStyle: {
      left: indicatorPosition,
      width: indicatorWidth,
      transition: 'left 300ms cubic-bezier(0.4, 0, 0.2, 1), width 300ms cubic-bezier(0.4, 0, 2, 1)',
    },
    updateIndicator,
  };
}

/**
 * 下拉刷新动画Hook
 */
export function usePullToRefresh(onRefresh: () => Promise<void> | void) {
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [pullDistance, setPullDistance] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const startY = useRef(0);
  const isPulling = useRef(false);

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    const container = containerRef.current;
    if (container && container.scrollTop === 0) {
      startY.current = e.touches[0].clientY;
      isPulling.current = true;
    }
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (!isPulling.current || isRefreshing) return;
    
    const currentY = e.touches[0].clientY;
    const diff = currentY - startY.current;
    
    if (diff > 0) {
      setPullDistance(Math.min(diff * 0.5, 80));
    }
  }, [isRefreshing]);

  const handleTouchEnd = useCallback(async () => {
    if (!isPulling.current) return;
    isPulling.current = false;

    if (pullDistance >= 60) {
      setIsRefreshing(true);
      await onRefresh();
      setIsRefreshing(false);
    }
    
    setPullDistance(0);
  }, [pullDistance, onRefresh]);

  return {
    containerRef,
    isRefreshing,
    pullDistance,
    handlers: {
      onTouchStart: handleTouchStart,
      onTouchMove: handleTouchMove,
      onTouchEnd: handleTouchEnd,
    },
    spinnerStyle: {
      opacity: isRefreshing || pullDistance > 0 ? 1 : 0,
      transform: `rotate(${isRefreshing ? 360 : pullDistance * 3}deg)`,
      transition: isRefreshing ? 'transform 0.5s linear infinite' : 'none',
    },
  };
}

/**
 * CSS动画关键帧定义
 */
export const animationKeyframes = `
  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @keyframes heartBounce {
    0% { transform: scale(1); }
    50% { transform: scale(1.3); }
    100% { transform: scale(1); }
  }

  @keyframes heartFlash {
    0% {
      box-shadow: 0 0 0 0 rgba(255, 107, 53, 0.7);
    }
    100% {
      box-shadow: 0 0 0 15px rgba(255, 107, 53, 0);
    }
  }

  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  @keyframes slideInRight {
    from {
      opacity: 0;
      transform: translateX(20px);
    }
    to {
      opacity: 1;
      transform: translateX(0);
    }
  }

  @keyframes scaleIn {
    from {
      opacity: 0;
      transform: scale(0.9);
    }
    to {
      opacity: 1;
      transform: scale(1);
    }
  }
`;

/**
 * 动画类名
 */
export const animationClasses = {
  fadeInUp: 'animate-[fadeInUp_400ms_ease-out_forwards]',
  heartBounce: 'animate-[heartBounce_300ms_ease-out]',
  heartFlash: 'animate-[heartFlash_300ms_ease-out]',
  spin: 'animate-spin',
  pulse: 'animate-pulse',
  slideInRight: 'animate-[slideInRight_400ms_ease-out_forwards]',
  scaleIn: 'animate-[scaleIn_300ms_ease-out_forwards]',
};