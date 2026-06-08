import React, { useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, RotateCcw, Check, Sun, Contrast, SunDim, Thermometer, Focus, Sparkles, Droplets, Circle, Layers, Palette, Grid3X3 } from 'lucide-react';

// 基础调整参数配置
const BASIC_SLIDER_CONFIG = [
  { key: 'exposure' as const, label: '曝光', icon: Sun, min: -100, max: 100 },
  { key: 'contrast' as const, label: '对比度', icon: Contrast, min: -100, max: 100 },
  { key: 'brightness' as const, label: '亮度', icon: SunDim, min: -100, max: 100 },
  { key: 'highlights' as const, label: '高光', icon: Sparkles, min: -100, max: 100 },
  { key: 'shadows' as const, label: '阴影', icon: SunDim, min: -100, max: 100 },
  { key: 'whites' as const, label: '白色', icon: Sun, min: -100, max: 100 },
  { key: 'blacks' as const, label: '黑色', icon: SunDim, min: -100, max: 100 },
];

// 色彩调整参数配置
const COLOR_SLIDER_CONFIG = [
  { key: 'saturation' as const, label: '饱和度', icon: Palette, min: -100, max: 100 },
  { key: 'vibrance' as const, label: '自然饱和度', icon: Sparkles, min: -100, max: 100 },
  { key: 'warmth' as const, label: '色温', icon: Thermometer, min: -100, max: 100 },
];

// 效果调整参数配置
const EFFECT_SLIDER_CONFIG = [
  { key: 'clarity' as const, label: '清晰度', icon: Focus, min: -100, max: 100 },
  { key: 'sharpness' as const, label: '锐度', icon: Focus, min: 0, max: 100 },
  { key: 'dehaze' as const, label: '去雾', icon: Droplets, min: 0, max: 100 },
  { key: 'vignette' as const, label: '暗角', icon: Circle, min: 0, max: 100 },
  { key: 'grain' as const, label: '颗粒', icon: Grid3X3, min: 0, max: 100 },
];

