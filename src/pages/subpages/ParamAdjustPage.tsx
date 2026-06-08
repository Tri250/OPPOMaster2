import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Aperture, Timer, Sun, Thermometer, Sparkles, Camera, Check, RefreshCw, Wand2, Download, Image as ImageIcon } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { analyzeImageScene, applyImageAdjustments, downloadImage, ImageAdjustParams } from '../../utils/imageProcessor';

// OPPO哈苏大师风格预设
const hasselbladPresets = [
  {
    id: 'portrait',
    name: '哈苏人像大师',
    icon: Camera,
    color: '#E91E63',
    desc: '柔美肤色，自然光影',
    params: { iso: 200, shutter: 125, aperture: 2.8, wb: 5500, saturation: 10, contrast: 5, warmth: 8, sharpness: 15 },
    imgParams: { saturation: 10, contrast: 5, brightness: 8, warmth: 8, cyanMagenta: 0, sharpness: 15, tone: 5, softLight: 35, vignette: false, filter: '原图' }
  },
  {
    id: 'landscape',
    name: '哈苏风景大师',
    icon: Sun,
    color: '#4CAF50',
    desc: '通透质感，色彩饱满',
    params: { iso: 100, shutter: 60, aperture: 8, wb: 5600, saturation: 20, contrast: 15, warmth: -5, sharpness: 25 },
    imgParams: { saturation: 20, contrast: 15, brightness: 10, warmth: -5, cyanMagenta: -5, sharpness: 25, tone: 15, softLight: 10, vignette: false, filter: '原图' }
  },
  {
    id: 'night',
    name: '哈苏夜景大师',
    icon: Sparkles,
    color: '#3F51B5',
    desc: '降噪增强，氛围感强',
    params: { iso: 3200, shutter: 30, aperture: 2.8, wb: 4000, saturation: 25, contrast: 20, warmth: -10, sharpness: 30 },
    imgParams: { saturation: 25, contrast: 20, brightness: 0, warmth: -10, cyanMagenta: 5, sharpness: 30, tone: 20, softLight: 20, vignette: true, filter: '原图' }
  },
  {
    id: 'film',
    name: '哈苏胶片大师',
    icon: Aperture,
    color: '#FF9800',
    desc: '复古质感，经典色调',
    params: { iso: 400, shutter: 125, aperture: 4, wb: 5200, saturation: 5, contrast: 10, warmth: 15, sharpness: 20 },
    imgParams: { saturation: -5, contrast: 15, brightness: 0, warmth: 25, cyanMagenta: 0, sharpness: 15, tone: 20, softLight: 25, vignette: true, filter: '胶片' }
  },
];

const quickPresets = [
  { name: '人像', iso: 200, shutter: 125, aperture: 2.8, wb: 5500 },
  { name: '风景', iso: 100, shutter: 60, aperture: 8, wb: 5600 },
  { name: '夜景', iso: 3200, shutter: 30, aperture: 2.8, wb: 4000 },
  { name: '运动', iso: 800, shutter: 500, aperture: 4, wb: 5500 },
];

