import React from 'react';
import { Heart, Sparkles, Crown, Download, Star, Zap, Check } from 'lucide-react';

interface PresetCardProps {
  preset: {
    id: string;
    name: string;
    coverPath: string;
    author: string;
    brand?: string;
    isNew: boolean;
    isHncs: boolean;
  };
  isFavorite: boolean;
  onToggleFavorite: (id: string) => void;
  onApply?: () => void;
  variant?: 'compact' | 'full';
  index: number;
}

const PresetCard: React.FC<PresetCardProps> = React.memo(({
  preset,
  isFavorite,
  onToggleFavorite,
  onApply,
  variant = 'compact',
  index,
}) => {
  const getImageHeight = (idx: number) => {
    switch (idx % 3) {
      case 0:
        return 'aspect-[3/4]';
      case 1:
        return 'aspect-square';
      default:
        return 'aspect-[4/5]';
    }
  };

  if (variant === 'full') {
    return (
      <div
        className="group relative animate-liquid-slide-up"
        style={{
          animationDelay: `${index * 60}ms`,
          animationFillMode: 'both',
          background: 'rgba(255, 255, 255, 0.05)',
          borderRadius: '20px',
          overflow: 'hidden',
          border: '1px solid rgba(255, 255, 255, 0.08)'
        }}
        role="article"
      >
        {/* 图片 */}
        <div className="aspect-[4/3] overflow-hidden rounded-t-2xl">
          <img
            src={preset.coverPath}
            alt={preset.name}
            className="w-full h-full object-cover transition-liquid group-hover:scale-110"
            loading="lazy"
          />
        </div>

        {/* NEW徽章 - 白色边框透明背景 */}
        {preset.isNew && !preset.isHncs && (
          <div
            className="absolute top-3 left-3 flex items-center gap-1 px-2 py-1 rounded-md"
            style={{
              background: 'transparent',
              border: '1px solid rgba(255, 255, 255, 0.5)'
            }}
          >
            <Sparkles size={12} style={{ color: '#FFFFFF' }} />
            <span className="text-xs font-bold" style={{ color: '#FFFFFF' }}>NEW</span>
          </div>
        )}

        {/* HNCS徽章 - 橙色背景 */}
        {preset.isHncs && (
          <div
            className="absolute top-3 left-3 flex items-center gap-1 px-2 py-1 rounded-md"
            style={{ background: 'rgba(255, 107, 53, 0.9)' }}
          >
            <Crown size={12} style={{ color: '#FFFFFF' }} />
            <span className="text-xs font-bold" style={{ color: '#FFFFFF' }}>HNCS</span>
          </div>
        )}

        {/* 收藏按钮 */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            onToggleFavorite(preset.id);
          }}
          aria-label={isFavorite ? '取消收藏' : '添加收藏'}
          aria-pressed={isFavorite}
          className="absolute top-3 right-3 p-2.5 rounded-full z-20 transition-spring-soft ripple-container"
          style={{
            background: isFavorite
              ? 'rgba(244, 67, 54, 0.2)'
              : 'rgba(0, 0, 0, 0.5)',
            backdropFilter: 'blur(12px)'
          }}
        >
          <Heart
            size={18}
            style={{
              color: isFavorite ? '#F44336' : '#FFFFFF',
              fill: isFavorite ? '#F44336' : 'transparent'
            }}
          />
        </button>

        {/* 内容区 */}
        <div className="p-4">
          <h3
            className="font-bold text-base mb-1 truncate"
            style={{ color: '#FFFFFF' }}
          >
            {preset.name}
          </h3>
          <div className="flex items-center gap-2 mb-3">
            <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>
              {preset.author}
            </span>
            {preset.isHncs && (
              <>
                <Check size={12} style={{ color: '#FF6B35' }} />
                <span className="text-xs font-medium" style={{ color: '#FF6B35' }}>
                  HNCS认证
                </span>
              </>
            )}
          </div>

          {/* 统计信息 */}
          <div className="flex items-center gap-4 mb-3">
            <div className="flex items-center gap-1">
              <Star
                size={12}
                style={{ color: '#FFD700' }}
                className="fill-yellow-400"
              />
              <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>
                4.{index + 7}
              </span>
            </div>
            <div className="flex items-center gap-1">
              <Download size={12} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
              <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>
                {(index + 1) * 3.5}w
              </span>
            </div>
          </div>

          {/* 应用按钮 - 橙色实心 */}
          <button
            onClick={onApply}
            aria-label="应用参数"
            className="w-full py-3 rounded-xl font-semibold text-sm flex items-center justify-center gap-2 transition-all active:scale-95"
            style={{
              background: '#FF6B35',
              color: '#FFFFFF'
            }}
          >
            <Sparkles size={16} />
            应用参数
          </button>
        </div>
      </div>
    );
  }

  // compact variant - 首页风格
  return (
    <div
      className={`group relative ${getImageHeight(index)} animate-liquid-slide-up`}
      style={{
        animationDelay: `${index * 60}ms`,
        animationFillMode: 'both',
        background: 'rgba(255, 255, 255, 0.05)',
        borderRadius: '20px',
        overflow: 'hidden',
        border: '1px solid rgba(255, 255, 255, 0.08)'
      }}
      role="article"
    >
      {/* 图片 */}
      <img
        src={preset.coverPath}
        alt={preset.name}
        className="w-full h-full object-cover transition-liquid group-hover:scale-105"
        loading="lazy"
      />

      {/* 渐变遮罩 */}
      <div
        className="absolute inset-0"
        style={{
          background: 'linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, rgba(0, 0, 0, 0.2) 50%, transparent 100%)'
        }}
      />

      {/* HNCS徽章 - 橙色背景 */}
      {preset.isHncs && (
        <div
          className="absolute top-3 left-3 flex items-center gap-1 px-2 py-1 rounded-md"
          style={{ background: 'rgba(255, 107, 53, 0.9)' }}
        >
          <Crown size={12} style={{ color: '#FFFFFF' }} />
          <span className="text-xs font-bold" style={{ color: '#FFFFFF' }}>HNCS</span>
        </div>
      )}

      {/* NEW徽章 - 白色边框透明背景 */}
      {preset.isNew && !preset.isHncs && (
        <div
          className="absolute top-3 left-3 flex items-center gap-1 px-2 py-1 rounded-md"
          style={{
            background: 'transparent',
            border: '1px solid rgba(255, 255, 255, 0.5)'
          }}
        >
          <Sparkles size={12} style={{ color: '#FFFFFF' }} />
          <span className="text-xs font-bold" style={{ color: '#FFFFFF' }}>NEW</span>
        </div>
      )}

      {/* 收藏按钮 */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          onToggleFavorite(preset.id);
        }}
        aria-label={isFavorite ? '取消收藏' : '添加收藏'}
        aria-pressed={isFavorite}
        className="absolute top-3 right-3 p-2.5 rounded-full z-20 transition-spring-soft ripple-container"
        style={{
          background: isFavorite
            ? 'rgba(244, 67, 54, 0.2)'
            : 'rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(12px)'
        }}
      >
        <Heart
          size={18}
          style={{
            color: isFavorite ? '#F44336' : '#FFFFFF',
            fill: isFavorite ? '#F44336' : 'transparent',
            transition: 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)'
          }}
        />
      </button>

      {/* 底部信息区 */}
      <div className="absolute bottom-0 left-0 right-0 p-4">
        <h3
          className="font-bold text-base mb-1 truncate"
          style={{ color: '#FFFFFF' }}
        >
          {preset.name}
        </h3>
        <p className="text-xs truncate mb-2" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>
          {preset.author}
        </p>

        {/* 统计信息 */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1">
            <Star
              size={12}
              style={{ color: '#FFD700' }}
              className="fill-yellow-400"
            />
            <span style={{ color: 'rgba(255, 255, 255, 0.5)' }} className="text-xs">
              4.{index + 6}
            </span>
          </div>
          <div className="flex items-center gap-1">
            <Download size={12} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            <span style={{ color: 'rgba(255, 255, 255, 0.5)' }} className="text-xs">
              {(index + 1) * 2.3}w
            </span>
          </div>
          {preset.brand && (
            <div className="flex items-center gap-1 ml-auto">
              <Zap size={10} style={{ color: '#FF6B35' }} />
              <span style={{ color: '#FF6B35' }} className="text-xs font-medium">
                {preset.brand}
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
});

PresetCard.displayName = 'PresetCard';

export default PresetCard;
