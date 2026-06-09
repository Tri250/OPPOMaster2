/**
 * Before/After对比滑块组件
 * 用于展示LUT应用前后效果对比
 */

import React, { useState, useRef, useCallback } from 'react';

interface BeforeAfterSliderProps {
  originalImage: string;      // 原始图片 URL
  processedImage: string;     // LUT 应用后图片 URL
  className?: string;
}

const BeforeAfterSlider: React.FC<BeforeAfterSliderProps> = ({
  originalImage,
  processedImage,
  className = '',
}) => {
  const [sliderPosition, setSliderPosition] = useState(0.5);
  const containerRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef(false);

  const handleMouseDown = useCallback(() => {
    isDragging.current = true;
  }, []);

  const handleMouseUp = useCallback(() => {
    isDragging.current = false;
  }, []);

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!isDragging.current || !containerRef.current) return;
    
    const rect = containerRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const newPosition = x / rect.width;
    setSliderPosition(Math.max(0.1, Math.min(0.9, newPosition)));
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (!containerRef.current) return;
    
    const rect = containerRef.current.getBoundingClientRect();
    const x = e.touches[0].clientX - rect.left;
    const newPosition = x / rect.width;
    setSliderPosition(Math.max(0.1, Math.min(0.9, newPosition)));
  }, []);

  return (
    <div
      ref={containerRef}
      className={`relative overflow-hidden rounded-xl ${className}`}
      style={{ aspectRatio: '16/9' }}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      {/* 处理后图片（底层） */}
      <img
        src={processedImage}
        alt="应用LUT后"
        className="absolute inset-0 w-full h-full object-cover"
        draggable={false}
      />

      {/* 原始图片（左侧裁剪） */}
      <div
        className="absolute inset-y-0 left-0 overflow-hidden"
        style={{ width: `${sliderPosition * 100}%` }}
      >
        <img
          src={originalImage}
          alt="原始图片"
          className="absolute inset-y-0 left-0 h-full object-cover"
          style={{ width: `${100 / sliderPosition}%`, maxWidth: 'none' }}
          draggable={false}
        />
      </div>

      {/* 分割线 */}
      <div
        className="absolute inset-y-0 w-[3px] bg-[#FF6B35]"
        style={{ left: `${sliderPosition * 100}%` }}
      />

      {/* 拖拽手柄 */}
      <div
        className="absolute w-9 h-9 rounded-full bg-[#FF6B35] flex items-center justify-center cursor-grab active:cursor-grabbing shadow-lg"
        style={{
          left: `${sliderPosition * 100}%`,
          top: '50%',
          transform: 'translate(-50%, -50%)',
        }}
        onMouseDown={handleMouseDown}
        onTouchMove={handleTouchMove}
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="white"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M8 12H4" />
          <path d="M16 12h4" />
          <path d="M12 8V4" />
          <path d="M12 16v4" />
        </svg>
      </div>

      {/* 标签 */}
      {sliderPosition > 0.3 && (
        <div
          className="absolute top-2 left-2 px-2 py-1 bg-black/50 rounded text-white text-xs font-medium"
        >
          原始
        </div>
      )}
      {sliderPosition < 0.7 && (
        <div
          className="absolute top-2 right-2 px-2 py-1 bg-black/50 rounded text-[#FF6B35] text-xs font-medium"
        >
          配方效果
        </div>
      )}
    </div>
  );
};

export default BeforeAfterSlider;