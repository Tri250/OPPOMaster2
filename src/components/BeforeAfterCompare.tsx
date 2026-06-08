import React, { useState, useRef, useCallback, useEffect } from 'react';
import { MoveHorizontal, Columns, ToggleLeft } from 'lucide-react';

type CompareMode = 'split' | 'slider' | 'toggle';

interface BeforeAfterCompareProps {
  beforeImage: string;
  afterImage: string;
  presetName: string;
}

const BeforeAfterCompare: React.FC<BeforeAfterCompareProps> = ({
  beforeImage,
  afterImage,
  presetName,
}) => {
  const [mode, setMode] = useState<CompareMode>('slider');
  const [sliderPosition, setSliderPosition] = useState(50);
  const [showAfter, setShowAfter] = useState(true);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // 处理滑块拖动
  const handleSliderMove = useCallback((clientX: number) => {
    if (!containerRef.current || !isDragging) return;
    
    const rect = containerRef.current.getBoundingClientRect();
    const x = clientX - rect.left;
    const percentage = Math.max(0, Math.min(100, (x / rect.width) * 100));
    setSliderPosition(percentage);
  }, [isDragging]);

  // 鼠标事件
  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleMouseMove = useCallback((e: MouseEvent) => {
    handleSliderMove(e.clientX);
  }, [handleSliderMove]);

  const handleMouseUp = useCallback(() => {
    setIsDragging(false);
  }, []);

  // 触摸事件
  const handleTouchStart = () => {
    setIsDragging(true);
  };

  const handleTouchMove = useCallback((e: TouchEvent) => {
    if (e.touches.length > 0) {
      handleSliderMove(e.touches[0].clientX);
    }
  }, [handleSliderMove]);

  const handleTouchEnd = useCallback(() => {
    setIsDragging(false);
  }, []);

  // 绑定全局事件
  useEffect(() => {
    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
      window.addEventListener('touchmove', handleTouchMove);
      window.addEventListener('touchend', handleTouchEnd);
    }
    
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('touchend', handleTouchEnd);
    };
  }, [isDragging, handleMouseMove, handleMouseUp, handleTouchMove, handleTouchEnd]);

  // 模式切换按钮
  const modeButtons = [
    { id: 'slider' as const, icon: MoveHorizontal, label: '滑动' },
    { id: 'split' as const, icon: Columns, label: '分屏' },
    { id: 'toggle' as const, icon: ToggleLeft, label: '切换' },
  ];

  return (
    <div className="flex flex-col h-full" style={{ background: '#0a0a0a' }}>
      {/* 标题栏 */}
      <div className="flex items-center justify-between px-4 py-3">
        <span 
          className="text-sm font-medium"
          style={{ color: '#FF6B35' }}
        >
          {presetName} - 效果对比
        </span>
        
        {/* 模式切换按钮 */}
        <div className="flex items-center gap-1 p-1 rounded-lg" style={{ background: 'rgba(255, 255, 255, 0.05)' }}>
          {modeButtons.map(({ id, icon: Icon, label }) => (
            <button
              key={id}
              onClick={() => setMode(id)}
              className="flex items-center gap-1 px-3 py-1.5 rounded-md text-xs font-medium transition-all"
              style={{
                background: mode === id ? '#FF6B35' : 'transparent',
                color: mode === id ? '#FFFFFF' : 'rgba(255, 255, 255, 0.6)',
              }}
            >
              <Icon size={14} />
              <span>{label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 对比区域 */}
      <div 
        ref={containerRef}
        className="flex-1 relative overflow-hidden mx-4 mb-4 rounded-xl"
        style={{ 
          border: '2px solid #FF6B35',
          background: '#000',
        }}
      >
        {/* 滑动对比模式 */}
        {mode === 'slider' && (
          <>
            {/* After 图片 (底层) */}
            <img
              src={afterImage}
              alt="效果"
              className="absolute inset-0 w-full h-full object-cover"
              draggable={false}
            />
            
            {/* Before 图片 (带裁剪) */}
            <div 
              className="absolute inset-0 overflow-hidden"
              style={{ width: `${sliderPosition}%` }}
            >
              <img
                src={beforeImage}
                alt="原图"
                className="absolute inset-0 w-full h-full object-cover"
                style={{ width: `${100 / (sliderPosition / 100)}%`, maxWidth: 'none' }}
                draggable={false}
              />
            </div>

            {/* 分割线 */}
            <div 
              className="absolute top-0 bottom-0 w-1 cursor-ew-resize"
              style={{ 
                left: `${sliderPosition}%`,
                background: '#FF6B35',
                transform: 'translateX(-50%)',
              }}
              onMouseDown={handleMouseDown}
              onTouchStart={handleTouchStart}
            >
              {/* 拖动手柄 */}
              <div 
                className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-10 h-10 rounded-full flex items-center justify-center"
                style={{ 
                  background: '#FF6B35',
                  boxShadow: '0 2px 8px rgba(0, 0, 0, 0.3)',
                }}
              >
                <MoveHorizontal size={20} style={{ color: '#FFFFFF' }} />
              </div>
            </div>

            {/* 标签 */}
            <div 
              className="absolute top-4 left-4 px-3 py-1.5 rounded-full text-xs font-medium"
              style={{ 
                background: 'rgba(0, 0, 0, 0.6)',
                color: '#FFFFFF',
                backdropFilter: 'blur(8px)',
              }}
            >
              原图
            </div>
            <div 
              className="absolute top-4 right-4 px-3 py-1.5 rounded-full text-xs font-medium"
              style={{ 
                background: 'rgba(255, 107, 53, 0.9)',
                color: '#FFFFFF',
                backdropFilter: 'blur(8px)',
              }}
            >
              效果
            </div>
          </>
        )}

        {/* 分屏对比模式 */}
        {mode === 'split' && (
          <div className="flex h-full">
            {/* 左侧 - 原图 */}
            <div className="flex-1 relative border-r" style={{ borderRightColor: '#FF6B35' }}>
              <img
                src={beforeImage}
                alt="原图"
                className="w-full h-full object-cover"
                draggable={false}
              />
              <div 
                className="absolute top-4 left-4 px-3 py-1.5 rounded-full text-xs font-medium"
                style={{ 
                  background: 'rgba(0, 0, 0, 0.6)',
                  color: '#FFFFFF',
                  backdropFilter: 'blur(8px)',
                }}
              >
                原图
              </div>
            </div>
            
            {/* 右侧 - 效果 */}
            <div className="flex-1 relative">
              <img
                src={afterImage}
                alt="效果"
                className="w-full h-full object-cover"
                draggable={false}
              />
              <div 
                className="absolute top-4 right-4 px-3 py-1.5 rounded-full text-xs font-medium"
                style={{ 
                  background: 'rgba(255, 107, 53, 0.9)',
                  color: '#FFFFFF',
                  backdropFilter: 'blur(8px)',
                }}
              >
                效果
              </div>
            </div>
          </div>
        )}

        {/* 快速切换模式 */}
        {mode === 'toggle' && (
          <div 
            className="w-full h-full cursor-pointer"
            onClick={() => setShowAfter(!showAfter)}
          >
            <img
              src={showAfter ? afterImage : beforeImage}
              alt={showAfter ? '效果' : '原图'}
              className="w-full h-full object-cover transition-opacity duration-300"
              draggable={false}
            />
            <div 
              className="absolute top-4 left-4 px-3 py-1.5 rounded-full text-xs font-medium"
              style={{ 
                background: showAfter ? 'rgba(255, 107, 53, 0.9)' : 'rgba(0, 0, 0, 0.6)',
                color: '#FFFFFF',
                backdropFilter: 'blur(8px)',
              }}
            >
              {showAfter ? '效果' : '原图'}
            </div>
            
            {/* 点击提示 */}
            <div 
              className="absolute bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full text-xs"
              style={{ 
                background: 'rgba(0, 0, 0, 0.6)',
                color: 'rgba(255, 255, 255, 0.7)',
                backdropFilter: 'blur(8px)',
              }}
            >
              点击切换
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default BeforeAfterCompare;