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
  Sparkles,
  Download,
  Info,
  Award,
  Heart,
} from 'lucide-react';

/**
 * ============================================
 * 关于页 - ColorOS 16 优化版
 * 哈苏橙风格 + 清晰点击区域
 * ============================================
 */
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
      icon: Database, 
      label: '预设源管理', 
      value: '',
      route: 'preset-sources' as const,
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
      className="h-full flex flex-col overflow-hidden animate-liquid-fade"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* 沉浸式标题栏 */}
      <div className="immersive-header animate-liquid-slide-down">
        <h1 className="immersive-title">关于</h1>
      </div>

      {/* 品牌Logo卡片 - 哈苏橙风格 */}
      <div className="px-4 pb-4 animate-spring-in">
        <div 
          className="relative rounded-3xl overflow-hidden"
          style={{
            background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.2) 0%, rgba(255, 107, 53, 0.1) 100%)',
            boxShadow: '0 12px 32px rgba(0, 0, 0, 0.4), 0 0 20px rgba(255, 107, 53, 0.15)'
          }}
        >
          {/* 液态玻璃遮罩 */}
          <div 
            className="absolute inset-0"
            style={{
              backdropFilter: 'blur(40px) saturate(180%)',
              background: 'rgba(255, 255, 255, 0.03)'
            }}
          />
          
          {/* 顶部光效 */}
          <div 
            className="absolute top-0 left-0 right-0 h-1"
            style={{
              background: 'linear-gradient(90deg, transparent 0%, rgba(255, 107, 53, 0.6) 50%, transparent 100%)'
            }}
          />
          
          <div className="relative p-6 text-center">
            {/* Logo - 哈苏橙渐变 */}
            <div 
              className="w-20 h-20 mx-auto mb-4 rounded-2xl flex items-center justify-center animate-liquid-breathe"
              style={{
                background: 'linear-gradient(135deg, #FF6B35 0%, #FF8C5A 50%, #FFD93D 100%)',
                boxShadow: '0 8px 24px rgba(255, 107, 53, 0.4)'
              }}
            >
              <Camera size={36} style={{ color: '#FFFFFF' }} />
            </div>

            {/* 应用名称 */}
            <h2 
              className="text-xl font-bold mb-1"
              style={{ color: 'var(--color-text-primary)' }}
            >
              小O帮帮
            </h2>
            
            {/* 应用描述 */}
            <p className="text-sm mb-4" style={{ color: 'var(--color-text-tertiary)' }}>
              专业影像参数管理工具
            </p>

            {/* 版本信息 */}
            <div 
              className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full"
              style={{
                background: 'rgba(255, 255, 255, 0.1)',
                backdropFilter: 'blur(12px)'
              }}
            >
              <span className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>
                版本 1.3.1
              </span>
              <div 
                className="w-1 h-1 rounded-full"
                style={{ background: 'var(--color-success)' }}
              />
              <span className="text-xs font-semibold" style={{ color: 'var(--color-success)' }}>
                最新
              </span>
            </div>

            {/* 功能亮点 */}
            <div className="flex items-center justify-center gap-3 mt-4">
              <div className="flex items-center gap-1">
                <Award size={12} style={{ color: 'var(--color-accent-primary)' }} />
                <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                  哈苏认证
                </span>
              </div>
              <div className="flex items-center gap-1">
                <Heart size={12} style={{ color: '#F44336' }} />
                <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                  10万+用户
                </span>
              </div>
              <div className="flex items-center gap-1">
                <Download size={12} style={{ color: 'var(--color-accent-primary)' }} />
                <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                  100万+下载
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 设置列表 - 哈苏橙风格 + 清晰点击区域 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar">
        <div 
          className="rounded-2xl overflow-hidden"
          style={{
            background: 'var(--color-bg-secondary)',
            border: '1px solid var(--color-border-light)'
          }}
        >
          {settingsItems.map((item, index) => (
            <button
              key={item.label}
              onClick={() => handleItemClick(item.route)}
              aria-label={item.label}
              className={`w-full flex items-center justify-between p-4 transition-liquid ripple-container animate-liquid-slide-up ${
                index !== settingsItems.length - 1 ? 'border-b' : ''
              }`}
              style={{
                borderBottomColor: index !== settingsItems.length - 1 ? 'var(--color-border-light)' : 'transparent',
                animationDelay: `${index * 50}ms`,
                animationFillMode: 'both',
                background: 'transparent',
                cursor: 'pointer'
              }}
            >
              <div className="flex items-center gap-3">
                {/* 哈苏橙图标 */}
                <div 
                  className="w-10 h-10 rounded-xl flex items-center justify-center"
                  style={{
                    background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.2) 0%, rgba(255, 107, 53, 0.1) 100%)',
                    border: '1px solid rgba(255, 107, 53, 0.3)'
                  }}
                >
                  <item.icon size={18} style={{ color: 'var(--color-accent-primary)' }} />
                </div>
                <span className="text-sm font-medium" style={{ color: 'var(--color-text-primary)' }}>
                  {item.label}
                </span>
              </div>
              <div className="flex items-center gap-2">
                {item.value && (
                  <span 
                    className="text-xs px-2 py-1 rounded-full"
                    style={{ 
                      background: 'rgba(255, 107, 53, 0.1)', 
                      color: 'var(--color-accent-primary)' 
                    }}
                  >
                    {item.value}
                  </span>
                )}
                <ChevronRight 
                  size={16} 
                  style={{ color: 'var(--color-accent-primary)' }}
                />
              </div>
            </button>
          ))}
        </div>

        {/* 功能介绍卡片 - 哈苏橙风格 */}
        <div 
          className="mt-6 p-4 rounded-2xl animate-liquid-fade"
          style={{
            background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%)',
            border: '1px solid rgba(255, 107, 53, 0.2)'
          }}
        >
          <div className="flex items-center gap-2 mb-3">
            <Sparkles size={14} style={{ color: 'var(--color-accent-primary)' }} />
            <span className="text-sm font-semibold" style={{ color: 'var(--color-text-primary)' }}>
              功能亮点
            </span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {[
              { icon: Camera, label: '36+场景识别' },
              { icon: Palette, label: 'AI智能微调' },
              { icon: Sparkles, label: '14+水印模板' },
              { icon: Download, label: '云端同步' },
            ].map((feature, i) => (
              <div 
                key={i}
                className="flex items-center gap-2 p-2 rounded-xl"
                style={{
                  background: 'rgba(255, 255, 255, 0.05)'
                }}
              >
                <feature.icon size={14} style={{ color: 'var(--color-accent-primary)' }} />
                <span className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>
                  {feature.label}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* 开发者信息 */}
        <div className="mt-6 text-center animate-liquid-fade">
          <div 
            className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full"
            style={{
              background: 'rgba(255, 255, 255, 0.05)'
            }}
          >
            <Info size={12} style={{ color: 'var(--color-text-muted)' }} />
            <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
              Developed by Silas
            </span>
          </div>
          <p className="text-xs mt-2" style={{ color: 'var(--color-text-muted)', opacity: 0.5 }}>
            © 2024 小O帮帮. All rights reserved.
          </p>
        </div>

        {/* 底部间距 */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default AboutScreen;