import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Palette, Check, Sun, Moon, Monitor } from 'lucide-react';

const ThemeSettingsPage: React.FC = () => {
  const { theme, setTheme, darkMode, setDarkMode, goBack } = useAppStore();

  const themes = [
    { id: 'hasselblad', name: '哈苏橙', color: '#FF6B35', desc: '经典哈苏影像风格' },
    { id: 'oppo', name: 'OPPO 绿', color: '#1BAA52', desc: 'OPPO 品牌配色' },
    { id: 'vivo', name: 'vivo 蓝', color: '#415FFF', desc: 'vivo 品牌配色' },
    { id: 'realme', name: 'realme 黄', color: '#FFC107', desc: 'realme 品牌配色' },
    { id: 'honor', name: '荣耀蓝', color: '#00BFFF', desc: '荣耀品牌配色' },
    { id: 'xiaomi', name: '小米橙', color: '#FF6900', desc: '小米品牌配色' },
  ];

  const darkModes = [
    { id: 'system', name: '跟随系统', icon: Monitor, desc: '根据系统设置自动切换' },
    { id: 'light', name: '浅色模式', icon: Sun, desc: '明亮的界面风格' },
    { id: 'dark', name: '深色模式', icon: Moon, desc: '护眼的深色界面' },
  ];

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">外观设置</h1>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-6">
        {/* Current Theme */}
        <div className="p-4 rounded-2xl bg-white/5">
          <div className="flex items-center gap-3">
            <div
              className="w-12 h-12 rounded-xl flex items-center justify-center"
              style={{ backgroundColor: themes.find(t => t.id === theme)?.color + '30' }}
            >
              <Palette size={24} style={{ color: themes.find(t => t.id === theme)?.color }} />
            </div>
            <div>
              <p className="text-white font-medium">{themes.find(t => t.id === theme)?.name}</p>
              <p className="text-white/50 text-xs">{themes.find(t => t.id === theme)?.desc}</p>
            </div>
          </div>
        </div>

        {/* Theme List */}
        <div>
          <p className="text-white/50 text-xs mb-3">选择主题</p>
          <div className="space-y-2">
            {themes.map((t) => (
              <button
                key={t.id}
                onClick={() => setTheme(t.id as typeof theme)}
                className={`w-full p-4 rounded-xl flex items-center gap-3 transition-all ${
                  theme === t.id
                    ? 'bg-white/10 border border-white/20'
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center"
                  style={{ backgroundColor: t.color + '30' }}
                >
                  <div
                    className="w-5 h-5 rounded-full"
                    style={{ backgroundColor: t.color }}
                  />
                </div>
                <div className="flex-1 text-left">
                  <p className="text-white font-medium">{t.name}</p>
                  <p className="text-white/50 text-xs">{t.desc}</p>
                </div>
                {theme === t.id && (
                  <div
                    className="w-6 h-6 rounded-full flex items-center justify-center"
                    style={{ backgroundColor: t.color }}
                  >
                    <Check size={14} className="text-white" />
                  </div>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Dark Mode */}
        <div>
          <p className="text-white/50 text-xs mb-3">深色模式</p>
          <div className="space-y-2">
            {darkModes.map((mode) => {
              const Icon = mode.icon;
              return (
                <button
                  key={mode.id}
                  onClick={() => setDarkMode(mode.id as typeof darkMode)}
                  className={`w-full p-4 rounded-xl flex items-center gap-3 transition-all ${
                    darkMode === mode.id
                      ? 'bg-[#FF6B35]/10 border border-[#FF6B35]/30'
                      : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                    darkMode === mode.id ? 'bg-[#FF6B35]/20' : 'bg-white/10'
                  }`}>
                    <Icon size={20} className={darkMode === mode.id ? 'text-[#FF6B35]' : 'text-white/60'} />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="text-white font-medium">{mode.name}</p>
                    <p className="text-white/50 text-xs">{mode.desc}</p>
                  </div>
                  {darkMode === mode.id && (
                    <div className="w-6 h-6 rounded-full bg-[#FF6B35] flex items-center justify-center">
                      <Check size={14} className="text-white" />
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ThemeSettingsPage;
