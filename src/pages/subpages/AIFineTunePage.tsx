import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, RefreshCw, Check, Wand2 } from 'lucide-react';

const AIFineTunePage: React.FC = () => {
  const { aiParams, setAiParam, goBack } = useAppStore();
  const [isProcessing, setIsProcessing] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  const handleAutoTune = () => {
    setIsProcessing(true);
    setTimeout(() => {
      setAiParam('saturation', 15);
      setAiParam('contrast', 8);
      setAiParam('brightness', 5);
      setAiParam('warmth', 12);
      setAiParam('sharpness', 20);
      setIsProcessing(false);
      setShowSuccess(true);
      setTimeout(() => setShowSuccess(false), 2000);
    }, 1500);
  };

  const handleReset = () => {
    setAiParam('saturation', 10);
    setAiParam('contrast', 5);
    setAiParam('brightness', 0);
    setAiParam('warmth', 8);
    setAiParam('sharpness', 15);
  };

  const params = [
    { key: 'saturation', label: '饱和度', min: -50, max: 50, unit: '' },
    { key: 'contrast', label: '对比度', min: -50, max: 50, unit: '' },
    { key: 'brightness', label: '亮度', min: -50, max: 50, unit: '' },
    { key: 'warmth', label: '色温', min: -50, max: 50, unit: '' },
    { key: 'sharpness', label: '锐度', min: 0, max: 100, unit: '' },
  ];

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button 
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">AI 微调</h1>
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
            <div className="absolute inset-0 bg-black/70 flex items-center justify-center">
              <div className="flex flex-col items-center gap-3">
                <RefreshCw size={32} className="text-[#9C27B0] animate-spin" />
                <span className="text-white text-sm">AI 分析中...</span>
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

          {/* Params Display */}
          <div className="absolute bottom-3 left-3 right-3">
            <div className="flex flex-wrap gap-2">
              {params.map((param) => (
                <span 
                  key={param.key}
                  className="px-2 py-1 rounded-full bg-black/50 backdrop-blur-sm text-white text-xs"
                >
                  {param.label}: {aiParams[param.key as keyof typeof aiParams] > 0 ? '+' : ''}
                  {aiParams[param.key as keyof typeof aiParams]}
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

      {/* Param Sliders */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="space-y-4">
          {params.map((param) => (
            <div key={param.key} className="bg-white/5 rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white text-sm font-medium">{param.label}</span>
                <span className="text-[#9C27B0] text-sm font-bold">
                  {aiParams[param.key as keyof typeof aiParams] > 0 ? '+' : ''}
                  {aiParams[param.key as keyof typeof aiParams]}
                  {param.unit}
                </span>
              </div>
              <input
                type="range"
                min={param.min}
                max={param.max}
                value={aiParams[param.key as keyof typeof aiParams]}
                onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0]"
              />
              <div className="flex justify-between mt-1">
                <span className="text-white/30 text-xs">{param.min}</span>
                <span className="text-white/30 text-xs">{param.max}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Reset Button */}
        <button
          onClick={handleReset}
          className="w-full mt-4 py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium transition-all hover:bg-white/5"
        >
          重置参数
        </button>
      </div>
    </div>
  );
};

export default AIFineTunePage;
