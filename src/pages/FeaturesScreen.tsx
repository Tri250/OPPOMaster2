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
import { tokens } from '../styles/designTokens';

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
  const brandFeatures = features.slice(6);

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
        className="w-full text-left rounded-2xl overflow-hidden transition-all duration-slow active:scale-[0.98] group hover:shadow-medium"
        style={{
          background: `linear-gradient(135deg, ${feature.gradientColors[0]}, ${feature.gradientColors[1]})`,
          transitionTimingFunction: tokens.animation.easing.spring,
          animation: `fade-in-up 0.4s ${tokens.animation.easing.smooth} ${index * 0.06}s both`,
        }}
      >
        <div className="p-4">
          <div className="flex items-start justify-between mb-3">
            <div
              className="w-14 h-14 rounded-2xl flex items-center justify-center backdrop-blur-md border border-white/10"
              style={{ backgroundColor: 'rgba(255,255,255,0.12)' }}
            >
              <Icon size={28} className="text-white" />
            </div>
            <div
              className="w-10 h-10 rounded-full border border-white/30 flex items-center justify-center transition-all duration-normal group-hover:bg-white/20 group-hover:scale-110"
              style={{ transitionTimingFunction: tokens.animation.easing.spring }}
            >
              <ChevronRight size={18} className="text-white/80" />
            </div>
          </div>

          <div className="mb-2">
            <h3 className="text-white font-bold text-h3">{feature.title}</h3>
            <p className="text-white/75 text-xs mt-1">{feature.subtitle}</p>
          </div>

          {info && (
            <div className="mt-3 pt-3 border-t border-white/10">
              <p className="text-white/60 text-micro mb-2">{info.desc}</p>
              <div className="flex flex-wrap gap-1">
                {info.tips?.slice(0, 4).map((tip, i) => (
                  <span
                    key={i}
                    className="px-2 py-0.5 rounded-full text-micro text-white/80"
                    style={{ backgroundColor: 'rgba(255,255,255,0.12)' }}
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
    <div className="flex items-center justify-between py-2 animate-fade-in-up">
      <div className="flex items-center gap-3">
        <div
          className="w-10 h-10 rounded-xl flex items-center justify-center backdrop-blur-sm"
          style={{ backgroundColor: `${tokens.colors.accent}20` }}
        >
          <Icon size={20} style={{ color: tokens.colors.accent }} />
        </div>
        <div>
          <h2 className="text-master-text-primary font-semibold text-h3">{title}</h2>
          <p className="text-master-text-tertiary text-xs">{description}</p>
        </div>
      </div>
      {count && (
        <span
          className="px-2 py-1 rounded-full text-xs"
          style={{ background: tokens.colors.glass, color: tokens.colors.textTertiary }}
        >
          {count}
        </span>
      )}
    </div>
  );

  return (
    <div className="h-full flex flex-col bg-master-bg overflow-hidden">
      {/* Header */}
      <div className="px-lg pt-sm pb-md">
        <h1 className="text-h1 font-bold text-master-text-primary">核心功能</h1>
        <p className="text-master-text-tertiary text-xs mt-0.5">点击进入功能操作界面</p>
      </div>

      {/* Features List */}
      <div className="flex-1 overflow-y-auto px-lg pb-lg space-y-6 scrollbar-hide">
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

      {/* Styles */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default FeaturesScreen;
