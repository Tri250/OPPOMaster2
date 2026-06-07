import React, { useState, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Sparkles, RefreshCw, Check, Wand2, Brain, Zap, Target, TrendingUp, BarChart3 } from 'lucide-react';

interface AIOptimizeMode {
  id: string;
  name: string;
  description: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
  color: string;
  params: {
    saturation: number;
    contrast: number;
    brightness: number;
    warmth: number;
    sharpness: number;
    highlights: number;
    shadows: number;
    clarity: number;
    noiseReduction: number;
    skinSmooth: number;
  };
}

const aiModes: AIOptimizeMode[] = [
  {
    id: 'auto',
    name: 'AI 智能优化',
    description: 'AI 分析图片自动优化',
    icon: Brain,
    color: '#9C27B0',
    params: { saturation: 12, contrast: 10, brightness: 3, warmth: 5, sharpness: 15, highlights: -8, shadows: 5, clarity: 12, noiseReduction: 5, skinSmooth: 5 }
  },
  {
    id: 'hdr',
    name: 'HDR 增强',
    description: '提升动态范围',
    icon: TrendingUp,
    color: '#FF9800',
    params: { saturation: 10, contrast: 20, brightness: 0, warmth: 0, sharpness: 12, highlights: -30, shadows: 25, clarity: 15, noiseReduction: 0, skinSmooth: 0 }
  },
  {
    id: 'night',
    name: '夜景优化',
    description: '暗部增强降噪',
    icon: Zap,
    color: '#3F51B5',
    params: { saturation: 5, contrast: 15, brightness: -3, warmth: -8, sharpness: 20, highlights: -15, shadows: 20, clarity: 18, noiseReduction: 35, skinSmooth: 0 }
  },
  {
    id: 'portrait',
    name: '人像优化',
    description: '美肤肤色优化',
    icon: Target,
    color: '#E91E63',
    params: { saturation: 8, contrast: -3, brightness: 5, warmth: 10, sharpness: 8, highlights: -8, shadows: 8, clarity: 10, noiseReduction: 10, skinSmooth: 30 }
  },
  {
    id: 'landscape',
    name: '风景优化',
    description: '鲜明饱和清晰',
    icon: BarChart3,
    color: '#4CAF50',
    params: { saturation: 18, contrast: 12, brightness: 3, warmth: 0, sharpness: 18, highlights: -8, shadows: 5, clarity: 20, noiseReduction: 0, skinSmooth: 0 }
  },
  {
    id: 'food',
    name: '美食优化',
    description: '暖色食欲感',
    icon: Wand2,
    color: '#FF5722',
    params: { saturation: 15, contrast: 10, brightness: 5, warmth: 20, sharpness: 12, highlights: -5, shadows: 5, clarity: 12, noiseReduction: 0, skinSmooth: 0 }
  },
];

