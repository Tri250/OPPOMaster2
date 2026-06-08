import React, { useState, useEffect, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Wand2, RefreshCw, Check, Sparkles, Sun, Moon, Contrast, Palette, Camera, Mountain, User, UtensilsCrossed, Building2, TreePine, Car, Waves, Flower2, Cat, Download, RotateCcw, Save, ChevronDown, ChevronUp, Trash2, Columns, SlidersHorizontal } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { analyzeImageScene, applyImageAdjustments, downloadImage, ImageAdjustParams } from '../../utils/imageProcessor';

// 2026年OPPO哈苏大师模式预设情景 - 完整参数
const presetScenes = [
  { id: 'portrait', name: '人像大师', desc: '柔美肤色，自然光影', color: '#E91E63', icon: User,
    params: { saturation: 10, contrast: 5, brightness: 8, warmth: 8, cyanMagenta: 0, sharpness: 15, tone: 5, softLight: 35, vignette: false, filter: '原图' } },
  { id: 'landscape', name: '风景大师', desc: '通透质感，色彩饱满', color: '#4CAF50', icon: Mountain,
    params: { saturation: 20, contrast: 15, brightness: 10, warmth: -5, cyanMagenta: -5, sharpness: 25, tone: 15, softLight: 10, vignette: false, filter: '原图' } },
  { id: 'night', name: '夜景大师', desc: '降噪增强，氛围感强', color: '#3F51B5', icon: Moon,
    params: { saturation: 25, contrast: 20, brightness: 0, warmth: -10, cyanMagenta: 5, sharpness: 30, tone: 20, softLight: 20, vignette: true, filter: '原图' } },
  { id: 'food', name: '美食大师', desc: '暖色调，食欲感强', color: '#FF9800', icon: UtensilsCrossed,
    params: { saturation: 15, contrast: 10, brightness: 5, warmth: 20, cyanMagenta: 0, sharpness: 12, tone: 5, softLight: 40, vignette: false, filter: '原图' } },
  { id: 'architecture', name: '建筑大师', desc: '线条清晰，质感强', color: '#607D8B', icon: Building2,
    params: { saturation: 8, contrast: 15, brightness: 0, warmth: 0, cyanMagenta: 0, sharpness: 20, tone: 15, softLight: 15, vignette: false, filter: '原图' } },
  { id: 'nature', name: '自然大师', desc: '清新自然，生机盎然', color: '#8BC34A', icon: TreePine,
    params: { saturation: 18, contrast: 12, brightness: 5, warmth: 5, cyanMagenta: 0, sharpness: 22, tone: 8, softLight: 20, vignette: false, filter: '原图' } },
  { id: 'street', name: '街拍大师', desc: '人文气息，故事感', color: '#FF5722', icon: Car,
    params: { saturation: 12, contrast: 18, brightness: 0, warmth: 10, cyanMagenta: 0, sharpness: 18, tone: 12, softLight: 20, vignette: true, filter: '胶片' } },
  { id: 'water', name: '水景大师', desc: '流动质感，梦幻感', color: '#00BCD4', icon: Waves,
    params: { saturation: 15, contrast: 8, brightness: 10, warmth: -8, cyanMagenta: 0, sharpness: 15, tone: 8, softLight: 15, vignette: false, filter: '原图' } },
  { id: 'flower', name: '花卉大师', desc: '色彩鲜艳，细节丰富', color: '#E91E63', icon: Flower2,
    params: { saturation: 25, contrast: 10, brightness: 5, warmth: 5, cyanMagenta: 0, sharpness: 20, tone: 5, softLight: 30, vignette: false, filter: '原图' } },
  { id: 'pet', name: '宠物大师', desc: '毛发细节，眼神灵动', color: '#9C27B0', icon: Cat,
    params: { saturation: 12, contrast: 8, brightness: 5, warmth: 8, cyanMagenta: 0, sharpness: 25, tone: 8, softLight: 20, vignette: false, filter: '原图' } },
];

