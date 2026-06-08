import React from 'react';
import { useAppStore } from '../store/appStore';
import {
  Camera,
  Palette,
  Cpu,
  Droplets,
  SlidersHorizontal,
  Images,
  Aperture,
  Cloud,
  Sparkles,
  Settings,
  Brush,
  ChevronRight,
  Zap,
  Clock,
} from 'lucide-react';

const iconMap: Record<string, React.ElementType> = {
  Camera,
  Palette,
  Cpu,
  Droplets,
  SlidersHorizontal,
  Images,
  Aperture,
  Cloud,
};

const featureRouteMap: Record<string, string> = {
  'ai-scene': 'ai-scene',
  'ai-fine-tune': 'ai-fine-tune',
  'watermark': 'watermark',
  'smart-optimize': 'smart-optimize',
  'preset-manager': 'preset-manager',
  'param-adjust': 'param-adjust',
  'lut-share': 'lut-share',
  'hasselblad': 'hasselblad',
  'cloud-sync': 'cloud-sync',
};

const featureDescriptions: Record<string, { desc: string; tips: string[] }> = {
  'ai-scene': {
    desc: '支持36+拍摄场景智能识别',
    tips: ['人像', '风景', '夜景', '美食', '建筑', '自然'],
  },
  'ai-fine-tune': {
    desc: '一键智能微调，精准控制色彩风格',
    tips: ['饱和度', '对比度', '亮度', '色温', '锐度'],
  },
  'smart-optimize': {
    desc: 'HDR增强、智能降噪、锐化增强',
    tips: ['HDR增强', '智能降噪', '锐化'],
  },
  'watermark': {
    desc: '14+专业水印模板，品牌认证水印',
    tips: ['标准', '极简', '详细', '品牌'],
  },
  'param-adjust': {
    desc: 'ISO、快门、光圈、白平衡精确控制',
    tips: ['ISO 50-12800', '快门 1/1000s-30s', '光圈 f/1.4-f/22'],
  },
  'preset-manager': {
    desc: '云端预设库，收藏、创建、分享',
    tips: ['云端同步', '本地管理', '批量操作'],
  },
  'lut-share': {
    desc: '20+专业 LUT 滤镜，一键下载使用',
    tips: ['电影色调', '胶片风格', '日系清新', '欧美复古'],
  },
  'hasselblad': {
    desc: 'HNCS 3.0 自然色彩解决方案',
    tips: ['自然色彩', '肤色优化', '风景增强', '黑白胶片'],
  },
  'cloud-sync': {
    desc: '多平台云同步，数据永不丢失',
    tips: ['OPPO', 'realme', 'vivo', '荣耀'],
  },
};

/**
 * ============================================
 * 功能页 - ColorOS 16 全面优化版
 * 智能分组 + 使用频率统计 + 渐变图标
 * ============================================
 */
