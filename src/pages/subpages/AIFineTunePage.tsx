import React, { useState, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Sparkles, RefreshCw, Check, Wand2, Brain, Target, Layers, Zap, Heart, Camera } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { userImageStore, UserImage } from '../../store/userImageStore';
import { imageAnalysisService, ImageAnalysis, RecommendedParams } from '../../services/imageAnalysisService';

interface AIFineTunePreset {
  id: string;
  name: string;
  description: string;
  category: 'portrait' | 'landscape' | 'night' | 'food' | 'street' | 'film' | 'fresh' | 'bw';
  icon: string;
  // 2026 流行风格
  isTrending?: boolean;
  changes: Array<{ key: string; label: string; value: number; before: number }>;
}

// 2026 年小红书/微博摄影师喜欢的风格
const aiPresets: AIFineTunePreset[] = [
  // 基础 7 类
  {
    id: 'auto_optimize', name: '智能优化', description: 'AI 自动分析并优化全局参数', category: 'fresh', icon: '✨',
    changes: [
      { key: 'saturation', label: '饱和度', value: 10, before: 0 },
      { key: 'contrast', label: '对比度', value: 8, before: 0 },
      { key: 'brightness', label: '亮度', value: 5, before: 0 },
      { key: 'sharpness', label: '锐度', value: 15, before: 0 },
      { key: 'clarity', label: '清晰度', value: 10, before: 0 },
    ],
  },
  {
    id: 'hdr_enhance', name: 'HDR 增强', description: '提升动态范围，保留高光与暗部细节', category: 'landscape', icon: '🌅',
    changes: [
      { key: 'contrast', label: '对比度', value: 20, before: 0 },
      { key: 'highlights', label: '高光', value: -30, before: 0 },
      { key: 'shadows', label: '阴影', value: 25, before: 0 },
      { key: 'clarity', label: '清晰度', value: 15, before: 0 },
    ],
  },
  {
    id: 'noise_reduce', name: '降噪处理', description: '降低暗光环境下的画面噪点', category: 'night', icon: '🔇',
    changes: [
      { key: 'noiseReduction', label: '降噪', value: 40, before: 0 },
      { key: 'sharpness', label: '锐度', value: -5, before: 0 },
    ],
  },
  {
    id: 'skin_smooth', name: '肤色优化', description: '智能美肤，保留真实肤质细节', category: 'portrait', icon: '👤',
    changes: [
      { key: 'skinSmooth', label: '美肤', value: 25, before: 0 },
      { key: 'warmth', label: '色温', value: 5, before: 0 },
      { key: 'saturation', label: '饱和度', value: -5, before: 0 },
    ],
  },
  {
    id: 'sky_enhance', name: '天空增强', description: '强化天空色彩与云层层次', category: 'landscape', icon: '☁️',
    changes: [
      { key: 'saturation', label: '饱和度', value: 20, before: 0 },
      { key: 'highlights', label: '高光', value: -15, before: 0 },
      { key: 'contrast', label: '对比度', value: 10, before: 0 },
    ],
  },
  {
    id: 'clarity_enhance', name: '清晰度增强', description: '强化画面细节与边缘锐度', category: 'street', icon: '🔍',
    changes: [
      { key: 'clarity', label: '清晰度', value: 25, before: 0 },
      { key: 'sharpness', label: '锐度', value: 20, before: 0 },
    ],
  },
  {
    id: 'night_enhance', name: '夜景优化', description: '针对暗光环境的多项优化', category: 'night', icon: '🌃',
    changes: [
      { key: 'contrast', label: '对比度', value: 15, before: 0 },
      { key: 'shadows', label: '阴影', value: 20, before: 0 },
      { key: 'noiseReduction', label: '降噪', value: 35, before: 0 },
      { key: 'saturation', label: '饱和度', value: 8, before: 0 },
    ],
  },
  // 2026 流行风格
  {
    id: 'oxygen_fresh', name: '氧气感', description: '2026 春夏最火·清新透亮', category: 'fresh', icon: '💧', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: -8, before: 0 },
      { key: 'contrast', label: '对比度', value: -3, before: 0 },
      { key: 'brightness', label: '亮度', value: 15, before: 0 },
      { key: 'highlights', label: '高光', value: -15, before: 0 },
      { key: 'shadows', label: '阴影', value: 10, before: 0 },
      { key: 'clarity', label: '清晰度', value: -5, before: 0 },
    ],
  },
  {
    id: 'morandi', name: '莫兰迪', description: '低饱和高级感', category: 'film', icon: '🎨', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: -20, before: 0 },
      { key: 'contrast', label: '对比度', value: -5, before: 0 },
      { key: 'brightness', label: '亮度', value: 5, before: 0 },
      { key: 'warmth', label: '色温', value: 3, before: 0 },
    ],
  },
  {
    id: 'osmanthus', name: '桂花黄', description: '2026 秋季流行·温柔暖调', category: 'fresh', icon: '🍂', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: 5, before: 0 },
      { key: 'warmth', label: '色温', value: 25, before: 0 },
      { key: 'contrast', label: '对比度', value: -5, before: 0 },
      { key: 'brightness', label: '亮度', value: 5, before: 0 },
    ],
  },
  {
    id: 'kodak_gold', name: '柯达金', description: '致敬 Kodak Gold 200 胶片', category: 'film', icon: '📷', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: 10, before: 0 },
      { key: 'contrast', label: '对比度', value: 5, before: 0 },
      { key: 'warmth', label: '色温', value: 15, before: 0 },
      { key: 'highlights', label: '高光', value: -10, before: 0 },
      { key: 'clarity', label: '清晰度', value: -5, before: 0 },
    ],
  },
  {
    id: 'cyber_neon', name: '赛博霓虹', description: 'Y2K 复古·赛博朋克', category: 'night', icon: '🌈', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: 25, before: 0 },
      { key: 'contrast', label: '对比度', value: 25, before: 0 },
      { key: 'warmth', label: '色温', value: -20, before: 0 },
      { key: 'highlights', label: '高光', value: -25, before: 0 },
      { key: 'clarity', label: '清晰度', value: 20, before: 0 },
    ],
  },
  {
    id: 'bw_classic', name: '经典黑白', description: '致敬纪实摄影大师', category: 'bw', icon: '⚫', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: -100, before: 0 },
      { key: 'contrast', label: '对比度', value: 20, before: 0 },
      { key: 'sharpness', label: '锐度', value: 20, before: 0 },
      { key: 'clarity', label: '清晰度', value: 18, before: 0 },
    ],
  },
  {
    id: 'japanese_fresh', name: '日系清新', description: '低对比·高亮度·空气感', category: 'fresh', icon: '🌸', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: -5, before: 0 },
      { key: 'contrast', label: '对比度', value: -10, before: 0 },
      { key: 'brightness', label: '亮度', value: 12, before: 0 },
      { key: 'highlights', label: '高光', value: -12, before: 0 },
      { key: 'shadows', label: '阴影', value: 15, before: 0 },
    ],
  },
  {
    id: 'old_money', name: '老钱风', description: '低饱和奢华质感', category: 'fresh', icon: '💎', isTrending: true,
    changes: [
      { key: 'saturation', label: '饱和度', value: -15, before: 0 },
      { key: 'contrast', label: '对比度', value: 5, before: 0 },
      { key: 'warmth', label: '色温', value: 5, before: 0 },
      { key: 'clarity', label: '清晰度', value: 10, before: 0 },
    ],
  },
];