const AIFineTunePage: React.FC = () => {
  const { goBack, aiParams, setAiParam } = useAppStore();

  const handleReset = useCallback(() => {
    setAiParam('saturation', 0);
    setAiParam('contrast', 0);
    setAiParam('brightness', 0);
    setAiParam('warmth', 0);
    setAiParam('sharpness', 0);
    setAiParam('exposure', 0);
    setAiParam('highlights', 0);
    setAiParam('shadows', 0);
    setAiParam('whites', 0);
    setAiParam('blacks', 0);
    setAiParam('vibrance', 0);
    setAiParam('clarity', 0);
    setAiParam('dehaze', 0);
    setAiParam('vignette', 0);
    setAiParam('grain', 0);
  }, [setAiParam]);

  const handleApply = useCallback(() => {
    // 参数已通过 setAiParam 实时同步到 store，此处可扩展保存逻辑
  }, []);

  // 滑块组件
  const SliderItem: React.FC<{
    config: { key: string; label: string; icon: React.ComponentType<{ size: number; style?: React.CSSProperties }>; min: number; max: number };
    index: number;
  }> = ({ config, index }) => {
    const Icon = config.icon;
    const value = aiParams[config.key as keyof typeof aiParams] as number;
    return (
      <div
        className="rounded-2xl p-4 animate-liquid-slide-up"
        style={{
          background: 'var(--color-bg-secondary)',
          border: '1px solid var(--color-border-light)',
          animationDelay: `${index * 60}ms`,
        }}
      >
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <div
              className="w-8 h-8 rounded-lg flex items-center justify-center"
              style={{ background: 'var(--color-accent-primary-muted)' }}
            >
              <Icon size={16} style={{ color: 'var(--color-accent-primary)' }} />
            </div>
            <span className="text-sm font-medium">{config.label}</span>
          </div>
          <span
            className="text-sm font-bold tabular-nums"
            style={{ color: 'var(--color-accent-primary)' }}
          >
            {value > 0 ? '+' : ''}
            {value}
          </span>
        </div>
        <input
          type="range"
          min={config.min}
          max={config.max}
          value={value}
          onChange={(e) => setAiParam(config.key as keyof typeof aiParams, parseInt(e.target.value))}
          aria-label={`${config.label}调节`}
          className="w-full h-2 rounded-full appearance-none cursor-pointer"
          style={{
            background: `linear-gradient(to right, var(--color-accent-primary) 0%, var(--color-accent-primary) ${((value - config.min) / (config.max - config.min)) * 100}%, var(--color-border-light) ${((value - config.min) / (config.max - config.min)) * 100}%, var(--color-border-light) 100%)`,
          }}
        />
        <div className="flex justify-between mt-1">
          <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
            {config.min}
          </span>
          <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
            {config.max}
          </span>
        </div>
      </div>
    );
  };

  // 参数组标题组件
  const ParamGroupHeader: React.FC<{ title: string; icon: React.ComponentType<{ size: number; style?: React.CSSProperties }> }> = ({ title, icon: Icon }) => (
    <div className="flex items-center gap-2 mb-3 mt-4 first:mt-0">
      <Icon size={18} style={{ color: 'var(--color-accent-primary)' }} />
      <h2 className="text-base font-bold" style={{ color: 'var(--color-text-primary)' }}>
        {title}
      </h2>
    </div>
  );

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: 'var(--color-bg-primary)', color: 'var(--color-text-primary)' }}
    >
      {/* 顶部标题栏 */}
      <div
        className="sticky top-0 z-50 backdrop-blur-md"
        style={{
          background: 'rgba(10,10,10,0.92)',
          borderBottom: '1px solid var(--color-border-light)',
        }}
      >
        <div className="flex items-center gap-3 px-4 py-3">
          <button
            onClick={goBack}
            aria-label="返回上一页"
            className="p-2 -ml-2 rounded-full transition-colors"
            style={{ color: 'var(--color-text-primary)' }}
          >
            <ArrowLeft size={20} />
          </button>
          <div className="flex-1">
            <h1 className="text-lg font-bold">AI精调</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
              专业色彩优化引擎
            </p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 基础调整 */}
        <ParamGroupHeader title="基础调整" icon={Sun} />
        <div className="space-y-3">
          {BASIC_SLIDER_CONFIG.map((config, index) => (
            <SliderItem key={config.key} config={config} index={index} />
          ))}
        </div>

        {/* 色彩调整 */}
        <ParamGroupHeader title="色彩调整" icon={Palette} />
        <div className="space-y-3">
          {COLOR_SLIDER_CONFIG.map((config, index) => (
            <SliderItem key={config.key} config={config} index={index + BASIC_SLIDER_CONFIG.length} />
          ))}
        </div>

        {/* 效果调整 */}
        <ParamGroupHeader title="效果调整" icon={Layers} />
        <div className="space-y-3">
          {EFFECT_SLIDER_CONFIG.map((config, index) => (
            <SliderItem key={config.key} config={config} index={index + BASIC_SLIDER_CONFIG.length + COLOR_SLIDER_CONFIG.length} />
          ))}
        </div>

        {/* 操作按钮 */}
        <div className="flex gap-3 mt-6 pb-6">
          <button
            onClick={handleReset}
            aria-label="重置所有参数"
            className="flex-1 py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid"
            style={{
              border: '1px solid var(--color-border-medium)',
              color: 'var(--color-text-secondary)',
            }}
          >
            <RotateCcw size={16} />
            重置
          </button>
          <button
            onClick={handleApply}
            aria-label="应用参数"
            className="flex-1 py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid"
            style={{
              background: 'var(--color-accent-primary)',
              color: '#fff',
            }}
          >
            <Check size={16} />
            应用
          </button>
        </div>
      </div>
    </div>
  );
};

export default React.memo(AIFineTunePage);