import React, { useState, useRef, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft,
  Sun,
  Moon,
  Monitor,
  Bell,
  BellRing,
  Gift,
  Download,
  Shield,
  Zap,
  RotateCcw,
  Check,
  Clock
} from 'lucide-react';

const themeOptions = [
  { id: 'hasselblad', name: '哈苏橙', color: '#FF6B35', desc: '经典哈苏影像风格' },
  { id: 'oppo', name: 'OPPO 绿', color: '#1BAA52', desc: 'OPPO 品牌配色' },
  { id: 'vivo', name: 'vivo 蓝', color: '#415FFF', desc: 'vivo 品牌配色' },
  { id: 'realme', name: 'realme 黄', color: '#FFC107', desc: 'realme 品牌配色' },
  { id: 'honor', name: '荣耀蓝', color: '#00BFFF', desc: '荣耀品牌配色' },
  { id: 'xiaomi', name: '小米橙', color: '#FF6900', desc: '小米品牌配色' },
];

const darkModeOptions = [
  { id: 'system', name: '跟随系统', icon: Monitor, desc: '根据系统设置自动切换' },
  { id: 'light', name: '浅色模式', icon: Sun, desc: '明亮的界面风格' },
  { id: 'dark', name: '深色模式', icon: Moon, desc: '护眼的深色界面' },
];

const updateChannels = [
  { id: 'stable', name: '稳定版', desc: '最稳定的版本，推荐日常使用', icon: Shield, color: '#10B981' },
  { id: 'beta', name: '测试版', desc: '提前体验新功能，可能存在小问题', icon: Zap, color: '#F59E0B' },
  { id: 'dev', name: '开发版', desc: '最新功能，适合尝鲜用户', icon: RotateCcw, color: '#3B82F6' },
];

const updateSettingsList = [
  { id: 'auto-check', name: '自动检查更新', enabled: true },
  { id: 'wifi-only', name: '仅 Wi-Fi 下下载', enabled: true },
  { id: 'auto-install', name: '夜间自动安装', enabled: false },
];

