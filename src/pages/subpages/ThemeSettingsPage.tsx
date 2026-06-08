import React, { useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Check } from 'lucide-react';

const THEMES: { id: 'hasselblad' | 'oppo' | 'vivo' | 'realme' | 'honor' | 'xiaomi'; name: string; color: string }[] = [
  { id: 'hasselblad', name: '哈苏橙', color: '#FF6B35' },
  { id: 'oppo', name: 'OPPO 绿', color: '#1BA784' },
  { id: 'vivo', name: 'vivo 蓝', color: '#4A90D9' },
  { id: 'realme', name: 'realme 黄', color: '#F5C542' },
  { id: 'honor', name: '荣耀蓝', color: '#2D6BE6' },
  { id: 'xiaomi', name: '小米橙', color: '#FF6900' },
];

const ThemeSettingsPage: React.FC = () => {
  const { goBack, theme, setTheme } = useAppStore();

  const handleSelectTheme = useCallback(
    (themeId: 'hasselblad' | 'oppo' | 'vivo' | 'realme' | 'honor' | 'xiaomi') => {
      setTheme(themeId);
    },
    [setTheme],
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
            <h1 className="text-lg font-bold">主题设置</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>选择你喜欢的主题风格</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        <div className="grid grid-cols-2 gap-3">
          {THEMES.map((t, index) => {
            const isActive = theme === t.id;
            return (
              <button
                key={t.id}
                onClick={() => handleSelectTheme(t.id)}
                aria-label={`选择${t.name}主题`}
                className="rounded-2xl p-4 text-left transition-liquid animate-liquid-slide-up"
                style={{
                  background: isActive ? `${t.color}15` : 'var(--color-bg-secondary)',
                  border: `1px solid ${isActive ? t.color : 'var(--color-border-light)'}`,
                  animationDelay: `${index * 60}ms`,
                }}
              >
                <div className="flex items-center gap-3 mb-3">
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center"
                    style={{ background: t.color }}
                  >
                    {isActive && <Check size={18} style={{ color: '#fff' }} />}
                  </div>
                  <span className="text-sm font-bold">{t.name}</span>
                </div>
                <div className="flex gap-1">
                  <div className="flex-1 h-2 rounded-full" style={{ background: t.color }} />
                  <div className="flex-1 h-2 rounded-full" style={{ background: `${t.color}80` }} />
                  <div className="flex-1 h-2 rounded-full" style={{ background: `${t.color}40` }} />
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default React.memo(ThemeSettingsPage);
