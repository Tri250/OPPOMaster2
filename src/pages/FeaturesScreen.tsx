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
  Layers,
  TrendingUp,
  BarChart3,
  Heart,
  MapPin,
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
  Layers,
  TrendingUp,
  BarChart3,
  Heart,
  MapPin,
};

const featureRouteMap: Record<string, string> = {
  'ai-scene': 'ai-scene',
  'ai-fine-tune': 'ai-fine-tune',
  'watermark': 'watermark',
  'smart-optimize': 'smart-optimize',
  'hsl-adjustment': 'hsl-adjustment',
  'batch-processing': 'batch-processing',
  'raw-processing': 'raw-processing',
  'tone-curve': 'tone-curve',
  'histogram': 'histogram',
  'favorites': 'favorites',
  'trend-2026': 'trend-2026',
  'scene-detail': 'scene-detail',
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
  'hsl-adjustment': {
    desc: '8色独立HSL调节，精准色彩控制',
    tips: ['红/橙/黄/绿', '青/蓝/紫/粉'],
  },
  'batch-processing': {
    desc: '多图同时批量处理，提高效率',
    tips: ['批量导入', '统一参数', '批量导出'],
  },
  'raw-processing': {
    desc: '专业RAW格式处理，最大程度保留细节',
    tips: ['DNG/CR2/NEF', '曝光补偿', '色温调节'],
  },
  'tone-curve': {
    desc: '自定义RGB曲线，精准影调控制',
    tips: ['RGB通道', '控制点', '曲线预设'],
  },
  'histogram': {
    desc: 'RGB直方图查看，曝光警告提示',
    tips: ['实时直方图', '过曝警告', '欠曝提示'],
  },
  'favorites': {
    desc: '收藏夹管理，分类整理喜爱的预设',
    tips: ['分类管理', '快速访问', '自定义文件夹'],
  },
  'trend-2026': {
    desc: '2026年度流行趋势，风格色彩预览',
    tips: ['流行色', '趋势风格', '季节推荐'],
  },
  'scene-detail': {
    desc: '细分场景参数，一键应用优化',
    tips: ['场景参数', '拍摄技巧', '优化建议'],
  },
};

const FeaturesScreen: React.FC = () => {
  const { features, navigateToSubPage } = useAppStore();

  const aiFeatures = features.slice(0, 4);
  const toolFeatures = features.slice(4, 9);
  const brandFeatures = features.slice(9, 13);

  const handleFeatureClick = (featureId: string) => {
    if (featureRouteMap[featureId]) {
      navigateToSubPage(featureRouteMap[featureId] as any);
    }
  };

  const FeatureCard: React.FC<{ 
    feature: (typeof features)[0]; 
    index: number;
  }> = ({ feature, index }) => {
    const Icon = iconMap[feature.icon] || Sparkles;
    const route = featureRouteMap[feature.id];
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
        <h1 className="text-xl font-bold text-white">摄影工具</h1>
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
              <FeatureCard key={feature.id} feature={feature} index={index} />
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
              <FeatureCard key={feature.id} feature={feature} index={index} />
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