const GeneralSettingsPage: React.FC = () => {
  const {
    theme,
    setTheme,
    darkMode,
    setDarkMode,
    notifications,
    setNotification,
    setCurrentSubPage,
  } = useAppStore();

  const [activeTab, setActiveTab] = useState<'appearance' | 'notification' | 'update'>('appearance');
  const [selectedChannel, setSelectedChannel] = useState('stable');
  const [updateSettings, setUpdateSettings] = useState(updateSettingsList);
  const [isChecking, setIsChecking] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const toggleUpdateSetting = (id: string) => {
    setUpdateSettings(prev =>
      prev.map(s => (s.id === id ? { ...s, enabled: !s.enabled } : s))
    );
  };

  const handleCheckUpdate = useCallback(() => {
    setIsChecking(true);
    setTimeout(() => setIsChecking(false), 1500);
  }, []);

  const renderAppearance = () => (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* 主题色 */}
      <div>
        <h3 className="text-white/50 text-xs mb-3">主题色</h3>
        <div className="space-y-2">
          {themeOptions.map((t) => (
            <button
              key={t.id}
              onClick={() => setTheme(t.id as typeof theme)}
              className={`w-full p-4 rounded-xl flex items-center gap-3 transition-all active:scale-[0.98] ${
                theme === t.id
                  ? 'bg-white/10 border border-white/20'
                  : 'bg-white/5 hover:bg-white/10'
              }`}
            >
              <div
                className="w-10 h-10 rounded-lg flex items-center justify-center"
                style={{ backgroundColor: t.color + '30' }}
              >
                <div className="w-5 h-5 rounded-full" style={{ backgroundColor: t.color }} />
              </div>
              <div className="flex-1 text-left">
                <p className="text-white font-medium text-sm">{t.name}</p>
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

      {/* 深色模式 */}
      <div>
        <h3 className="text-white/50 text-xs mb-3">深色模式</h3>
        <div className="space-y-2">
          {darkModeOptions.map((mode) => {
            const Icon = mode.icon;
            return (
              <button
                key={mode.id}
                onClick={() => setDarkMode(mode.id as typeof darkMode)}
                className={`w-full p-4 rounded-xl flex items-center gap-3 transition-all active:scale-[0.98] ${
                  darkMode === mode.id
                    ? 'bg-[#FF6B35]/10 border border-[#FF6B35]/30'
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${darkMode === mode.id ? 'bg-[#FF6B35]/20' : 'bg-white/10'}`}>
                  <Icon size={20} className={darkMode === mode.id ? 'text-[#FF6B35]' : 'text-white/60'} />
                </div>
                <div className="flex-1 text-left">
                  <p className="text-white font-medium text-sm">{mode.name}</p>
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

      {/* 预览 */}
      <div className="p-4 rounded-2xl bg-white/5">
        <p className="text-white/50 text-xs mb-3">预览</p>
        <div className={`p-4 rounded-xl transition-all ${darkMode === 'light' ? 'bg-white' : 'bg-[#1a1a1a]'}`}>
          <p className={darkMode === 'light' ? 'text-[#1a1a1a]' : 'text-white'}>这是一段示例文字</p>
          <p className={`text-sm mt-1 ${darkMode === 'light' ? 'text-white/60' : 'text-white/50'}`}>
            用于展示当前模式效果
          </p>
        </div>
      </div>
    </div>
  );

  const renderNotification = () => {
    const items = [
      { key: 'enabled', label: '接收通知', icon: Bell, desc: '开启或关闭所有通知' },
      { key: 'updates', label: '更新提醒', icon: BellRing, desc: '新版本发布时通知我' },
      { key: 'promotions', label: '活动推送', icon: Gift, desc: '精选推荐和优惠活动' },
    ] as const;

    return (
      <div className="space-y-3 animate-in fade-in duration-200">
        {items.map((item) => {
          const Icon = item.icon;
          const isEnabled = notifications[item.key as keyof typeof notifications];
          return (
            <div
              key={item.key}
              className="p-4 rounded-2xl bg-white/5 flex items-center gap-4"
            >
              <div className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0">
                <Icon size={24} className="text-white/60" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-white font-medium text-sm">{item.label}</p>
                <p className="text-white/50 text-xs">{item.desc}</p>
              </div>
              <button
                onClick={() => setNotification(item.key, !isEnabled)}
                className={`w-14 h-7 rounded-full relative transition-colors flex-shrink-0 ${
                  isEnabled ? 'bg-[#FF6B35]' : 'bg-white/20'
                }`}
              >
                <div
                  className={`absolute top-0.5 w-6 h-6 rounded-full bg-white transition-all ${
                    isEnabled ? 'left-7' : 'left-0.5'
                  }`}
                />
              </button>
            </div>
          );
        })}
      </div>
    );
  };

  const renderUpdate = () => (
    <div className="space-y-4 animate-in fade-in duration-200">
      {/* 当前版本 */}
      <div className="bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] rounded-2xl p-5 text-white">
        <div className="flex items-start gap-4">
          <div className="p-3 bg-white/20 rounded-xl">
            <Check size={32} className="text-white" />
          </div>
          <div className="flex-1">
            <h2 className="text-xl font-bold mb-1">已是最新版本</h2>
            <p className="text-white/80 text-sm">v3.2.0 (20260608)</p>
            <div className="flex items-center gap-2 mt-3">
              <Clock size={14} className="text-white/60" />
              <span className="text-white/60 text-xs">最后检查：刚刚</span>
            </div>
          </div>
        </div>
        <button
          onClick={handleCheckUpdate}
          disabled={isChecking}
          className="w-full mt-4 py-3 bg-white/20 hover:bg-white/30 rounded-xl font-medium transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {isChecking ? (
            <>
              <RotateCcw size={18} className="animate-spin" />
              <span>检查中...</span>
            </>
          ) : (
            <>
              <Download size={18} />
              <span>检查更新</span>
            </>
          )}
        </button>
      </div>

      {/* 更新渠道 */}
      <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
        <h3 className="text-sm font-semibold text-white mb-3">更新渠道</h3>
        <div className="space-y-2">
          {updateChannels.map((channel) => {
            const Icon = channel.icon;
            const isSelected = selectedChannel === channel.id;
            return (
              <button
                key={channel.id}
                onClick={() => setSelectedChannel(channel.id)}
                className={`w-full flex items-center justify-between p-3 rounded-xl border-2 transition-all text-left active:scale-[0.98] ${
                  isSelected
                    ? 'border-[#FF6B35] bg-[#FF6B35]/10'
                    : 'border-white/5 hover:border-white/10 hover:bg-white/5'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg" style={{ backgroundColor: `${channel.color}20` }}>
                    <Icon size={20} style={{ color: channel.color }} />
                  </div>
                  <div>
                    <span className="text-sm font-medium text-white">{channel.name}</span>
                    <p className="text-xs text-white/50">{channel.desc}</p>
                  </div>
                </div>
                {isSelected && <Check size={20} className="text-[#FF6B35]" />}
              </button>
            );
          })}
        </div>
      </div>

      {/* 更新选项 */}
      <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
        <h3 className="text-sm font-semibold text-white mb-3">更新选项</h3>
        <div className="space-y-2">
          {updateSettings.map((setting) => (
            <div
              key={setting.id}
              className="flex items-center justify-between p-3 bg-white/5 rounded-xl"
            >
              <span className="text-sm text-white/70">{setting.name}</span>
              <button
                onClick={() => toggleUpdateSetting(setting.id)}
                className={`w-12 h-6 rounded-full transition-colors ${
                  setting.enabled ? 'bg-[#FF6B35]' : 'bg-white/20'
                }`}
              >
                <div
                  className={`w-5 h-5 bg-white rounded-full shadow transition-transform ${
                    setting.enabled ? 'translate-x-6' : 'translate-x-1'
                  }`}
                  style={{ marginTop: 2 }}
                />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* 更新日志 */}
      <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
        <h3 className="text-sm font-semibold text-white mb-3">v3.2.0 更新内容</h3>
        <div className="space-y-2 text-sm">
          {[
            '全新 LUT 资源下载功能，20+ 专业滤镜',
            '哈苏色彩科学升级，HNCS 3.0 自然色彩',
            '哈苏之眼增强，支持 50+ 精细场景',
            '优化性能，启动速度提升 30%',
          ].map((text, i) => (
            <div key={i} className="flex items-start gap-2">
              <span className="text-[#FF6B35] font-bold mt-0.5">•</span>
              <span className="text-white/60 text-xs">{text}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5 shrink-0">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">通用设置</h1>
      </div>

      {/* Tabs */}
      <div className="px-4 pt-4 pb-2 shrink-0">
        <div className="flex p-1 rounded-xl bg-white/5">
          {[
            { id: 'appearance', label: '外观' },
            { id: 'notification', label: '通知' },
            { id: 'update', label: '更新' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as typeof activeTab)}
              className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all ${
                activeTab === tab.id
                  ? 'bg-[#FF6B35] text-white'
                  : 'text-white/50 hover:text-white/70'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 pb-6">
        {activeTab === 'appearance' && renderAppearance()}
        {activeTab === 'notification' && renderNotification()}
        {activeTab === 'update' && renderUpdate()}
      </div>

      <input ref={fileInputRef} type="file" className="hidden" />
    </div>
  );
};

export default GeneralSettingsPage;
