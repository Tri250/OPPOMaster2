import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Sun, Moon, Monitor, Check } from 'lucide-react';

const DarkModePage: React.FC = () => {
  const { darkMode, setDarkMode, goBack } = useAppStore();

  const modes = [
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
        <h1 className="text-lg font-bold text-white">深色模式</h1>
      </div>

      {/* Mode List */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="space-y-3">
          {modes.map((mode) => {
            const Icon = mode.icon;
            return (
              <button
                key={mode.id}
                onClick={() => setDarkMode(mode.id as typeof darkMode)}
                className={`w-full p-4 rounded-2xl flex items-center gap-4 transition-all ${
                  darkMode === mode.id 
                    ? 'bg-[#FF6B35]/10 border border-[#FF6B35]/30' 
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                  darkMode === mode.id ? 'bg-[#FF6B35]/20' : 'bg-white/10'
                }`}>
                  <Icon size={24} className={darkMode === mode.id ? 'text-[#FF6B35]' : 'text-white/60'} />
                </div>
                <div className="flex-1 text-left">
                  <p className={`font-medium ${darkMode === mode.id ? 'text-white' : 'text-white/80'}`}>
                    {mode.name}
                  </p>
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

        {/* Preview */}
        <div className="mt-6 p-4 rounded-2xl bg-white/5">
          <p className="text-white/50 text-xs mb-3">预览</p>
          <div className={`p-4 rounded-xl transition-all ${
            darkMode === 'light' ? 'bg-white' : 'bg-[#1a1a1a]'
          }`}>
            <p className={darkMode === 'light' ? 'text-gray-900' : 'text-white'}>
              这是一段示例文字
            </p>
            <p className={`text-sm mt-1 ${darkMode === 'light' ? 'text-gray-500' : 'text-white/50'}`}>
              用于展示当前模式效果
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DarkModePage;
