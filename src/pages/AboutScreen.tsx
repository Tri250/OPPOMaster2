import React from 'react';
import { useAppStore } from '../store/appStore';
import {
  Palette,
  Moon,
  Globe,
  Bell,
  Shield,
  FileText,
  ChevronRight,
  Camera,
  Database,
} from 'lucide-react';

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
      label: '主题设置', 
      value: themeNames[theme] || '哈苏橙',
      route: 'theme-settings' as const
    },
    { 
      icon: Moon, 
      label: '深色模式', 
      value: darkModeNames[darkMode] || '跟随系统',
      route: 'dark-mode' as const
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
    <div 
      className="h-full flex flex-col overflow-hidden animate-liquid-fade"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* Header */}
      <div className="px-4 pt-3 pb-3">
        <h1 style={{ color: 'var(--color-text-primary)' }} className="text-xl font-bold">
          关于
        </h1>
      </div>

      {/* App Info Card - 液态玻璃效果 */}
      <div className="px-4 pb-4">
        <div 
          className="relative rounded-2xl overflow-hidden animate-spring-in"
          style={{
            background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.2) 0%, rgba(255, 107, 53, 0.1) 50%, transparent 100%)',
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3), 0 0 20px rgba(255, 107, 53, 0.1)'
          }}
        >
          {/* Glass Overlay */}
          <div 
            className="absolute inset-0"
            style={{
              backdropFilter: 'blur(20px)',
              background: 'rgba(255, 255, 255, 0.05)'
            }}
          />
          
          <div className="relative p-6 text-center">
            {/* Logo - 液态玻璃效果 */}
            <div 
              className="w-20 h-20 mx-auto mb-4 rounded-2xl flex items-center justify-center animate-liquid-breathe"
              style={{
                background: 'linear-gradient(135deg, var(--color-accent-primary) 0%, #FF8C42 100%)',
                boxShadow: '0 8px 24px rgba(255, 107, 53, 0.4)'
              }}
            >
              <Camera size={36} style={{ color: 'var(--color-text-primary)' }} />
            </div>

            {/* App Name */}
            <h2 style={{ color: 'var(--color-text-primary)' }} className="text-2xl font-bold mb-1">
              小O帮帮
            </h2>
            <p style={{ color: 'var(--color-text-muted)' }} className="text-sm mb-4">
              专业影像参数管理工具
            </p>

            {/* Version Badge */}
            <div 
              className="inline-flex items-center gap-2 px-3 py-1 rounded-full glass-light animate-liquid-pulse"
            >
              <span style={{ color: 'var(--color-text-secondary)' }} className="text-xs">
                版本 1.3.1
              </span>
              <span 
                className="w-1 h-1 rounded-full animate-liquid-breathe"
                style={{ background: 'var(--color-accent-primary)' }}
              />
              <span style={{ color: 'var(--color-accent-primary)' }} className="text-xs">
                最新
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Settings List - 液态玻璃卡片 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar">
        <div className="rounded-2xl overflow-hidden glass-card">
          {settingsItems.map((item, index) => (
            <button
              key={item.label}
              onClick={() => handleItemClick(item.route)}
              aria-label={item.label}
              className={`w-full flex items-center justify-between p-4 transition-liquid hover-liquid animate-liquid-slide-up ${
                index !== settingsItems.length - 1 ? 'border-b' : ''
              }`}
              style={{
                borderBottomColor: index !== settingsItems.length - 1 ? 'var(--color-border-light)' : 'transparent',
                animationDelay: `${index * 50}ms`,
                animationFillMode: 'both'
              }}
            >
              <div className="flex items-center gap-3">
                <div 
                  className="w-10 h-10 rounded-xl flex items-center justify-center glass-light"
                  style={{ boxShadow: '0 0 8px rgba(255, 107, 53, 0.15)' }}
                >
                  <item.icon size={18} style={{ color: 'var(--color-accent-primary)' }} />
                </div>
                <span style={{ color: 'var(--color-text-primary)' }} className="text-sm font-medium">
                  {item.label}
                </span>
              </div>
              <div className="flex items-center gap-2">
                {item.value && (
                  <span style={{ color: 'var(--color-text-muted)' }} className="text-xs">
                    {item.value}
                  </span>
                )}
                <ChevronRight 
                  size={16} 
                  style={{ color: 'var(--color-text-muted)' }}
                  className="transition-liquid group-hover:translate-x-1"
                />
              </div>
            </button>
          ))}
        </div>

        {/* Developer Info */}
        <div className="mt-6 text-center animate-liquid-fade">
          <p style={{ color: 'var(--color-text-muted)' }} className="text-xs opacity-60">
            Developed by Silas
          </p>
          <p style={{ color: 'var(--color-text-muted)' }} className="text-xs mt-1 opacity-40">
            © 2024 小O帮帮. All rights reserved.
          </p>
        </div>

        {/* Bottom Spacing */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default AboutScreen;