import React, { useState, useRef, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Cpu, Wand2, Check, RefreshCw, Zap, Sun, Droplets, Focus, Upload, Eye } from 'lucide-react';

const optimizeOptions = [
  { id: 'hdr', name: 'HDR增强', icon: Sun, color: '#FF9800', desc: '提升动态范围，保留更多细节' },
  { id: 'denoise', name: '智能降噪', icon: Droplets, color: '#2196F3', desc: 'AI识别并消除噪点' },
  { id: 'sharpen', name: '锐化增强', icon: Focus, color: '#9C27B0', desc: '提升画面清晰度和质感' },
  { id: 'enhance', name: '综合优化', icon: Zap, color: '#4CAF50', desc: '一键优化全部参数' },
];

const DEFAULT_IMAGE_SOURCE = 'https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=600&h=400&fit=crop';

// 将 aiParams 转换为可见的 CSS filter
const buildOptimizeFilter = (params: Record<string, number>): string => {
  const parts: string[] = [];
  const saturation = params.saturation ?? 0;
  const contrast = params.contrast ?? 0;
  const brightness = params.brightness ?? 0;
  const warmth = params.warmth ?? 0;
  const sharpness = params.sharpness ?? 0;

  parts.push(`saturate(${100 + saturation}%)`);
  parts.push(`contrast(${100 + contrast}%)`);
  if (brightness !== 0) parts.push(`brightness(${100 + brightness}%)`);
  if (warmth > 0) parts.push(`sepia(${warmth * 0.4}%)`);
  if (warmth < 0) parts.push(`hue-rotate(${warmth * 0.4}deg)`);
  // 锐度通过轻微对比度增强模拟
  if (sharpness > 0) parts.push(`contrast(${100 + sharpness * 0.3}%)`);

  return parts.join(' ');
};