const AIFineTunePage: React.FC = () => {
  const { aiParams, setAiParam, goBack } = useAppStore();
  const [userImage, setUserImage] = useState<UserImage | null>(null);
  const [analysis, setAnalysis] = useState<ImageAnalysis | null>(null);
  const [recommendation, setRecommendation] = useState<RecommendedParams | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [activePreset, setActivePreset] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [progress, setProgress] = useState(0);
  const [activeCategory, setActiveCategory] = useState<'all' | 'portrait' | 'landscape' | 'night' | 'food' | 'street' | 'film' | 'fresh' | 'bw' | 'trending'>('all');

  useEffect(() => {
    const unsubscribe = userImageStore.subscribe(image => {
      setUserImage(image);
      if (image) {
        analyzeImage(image);
      } else {
        setAnalysis(null);
        setRecommendation(null);
      }
    });
    return unsubscribe;
  }, []);

  const analyzeImage = async (image: UserImage) => {
    setIsProcessing(true);
    setProgress(0);
    
    const interval = setInterval(() => {
      setProgress(prev => Math.min(prev + 12, 90));
    }, 100);
    
    try {
      const result = await imageAnalysisService.analyze(image);
      setAnalysis(result);
      const rec = imageAnalysisService.recommendHasselbladParams(result);
      setRecommendation(rec);
      
      clearInterval(interval);
      setProgress(100);
      setIsProcessing(false);
    } catch (e) {
      console.error(e);
      clearInterval(interval);
      setIsProcessing(false);
    }
  };

  const handleAutoTune = () => {
    if (!userImage) return;
    setIsProcessing(true);
    setProgress(0);
    
    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          return 100;
        }
        return prev + 10;
      });
    }, 100);
    
    setTimeout(() => {
      clearInterval(interval);
      setProgress(100);
      const preset = aiPresets[0];
      setActivePreset(preset.id);
      preset.changes.forEach(change => {
        setAiParam(change.key, change.value);
      });
      setIsProcessing(false);
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 2000);
    }, 1200);
  };

  const handleApplyPreset = (preset: AIFineTunePreset) => {
    setIsProcessing(true);
    setActivePreset(preset.id);
    
    setTimeout(() => {
      preset.changes.forEach(change => {
        setAiParam(change.key, change.value);
      });
      setIsProcessing(false);
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 2000);
    }, 500);
  };

  const handleApplyRecommendation = () => {
    if (!recommendation) return;
    setActivePreset('recommended');
    Object.entries(recommendation).forEach(([key, val]) => {
      if (key !== 'style' && key !== 'reason' && typeof val === 'number') {
        setAiParam(key, val);
      }
    });
    setShowSuccess(true);
    setTimeout(() => setShowSuccess(false), 2000);
  };

  const handleReset = () => {
    setActivePreset(null);
    setAiParam('saturation', 0);
    setAiParam('contrast', 0);
    setAiParam('brightness', 0);
    setAiParam('warmth', 0);
    setAiParam('sharpness', 0);
    setAiParam('clarity', 0);
    setAiParam('highlights', 0);
    setAiParam('shadows', 0);
    setAiParam('noiseReduction', 0);
    setAiParam('skinSmooth', 0);
  };

  const mainParams = [
    { key: 'saturation', label: '饱和度', min: -100, max: 100, icon: '🎨' },
    { key: 'contrast', label: '对比度', min: -100, max: 100, icon: '⚖️' },
    { key: 'brightness', label: '亮度', min: -100, max: 100, icon: '☀️' },
    { key: 'warmth', label: '色温', min: -100, max: 100, icon: '🌡️' },
    { key: 'sharpness', label: '锐度', min: 0, max: 100, icon: '🔪' },
  ];

  const advancedParams = [
    { key: 'highlights', label: '高光', min: -100, max: 100, icon: '✨' },
    { key: 'shadows', label: '阴影', min: -100, max: 100, icon: '🌑' },
    { key: 'clarity', label: '清晰度', min: 0, max: 100, icon: '🔍' },
    { key: 'noiseReduction', label: '降噪', min: 0, max: 100, icon: '🔇' },
    { key: 'skinSmooth', label: '美肤', min: 0, max: 100, icon: '👤' },
  ];

  const categories = [
    { key: 'all', label: '全部', count: aiPresets.length },
    { key: 'trending', label: '2026流行', icon: '🔥' },
    { key: 'portrait', label: '人像' },
    { key: 'landscape', label: '风景' },
    { key: 'night', label: '夜景' },
    { key: 'food', label: '美食' },
    { key: 'street', label: '街拍' },
    { key: 'film', label: '胶片' },
    { key: 'fresh', label: '清新' },
    { key: 'bw', label: '黑白' },
  ];

  const filteredPresets = activeCategory === 'all' 
    ? aiPresets 
    : activeCategory === 'trending'
    ? aiPresets.filter(p => p.isTrending)
    : aiPresets.filter(p => p.category === activeCategory);

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">AI 微调</h1>
        <div className="ml-auto flex items-center gap-1 text-[10px] text-white/50">
          <Brain size={12} />
          <span>2026流行风格</span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto pb-4 scrollbar-hide">
        {/* 上传区域 */}
        <div className="px-4 pt-4">
          <ImageUploader
            onImageLoaded={(img) => setUserImage(img)}
            buttonText="上传照片"
            hint="上传后 AI 将推荐哈苏参数+2026流行风格"
            sampleImages={[
              { url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=200&h=200&fit=crop', label: '风景', tag: '山' },
              { url: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=200&h=200&fit=crop', label: '人像', tag: '人物' },
              { url: 'https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=200&h=200&fit=crop', label: '胶片', tag: '复古' },
              { url: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200&h=200&fit=crop', label: '夜景', tag: '城市' },
            ]}
          />
        </div>

        {/* 处理中 */}
        {isProcessing && (
          <div className="px-4 mt-4">
            <div className="p-4 rounded-2xl bg-gradient-to-br from-purple-600/15 to-blue-500/5 border border-purple-500/30">
              <div className="flex items-center gap-3 mb-3">
                <RefreshCw size={20} className="text-purple-400 animate-spin" />
                <div>
                  <p className="text-white text-sm font-medium">AI 微调中...</p>
                  <p className="text-white/50 text-xs">分析图片特征并应用最优参数</p>
                </div>
              </div>
              <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-purple-600 to-blue-500 transition-all duration-150"
                  style={{ width: `${progress}%` }}
                />
              </div>
              <p className="text-white/40 text-xs text-center mt-2">{progress}%</p>
            </div>
          </div>
        )}

        {/* AI 推荐结果 */}
        {userImage && analysis && recommendation && !isProcessing && (
          <div className="px-4 mt-4">
            <div className="p-4 rounded-2xl bg-gradient-to-br from-purple-600/15 to-blue-500/5 border border-purple-500/30">
              <div className="flex items-center gap-2 mb-2">
                <Wand2 size={16} className="text-purple-400" />
                <h3 className="text-white text-sm font-bold">AI 为您推荐</h3>
              </div>
              <p className="text-white/70 text-sm leading-relaxed mb-3">{recommendation.reason}</p>
              <div className="flex flex-wrap gap-1.5 mb-3">
                <span className="px-2 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">场景: {analysis.detectedScene}</span>
                <span className="px-2 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">亮度: {Math.round(analysis.brightness)}</span>
                <span className="px-2 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">饱和: {Math.round(analysis.saturation*100)}%</span>
                <span className="px-2 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">对比: {Math.round(analysis.contrast*100)}%</span>
              </div>
              <button
                onClick={handleApplyRecommendation}
                className="w-full py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-blue-500 text-white text-sm font-medium flex items-center justify-center gap-2"
              >
                <Zap size={14} />
                <span>应用 AI 推荐参数</span>
              </button>
            </div>
          </div>
        )}

        {/* 一键 AI 微调按钮 */}
        {userImage && (
          <div className="px-4 mt-4">
            <button
              onClick={handleAutoTune}
              disabled={isProcessing}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-purple-600 to-blue-500 flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98 disabled:opacity-50"
            >
              <Wand2 size={18} />
              <span>一键 AI 微调</span>
            </button>
          </div>
        )}

        {/* 分类筛选 */}
        <div className="px-4 mt-4">
          <div className="flex gap-1.5 overflow-x-auto scrollbar-hide pb-1">
            {categories.map(cat => (
              <button
                key={cat.key}
                onClick={() => setActiveCategory(cat.key as any)}
                className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                  activeCategory === cat.key ? 'bg-purple-500 text-white' : 'bg-white/5 text-white/60'
                }`}
              >
                {cat.icon && <span className="mr-1">{cat.icon}</span>}
                {cat.label}
                {cat.count !== undefined && (
                  <span className="ml-1 opacity-60">{cat.count}</span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* 预设列表 */}
        <div className="px-4 mt-3">
          <div className="flex items-center gap-2 mb-3">
            <Sparkles size={14} className="text-purple-400" />
            <h3 className="text-white text-sm font-bold">AI 预设库</h3>
            <span className="text-white/40 text-xs ml-auto">{filteredPresets.length} 套</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {filteredPresets.map(preset => (
              <button
                key={preset.id}
                onClick={() => handleApplyPreset(preset)}
                disabled={isProcessing}
                className={`relative p-3 rounded-xl text-left transition-all ${
                  activePreset === preset.id
                    ? 'bg-gradient-to-br from-purple-600/30 to-blue-500/20 border border-purple-500/50'
                    : 'bg-white/5 hover:bg-white/10 border border-transparent'
                }`}
              >
                {preset.isTrending && (
                  <div className="absolute top-1.5 right-1.5 px-1.5 py-0.5 rounded bg-red-500/80 text-white text-[8px] font-bold">
                    🔥 2026
                  </div>
                )}
                <div className="text-2xl mb-1">{preset.icon}</div>
                <p className="text-white text-sm font-medium">{preset.name}</p>
                <p className="text-white/40 text-[10px] mt-0.5 line-clamp-1">{preset.description}</p>
              </button>
            ))}
          </div>
        </div>

        {/* 基础参数 */}
        <div className="px-4 mt-6">
          <div className="flex items-center gap-2 mb-3">
            <Target size={14} className="text-purple-400" />
            <h3 className="text-white text-sm font-bold">基础参数</h3>
          </div>
          <div className="space-y-3">
            {mainParams.map((param) => (
              <div key={param.key} className="bg-white/5 rounded-xl p-3">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">{param.icon}</span>
                    <span className="text-white text-sm font-medium">{param.label}</span>
                  </div>
                  <span className="text-purple-400 text-sm font-bold">
                    {aiParams[param.key as keyof typeof aiParams] > 0 ? '+' : ''}{aiParams[param.key as keyof typeof aiParams]}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  value={aiParams[param.key as keyof typeof aiParams]}
                  onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                  className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-purple-500"
                />
              </div>
            ))}
          </div>
        </div>

        {/* 高级参数 */}
        <div className="px-4 mt-6">
          <div className="flex items-center gap-2 mb-3">
            <Layers size={14} className="text-purple-400" />
            <h3 className="text-white text-sm font-bold">高级参数</h3>
          </div>
          <div className="space-y-3">
            {advancedParams.map((param) => (
              <div key={param.key} className="bg-white/5 rounded-xl p-3">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">{param.icon}</span>
                    <span className="text-white text-sm font-medium">{param.label}</span>
                  </div>
                  <span className="text-purple-400 text-sm font-bold">
                    {aiParams[param.key as keyof typeof aiParams] > 0 ? '+' : ''}{aiParams[param.key as keyof typeof aiParams]}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  value={aiParams[param.key as keyof typeof aiParams]}
                  onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                  className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-purple-500"
                />
              </div>
            ))}
          </div>
        </div>

        {/* 重置 */}
        <div className="px-4 mt-6">
          <button
            onClick={handleReset}
            className="w-full py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium transition-all hover:bg-white/5"
          >
            重置所有参数
          </button>
        </div>

        {/* 成功提示 */}
        {showSuccess && (
          <div className="fixed top-20 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full bg-green-500 text-white text-sm font-medium z-50 flex items-center gap-2 shadow-lg">
            <Check size={16} />
            <span>微调参数已应用</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default AIFineTunePage;
