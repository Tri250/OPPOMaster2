import React, { useState, useEffect, useCallback } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { tokens } from '../styles/designTokens';
import SafeImage from './SafeImage';

/**
 * 预设图片画廊组件（对齐 Android 端）
 * 支持自动轮播和手动切换
 */
interface PresetImageGalleryProps {
  images: string[];
  isPro?: boolean;
  autoPlayInterval?: number;
}

const PresetImageGallery: React.FC<PresetImageGalleryProps> = React.memo(({
  images,
  isPro = false,
  autoPlayInterval = 3000,
}) => {
  const [activeIndex, setActiveIndex] = useState(0);

  // 至少一张图片兜底
  const allImages = images.length > 0 ? images : ['https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=500&fit=crop'];

  // 自动轮播
  useEffect(() => {
    if (allImages.length <= 1) return;

    const timer = setInterval(() => {
      setActiveIndex(prev => (prev + 1) % allImages.length);
    }, autoPlayInterval);

    return () => clearInterval(timer);
  }, [allImages.length, autoPlayInterval]);

  const prev = useCallback(() => {
    setActiveIndex(i => (i > 0 ? i - 1 : allImages.length - 1));
  }, [allImages.length]);

  const next = useCallback(() => {
    setActiveIndex(i => (i + 1) % allImages.length);
  }, [allImages.length]);

  return (
    <div
      className="relative aspect-[4/3] rounded-2xl overflow-hidden"
      style={{ background: tokens.colors.surface }}
    >
      {/* 当前图片 */}
      <SafeImage
        src={allImages[activeIndex]}
        alt={`预设图片 ${activeIndex + 1}`}
        className="w-full h-full object-cover"
        style={{ transition: `opacity ${tokens.animation.duration.slow} ease` }}
        loading="lazy"
      />

      {/* 左右切换箭头 */}
      {allImages.length > 1 && (
        <>
          <button
            onClick={prev}
            className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-normal active:scale-95"
            style={{
              background: tokens.colors.glass,
              backdropFilter: 'blur(8px)',
              color: tokens.colors.textPrimary,
            }}
            aria-label="上一张"
          >
            <ChevronLeft size={16} />
          </button>
          <button
            onClick={next}
            className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full flex items-center justify-center transition-all duration-normal active:scale-95"
            style={{
              background: tokens.colors.glass,
              backdropFilter: 'blur(8px)',
              color: tokens.colors.textPrimary,
            }}
            aria-label="下一张"
          >
            <ChevronRight size={16} />
          </button>
        </>
      )}

      {/* 指示器 */}
      {allImages.length > 1 && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
          {allImages.map((_, i) => (
            <button
              key={i}
              onClick={() => setActiveIndex(i)}
              className="h-1.5 rounded-full transition-all duration-normal"
              style={{
                width: i === activeIndex ? '16px' : '6px',
                background: i === activeIndex ? tokens.colors.accent : tokens.colors.textTertiary,
              }}
              aria-label={`切换到第 ${i + 1} 张`}
            />
          ))}
        </div>
      )}

      {/* 模式标签 */}
      <div className="absolute top-3 left-3">
        <span
          className="px-2 py-1 rounded-md text-micro font-medium backdrop-blur-sm"
          style={{
            background: tokens.colors.glass,
            color: tokens.colors.textPrimary,
            border: `1px solid ${tokens.colors.glassBorder}`,
          }}
        >
          {isPro ? '⚡ PRO' : '🅰 AUTO'}
        </span>
      </div>
    </div>
  );
});

export default PresetImageGallery;