const SmartOptimizePage: React.FC = () => {
  const { aiParams, setAiParam, goBack, tuneImageSource, setTuneImageSource } = useAppStore();
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [optimizedOptions, setOptimizedOptions] = useState<string[]>([]);
  const [selectedOptions, setSelectedOptions] = useState<string[]>(['enhance']);
  const [showCompare, setShowCompare] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const toggleOption = (id: string) => {
    setSelectedOptions(prev =>
      prev.includes(id)
        ? prev.filter(o => o !== id)
        : [...prev, id]
    );
  };

  const triggerUpload = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const handleImageUpload = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      alert('请选择图片文件');
      return;
    }
    const reader = new FileReader();
    reader.onload = (ev) => {
      const dataUrl = ev.target?.result as string;
      if (dataUrl) setTuneImageSource(dataUrl);
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  }, [setTuneImageSource]);

  const resetParams = useCallback(() => {
    setAiParam('contrast', 0);
    setAiParam('saturation', 0);
    setAiParam('brightness', 0);
    setAiParam('warmth', 0);
    setAiParam('sharpness', 0);
  }, [setAiParam]);

  const handleOptimize = () => {
    if (selectedOptions.length === 0) return;

    setIsOptimizing(true);
    setShowCompare(false);

    // 重置后应用，确保效果可见
    resetParams();

    // 模拟优化过程
    const processStep = (index: number) => {
      if (index < selectedOptions.length) {
        setTimeout(() => {
          setOptimizedOptions(prev => [...prev, selectedOptions[index]]);
          processStep(index + 1);
        }, 500);
      } else {
        // 完成优化
        setTimeout(() => {
          setIsOptimizing(false);

          // 应用优化参数（数值更明显，便于用户感知）
          if (selectedOptions.includes('enhance')) {
            setAiParam('contrast', 18);
            setAiParam('saturation', 12);
            setAiParam('sharpness', 28);
            setAiParam('brightness', 8);
          }
          if (selectedOptions.includes('hdr')) {
            setAiParam('contrast', 12);
            setAiParam('brightness', 15);
            setAiParam('saturation', 8);
          }
          if (selectedOptions.includes('sharpen')) {
            setAiParam('sharpness', 40);
            setAiParam('contrast', 8);
          }
          if (selectedOptions.includes('denoise')) {
            setAiParam('warmth', 5);
            setAiParam('sharpness', 0);
          }
        }, 500);
      }
    };

    setOptimizedOptions([]);
    processStep(0);
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* 隐藏的文件输入 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleImageUpload}
        className="hidden"
      />

      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">智能优化</h1>
        <div className="flex-1" />
        <button
          onClick={triggerUpload}
          className="p-2 rounded-full hover:bg-white/10"
          title="上传图片"
        >
          <Upload size={18} className="text-white/50" />
        </button>
      </div>

      {/* Preview */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-gray-900 to-gray-800">
          <img
            src={tuneImageSource || DEFAULT_IMAGE_SOURCE}
            alt="Preview"
            className="w-full h-full object-cover transition-all duration-500"
            style={{ filter: buildOptimizeFilter(aiParams) }}
          />

          {/* 上传提示 */}
          {!tuneImageSource && (
            <button
              onClick={triggerUpload}
              className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 px-5 py-2.5 rounded-full bg-black/60 backdrop-blur-sm border border-white/20 flex items-center gap-2 hover:bg-black/80 transition-colors"
            >
              <Upload size={16} className="text-white" />
              <span className="text-white text-sm font-medium">点击上传图片</span>
            </button>
          )}

          {/* 对比按钮 */}
          {optimizedOptions.length > 0 && !isOptimizing && (
            <button
              onClick={() => setShowCompare(v => !v)}
              className="absolute top-3 right-3 px-3 py-1.5 rounded-full bg-black/60 backdrop-blur-sm text-white text-xs font-medium flex items-center gap-1.5"
            >
              <Eye size={12} className={showCompare ? 'text-[#2196F3]' : 'text-white/60'} />
              {showCompare ? '退出对比' : '对比原图'}
            </button>
          )}

          {/* 对比遮罩 */}
          {showCompare && (
            <div className="absolute inset-0 flex">
              <div className="w-1/2 h-full border-r-2 border-white overflow-hidden">
                <img
                  src={tuneImageSource || DEFAULT_IMAGE_SOURCE}
                  alt="Original"
                  className="w-full h-full object-cover"
                  style={{ filter: 'none' }}
                />
                <div className="absolute bottom-2 left-2 px-2 py-1 rounded bg-black/50 text-xs text-white">原图</div>
              </div>
              <div className="w-1/2 h-full overflow-hidden">
                <div className="absolute bottom-2 right-2 px-2 py-1 rounded bg-black/50 text-xs text-white">优化后</div>
              </div>
            </div>
          )}
          
          {/* Processing Overlay */}
          {isOptimizing && (
            <div className="absolute inset-0 bg-black/70 flex flex-col items-center justify-center">
              <div className="w-16 h-16 rounded-full border-4 border-[#2196F3] border-t-transparent animate-spin mb-4" />
              <span className="text-white text-sm">智能优化中...</span>
              <div className="flex gap-2 mt-3">
                {selectedOptions.map((opt) => (
                  <div 
                    key={opt}
                    className={`px-2 py-1 rounded-full text-xs ${
                      optimizedOptions.includes(opt) 
                        ? 'bg-[#4CAF50] text-white' 
                        : 'bg-white/20 text-white/70'
                    }`}
                  >
                    {optimizeOptions.find(o => o.id === opt)?.name}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Optimized Overlay */}
          {!isOptimizing && optimizedOptions.length > 0 && (
            <div className="absolute inset-0 bg-black/60 flex items-center justify-center">
              <div className="flex flex-col items-center gap-2">
                <div className="w-12 h-12 rounded-full bg-[#4CAF50] flex items-center justify-center">
                  <Check size={24} className="text-white" />
                </div>
                <span className="text-white text-sm">优化完成</span>
              </div>
            </div>
          )}

          {/* Current Params */}
          <div className="absolute bottom-3 left-3 right-3">
            <div className="flex flex-wrap gap-2">
              <span className="px-2 py-1 rounded-full bg-black/50 backdrop-blur-sm text-white text-xs">
                对比度: +{aiParams.contrast}
              </span>
              <span className="px-2 py-1 rounded-full bg-black/50 backdrop-blur-sm text-white text-xs">
                锐度: +{aiParams.sharpness}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Optimize Button */}
      <div className="px-4 pb-4">
        <button
          onClick={handleOptimize}
          disabled={isOptimizing || selectedOptions.length === 0}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-[#2196F3] to-[#0D47A1] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98 disabled:opacity-50"
        >
          {isOptimizing ? (
            <>
              <RefreshCw size={18} className="animate-spin" />
              <span>优化中...</span>
            </>
          ) : (
            <>
              <Wand2 size={18} />
              <span>开始智能优化</span>
            </>
          )}
        </button>
      </div>

      {/* Options */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3">选择优化项目</p>
        
        <div className="space-y-3">
          {optimizeOptions.map((option) => {
            const Icon = option.icon;
            const isSelected = selectedOptions.includes(option.id);
            const isOptimized = optimizedOptions.includes(option.id);
            
            return (
              <button
                key={option.id}
                onClick={() => toggleOption(option.id)}
                disabled={isOptimizing}
                className={`w-full p-4 rounded-2xl flex items-center gap-4 transition-all ${
                  isOptimized
                    ? 'bg-[#4CAF50]/20 border border-[#4CAF50]/50'
                    : isSelected
                      ? 'bg-white/10 border border-white/20'
                      : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div 
                  className="w-12 h-12 rounded-xl flex items-center justify-center"
                  style={{ backgroundColor: `${option.color}20` }}
                >
                  <Icon size={24} style={{ color: option.color }} />
                </div>
                <div className="flex-1 text-left">
                  <p className="text-white font-medium">{option.name}</p>
                  <p className="text-white/50 text-xs">{option.desc}</p>
                </div>
                {isOptimized && (
                  <div className="w-6 h-6 rounded-full bg-[#4CAF50] flex items-center justify-center">
                    <Check size={14} className="text-white" />
                  </div>
                )}
                {!isOptimized && (
                  <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                    isSelected ? 'border-[#2196F3] bg-[#2196F3]' : 'border-white/30'
                  }`}>
                    {isSelected && <Check size={12} className="text-white" />}
                  </div>
                )}
              </button>
            );
          })}
        </div>

        {/* Info */}
        <div className="mt-6 p-4 rounded-2xl bg-white/5">
          <div className="flex items-start gap-3">
            <Cpu size={20} className="text-[#2196F3] mt-0.5" />
            <div>
              <p className="text-white text-sm font-medium">AI 智能引擎</p>
              <p className="text-white/50 text-xs mt-1">
                基于深度学习的图像优化算法，自动识别场景并调整最佳参数
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SmartOptimizePage;
