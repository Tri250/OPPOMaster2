import React, { useState, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Cpu, Wand2, Check, RefreshCw, Zap, Sun, Droplets, Focus, Crown, Sparkles, Eye, Brain } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { userImageStore, UserImage } from '../../store/userImageStore';
import { imageAnalysisService, ImageAnalysis, RecommendedParams } from '../../services/imageAnalysisService';

const SmartOptimizePage: React.FC = () => {
  const { aiParams, setAiParam, goBack } = useAppStore();
  const [userImage, setUserImage] = useState<UserImage | null>(null);
  const [analysis, setAnalysis] = useState<ImageAnalysis | null>(null);
  const [recommendation, setRecommendation] = useState<RecommendedParams | null>(null);
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [optimized, setOptimized] = useState(false);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const unsubscribe = userImageStore.subscribe(image => {
      setUserImage(image);
      if (image) {
        analyzeAndRecommend(image);
      } else {
        setAnalysis(null);
        setRecommendation(null);
        setOptimized(false);
      }
    });
    return unsubscribe;
  }, []);

  const analyzeAndRecommend = async (image: UserImage) => {
    setIsOptimizing(true);
    setProgress(0);
    setOptimized(false);
    
    // 模拟进度
    const progressInterval = setInterval(() => {
      setProgress(prev => Math.min(prev + 10, 90));
    }, 100);
    
    try {
      const result = await imageAnalysisService.analyze(image);
      setAnalysis(result);
      const rec = imageAnalysisService.recommendHasselbladParams(result);
      setRecommendation(rec);
      
      setProgress(100);
      clearInterval(progressInterval);
      
      // 自动应用
      setTimeout(() => {
        applyRecommendation(rec);
      }, 500);
    } catch (e) {
      console.error(e);
      clearInterval(progressInterval);
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
    setIsOptimizing(false);
    setOptimized(true);
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">智能优化</h1>
        <div className="ml-auto px-2 py-1 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
          哈苏大师
        </div>
      </div>

      <div className="flex-1 overflow-y-auto pb-4 scrollbar-hide">
        {/* 上传区域 */}
        <div className="px-4 pt-4">
          <ImageUploader
            onImageLoaded={(img) => setUserImage(img)}
            buttonText="上传照片·哈苏大师出片"
            hint="上传后 AI 将智能分析并优化至哈苏大师级效果"
            sampleImages={[
              { url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=200&h=200&fit=crop', label: '风景', tag: '山' },
              { url: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=200&h=200&fit=crop', label: '人像', tag: '人物' },
              { url: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=200&h=200&fit=crop', label: '夜景', tag: '城市' },
              { url: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=200&fit=crop', label: '美食', tag: '食物' },
            ]}
          />
        </div>

        {/* 优化进度 */}
        {isOptimizing && (
          <div className="px-4 mt-4">
            <div className="p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-[#FF9800]/5 border border-[#FF6B35]/30">
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-full bg-[#FF6B35]/20 flex items-center justify-center">
                  <Cpu size={20} className="text-[#FF6B35] animate-pulse" />
                </div>
                <div>
                  <p className="text-white text-sm font-bold">哈苏大师出片中...</p>
                  <p className="text-white/50 text-xs">正在分析图片并应用哈苏色彩科学</p>
                </div>
              </div>
              <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] rounded-full transition-all duration-200"
                  style={{ width: `${progress}%` }}
                />
              </div>
              <p className="text-white/40 text-xs text-center mt-2">{progress}%</p>
            </div>
          </div>
        )}

        {/* 分析结果 */}
        {analysis && !isOptimizing && (
          <>
            {/* 对比预览 */}
            {userImage && (
              <div className="px-4 mt-4">
                <div className="grid grid-cols-2 gap-2">
                  <div className="relative aspect-square rounded-2xl overflow-hidden">
                    <img src={userImage.dataUrl} alt="原图" className="w-full h-full object-cover" />
                    <div className="absolute top-2 left-2 px-2 py-0.5 rounded bg-black/60 text-white text-[10px]">
                      原图
                    </div>
                  </div>
                  <div className="relative aspect-square rounded-2xl overflow-hidden ring-2 ring-[#FF6B35]">
                    <img 
                      src={userImage.dataUrl} 
                      alt="优化后" 
                      className="w-full h-full object-cover"
                      style={{
                        filter: `saturate(${100 + aiParams.saturation}%) contrast(${100 + aiParams.contrast}%) brightness(${100 + aiParams.brightness}%)`,
                      }}
                    />
                    <div className="absolute top-2 left-2 px-2 py-0.5 rounded bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-white text-[10px] flex items-center gap-1">
                      <Crown size={10} />
                      <span>哈苏</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 推荐详情 */}
            {recommendation && (
              <div className="px-4 mt-4">
                <div className="p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30">
                  <div className="flex items-center gap-2 mb-3">
                    <Crown size={18} className="text-[#FF6B35]" />
                    <h3 className="text-white text-base font-bold">{recommendation.style}</h3>
                  </div>
                  
                  <p className="text-white/70 text-sm mb-3 leading-relaxed">
                    {recommendation.reason}
                  </p>

                  <div className="grid grid-cols-3 gap-2 mb-3">
                    {[
                      { label: '场景', value: analysis.detectedScene },
                      { label: '亮度', value: Math.round(analysis.brightness) },
                      { label: '饱和度', value: `${Math.round(analysis.saturation * 100)}%` },
                    ].map((item, i) => (
                      <div key={i} className="bg-black/30 rounded-lg p-2 text-center">
                        <p className="text-white text-sm font-bold">{item.value}</p>
                        <p className="text-white/50 text-[10px]">{item.label}</p>
                      </div>
                    ))}
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
                      const value = aiParams[p.key as keyof typeof aiParams] as number;
                      return (
                        <div key={p.key} className="bg-black/30 rounded p-1.5 text-center">
                          <p className={`text-[10px] font-bold ${value > 0 ? 'text-[#FF6B35]' : value < 0 ? 'text-[#2196F3]' : 'text-white/40'}`}>
                            {value > 0 ? '+' : ''}{value}
                          </p>
                          <p className="text-white/50 text-[9px]">{p.label}</p>
                        </div>
                      );
                    })}
                  </div>

                  {optimized && (
                    <div className="mt-3 flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-[#4CAF50]/20 border border-[#4CAF50]/40">
                      <Check size={14} className="text-[#4CAF50]" />
                      <span className="text-white text-xs font-medium">哈苏大师出片完成</span>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* 大师级选项 */}
            <div className="px-4 mt-4">
              <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
                <Sparkles size={14} className="text-[#FF6B35]" />
                <span>哈苏大师出片</span>
              </h3>
              <div className="space-y-2">
                {[
                  { name: '大师人像', desc: '保留真实肤质，柔光自然美肤', icon: '👤', params: { saturation: 8, contrast: 3, brightness: 5, warmth: 8, sharpness: 10, highlights: -8, shadows: 5, clarity: 12, noiseReduction: 10, skinSmooth: 25 } },
                  { name: '大师风景', desc: '哈苏浓郁，层次分明', icon: '🏔️', params: { saturation: 15, contrast: 12, brightness: 0, warmth: -3, sharpness: 18, highlights: -8, shadows: 5, clarity: 20, noiseReduction: 0, skinSmooth: 0 } },
                  { name: '大师夜景', desc: '暗部细节丰富，霓虹氛围', icon: '🌃', params: { saturation: 8, contrast: 18, brightness: -3, warmth: -8, sharpness: 20, highlights: -18, shadows: 15, clarity: 20, noiseReduction: 35, skinSmooth: 0 } },
                  { name: '大师街拍', desc: '胶片质感，人文纪实', icon: '🚶', params: { saturation: 5, contrast: 15, brightness: 0, warmth: 5, sharpness: 20, highlights: -10, shadows: 8, clarity: 18, noiseReduction: 0, skinSmooth: 0 } },
                ].map((master, i) => (
                  <button
                    key={i}
                    onClick={() => {
                      Object.entries(master.params).forEach(([k, v]) => {
                        setAiParam(k, v as number);
                      });
                      setOptimized(true);
                    }}
                    className="w-full p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-all flex items-center gap-3"
                  >
                    <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-[#FF6B35]/20 to-[#FF9800]/10 flex items-center justify-center text-lg">
                      {master.icon}
                    </div>
                    <div className="flex-1 text-left">
                      <p className="text-white text-sm font-medium">{master.name}</p>
                      <p className="text-white/50 text-xs">{master.desc}</p>
                    </div>
                    <Sparkles size={14} className="text-[#FF6B35]" />
                  </button>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default SmartOptimizePage;
