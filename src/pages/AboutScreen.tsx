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
 * 关于页 - ColorOS 16 全面优化版
 * 沉浸式品牌展示 + 智能设置列表
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
      color: 'var(--color-feature-theme)'
    },
    { 
      icon: Moon, 
      label: '深色模式', 
      value: darkModeNames[darkMode] || '跟随系统',
      route: 'dark-mode' as const,
      color: 'var(--color-feature-ai)'
    },
    { 
      icon: Globe, 
      label: '更新渠道', 
      value: '稳定版',
      route: 'update-channel' as const,
      color: 'var(--color-feature-sync)'
    },
    { 
      icon: Bell, 
      label: '通知设置', 
      value: '',
      route: 'notification' as const,
      color: 'var(--color-warning)'
    },
    { 
      icon: Database, 
      label: '预设源管理', 
      value: '',
      route: 'preset-sources' as const,
      color: 'var(--color-feature-preset)'
    },
    { 
      icon: Shield, 
      label: '隐私政策', 
      value: '',
      route: 'privacy' as const,
      color: 'var(--color-success)'
    },
    { 
      icon: FileText, 
      label: '用户协议', 
      value: '',
      route: 'terms' as const,
      color: 'var(--color-info)'
    },
  ];

  const handleItemClick = (route: string | null) => {
    if (route) {
      navigateToSubPage(route as Parameters<typeof navigateToSubPage>[0]);
    }
  };

  return (
    <div 
      className="h-full flex flex-col overflow-hidden animate-liquid-fade dynamic-bg"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* 沉浸式标题栏 */}
      <div className="immersive-header animate-liquid-slide-down">
        <h1 className="immersive-title">关于</h1>
      </div>

      {/* 品牌Logo卡片 - 增强版 */}
      <div className="px-4 pb-4 animate-spring-in">
        <div 
          className="relative rounded-3xl overflow-hidden"
          style={{
            background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.25) 0%, rgba(255, 107, 53, 0.15) 40%, rgba(255, 255, 255, 0.05) 100%)',
            boxShadow: '0 16px 40px rgba(0, 0, 0, 0.4), 0 0 30px rgba(255, 107, 53, 0.2)'
          }}
        >
          {/* 液态玻璃遮罩 */}
          <div 
            className="absolute inset-0"
            style={{
              backdropFilter: 'blur(40px) saturate(200%)',
              background: 'rgba(255, 255, 255, 0.05)'
            }}
          />
          
          {/* 动态光效 */}
          <div 
            className="absolute top-0 left-0 right-0 h-1 animate-shimmer"
            style={{
              background: 'linear-gradient(90deg, transparent 0%, rgba(255, 107, 53, 0.5) 50%, transparent 100%)',
              backgroundSize: '200% 100%'
            }}
          />
          
          <div className="relative p-6 text-center">
            {/* Logo - 增强版渐变 */}
            <div 
              className="w-24 h-24 mx-auto mb-5 rounded-2xl flex items-center justify-center animate-liquid-breathe"
              style={{
                background: 'linear-gradient(135deg, var(--color-accent-primary) 0%, var(--color-accent-gradient-mid) 50%, var(--color-accent-gradient-end) 100%)',
                boxShadow: '0 12px 30px rgba(255, 107, 53, 0.5), 0 0 40px rgba(255, 107, 53, 0.3)'
              }}
            >
              <Camera size={44} style={{ color: 'var(--color-text-primary)' }} />
            </div>

            {/* 应用名称 */}
            <h2 
              className="text-2xl font-bold mb-2 text-gradient-glow"
              style={{ color: 'var(--color-text-primary)' }}
            >
              小O帮帮
            </h2>
            
            {/* 应用描述 */}
            <p className="text-sm mb-5" style={{ color: 'var(--color-text-tertiary)' }}>
              专业影像参数管理工具
            </p>

            {/* 版本信息 - 增强版 */}
            <div 
              className="inline-flex items-center gap-3 px-4 py-2 rounded-full glass-light animate-liquid-pulse"
            >
              <span className="text-sm" style={{ color: 'var(--color-text-secondary)' }}>
                版本 1.3.1
              </span>
              <div 
                className="w-1.5 h-1.5 rounded-full animate-glow-breathe"
                style={{ background: 'var(--color-success)' }}
              />
              <span className="text-sm font-semibold" style={{ color: 'var(--color-success)' }}>
                最新
              </span>
            </div>

            {/* 功能亮点 */}
            <div className="flex items-center justify-center gap-4 mt-5">
              <div className="flex items-center gap-1">
                <Award size={14} style={{ color: 'var(--color-accent-primary)' }} />
                <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                  哈苏认证
                </span>
              </div>
              <div className="flex items-center gap-1">
                <Heart size={14} style={{ color: 'var(--color-error)' }} />
                <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                  10万+用户
                </span>
              </div>
              <div className="flex items-center gap-1">
                <Download size={14} style={{ color: 'var(--color-feature-sync)' }} />
                <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                  100万+下载
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 设置列表 - 增强版 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar">
        <div className="glass-card rounded-2xl overflow-hidden animate-liquid-fade">
          {settingsItems.map((item, index) => (
            <button
              key={item.label}
              onClick={() => handleItemClick(item.route)}
              aria-label={item.label}
              className={`w-full flex items-center justify-between p-4 transition-liquid hover-liquid ripple-container animate-liquid-slide-up ${
                index !== settingsItems.length - 1 ? 'border-b' : ''
              }`}
              style={{
                borderBottomColor: index !== settingsItems.length - 1 ? 'var(--color-border-light)' : 'transparent',
                animationDelay: `${index * 50}ms`,
                animationFillMode: 'both'
              }}
            >
              <div className="flex items-center gap-3">
                {/* 渐变图标 */}
                <div 
                  className="w-12 h-12 rounded-xl flex items-center justify-center"
                  style={{
                    background: `linear-gradient(135deg, ${item.color} 0%, ${item.color}80 100%)`,
                    boxShadow: `0 4px 12px ${item.color}30`
                  }}
                >
                  <item.icon size={20} style={{ color: 'var(--color-text-primary)' }} />
                </div>
                <span className="text-sm font-medium" style={{ color: 'var(--color-text-primary)' }}>
                  {item.label}
                </span>
              </div>
              <div className="flex items-center gap-2">
                {item.value && (
                  <span className="text-xs px-2 py-1 rounded-full" style={{ background: 'rgba(255, 255, 255, 0.1)', color: 'var(--color-text-muted)' }}>
                    {item.value}
                  </span>
                )}
                <ChevronRight 
                  size={18} 
                  style={{ color: 'var(--color-text-muted)' }}
                  className="transition-liquid"
                />
              </div>
            </button>
          ))}
        </div>

        {/* 功能介绍卡片 */}
        <div className="smart-recommend mt-6 p-4 animate-liquid-fade">
          <div className="flex items-center gap-2 mb-3">
            <Sparkles size={16} style={{ color: 'var(--color-accent-primary)' }} />
            <span className="text-sm font-bold" style={{ color: 'var(--color-text-primary)' }}>
              功能亮点
            </span>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {[
              { icon: Camera, label: '36+场景识别', color: 'var(--color-feature-scene)' },
              { icon: Palette, label: 'AI智能微调', color: 'var(--color-feature-ai)' },
              { icon: Droplets, label: '14+水印模板', color: 'var(--color-feature-watermark)' },
              { icon: Cloud, label: '云端同步', color: 'var(--color-feature-sync)' },
            ].map((feature, i) => (
              <div 
                key={i}
                className="flex items-center gap-2 p-3 rounded-xl glass-light"
              >
                <feature.icon size={16} style={{ color: feature.color }} />
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
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full glass-light"
          >
            <Info size={12} style={{ color: 'var(--color-text-muted)' }} />
            <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
              Developed by Silas
            </span>
          </div>
          <p className="text-xs mt-2" style={{ color: 'var(--color-text-muted)', opacity: 0.6 }}>
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