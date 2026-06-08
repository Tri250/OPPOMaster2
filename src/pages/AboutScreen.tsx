import React from 'react';
import { useAppStore } from '../store/appStore';
import {
  Palette,
  Moon,
  Globe,
  Bell,
  Shield,
  FileText,
  ChevronLeft,
} from 'lucide-react';

/**
 * ============================================
 * 关于页 - 极简风格
 * ============================================
 */
const AboutScreen: React.FC = () => {
  const { theme, darkMode, navigateToSubPage, goBack } = useAppStore();

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
      route: 'theme-settings' as const,
    },
    { 
      icon: Moon, 
      label: '深色模式', 
      value: darkModeNames[darkMode] || '跟随系统',
      route: 'dark-mode' as const,
    },
    { 
      icon: Globe, 
      label: '更新渠道', 
      value: '稳定版',
      route: 'update-channel' as const,
    },
    { 
      icon: Bell, 
      label: '通知设置', 
      value: '',
      route: 'notification' as const,
    },
    { 
      icon: Shield, 
      label: '隐私政策', 
      value: '',
      route: 'privacy' as const,
    },
    { 
      icon: FileText, 
      label: '用户协议', 
      value: '',
      route: 'terms' as const,
    },
  ];

  const handleItemClick = (route: string | null) => {
    if (route) {
      navigateToSubPage(route as Parameters<typeof navigateToSubPage>[0]);
    }
  };

  return (
    <div 
      className="h-full flex flex-col overflow-hidden"
      style={{ background: '#0a0a0a' }}
    >
      {/* 简洁标题栏 */}
      <div className="flex items-center justify-center px-4 pt-12 pb-4 relative">
        <button 
          onClick={() => goBack()}
          className="absolute left-4 p-2 -ml-2"
        >
          <ChevronLeft size={24} style={{ color: '#FFFFFF' }} />
        </button>
        <h1 
          className="text-lg font-medium"
          style={{ color: '#FFFFFF' }}
        >
          关于
        </h1>
      </div>

      {/* 可滚动内容区 */}
      <div className="flex-1 overflow-y-auto">
        {/* 品牌名称 */}
        <div className="px-6 pt-8 pb-6 text-center">
          <h2 
            className="text-4xl font-bold mb-2"
            style={{ color: '#FF6B35' }}
          >
            小O帮帮
          </h2>
          <p 
            className="text-base"
            style={{ color: 'rgba(255, 255, 255, 0.5)' }}
          >
            为热爱生活 热爱摄影的 追求梦想您 用心打造
          </p>
        </div>

        {/* 设置列表 */}
        <div className="px-4 pb-6">
          <div 
            className="rounded-xl overflow-hidden"
            style={{
              background: '#141414',
            }}
          >
            {settingsItems.map((item, index) => (
              <button
                key={item.label}
                onClick={() => handleItemClick(item.route)}
                aria-label={item.label}
                className="w-full flex items-center justify-between p-4 transition-all duration-200 active:opacity-70"
                style={{
                  borderBottom: index !== settingsItems.length - 1 
                    ? '1px solid rgba(255, 255, 255, 0.05)' 
                    : 'none',
                  cursor: 'pointer'
                }}
              >
                <div className="flex items-center gap-3">
                  <item.icon size={20} style={{ color: 'rgba(255, 255, 255, 0.4)' }} />
                  <span 
                    className="text-base"
                    style={{ color: '#FFFFFF' }}
                  >
                    {item.label}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  {item.value && (
                    <span 
                      className="text-sm"
                      style={{ color: 'rgba(255, 255, 255, 0.4)' }}
                    >
                      {item.value}
                    </span>
                  )}
                  <div 
                    className="w-1.5 h-1.5 rounded-full"
                    style={{ background: 'rgba(255, 255, 255, 0.2)' }}
                  />
                </div>
              </button>
            ))}
          </div>

          {/* 版本信息 */}
          <div className="mt-8 text-center">
            <p 
              className="text-sm mb-1"
              style={{ color: 'rgba(255, 255, 255, 0.4)' }}
            >
              版本 1.3.1
            </p>
            <p 
              className="text-xs"
              style={{ color: 'rgba(255, 255, 255, 0.25)' }}
            >
              © 2024 小O帮帮. All rights reserved.
            </p>
          </div>

          {/* 底部间距 */}
          <div className="h-8" />
        </div>
      </div>
    </div>
  );
};

export default AboutScreen;
