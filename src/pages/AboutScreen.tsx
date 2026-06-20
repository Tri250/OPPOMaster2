import React from 'react';
import { useAppStore } from '../store/appStore';
import {
  Palette,
  Globe,
  Bell,
  Shield,
  FileText,
  ChevronRight,
  Camera,
  Database,
} from 'lucide-react';
import { tokens } from '../styles/designTokens';

const AboutScreen: React.FC = () => {
  const { theme, darkMode, navigateToSubPage } = useAppStore();

  const themeNames: Record<string, string> = {
    hasselblad: '哈苏橙',
    oppo: 'OPPO 绿',
    vivo: 'vivo 蓝',
    realme: 'realme 黄',
    honor: '荣耀蓝',
    xiaomi: '小米橙',
  };

  const darkModeNames: Record<string, string> = {
    system: '跟随系统',
    light: '浅色模式',
    dark: '深色模式',
  };

  const settingsItems = [
    {
      icon: Palette,
      label: '外观设置',
      value: `${themeNames[theme] || '哈苏橙'} · ${darkModeNames[darkMode] || '跟随系统'}`,
      route: 'theme-settings' as const
    },
    {
      icon: Globe,
      label: '更新渠道',
      value: '稳定版',
      route: 'update-channel' as const
    },
    {
      icon: Bell,
      label: '通知设置',
      value: '',
      route: 'notification' as const
    },
    {
      icon: Database,
      label: '预设源管理',
      value: '',
      route: 'preset-sources' as const
    },
    {
      icon: Shield,
      label: '隐私政策',
      value: '',
      route: 'privacy' as const
    },
    {
      icon: FileText,
      label: '用户协议',
      value: '',
      route: 'terms' as const
    },
  ];

  const handleItemClick = (route: string | null) => {
    if (route) {
      navigateToSubPage(route as Parameters<typeof navigateToSubPage>[0]);
    }
  };

  return (
    <div className="h-full flex flex-col bg-master-bg overflow-hidden">
      {/* Header */}
      <div className="px-lg pt-sm pb-md">
        <h1 className="text-h1 font-bold text-master-text-primary">关于</h1>
      </div>

      {/* App Info Card */}
      <div className="px-lg pb-lg">
        <div
          className="relative rounded-2xl overflow-hidden animate-scale-in"
          style={{
            border: `1px solid ${tokens.colors.glassBorder}`,
            boxShadow: tokens.shadows.glass,
          }}
        >
          {/* Glass Background */}
          <div
            className="absolute inset-0"
            style={{
              background: `linear-gradient(135deg, ${tokens.colors.accent}25 0%, ${tokens.colors.accent}10 50%, transparent 100%)`,
            }}
          />
          <div
            className="absolute inset-0 backdrop-blur-glass"
            style={{ backgroundColor: tokens.colors.glass }}
          />

          <div className="relative p-6 text-center">
            {/* Logo */}
            <div
              className="w-20 h-20 mx-auto mb-4 rounded-2xl flex items-center justify-center"
              style={{
                background: `linear-gradient(135deg, ${tokens.colors.accent}, ${tokens.colors.accentLight})`,
                boxShadow: tokens.shadows.glow,
              }}
            >
              <Camera size={36} className="text-white" />
            </div>

            {/* App Name */}
            <h2 className="text-hero font-bold text-master-text-primary mb-1">OMaster</h2>
            <p className="text-master-text-tertiary text-sm mb-4">专业影像参数管理工具</p>

            {/* Version */}
            <div
              className="inline-flex items-center gap-2 px-3 py-1 rounded-full backdrop-blur-sm"
              style={{ background: tokens.colors.glassStrong, border: `1px solid ${tokens.colors.glassBorder}` }}
            >
              <span className="text-master-text-secondary text-xs">版本 1.3.1</span>
              <span
                className="w-1 h-1 rounded-full"
                style={{ background: tokens.colors.accent }}
              />
              <span className="text-xs font-medium" style={{ color: tokens.colors.accent }}>最新</span>
            </div>
          </div>
        </div>
      </div>

      {/* Settings List */}
      <div className="flex-1 overflow-y-auto px-lg pb-lg scrollbar-hide">
        <div
          className="rounded-2xl overflow-hidden animate-fade-in-up"
          style={{
            background: tokens.colors.glass,
            backdropFilter: 'blur(20px)',
            border: `1px solid ${tokens.colors.glassBorder}`,
          }}
        >
          {settingsItems.map((item, index) => (
            <button
              key={item.label}
              onClick={() => handleItemClick(item.route)}
              className="w-full flex items-center justify-between p-4 transition-all duration-normal hover:bg-master-glass-strong active:scale-[0.99]"
              style={{
                borderBottom: index !== settingsItems.length - 1 ? `1px solid ${tokens.colors.glassBorder}` : 'none',
                transitionTimingFunction: tokens.animation.easing.spring,
                animation: `fade-in-up 0.35s ${tokens.animation.easing.smooth} ${index * 0.05}s both`,
              }}
            >
              <div className="flex items-center gap-3">
                <div
                  className="w-10 h-10 rounded-xl flex items-center justify-center"
                  style={{ backgroundColor: `${tokens.colors.accent}15` }}
                >
                  <item.icon size={18} style={{ color: tokens.colors.accent }} />
                </div>
                <span className="text-master-text-primary text-sm font-medium">{item.label}</span>
              </div>
              <div className="flex items-center gap-2">
                {item.value && (
                  <span className="text-master-text-tertiary text-xs">{item.value}</span>
                )}
                <ChevronRight size={16} className="text-master-text-muted" />
              </div>
            </button>
          ))}
        </div>

        {/* Developer Info */}
        <div className="mt-6 text-center animate-fade-in-up">
          <p className="text-master-text-muted text-xs">Developed by Silas</p>
          <p className="text-master-text-muted/70 text-xs mt-1">© 2026 OMaster. All rights reserved.</p>
        </div>

        {/* Bottom Spacing */}
        <div className="h-8" />
      </div>

      {/* Styles */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default AboutScreen;
