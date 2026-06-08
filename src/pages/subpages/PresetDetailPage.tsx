import React from 'react';
import { useAppStore, HASSelBLAD_ORANGE } from '../../store/appStore';
import { ChevronLeft, Heart, Share2, Download, Lightbulb, ChevronDown, ChevronUp, Camera, Aperture, Timer, Sun, MapPin, Calendar, Gauge } from 'lucide-react';

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
  const [showExifInfo, setShowExifInfo] = React.useState(true);
  const [showHsl, setShowHsl] = React.useState(false);
  const [showSplitToning, setShowSplitToning] = React.useState(false);

  // 如果没有选中预设，返回首页
  if (!selectedPreset) {
    goBack();
    return null;
  }

  const isFavorite = isPresetFavorite(selectedPreset.id);

  // 模拟 EXIF 数据（如果预设没有提供，使用默认值）
  const exifData = selectedPreset.exif || {
    camera: 'OPPO Find X8 Ultra',
    lens: '哈苏主摄 23mm',
    focalLength: '23mm',
    aperture: 'f/1.8',
    shutter: '1/125s',
    iso: 100,
    dateTime: '2025-01-15 14:30',
    location: '北京·故宫',
  };

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

  // 参数卡片组件
  const ParamCard: React.FC<{ label: string; value: number | undefined; unit?: string }> = ({ label, value, unit = '' }) => (
    <div 
      className="p-4 rounded-xl text-center"
      style={{ background: 'rgba(255, 255, 255, 0.05)' }}
    >
      <div 
        className="text-xs mb-2"
        style={{ color: 'rgba(255, 255, 255, 0.5)' }}
      >
        {label}
      </div>
      <div 
        className="text-lg font-bold"
        style={{ color: '#FFFFFF' }}
      >
        {getParamValue(value, unit)}
      </div>
    </div>
  );

  // HSL颜色名称映射
  const hslColorNames: Record<string, string> = {
    Red: '红色',
    Orange: '橙色',
    Yellow: '黄色',
    Green: '绿色',
    Cyan: '青色',
    Blue: '蓝色',
    Purple: '紫色',
    Magenta: '洋红',
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

        {/* EXIF信息 - 可折叠 */}
        <div className="px-4 mb-4">
          <button
            onClick={() => setShowExifInfo(!showExifInfo)}
            className="w-full flex items-center justify-between p-4 rounded-xl"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div className="flex items-center gap-3">
              <Camera size={20} style={{ color: HASSelBLAD_ORANGE }} />
              <span 
                className="font-medium"
                style={{ color: HASSelBLAD_ORANGE }}
              >
                EXIF 信息
              </span>
            </div>
            {showExifInfo ? (
              <ChevronUp size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            ) : (
              <ChevronDown size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            )}
          </button>
          
          {showExifInfo && (
            <div 
              className="mt-2 p-4 rounded-xl"
              style={{ 
                background: 'rgba(255, 255, 255, 0.03)',
              }}
            >
              {/* 设备信息行 */}
              <div className="grid grid-cols-2 gap-3 mb-3">
                {/* 拍摄设备 */}
                <div className="flex items-center gap-2">
                  <div 
                    className="w-8 h-8 rounded-lg flex items-center justify-center"
                    style={{ background: `${HASSelBLAD_ORANGE}20` }}
                  >
                    <Camera size={16} style={{ color: HASSelBLAD_ORANGE }} />
                  </div>
                  <div>
                    <p 
                      className="text-xs"
                      style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                    >
                      设备
                    </p>
                    <p 
                      className="text-sm font-medium"
                      style={{ color: '#FFFFFF' }}
                    >
                      {exifData.camera}
                    </p>
                  </div>
                </div>

                {/* 镜头 */}
                <div className="flex items-center gap-2">
                  <div 
                    className="w-8 h-8 rounded-lg flex items-center justify-center"
                    style={{ background: `${HASSelBLAD_ORANGE}20` }}
                  >
                    <Aperture size={16} style={{ color: HASSelBLAD_ORANGE }} />
                  </div>
                  <div>
                    <p 
                      className="text-xs"
                      style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                    >
                      镜头
                    </p>
                    <p 
                      className="text-sm font-medium"
                      style={{ color: '#FFFFFF' }}
                    >
                      {exifData.lens}
                    </p>
                  </div>
                </div>
              </div>

              {/* 参数信息行 */}
              <div className="grid grid-cols-3 gap-3 mb-3">
                {/* 焦距 */}
                <div 
                  className="p-3 rounded-xl text-center"
                  style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                >
                  <Gauge size={14} style={{ color: 'rgba(255, 255, 255, 0.5)' }} className="mx-auto mb-1" />
                  <p 
                    className="text-xs mb-1"
                    style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                  >
                    焦距
                  </p>
                  <p 
                    className="text-sm font-semibold"
                    style={{ color: '#FFFFFF' }}
                  >
                    {exifData.focalLength}
                  </p>
                </div>

                {/* 光圈 */}
                <div 
                  className="p-3 rounded-xl text-center"
                  style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                >
                  <Aperture size={14} style={{ color: 'rgba(255, 255, 255, 0.5)' }} className="mx-auto mb-1" />
                  <p 
                    className="text-xs mb-1"
                    style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                  >
                    光圈
                  </p>
                  <p 
                    className="text-sm font-semibold"
                    style={{ color: '#FFFFFF' }}
                  >
                    {exifData.aperture}
                  </p>
                </div>

                {/* 快门 */}
                <div 
                  className="p-3 rounded-xl text-center"
                  style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                >
                  <Timer size={14} style={{ color: 'rgba(255, 255, 255, 0.5)' }} className="mx-auto mb-1" />
                  <p 
                    className="text-xs mb-1"
                    style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                  >
                    快门
                  </p>
                  <p 
                    className="text-sm font-semibold"
                    style={{ color: '#FFFFFF' }}
                  >
                    {exifData.shutter}
                  </p>
                </div>
              </div>

              {/* ISO和拍摄时间 */}
              <div className="grid grid-cols-2 gap-3 mb-3">
                {/* ISO */}
                <div className="flex items-center gap-2">
                  <div 
                    className="w-8 h-8 rounded-lg flex items-center justify-center"
                    style={{ background: `${HASSelBLAD_ORANGE}20` }}
                  >
                    <Sun size={16} style={{ color: HASSelBLAD_ORANGE }} />
                  </div>
                  <div>
                    <p 
                      className="text-xs"
                      style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                    >
                      ISO
                    </p>
                    <p 
                      className="text-sm font-medium"
                      style={{ color: '#FFFFFF' }}
                    >
                      {exifData.iso}
                    </p>
                  </div>
                </div>

                {/* 拍摄时间 */}
                <div className="flex items-center gap-2">
                  <div 
                    className="w-8 h-8 rounded-lg flex items-center justify-center"
                    style={{ background: `${HASSelBLAD_ORANGE}20` }}
                  >
                    <Calendar size={16} style={{ color: HASSelBLAD_ORANGE }} />
                  </div>
                  <div>
                    <p 
                      className="text-xs"
                      style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                    >
                      时间
                    </p>
                    <p 
                      className="text-sm font-medium"
                      style={{ color: '#FFFFFF' }}
                    >
                      {exifData.dateTime}
                    </p>
                  </div>
                </div>
              </div>

              {/* 拍摄地点 */}
              {exifData.location && (
                <div className="flex items-center gap-2">
                  <div 
                    className="w-8 h-8 rounded-lg flex items-center justify-center"
                    style={{ background: `${HASSelBLAD_ORANGE}20` }}
                  >
                    <MapPin size={16} style={{ color: HASSelBLAD_ORANGE }} />
                  </div>
                  <div>
                    <p 
                      className="text-xs"
                      style={{ color: 'rgba(255, 255, 255, 0.5)' }}
                    >
                      地点
                    </p>
                    <p 
                      className="text-sm font-medium"
                      style={{ color: '#FFFFFF' }}
                    >
                      {exifData.location}
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 基础调整参数组 */}
        <div className="px-4 mb-4">
          <h2 
            className="text-lg font-bold mb-3"
            style={{ color: '#FF6B35' }}
          >
            基础调整
          </h2>
          <div className="grid grid-cols-2 gap-3">
            <ParamCard label="曝光" value={selectedPreset.exposure} />
            <ParamCard label="对比度" value={selectedPreset.contrast} />
            <ParamCard label="高光" value={selectedPreset.highlights} />
            <ParamCard label="阴影" value={selectedPreset.shadows} />
            <ParamCard label="白色" value={selectedPreset.whites} />
            <ParamCard label="黑色" value={selectedPreset.blacks} />
          </div>
        </div>

        {/* 色彩调整参数组 */}
        <div className="px-4 mb-4">
          <h2 
            className="text-lg font-bold mb-3"
            style={{ color: '#FF6B35' }}
          >
            色彩调整
          </h2>
          <div className="grid grid-cols-2 gap-3">
            <ParamCard label="饱和度" value={selectedPreset.saturation} />
            <ParamCard label="自然饱和度" value={selectedPreset.vibrance} />
            <ParamCard label="色温" value={selectedPreset.warmth} />
            <ParamCard label="色调" value={0} />
          </div>
        </div>

        {/* 效果调整参数组 */}
        <div className="px-4 mb-4">
          <h2 
            className="text-lg font-bold mb-3"
            style={{ color: '#FF6B35' }}
          >
            效果调整
          </h2>
          <div className="grid grid-cols-2 gap-3">
            <ParamCard label="清晰度" value={selectedPreset.clarity ?? selectedPreset.vibrance} />
            <ParamCard label="锐度" value={selectedPreset.sharpness} />
            <ParamCard label="去雾" value={selectedPreset.dehaze} />
            <ParamCard label="暗角" value={selectedPreset.vignette} />
            <ParamCard label="颗粒" value={selectedPreset.grain} />
          </div>
        </div>

        {/* HSL调整 - 可折叠 */}
        <div className="px-4 mb-4">
          <button
            onClick={() => setShowHsl(!showHsl)}
            className="w-full flex items-center justify-between p-4 rounded-xl"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div className="flex items-center gap-3">
              <span 
                className="font-bold"
                style={{ color: '#FF6B35' }}
              >
                HSL调整
              </span>
            </div>
            {showHsl ? (
              <ChevronUp size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            ) : (
              <ChevronDown size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            )}
          </button>
          
          {showHsl && (
            <div 
              className="mt-2 p-4 rounded-xl"
              style={{ background: 'rgba(255, 255, 255, 0.03)' }}
            >
              {['Red', 'Orange', 'Yellow', 'Green', 'Cyan', 'Blue', 'Purple', 'Magenta'].map(color => {
                const hueKey = `hue${color}` as keyof typeof selectedPreset.hsl;
                const satKey = `sat${color}` as keyof typeof selectedPreset.hsl;
                const lumKey = `lum${color}` as keyof typeof selectedPreset.hsl;
                return (
                  <div key={color} className="mb-3 last:mb-0">
                    <div 
                      className="text-sm font-medium mb-2"
                      style={{ color: 'rgba(255, 255, 255, 0.8)' }}
                    >
                      {hslColorNames[color]}
                    </div>
                    <div className="grid grid-cols-3 gap-2">
                      <div 
                        className="p-2 rounded-lg text-center"
                        style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                      >
                        <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>色相</div>
                        <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                          {getParamValue(selectedPreset.hsl[hueKey], '°')}
                        </div>
                      </div>
                      <div 
                        className="p-2 rounded-lg text-center"
                        style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                      >
                        <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>饱和</div>
                        <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                          {getParamValue(selectedPreset.hsl[satKey])}
                        </div>
                      </div>
                      <div 
                        className="p-2 rounded-lg text-center"
                        style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                      >
                        <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>亮度</div>
                        <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                          {getParamValue(selectedPreset.hsl[lumKey])}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 分离色调 - 可折叠 */}
        <div className="px-4 mb-4">
          <button
            onClick={() => setShowSplitToning(!showSplitToning)}
            className="w-full flex items-center justify-between p-4 rounded-xl"
            style={{ background: 'rgba(255, 255, 255, 0.05)' }}
          >
            <div className="flex items-center gap-3">
              <span 
                className="font-bold"
                style={{ color: '#FF6B35' }}
              >
                分离色调
              </span>
            </div>
            {showSplitToning ? (
              <ChevronUp size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            ) : (
              <ChevronDown size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
            )}
          </button>
          
          {showSplitToning && (
            <div 
              className="mt-2 p-4 rounded-xl"
              style={{ background: 'rgba(255, 255, 255, 0.03)' }}
            >
              <div className="mb-3">
                <div 
                  className="text-sm font-medium mb-2"
                  style={{ color: 'rgba(255, 255, 255, 0.8)' }}
                >
                  高光
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div 
                    className="p-2 rounded-lg text-center"
                    style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                  >
                    <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>色相</div>
                    <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                      {getParamValue(selectedPreset.splitToning.highlightHue, '°')}
                    </div>
                  </div>
                  <div 
                    className="p-2 rounded-lg text-center"
                    style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                  >
                    <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>饱和</div>
                    <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                      {getParamValue(selectedPreset.splitToning.highlightSat)}
                    </div>
                  </div>
                </div>
              </div>
              <div className="mb-3">
                <div 
                  className="text-sm font-medium mb-2"
                  style={{ color: 'rgba(255, 255, 255, 0.8)' }}
                >
                  阴影
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div 
                    className="p-2 rounded-lg text-center"
                    style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                  >
                    <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>色相</div>
                    <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                      {getParamValue(selectedPreset.splitToning.shadowHue, '°')}
                    </div>
                  </div>
                  <div 
                    className="p-2 rounded-lg text-center"
                    style={{ background: 'rgba(255, 255, 255, 0.05)' }}
                  >
                    <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>饱和</div>
                    <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                      {getParamValue(selectedPreset.splitToning.shadowSat)}
                    </div>
                  </div>
                </div>
              </div>
              <div 
                className="p-2 rounded-lg text-center"
                style={{ background: 'rgba(255, 255, 255, 0.05)' }}
              >
                <div className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>平衡</div>
                <div className="text-sm font-bold" style={{ color: '#FFFFFF' }}>
                  {getParamValue(selectedPreset.splitToning.balance)}
                </div>
              </div>
            </div>
          )}
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