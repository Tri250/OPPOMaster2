import React, { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, Crown, Sparkles, Zap } from 'lucide-react';

/**
 * 预设图片画廊组件
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
  const [isUserInteracted, setIsUserInteracted] = useState(false);

  // 至少一张图片
  const allImages = images.length > 0 ? images : [
    'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=500&fit=crop'
  ];

  // 自动轮播
  useEffect(() => {
    if (isUserInteracted || allImages.length <= 1) return;

    const timer = setInterval(() => {
      setActiveIndex(prev => (prev + 1) % allImages.length);
    }, autoPlayInterval);

    return () => clearInterval(timer);
  }, [allImages.length, autoPlayInterval, isUserInteracted]);

  // 切换到上一张
  const prev = () => {
    setIsUserInteracted(true);
    setActiveIndex(prev => (prev > 0 ? prev - 1 : allImages.length - 1));
  };

  // 切换到下一张
  const next = () => {
    setIsUserInteracted(true);
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

      {/* 渐变遮罩 */}
      <div className="absolute inset-0 bg-gradient-to-t from-[#1a1a1a] via-transparent to-transparent" />

      {/* 左右切换箭头 */}
      {allImages.length > 1 && (
        <>
          <button
            onClick={prev}
            className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center hover:bg-black/70 transition-colors"
          >
            <ChevronLeft size={16} className="text-white" />
          </button>
          <button
            onClick={next}
            className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center hover:bg-black/70 transition-colors"
          >
            <ChevronRight size={16} className="text-white" />
          </button>
        </>
      )}

      {/* 指示器 */}
      {allImages.length > 1 && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5 px-3 py-1.5 rounded-full bg-black/50 backdrop-blur-sm">
          {allImages.map((_, i) => (
            <div
              key={i}
              onClick={() => {
                setIsUserInteracted(true);
                setActiveIndex(i);
              }}
              className={`rounded-full transition-all cursor-pointer ${
                i === activeIndex
                  ? 'bg-[#FF6B35] w-4 h-1.5'
                  : 'bg-white/40 w-1.5 h-1.5 hover:bg-white/60'
              }`}
            />
          ))}
        </div>
      )}

      {/* 模式标签 */}
      <div className="absolute top-3 left-3 flex gap-2">
        {isPro ? (
          <span className="px-2 py-1 rounded-md bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-white text-[10px] font-bold flex items-center gap-1">
            <Zap size={10} />
            PRO
          </span>
        ) : (
          <span className="px-2 py-1 rounded-md bg-black/50 backdrop-blur-sm text-white text-[10px] font-medium">
            AUTO
          </span>
        )}
      </div>

      {/* 图片计数 */}
      {allImages.length > 1 && (
        <div className="absolute top-3 right-3 px-2 py-1 rounded-md bg-black/50 backdrop-blur-sm text-white text-[10px]">
          {activeIndex + 1} / {allImages.length}
        </div>
      )}
    </div>
  );
};

export default PresetImageGallery;