// 参数中文映射
const paramLabels: Record<string, string> = {
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

// 精细调节滑块配置
const fineTuneSliders = [
  { key: 'saturation', label: '饱和度', icon: Palette },
  { key: 'contrast', label: '对比度', icon: Contrast },
  { key: 'warmth', label: '色温', icon: Sun },
  { key: 'sharpness', label: '锐度', icon: Camera },
] as const;

// 默认参数
const defaultParams = { saturation: 0, contrast: 0, warmth: 0, sharpness: 0 };

// localStorage 键名
const SAVED_PARAMS_KEY = 'omaster_saved_fine_tune_params';

interface SavedParamSet {
  id: string;
  name: string;
  params: { saturation: number; contrast: number; warmth: number; sharpness: number };
  createdAt: number;
}

function loadSavedParams(): SavedParamSet[] {
  try {
    const raw = localStorage.getItem(SAVED_PARAMS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function persistSavedParams(list: SavedParamSet[]) {
  localStorage.setItem(SAVED_PARAMS_KEY, JSON.stringify(list));
}

const AIFineTunePage: React.FC = () => {
  const { goBack, aiParams, setAiParam } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [recommendedScene, setRecommendedScene] = useState<string | null>(null);
  const [appliedScene, setAppliedScene] = useState<string | null>(null);
  const [processedImage, setProcessedImage] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);

  // 精细调节参数（本地状态，与 store 同步）
  const [fineParams, setFineParams] = useState({
    saturation: aiParams.saturation,
    contrast: aiParams.contrast,
    warmth: aiParams.warmth,
    sharpness: aiParams.sharpness,
  });

  // 对比预览模式
  const [showCompare, setShowCompare] = useState(false);
  const [compareImage, setCompareImage] = useState<string>('');

  // 预设详情展开
  const [expandedScene, setExpandedScene] = useState<string | null>(null);

  // 已保存参数列表
  const [savedParams, setSavedParams] = useState<SavedParamSet[]>(loadSavedParams);
  const [saveName, setSaveName] = useState('');
  const [showSaveInput, setShowSaveInput] = useState(false);

  // 同步 store 参数到本地
  useEffect(() => {
    setFineParams({
      saturation: aiParams.saturation,
      contrast: aiParams.contrast,
      warmth: aiParams.warmth,
      sharpness: aiParams.sharpness,
    });
  }, [aiParams]);

  // 实时应用参数到图片
  const applyParamsToImage = useCallback(async (params: ImageAdjustParams) => {
    if (!uploadedImage) return;
    setIsProcessing(true);
    try {
      const result = await applyImageAdjustments(uploadedImage, params);
      setProcessedImage(result);
    } catch (e) {
      console.error('应用参数失败:', e);
    } finally {
      setIsProcessing(false);
    }
  }, [uploadedImage]);

  // 滑块变化处理 - 实时调节
  const handleSliderChange = (key: string, value: number) => {
    setFineParams(prev => ({ ...prev, [key]: value }));
    setAiParam(key, value);
  };

  // 滑块释放后应用处理
  const handleSliderCommit = () => {
    if (!uploadedImage) return;
    const fullParams: ImageAdjustParams = {
      saturation: fineParams.saturation,
      contrast: fineParams.contrast,
      brightness: aiParams.brightness ?? 0,
      warmth: fineParams.warmth,
      cyanMagenta: 0,
      sharpness: fineParams.sharpness,
      tone: 0,
      softLight: 0,
      vignette: false,
      filter: '原图',
    };
    applyParamsToImage(fullParams);
  };

  // AI分析图片推荐情景 - 真实像素分析
  const handleAnalyze = async () => {
    if (!uploadedImage) return;

    setIsAnalyzing(true);
    setRecommendedScene(null);
    setProcessedImage('');

    try {
      // 真实调用像素级场景分析
      const result = await analyzeImageScene(uploadedImage);
      // 找到匹配的情景
      const matchedScene = presetScenes.find(s => s.id === result.scene || s.name === result.scene);
      const fallbackScene = presetScenes.find(s => s.id === result.hasselbladStyle) || presetScenes[0];
      const finalScene = matchedScene || fallbackScene;
      setRecommendedScene(finalScene.id);
    } catch (e) {
      console.error('AI分析失败:', e);
    } finally {
      setIsAnalyzing(false);
    }
  };

  // 应用情景预设 - 真实处理
  const applyScene = async (scene: typeof presetScenes[0]) => {
    setAiParam('saturation', scene.params.saturation);
    setAiParam('contrast', scene.params.contrast);
    setAiParam('warmth', scene.params.warmth);
    setAiParam('sharpness', scene.params.sharpness);
    setAppliedScene(scene.id);

    // 真实应用参数到图片
    if (uploadedImage) {
      await applyParamsToImage(scene.params);
    }

    setTimeout(() => setAppliedScene(null), 3000);
  };

  // 重置参数
  const handleReset = () => {
    setFineParams({ ...defaultParams });
    setAiParam('saturation', 0);
    setAiParam('contrast', 0);
    setAiParam('warmth', 0);
    setAiParam('sharpness', 0);
    setProcessedImage('');
  };

  // 保存当前参数
  const handleSaveParams = () => {
    if (!saveName.trim()) return;
    const newSet: SavedParamSet = {
      id: `saved_${Date.now()}`,
      name: saveName.trim(),
      params: { ...fineParams },
      createdAt: Date.now(),
    };
    const updated = [newSet, ...savedParams];
    setSavedParams(updated);
    persistSavedParams(updated);
    setSaveName('');
    setShowSaveInput(false);
  };

  // 删除已保存参数
  const handleDeleteSaved = (id: string) => {
    const updated = savedParams.filter(p => p.id !== id);
    setSavedParams(updated);
    persistSavedParams(updated);
  };

  // 应用已保存参数
  const handleApplySaved = async (paramSet: SavedParamSet) => {
    setFineParams({ ...paramSet.params });
    setAiParam('saturation', paramSet.params.saturation);
    setAiParam('contrast', paramSet.params.contrast);
    setAiParam('warmth', paramSet.params.warmth);
    setAiParam('sharpness', paramSet.params.sharpness);

    if (uploadedImage) {
      const fullParams: ImageAdjustParams = {
        saturation: paramSet.params.saturation,
        contrast: paramSet.params.contrast,
        brightness: aiParams.brightness ?? 0,
        warmth: paramSet.params.warmth,
        cyanMagenta: 0,
        sharpness: paramSet.params.sharpness,
        tone: 0,
        softLight: 0,
        vignette: false,
        filter: '原图',
      };
      await applyParamsToImage(fullParams);
    }
  };

  // 对比预览
  const handleCompare = async () => {
    if (!uploadedImage) return;
    if (showCompare) {
      setShowCompare(false);
      return;
    }
    // 生成当前参数的处理图
    const fullParams: ImageAdjustParams = {
      saturation: fineParams.saturation,
      contrast: fineParams.contrast,
      brightness: aiParams.brightness ?? 0,
      warmth: fineParams.warmth,
      cyanMagenta: 0,
      sharpness: fineParams.sharpness,
      tone: 0,
      softLight: 0,
      vignette: false,
      filter: '原图',
    };
    setIsProcessing(true);
    try {
      const result = await applyImageAdjustments(uploadedImage, fullParams);
      setCompareImage(result);
      setProcessedImage(result);
      setShowCompare(true);
    } catch (e) {
      console.error('生成对比图失败:', e);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">AI 微调</h1>
        {uploadedImage && (
          <div className="ml-auto px-2 py-1 rounded-full bg-[#FF6B35]/20">
            <span className="text-[#FF6B35] text-xs">已上传照片</span>
          </div>
        )}
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* Image Upload */}
        <div className="px-4 py-4">
          <ImageUploader
            onImageSelect={setUploadedImage}
            currentImage={uploadedImage}
            title="上传照片AI微调"
            description="AI分析并推荐哈苏大师模式"
          />
        </div>

        {/* AI Analyze Button */}
        {uploadedImage && (
          <div className="px-4 pb-4">
            <button
              onClick={handleAnalyze}
              disabled={isAnalyzing}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium"
            >
              {isAnalyzing ? (
                <>
                  <RefreshCw size={18} className="animate-spin" />
                  <span>AI分析中...</span>
                </>
              ) : (
                <>
                  <Wand2 size={18} />
                  <span>AI分析推荐情景</span>
                </>
              )}
            </button>
          </div>
        )}

        {/* 精细参数滑块 */}
        {uploadedImage && (
          <div className="px-4 pb-4">
            <div className="p-4 rounded-2xl bg-white/5">
              <p className="text-white text-sm font-medium mb-4 flex items-center gap-2">
                <SlidersHorizontal size={14} />
                精细调节
              </p>
              <div className="space-y-4">
                {fineTuneSliders.map(({ key, label, icon: Icon }) => (
                  <div key={key}>
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <Icon size={14} className="text-white/50" />
                        <span className="text-white/70 text-sm">{label}</span>
                      </div>
                      <span className={`text-sm font-bold tabular-nums ${
                        fineParams[key] > 0 ? 'text-[#FF6B35]' :
                        fineParams[key] < 0 ? 'text-[#4FC3F7]' :
                        'text-white/40'
                      }`}>
                        {fineParams[key] > 0 ? '+' : ''}{fineParams[key]}
                      </span>
                    </div>
                    <div className="relative">
                      <input
                        type="range"
                        min={-100}
                        max={100}
                        value={fineParams[key]}
                        onChange={e => handleSliderChange(key, Number(e.target.value))}
                        onMouseUp={handleSliderCommit}
                        onTouchEnd={handleSliderCommit}
                        className="w-full h-1.5 rounded-full appearance-none cursor-pointer bg-white/10
                          [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-5 [&::-webkit-slider-thumb]:h-5
                          [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#FF6B35]
                          [&::-webkit-slider-thumb]:shadow-[0_0_8px_rgba(255,107,53,0.5)]
                          [&::-webkit-slider-thumb]:cursor-pointer [&::-webkit-slider-thumb]:border-2 [&::-webkit-slider-thumb]:border-white/30
                          [&::-moz-range-thumb]:w-5 [&::-moz-range-thumb]:h-5 [&::-moz-range-thumb]:rounded-full
                          [&::-moz-range-thumb]:bg-[#FF6B35] [&::-moz-range-thumb]:border-2 [&::-moz-range-thumb]:border-white/30
                          [&::-moz-range-thumb]:cursor-pointer"
                        style={{
                          background: `linear-gradient(to right, #4FC3F7 0%, #4FC3F7 ${((fineParams[key] + 100) / 200) * 50}%, #1a1a1a ${((fineParams[key] + 100) / 200) * 50}%, #1a1a1a ${50 + (fineParams[key] / 200) * 50}%, #FF6B35 ${50 + (fineParams[key] / 200) * 50}%, #FF6B35 100%)`
                        }}
                      />
                      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-0.5 h-3 bg-white/20 pointer-events-none" />
                    </div>
                  </div>
                ))}
              </div>

              {/* 操作按钮组 */}
              <div className="flex gap-2 mt-4">
                <button
                  onClick={handleReset}
                  className="flex-1 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-center gap-1.5 text-white/70 text-sm transition-colors"
                >
                  <RotateCcw size={14} />
                  重置参数
                </button>
                <button
                  onClick={handleCompare}
                  disabled={isProcessing || !uploadedImage}
                  className="flex-1 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 flex items-center justify-center gap-1.5 text-white/70 text-sm transition-colors disabled:opacity-40"
                >
                  {isProcessing ? <RefreshCw size={14} className="animate-spin" /> : <Columns size={14} />}
                  {showCompare ? '关闭对比' : '对比预览'}
                </button>
                <button
                  onClick={() => setShowSaveInput(true)}
                  className="flex-1 py-2.5 rounded-xl bg-[#FF6B35]/20 hover:bg-[#FF6B35]/30 flex items-center justify-center gap-1.5 text-[#FF6B35] text-sm transition-colors"
                >
                  <Save size={14} />
                  保存参数
                </button>
              </div>

              {/* 保存参数输入 */}
              {showSaveInput && (
                <div className="mt-3 flex gap-2">
                  <input
                    type="text"
                    value={saveName}
                    onChange={e => setSaveName(e.target.value)}
                    placeholder="输入参数名称"
                    maxLength={20}
                    className="flex-1 px-3 py-2 rounded-lg bg-white/5 border border-white/10 text-white text-sm placeholder:text-white/30 focus:outline-none focus:border-[#FF6B35]/50"
                    onKeyDown={e => e.key === 'Enter' && handleSaveParams()}
                  />
                  <button
                    onClick={handleSaveParams}
                    disabled={!saveName.trim()}
                    className="px-4 py-2 rounded-lg bg-[#FF6B35] text-white text-sm font-medium disabled:opacity-40"
                  >
                    保存
                  </button>
                  <button
                    onClick={() => { setShowSaveInput(false); setSaveName(''); }}
                    className="px-3 py-2 rounded-lg bg-white/5 text-white/50 text-sm"
                  >
                    取消
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* 对比预览 - 左右分屏 */}
        {showCompare && uploadedImage && compareImage && (
          <div className="px-4 pb-4">
            <div className="rounded-2xl overflow-hidden border border-white/10">
              <div className="p-2 bg-white/5 flex items-center justify-between">
                <span className="text-white/50 text-xs">对比预览</span>
                <button onClick={() => setShowCompare(false)} className="text-white/40 text-xs hover:text-white/70">
                  关闭
                </button>
              </div>
              <div className="flex">
                <div className="w-1/2 relative">
                  <img src={uploadedImage} alt="原图" className="w-full aspect-video object-cover" />
                  <div className="absolute bottom-0 left-0 right-0 p-1.5 bg-black/60 text-center">
                    <span className="text-white/80 text-xs">原图</span>
                  </div>
                </div>
                <div className="w-1/2 border-l border-white/10 relative">
                  <img src={compareImage} alt="AI微调后" className="w-full aspect-video object-cover" />
                  <div className="absolute bottom-0 left-0 right-0 p-1.5 bg-[#FF6B35]/80 text-center">
                    <span className="text-white text-xs">AI微调</span>
                  </div>
                </div>
              </div>
              <div className="p-2 bg-white/5 text-center">
                <span className="text-white/40 text-xs">左：原图 | 右：AI微调效果</span>
              </div>
            </div>
          </div>
        )}

        {/* 已保存参数列表 */}
        {savedParams.length > 0 && (
          <div className="px-4 pb-4">
            <div className="p-4 rounded-2xl bg-white/5">
              <p className="text-white text-sm font-medium mb-3 flex items-center gap-2">
                <Save size={14} className="text-[#FF6B35]" />
                已保存参数
                <span className="text-white/30 text-xs">({savedParams.length})</span>
              </p>
              <div className="space-y-2">
                {savedParams.map(sp => (
                  <div key={sp.id} className="flex items-center gap-3 p-3 rounded-xl bg-white/5 hover:bg-white/8 transition-colors">
                    <div className="flex-1 min-w-0">
                      <p className="text-white text-sm font-medium truncate">{sp.name}</p>
                      <p className="text-white/40 text-xs mt-0.5">
                        饱和度{sp.params.saturation > 0 ? '+' : ''}{sp.params.saturation} ·
                        对比度{sp.params.contrast > 0 ? '+' : ''}{sp.params.contrast} ·
                        色温{sp.params.warmth > 0 ? '+' : ''}{sp.params.warmth} ·
                        锐度{sp.params.sharpness > 0 ? '+' : ''}{sp.params.sharpness}
                      </p>
                    </div>
                    <button
                      onClick={() => handleApplySaved(sp)}
                      disabled={isProcessing}
                      className="px-3 py-1.5 rounded-lg bg-[#FF6B35]/20 text-[#FF6B35] text-xs font-medium hover:bg-[#FF6B35]/30 transition-colors disabled:opacity-40"
                    >
                      应用
                    </button>
                    <button
                      onClick={() => handleDeleteSaved(sp.id)}
                      className="p-1.5 rounded-lg hover:bg-white/10 transition-colors"
                    >
                      <Trash2 size={14} className="text-white/30 hover:text-red-400" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Preset Scenes */}
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Sparkles size={12} />
            2026年OPPO哈苏大师模式预设情景
          </p>

          <div className="space-y-3">
            {presetScenes.map((scene) => {
              const Icon = scene.icon;
              const isRecommended = recommendedScene === scene.id;
              const isApplied = appliedScene === scene.id;
              const isExpanded = expandedScene === scene.id;

              return (
                <div key={scene.id}>
                  <button
                    onClick={() => applyScene(scene)}
                    disabled={isProcessing}
                    className={`w-full relative p-4 rounded-2xl transition-all text-left ${
                      isApplied ? 'bg-[#FF6B35]/30 border border-[#FF6B35]' :
                      isRecommended ? 'bg-[#4CAF50]/20 border border-[#4CAF50]/50' :
                      'bg-white/5 hover:bg-white/10'
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
                      <div className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0" style={{ backgroundColor: `${scene.color}20` }}>
                        <Icon size={24} style={{ color: scene.color }} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <p className="text-white text-sm font-medium">{scene.name}</p>
                          {isRecommended && (
                            <span className="text-[#4CAF50] text-xs flex items-center gap-1">
                              <Sparkles size={10} /> AI推荐
                            </span>
                          )}
                        </div>
                        <p className="text-white/50 text-xs">{scene.desc}</p>
                      </div>
                    </div>
                  </button>

                  {/* 展开/收起详情按钮 */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setExpandedScene(isExpanded ? null : scene.id);
                    }}
                    className="w-full flex items-center justify-center gap-1 py-1.5 text-white/30 hover:text-white/50 transition-colors"
                  >
                    <span className="text-xs">{isExpanded ? '收起详情' : '查看详情'}</span>
                    {isExpanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
                  </button>

                  {/* 预设情景详情 - 完整10项参数 */}
                  {isExpanded && (
                    <div className="p-3 rounded-xl bg-white/5 mb-1">
                      <div className="grid grid-cols-2 gap-x-4 gap-y-2">
                        {Object.entries(scene.params).map(([key, value]) => (
                          <div key={key} className="flex items-center justify-between">
                            <span className="text-white/50 text-xs">{paramLabels[key] || key}</span>
                            <span className={`text-xs font-medium tabular-nums ${
                              key === 'vignette' ? (value ? 'text-[#FF6B35]' : 'text-white/30') :
                              key === 'filter' ? 'text-[#FF6B35]' :
                              typeof value === 'number' && value > 0 ? 'text-[#FF6B35]' :
                              typeof value === 'number' && value < 0 ? 'text-[#4FC3F7]' :
                              'text-white/40'
                            }`}>
                              {key === 'vignette' ? (value ? '开启' : '关闭') :
                               key === 'filter' ? value :
                               typeof value === 'number' ? `${value > 0 ? '+' : ''}${value}` :
                               String(value)}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Processed Image Preview (非对比模式) */}
        {processedImage && !showCompare && (
          <div className="px-4 pb-4">
            <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
              <Sparkles size={12} />
              AI 微调出片
            </p>
            <div className="grid grid-cols-2 gap-2 mb-3">
              <div className="rounded-xl overflow-hidden">
                <img src={uploadedImage} alt="原图" className="w-full aspect-video object-cover" />
                <div className="p-2 bg-white/5 text-center">
                  <span className="text-white/50 text-xs">原图</span>
                </div>
              </div>
              <div className="rounded-xl overflow-hidden">
                <img src={processedImage} alt="AI微调后" className="w-full aspect-video object-cover" />
                <div className="p-2 bg-[#FF6B35]/20 text-center">
                  <span className="text-[#FF6B35] text-xs">哈苏AI出片</span>
                </div>
              </div>
            </div>
            <button
              onClick={() => downloadImage(processedImage, `OMaster_AIFineTune_${Date.now()}.jpg`)}
              className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white text-sm font-medium"
            >
              <Download size={16} />
              <span>保存AI微调出片</span>
            </button>
          </div>
        )}

        {/* Current Params */}
        <div className="px-4 pb-4">
          <div className="p-4 rounded-2xl bg-white/5">
            <p className="text-white text-sm font-medium mb-4">当前调色参数</p>
            <div className="space-y-3">
              {Object.entries(aiParams).map(([key, value]) => (
                <div key={key} className="flex items-center justify-between">
                  <span className="text-white/70 text-sm">{key === 'saturation' ? '饱和度' : key === 'contrast' ? '对比度' : key === 'warmth' ? '色温' : key === 'brightness' ? '亮度' : '锐度'}</span>
                  <span className="text-[#FF6B35] text-sm font-bold">{value > 0 ? '+' : ''}{value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AIFineTunePage;
