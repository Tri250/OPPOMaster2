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
  Star,
  Crown,
} from 'lucide-react';

/**
 * ============================================
 * 关于页 - 哈苏品牌高端视觉风格
 * 精选大品牌设计
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
      className="h-full flex flex-col overflow-hidden"
      style={{ background: '#0a0a0a' }}
    >
      {/* 哈苏品牌沉浸式标题栏 */}
      <div 
        className="px-4 pt-4 pb-3"
        style={{
          background: 'linear-gradient(180deg, rgba(255, 107, 53, 0.15) 0%, transparent 100%)'
        }}
      >
        <h1 
          className="text-xl font-bold"
          style={{ 
            color: '#FF6B35',
            letterSpacing: '1px'
          }}
        >
          关于
        </h1>
      </div>

      {/* 哈苏品牌Logo卡片 - 精选大品牌视觉 */}
      <div className="px-4 pb-4">
        <div 
          className="relative rounded-2xl overflow-hidden"
          style={{
            background: 'linear-gradient(135deg, #FF6B35 0%, #FF8C5A 40%, #FFD93D 100%)',
            boxShadow: '0 20px 50px rgba(255, 107, 53, 0.35), 0 0 60px rgba(255, 107, 53, 0.2)'
          }}
        >
          {/* 高光层 */}
          <div 
            className="absolute inset-0"
            style={{
              background: 'linear-gradient(180deg, rgba(255, 255, 255, 0.25) 0%, transparent 40%, rgba(0, 0, 0, 0.1) 100%)'
            }}
          />
          
          {/* 内部内容区 */}
          <div 
            className="relative m-3 rounded-xl p-6 text-center"
            style={{
              background: 'rgba(10, 10, 10, 0.85)',
              backdropFilter: 'blur(20px)'
            }}
          >
            {/* Logo - 哈苏橙渐变 */}
            <div 
              className="w-24 h-24 mx-auto mb-5 rounded-2xl flex items-center justify-center relative"
              style={{
                background: 'linear-gradient(135deg, #FF6B35 0%, #FF9F6B 50%, #FFD93D 100%)',
                boxShadow: '0 12px 30px rgba(255, 107, 53, 0.5), inset 0 2px 4px rgba(255, 255, 255, 0.3)'
              }}
            >
              {/* Logo内光效 */}
              <div 
                className="absolute inset-0 rounded-2xl"
                style={{
                  background: 'linear-gradient(180deg, rgba(255, 255, 255, 0.2) 0%, transparent 50%)'
                }}
              />
              <Camera size={48} style={{ color: '#FFFFFF' }} />
            </div>

            {/* 应用名称 - 哈苏橙渐变文字 */}
            <h2 
              className="text-2xl font-bold mb-2"
              style={{
                background: 'linear-gradient(135deg, #FF6B35 0%, #FF9F6B 50%, #FFD93D 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                letterSpacing: '2px'
              }}
            >
              小O帮帮
            </h2>
            
            {/* 应用描述 */}
            <p 
              className="text-sm mb-5"
              style={{ color: 'rgba(255, 255, 255, 0.7)' }}
            >
              专业影像参数管理工具
            </p>

            {/* 版本信息 - 哈苏橙风格 */}
            <div 
              className="inline-flex items-center gap-3 px-5 py-2 rounded-full"
              style={{
                background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.2) 0%, rgba(255, 159, 107, 0.1) 100%)',
                border: '1px solid rgba(255, 107, 53, 0.4)'
              }}
            >
              <span className="text-sm" style={{ color: 'rgba(255, 255, 255, 0.85)' }}>
                v1.3.1
              </span>
              <div 
                className="w-2 h-2 rounded-full animate-pulse"
                style={{ background: '#4CAF50' }}
              />
              <span 
                className="text-sm font-semibold"
                style={{ color: '#4CAF50' }}
              >
                最新版本
              </span>
            </div>

            {/* 品牌认证徽章 */}
            <div 
              className="flex items-center justify-center gap-4 mt-5"
            >
              {/* 哈苏认证 */}
              <div 
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
                style={{
                  background: 'rgba(255, 107, 53, 0.15)',
                  border: '1px solid rgba(255, 107, 53, 0.3)'
                }}
              >
                <Crown size={14} style={{ color: '#FF6B35' }} />
                <span className="text-xs font-medium" style={{ color: '#FF6B35' }}>
                  哈苏认证
                </span>
              </div>
              
              {/* 用户数 */}
              <div 
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
                style={{
                  background: 'rgba(244, 67, 54, 0.15)',
                  border: '1px solid rgba(244, 67, 54, 0.3)'
                }}
              >
                <Heart size={14} style={{ color: '#F44336' }} />
                <span className="text-xs font-medium" style={{ color: '#F44336' }}>
                  10万+用户
                </span>
              </div>
              
              {/* 下载量 */}
              <div 
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-full"
                style={{
                  background: 'rgba(255, 107, 53, 0.15)',
                  border: '1px solid rgba(255, 107, 53, 0.3)'
                }}
              >
                <Download size={14} style={{ color: '#FF6B35' }} />
                <span className="text-xs font-medium" style={{ color: '#FF6B35' }}>
                  100万+
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 设置列表 - 哈苏橙精选风格 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        <div 
          className="rounded-2xl overflow-hidden"
          style={{
            background: '#1a1a1a',
            border: '1px solid rgba(255, 107, 53, 0.2)',
            boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)'
          }}
        >
          {settingsItems.map((item, index) => (
            <button
              key={item.label}
              onClick={() => handleItemClick(item.route)}
              aria-label={item.label}
              className="w-full flex items-center justify-between p-4 transition-all duration-200 hover:bg-white/5"
              style={{
                borderBottom: index !== settingsItems.length - 1 
                  ? '1px solid rgba(255, 107, 53, 0.1)' 
                  : 'none',
                cursor: 'pointer'
              }}
            >
              <div className="flex items-center gap-3">
                {/* 哈苏橙精选图标 */}
                <div 
                  className="w-12 h-12 rounded-xl flex items-center justify-center"
                  style={{
                    background: 'linear-gradient(135deg, #FF6B35 0%, #FF8C5A 100%)',
                    boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)'
                  }}
                >
                  <item.icon size={22} style={{ color: '#FFFFFF' }} />
                </div>
                <span 
                  className="text-base font-medium"
                  style={{ color: '#FFFFFF' }}
                >
                  {item.label}
                </span>
              </div>
              <div className="flex items-center gap-2">
                {item.value && (
                  <span 
                    className="text-xs px-3 py-1 rounded-full font-medium"
                    style={{ 
                      background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.2) 0%, rgba(255, 159, 107, 0.1) 100%)',
                      border: '1px solid rgba(255, 107, 53, 0.3)',
                      color: '#FF6B35'
                    }}
                  >
                    {item.value}
                  </span>
                )}
                <ChevronRight 
                  size={20} 
                  style={{ color: '#FF6B35' }}
                />
              </div>
            </button>
          ))}
        </div>

        {/* 功能亮点卡片 - 哈苏橙精选风格 */}
        <div 
          className="mt-5 p-5 rounded-2xl"
          style={{
            background: 'linear-gradient(135deg, rgba(255, 107, 53, 0.15) 0%, rgba(255, 159, 107, 0.08) 100%)',
            border: '1px solid rgba(255, 107, 53, 0.25)',
            boxShadow: '0 8px 24px rgba(255, 107, 53, 0.1)'
          }}
        >
          <div className="flex items-center gap-2 mb-4">
            <div 
              className="w-8 h-8 rounded-lg flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #FF6B35 0%, #FF8C5A 100%)'
              }}
            >
              <Star size={16} style={{ color: '#FFFFFF' }} />
            </div>
            <span 
              className="text-base font-bold"
              style={{ color: '#FF6B35' }}
            >
              核心功能
            </span>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {[
              { icon: Camera, label: '36+场景智能识别' },
              { icon: Sparkles, label: 'AI智能微调' },
              { icon: Award, label: '14+专业水印' },
              { icon: Download, label: '云端实时同步' },
            ].map((feature, i) => (
              <div 
                key={i}
                className="flex items-center gap-2 p-3 rounded-xl"
                style={{
                  background: 'rgba(255, 255, 255, 0.05)',
                  border: '1px solid rgba(255, 107, 53, 0.15)'
                }}
              >
                <div 
                  className="w-6 h-6 rounded-lg flex items-center justify-center"
                  style={{
                    background: 'rgba(255, 107, 53, 0.2)'
                  }}
                >
                  <feature.icon size={14} style={{ color: '#FF6B35' }} />
                </div>
                <span 
                  className="text-xs font-medium"
                  style={{ color: 'rgba(255, 255, 255, 0.85)' }}
                >
                  {feature.label}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* 开发者信息 - 哈苏橙风格 */}
        <div className="mt-5 text-center">
          <div 
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full"
            style={{
              background: 'rgba(255, 107, 53, 0.1)',
              border: '1px solid rgba(255, 107, 53, 0.2)'
            }}
          >
            <Info size={12} style={{ color: '#FF6B35' }} />
            <span className="text-xs" style={{ color: '#FF6B35' }}>
              Developed by Silas
            </span>
          </div>
          <p 
            className="text-xs mt-2"
            style={{ color: 'rgba(255, 255, 255, 0.4)' }}
          >
            © 2024 小O帮帮. All rights reserved.
          </p>
        </div>

        {/* 底部间距 */}
        <div className="h-6" />
      </div>
    </div>
  );
};

export default AboutScreen;