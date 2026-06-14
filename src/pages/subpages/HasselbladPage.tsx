import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Sun, Palette,
  Eye, Sparkles, Moon, Leaf,
  Aperture, Camera, Zap, Layers, Check
} from 'lucide-react';

// 色彩参数类型
interface ColorParams {
  saturation: number;
  contrast: number;
  warmth: number;
  vibrance: number;
  clarity: number;
  skinTone?: number;
  grain?: number;
}

// 哈苏色彩模式
const COLOR_MODES: Array<{ id: string; name: string; icon: React.ElementType; color: string; desc: string; params: Partial<ColorParams> }> = [
  { id: 'natural', name: '哈苏自然色彩', icon: Eye, color: '#FF6B35', desc: 'HNCS 3.0 自然色彩解决方案', params: { saturation: 0, contrast: 5, warmth: 0, vibrance: 5, clarity: 0 } },
  { id: 'portrait', name: '人像肤色优化', icon: Sun, color: '#FF6B9D', desc: '自然美化肤色，保留细节', params: { saturation: 5, contrast: 8, warmth: 3, vibrance: 0, skinTone: 10, clarity: 0 } },
  { id: 'landscape', name: '风景色彩增强', icon: Leaf, color: '#4ECDC4', desc: '增强风景色彩层次', params: { saturation: 12, contrast: 10, warmth: 5, vibrance: 0, clarity: 10 } },
  { id: 'classic', name: '哈苏经典胶片', icon: Sparkles, color: '#9C27B0', desc: '复古胶片色彩质感', params: { saturation: 8, contrast: 15, warmth: 8, vibrance: 0, grain: 5, clarity: 0 } },
  { id: 'bw', name: '哈苏黑白', icon: Moon, color: '#808080', desc: '经典黑白摄影风格', params: { saturation: -100, contrast: 20, vibrance: 0, clarity: 15, warmth: 0 } },
  { id: 'vivid', name: '鲜艳色彩', icon: Palette, color: '#FF9800', desc: '鲜艳饱满的色彩表现', params: { saturation: 20, contrast: 10, vibrance: 15, warmth: 0, clarity: 0 } },
];

// 色彩参数调节
const COLOR_PARAMS = [
  { id: 'saturation', name: '饱和度', min: -100, max: 100, default: 0 },
  { id: 'contrast', name: '对比度', min: -100, max: 100, default: 0 },
  { id: 'warmth', name: '色温', min: -100, max: 100, default: 0 },
  { id: 'vibrance', name: '鲜艳度', min: -100, max: 100, default: 0 },
  { id: 'clarity', name: '清晰度', min: -100, max: 100, default: 0 },
];

const HasselbladPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();
  const [selectedMode, setSelectedMode] = useState('natural');
  const [params, setParams] = useState<ColorParams>({
    saturation: 0,
    contrast: 5,
    warmth: 0,
    vibrance: 5,
    clarity: 0,
  });
  const [isApplied, setIsApplied] = useState(false);

  const handleModeSelect = (modeId: string) => {
    const mode = COLOR_MODES.find(m => m.id === modeId);
    if (mode) {
      setSelectedMode(modeId);
      setParams({
        saturation: mode.params.saturation ?? 0,
        contrast: mode.params.contrast ?? 0,
        warmth: mode.params.warmth ?? 0,
        vibrance: mode.params.vibrance ?? 0,
        clarity: mode.params.clarity ?? 0,
        skinTone: mode.params.skinTone,
        grain: mode.params.grain,
      });
    }
  };

  const handleParamChange = (id: string, value: number) => {
    setParams(prev => ({ ...prev, [id]: value }));
  };

  const handleApply = () => {
    setIsApplied(true);
    setTimeout(() => setIsApplied(false), 2000);
  };

  const handleReset = () => {
    const mode = COLOR_MODES.find(m => m.id === selectedMode);
    if (mode) {
      setParams({
        saturation: mode.params.saturation ?? 0,
        contrast: mode.params.contrast ?? 0,
        warmth: mode.params.warmth ?? 0,
        vibrance: mode.params.vibrance ?? 0,
        clarity: mode.params.clarity ?? 0,
        skinTone: mode.params.skinTone,
        grain: mode.params.grain,
      });
    }
  };

  return (
    <div className="h-full w-full bg-[#0a0a0a] flex flex-col overflow-hidden">
      {/* Header */}
      <div className="bg-[#0a0a0a] border-b border-white/5 px-4 py-3 flex items-center gap-3">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-white/10 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <div className="flex items-center gap-2">
          <Aperture size={20} className="text-[#FF6B35]" />
          <h1 className="text-lg font-semibold text-white">哈苏色彩科学</h1>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Hero Section */}
        <div className="bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] rounded-2xl p-5 text-white">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-white/20 rounded-xl">
              <Camera size={32} className="text-white" />
            </div>
            <div className="flex-1">
              <h2 className="text-xl font-bold mb-1">HNCS 3.0</h2>
              <p className="text-white/80 text-sm">哈苏自然色彩解决方案</p>
              <p className="text-white/60 text-xs mt-2">还原真实色彩，呈现自然之美</p>
            </div>
          </div>
        </div>

        {/* Color Modes */}
        <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">色彩模式</h3>
          <div className="grid grid-cols-2 gap-2">
            {COLOR_MODES.map(mode => {
              const Icon = mode.icon;
              const isSelected = selectedMode === mode.id;
              return (
                <button
                  key={mode.id}
                  onClick={() => handleModeSelect(mode.id)}
                  className={`p-3 rounded-xl border-2 transition-all text-left ${
                    isSelected
                      ? 'border-[#FF6B35] bg-[#FF6B35]/10'
                      : 'border-white/5 hover:border-white/10 hover:bg-white/5'
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1">
                    <Icon size={18} style={{ color: mode.color }} />
                    <span className={`text-sm font-medium ${isSelected ? 'text-white' : 'text-white/70'}`}>
                      {mode.name}
                    </span>
                  </div>
                  <p className="text-xs text-white/50">{mode.desc}</p>
                </button>
              );
            })}
          </div>
        </div>

        {/* Fine Tuning */}
        <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">精细调节</h3>
          <div className="space-y-4">
            {COLOR_PARAMS.map(param => (
              <div key={param.id}>
                <div className="flex justify-between items-center mb-1">
                  <span className="text-sm text-white/60">{param.name}</span>
                  <span className="text-sm font-medium text-white">{params[param.id as keyof typeof params]}</span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  value={params[param.id as keyof typeof params]}
                  onChange={(e) => handleParamChange(param.id, parseInt(e.target.value))}
                  className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-[#FF6B35]"
                />
              </div>
            ))}
          </div>
        </div>

        {/* Features */}
        <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">核心特性</h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
              <div className="p-2 bg-[#FF6B35]/20 rounded-lg">
                <Zap size={18} className="text-[#FF6B35]" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-white">自然肤色还原</h4>
                <p className="text-xs text-white/50 mt-0.5">智能识别肤色区域，自然美化不偏色</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
              <div className="p-2 bg-[#2196F3]/20 rounded-lg">
                <Layers size={18} className="text-[#2196F3]" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-white">色彩层次增强</h4>
                <p className="text-xs text-white/50 mt-0.5">智能增强色彩过渡，层次更丰富</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
              <div className="p-2 bg-[#9C27B0]/20 rounded-lg">
                <Sparkles size={18} className="text-[#9C27B0]" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-white">16-bit 色彩深度</h4>
                <p className="text-xs text-white/50 mt-0.5">超高色彩精度，细节分毫毕现</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="bg-[#0a0a0a] border-t border-white/5 p-4 flex gap-3">
        <button
          onClick={handleReset}
          className="flex-1 py-3 px-4 bg-white/10 text-white/70 rounded-xl font-medium hover:bg-white/15 transition-colors"
        >
          重置
        </button>
        <button
          onClick={handleApply}
          className="flex-1 py-3 px-4 bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] text-white rounded-xl font-medium hover:opacity-90 transition-all flex items-center justify-center gap-2"
        >
          {isApplied ? <Check size={20} /> : <Aperture size={20} />}
          {isApplied ? '已应用' : '应用色彩'}
        </button>
      </div>
    </div>
  );
};

export default HasselbladPage;
