import React, { useState, useEffect, useRef } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Sliders, Sparkles, Camera, Sun, Eye, Zap, RefreshCw, Check, Brain, Target, Layers } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { userImageStore, UserImage } from '../../store/userImageStore';
import { imageAnalysisService, ImageAnalysis, RecommendedParams } from '../../services/imageAnalysisService';

const ParamAdjustPage: React.FC = () => {
  const { cameraParams, setCameraParam, aiParams, setAiParam, goBack } = useAppStore();
  const [userImage, setUserImage] = useState<UserImage | null>(null);
  const [analysis, setAnalysis] = useState<ImageAnalysis | null>(null);
  const [recommendations, setRecommendations] = useState<RecommendedParams[]>([]);
  const [analyzing, setAnalyzing] = useState(false);
  const [activeTab, setActiveTab] = useState<'camera' | 'color' | 'effects'>('color');
  const [compareMode, setCompareMode] = useState(false);
  const [appliedStyle, setAppliedStyle] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = userImageStore.subscribe(image => {
      setUserImage(image);
      if (image) {
        analyzeImage(image);
      } else {
        setAnalysis(null);
        setRecommendations([]);
      }
    });
    return unsubscribe;
  }, []);

  const analyzeImage = async (image: UserImage) => {
    setAnalyzing(true);
    try {
      const result = await imageAnalysisService.analyze(image);
      setAnalysis(result);
      const recs = imageAnalysisService.recommendMultipleStyles(result);
      setRecommendations(recs);
    } catch (e) {
      console.error(e);
    } finally {
      setAnalyzing(false);
    }
  };

  const applyRecommendation = (rec: RecommendedParams) => {
    setAiParam('saturation', rec.saturation);
    setAiParam('contrast', rec.contrast);
    setAiParam('brightness', rec.brightness);
    setAiParam('warmth', rec.warmth);
    setAiParam('sharpness', rec.sharpness);
    setAiParam('highlights', rec.highlights);
    setAiParam('shadows', rec.shadows);
    setAiParam('clarity', rec.clarity);
    setAiParam('noiseReduction', rec.noiseReduction);
    setAiParam('skinSmooth', rec.skinSmooth);
    setAppliedStyle(rec.style);
    setTimeout(() => setAppliedStyle(null), 2000);
  };

  // 相机参数
  const cameraParamsConfig = [
    { key: 'iso', label: 'ISO 感光度', min: 50, max: 12800, step: 50, marks: [100, 200, 400, 800, 1600, 3200], unit: '', description: '感光度越高，画面越亮但噪点越多' },
    { key: 'shutter', label: '快门速度', min: 1, max: 4000, step: 1, format: (v: number) => v >= 1000 ? `${(v/1000).toFixed(1)}s` : `1/${v}s`, description: '快门越快，运动越清晰' },
    { key: 'aperture', label: '光圈', min: 1.4, max: 22, step: 0.1, format: (v: number) => `f/${v.toFixed(1)}`, description: '光圈越大，背景虚化越明显' },
    { key: 'wb', label: '白平衡', min: 2000, max: 10000, step: 100, format: (v: number) => `${v}K`, description: '色温越低画面越暖，越高越冷' },
  ];

  // 调色参数
  const colorParamsConfig = [
    { key: 'saturation', label: '饱和度', icon: '🎨', min: -100, max: 100, step: 1, description: '影响色彩鲜艳程度' },
    { key: 'contrast', label: '对比度', icon: '⚖️', min: -100, max: 100, step: 1, description: '影响明暗反差' },
    { key: 'brightness', label: '亮度', icon: '☀️', min: -100, max: 100, step: 1, description: '影响整体明暗' },
    { key: 'warmth', label: '色温', icon: '🌡️', min: -100, max: 100, step: 1, description: '冷暖色调调整' },
  ];

  // 效果参数
  const effectsParamsConfig = [
    { key: 'sharpness', label: '锐度', icon: '🔪', min: 0, max: 100, step: 1, description: '影响画面清晰程度' },
    { key: 'clarity', label: '清晰度', icon: '🔍', min: 0, max: 100, step: 1, description: '中等对比度增强' },
    { key: 'highlights', label: '高光', icon: '✨', min: -100, max: 100, step: 1, description: '亮部细节调整' },
    { key: 'shadows', label: '阴影', icon: '🌑', min: -100, max: 100, step: 1, description: '暗部细节调整' },
    { key: 'noiseReduction', label: '降噪', icon: '🔇', min: 0, max: 100, step: 1, description: '降低暗部噪点' },
    { key: 'skinSmooth', label: '美肤', icon: '👤', min: 0, max: 100, step: 1, description: '人像美肤处理' },
  ];

  // 档位强度
  const strengthLevels = [
    { name: '原图', multiplier: 0, color: '#9E9E9E' },
    { name: '轻微', multiplier: 0.3, color: '#4CAF50' },
    { name: '中等', multiplier: 0.6, color: '#FF9800' },
    { name: '强力', multiplier: 1.0, color: '#F44336' },
  ];

  const formatVal = (v: number, format?: (v: number) => string) => 
    format ? format(v) : `${v}`;

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">参数精细调节</h1>
        {userImage && (
          <button
            onClick={() => setCompareMode(!compareMode)}
            className={`ml-auto p-2 rounded-full transition-colors ${
              compareMode ? 'bg-[#FF6B35] text-white' : 'hover:bg-white/10 text-white/70'
            }`}
          >
            <Eye size={16} />
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto pb-4 scrollbar-hide">
        {/* 上传区域 */}
        <div className="px-4 pt-4">
          <ImageUploader
            onImageLoaded={(img) => setUserImage(img)}
            buttonText="上传您要调色的照片"
            hint="上传后 AI 将自动分析图片并推荐哈苏参数"
            sampleImages={[
              { url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=200&h=200&fit=crop', label: '风景', tag: '山' },
              { url: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=200&h=200&fit=crop', label: '人像', tag: '人物' },
              { url: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200&h=200&fit=crop', label: '夜景', tag: '城市' },
              { url: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=200&fit=crop', label: '美食', tag: '食物' },
            ]}
          />
        </div>

        {/* 分析中 */}
        {analyzing && (
          <div className="px-4 mt-4 p-4 rounded-2xl bg-white/5 flex items-center gap-3">
            <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
            <div>
              <p className="text-white text-sm font-medium">AI 正在分析图片...</p>
              <p className="text-white/50 text-xs">检测场景 · 评估色彩 · 推荐参数</p>
            </div>
          </div>
        )}

        {/* AI 分析结果 */}
        {analysis && !analyzing && (
          <>
            {/* 分析报告 */}
            <div className="px-4 mt-4">
              <div className="p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30">
                <div className="flex items-center gap-2 mb-3">
                  <Brain size={16} className="text-[#FF6B35]" />
                  <h3 className="text-white text-sm font-bold">AI 智能分析</h3>
                </div>
                <div className="grid grid-cols-4 gap-2 mb-3">
                  <div className="bg-black/30 rounded-lg p-2 text-center">
                    <p className="text-[#FF6B35] text-base font-bold">{analysis.detectedScene}</p>
                    <p className="text-white/50 text-[10px]">场景</p>
                  </div>
                  <div className="bg-black/30 rounded-lg p-2 text-center">
                    <p className="text-white text-base font-bold">{Math.round(analysis.brightness)}</p>
                    <p className="text-white/50 text-[10px]">亮度</p>
                  </div>
                  <div className="bg-black/30 rounded-lg p-2 text-center">
                    <p className="text-white text-base font-bold">{Math.round(analysis.saturation * 100)}%</p>
                    <p className="text-white/50 text-[10px]">饱和</p>
                  </div>
                  <div className="bg-black/30 rounded-lg p-2 text-center">
                    <p className="text-white text-base font-bold">{Math.round(analysis.contrast * 100)}%</p>
                    <p className="text-white/50 text-[10px]">对比</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="text-white/40 text-[10px]">主色调:</span>
                  {analysis.dominantColors.map((c, i) => (
                    <div key={i} className="w-5 h-5 rounded" style={{ backgroundColor: c }} />
                  ))}
                </div>
              </div>
            </div>

            {/* 推荐参数 */}
            {recommendations.length > 0 && (
              <div className="px-4 mt-4">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <Sparkles size={14} className="text-[#FF6B35]" />
                    <h3 className="text-white text-sm font-bold">AI 推荐哈苏参数</h3>
                  </div>
                  <span className="text-white/40 text-xs">{recommendations.length} 套</span>
                </div>
                <div className="space-y-2">
                  {recommendations.map((rec, i) => (
                    <button
                      key={i}
                      onClick={() => applyRecommendation(rec)}
                      className={`w-full text-left p-3 rounded-xl transition-all ${
                        appliedStyle === rec.style
                          ? 'bg-[#FF6B35]/20 border border-[#FF6B35]/50'
                          : 'bg-white/5 hover:bg-white/10 border border-transparent'
                      }`}
                    >
                      <div className="flex items-start justify-between mb-1.5">
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            {i === 0 && (
                              <span className="px-1.5 py-0.5 rounded bg-[#FF6B35] text-white text-[9px] font-bold">
                                推荐
                              </span>
                            )}
                            <p className="text-white text-sm font-medium">{rec.style}</p>
                          </div>
                          <p className="text-white/50 text-xs mt-0.5">{rec.reason}</p>
                        </div>
                        {appliedStyle === rec.style && (
                          <Check size={16} className="text-[#FF6B35] flex-shrink-0" />
                        )}
                      </div>
                      <div className="flex flex-wrap gap-1 mt-2">
                        <span className="px-1.5 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">饱 {rec.saturation > 0 ? '+' : ''}{rec.saturation}</span>
                        <span className="px-1.5 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">对 {rec.contrast > 0 ? '+' : ''}{rec.contrast}</span>
                        <span className="px-1.5 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">亮 {rec.brightness > 0 ? '+' : ''}{rec.brightness}</span>
                        <span className="px-1.5 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">暖 {rec.warmth > 0 ? '+' : ''}{rec.warmth}</span>
                        <span className="px-1.5 py-0.5 rounded bg-black/30 text-white/70 text-[10px]">锐 {rec.sharpness}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        {/* Tabs */}
        <div className="px-4 mt-4">
          <div className="flex gap-1 p-1 rounded-xl bg-white/5">
            <button
              onClick={() => setActiveTab('camera')}
              className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 text-xs font-medium transition-all ${
                activeTab === 'camera' ? 'bg-[#FF6B35] text-white' : 'text-white/60'
              }`}
            >
              <Camera size={14} />
              <span>相机参数</span>
            </button>
            <button
              onClick={() => setActiveTab('color')}
              className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 text-xs font-medium transition-all ${
                activeTab === 'color' ? 'bg-[#FF6B35] text-white' : 'text-white/60'
              }`}
            >
              <Sun size={14} />
              <span>调色</span>
            </button>
            <button
              onClick={() => setActiveTab('effects')}
              className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 text-xs font-medium transition-all ${
                activeTab === 'effects' ? 'bg-[#FF6B35] text-white' : 'text-white/60'
              }`}
            >
              <Layers size={14} />
              <span>效果</span>
            </button>
          </div>
        </div>

        {/* 档位强度 */}
        {activeTab !== 'camera' && (
          <div className="px-4 mt-4">
            <div className="p-3 rounded-xl bg-white/5">
              <p className="text-white/50 text-xs mb-2">档位强度</p>
              <div className="grid grid-cols-4 gap-1.5">
                {strengthLevels.map(level => (
                  <button
                    key={level.name}
                    className="py-2 rounded-lg text-xs font-medium transition-all hover:scale-105"
                    style={{ backgroundColor: `${level.color}30`, color: level.color }}
                  >
                    {level.name}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* 参数列表 */}
        <div className="px-4 mt-4">
          {activeTab === 'camera' && cameraParamsConfig.map(param => {
            const value = cameraParams[param.key as keyof typeof cameraParams] as number;
            return (
              <div key={param.key} className="mb-3 p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-white text-sm font-medium">{param.label}</span>
                  <span className="text-[#FF6B35] text-sm font-bold">
                    {formatVal(value, param.format)}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  step={param.step}
                  value={value}
                  onChange={(e) => setCameraParam(param.key, parseFloat(e.target.value))}
                  className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                />
                <p className="text-white/40 text-[10px] mt-1.5">{param.description}</p>
                {param.marks && (
                  <div className="flex justify-between mt-1.5">
                    {param.marks.map(mark => (
                      <button
                        key={mark}
                        onClick={() => setCameraParam(param.key, mark)}
                        className="text-white/30 text-[9px] hover:text-[#FF6B35] transition-colors"
                      >
                        {mark}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            );
          })}

          {(activeTab === 'color' || activeTab === 'effects') && (activeTab === 'color' ? colorParamsConfig : effectsParamsConfig).map(param => {
            const value = aiParams[param.key as keyof typeof aiParams] as number;
            return (
              <div key={param.key} className="mb-3 p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <span className="text-base">{param.icon}</span>
                    <span className="text-white text-sm font-medium">{param.label}</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-bold">
                    {value > 0 ? '+' : ''}{value}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  step={param.step}
                  value={value}
                  onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                  className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                />
                <p className="text-white/40 text-[10px] mt-1.5">{param.description}</p>
              </div>
            );
          })}

          {/* 联动调整提示 */}
          {activeTab === 'effects' && (
            <div className="mt-4 p-3 rounded-xl bg-[#FF6B35]/10 border border-[#FF6B35]/30">
              <div className="flex items-center gap-2 mb-1">
                <Zap size={12} className="text-[#FF6B35]" />
                <span className="text-white text-xs font-medium">联动调整</span>
              </div>
              <p className="text-white/50 text-[10px]">
                锐度与清晰度会自动联动调节，避免过度处理
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ParamAdjustPage;
