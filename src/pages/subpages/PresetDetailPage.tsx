import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ChevronLeft, Heart, Share2, Download, Lightbulb, ChevronDown, ChevronUp } from 'lucide-react';

/**
 * ============================================
 * 预设详情页 - 展示样张影像参数
 * 参考图片设计：大图预览 + 调色参数卡片
 * ============================================
 */
const PresetDetailPage: React.FC = () => {
  const { 
    selectedPreset, 
    goBack, 
    toggleFavorite,
    isPresetFavorite,
    applyPreset,
    navigateToSubPage
  } = useAppStore();

  const [showShootingTips, setShowShootingTips] = React.useState(true);

  // 如果没有选中预设，返回首页
  if (!selectedPreset) {
    goBack();
    return null;
  }

  const isFavorite = isPresetFavorite(selectedPreset.id);

  // 获取参数显示值
  const getParamValue = (value: number | undefined, unit: string = '') => {
    if (value === undefined) return '0';
    const sign = value > 0 ? '+' : '';
    return `${sign}${value}${unit}`;
  };

  // 获取滤镜强度百分比
  const getFilterIntensity = () => {
    const baseIntensity = Math.abs(selectedPreset.saturation || 0) + 
                         Math.abs(selectedPreset.contrast || 0) + 
                         Math.abs(selectedPreset.warmth || 0);
    return Math.min(Math.round(baseIntensity * 1.5), 100);
  };

  return (
    <div 
      className="h-full flex flex-col overflow-hidden"
      style={{ background: '#0a0a0a' }}
    >
      {/* 标题栏 */}
      <div className="flex items-center justify-between px-4 pt-12 pb-4 relative">
        <button 
          onClick={goBack}
          className="p-2 -ml-2"
          aria-label="返回"
        >
          <ChevronLeft size={24} style={{ color: '#FFFFFF' }} />
        </button>
        
        <div className="flex flex-col items-center">
          <h1 
            className="text-xl font-bold"
            style={{ color: '#FFFFFF' }}
          >
            {selectedPreset.name}
          </h1>
          <span 
            className="text-sm"
            style={{ color: 'rgba(255, 255, 255, 0.5)' }}
          >
            {selectedPreset.author}
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button 
            onClick={() => {}}
            className="p-2"
            aria-label="分享"
          >
            <Share2 size={20} style={{ color: '#FFFFFF' }} />
          </button>
          <button 
            onClick={() => toggleFavorite(selectedPreset.id)}
            className="p-2"
            aria-label={isFavorite ? '取消收藏' : '收藏'}
          >
            <Heart 
              size={20} 
              style={{ 
                color: isFavorite ? '#F44336' : '#FFFFFF',
                fill: isFavorite ? '#F44336' : 'transparent'
              }} 
            />
          </button>
        </div>
      </div>

      {/* 可滚动内容区 */}
      <div className="flex-1 overflow-y-auto">
        {/* 大图预览 */}
        <div className="relative px-4 mb-6">
          <div 
            className="relative rounded-2xl overflow-hidden"
            style={{ aspectRatio: '4/3' }}
          >
            <img
              src={selectedPreset.coverPath}
              alt={selectedPreset.name}
              className="w-full h-full object-cover"
              loading="lazy"
            />
            
            {/* 哈苏HNCS标识 */}
            {selectedPreset.isHncs && (
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="text-center">
                  <div 
                    className="text-4xl font-bold mb-1"
                    style={{ color: '#FFFFFF', textShadow: '0 2px 8px rgba(0,0,0,0.5)' }}
                  >
                    H
                  </div>
                  <div 
                    className="text-xs"
                    style={{ color: 'rgba(255, 255, 255, 0.8)' }}
                  >
                    OPPO Find X8 Ultra
                  </div>
                </div>
              </div>
            )}

            {/* 左右切换按钮 */}
            <button 
              className="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full flex items-center justify-center"
              style={{ background: 'rgba(0, 0, 0, 0.5)' }}
              aria-label="上一张"
            >
              <ChevronLeft size={20} style={{ color: '#FFFFFF' }} />
            </button>
            <button 
              className="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full flex items-center justify-center"
              style={{ background: 'rgba(0, 0, 0, 0.5)' }}
              aria-label="下一张"
            >
              <ChevronLeft size={20} style={{ color: '#FFFFFF', transform: 'rotate(180deg)' }} />
            </button>
          </div>

          {/* Auto标签 */}
          <div 
            className="absolute bottom-8 left-8 px-3 py-1.5 rounded-full text-xs font-medium"
            style={{ 
              background: 'rgba(255, 255, 255, 0.2)',
              color: '#FFFFFF',
              backdropFilter: 'blur(8px)'
            }}
          >
            Auto
          </div>
        </div>

        {/* 拍摄建议 - 可折叠 */}
        <div className="px-4 mb-4">
          <button
            onClick={() => setShowShootingTips(!showShootingTips)}
            className="w-full flex items-center justify-between p-4 rounded-xl"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div className="flex items-center gap-3">
              <Lightbulb size={20} style={{ color: '#FF6B35' }} />
              <span 
                className="font-medium"
                style={{ color: '#FF6B35' }}
              >
                拍摄建议
              </span>
            </div>
            {showShootingTips ? (
              <ChevronUp size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            ) : (
              <ChevronDown size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            )}
          </button>
          
          {showShootingTips && (
            <div 
              className="mt-2 p-4 rounded-xl text-sm"
              style={{ 
                background: 'rgba(255, 255, 255, 0.03)',
                color: 'rgba(255, 255, 255, 0.7)'
              }}
            >
              <p className="mb-2">• 建议在光线充足的环境下拍摄</p>
              <p className="mb-2">• 使用主摄镜头获得最佳画质</p>
              <p>• 开启HDR模式增强动态范围</p>
            </div>
          )}
        </div>

        {/* 调色参数标题 */}
        <div className="px-4 mb-4">
          <h2 
            className="text-lg font-bold"
            style={{ color: '#FF6B35' }}
          >
            调色参数
          </h2>
        </div>

        {/* 滤镜强度 */}
        <div className="px-4 mb-4">
          <div 
            className="p-4 rounded-xl text-center"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              滤镜
            </div>
            <div 
              className="text-xl font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {selectedPreset.name} {getFilterIntensity()}%
            </div>
          </div>
        </div>

        {/* 参数网格 */}
        <div className="px-4 grid grid-cols-2 gap-3 mb-6">
          {/* 柔光 */}
          <div 
            className="p-4 rounded-xl text-center"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              柔光
            </div>
            <div 
              className="text-lg font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {selectedPreset.clarity && selectedPreset.clarity > 10 ? '梦幻' : '自然'}
            </div>
          </div>

          {/* 影调 */}
          <div 
            className="p-4 rounded-xl text-center"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              影调
            </div>
            <div 
              className="text-lg font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {getParamValue(selectedPreset.contrast)}
            </div>
          </div>

          {/* 饱和度 */}
          <div 
            className="p-4 rounded-xl text-center"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              饱和度
            </div>
            <div 
              className="text-lg font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {getParamValue(selectedPreset.saturation)}
            </div>
          </div>

          {/* 冷暖 */}
          <div 
            className="p-4 rounded-xl text-center"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              冷暖
            </div>
            <div 
              className="text-lg font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {getParamValue(selectedPreset.warmth)}
            </div>
          </div>

          {/* 青品 */}
          <div 
            className="p-4 rounded-xl text-center"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              青品
            </div>
            <div 
              className="text-lg font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {getParamValue(Math.round((selectedPreset.saturation || 0) * 0.6))}
            </div>
          </div>

          {/* 锐度 */}
          <div 
            className="p-4 rounded-xl text-center"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              锐度
            </div>
            <div 
              className="text-lg font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {selectedPreset.sharpness || 0}
            </div>
          </div>

          {/* 暗角 */}
          <div 
            className="p-4 rounded-xl text-center col-span-2"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div 
              className="text-xs mb-2"
              style={{ color: 'rgba(255, 255, 255, 0.5)' }}
            >
              暗角
            </div>
            <div 
              className="text-lg font-bold"
              style={{ color: '#FFFFFF' }}
            >
              {(selectedPreset.clarity || 0) > 15 ? '开启' : '关闭'}
            </div>
          </div>
        </div>

        {/* 应用按钮 */}
        <div className="px-4 pb-8">
          <button
            onClick={() => {
              applyPreset(selectedPreset.id);
              goBack();
            }}
            className="w-full py-4 rounded-xl font-bold text-base flex items-center justify-center gap-2"
            style={{ 
              background: '#FF6B35',
              color: '#FFFFFF'
            }}
          >
            <Download size={20} />
            应用此预设
          </button>
        </div>
      </div>
    </div>
  );
};

export default PresetDetailPage;
