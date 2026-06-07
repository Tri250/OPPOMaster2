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
  Check,
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

const FeaturesScreen: React.FC = () => {
  const { features, toggleFeature } = useAppStore();

  const aiFeatures = features.slice(0, 3);
  const toolFeatures = features.slice(3, 6);
  const brandFeatures = features.slice(6, 8);

  const FeatureCard: React.FC<{ feature: (typeof features)[0] }> = ({ feature }) => {
    const Icon = iconMap[feature.icon] || Sparkles;

    return (
      <div
        className="relative rounded-2xl overflow-hidden cursor-pointer transition-all duration-300 hover:scale-[1.02] active:scale-[0.98]"
        style={{
          background: feature.enabled
            ? `linear-gradient(135deg, ${feature.gradientColors[0]}, ${feature.gradientColors[1]})`
            : 'linear-gradient(135deg, #2a2a2a, #1a1a1a)',
        }}
        onClick={() => feature.showToggle && toggleFeature(feature.id)}
      >
        <div className="p-4">
          <div className="flex items-start justify-between mb-3">
            <div
              className="w-12 h-12 rounded-xl flex items-center justify-center"
              style={{ backgroundColor: 'rgba(255,255,255,0.15)' }}
            >
              <Icon size={24} className="text-white" />
            </div>
            {feature.showToggle ? (
              <div
                className={`w-12 h-6 rounded-full relative transition-colors duration-300 ${
                  feature.enabled ? 'bg-white/30' : 'bg-gray-600'
                }`}
              >
                <div
                  className={`absolute top-0.5 w-5 h-5 rounded-full bg-white transition-all duration-300 ${
                    feature.enabled ? 'left-6' : 'left-0.5'
                  }`}
                />
              </div>
            ) : (
              <div className="w-9 h-9 rounded-full border border-white/30 flex items-center justify-center">
                <Brush size={16} className="text-white/70" />
              </div>
            )}
          </div>

          <div className="flex items-center gap-2 mb-1">
            <h3 className="text-white font-bold text-base">{feature.title}</h3>
            {feature.enabled && feature.showToggle && (
              <div className="w-4 h-4 rounded-full bg-white/30 flex items-center justify-center">
                <Check size={10} className="text-white" />
              </div>
            )}
          </div>

          <p
            className={`text-xs ${
              feature.enabled ? 'text-white/80' : 'text-white/50'
            }`}
          >
            {feature.subtitle}
          </p>
        </div>
      </div>
    );
  };

  const SectionHeader: React.FC<{
    title: string;
    description: string;
    icon: React.ElementType;
  }> = ({ title, description, icon: Icon }) => (
    <div className="flex items-center gap-3 py-2">
      <div className="w-8 h-8 rounded-full bg-[#FF6B35]/20 flex items-center justify-center">
        <Icon size={16} className="text-[#FF6B35]" />
      </div>
      <div>
        <h2 className="text-white font-semibold text-sm">{title}</h2>
        <p className="text-white/50 text-xs">{description}</p>
      </div>
    </div>
  );

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3">
        <h1 className="text-xl font-bold text-white">核心功能</h1>
        <p className="text-white/50 text-xs">AI驱动的专业影像体验</p>
      </div>

      {/* Features List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-4">
        {/* AI Features */}
        <div>
          <SectionHeader
            title="AI 智能功能"
            description="智能识别与自动优化"
            icon={Sparkles}
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
