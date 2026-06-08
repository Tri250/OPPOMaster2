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
  Sparkles,
  Download,
  Crown,
  ChevronLeft,
} from 'lucide-react';

/**
 * ============================================
 * 关于页 - 简洁哈苏品牌风格
 * 参考图片设计：深色背景 + 橙色强调 + 清晰层级
 * ============================================
 */
const AboutScreen: React.FC = () => {
  const { theme, darkMode, navigateToSubPage, setCurrentPage } = useAppStore();

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
      {/* 简洁标题栏 - 参考图片风格 */}
      <div className="flex items-center justify-center px-4 pt-12 pb-4 relative">
        <button 
          onClick={() => setCurrentPage('home')}
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
        {/* 品牌展示区 - 简洁大气 */}
        <div className="px-6 pt-4 pb-8">
          {/* 认证徽章 - 橙色边框风格 */}
          <div className="flex justify-center mb-8">
            <div 
              className="inline-flex items-center gap-2 px-4 py-2 rounded-full"
              style={{
                background: 'transparent',
                border: '1px solid #FF6B35'
              }}
            >
              <Crown size={14} style={{ color: '#FF6B35' }} />
              <span className="text-sm font-medium" style={{ color: '#FF6B35' }}>
                哈苏 HNCS 官方认证
              </span>
            </div>
          </div>

          {/* 品牌名称 - 大号橙色 */}
          <div className="text-center mb-6">
            <h2 
              className="text-5xl font-bold mb-4"
              style={{ color: '#FF6B35' }}
            >
              小O帮帮
            </h2>
            <p 
              className="text-2xl font-bold leading-tight"
              style={{ color: '#FFFFFF' }}
            >
              专业摄影参数预设
            </p>
          </div>

          {/* 产品描述 - 灰色小字 */}
          <div className="text-center mb-8">
            <p 
              className="text-base mb-1"
              style={{ color: 'rgba(255, 255, 255, 0.6)' }}
            >
              为 <span className="font-semibold" style={{ color: 'rgba(255, 255, 255, 0.8)' }}>OPPO Find 系列</span> 打造的专业级
            </p>
            <p 
              className="text-base mb-3"
              style={{ color: 'rgba(255, 255, 255, 0.6)' }}
            >
              摄影工具
            </p>
            <p 
              className="text-sm"
              style={{ color: 'rgba(255, 255, 255, 0.4)' }}
            >
              哈苏色彩科学 · 智能场景识别 · 一键参数优化
            </p>
          </div>

          {/* 操作按钮 */}
          <div className="space-y-3 mb-8">
            <button 
              className="w-full py-3.5 rounded-xl font-medium text-base flex items-center justify-center gap-2"
              style={{ 
                background: '#FF6B35',
                color: '#FFFFFF'
              }}
            >
              <Download size={18} />
              下载 App
            </button>
            <button 
              className="w-full py-3.5 rounded-xl font-medium text-base"
              style={{ 
                background: 'transparent',
                color: '#FFFFFF',
                border: '1px solid rgba(255, 255, 255, 0.2)'
              }}
            >
              了解更多
            </button>
          </div>

          {/* 数据统计 - 简洁三列 */}
          <div 
            className="flex justify-around py-6"
            style={{
              borderTop: '1px solid rgba(255, 255, 255, 0.08)',
            }}
          >
            <div className="text-center">
              <Camera size={22} style={{ color: '#FF6B35' }} className="mx-auto mb-2" />
              <p className="text-xl font-bold" style={{ color: '#FFFFFF' }}>500+</p>
              <p className="text-xs mt-0.5" style={{ color: 'rgba(255, 255, 255, 0.4)' }}>专业预设</p>
            </div>
            <div className="text-center">
              <Sparkles size={22} style={{ color: '#FF6B35' }} className="mx-auto mb-2" />
              <p className="text-xl font-bold" style={{ color: '#FFFFFF' }}>35+</p>
              <p className="text-xs mt-0.5" style={{ color: 'rgba(255, 255, 255, 0.4)' }}>智能场景</p>
            </div>
            <div className="text-center">
              <Download size={22} style={{ color: '#FF6B35' }} className="mx-auto mb-2" />
              <p className="text-xl font-bold" style={{ color: '#FFFFFF' }}>100万</p>
              <p className="text-xs mt-0.5" style={{ color: 'rgba(255, 255, 255, 0.4)' }}>用户下载</p>
            </div>
          </div>
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
                  <item.icon size={20} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
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
                  <ChevronRight 
                    size={18} 
                    style={{ color: 'rgba(255, 255, 255, 0.25)' }}
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
