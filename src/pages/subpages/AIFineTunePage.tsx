import React, { useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, RotateCcw, Check, Sun, Contrast, SunDim, Thermometer, Focus } from 'lucide-react';

const SLIDER_CONFIG = [
  { key: 'saturation' as const, label: '饱和度', icon: Sun, min: -100, max: 100 },
  { key: 'contrast' as const, label: '对比度', icon: Contrast, min: -100, max: 100 },
  { key: 'brightness' as const, label: '亮度', icon: SunDim, min: -100, max: 100 },
  { key: 'warmth' as const, label: '色温', icon: Thermometer, min: -100, max: 100 },
  { key: 'sharpness' as const, label: '锐度', icon: Focus, min: 0, max: 100 },
];

const AIFineTunePage: React.FC = () => {
  const { goBack, aiParams, setAiParam } = useAppStore();

  const handleReset = useCallback(() => {
    setAiParam('saturation', 0);
    setAiParam('contrast', 0);
    setAiParam('brightness', 0);
    setAiParam('warmth', 0);
    setAiParam('sharpness', 0);
  }, [setAiParam]);

  const handleApply = useCallback(() => {
    // 参数已通过 setAiParam 实时同步到 store，此处可扩展保存逻辑
  }, []);

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
            <h1 className="text-lg font-bold">AI 微调</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
              专业色彩优化引擎
            </p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        <div className="space-y-3">
          {SLIDER_CONFIG.map((slider, index) => {
            const Icon = slider.icon;
            const value = aiParams[slider.key];
            return (
              <div
                key={slider.key}
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
                    <span className="text-sm font-medium">{slider.label}</span>
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
                  min={slider.min}
                  max={slider.max}
                  value={value}
                  onChange={(e) => setAiParam(slider.key, parseInt(e.target.value))}
                  aria-label={`${slider.label}调节`}
                  className="w-full h-2 rounded-full appearance-none cursor-pointer"
                  style={{
                    background: `linear-gradient(to right, var(--color-accent-primary) 0%, var(--color-accent-primary) ${((value - slider.min) / (slider.max - slider.min)) * 100}%, var(--color-border-light) ${((value - slider.min) / (slider.max - slider.min)) * 100}%, var(--color-border-light) 100%)`,
                  }}
                />
                <div className="flex justify-between mt-1">
                  <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
                    {slider.min}
                  </span>
                  <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
                    {slider.max}
                  </span>
                </div>
              </div>
            );
          })}
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
