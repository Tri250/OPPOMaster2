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

  const FeatureCard: React.FC<{ 
    feature: (typeof features)[0]; 
    index: number;
  }> = ({ feature, index }) => {
    const Icon = iconMap[feature.icon] || Sparkles;
    const info = featureDescriptions[feature.id];

    return (
      <button
        onClick={() => handleFeatureClick(feature.id)}
        aria-label={`打开${feature.title}功能`}
        className="w-full text-left rounded-2xl overflow-hidden transition-spring group animate-liquid-slide-up"
        style={{
          background: `linear-gradient(135deg, ${feature.gradientColors[0]}, ${feature.gradientColors[1]})`,
          boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',
          animationDelay: `${index * 80}ms`,
          animationFillMode: 'both'
        }}
      >
        <div className="p-4">
          <div className="flex items-start justify-between mb-3">
            <div
              className="w-14 h-14 rounded-2xl flex items-center justify-center glass-light"
            >
              <Icon size={28} style={{ color: 'var(--color-text-primary)' }} />
            </div>
            <div 
              className="w-10 h-10 rounded-full flex items-center justify-center transition-liquid group-hover:scale-110"
              style={{
                border: '1px solid rgba(255, 255, 255, 0.3)',
                background: 'rgba(255, 255, 255, 0.1)'
              }}
            >
              <ChevronRight size={18} style={{ color: 'rgba(255, 255, 255, 0.7)' }} />
            </div>
          </div>

          <div className="mb-2">
            <h3 style={{ color: 'var(--color-text-primary)' }} className="font-bold text-lg">
              {feature.title}
            </h3>
            <p style={{ color: 'rgba(255, 255, 255, 0.7)' }} className="text-xs mt-1">
              {feature.subtitle}
            </p>
          </div>

          {info && (
            <div className="mt-3 pt-3" style={{ borderTop: '1px solid rgba(255, 255, 255, 0.1)' }}>
              <p style={{ color: 'rgba(255, 255, 255, 0.5)' }} className="text-[10px] mb-2">
                {info.desc}
              </p>
              <div className="flex flex-wrap gap-1">
                {info.tips?.slice(0, 4).map((tip, i) => (
                  <span
                    key={i}
                    className="px-2 py-0.5 rounded-full text-[10px]"
                    style={{
                      background: 'rgba(255, 255, 255, 0.1)',
                      color: 'rgba(255, 255, 255, 0.7)'
                    }}
                  >
                    {tip}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      </button>
    );
  };

  const SectionHeader: React.FC<{
    title: string;
    description: string;
    icon: React.ElementType;
    count?: number;
  }> = ({ title, description, icon: Icon, count }) => (
    <div className="flex items-center justify-between py-2 animate-liquid-fade">
      <div className="flex items-center gap-3">
        <div 
          className="w-10 h-10 rounded-xl flex items-center justify-center glass-light"
          style={{ boxShadow: '0 0 12px rgba(255, 107, 53, 0.2)' }}
        >
          <Icon size={20} style={{ color: 'var(--color-accent-primary)' }} />
        </div>
        <div>
          <h2 style={{ color: 'var(--color-text-primary)' }} className="font-semibold">
            {title}
          </h2>
          <p style={{ color: 'var(--color-text-muted)' }} className="text-xs">
            {description}
          </p>
        </div>
      </div>
      {count && (
        <span 
          className="px-2 py-1 rounded-full text-xs"
          style={{
            background: 'rgba(255, 255, 255, 0.1)',
            color: 'var(--color-text-muted)'
          }}
        >
          {count}
        </span>
      )}
    </div>
  );

  return (
    <div 
      className="h-full flex flex-col overflow-hidden animate-liquid-fade"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* Header */}
      <div className="px-4 pt-3 pb-3">
        <h1 style={{ color: 'var(--color-text-primary)' }} className="text-xl font-bold">
          核心功能
        </h1>
        <p style={{ color: 'var(--color-text-muted)' }} className="text-xs">
          点击进入功能操作界面
        </p>
      </div>

      {/* Features List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-6 scrollbar-hide custom-scrollbar">
        {/* AI Features */}
        <div>
          <SectionHeader
            title="AI 智能功能"
            description="智能识别与自动优化"
            icon={Sparkles}
            count={aiFeatures.length}
          />
          <div className="space-y-3 mt-2">
            {aiFeatures.map((feature, index) => (
              <FeatureCard key={feature.id} feature={feature} index={index} />
            ))}
          </div>
        </div>

        {/* Tool Features */}
        <div>
          <SectionHeader
            title="专业工具"
            description="精细调节与创作工具"
            icon={Settings}
            count={toolFeatures.length}
          />
          <div className="space-y-3 mt-2">
            {toolFeatures.map((feature, index) => (
              <FeatureCard key={feature.id} feature={feature} index={index + 4} />
            ))}
          </div>
        </div>

        {/* Brand Features */}
        <div>
          <SectionHeader
            title="品牌特色"
            description="哈苏影像系统专属功能"
            icon={Brush}
            count={brandFeatures.length}
          />
          <div className="space-y-3 mt-2">
            {brandFeatures.map((feature, index) => (
              <FeatureCard key={feature.id} feature={feature} index={index + 6} />
            ))}
          </div>
        </div>

        {/* Bottom Spacing */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default FeaturesScreen;