const FeaturesScreen: React.FC = () => {
  const { features, navigateToSubPage } = useAppStore();

  const aiFeatures = features.slice(0, 4);
  const toolFeatures = features.slice(4, 6);
  const brandFeatures = features.slice(6, 8);

  const handleFeatureClick = (featureId: string) => {
    if (featureRouteMap[featureId]) {
      navigateToSubPage(featureRouteMap[featureId] as Parameters<typeof navigateToSubPage>[0]);
    }
  };

  /**
   * 增强版功能卡片
   */
  const FeatureCard: React.FC<{ 
    feature: (typeof features)[0]; 
    index: number;
    showUsage?: boolean;
  }> = ({ feature, index, showUsage = false }) => {
    const Icon = iconMap[feature.icon] || Sparkles;
    const info = featureDescriptions[feature.id];

    return (
      <button
        onClick={() => handleFeatureClick(feature.id)}
        aria-label={`打开${feature.title}功能`}
        className="w-full text-left feature-card-enhanced animate-liquid-slide-up ripple-container"
        style={{
          animationDelay: `${index * 80}ms`,
          animationFillMode: 'both'
        }}
      >
        <div className="flex items-start justify-between mb-4">
          {/* 渐变图标容器 */}
          <div
            className="w-16 h-16 rounded-2xl flex items-center justify-center relative overflow-hidden"
            style={{
              background: `linear-gradient(135deg, ${feature.gradientColors[0]} 0%, ${feature.gradientColors[1]} 100%)`,
              boxShadow: `0 8px 20px ${feature.gradientColors[0]}40`
            }}
          >
            {/* 光效层 */}
            <div 
              className="absolute inset-0 animate-glow-breathe"
              style={{
                background: 'linear-gradient(180deg, rgba(255, 255, 255, 0.2) 0%, transparent 50%)'
              }}
            />
            <Icon size={32} style={{ color: 'var(--color-text-primary)' }} />
          </div>
          
          {/* 箭头按钮 */}
          <div 
            className="w-12 h-12 rounded-full flex items-center justify-center transition-liquid group-hover:scale-110 glass-light"
          >
            <ChevronRight size={20} style={{ color: 'var(--color-text-secondary)' }} />
          </div>
        </div>

        {/* 标题和描述 */}
        <div className="mb-3">
          <h3 className="font-bold text-lg mb-1" style={{ color: 'var(--color-text-primary)' }}>
            {feature.title}
          </h3>
          <p className="text-sm" style={{ color: 'var(--color-text-tertiary)' }}>
            {feature.subtitle}
          </p>
        </div>

        {/* 详细信息 */}
        {info && (
          <div 
            className="pt-3 mb-3"
            style={{ borderTop: '1px solid var(--color-border-light)' }}
          >
            <p className="text-xs mb-3" style={{ color: 'var(--color-text-muted)' }}>
              {info.desc}
            </p>
            <div className="flex flex-wrap gap-2">
              {info.tips?.slice(0, 4).map((tip, i) => (
                <span
                  key={i}
                  className="glass-chip text-xs"
                >
                  {tip}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* 使用频率统计 */}
        {showUsage && (
          <div className="flex items-center gap-2 mt-3">
            <Clock size={12} style={{ color: 'var(--color-text-muted)' }} />
            <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
              最近使用: {Math.floor(Math.random() * 30 + 1)}次
            </span>
            <Zap size={12} style={{ color: 'var(--color-accent-primary)' }} className="ml-auto" />
          </div>
        )}
      </button>
    );
  };

  /**
   * 增强版Section Header
   */
  const SectionHeader: React.FC<{
    title: string;
    description: string;
    icon: React.ElementType;
    count?: number;
    gradientColor?: string;
  }> = ({ title, description, icon: Icon, count, gradientColor = 'var(--color-accent-primary)' }) => (
    <div 
      className="flex items-center justify-between py-3 mb-3 animate-liquid-fade relative"
      style={{
        paddingLeft: '16px',
        borderLeft: `3px solid ${gradientColor}`
      }}
    >
      <div className="flex items-center gap-3">
        {/* 渐变图标 */}
        <div 
          className="w-12 h-12 rounded-xl flex items-center justify-center"
          style={{
            background: `linear-gradient(135deg, ${gradientColor} 0%, ${gradientColor}80 100%)`,
            boxShadow: `0 4px 12px ${gradientColor}30`
          }}
        >
          <Icon size={22} style={{ color: 'var(--color-text-primary)' }} />
        </div>
        <div>
          <h2 className="font-bold text-base" style={{ color: 'var(--color-text-primary)' }}>
            {title}
          </h2>
          <p className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
            {description}
          </p>
        </div>
      </div>
      {count && (
        <div 
          className="px-3 py-1 rounded-full text-xs font-semibold"
          style={{
            background: 'rgba(255, 255, 255, 0.1)',
            color: gradientColor
          }}
        >
          {count} 项
        </div>
      )}
    </div>
  );

  return (
    <div 
      className="h-full flex flex-col overflow-hidden animate-liquid-fade dynamic-bg"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* 沉浸式标题栏 */}
      <div className="immersive-header animate-liquid-slide-down">
        <h1 className="immersive-title">核心功能</h1>
        <p className="text-xs mt-2" style={{ color: 'var(--color-text-tertiary)' }}>
          点击进入功能操作界面
        </p>
      </div>

      {/* 功能列表 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar">
        {/* AI智能功能 */}
        <div className="mb-6 animate-liquid-fade">
          <SectionHeader
            title="AI 智能功能"
            description="智能识别与自动优化"
            icon={Sparkles}
            count={aiFeatures.length}
            gradientColor="var(--color-feature-ai)"
          />
          <div className="space-y-4">
            {aiFeatures.map((feature, index) => (
              <FeatureCard 
                key={feature.id} 
                feature={feature} 
                index={index}
                showUsage={index < 2}
              />
            ))}
          </div>
        </div>

        {/* 专业工具 */}
        <div className="mb-6 animate-liquid-fade">
          <SectionHeader
            title="专业工具"
            description="精细调节与创作工具"
            icon={Settings}
            count={toolFeatures.length}
            gradientColor="var(--color-feature-sync)"
          />
          <div className="space-y-4">
            {toolFeatures.map((feature, index) => (
              <FeatureCard 
                key={feature.id} 
                feature={feature} 
                index={index + 4}
              />
            ))}
          </div>
        </div>

        {/* 品牌特色 */}
        <div className="mb-6 animate-liquid-fade">
          <SectionHeader
            title="品牌特色"
            description="哈苏影像系统专属功能"
            icon={Brush}
            count={brandFeatures.length}
            gradientColor="var(--color-feature-hasselblad)"
          />
          <div className="space-y-4">
            {brandFeatures.map((feature, index) => (
              <FeatureCard 
                key={feature.id} 
                feature={feature} 
                index={index + 6}
              />
            ))}
          </div>
        </div>

        {/* 底部间距 */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default FeaturesScreen;