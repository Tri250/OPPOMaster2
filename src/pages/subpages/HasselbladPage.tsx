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
    <div className="h-full w-full bg-gray-50 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center gap-3 shadow-sm">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-gray-700" />
        </button>
        <div className="flex items-center gap-2">
          <Aperture size={20} className="text-orange-500" />
          <h1 className="text-lg font-semibold text-gray-900">哈苏色彩科学</h1>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Hero Section */}
        <div className="bg-gradient-to-br from-orange-500 to-orange-600 rounded-2xl p-5 text-white">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-white/20 rounded-xl">
              <Camera size={32} className="text-white" />
            </div>
            <div className="flex-1">
              <h2 className="text-xl font-bold mb-1">HNCS 3.0</h2>
              <p className="text-orange-100 text-sm">哈苏自然色彩解决方案</p>
              <p className="text-orange-100/80 text-xs mt-2">还原真实色彩，呈现自然之美</p>
            </div>
          </div>
        </div>

        {/* Color Modes */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">色彩模式</h3>
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
                      ? 'border-orange-500 bg-orange-50'
                      : 'border-gray-100 hover:border-gray-200 hover:bg-gray-50'
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1">
                    <Icon size={18} style={{ color: mode.color }} />
                    <span className={`text-sm font-medium ${isSelected ? 'text-orange-700' : 'text-gray-700'}`}>
                      {mode.name}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500">{mode.desc}</p>
                </button>
              );
            })}
          </div>
        </div>

        {/* Fine Tuning */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">精细调节</h3>
          <div className="space-y-4">
            {COLOR_PARAMS.map(param => (
              <div key={param.id}>
                <div className="flex justify-between items-center mb-1">
                  <span className="text-sm text-gray-600">{param.name}</span>
                  <span className="text-sm font-medium text-gray-900">{params[param.id as keyof typeof params]}</span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  value={params[param.id as keyof typeof params]}
                  onChange={(e) => handleParamChange(param.id, parseInt(e.target.value))}
                  className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-orange-500"
                />
              </div>
            ))}
          </div>
        </div>

        {/* Features */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">核心特性</h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
              <div className="p-2 bg-orange-100 rounded-lg">
                <Zap size={18} className="text-orange-600" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-900">自然肤色还原</h4>
                <p className="text-xs text-gray-500 mt-0.5">智能识别肤色区域，自然美化不偏色</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
              <div className="p-2 bg-blue-100 rounded-lg">
                <Layers size={18} className="text-blue-600" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-900">色彩层次增强</h4>
                <p className="text-xs text-gray-500 mt-0.5">智能增强色彩过渡，层次更丰富</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Sparkles size={18} className="text-purple-600" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-900">16-bit 色彩深度</h4>
                <p className="text-xs text-gray-500 mt-0.5">超高色彩精度，细节分毫毕现</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="bg-white border-t border-gray-200 p-4 flex gap-3">
        <button
          onClick={handleReset}
          className="flex-1 py-3 px-4 bg-gray-100 text-gray-700 rounded-xl font-medium hover:bg-gray-200 transition-colors"
        >
          重置
        </button>
        <button
          onClick={handleApply}
          className="flex-1 py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-xl font-medium hover:from-orange-600 hover:to-orange-700 transition-all flex items-center justify-center gap-2"
        >
          {isApplied ? <Check size={20} /> : <Aperture size={20} />}
          {isApplied ? '已应用' : '应用色彩'}
        </button>
      </div>
    </div>
  );
};

export default HasselbladPage;
