import React from 'react';

/**
 * 预设卡片骨架屏 - 模拟预设卡片加载状态
 * 2列网格布局 + shimmer 动画
 */
export const SkeletonPresetCard: React.FC<{ count?: number }> = ({ count = 6 }) => {
  return (
    <div className="grid grid-cols-2 gap-4">
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className="animate-liquid-slide-up"
          style={{
            animationDelay: `${index * 60}ms`,
            animationFillMode: 'both',
            background: 'rgba(255, 255, 255, 0.05)',
            borderRadius: '20px',
            overflow: 'hidden',
            border: '1px solid rgba(255, 255, 255, 0.08)'
          }}
        >
          {/* 图片占位 */}
          <div
            className="aspect-[3/4] animate-shimmer"
            style={{ background: 'rgba(255, 255, 255, 0.03)' }}
          />

          {/* 底部信息占位 */}
          <div className="p-4">
            <div
              className="h-4 rounded-md mb-2 animate-shimmer"
              style={{ background: 'rgba(255, 255, 255, 0.06)', width: '70%' }}
            />
            <div
              className="h-3 rounded-md animate-shimmer"
              style={{ background: 'rgba(255, 255, 255, 0.04)', width: '45%' }}
            />
          </div>
        </div>
      ))}
    </div>
  );
};

/**
 * 功能卡片骨架屏 - 模拟功能卡片加载状态
 * 圆形图标占位 + 标题行 + 副标题行
 */
export const SkeletonFeatureCard: React.FC<{ count?: number }> = ({ count = 4 }) => {
  return (
    <div className="space-y-4">
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className="animate-liquid-slide-up"
          style={{
            animationDelay: `${index * 80}ms`,
            animationFillMode: 'both',
            background: 'rgba(255, 255, 255, 0.05)',
            borderRadius: '20px',
            padding: '16px',
            border: '1px solid rgba(255, 255, 255, 0.08)'
          }}
        >
          <div className="flex items-start justify-between mb-4">
            {/* 圆形图标占位 */}
            <div
              className="w-14 h-14 rounded-2xl animate-shimmer"
              style={{ background: 'rgba(255, 255, 255, 0.06)' }}
            />
            {/* 箭头占位 */}
            <div
              className="w-10 h-10 rounded-full animate-shimmer"
              style={{ background: 'rgba(255, 255, 255, 0.04)' }}
            />
          </div>

          {/* 标题行 */}
          <div
            className="h-5 rounded-md mb-2 animate-shimmer"
            style={{ background: 'rgba(255, 255, 255, 0.06)', width: '50%' }}
          />

          {/* 副标题行 */}
          <div
            className="h-3.5 rounded-md animate-shimmer"
            style={{ background: 'rgba(255, 255, 255, 0.04)', width: '75%' }}
          />
        </div>
      ))}
    </div>
  );
};
