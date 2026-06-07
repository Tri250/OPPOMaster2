import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Sparkles, RefreshCw, Check, Wand2, Brain, Layers, Target, Zap } from 'lucide-react';

interface AIOptimizePreset {
  id: string;
  name: string;
  description: string;
  icon: string;
  changes: Array<{ key: string; label: string; value: number; before: number }>;
}

const aiPresets: AIOptimizePreset[] = [
  {
    id: 'auto_optimize',
    name: '智能优化',
    description: 'AI 自动分析并优化全局参数',
    icon: '✨',
    changes: [
      { key: 'saturation', label: '饱和度', value: 10, before: 0 },
      { key: 'contrast', label: '对比度', value: 8, before: 0 },
      { key: 'brightness', label: '亮度', value: 5, before: 0 },
      { key: 'sharpness', label: '锐度', value: 15, before: 0 },
      { key: 'clarity', label: '清晰度', value: 10, before: 0 },
    ],
  },
  {
    id: 'hdr_enhance',
    name: 'HDR 增强',
    description: '提升动态范围，保留高光与暗部细节',
    icon: '🌅',
    changes: [
      { key: 'contrast', label: '对比度', value: 20, before: 0 },
      { key: 'highlights', label: '高光', value: -30, before: 0 },
      { key: 'shadows', label: '阴影', value: 25, before: 0 },
      { key: 'clarity', label: '清晰度', value: 15, before: 0 },
    ],
  },
  {
    id: 'noise_reduce',
    name: '降噪处理',
    description: '降低暗光环境下的画面噪点',
    icon: '🔇',
    changes: [
      { key: 'noiseReduction', label: '降噪', value: 40, before: 0 },
      { key: 'sharpness', label: '锐度', value: -5, before: 0 },
    ],
  },
  {
    id: 'skin_smooth',
    name: '肤色优化',
    description: '智能美肤，保留真实肤质细节',
    icon: '👤',
    changes: [
      { key: 'skinSmooth', label: '美肤', value: 25, before: 0 },
      { key: 'warmth', label: '色温', value: 5, before: 0 },
      { key: 'saturation', label: '饱和度', value: -5, before: 0 },
    ],
  },
  {
    id: 'sky_enhance',
    name: '天空增强',
    description: '强化天空色彩与云层层次',
    icon: '☁️',
    changes: [
      { key: 'saturation', label: '饱和度', value: 20, before: 0 },
      { key: 'highlights', label: '高光', value: -15, before: 0 },
      { key: 'contrast', label: '对比度', value: 10, before: 0 },
    ],
  },
  {
    id: 'clarity_enhance',
    name: '清晰度增强',
    description: '强化画面细节与边缘锐度',
    icon: '🔍',
    changes: [
      { key: 'clarity', label: '清晰度', value: 25, before: 0 },
      { key: 'sharpness', label: '锐度', value: 20, before: 0 },
    ],
  },
  {
    id: 'night_enhance',
    name: '夜景优化',
    description: '针对暗光环境的多项优化',
    icon: '🌃',
    changes: [
      { key: 'contrast', label: '对比度', value: 15, before: 0 },
      { key: 'shadows', label: '阴影', value: 20, before: 0 },
      { key: 'noiseReduction', label: '降噪', value: 35, before: 0 },
      { key: 'saturation', label: '饱和度', value: 8, before: 0 },
    ],
  },
];

