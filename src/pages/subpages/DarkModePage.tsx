import React, { useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Sun, Moon, Monitor, Check } from 'lucide-react';

const MODES: { id: 'system' | 'light' | 'dark'; name: string; desc: string; icon: typeof Monitor }[] = [
  { id: 'system', name: '跟随系统', desc: '自动适配系统深色模式设置', icon: Monitor },
  { id: 'light', name: '浅色模式', desc: '始终使用浅色界面', icon: Sun },
  { id: 'dark', name: '深色模式', desc: '始终使用深色界面', icon: Moon },
];

const DarkModePage: React.FC = () => {
  const { goBack, darkMode, setDarkMode } = useAppStore();

  const handleSelectMode = useCallback(
    (mode: 'system' | 'light' | 'dark') => {
      setDarkMode(mode);
    },
    [setDarkMode],
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
            <h1 className="text-lg font-bold">深色模式</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>选择界面显示模式</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        <div className="space-y-3">
          {MODES.map((mode, index) => {
            const Icon = mode.icon;
            const isActive = darkMode === mode.id;
            return (
              <button
                key={mode.id}
                onClick={() => handleSelectMode(mode.id)}
                aria-label={`选择${mode.name}`}
                className="w-full rounded-2xl p-4 text-left transition-liquid animate-liquid-slide-up"
                style={{
                  background: isActive ? 'var(--color-accent-primary-muted)' : 'var(--color-bg-secondary)',
                  border: `1px solid ${isActive ? 'var(--color-accent-primary)' : 'var(--color-border-light)'}`,
                  animationDelay: `${index * 60}ms`,
                }}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center"
                    style={{ background: isActive ? 'var(--color-accent-primary)' : 'var(--color-bg-tertiary)' }}
                  >
                    <Icon size={20} style={{ color: isActive ? '#fff' : 'var(--color-text-tertiary)' }} />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold">{mode.name}</p>
                      {isActive && <Check size={14} style={{ color: 'var(--color-accent-primary)' }} />}
                    </div>
                    <p className="text-xs mt-0.5" style={{ color: 'var(--color-text-tertiary)' }}>
                      {mode.desc}
                    </p>
                  </div>
                </div>
              </button>
            );
          })}
        </div>

        {/* 当前模式指示 */}
        <div
          className="mt-4 p-4 rounded-2xl animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '180ms' }}
        >
          <p className="text-xs font-medium" style={{ color: 'var(--color-text-tertiary)' }}>
            当前模式
          </p>
          <p className="text-sm font-bold mt-1">
            {MODES.find((m) => m.id === darkMode)?.name ?? '跟随系统'}
          </p>
        </div>
      </div>
    </div>
  );
};

export default React.memo(DarkModePage);
