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
  'hasselblad': 'hasselblad-master',
  'cloud-sync': 'cloud-sync',
  'hasselblad-master': 'hasselblad-master',
  'recipe-manager': 'recipe-manager',
};

const featureDescriptions: Record<string, { desc: string; tips: string[] }> = {
  'ai-scene': {
    desc: '哈苏大师识别：50+场景智能识别',
    tips: ['混合推理', 'TFLite+启发式', 'HNCS优化', '80%+准确率'],
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
    tips: ['标准', '极简', '详细', 'HNCS水印'],
  },
  'param-adjust': {
    desc: '哈苏大师参数：影调/饱和度/对比度精确控制',
    tips: ['影调-30~+30', '柔光模式', '青品调', '暗角'],
  },
  'preset-manager': {
    desc: '云端预设库，收藏、创建、分享',
    tips: ['云端同步', '本地管理', '批量操作'],
  },
  'lut-share': {
    desc: '9款哈苏胶片预设：原生经典/情绪表达/结构时间',
    tips: ['Portra 400', '800T夜景', 'TX400黑白', 'CC经典'],
  },
  'hasselblad': {
    desc: 'HNCS 3.0 自然色彩解决方案',
    tips: ['自然色彩', '肤色优化', '风景增强', '黑白胶片'],
  },
  'hasselblad-master': {
    desc: '哈苏大师之眼：场景识别+胶片推荐+大师参数',
    tips: ['50+场景', '9款胶片', '大师建议', '配方保存'],
  },
  'recipe-manager': {
    desc: '哈苏配方库：保存/分享/导入/复刻',
    tips: ['配方分享', '二维码导入', '收藏管理', '使用统计'],
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