const ParamAdjustPage: React.FC = () => {
  const { cameraParams, setCameraParam, aiParams, setAiParam, goBack } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [recommendedPreset, setRecommendedPreset] = useState<string | null>(null);
  const [appliedPreset, setAppliedPreset] = useState<string | null>(null);
  const [processedImage, setProcessedImage] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);

  const params = [
    { 
      key: 'iso', 
      label: 'ISO 感光度', 
      icon: Aperture,
      min: 50, 
      max: 12800, 
      step: 50,
      marks: [50, 100, 200, 400, 800, 1600, 3200, 6400, 12800]
    },
    { 
      key: 'shutter', 
      label: '快门速度', 
      icon: Timer,
      min: 1, 
      max: 1000, 
      step: 1,
      format: (v: number) => v >= 1000 ? `${v/1000}s` : `1/${v}s`
    },
    { 
      key: 'aperture', 
      label: '光圈', 
      icon: Aperture,
      min: 1.4, 
      max: 22, 
      step: 0.1,
      format: (v: number) => `f/${v.toFixed(1)}`
    },
    { 
      key: 'wb', 
      label: '白平衡', 
      icon: Thermometer,
      min: 2000, 
      max: 10000, 
      step: 100,
      format: (v: number) => `${v}K`
    },
  ];

  const aiParamsList = [
    { key: 'saturation', label: '饱和度', min: -100, max: 100 },
    { key: 'contrast', label: '对比度', min: -100, max: 100 },
    { key: 'warmth', label: '色温偏移', min: -100, max: 100 },
    { key: 'sharpness', label: '锐度', min: 0, max: 100 },
  ];

  // AI分析图片推荐参数 - 真实像素分析
  const handleAnalyzeImage = async () => {
    if (!uploadedImage) return;

    setIsAnalyzing(true);
    setRecommendedPreset(null);
    setProcessedImage('');

    try {
      // 真实调用像素级场景分析
      const result = await analyzeImageScene(uploadedImage);

      // 根据分析结果匹配最佳预设
      let matchedPreset: typeof hasselbladPresets[0] | undefined;
      if (result.scene === '人像' || result.hasselbladStyle === 'portrait') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'portrait');
      } else if (result.scene === '风景' || result.scene === '花卉' || result.scene === '海景水域' || result.scene === '自然' || result.hasselbladStyle === 'natural') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'landscape');
      } else if (result.scene === '夜景' || result.hasselbladStyle === 'cinematic') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'night');
      } else if (result.scene === '日落黄昏') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'film');
      }

      const finalPreset = matchedPreset || hasselbladPresets[0];
      setRecommendedPreset(finalPreset.id);
    } catch (e) {
      console.error('AI分析失败:', e);
    } finally {
      setIsAnalyzing(false);
    }
  };

  // 应用哈苏大师预设 - 真实处理
  const applyHasselbladPreset = async (preset: typeof hasselbladPresets[0]) => {
    setCameraParam('iso', preset.params.iso);
    setCameraParam('shutter', preset.params.shutter);
    setCameraParam('aperture', preset.params.aperture);
    setCameraParam('wb', preset.params.wb);
    setAiParam('saturation', preset.params.saturation);
    setAiParam('contrast', preset.params.contrast);
    setAiParam('warmth', preset.params.warmth);
    setAiParam('sharpness', preset.params.sharpness);
    setAppliedPreset(preset.id);

    // 真实应用参数到图片
    if (uploadedImage && preset.imgParams) {
      setIsProcessing(true);
      try {
        const result = await applyImageAdjustments(uploadedImage, preset.imgParams);
        setProcessedImage(result);
      } catch (e) {
        console.error('应用参数失败:', e);
      } finally {
        setIsProcessing(false);
      }
    }

    setTimeout(() => setAppliedPreset(null), 3000);
  };

  const applyQuickPreset = (preset: typeof quickPresets[0]) => {
    setCameraParam('iso', preset.iso);
    setCameraParam('shutter', preset.shutter);
    setCameraParam('aperture', preset.aperture);
    setCameraParam('wb', preset.wb);
  };

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
        <h1 className="text-lg font-bold text-white">参数精细调节</h1>
        {uploadedImage && (
          <div className="ml-auto px-2 py-1 rounded-full bg-[#FF6B35]/20">
            <span className="text-[#FF6B35] text-xs">已上传照片</span>
          </div>
        )}
      </div>

      {/* Image Upload Section */}
      <div className="px-4 py-4">
        <ImageUploader 
          onImageSelect={setUploadedImage}
          currentImage={uploadedImage}
          title="上传照片分析"
          description="AI将分析并推荐哈苏大师参数"
        />
      </div>

      {/* AI Analyze Button */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <button
            onClick={handleAnalyzeImage}
            disabled={isAnalyzing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98 disabled:opacity-50"
          >
            {isAnalyzing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>AI分析中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>AI分析推荐参数</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Hasselblad Presets */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3">OPPO 哈苏大师风格参数</p>
          <div className="grid grid-cols-2 gap-3">
            {hasselbladPresets.map((preset) => {
              const Icon = preset.icon;
              const isRecommended = recommendedPreset === preset.id;
              const isApplied = appliedPreset === preset.id;
              
              return (
                <button
                  key={preset.id}
                  onClick={() => applyHasselbladPreset(preset)}
                  className={`relative p-4 rounded-2xl transition-all ${
                    isApplied
                      ? 'bg-[#FF6B35]/30 border border-[#FF6B35]'
                      : isRecommended
                        ? 'bg-[#4CAF50]/20 border border-[#4CAF50]/50'
                        : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  {isApplied && (
                    <div className="absolute inset-0 flex items-center justify-center bg-[#FF6B35]/20 rounded-2xl">
                      <div className="w-10 h-10 rounded-full bg-[#FF6B35] flex items-center justify-center">
                        <Check size={20} className="text-white" />
                      </div>
                    </div>
                  )}
                  
                  <div className="flex items-center gap-3 relative z-10">
                    <div 
                      className="w-12 h-12 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${preset.color}20` }}
                    >
                      <Icon size={24} style={{ color: preset.color }} />
                    </div>
                    <div className="flex-1">
                      <p className="text-white text-sm font-medium">{preset.name}</p>
                      <p className="text-white/50 text-xs">{preset.desc}</p>
                      {isRecommended && (
                        <span className="text-[#4CAF50] text-xs mt-1 flex items-center gap-1">
                          <Sparkles size={10} />
                          AI推荐
                        </span>
                      )}
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Processed Image Preview */}
      {processedImage && uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Sparkles size={12} />
            参数调节出片
          </p>
          <div className="grid grid-cols-2 gap-2 mb-3">
            <div className="rounded-xl overflow-hidden">
              <img src={uploadedImage} alt="原图" className="w-full aspect-video object-cover" />
              <div className="p-2 bg-white/5 text-center">
                <span className="text-white/50 text-xs">原图</span>
              </div>
            </div>
            <div className="rounded-xl overflow-hidden">
              <img src={processedImage} alt="参数调节后" className="w-full aspect-video object-cover" />
              <div className="p-2 bg-[#E91E63]/20 text-center">
                <span className="text-[#E91E63] text-xs">哈苏参数出片</span>
              </div>
            </div>
          </div>
          <button
            onClick={() => downloadImage(processedImage, `OMaster_Hasselblad_${Date.now()}.jpg`)}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#E91E63] to-[#C2185B] flex items-center justify-center gap-2 text-white text-sm font-medium"
          >
            <Download size={16} />
            <span>保存参数出片</span>
          </button>
        </div>
      )}

      {/* Quick Presets */}
      <div className="px-4 py-4">
        <p className="text-white/50 text-xs mb-3">快捷档位</p>
        <div className="flex gap-2">
          {quickPresets.map((preset) => (
            <button
              key={preset.name}
              onClick={() => applyQuickPreset(preset)}
              className="flex-1 py-2 rounded-xl bg-white/5 text-white text-sm font-medium transition-all hover:bg-white/10 active:scale-95"
            >
              {preset.name}
            </button>
          ))}
        </div>
      </div>

      {/* Param Controls */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        {/* Camera Params */}
        <div className="space-y-4 mb-6">
          <p className="text-white/50 text-xs">相机参数</p>
          {params.map((param) => {
            const Icon = param.icon;
            const value = cameraParams[param.key as keyof typeof cameraParams];
            return (
              <div key={param.key} className="bg-white/5 rounded-2xl p-4">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-[#E91E63]/20 flex items-center justify-center">
                    <Icon size={20} className="text-[#E91E63]" />
                  </div>
                  <div className="flex-1">
                    <span className="text-white text-sm font-medium">{param.label}</span>
                    <span className="text-[#E91E63] text-lg font-bold ml-2">
                      {param.format ? param.format(value) : value}
                    </span>
                  </div>
                </div>

                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  step={param.step}
                  value={value}
                  onChange={(e) => setCameraParam(param.key, parseFloat(e.target.value))}
                  className="w-full h-3 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#E91E63]"
                />

                {param.marks && (
                  <div className="flex justify-between mt-2">
                    {param.marks.map((mark) => (
                      <button
                        key={mark}
                        onClick={() => setCameraParam(param.key, mark)}
                        className="text-white/30 text-[10px] hover:text-white/60 transition-colors"
                      >
                        {mark}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* AI Params */}
        <div className="space-y-4">
          <p className="text-white/50 text-xs">调色参数</p>
          {aiParamsList.map((param) => {
            const value = aiParams[param.key as keyof typeof aiParams];
            return (
              <div key={param.key} className="bg-white/5 rounded-xl p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-white text-sm font-medium">{param.label}</span>
                  <span className="text-[#FF6B35] text-sm font-bold">
                    {value > 0 ? '+' : ''}{value}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  value={value}
                  onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                  className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default ParamAdjustPage;