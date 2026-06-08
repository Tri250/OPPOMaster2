import React, { useState, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Type, AlignLeft, AlignRight, Check } from 'lucide-react';

const TEMPLATES = [
  { id: 'default', name: '标准' },
  { id: 'minimal', name: '极简' },
  { id: 'detailed', name: '详细' },
  { id: 'brand', name: '品牌' },
];

const POSITIONS = [
  { id: 'bottom-left', name: '左下', icon: AlignLeft },
  { id: 'bottom-right', name: '右下', icon: AlignRight },
  { id: 'top-left', name: '左上', icon: AlignLeft },
  { id: 'top-right', name: '右上', icon: AlignRight },
] as const;

const WatermarkPage: React.FC = () => {
  const { goBack, watermarkSettings, setWatermarkSetting } = useAppStore();
  const [localText, setLocalText] = useState(watermarkSettings.customText);

  const handleTextChange = useCallback(
    (text: string) => {
      setLocalText(text);
      setWatermarkSetting('customText', text);
    },
    [setWatermarkSetting],
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
            <h1 className="text-lg font-bold">水印编辑器</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>专业水印模板与自定义</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 水印开关 */}
        <div
          className="rounded-2xl p-4 flex items-center justify-between animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)' }}
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary-muted)' }}>
              <Type size={20} style={{ color: 'var(--color-accent-primary)' }} />
            </div>
            <div>
              <p className="text-sm font-medium">启用水印</p>
              <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
                {watermarkSettings.enabled ? '水印已开启' : '水印已关闭'}
              </p>
            </div>
          </div>
          <button
            onClick={() => setWatermarkSetting('enabled', !watermarkSettings.enabled)}
            aria-label={`${watermarkSettings.enabled ? '关闭' : '开启'}水印`}
            className="w-12 h-7 rounded-full relative transition-colors flex-shrink-0"
            style={{ background: watermarkSettings.enabled ? 'var(--color-accent-primary)' : 'var(--color-border-medium)' }}
          >
            <div
              className="absolute top-0.5 w-6 h-6 rounded-full bg-white transition-transform"
              style={{ left: watermarkSettings.enabled ? '22px' : '2px' }}
            />
          </button>
        </div>

        {/* 模板选择 */}
        <div className="mt-4 animate-liquid-slide-up" style={{ animationDelay: '60ms' }}>
          <p className="text-xs font-medium mb-2" style={{ color: 'var(--color-text-tertiary)' }}>模板选择</p>
          <div className="grid grid-cols-4 gap-2">
            {TEMPLATES.map((tpl) => (
              <button
                key={tpl.id}
                onClick={() => setWatermarkSetting('template', tpl.id)}
                aria-label={`选择${tpl.name}模板`}
                className="py-2.5 rounded-xl text-sm font-medium transition-liquid"
                style={{
                  background: watermarkSettings.template === tpl.id ? 'var(--color-accent-primary)' : 'var(--color-bg-secondary)',
                  color: watermarkSettings.template === tpl.id ? '#fff' : 'var(--color-text-secondary)',
                  border: `1px solid ${watermarkSettings.template === tpl.id ? 'var(--color-accent-primary)' : 'var(--color-border-light)'}`,
                }}
              >
                {tpl.name}
              </button>
            ))}
          </div>
        </div>

        {/* 自定义文字 */}
        <div className="mt-4 animate-liquid-slide-up" style={{ animationDelay: '120ms' }}>
          <p className="text-xs font-medium mb-2" style={{ color: 'var(--color-text-tertiary)' }}>自定义文字</p>
          <input
            type="text"
            value={localText}
            onChange={(e) => handleTextChange(e.target.value)}
            aria-label="水印自定义文字"
            placeholder="输入水印文字"
            className="w-full px-4 py-3 rounded-xl text-sm outline-none transition-liquid"
            style={{
              background: 'var(--color-bg-secondary)',
              border: '1px solid var(--color-border-light)',
              color: 'var(--color-text-primary)',
            }}
          />
        </div>

        {/* 位置选择 */}
        <div className="mt-4 animate-liquid-slide-up" style={{ animationDelay: '180ms' }}>
          <p className="text-xs font-medium mb-2" style={{ color: 'var(--color-text-tertiary)' }}>水印位置</p>
          <div className="grid grid-cols-4 gap-2">
            {POSITIONS.map((pos) => {
              const Icon = pos.icon;
              const isActive = watermarkSettings.position === pos.id;
              return (
                <button
                  key={pos.id}
                  onClick={() => setWatermarkSetting('position', pos.id)}
                  aria-label={`水印位置${pos.name}`}
                  className="py-3 rounded-xl flex flex-col items-center gap-1 transition-liquid"
                  style={{
                    background: isActive ? 'var(--color-accent-primary-muted)' : 'var(--color-bg-secondary)',
                    border: `1px solid ${isActive ? 'var(--color-accent-primary)' : 'var(--color-border-light)'}`,
                  }}
                >
                  <Icon size={16} style={{ color: isActive ? 'var(--color-accent-primary)' : 'var(--color-text-tertiary)' }} />
                  <span className="text-xs" style={{ color: isActive ? 'var(--color-accent-primary)' : 'var(--color-text-tertiary)' }}>
                    {pos.name}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* 预览 */}
        <div
          className="mt-4 rounded-2xl p-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '240ms' }}
        >
          <p className="text-xs font-medium mb-2" style={{ color: 'var(--color-text-tertiary)' }}>预览</p>
          <div className="relative aspect-video rounded-xl overflow-hidden" style={{ background: 'var(--color-bg-tertiary)' }}>
            <div className="absolute inset-0 flex items-center justify-center" style={{ color: 'var(--color-text-tertiary)' }}>
              <span className="text-xs">图片预览区</span>
            </div>
            {watermarkSettings.enabled && (
              <div
                className="absolute text-xs font-medium"
                style={{
                  [watermarkSettings.position.includes('top') ? 'top' : 'bottom']: '12px',
                  [watermarkSettings.position.includes('left') ? 'left' : 'right']: '12px',
                  color: 'rgba(255,255,255,0.8)',
                  textShadow: '0 1px 3px rgba(0,0,0,0.5)',
                }}
              >
                {watermarkSettings.customText}
              </div>
            )}
          </div>
        </div>

        {/* 应用按钮 */}
        <button
          onClick={goBack}
          aria-label="保存水印设置"
          className="w-full mt-6 py-3.5 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid mb-6"
          style={{ background: 'var(--color-accent-primary)', color: '#fff' }}
        >
          <Check size={18} />
          保存设置
        </button>
      </div>
    </div>
  );
};

export default React.memo(WatermarkPage);
