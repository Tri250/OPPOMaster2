import React, { useState, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Palette, Check, Shield } from 'lucide-react';

const COLOR_MODES = [
  {
    id: 'natural',
    name: '自然色彩',
    desc: '真实还原，忠于原色，适合日常记录',
    color: '#8BC34A',
    params: { saturation: 5, contrast: 5, warmth: 0, sharpness: 10 },
  },
  {
    id: 'portrait',
    name: '人像优化',
    desc: '柔美肤色，自然美化，人像首选',
    color: '#FF6B9D',
    params: { saturation: 8, contrast: 8, warmth: 3, sharpness: 15 },
  },
  {
    id: 'landscape',
    name: '风景增强',
    desc: '色彩鲜明，层次丰富，风光利器',
    color: '#4ECDC4',
    params: { saturation: 15, contrast: 12, warmth: 5, sharpness: 20 },
  },
  {
    id: 'classic',
    name: '经典胶片',
    desc: '模拟经典胶片质感，怀旧氛围',
    color: '#D4A574',
    params: { saturation: -5, contrast: 10, warmth: 8, sharpness: 12 },
  },
  {
    id: 'bw',
    name: '黑白',
    desc: '去除色彩干扰，专注光影表达',
    color: '#9E9E9E',
    params: { saturation: -100, contrast: 15, warmth: 0, sharpness: 18 },
  },
  {
    id: 'vivid',
    name: '鲜艳',
    desc: '高饱和高对比，视觉冲击力强',
    color: '#FF6B35',
    params: { saturation: 25, contrast: 15, warmth: 3, sharpness: 22 },
  },
];

const HasselbladPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [selectedMode, setSelectedMode] = useState('natural');

  const handleSelectMode = useCallback(
    (mode: (typeof COLOR_MODES)[0]) => {
      setSelectedMode(mode.id);
      const entries = Object.entries(mode.params) as [keyof typeof mode.params, number][];
      entries.forEach(([key, value]) => {
        setAiParam(key, value);
      });
    },
    [setAiParam],
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
            <h1 className="text-lg font-bold">哈苏色彩科学</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>Hasselblad Natural Colour Solution</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* HNCS 认证标识 */}
        <div
          className="rounded-2xl p-4 flex items-center gap-3 mb-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-accent-primary-muted)', border: '1px solid var(--color-border-accent)' }}
        >
          <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary)' }}>
            <Shield size={20} style={{ color: '#fff' }} />
          </div>
          <div>
            <p className="text-sm font-bold" style={{ color: 'var(--color-accent-primary)' }}>HNCS 认证</p>
            <p className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>Hasselblad Natural Colour Solution</p>
          </div>
        </div>

        {/* 色彩模式列表 */}
        <div className="space-y-3">
          {COLOR_MODES.map((mode, index) => {
            const isSelected = selectedMode === mode.id;
            return (
              <button
                key={mode.id}
                onClick={() => handleSelectMode(mode)}
                aria-label={`选择${mode.name}色彩模式`}
                className="w-full rounded-2xl p-4 text-left transition-liquid animate-liquid-slide-up"
                style={{
                  background: isSelected ? 'var(--color-accent-primary-muted)' : 'var(--color-bg-secondary)',
                  border: `1px solid ${isSelected ? 'var(--color-accent-primary)' : 'var(--color-border-light)'}`,
                  animationDelay: `${index * 60}ms`,
                }}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                    style={{ background: `${mode.color}20` }}
                  >
                    <Palette size={20} style={{ color: mode.color }} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold">{mode.name}</p>
                      {isSelected && (
                        <Check size={14} style={{ color: 'var(--color-accent-primary)' }} />
                      )}
                    </div>
                    <p className="text-xs mt-0.5" style={{ color: 'var(--color-text-tertiary)' }}>
                      {mode.desc}
                    </p>
                  </div>
                  <div
                    className="w-6 h-6 rounded-full flex-shrink-0"
                    style={{ background: mode.color }}
                  />
                </div>
                {/* 推荐参数 */}
                <div className="flex gap-2 mt-3 ml-13">
                  {Object.entries(mode.params).map(([key, value]) => (
                    <span
                      key={key}
                      className="text-[10px] px-1.5 py-0.5 rounded"
                      style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-tertiary)' }}
                    >
                      {key === 'saturation'
                        ? '饱和'
                        : key === 'contrast'
                          ? '对比'
                          : key === 'warmth'
                            ? '色温'
                            : '锐度'}
                      :{value > 0 ? '+' : ''}
                      {value}
                    </span>
                  ))}
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default React.memo(HasselbladPage);
