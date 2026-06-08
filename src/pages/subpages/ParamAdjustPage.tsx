import React, { useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Aperture, Timer, Thermometer, Sun } from 'lucide-react';

const PARAM_CONFIG = [
  {
    key: 'iso' as const,
    label: 'ISO 感光度',
    icon: Aperture,
    min: 50,
    max: 12800,
    step: 50,
    format: (v: number) => String(v),
  },
  {
    key: 'shutter' as const,
    label: '快门速度',
    icon: Timer,
    min: 1,
    max: 1000,
    step: 1,
    format: (v: number) => (v >= 1000 ? `${v / 1000}s` : `1/${v}s`),
  },
  {
    key: 'aperture' as const,
    label: '光圈',
    icon: Aperture,
    min: 1.4,
    max: 22,
    step: 0.1,
    format: (v: number) => `f/${v.toFixed(1)}`,
  },
  {
    key: 'wb' as const,
    label: '白平衡',
    icon: Thermometer,
    min: 3000,
    max: 8000,
    step: 100,
    format: (v: number) => `${v}K`,
  },
];

const QUICK_PRESETS = [
  { name: '人像', iso: 200, shutter: 125, aperture: 2.8, wb: 5500 },
  { name: '风景', iso: 100, shutter: 60, aperture: 8, wb: 5600 },
  { name: '夜景', iso: 3200, shutter: 30, aperture: 2.8, wb: 4000 },
  { name: '运动', iso: 800, shutter: 500, aperture: 4, wb: 5500 },
];

const ParamAdjustPage: React.FC = () => {
  const { goBack, cameraParams, setCameraParam } = useAppStore();

  const applyPreset = useCallback(
    (preset: typeof QUICK_PRESETS[0]) => {
      setCameraParam('iso', preset.iso);
      setCameraParam('shutter', preset.shutter);
      setCameraParam('aperture', preset.aperture);
      setCameraParam('wb', preset.wb);
    },
    [setCameraParam],
  );

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: 'var(--color-bg-primary)', color: 'var(--color-text-primary)' }}
    >
      {/* 顶部标题栏 */}
      <div
        className="sticky top-0 z-50 backdrop-blur-md"
        style={{ background: 'rgba(10,10,10,0.92)', borderBottom: '1px solid var(--color-border-light)' }}
      >
        <div className="flex items-center gap-3 px-4 py-3">
          <button onClick={goBack} aria-label="返回上一页" className="p-2 -ml-2 rounded-full transition-colors" style={{ color: 'var(--color-text-primary)' }}>
            <ArrowLeft size={20} />
          </button>
          <div className="flex-1">
            <h1 className="text-lg font-bold">参数精细调节</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>ISO、快门、光圈、白平衡精确控制</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 快捷档位 */}
        <div className="flex gap-2 mb-4 animate-liquid-slide-up">
          {QUICK_PRESETS.map((preset) => (
            <button
              key={preset.name}
              onClick={() => applyPreset(preset)}
              aria-label={`应用${preset.name}预设`}
              className="flex-1 py-2 rounded-xl text-sm font-medium transition-liquid"
              style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', color: 'var(--color-text-secondary)' }}
            >
              {preset.name}
            </button>
          ))}
        </div>

        {/* 参数滑块 */}
        <div className="space-y-3">
          {PARAM_CONFIG.map((param, index) => {
            const Icon = param.icon;
            const value = cameraParams[param.key];
            const percent = ((value - param.min) / (param.max - param.min)) * 100;
            return (
              <div
                key={param.key}
                className="rounded-2xl p-4 animate-liquid-slide-up"
                style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: `${index * 60}ms` }}
              >
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: 'var(--color-accent-primary-muted)' }}>
                    <Icon size={16} style={{ color: 'var(--color-accent-primary)' }} />
                  </div>
                  <span className="text-sm font-medium flex-1">{param.label}</span>
                  <span className="text-sm font-bold tabular-nums" style={{ color: 'var(--color-accent-primary)' }}>
                    {param.format(value)}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  step={param.step}
                  value={value}
                  onChange={(e) => setCameraParam(param.key, parseFloat(e.target.value))}
                  aria-label={`${param.label}调节`}
                  className="w-full h-2 rounded-full appearance-none cursor-pointer"
                  style={{
                    background: `linear-gradient(to right, var(--color-accent-primary) 0%, var(--color-accent-primary) ${percent}%, var(--color-border-light) ${percent}%, var(--color-border-light) 100%)`,
                  }}
                />
                <div className="flex justify-between mt-1">
                  <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>{param.format(param.min)}</span>
                  <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>{param.format(param.max)}</span>
                </div>
              </div>
            );
          })}
        </div>

        {/* 曝光指示器 */}
        <div
          className="mt-4 rounded-2xl p-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '240ms' }}
        >
          <div className="flex items-center gap-2 mb-2">
            <Sun size={14} style={{ color: 'var(--color-accent-primary)' }} />
            <span className="text-xs font-medium" style={{ color: 'var(--color-text-tertiary)' }}>曝光指示器</span>
          </div>
          <div className="relative h-3 rounded-full overflow-hidden" style={{ background: 'var(--color-bg-tertiary)' }}>
            <div
              className="absolute top-0 bottom-0 w-0.5 transition-all"
              style={{
                background: 'var(--color-accent-primary)',
                left: `${Math.min(100, Math.max(0, ((cameraParams.iso - 50) / (12800 - 50)) * 100))}%`,
              }}
            />
          </div>
          <div className="flex justify-between mt-1">
            <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>-3</span>
            <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>0</span>
            <span className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>+3</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default React.memo(ParamAdjustPage);
