import React, { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * 预设图片画廊组件（对齐Android端）
 * 支持自动轮播和手动切换
 * 
 * @param images 图片路径列表
 * @param isPro 是否为Pro模式
 * @param autoPlayInterval 自动播放间隔（毫秒），默认 3000ms
 */
interface PresetImageGalleryProps {
  images: string[];
  isPro?: boolean;
  autoPlayInterval?: number;
}

const PresetImageGallery: React.FC<PresetImageGalleryProps> = ({
  images,
  isPro = false,
  autoPlayInterval = 3000,
}) => {
  const [activeIndex, setActiveIndex] = useState(0);

  // 至少一张图片（对齐Android端逻辑）
  const allImages = images.length > 0 ? images : [images[0] || 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=500&fit=crop'];

  // 自动轮播（对齐用户规范）
  useEffect(() => {
    if (allImages.length <= 1) return;

    const timer = setInterval(() => {
      setActiveIndex(prev => (prev + 1) % allImages.length);
    }, autoPlayInterval);

    return () => clearInterval(timer);
  }, [allImages.length, autoPlayInterval]);

  // 切换到上一张
  const prev = () => {
    setActiveIndex(prev => (prev > 0 ? prev - 1 : allImages.length - 1));
  };

  // 切换到下一张
  const next = () => {
    setActiveIndex(prev => (prev + 1) % allImages.length);
  };

  return (
    <div className="relative aspect-[4/3] rounded-2xl overflow-hidden bg-[#1a1a1a]">
      {/* 当前图片 */}
      <img
        src={allImages[activeIndex]}
        alt={`预设图片 ${activeIndex + 1}`}
        className="w-full h-full object-cover transition-opacity duration-500"
        loading="lazy"
      />

      {/* 左右切换箭头（对齐用户规范样式） */}
      {allImages.length > 1 && (
        <>
          <button
            onClick={prev}
            className="absolute left-2 top-1/2 -translate-y-1/2 
              w-8 h-8 rounded-full bg-black/50 flex items-center justify-center
              hover:bg-black/70 transition-colors"
          >
            <ChevronLeft size={16} className="text-white" />
          </button>
          <button
            onClick={next}
            className="absolute right-2 top-1/2 -translate-y-1/2
              w-8 h-8 rounded-full bg-black/50 flex items-center justify-center
              hover:bg-black/70 transition-colors"
          >
            <ChevronRight size={16} className="text-white" />
          </button>
        </>
      )}

      {/* 指示器（对齐用户规范：底部居中，圆点样式） */}
      {allImages.length > 1 && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
          {allImages.map((_, i) => (
            <div
              key={i}
              onClick={() => setActiveIndex(i)}
              className={`w-1.5 h-1.5 rounded-full transition-all cursor-pointer ${
                i === activeIndex
                  ? 'bg-[#FF6B35] w-4'
                  : 'bg-white/40 hover:bg-white/60'
              }`}
            />
          ))}
        </div>
      )}

      {/* 模式标签（对齐用户规范） */}
      <div className="absolute top-3 left-3">
        <span className="px-2 py-1 rounded-md bg-black/50 text-white text-[10px] font-medium">
          {isPro ? '⚡ PRO' : '🅰 AUTO'}
        </span>
      </div>
    </div>
  );
};

export default PresetImageGallery;