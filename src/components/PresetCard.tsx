import React, { useState, useRef, useCallback } from 'react';
import { Heart, Sparkles, Crown, Download, Star, Zap } from 'lucide-react';
import { useAppStore, Preset } from '../store/appStore';
import QuickActionMenu from './QuickActionMenu';

interface PresetCardProps {
  preset: Preset;
  variant?: 'compact' | 'full';
  index: number;
}

const PresetCard: React.FC<PresetCardProps> = React.memo(({
  preset,
  variant = 'compact',
  index,
}) => {
  const { 
    favoritePresetIds, 
    toggleFavorite, 
    setSelectedPreset, 
    navigateToSubPage,
    applyPreset,
    addToRecent
  } = useAppStore();

  const isFavorite = favoritePresetIds.includes(preset.id);

  // 长按快捷菜单状态
  const [showQuickMenu, setShowQuickMenu] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });
  const longPressTimer = useRef<NodeJS.Timeout | null>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const isLongPressTriggered = useRef(false);

  // 长按开始
  const handleLongPressStart = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    isLongPressTriggered.current = false;
    
    // 获取触点位置
    let clientX = 0, clientY = 0;
    if ('touches' in e) {
      clientX = e.touches[0].clientX;
      clientY = e.touches[0].clientY;
    } else {
      clientX = e.clientX;
      clientY = e.clientY;
    }

    longPressTimer.current = setTimeout(() => {
      isLongPressTriggered.current = true;
      setMenuPosition({ x: clientX, y: clientY });
      setShowQuickMenu(true);
    }, 500); // 500ms 长按触发
  }, []);

  // 长按结束
  const handleLongPressEnd = useCallback(() => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }
  }, []);

  const handleCardClick = () => {
    // 如果是长按触发，不执行点击
    if (isLongPressTriggered.current) {
      isLongPressTriggered.current = false;
      return;
    }
    addToRecent(preset);
    setSelectedPreset(preset);
    navigateToSubPage('preset-detail');
  };

  const handleFavoriteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    toggleFavorite(preset.id);
  };

  const handleApplyClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    addToRecent(preset);
    applyPreset(preset.id);
  };

  // 右键菜单（桌面端）
  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    setMenuPosition({ x: e.clientX, y: e.clientY });
    setShowQuickMenu(true);
  };

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
      <>
        <div
          ref={cardRef}
          onClick={handleCardClick}
          onContextMenu={handleContextMenu}
          onTouchStart={handleLongPressStart}
          onTouchEnd={handleLongPressEnd}
          onTouchMove={handleLongPressEnd}
          className="group relative animate-liquid-slide-up cursor-pointer"
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
            decoding="async"
            onError={(e) => {
              const img = e.currentTarget;
              img.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" fill="%23333"><rect width="400" height="300"/><text x="50%" y="50%" fill="%23666" font-size="14" text-anchor="middle" dy=".3em">图片加载失败</text></svg>');
              img.style.objectFit = 'contain';
            }}
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
          onClick={handleFavoriteClick}
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
            onClick={handleApplyClick}
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

      {/* 快捷菜单 */}
      {showQuickMenu && (
        <QuickActionMenu
          preset={preset}
          position={menuPosition}
          isFavorite={isFavorite}
          onClose={() => setShowQuickMenu(false)}
          onFavorite={() => toggleFavorite(preset.id)}
          onShare={() => console.log('分享预设:', preset.id)}
          onApply={() => {
            addToRecent(preset);
            applyPreset(preset.id);
          }}
          onViewDetails={() => {
            addToRecent(preset);
            setSelectedPreset(preset);
            navigateToSubPage('preset-detail');
          }}
        />
      )}
    </>
    );
  }

  // compact variant - 首页风格
  return (
    <>
      <div
        ref={cardRef}
        onClick={handleCardClick}
        onContextMenu={handleContextMenu}
        onTouchStart={handleLongPressStart}
        onTouchEnd={handleLongPressEnd}
        onTouchMove={handleLongPressEnd}
        className={`group relative ${getImageHeight(index)} animate-liquid-slide-up cursor-pointer`}
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
        decoding="async"
        onError={(e) => {
          const img = e.currentTarget;
          img.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" fill="%23333"><rect width="400" height="300"/><text x="50%" y="50%" fill="%23666" font-size="14" text-anchor="middle" dy=".3em">图片加载失败</text></svg>');
          img.style.objectFit = 'contain';
        }}
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
        onClick={handleFavoriteClick}
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

    {/* 快捷菜单 */}
    {showQuickMenu && (
      <QuickActionMenu
        preset={preset}
        position={menuPosition}
        isFavorite={isFavorite}
        onClose={() => setShowQuickMenu(false)}
        onFavorite={() => toggleFavorite(preset.id)}
        onShare={() => console.log('分享预设:', preset.id)}
        onApply={() => {
          addToRecent(preset);
          applyPreset(preset.id);
        }}
        onViewDetails={() => {
          addToRecent(preset);
          setSelectedPreset(preset);
          navigateToSubPage('preset-detail');
        }}
      />
    )}
  </>
  );
});

PresetCard.displayName = 'PresetCard';

export default PresetCard;
