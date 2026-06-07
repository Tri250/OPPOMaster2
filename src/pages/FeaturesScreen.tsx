import React, { useState } from 'react';
import { useAppStore } from '../store/appStore';
import {
  Cpu,
  Palette,
  Layers,
  Sparkles,
  Share2,
  ChevronRight,
  Zap,
  Target,
  Wand2,
  SlidersHorizontal,
  TrendingUp,
  BarChart3,
  Aperture,
  Images,
  Heart,
  Crown,
  Camera,
  Gauge,
  Timer,
  Droplets,
  MapPin,
  FileCode,
  Users,
} from 'lucide-react';

const iconMap: Record<string, React.ElementType> = {
  Cpu,
  Palette,
  Layers,
  Sparkles,
  Share2,
  Droplets,
  MapPin,
  Zap,
  Target,
  Wand2,
  SlidersHorizontal,
  TrendingUp,
  BarChart3,
  Aperture,
  Images,
  Heart,
  Crown,
  Camera,
  Gauge,
  Timer,
  FileCode,
  Users,
};

const featureRouteMap: Record<string, string> = {
  'ai-engine': 'ai-fine-tune',
  'pro-color': 'hsl-adjustment',
  'workflow': 'raw-processing',
  'preset-center': 'favorites',
  'lut-share': 'lut-share',
};

const subFeatureRouteMap: Record<string, string> = {
  'scene-recognition': 'ai-scene',
  'one-click-tune': 'ai-fine-tune',
  'smart-enhance': 'smart-optimize',
  'scene-detail': 'scene-detail',
  'hsl': 'hsl-adjustment',
  'curve': 'tone-curve',
  'histogram': 'histogram',
  'raw': 'raw-processing',
  'batch': 'batch-processing',
  'watermark': 'watermark',
  'favorites': 'favorites',
  'trend': 'trend-2026',
  'brand': 'favorites',
  'lut-library': 'lut-share',
  'shot-share': 'lut-share',
  'community': 'lut-share',
};

