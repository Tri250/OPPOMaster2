import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Wand2, RefreshCw, Check, Sparkles, Sun, Moon, Contrast, Palette, Camera, Zap, Download, Share2, GripVertical, Lightbulb, TrendingUp } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { applyImageAdjustments, downloadImage, ImageAdjustParams, analyzeImageScene, SceneAnalysisResult } from '../../utils/imageProcessor';

// 哈苏大师优化风格 - 真实参数
const hasselbladStyles = [
  {
    id: 'natural',
    name: '哈苏自然',
    desc: '真实还原，细节丰富',
    color: '#4CAF50',
    icon: Sun,
    params: { saturation: 12, contrast: 8, brightness: 5, warmth: 0, cyanMagenta: 0, sharpness: 18, tone: 8, softLight: 15, vignette: false, filter: '原图' }
  },
  {
    id: 'portrait',
    name: '哈苏人像',
    desc: '柔美肤色，光影层次',
    color: '#E91E63',
    icon: Camera,
    params: { saturation: 8, contrast: 5, brightness: 8, warmth: 12, cyanMagenta: 0, sharpness: 12, tone: 5, softLight: 35, vignette: false, filter: '原图' }
  },
  {
    id: 'cinematic',
    name: '哈苏电影',
    desc: '电影质感，氛围感强',
    color: '#FF9800',
    icon: Moon,
    params: { saturation: 18, contrast: 22, brightness: 0, warmth: -5, cyanMagenta: 5, sharpness: 25, tone: 18, softLight: 20, vignette: true, filter: '胶片' }
  },
  {
    id: 'vintage',
    name: '哈苏复古',
    desc: '经典胶片，怀旧质感',
    color: '#795548',
    icon: Palette,
    params: { saturation: -5, contrast: 15, brightness: 0, warmth: 25, cyanMagenta: 0, sharpness: 15, tone: 20, softLight: 25, vignette: true, filter: '胶片' }
  },
];

// 优化选项 - 真实调整参数
const optimizeOptionsInit = [
  { id: 'hdr', name: 'HDR增强', desc: '动态范围优化', enabled: true, params: { contrast: 15, brightness: 8, softLight: 20 } },
  { id: 'noise', name: '智能降噪', desc: '噪点消除', enabled: true, params: { softLight: 30, brightness: 5 } },
  { id: 'sharp', name: '锐化增强', desc: '细节提升', enabled: true, params: { sharpness: 25 } },
  { id: 'color', name: '色彩优化', desc: '色彩校正', enabled: true, params: { saturation: 15, contrast: 8 } },
  { id: 'exposure', name: '曝光调整', desc: '亮度优化', enabled: false, params: { brightness: 15 } },
  { id: 'contrast', name: '对比度增强', desc: '层次感提升', enabled: false, params: { contrast: 25 } },
];

// 参数中文名映射
const paramLabelMap: Record<string, string> = {
  saturation: '饱和度',
  contrast: '对比度',
  brightness: '亮度',
  warmth: '色温',
  cyanMagenta: '青品',
  sharpness: '锐度',
  tone: '色调',
  softLight: '柔光',
  vignette: '暗角',
  filter: '滤镜',
};

