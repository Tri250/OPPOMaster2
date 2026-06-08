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
  Images,
  Cloud,
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

  // 设置项分为两组：功能设置和系统设置
  const functionSettings = [
    { 
      icon: Images, 
      label: '预设管理', 
      value: '',
      desc: '云端预设库，收藏、创建、分享',
      route: 'preset-manager' as const
    },
    { 
      icon: Cloud, 
      label: '云同步', 
      value: '未连接',
      desc: 'OPPO/realme/vivo/荣耀 CDN数据同步',
      route: 'cloud-sync' as const
    },
  ];

  const systemSettings = [
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
      value: 'Gitee',
      route: null
    },
    { 
      icon: Bell, 
      label: '通知设置', 
      value: '',
      route: 'notification' as const
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
      route: 'privacy' as const
    },
  ];

  const handleItemClick = (route: string | null) => {
    if (route) {
      navigateToSubPage(route as any);
    }
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3">
        <h1 className="text-xl font-bold text-white">设置</h1>
      </div>

      {/* App Info Card */}
      <div className="px-4 pb-4">
        <div className="relative rounded-2xl overflow-hidden">
          {/* Glass Background */}
          <div className="absolute inset-0 bg-gradient-to-br from-[#FF6B35]/20 via-[#FF6B35]/10 to-transparent" />
          <div className="absolute inset-0 backdrop-blur-xl bg-white/5" />
          
          <div className="relative p-6 text-center">
            {/* Logo */}
            <div className="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] flex items-center justify-center shadow-lg shadow-[#FF6B35]/30">
              <Camera size={36} className="text-white" />
            </div>

            {/* App Name */}
            <h2 className="text-2xl font-bold text-white mb-1">OMaster</h2>
            <p className="text-white/50 text-sm mb-4">专业影像参数管理工具</p>

            {/* Version */}
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-sm">
              <span className="text-white/70 text-xs">版本 1.3.1</span>
              <span className="w-1 h-1 rounded-full bg-[#FF6B35]" />
              <span className="text-[#FF6B35] text-xs">最新</span>
            </div>
          </div>
        </div>
      </div>

      {/* Settings List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        {/* 功能设置 */}
        <div className="mb-4">
          <p className="text-white/50 text-xs mb-2 px-1">功能设置</p>
          <div className="rounded-2xl overflow-hidden bg-white/5 backdrop-blur-sm">
            {functionSettings.map((item, index) => (
              <button
                key={item.label}
                onClick={() => handleItemClick(item.route)}
                className={`w-full flex items-center justify-between p-4 transition-all duration-200 hover:bg-white/10 ${
                  index !== functionSettings.length - 1 ? 'border-b border-white/5' : ''
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/10 flex items-center justify-center">
                    <item.icon size={18} className="text-[#FF6B35]" />
                  </div>
                  <div>
                    <span className="text-white text-sm font-medium">{item.label}</span>
                    {item.desc && (
                      <p className="text-white/40 text-xs">{item.desc}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {item.value && (
                    <span className="text-white/50 text-xs">{item.value}</span>
                  )}
                  <ChevronRight size={16} className="text-white/30" />
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* 系统设置 */}
        <div className="mb-4">
          <p className="text-white/50 text-xs mb-2 px-1">系统设置</p>
          <div className="rounded-2xl overflow-hidden bg-white/5 backdrop-blur-sm">
            {systemSettings.map((item, index) => (
              <button
                key={item.label}
                onClick={() => handleItemClick(item.route)}
                className={`w-full flex items-center justify-between p-4 transition-all duration-200 hover:bg-white/10 ${
                  index !== systemSettings.length - 1 ? 'border-b border-white/5' : ''
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/10 flex items-center justify-center">
                    <item.icon size={18} className="text-[#FF6B35]" />
                  </div>
                  <span className="text-white text-sm font-medium">{item.label}</span>
                </div>
                <div className="flex items-center gap-2">
                  {item.value && (
                    <span className="text-white/50 text-xs">{item.value}</span>
                  )}
                  <ChevronRight size={16} className="text-white/30" />
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Developer Info */}
        <div className="mt-6 text-center">
          <p className="text-white/30 text-xs">Developed by Silas</p>
          <p className="text-white/20 text-xs mt-1">© 2024 OMaster. All rights reserved.</p>
        </div>

        {/* Bottom Spacing */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default AboutScreen;