const AIFineTunePage: React.FC = () => {
  const { aiParams, setAiParam, goBack } = useAppStore();
  const [isProcessing, setIsProcessing] = useState(false);
  const [activePreset, setActivePreset] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [progress, setProgress] = useState(0);

  const handleAutoTune = () => {
    setIsProcessing(true);
    setProgress(0);
    
    // 模拟AI分析进度
    const interval = setInterval(() => {
      setProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          return 100;
        }
        return prev + 10;
      });
    }, 150);
    
    setTimeout(() => {
      clearInterval(interval);
      setProgress(100);
      // 智能优化预设
      const preset = aiPresets[0];
      setActivePreset(preset.id);
      preset.changes.forEach(change => {
        setAiParam(change.key, change.value);
      });
      setIsProcessing(false);
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 2000);
    }, 1500);
  };

  const handleApplyPreset = (preset: AIOptimizePreset) => {
    setIsProcessing(true);
    setActivePreset(preset.id);
    
    setTimeout(() => {
      preset.changes.forEach(change => {
        setAiParam(change.key, change.value);
      });
      setIsProcessing(false);
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 2000);
    }, 800);
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
    { key: 'saturation', label: '饱和度', min: -100, max: 100, unit: '', icon: '🎨' },
    { key: 'contrast', label: '对比度', min: -100, max: 100, unit: '', icon: '⚖️' },
    { key: 'brightness', label: '亮度', min: -100, max: 100, unit: '', icon: '☀️' },
    { key: 'warmth', label: '色温', min: -100, max: 100, unit: '', icon: '🌡️' },
    { key: 'sharpness', label: '锐度', min: 0, max: 100, unit: '', icon: '🔪' },
  ];

  const advancedParams = [
    { key: 'highlights', label: '高光', min: -100, max: 100, unit: '', icon: '✨' },
    { key: 'shadows', label: '阴影', min: -100, max: 100, unit: '', icon: '🌑' },
    { key: 'clarity', label: '清晰度', min: 0, max: 100, unit: '', icon: '🔍' },
    { key: 'noiseReduction', label: '降噪', min: 0, max: 100, unit: '', icon: '🔇' },
    { key: 'skinSmooth', label: '美肤', min: 0, max: 100, unit: '', icon: '👤' },
  ];

  const formatValue = (val: number) => {
    return val > 0 ? `+${val}` : `${val}`;
  };

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
          <span>智能分析</span>
        </div>
      </div>

      {/* Preview Area */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-purple-900/50 to-blue-900/50">
          <img 
            src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600&h=400&fit=crop"
            alt="Preview"
            className="w-full h-full object-cover"
            style={{
              filter: `
                saturate(${100 + aiParams.saturation}%) 
                contrast(${100 + aiParams.contrast}%) 
                brightness(${100 + aiParams.brightness}%)
              `,
            }}
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
          
          {/* Processing Overlay */}
          {isProcessing && (
            <div className="absolute inset-0 bg-black/70 flex flex-col items-center justify-center">
              <div className="flex flex-col items-center gap-3">
                <RefreshCw size={32} className="text-[#9C27B0] animate-spin" />
                <span className="text-white text-sm">AI 分析中...</span>
                <div className="w-48 h-1.5 bg-white/20 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-gradient-to-r from-[#9C27B0] to-blue-500 transition-all duration-150"
                    style={{ width: `${progress}%` }}
                  />
                </div>
                <span className="text-white/60 text-xs">{progress}%</span>
              </div>
            </div>
          )}

          {/* Success Overlay */}
          {showSuccess && (
            <div className="absolute inset-0 bg-black/70 flex items-center justify-center">
              <div className="flex flex-col items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-green-500 flex items-center justify-center">
                  <Check size={24} className="text-white" />
                </div>
                <span className="text-white text-sm">优化完成</span>
              </div>
            </div>
          )}

          {/* Active Preset Badge */}
          {activePreset && !isProcessing && (
            <div className="absolute top-3 left-3 px-3 py-1.5 rounded-full bg-[#9C27B0]/80 backdrop-blur-sm flex items-center gap-2">
              <Zap size={14} className="text-white" />
              <span className="text-white text-xs">
                {aiPresets.find(p => p.id === activePreset)?.name}
              </span>
            </div>
          )}

          {/* Params Display */}
          <div className="absolute bottom-3 left-3 right-3">
            <div className="flex flex-wrap gap-1.5">
              {Object.entries(aiParams)
                .filter(([_, val]) => val !== 0)
                .slice(0, 5)
                .map(([key, val]) => (
                  <span 
                    key={key}
                    className="px-2 py-0.5 rounded-full bg-black/50 backdrop-blur-sm text-white text-[10px]"
                  >
                    {key}: {formatValue(val as number)}
                  </span>
                ))}
            </div>
          </div>
        </div>
      </div>

      {/* Auto Tune Button */}
      <div className="px-4 pb-4">
        <button
          onClick={handleAutoTune}
          disabled={isProcessing}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-purple-600 to-blue-600 flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98 disabled:opacity-50"
        >
          <Wand2 size={18} />
          <span>一键 AI 微调</span>
        </button>
      </div>

      {/* AI Presets Grid */}
      <div className="px-4 pb-4">
        <div className="flex items-center gap-2 mb-3">
          <Sparkles size={14} className="text-[#9C27B0]" />
          <h3 className="text-white text-sm font-bold">AI 智能预设</h3>
        </div>
        <div className="grid grid-cols-3 gap-2">
          {aiPresets.map(preset => (
            <button
              key={preset.id}
              onClick={() => handleApplyPreset(preset)}
              disabled={isProcessing}
              className={`p-3 rounded-xl text-left transition-all ${
                activePreset === preset.id
                  ? 'bg-gradient-to-br from-[#9C27B0]/30 to-blue-500/20 border border-[#9C27B0]/50'
                  : 'bg-white/5 hover:bg-white/10'
              }`}
            >
              <div className="text-2xl mb-1">{preset.icon}</div>
              <p className="text-white text-xs font-medium">{preset.name}</p>
            </button>
          ))}
        </div>
      </div>

      {/* Main Params */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        <div className="flex items-center gap-2 mb-3">
          <Target size={14} className="text-[#9C27B0]" />
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
                <span className="text-[#9C27B0] text-sm font-bold">
                  {formatValue(aiParams[param.key as keyof typeof aiParams])}
                </span>
              </div>
              <input
                type="range"
                min={param.min}
                max={param.max}
                value={aiParams[param.key as keyof typeof aiParams]}
                onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0]"
              />
            </div>
          ))}
        </div>

        {/* Advanced Params */}
        <div className="flex items-center gap-2 mb-3 mt-6">
          <Layers size={14} className="text-[#9C27B0]" />
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
                <span className="text-[#9C27B0] text-sm font-bold">
                  {formatValue(aiParams[param.key as keyof typeof aiParams])}
                </span>
              </div>
              <input
                type="range"
                min={param.min}
                max={param.max}
                value={aiParams[param.key as keyof typeof aiParams]}
                onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0]"
              />
            </div>
          ))}
        </div>

        {/* Reset Button */}
        <button
          onClick={handleReset}
          className="w-full mt-6 py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium transition-all hover:bg-white/5"
        >
          重置所有参数
        </button>
      </div>
    </div>
  );
};

export default AIFineTunePage;
