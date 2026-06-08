import React from 'react';
import { useAppStore } from '../store/appStore';
import {
  Camera,
  Palette,
  Cpu,
  Droplets,
  SlidersHorizontal,
  Aperture,
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
  Aperture,
};

const featureRouteMap: Record<string, string> = {
  'ai-scene': 'ai-scene',
  'camera-scene': 'camera-scene',
  'ai-fine-tune': 'ai-fine-tune',
  'watermark': 'watermark',
  'smart-optimize': 'smart-optimize',
  'param-adjust': 'param-adjust',
};

const featureDescriptions: Record<string, { desc: string; tips: string[] }> = {
  'ai-scene': {
    desc: '支持36+拍摄场景智能识别',
    tips: ['人像', '风景', '夜景', '美食', '建筑', '自然'],
  },
  'camera-scene': {
    desc: '相机实时场景识别，推荐哈苏大师参数',
    tips: ['实时识别', '智能推荐', '大师样张', '拍摄指导'],
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
};

const FeaturesScreen: React.FC = () => {
  const { features, navigateToSubPage } = useAppStore();

  // 只保留核心功能：AI场景、相机场景、AI微调、智能优化、水印、参数调节、哈苏色彩
  // 预设管理和云同步已移到设置页面
  const aiFeatures = features.filter(f =>
    ['ai-scene', 'camera-scene', 'ai-fine-tune', 'smart-optimize', 'watermark'].includes(f.id)
  );
  const toolFeatures = features.filter(f => f.id === 'param-adjust');
  const brandFeatures = features.filter(f => f.id === 'hasselblad');

  const handleFeatureClick = (featureId: string) => {
    if (featureRouteMap[featureId]) {
      navigateToSubPage(featureRouteMap[featureId] as any);
    }
  };

  const FeatureCard: React.FC<{ 
    feature: (typeof features)[0]; 
  }> = ({ feature }) => {
    const Icon = iconMap[feature.icon] || Sparkles;
    const info = featureDescriptions[feature.id];

    return (
      <button
        onClick={() => handleFeatureClick(feature.id)}
        className="w-full text-left rounded-2xl overflow-hidden transition-all duration-300 hover:scale-[1.02] active:scale-[0.98] group"
        style={{
          background: `linear-gradient(135deg, ${feature.gradientColors[0]}, ${feature.gradientColors[1]})`,
        }}
      >
        <div className="p-4">
          <div className="flex items-start justify-between mb-3">
            <div
              className="w-14 h-14 rounded-2xl flex items-center justify-center backdrop-blur-sm"
              style={{ backgroundColor: 'rgba(255,255,255,0.15)' }}
            >
              <Icon size={28} className="text-white" />
            </div>
            <div className="w-10 h-10 rounded-full border border-white/30 flex items-center justify-center group-hover:bg-white/20 transition-colors">
              <ChevronRight size={18} className="text-white/70" />
            </div>
          </div>

          <div className="mb-2">
            <h3 className="text-white font-bold text-lg">{feature.title}</h3>
            <p className="text-white/70 text-xs mt-1">{feature.subtitle}</p>
          </div>

          {info && (
            <div className="mt-3 pt-3 border-t border-white/10">
              <p className="text-white/50 text-[10px] mb-2">{info.desc}</p>
              <div className="flex flex-wrap gap-1">
                {info.tips?.slice(0, 4).map((tip, i) => (
                  <span
                    key={i}
                    className="px-2 py-0.5 rounded-full bg-white/10 text-white/70 text-[10px]"
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
    <div className="flex items-center justify-between py-2">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/20 flex items-center justify-center backdrop-blur-sm">
          <Icon size={20} className="text-[#FF6B35]" />
        </div>
        <div>
          <h2 className="text-white font-semibold">{title}</h2>
          <p className="text-white/50 text-xs">{description}</p>
        </div>
      </div>
      {count && (
        <span className="px-2 py-1 rounded-full bg-white/10 text-white/50 text-xs">
          {count}
        </span>
      )}
    </div>
  );

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3">
        <h1 className="text-xl font-bold text-white">核心功能</h1>
        <p className="text-white/50 text-xs">点击进入功能操作界面</p>
      </div>

      {/* Features List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-6 scrollbar-hide">
        {/* AI Features */}
        <div>
          <SectionHeader
            title="AI 智能功能"
            description="智能识别与自动优化"
            icon={Sparkles}
            count={aiFeatures.length}
          />
          <div className="space-y-3 mt-2">
            {aiFeatures.map((feature) => (
              <FeatureCard key={feature.id} feature={feature} />
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
            {toolFeatures.map((feature) => (
              <FeatureCard key={feature.id} feature={feature} />
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
            {brandFeatures.map((feature) => (
              <FeatureCard key={feature.id} feature={feature} />
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