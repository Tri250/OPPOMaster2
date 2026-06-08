import React, { useState, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Zap, Droplets, Focus, Wand2, Check } from 'lucide-react';

const SmartOptimizePage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [hdrEnabled, setHdrEnabled] = useState(false);
  const [denoiseEnabled, setDenoiseEnabled] = useState(false);
  const [sharpenEnabled, setSharpenEnabled] = useState(false);
  const [strength, setStrength] = useState(50);
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [isDone, setIsDone] = useState(false);

  const handleOneClickOptimize = useCallback(() => {
    setIsOptimizing(true);
    setIsDone(false);
    setTimeout(() => {
      const factor = strength / 100;
      if (hdrEnabled) {
        setAiParam('contrast', Math.round(15 * factor));
        setAiParam('brightness', Math.round(10 * factor));
      }
      if (denoiseEnabled) {
        setAiParam('sharpness', Math.round(5 * factor));
      }
      if (sharpenEnabled) {
        setAiParam('sharpness', Math.round(25 * factor));
      }
      setIsOptimizing(false);
      setIsDone(true);
      setTimeout(() => setIsDone(false), 2000);
    }, 1500);
  }, [hdrEnabled, denoiseEnabled, sharpenEnabled, strength, setAiParam]);

  const toggleItems = [
    { id: 'hdr', label: 'HDR 增强', desc: '扩展动态范围，保留更多细节', icon: Zap, enabled: hdrEnabled, onToggle: () => setHdrEnabled((v) => !v) },
    { id: 'denoise', label: '智能降噪', desc: 'AI 识别并消除噪点', icon: Droplets, enabled: denoiseEnabled, onToggle: () => setDenoiseEnabled((v) => !v) },
    { id: 'sharpen', label: '锐化增强', desc: '提升画面清晰度和质感', icon: Focus, enabled: sharpenEnabled, onToggle: () => setSharpenEnabled((v) => !v) },
  ];

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
            <h1 className="text-lg font-bold">AI优化</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>智能影像优化引擎</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 开关项 */}
        <div className="space-y-3">
          {toggleItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <div
                key={item.id}
                className="rounded-2xl p-4 flex items-center gap-4 animate-liquid-slide-up"
                style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: `${index * 60}ms` }}
              >
                <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary-muted)' }}>
                  <Icon size={20} style={{ color: 'var(--color-accent-primary)' }} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium">{item.label}</p>
                  <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>{item.desc}</p>
                </div>
                <button
                  onClick={item.onToggle}
                  aria-label={`${item.enabled ? '关闭' : '开启'}${item.label}`}
                  className="w-12 h-7 rounded-full relative transition-colors flex-shrink-0"
                  style={{ background: item.enabled ? 'var(--color-accent-primary)' : 'var(--color-border-medium)' }}
                >
                  <div
                    className="absolute top-0.5 w-6 h-6 rounded-full bg-white transition-transform"
                    style={{ left: item.enabled ? '22px' : '2px' }}
                  />
                </button>
              </div>
            );
          })}
        </div>

        {/* 优化强度滑块 */}
        <div
          className="mt-4 rounded-2xl p-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '180ms' }}
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-medium">优化强度</span>
            <span className="text-sm font-bold tabular-nums" style={{ color: 'var(--color-accent-primary)' }}>{strength}%</span>
          </div>
          <input
            type="range"
            min={0}
            max={100}
            value={strength}
            onChange={(e) => setStrength(parseInt(e.target.value))}
            aria-label="优化强度调节"
            className="w-full h-2 rounded-full appearance-none cursor-pointer"
            style={{
              background: `linear-gradient(to right, var(--color-accent-primary) 0%, var(--color-accent-primary) ${strength}%, var(--color-border-light) ${strength}%, var(--color-border-light) 100%)`,
            }}
          />
        </div>

        {/* 一键优化按钮 */}
        <button
          onClick={handleOneClickOptimize}
          disabled={isOptimizing || (!hdrEnabled && !denoiseEnabled && !sharpenEnabled)}
          aria-label="一键优化"
          className="w-full mt-6 py-3.5 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid"
          style={{
            background: isDone ? 'var(--color-success)' : 'var(--color-accent-primary)',
            color: '#fff',
            opacity: isOptimizing || (!hdrEnabled && !denoiseEnabled && !sharpenEnabled) ? 0.5 : 1,
          }}
        >
          {isOptimizing ? (
            <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : isDone ? (
            <Check size={18} />
          ) : (
            <Wand2 size={18} />
          )}
          {isOptimizing ? '优化中...' : isDone ? '优化完成' : '一键优化'}
        </button>
      </div>
    </div>
  );
};

export default React.memo(SmartOptimizePage);