const FeaturesScreen: React.FC = () => {
  const { features, navigateToSubPage } = useAppStore();
  const [expandedFeature, setExpandedFeature] = useState<string | null>(null);

  const handleFeatureClick = (featureId: string) => {
    if (featureRouteMap[featureId]) {
      navigateToSubPage(featureRouteMap[featureId] as any);
    }
  };

  const handleSubFeatureClick = (subFeatureId: string) => {
    if (subFeatureRouteMap[subFeatureId]) {
      navigateToSubPage(subFeatureRouteMap[subFeatureId] as any);
    }
  };

  const FeatureCard: React.FC<{ 
    feature: (typeof features)[0]; 
    index: number;
  }> = ({ feature, index }) => {
    const Icon = iconMap[feature.icon] || Sparkles;
    const hasSubFeatures = feature.subFeatures && feature.subFeatures.length > 0;
    const isExpanded = expandedFeature === feature.id;

    return (
      <div className="w-full rounded-2xl overflow-hidden transition-all duration-300"
        style={{
          background: `linear-gradient(135deg, ${feature.gradientColors[0]}, ${feature.gradientColors[1]})`,
        }}
      >
        {/* Main Card */}
        <button
          onClick={() => {
            if (hasSubFeatures) {
              setExpandedFeature(isExpanded ? null : feature.id);
            } else {
              handleFeatureClick(feature.id);
            }
          }}
          className="w-full p-4 text-left"
        >
          <div className="flex items-start justify-between mb-3">
            <div
              className="w-14 h-14 rounded-2xl flex items-center justify-center backdrop-blur-sm"
              style={{ backgroundColor: 'rgba(255,255,255,0.15)' }}
            >
              <Icon size={28} className="text-white" />
            </div>
            <div className="flex items-center gap-2">
              {feature.showToggle && (
                <div 
                  className={`w-12 h-7 rounded-full p-1 transition-colors ${feature.enabled ? 'bg-green-500' : 'bg-white/20'}`}
                  onClick={(e) => {
                    e.stopPropagation();
                  }}
                >
                  <div className={`w-5 h-5 rounded-full bg-white transition-transform ${feature.enabled ? 'translate-x-5' : 'translate-x-0'}`} />
                </div>
              )}
              <div className="w-10 h-10 rounded-full border border-white/30 flex items-center justify-center hover:bg-white/20 transition-colors">
                <ChevronRight size={18} className={`text-white/70 transition-transform ${isExpanded ? 'rotate-90' : ''}`} />
              </div>
            </div>
          </div>

          <div className="mb-2">
            <h3 className="text-white font-bold text-lg">{feature.title}</h3>
            <p className="text-white/70 text-xs mt-1">{feature.subtitle}</p>
          </div>

          {/* Sub Features Preview */}
          {hasSubFeatures && !isExpanded && (
            <div className="flex flex-wrap gap-2 mt-3 pt-3 border-t border-white/10">
              {feature.subFeatures!.slice(0, 4).map((sub) => (
                <span
                  key={sub.id}
                  className="px-3 py-1.5 rounded-full bg-white/10 text-white/80 text-xs"
                >
                  {sub.name}
                </span>
              ))}
            </div>
          )}
        </button>

        {/* Expanded Sub Features */}
        {hasSubFeatures && isExpanded && (
          <div className="px-4 pb-4 space-y-2">
            {feature.subFeatures!.map((sub) => {
              const subIconMap: Record<string, React.ElementType> = {
                'scene-recognition': Camera,
                'one-click-tune': Wand2,
                'smart-enhance': Zap,
                'scene-detail': MapPin,
                'hsl': SlidersHorizontal,
                'curve': TrendingUp,
                'histogram': BarChart3,
                'raw': Aperture,
                'batch': Images,
                'watermark': Droplets,
                'favorites': Heart,
                'trend': Crown,
                'brand': Sparkles,
                'lut-library': FileCode,
                'shot-share': Camera,
                'community': Users,
              };
              const SubIcon = subIconMap[sub.id] || Target;
              
              return (
                <button
                  key={sub.id}
                  onClick={() => handleSubFeatureClick(sub.id)}
                  className="w-full flex items-center gap-4 p-4 rounded-xl bg-white/10 hover:bg-white/15 transition-all"
                >
                  <div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center">
                    <SubIcon size={20} className="text-white" />
                  </div>
                  <div className="flex-1 text-left">
                    <h4 className="text-white font-semibold text-sm">{sub.name}</h4>
                    <p className="text-white/50 text-xs">{sub.desc}</p>
                  </div>
                  <ChevronRight size={16} className="text-white/50" />
                </button>
              );
            })}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3">
        <h1 className="text-xl font-bold text-white">摄影工具</h1>
        <p className="text-white/50 text-xs">Find 产品经理精心设计 · 功能整合优化</p>
      </div>

      {/* Features List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-4 scrollbar-hide">
        {/* AI Section */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-green-500/20 flex items-center justify-center">
              <Cpu size={16} className="text-green-400" />
            </div>
            <div>
              <h2 className="text-white font-semibold text-sm">AI 智能引擎</h2>
              <p className="text-white/40 text-xs">场景识别 · 一键优化 · 细分参数</p>
            </div>
          </div>
          <FeatureCard feature={features[0]} index={0} />
        </div>

        {/* Color Section */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-orange-500/20 flex items-center justify-center">
              <Palette size={16} className="text-orange-400" />
            </div>
            <div>
              <h2 className="text-white font-semibold text-sm">专业调色</h2>
              <p className="text-white/40 text-xs">精准色彩控制</p>
            </div>
          </div>
          <FeatureCard feature={features[1]} index={1} />
        </div>

        {/* Workflow Section */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-amber-700/20 flex items-center justify-center">
              <Layers size={16} className="text-amber-500" />
            </div>
            <div>
              <h2 className="text-white font-semibold text-sm">工作流</h2>
              <p className="text-white/40 text-xs">RAW · 批量 · 水印</p>
            </div>
          </div>
          <FeatureCard feature={features[2]} index={2} />
        </div>

        {/* Preset Section */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-yellow-500/20 flex items-center justify-center">
              <Sparkles size={16} className="text-yellow-400" />
            </div>
            <div>
              <h2 className="text-white font-semibold text-sm">预设中心</h2>
              <p className="text-white/40 text-xs">收藏 · 趋势 · 品牌</p>
            </div>
          </div>
          <FeatureCard feature={features[3]} index={3} />
        </div>

        {/* LUT & Share Section */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-blue-500/20 flex items-center justify-center">
              <Share2 size={16} className="text-blue-400" />
            </div>
            <div>
              <h2 className="text-white font-semibold text-sm">LUT资源与分享</h2>
              <p className="text-white/40 text-xs">LUT库 · 拍摄分享 · 社区</p>
            </div>
          </div>
          <FeatureCard feature={features[4]} index={4} />
        </div>

        {/* Bottom Spacing */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default FeaturesScreen;
