import React, { useState, useRef } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Cpu, Wand2, Check, RefreshCw, Zap, Sun, Droplets, Focus, Download, Upload } from 'lucide-react';

const optimizeOptions = [
  { id: 'hdr', name: 'HDR增强', icon: Sun, color: '#FF9800', desc: '提升动态范围，保留更多细节' },
  { id: 'denoise', name: '智能降噪', icon: Droplets, color: '#2196F3', desc: 'AI识别并消除噪点' },
  { id: 'sharpen', name: '锐化增强', icon: Focus, color: '#9C27B0', desc: '提升画面清晰度和质感' },
  { id: 'enhance', name: '综合优化', icon: Zap, color: '#4CAF50', desc: '一键优化全部参数' },
];

// 导出优化配置
const exportOptimizeConfig = (options: string[], params: Record<string, number>) => {
  const exportData = {
    version: '2.0',
    timestamp: new Date().toISOString(),
    type: 'smart-optimize',
    selectedOptions: options,
    params,
    app: 'OMaster',
  };
  
  const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `omaster-optimize-${Date.now()}.json`;
  a.click();
  URL.revokeObjectURL(url);
};

// 导入优化配置
const importOptimizeConfig = (file: File): Promise<{ selectedOptions: string[]; params: Record<string, number> }> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target?.result as string);
        if (data.selectedOptions) {
          resolve({ selectedOptions: data.selectedOptions, params: data.params || {} });
        } else {
          reject(new Error('无效的配置文件'));
        }
      } catch {
        reject(new Error('解析文件失败'));
      }
    };
    reader.onerror = () => reject(new Error('读取文件失败'));
    reader.readAsText(file);
  });
};

const SmartOptimizePage: React.FC = () => {
  const { aiParams, setAiParam, goBack } = useAppStore();
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [optimizedOptions, setOptimizedOptions] = useState<string[]>([]);
  const [selectedOptions, setSelectedOptions] = useState<string[]>(['enhance']);
  
  // 文件输入引用
  const fileInputRef = useRef<HTMLInputElement>(null);

  const toggleOption = (id: string) => {
    setSelectedOptions(prev => 
      prev.includes(id) 
        ? prev.filter(o => o !== id)
        : [...prev, id]
    );
  };
  
  // 导出配置
  const handleExport = () => {
    exportOptimizeConfig(selectedOptions, {
      contrast: aiParams.contrast,
      sharpness: aiParams.sharpness,
      brightness: aiParams.brightness,
    });
  };
  
  // 导入配置
  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    try {
      const { selectedOptions: options, params } = await importOptimizeConfig(file);
      setSelectedOptions(options);
      if (params.contrast !== undefined) setAiParam('contrast', params.contrast);
      if (params.sharpness !== undefined) setAiParam('sharpness', params.sharpness);
      if (params.brightness !== undefined) setAiParam('brightness', params.brightness);
    } catch (error) {
      console.error('导入失败:', error);
    }
    
    // 清空文件输入
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleOptimize = () => {
    if (selectedOptions.length === 0) return;
    
    setIsOptimizing(true);
    
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
          
          // 应用优化参数
          if (selectedOptions.includes('enhance')) {
            setAiParam('contrast', 15);
            setAiParam('sharpness', 25);
          }
          if (selectedOptions.includes('hdr')) {
            setAiParam('brightness', 10);
          }
          if (selectedOptions.includes('sharpen')) {
            setAiParam('sharpness', 30);
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
        accept=".json"
        onChange={handleImport}
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
        {/* 导入按钮 */}
        <button 
          onClick={() => fileInputRef.current?.click()}
          className="p-2 rounded-full hover:bg-white/10"
          title="导入配置"
        >
          <Upload size={18} className="text-white/50" />
        </button>
        {/* 导出按钮 */}
        <button 
          onClick={handleExport}
          className="p-2 rounded-full hover:bg-white/10"
          title="导出配置"
        >
          <Download size={18} className="text-white/50" />
        </button>
      </div>

      {/* Preview */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-gray-900 to-gray-800">
          <img 
            src="https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=600&h=400&fit=crop"
            alt="Preview"
            className="w-full h-full object-cover"
          />
          
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
