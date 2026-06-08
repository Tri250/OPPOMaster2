import React, { useEffect, useRef } from 'react';
import { Heart, Share2, Zap, Info, X } from 'lucide-react';
import { Preset, HASSelBLAD_ORANGE } from '../store/appStore';

interface QuickActionMenuProps {
  preset: Preset;
  position: { x: number; y: number };
  isFavorite: boolean;
  onClose: () => void;
  onFavorite: () => void;
  onShare: () => void;
  onApply: () => void;
  onViewDetails: () => void;
}

/**
 * 浮动快捷菜单组件
 * 支持4个快捷操作按钮：快速收藏、快速分享、快速应用、查看详情
 * 点击外部关闭，带动画效果
 */
const QuickActionMenu: React.FC<QuickActionMenuProps> = ({
  preset,
  position,
  isFavorite,
  onClose,
  onFavorite,
  onShare,
  onApply,
  onViewDetails,
}) => {
  const menuRef = useRef<HTMLDivElement>(null);

  // 点击外部关闭
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent | TouchEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    // 延迟添加事件监听器，避免立即触发
    const timer = setTimeout(() => {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('touchstart', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }, 100);

    return () => {
      clearTimeout(timer);
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('touchstart', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [onClose]);

  // 计算菜单位置，确保不超出屏幕
  const menuStyle: React.CSSProperties = {
    position: 'fixed',
    left: Math.min(position.x, window.innerWidth - 200),
    top: Math.min(position.y, window.innerHeight - 280),
    zIndex: 1000,
  };

  const actions = [
    {
      id: 'favorite',
      icon: Heart,
      label: isFavorite ? '取消收藏' : '收藏',
      color: isFavorite ? '#F44336' : '#FFFFFF',
      fill: isFavorite ? '#F44336' : 'transparent',
      onClick: onFavorite,
    },
    {
      id: 'share',
      icon: Share2,
      label: '分享',
      color: '#FFFFFF',
      fill: 'transparent',
      onClick: onShare,
    },
    {
      id: 'apply',
      icon: Zap,
      label: '快速应用',
      color: HASSelBLAD_ORANGE,
      fill: HASSelBLAD_ORANGE,
      onClick: onApply,
    },
    {
      id: 'details',
      icon: Info,
      label: '查看详情',
      color: '#FFFFFF',
      fill: 'transparent',
      onClick: onViewDetails,
    },
  ];

  return (
    <>
      {/* 背景遮罩 */}
      <div 
        className="fixed inset-0 animate-fade-in"
        style={{ 
          background: 'rgba(0, 0, 0, 0.3)',
          zIndex: 999,
        }}
        onClick={onClose}
      />
      
      {/* 菜单容器 */}
      <div
        ref={menuRef}
        style={menuStyle}
        className="animate-scale-in"
      >
        {/* 预设信息头部 */}
        <div 
          className="flex items-center gap-3 p-3 rounded-t-2xl"
          style={{ 
            background: 'rgba(30, 30, 30, 0.95)',
            backdropFilter: 'blur(20px)',
            borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
          }}
        >
          <img
            src={preset.coverPath}
            alt={preset.name}
            className="w-12 h-12 rounded-lg object-cover"
            loading="lazy"
          />
          <div className="flex-1 min-w-0">
            <p 
              className="font-semibold text-sm truncate"
              style={{ color: '#FFFFFF' }}
            >
              {preset.name}
            </p>
            <p 
              className="text-xs truncate"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              {preset.author}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full transition-colors"
            style={{ background: 'rgba(255, 255, 255, 0.1)' }}
            aria-label="关闭菜单"
          >
            <X size={16} style={{ color: 'rgba(255, 255, 255, 0.7)' }} />
          </button>
        </div>

        {/* 操作按钮列表 */}
        <div 
          className="p-2 rounded-b-2xl"
          style={{ 
            background: 'rgba(30, 30, 30, 0.95)',
            backdropFilter: 'blur(20px)',
          }}
        >
          {actions.map((action, index) => {
            const Icon = action.icon;
            return (
              <button
                key={action.id}
                onClick={() => {
                  action.onClick();
                  onClose();
                }}
                className="w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all active:scale-95"
                style={{
                  background: action.id === 'apply' 
                    ? `linear-gradient(135deg, ${HASSelBLAD_ORANGE}20, ${HASSelBLAD_ORANGE}10)`
                    : 'transparent',
                  animationDelay: `${index * 50}ms`,
                }}
              >
                <div 
                  className="w-8 h-8 rounded-lg flex items-center justify-center"
                  style={{ 
                    background: action.id === 'apply' 
                      ? HASSelBLAD_ORANGE 
                      : 'rgba(255, 255, 255, 0.1)',
                  }}
                >
                  <Icon 
                    size={18} 
                    style={{ 
                      color: action.id === 'apply' ? '#FFFFFF' : action.color,
                      fill: action.id === 'apply' ? 'none' : action.fill,
                    }} 
                  />
                </div>
                <span 
                  className="font-medium text-sm"
                  style={{ 
                    color: action.id === 'apply' ? HASSelBLAD_ORANGE : action.color,
                  }}
                >
                  {action.label}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      <style>{`
        @keyframes fade-in {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes scale-in {
          from { 
            opacity: 0; 
            transform: scale(0.9) translateY(-10px); 
          }
          to { 
            opacity: 1; 
            transform: scale(1) translateY(0); 
          }
        }
        .animate-fade-in {
          animation: fade-in 0.2s ease-out forwards;
        }
        .animate-scale-in {
          animation: scale-in 0.25s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
        }
      `}</style>
    </>
  );
};

export default QuickActionMenu;