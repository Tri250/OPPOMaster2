import React, { useState, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { 
  ArrowLeft, Camera, Sparkles, Check, RefreshCw, Mountain, User, Moon, 
  UtensilsCrossed, Building2, TreePine, Sun, Cloud, Coffee, Heart,
  Zap, Brain, Target, Award, Crown, Image as ImageIcon
} from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { userImageStore, UserImage } from '../../store/userImageStore';
import { imageAnalysisService, ImageAnalysis, RecommendedParams } from '../../services/imageAnalysisService';

const AISceneRecognitionPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [userImage, setUserImage] = useState<UserImage | null>(null);
  const [analysis, setAnalysis] = useState<ImageAnalysis | null>(null);
  const [recommendation, setRecommendation] = useState<RecommendedParams | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [optimized, setOptimized] = useState(false);

  useEffect(() => {
    const unsubscribe = userImageStore.subscribe(image => {
      setUserImage(image);
      if (image) {
        analyzeImage(image);
      } else {
        setAnalysis(null);
        setRecommendation(null);
        setOptimized(false);
      }
    });
    return unsubscribe;
  }, []);

  const analyzeImage = async (image: UserImage) => {
    setIsAnalyzing(true);
    setOptimized(false);
    try {
      const result = await imageAnalysisService.analyze(image);
      setAnalysis(result);
      const rec = imageAnalysisService.recommendHasselbladParams(result);
      setRecommendation(rec);
      
      // 自动应用参数优化
      setTimeout(() => {
        applyOptimization(rec);
      }, 800);
    } catch (e) {
      console.error(e);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const applyOptimization = (rec: RecommendedParams) => {
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
    setOptimized(true);
    setTimeout(() => setOptimized(false), 3000);
  };

  // 不同哈苏大师风格
  const hasselbladMasterStyles = [
    {
      id: 'hncs_natural',
      name: '哈苏自然',
      english: 'Natural',
      description: '真实还原，低饱和度',
      params: { saturation: 5, contrast: 8, brightness: 0, warmth: 2, sharpness: 10, highlights: -5, shadows: 3, clarity: 10, noiseReduction: 0, skinSmooth: 0 },
    },
    {
      id: 'hncs_rich',
      name: '哈苏浓郁',
      english: 'Rich',
      description: '饱和色彩，浓郁层次',
      params: { saturation: 15, contrast: 12, brightness: 0, warmth: 0, sharpness: 18, highlights: -8, shadows: 5, clarity: 20, noiseReduction: 0, skinSmooth: 0 },
    },
    {
      id: 'hncs_portrait',
      name: '哈苏人像',
      english: 'Portrait',
      description: '肤色自然，柔光美肤',
      params: { saturation: 8, contrast: 5, brightness: 5, warmth: 8, sharpness: 10, highlights: -8, shadows: 5, clarity: 12, noiseReduction: 10, skinSmooth: 25 },
    },
    {
      id: 'hncs_film',
      name: '哈苏胶片',
      english: 'Film',
      description: '致敬 Hasselblad 500',
      params: { saturation: -5, contrast: 12, brightness: 0, warmth: 10, sharpness: 12, highlights: -8, shadows: 5, clarity: 8, noiseReduction: 0, skinSmooth: 0 },
    },
    {
      id: 'hncs_bw',
      name: '哈苏黑白',
      english: 'Mono',
      description: '极致黑白，复古纪实',
      params: { saturation: -100, contrast: 25, brightness: 5, sharpness: 20, highlights: -15, shadows: 10, clarity: 18, noiseReduction: 5, skinSmooth: 0, warmth: 0 },
    },
    {
      id: 'hncs_night',
      name: '哈苏夜景',
      english: 'Night',
      description: '霓虹氛围，影调丰富',
      params: { saturation: 8, contrast: 18, brightness: -3, warmth: -8, sharpness: 18, highlights: -20, shadows: 15, clarity: 20, noiseReduction: 35, skinSmooth: 0 },
    },
  ];

  const handleMasterStyle = (style: typeof hasselbladMasterStyles[0]) => {
    Object.entries(style.params).forEach(([k, v]) => {
      setAiParam(k, v as number);
    });
    setOptimized(true);
    setTimeout(() => setOptimized(false), 2000);
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">AI 场景识别</h1>
        <div className="ml-auto px-2 py-1 rounded-full bg-gradient-to-r from-[#4CAF50] to-[#2E7D32] text-[9px] font-bold text-white">
          哈苏大师
        </div>
      </div>

      <div className="flex-1 overflow-y-auto pb-4 scrollbar-hide">
        {/* 上传区域 */}
        <div className="px-4 pt-4">
          <ImageUploader
            onImageLoaded={(img) => setUserImage(img)}
            buttonText="上传照片·AI 场景识别"
            hint="上传后自动识别场景并应用哈苏大师风格"
            sampleImages={[
              { url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=200&h=200&fit=crop', label: '风景', tag: '山' },
              { url: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=200&h=200&fit=crop', label: '人像', tag: '人物' },
              { url: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200&h=200&fit=crop', label: '夜景', tag: '城市' },
              { url: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=200&fit=crop', label: '美食', tag: '食物' },
            ]}
          />
        </div>

        {/* 分析中 */}
        {isAnalyzing && (
          <div className="px-4 mt-4">
            <div className="p-4 rounded-2xl bg-gradient-to-br from-[#4CAF50]/15 to-[#2E7D32]/5 border border-[#4CAF50]/30">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-[#4CAF50]/20 flex items-center justify-center">
                  <Brain size={20} className="text-[#4CAF50] animate-pulse" />
                </div>
                <div>
                  <p className="text-white text-sm font-bold">AI 正在分析图片...</p>
                  <p className="text-white/50 text-xs">检测场景 · 评估光线 · 应用哈苏大师风格</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 识别结果 */}
        {userImage && analysis && recommendation && !isAnalyzing && (
          <>
            {/* 场景识别结果 */}
            <div className="px-4 mt-4">
              <div className="p-4 rounded-2xl bg-gradient-to-br from-[#4CAF50]/15 to-transparent border border-[#4CAF50]/30">
                <div className="flex items-center gap-2 mb-3">
                  <Crown size={18} className="text-[#4CAF50]" />
                  <h3 className="text-white text-base font-bold">哈苏大师已识别</h3>
                </div>
                
                <div className="grid grid-cols-2 gap-2 mb-3">
                  <div className="bg-black/30 rounded-lg p-2.5">
                    <p className="text-white/50 text-[10px]">识别场景</p>
                    <p className="text-[#4CAF50] text-base font-bold">{analysis.detectedScene}</p>
                  </div>
                  <div className="bg-black/30 rounded-lg p-2.5">
                    <p className="text-white/50 text-[10px]">推测时间</p>
                    <p className="text-white text-sm font-bold">
                      {analysis.detectedTime === 'night' ? '夜晚' : analysis.detectedTime === 'sunset' ? '黄昏' : '白天'}
                    </p>
                  </div>
                  <div className="bg-black/30 rounded-lg p-2.5">
                    <p className="text-white/50 text-[10px]">平均亮度</p>
                    <p className="text-white text-sm font-bold">{Math.round(analysis.brightness)}/255</p>
                  </div>
                  <div className="bg-black/30 rounded-lg p-2.5">
                    <p className="text-white/50 text-[10px]">主色调</p>
                    <div className="flex gap-0.5 mt-0.5">
                      {analysis.dominantColors.map((c, i) => (
                        <div key={i} className="w-5 h-5 rounded" style={{ backgroundColor: c }} />
                      ))}
                    </div>
                  </div>
                </div>

                <div className="p-2.5 rounded-lg bg-black/30 mb-3">
                  <p className="text-white/70 text-xs leading-relaxed">
                    <span className="text-[#4CAF50] font-medium">推荐理由：</span>{recommendation.reason}
                  </p>
                </div>

                {optimized && (
                  <div className="flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-[#4CAF50]/20 border border-[#4CAF50]/40">
                    <Check size={14} className="text-[#4CAF50]" />
                    <span className="text-white text-xs font-medium">哈苏大师风格已应用</span>
                  </div>
                )}
              </div>
            </div>

            {/* 哈苏大师模式 */}
            <div className="px-4 mt-4">
              <div className="flex items-center gap-2 mb-3">
                <Sparkles size={14} className="text-[#4CAF50]" />
                <h3 className="text-white text-sm font-bold">切换哈苏大师模式</h3>
              </div>
              <div className="grid grid-cols-2 gap-2">
                {hasselbladMasterStyles.map(style => (
                  <button
                    key={style.id}
                    onClick={() => handleMasterStyle(style)}
                    className="p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-all text-left border border-white/5"
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <Crown size={14} className="text-[#4CAF50]" />
                      <p className="text-white text-sm font-medium">{style.name}</p>
                    </div>
                    <p className="text-white/40 text-[10px] mb-1">{style.english}</p>
                    <p className="text-white/60 text-[10px] line-clamp-1">{style.description}</p>
                  </button>
                ))}
              </div>
            </div>

            {/* 当前参数总览 */}
            <div className="px-4 mt-4">
              <div className="p-3 rounded-2xl bg-white/5">
                <div className="flex items-center gap-2 mb-2">
                  <Target size={14} className="text-[#4CAF50]" />
                  <h3 className="text-white text-sm font-medium">当前优化参数</h3>
                </div>
                <div className="grid grid-cols-5 gap-1.5">
                  {[
                    { key: 'saturation', label: '饱' },
                    { key: 'contrast', label: '对' },
                    { key: 'brightness', label: '亮' },
                    { key: 'warmth', label: '暖' },
                    { key: 'sharpness', label: '锐' },
                    { key: 'highlights', label: '高' },
                    { key: 'shadows', label: '阴' },
                    { key: 'clarity', label: '清' },
                    { key: 'noiseReduction', label: '降噪' },
                    { key: 'skinSmooth', label: '美肤' },
                  ].map(p => {
                    const value = analysis && recommendation ? 
                      (p.key === 'saturation' ? recommendation.saturation :
                       p.key === 'contrast' ? recommendation.contrast :
                       p.key === 'brightness' ? recommendation.brightness :
                       p.key === 'warmth' ? recommendation.warmth :
                       p.key === 'sharpness' ? recommendation.sharpness :
                       p.key === 'highlights' ? recommendation.highlights :
                       p.key === 'shadows' ? recommendation.shadows :
                       p.key === 'clarity' ? recommendation.clarity :
                       p.key === 'noiseReduction' ? recommendation.noiseReduction :
                       recommendation.skinSmooth) : 0;
                    return (
                      <div key={p.key} className="bg-black/30 rounded p-1.5 text-center">
                        <p className={`text-[10px] font-bold ${value > 0 ? 'text-[#4CAF50]' : value < 0 ? 'text-[#2196F3]' : 'text-white/40'}`}>
                          {value > 0 ? '+' : ''}{value}
                        </p>
                        <p className="text-white/50 text-[9px]">{p.label}</p>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </>
        )}

        {/* 空状态提示 */}
        {!userImage && !isAnalyzing && (
          <div className="px-4 mt-6">
            <div className="p-4 rounded-2xl bg-gradient-to-br from-[#4CAF50]/10 to-transparent border border-[#4CAF50]/20 text-center">
              <Crown size={32} className="text-[#4CAF50] mx-auto mb-2" />
              <h3 className="text-white text-sm font-bold mb-1">哈苏大师模式</h3>
              <p className="text-white/60 text-xs leading-relaxed">
                上传照片后，AI 将自动识别场景<br/>
                并应用 6 种哈苏大师级色彩风格
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AISceneRecognitionPage;
