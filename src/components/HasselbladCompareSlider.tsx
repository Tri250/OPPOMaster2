import React, { useState, useRef, useCallback } from 'react';

/**
 * Layer 3: 大师呈现层 - Before/After 滑杆对比组件
 * 
 * 关键交互：让用户直观看到 AI 优化前后的差异
 * 设计规范：
 * - 哈苏橙强调色 #FF6B35
 * - 滑杆手柄带左右箭头
 * - 角落标签：原始/哈苏优化
 * - HNCS 水印
 */

interface HasselbladCompareSliderProps {
  original: string;
  processed: string;
  aspectRatio?: string;
}

export const HasselbladCompareSlider: React.FC<HasselbladCompareSliderProps> = ({
  original,
  processed,
  aspectRatio = '4/3',
}) => {
  const [position, setPosition] = useState(50);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleMove = useCallback((clientX: number) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = Math.max(0, Math.min(clientX - rect.left, rect.width));
    setPosition((x / rect.width) * 100);
  }, []);

  const handleMouseDown = () => setIsDragging(true);
  const handleMouseUp = () => setIsDragging(false);
  const handleMouseMove = (e: React.MouseEvent) => {
    if (isDragging) handleMove(e.clientX);
  };
  const handleTouchMove = (e: React.TouchEvent) => {
    handleMove(e.touches[0].clientX);
  };

  return (
    <div
      ref={containerRef}
      className="relative w-full overflow-hidden cursor-ew-resize select-none rounded-2xl bg-[#0A0A0A]"
      style={{ aspectRatio }}
      onMouseDown={handleMouseDown}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      onMouseMove={handleMouseMove}
      onTouchStart={handleMouseDown}
      onTouchEnd={handleMouseUp}
      onTouchMove={handleTouchMove}
    >
      {/* After 图层（全图） */}
      <img
        src={processed}
        className="absolute inset-0 w-full h-full object-cover"
        alt="哈苏优化后"
        draggable={false}
      />

      {/* Before 图层（左侧裁剪） */}
      <div
        className="absolute inset-0 overflow-hidden"
        style={{ clipPath: `inset(0 ${100 - position}% 0 0)` }}
      >
        <img
          src={original}
          className="absolute inset-0 w-full h-full object-cover"
          alt="原始照片"
          draggable={false}
        />
      </div>

      {/* 滑杆分隔线 */}
      <div
        className="absolute top-0 bottom-0 w-0.5 bg-white shadow-[0_0_12px_rgba(255,107,53,0.5)] z-10"
        style={{ left: `${position}%` }}
      >
        {/* 滑杆手柄 */}
        <div
          className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2
                     w-10 h-10 rounded-full bg-white shadow-lg
                     border-2 border-[#FF6B35]
                     flex items-center justify-center
                     transition-transform duration-150
                     hover:scale-110"
        >
          <div className="w-5 h-5 flex items-center justify-center">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M5 3L2 8L5 13" stroke="#FF6B35" strokeWidth="2" strokeLinecap="round" />
              <path d="M11 3L14 8L11 13" stroke="#FF6B35" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </div>
        </div>
      </div>

      {/* 角落标签 */}
      <div className="absolute top-3 left-3 px-2.5 py-1.5 bg-black/60 backdrop-blur-sm
                      rounded-md text-white text-[11px] font-medium tracking-wide">
        原始
      </div>
      <div className="absolute top-3 right-3 px-2.5 py-1.5 bg-[#FF6B35]/80 backdrop-blur-sm
                      rounded-md text-white text-[11px] font-medium tracking-wide">
        哈苏优化
      </div>

      {/* HNCS 水印 */}
      <div className="absolute bottom-3 right-3">
        <span className="text-white/30 text-[9px] tracking-widest font-light">
          HNCS · OMaster
        </span>
      </div>

      {/* 底部提示 */}
      <div className="absolute bottom-3 left-1/2 -translate-x-1/2
                      px-3 py-1.5 bg-black/40 backdrop-blur-sm rounded-full">
        <span className="text-white/50 text-[10px]">拖拽滑杆对比效果</span>
      </div>
    </div>
  );
};

export default HasselbladCompareSlider;