const SmartOptimizePage: React.FC = () => {
  const { goBack } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [optimizedImage, setOptimizedImage] = useState<string>('');
  const [selectedStyle, setSelectedStyle] = useState<string | null>(null);
  const [options, setOptions] = useState(optimizeOptionsInit);
  const [showComparison, setShowComparison] = useState(false);

  // 优化强度
  const [optimizeIntensity, setOptimizeIntensity] = useState(75);

  // 前后对比滑块
  const [sliderPos, setSliderPos] = useState(50);
  const comparisonRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef(false);

  // AI优化建议
  const [sceneAnalysis, setSceneAnalysis] = useState<SceneAnalysisResult | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [aiSuggestions, setAiSuggestions] = useState<string[]>([]);

  // 优化前后参数
  const [beforeParams, setBeforeParams] = useState<ImageAdjustParams | null>(null);
  const [afterParams, setAfterParams] = useState<ImageAdjustParams | null>(null);

  // 上传图片后进行AI分析
  const handleImageSelect = useCallback(async (imageUrl: string) => {
    setUploadedImage(imageUrl);
    setOptimizedImage('');
    setSceneAnalysis(null);
    setAiSuggestions([]);
    setBeforeParams(null);
    setAfterParams(null);

    if (!imageUrl) return;

    setIsAnalyzing(true);
    try {
      const result = await analyzeImageScene(imageUrl);
      setSceneAnalysis(result);

      // 基于分析结果生成3条AI优化建议
      const suggestions: string[] = [];

      // 根据场景类型给出建议
      if (result.scene === '人像') {
        suggestions.push('建议开启柔光效果，提升肤色质感');
        suggestions.push('建议适当提升色温，营造温暖氛围');
        suggestions.push('建议选择哈苏人像风格，获得最佳人像效果');
      } else if (result.scene === '风景') {
        suggestions.push('建议开启HDR增强，扩展动态范围');
        suggestions.push('建议提升锐度，还原风景细节');
        suggestions.push('建议增强饱和度，让色彩更加鲜活');
      } else if (result.scene === '夜景') {
        suggestions.push('建议开启智能降噪，消除夜景噪点');
        suggestions.push('建议增强对比度，突出光影层次');
        suggestions.push('建议开启暗角效果，增强氛围感');
      } else if (result.scene === '日落黄昏') {
        suggestions.push('建议提升色温，强化暖色调氛围');
        suggestions.push('建议开启HDR增强，保留天空细节');
        suggestions.push('建议选择胶片滤镜，增添复古质感');
      } else if (result.scene === '美食') {
        suggestions.push('建议提升饱和度，让食物更诱人');
        suggestions.push('建议增加柔光，营造温馨用餐氛围');
        suggestions.push('建议适当提升亮度，突出食物色泽');
      } else if (result.scene === '建筑') {
        suggestions.push('建议增强锐度，呈现建筑线条');
        suggestions.push('建议提升对比度，强化光影结构');
        suggestions.push('建议选择哈苏自然风格，还原建筑本色');
      } else if (result.scene === '街拍') {
        suggestions.push('建议开启对比度增强，增加画面张力');
        suggestions.push('建议选择胶片滤镜，营造街头氛围');
        suggestions.push('建议开启暗角效果，聚焦画面主体');
      } else if (result.scene === '海景水域') {
        suggestions.push('建议降低色温，还原清澈水色');
        suggestions.push('建议开启HDR增强，保留天空与水面细节');
        suggestions.push('建议提升锐度，展现水波纹理');
      } else if (result.scene === '花卉') {
        suggestions.push('建议增强饱和度，让花卉色彩更饱满');
        suggestions.push('建议提升柔光，营造梦幻氛围');
        suggestions.push('建议选择哈苏自然风格，真实还原花色');
      } else {
        // 通用建议，基于分析数据
        if (result.suggestedParams.contrast > 10) {
          suggestions.push('建议开启HDR增强，提升画面动态范围');
        }
        if (result.suggestedParams.sharpness > 15) {
          suggestions.push('建议提升锐度，增强画面清晰度');
        }
        if (result.suggestedParams.saturation > 10) {
          suggestions.push('建议增强色彩饱和度，让画面更生动');
        }
      }

      // 确保至少3条建议
      while (suggestions.length < 3) {
        const fallbacks = [
          '建议开启智能降噪，提升画面纯净度',
          '建议调整优化强度至70%以上，获得更明显的优化效果',
          '建议尝试不同哈苏风格，找到最佳匹配',
        ];
        suggestions.push(fallbacks[suggestions.length]);
      }

      setAiSuggestions(suggestions.slice(0, 3));
    } catch (e) {
      console.error('AI分析失败:', e);
      setAiSuggestions([
        '建议开启HDR增强，提升画面动态范围',
        '建议提升锐度，增强画面清晰度',
        '建议增强色彩饱和度，让画面更生动',
      ]);
    } finally {
      setIsAnalyzing(false);
    }
  }, []);

  // 对比滑块拖动处理
  const handleSliderMouseDown = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault();
    isDragging.current = true;
  }, []);

  const handleSliderMove = useCallback((clientX: number) => {
    if (!isDragging.current || !comparisonRef.current) return;
    const rect = comparisonRef.current.getBoundingClientRect();
    const x = clientX - rect.left;
    const percent = Math.max(0, Math.min(100, (x / rect.width) * 100));
    setSliderPos(percent);
  }, []);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => handleSliderMove(e.clientX);
    const handleTouchMove = (e: TouchEvent) => {
      if (e.touches.length > 0) handleSliderMove(e.touches[0].clientX);
    };
    const handleUp = () => { isDragging.current = false; };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('touchmove', handleTouchMove);
    window.addEventListener('mouseup', handleUp);
    window.addEventListener('touchend', handleUp);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('mouseup', handleUp);
      window.removeEventListener('touchend', handleUp);
    };
  }, [handleSliderMove]);

  // 根据强度缩放参数
  const scaleParamsByIntensity = (params: ImageAdjustParams, intensity: number): ImageAdjustParams => {
    const ratio = intensity / 100;
    return {
      saturation: Math.round(params.saturation * ratio),
      contrast: Math.round(params.contrast * ratio),
      brightness: Math.round(params.brightness * ratio),
      warmth: Math.round(params.warmth * ratio),
      cyanMagenta: Math.round(params.cyanMagenta * ratio),
      sharpness: Math.round(params.sharpness * ratio),
      tone: Math.round(params.tone * ratio),
      softLight: Math.round(params.softLight * ratio),
      vignette: params.vignette && ratio > 0.3,
      filter: params.filter,
    };
  };

  // 智能优化处理 - 真实算法
  const handleOptimize = async () => {
    if (!uploadedImage || !selectedStyle) return;

    setIsOptimizing(true);

    try {
      // 合并哈苏风格参数和优化选项参数
      const style = hasselbladStyles.find(s => s.id === selectedStyle);
      if (!style) return;

      // 基础参数
      const mergedParams: ImageAdjustParams = {
        saturation: style.params.saturation,
        contrast: style.params.contrast,
        brightness: style.params.brightness,
        warmth: style.params.warmth,
        cyanMagenta: style.params.cyanMagenta,
        sharpness: style.params.sharpness,
        tone: style.params.tone,
        softLight: style.params.softLight,
        vignette: style.params.vignette,
        filter: style.params.filter
      };

      // 叠加启用的优化选项参数
      options.filter(o => o.enabled).forEach(opt => {
        Object.entries(opt.params).forEach(([key, value]) => {
          if (key in mergedParams && typeof value === 'number') {
            const currentVal = mergedParams[key as keyof ImageAdjustParams];
            if (typeof currentVal === 'number') {
              (mergedParams as unknown as Record<string, number | string | boolean>)[key] = currentVal + value;
            }
          } else if (key === 'vignette' && value) {
            (mergedParams as unknown as Record<string, number | string | boolean>)[key] = true;
          }
        });
      });

      // 保存优化前参数（全量未缩放）
      setBeforeParams({ ...mergedParams });

      // 根据优化强度缩放参数
      const scaledParams = scaleParamsByIntensity(mergedParams, optimizeIntensity);

      // 保存优化后参数（缩放后）
      setAfterParams({ ...scaledParams });

      // 真实调用图片处理算法
      const result = await applyImageAdjustments(uploadedImage, scaledParams);
      setOptimizedImage(result);
      setShowComparison(true);
      setSliderPos(50);
    } catch (e) {
      console.error('优化失败:', e);
    } finally {
      setIsOptimizing(false);
    }
  };

  // 切换优化选项
  const toggleOption = (id: string) => {
    setOptions(prev => prev.map(opt =>
      opt.id === id ? { ...opt, enabled: !opt.enabled } : opt
    ));
  };

  // 一键分享
  const handleShare = async () => {
    if (!optimizedImage) return;

    try {
      // 将base64转为Blob
      const response = await fetch(optimizedImage);
      const blob = await response.blob();
      const file = new File([blob], `OMaster_${Date.now()}.jpg`, { type: 'image/jpeg' });

      if (navigator.share && navigator.canShare({ files: [file] })) {
        await navigator.share({
          title: '哈苏大师出片',
          text: '由OMaster智能优化生成',
          files: [file],
        });
      } else {
        // 降级：复制到剪贴板
        await navigator.clipboard.write([
          new ClipboardItem({ 'image/png': blob })
        ]);
        alert('图片已复制到剪贴板，可粘贴分享');
      }
    } catch (e) {
      console.error('分享失败:', e);
      // 最终降级：直接下载
      const style = hasselbladStyles.find(s => s.id === selectedStyle);
      downloadImage(optimizedImage, `OMaster_${style?.name || 'optimized'}_${Date.now()}.jpg`);
    }
  };

  // 计算参数差异
  const getParamDiff = () => {
    if (!beforeParams || !afterParams) return [];
    const diff: { key: string; label: string; before: string; after: string; changed: boolean }[] = [];
    const numericKeys: (keyof ImageAdjustParams)[] = [
      'saturation', 'contrast', 'brightness', 'warmth', 'cyanMagenta', 'sharpness', 'tone', 'softLight'
    ];

    numericKeys.forEach(key => {
      const bVal = beforeParams[key] as number;
      const aVal = afterParams[key] as number;
      diff.push({
        key,
        label: paramLabelMap[key] || key,
        before: `${bVal}`,
        after: `${aVal}`,
        changed: bVal !== aVal,
      });
    });

    // 暗角
    diff.push({
      key: 'vignette',
      label: paramLabelMap['vignette'],
      before: beforeParams.vignette ? '开' : '关',
      after: afterParams.vignette ? '开' : '关',
      changed: beforeParams.vignette !== afterParams.vignette,
    });

    // 滤镜
    diff.push({
      key: 'filter',
      label: paramLabelMap['filter'],
      before: beforeParams.filter || '原图',
      after: afterParams.filter || '原图',
      changed: (beforeParams.filter || '原图') !== (afterParams.filter || '原图'),
    });

    return diff;
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-y-auto">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">智能优化</h1>
        {uploadedImage && (
          <div className="ml-auto px-2 py-1 rounded-full bg-[#FF6B35]/20">
            <span className="text-[#FF6B35] text-xs">已上传照片</span>
          </div>
        )}
      </div>

      {/* Image Upload */}
      <div className="px-4 py-4">
        <ImageUploader
          onImageSelect={handleImageSelect}
          currentImage={uploadedImage}
          title="上传照片智能优化"
          description="AI分析并优化至哈苏大师出片"
        />
      </div>

      {/* AI优化建议 */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Lightbulb size={12} className="text-[#FF6B35]" />
            AI优化建议
            {isAnalyzing && <RefreshCw size={12} className="animate-spin text-[#FF6B35]" />}
          </p>
          {isAnalyzing ? (
            <div className="p-4 rounded-2xl bg-white/5">
              <p className="text-white/40 text-xs">正在分析图片特征...</p>
            </div>
          ) : aiSuggestions.length > 0 ? (
            <div className="space-y-2">
              {aiSuggestions.map((suggestion, idx) => (
                <div
                  key={idx}
                  className="p-3 rounded-xl bg-white/5 flex items-start gap-3"
                >
                  <div className="w-6 h-6 rounded-full bg-[#FF6B35]/20 flex items-center justify-center shrink-0 mt-0.5">
                    <span className="text-[#FF6B35] text-xs font-bold">{idx + 1}</span>
                  </div>
                  <div>
                    <p className="text-white text-sm">{suggestion}</p>
                    {sceneAnalysis && idx === 0 && (
                      <p className="text-white/40 text-xs mt-1">
                        识别场景：{sceneAnalysis.scene}（置信度 {sceneAnalysis.confidence}%）
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      )}

      {/* Hasselblad Styles */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Sparkles size={12} />
            哈苏大师优化风格
          </p>
          <div className="grid grid-cols-2 gap-3">
            {hasselbladStyles.map((style) => {
              const Icon = style.icon;
              const isSelected = selectedStyle === style.id;

              return (
                <button
                  key={style.id}
                  onClick={() => setSelectedStyle(style.id)}
                  className={`p-4 rounded-2xl transition-all ${
                    isSelected
                      ? 'bg-[#FF6B35]/20 border border-[#FF6B35]'
                      : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className="w-12 h-12 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${style.color}20` }}
                    >
                      <Icon size={24} style={{ color: style.color }} />
                    </div>
                    <div>
                      <p className="text-white text-sm font-medium">{style.name}</p>
                      <p className="text-white/50 text-xs">{style.desc}</p>
                    </div>
                    {isSelected && (
                      <div className="ml-auto w-6 h-6 rounded-full bg-[#FF6B35] flex items-center justify-center">
                        <Check size={14} className="text-white" />
                      </div>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Optimize Options */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3">优化选项</p>
          <div className="space-y-2">
            {options.map((option) => (
              <div
                key={option.id}
                onClick={() => toggleOption(option.id)}
                className="p-3 rounded-xl bg-white/5 flex items-center justify-between cursor-pointer hover:bg-white/10 transition-all"
              >
                <div>
                  <p className="text-white text-sm font-medium">{option.name}</p>
                  <p className="text-white/50 text-xs">{option.desc}</p>
                </div>
                <div className={`w-10 h-6 rounded-full transition-all ${
                  option.enabled ? 'bg-[#FF6B35]' : 'bg-white/10'
                }`}>
                  <div className={`w-5 h-5 rounded-full bg-white transition-all ${
                    option.enabled ? 'translate-x-5' : 'translate-x-0.5'
                  }`} />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 优化强度滑块 */}
      {uploadedImage && selectedStyle && (
        <div className="px-4 pb-4">
          <div className="flex items-center justify-between mb-3">
            <p className="text-white/50 text-xs flex items-center gap-2">
              <TrendingUp size={12} className="text-[#FF6B35]" />
              优化强度
            </p>
            <span className="text-[#FF6B35] text-xs font-bold">{optimizeIntensity}%</span>
          </div>
          <div className="relative">
            <input
              type="range"
              min="0"
              max="100"
              value={optimizeIntensity}
              onChange={(e) => setOptimizeIntensity(Number(e.target.value))}
              className="w-full h-2 rounded-full appearance-none cursor-pointer"
              style={{
                background: `linear-gradient(to right, #FF6B35 ${optimizeIntensity}%, rgba(255,255,255,0.1) ${optimizeIntensity}%)`,
              }}
            />
            <div className="flex justify-between mt-1">
              <span className="text-white/30 text-[10px]">轻微</span>
              <span className="text-white/30 text-[10px]">标准</span>
              <span className="text-white/30 text-[10px]">强烈</span>
            </div>
          </div>
        </div>
      )}

      {/* Optimize Button */}
      {uploadedImage && selectedStyle && (
        <div className="px-4 pb-4">
          <button
            onClick={handleOptimize}
            disabled={isOptimizing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 disabled:opacity-50"
          >
            {isOptimizing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>AI优化中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>开始智能优化</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Result Preview */}
      {optimizedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3">优化结果</p>

          {/* Comparison Toggle */}
          <div className="flex gap-2 mb-3">
            <button
              onClick={() => setShowComparison(false)}
              className={`px-3 py-1.5 rounded-lg text-xs ${
                !showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/50'
              }`}
            >
              优化后
            </button>
            <button
              onClick={() => { setShowComparison(true); setSliderPos(50); }}
              className={`px-3 py-1.5 rounded-lg text-xs ${
                showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/50'
              }`}
            >
              滑动对比
            </button>
          </div>

          {/* Image Preview */}
          {showComparison ? (
            /* 可拖动对比滑块 */
            <div
              ref={comparisonRef}
              className="relative rounded-2xl overflow-hidden select-none touch-none"
              style={{ aspectRatio: '16/9' }}
            >
              {/* 优化后图片（底层，完整显示） */}
              <img
                src={optimizedImage}
                alt="Optimized"
                className="absolute inset-0 w-full h-full object-cover"
                draggable={false}
              />
              {/* 原图（上层，通过clip裁剪） */}
              <div
                className="absolute inset-0"
                style={{ clipPath: `inset(0 ${100 - sliderPos}% 0 0)` }}
              >
                <img
                  src={uploadedImage}
                  alt="Original"
                  className="w-full h-full object-cover"
                  draggable={false}
                />
              </div>
              {/* 分割线 */}
              <div
                className="absolute top-0 bottom-0 w-0.5 bg-white/90 z-10"
                style={{ left: `${sliderPos}%`, transform: 'translateX(-50%)' }}
              />
              {/* 拖动手柄 */}
              <div
                className="absolute top-1/2 z-20 -translate-y-1/2 -translate-x-1/2 w-10 h-10 rounded-full bg-white/90 shadow-lg flex items-center justify-center cursor-ew-resize"
                style={{ left: `${sliderPos}%` }}
                onMouseDown={handleSliderMouseDown}
                onTouchStart={handleSliderMouseDown}
              >
                <GripVertical size={18} className="text-gray-700" />
              </div>
              {/* 标签 */}
              <div className="absolute top-3 left-3 px-2 py-1 rounded-md bg-black/60 backdrop-blur-sm z-10">
                <span className="text-white text-[10px]">原图</span>
              </div>
              <div className="absolute top-3 right-3 px-2 py-1 rounded-md bg-[#FF6B35]/80 backdrop-blur-sm z-10">
                <span className="text-white text-[10px]">优化后</span>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl overflow-hidden relative">
              <img src={optimizedImage} alt="Optimized" className="w-full aspect-video object-cover" />
              <div className="absolute bottom-2 left-2 px-2 py-1 rounded-lg bg-[#FF6B35]/80 backdrop-blur-sm">
                <span className="text-white text-xs font-medium">哈苏大师出片</span>
              </div>
            </div>
          )}

          {/* 优化前后参数对比 */}
          {beforeParams && afterParams && (
            <div className="mt-4 p-4 rounded-2xl bg-white/5">
              <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Contrast size={12} className="text-[#FF6B35]" />
                参数对比（强度 {optimizeIntensity}%）
              </p>
              <div className="space-y-1.5">
                {getParamDiff().map(item => (
                  <div key={item.key} className="flex items-center text-xs">
                    <span className="text-white/50 w-14 shrink-0">{item.label}</span>
                    <span className="text-white/30 flex-1 text-right">{item.before}</span>
                    <span className="text-white/20 mx-2">→</span>
                    <span className={`flex-1 ${item.changed ? 'text-[#FF6B35] font-medium' : 'text-white/50'}`}>
                      {item.after}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 下载和分享按钮 */}
          <div className="flex gap-3 mt-3">
            <button
              onClick={() => {
                const style = hasselbladStyles.find(s => s.id === selectedStyle);
                downloadImage(optimizedImage, `OMaster_${style?.name || 'optimized'}_${Date.now()}.jpg`);
              }}
              className="flex-1 py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90"
            >
              <Download size={18} />
              <span>保存</span>
            </button>
            <button
              onClick={handleShare}
              className="flex-1 py-3 rounded-xl bg-white/10 flex items-center justify-center gap-2 text-white font-medium transition-all hover:bg-white/20"
            >
              <Share2 size={18} />
              <span>分享</span>
            </button>
          </div>
        </div>
      )}

      {/* Tips */}
      <div className="flex-1 px-4 pb-4">
        <div className="p-4 rounded-2xl bg-white/5">
          <div className="flex items-start gap-3">
            <Zap size={18} className="text-[#FF6B35] mt-0.5" />
            <div>
              <p className="text-white text-sm font-medium">智能优化说明</p>
              <ul className="text-white/50 text-xs mt-2 space-y-1">
                <li>• AI分析照片内容，自动选择最佳优化方案</li>
                <li>• 基于哈苏大师调色经验，呈现专业质感</li>
                <li>• 支持HDR增强、智能降噪、锐化增强等</li>
                <li>• 优化强度滑块可精细控制效果程度</li>
                <li>• 滑动对比可直观查看优化前后差异</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SmartOptimizePage;