const AIFineTunePage: React.FC = () => {
  const { aiParams, setAiParam, goBack } = useAppStore();
  const [isProcessing, setIsProcessing] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [activeMode, setActiveMode] = useState<string | null>(null);
  const [analysisProgress, setAnalysisProgress] = useState(0);
  const [analysisStep, setAnalysisStep] = useState('');

  // 模拟AI分析过程
  const simulateAIAnalysis = (mode: AIOptimizeMode) => {
    setIsProcessing(true);
    setActiveMode(mode.id);
    
    const steps = [
      { text: '分析图片特征...', progress: 20 },
      { text: '检测主体内容...', progress: 40 },
      { text: 'AI 模型推理...', progress: 60 },
      { text: '生成优化方案...', progress: 80 },
      { text: '应用最佳参数...', progress: 100 },
    ];
    
    let stepIndex = 0;
    const interval = setInterval(() => {
      if (stepIndex < steps.length) {
        setAnalysisStep(steps[stepIndex].text);
        setAnalysisProgress(steps[stepIndex].progress);
        stepIndex++;
      } else {
        clearInterval(interval);
        applyMode(mode);
      }
    }, 400);
  };

  const applyMode = (mode: AIOptimizeMode) => {
    // 应用真实参数
    setAiParam('saturation', mode.params.saturation);
    setAiParam('contrast', mode.params.contrast);
    setAiParam('brightness', mode.params.brightness);
    setAiParam('warmth', mode.params.warmth);
    setAiParam('sharpness', mode.params.sharpness);
    setAiParam('highlights', mode.params.highlights);
    setAiParam('shadows', mode.params.shadows);
    setAiParam('clarity', mode.params.clarity);
    
    setIsProcessing(false);
    setShowSuccess(true);
    setTimeout(() => {
      setShowSuccess(false);
      setActiveMode(null);
    }, 2000);
  };

  const handleReset = () => {
    setAiParam('saturation', 0);
    setAiParam('contrast', 0);
    setAiParam('brightness', 0);
    setAiParam('warmth', 0);
    setAiParam('sharpness', 0);
    setAiParam('highlights', 0);
    setAiParam('shadows', 0);
    setAiParam('clarity', 0);
  };

  const params = [
    { key: 'saturation', label: '饱和度', min: -100, max: 100, unit: '' },
    { key: 'contrast', label: '对比度', min: -100, max: 100, unit: '' },
    { key: 'brightness', label: '亮度', min: -100, max: 100, unit: '' },
    { key: 'warmth', label: '色温', min: -100, max: 100, unit: '' },
    { key: 'sharpness', label: '锐度', min: 0, max: 100, unit: '' },
    { key: 'highlights', label: '高光', min: -100, max: 100, unit: '' },
    { key: 'shadows', label: '阴影', min: -100, max: 100, unit: '' },
    { key: 'clarity', label: '清晰度', min: 0, max: 100, unit: '' },
  ];

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">AI 微调 & 智能优化</h1>
        <div className="ml-auto flex items-center gap-1 text-[10px] text-white/50">
          <Brain size={12} />
          <span>v2.0</span>
        </div>
      </div>

      {/* Preview Area */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-purple-900/50 to-blue-900/50">
          <img 
            src="https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600&h=400&fit=crop"
            alt="Preview"
            className="w-full h-full object-cover transition-all duration-500"
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
            <div className="absolute inset-0 bg-black/80 flex items-center justify-center">
              <div className="flex flex-col items-center gap-4 w-64">
                <div className="relative">
                  <div className="w-16 h-16 rounded-full border-4 border-white/10" />
                  <div 
                    className="absolute inset-0 rounded-full border-4 border-transparent border-t-[#9C27B0] animate-spin"
                    style={{ animationDuration: '1s' }}
                  />
                  <Brain size={24} className="absolute inset-0 m-auto text-[#9C27B0]" />
                </div>
                <div className="text-center w-full">
                  <p className="text-white text-sm font-medium mb-2">{analysisStep}</p>
                  <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-gradient-to-r from-[#9C27B0] to-[#2196F3] rounded-full transition-all duration-300"
                      style={{ width: `${analysisProgress}%` }}
                    />
                  </div>
                  <p className="text-white/50 text-xs mt-1">{analysisProgress}%</p>
                </div>
              </div>
            </div>
          )}

          {/* Success Overlay */}
          {showSuccess && (
            <div className="absolute inset-0 bg-black/70 flex items-center justify-center">
              <div className="flex flex-col items-center gap-3">
                <div className="w-16 h-16 rounded-full bg-green-500 flex items-center justify-center animate-bounce">
                  <Check size={32} className="text-white" />
                </div>
                <span className="text-white text-base font-medium">AI 优化完成</span>
                <span className="text-white/60 text-xs">参数已自动应用</span>
              </div>
            </div>
          )}

          {/* Params Display */}
          <div className="absolute bottom-3 left-3 right-3">
            <div className="flex flex-wrap gap-1.5">
              {params.slice(0, 4).map((param) => {
                const value = aiParams[param.key as keyof typeof aiParams];
                if (value === 0) return null;
                return (
                  <span 
                    key={param.key}
                    className="px-2 py-0.5 rounded-full bg-black/60 backdrop-blur-sm text-white text-[10px]"
                  >
                    {param.label} {value > 0 ? '+' : ''}{value}
                  </span>
                );
              })}
            </div>
          </div>

          {/* AI Badge */}
          <div className="absolute top-3 left-3 px-3 py-1.5 rounded-full bg-black/50 backdrop-blur-sm flex items-center gap-2">
            <Sparkles size={14} className="text-[#9C27B0]" />
            <span className="text-white text-xs">AI 微调中</span>
          </div>
        </div>
      </div>

      {/* AI Optimize Modes */}
      <div className="px-4 pb-4">
        <div className="flex items-center gap-2 mb-3">
          <Wand2 size={16} className="text-[#9C27B0]" />
          <h2 className="text-white text-sm font-bold">AI 智能优化模式</h2>
        </div>
        <div className="grid grid-cols-3 gap-2">
          {aiModes.map((mode) => {
            const Icon = mode.icon;
            const isActive = activeMode === mode.id;
            return (
              <button
                key={mode.id}
                onClick={() => simulateAIAnalysis(mode)}
                disabled={isProcessing}
                className={`p-3 rounded-xl transition-all duration-300 ${
                  isActive 
                    ? 'bg-gradient-to-br from-[#9C27B0]/30 to-[#2196F3]/20 border border-[#9C27B0]/50' 
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div className="flex flex-col items-center gap-1.5">
                  <div 
                    className="w-9 h-9 rounded-lg flex items-center justify-center"
                    style={{ backgroundColor: `${mode.color}20` }}
                  >
                    <Icon size={18} className="" />
                    <Icon size={18} style={{ color: mode.color }} />
                  </div>
                  <span className="text-white text-[11px] font-medium">{mode.name}</span>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Param Sliders */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-white text-sm font-bold">参数精细调节</h2>
          <button
            onClick={handleReset}
            className="text-[#9C27B0] text-xs font-medium"
          >
            重置
          </button>
        </div>
        
        <div className="space-y-3">
          {params.map((param) => {
            const value = aiParams[param.key as keyof typeof aiParams];
            return (
              <div key={param.key} className="bg-white/5 rounded-xl p-3">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-white text-sm font-medium">{param.label}</span>
                  <span className={`text-sm font-bold ${
                    value > 0 ? 'text-[#9C27B0]' : value < 0 ? 'text-[#2196F3]' : 'text-white/50'
                  }`}>
                    {value > 0 ? '+' : ''}{value}{param.unit}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  value={value}
                  onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                  className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0]"
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default AIFineTunePage;
