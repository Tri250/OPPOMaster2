import React, { useState, useEffect, useCallback } from 'react';
import { ArrowLeft, Share2, RefreshCw, Save, Sparkles, Download, CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { HasselbladCompareSlider } from '../components/HasselbladCompareSlider';
import { FilmRecommendationStrip } from '../components/FilmRecommendationStrip';
import { HasselbladParamsDisplay } from '../components/HasselbladParamsDisplay';
import { SceneProfile, HasselbladParams } from '../store/sceneProfile';
import { getFullSceneProfile } from '../ai/SceneToHasselbladMapping';
import { AnalysisResult, getAnalyzer } from '../ai/HeuristicSceneAnalyzer';

/**
 * Layer 3: 大师呈现层 - 「哈苏大师之眼」识别结果页
 *
 * 完整设计规范：
 * - 主色调：#FF6B35（哈苏橙）
 * - 背景：#0A0A0A（纯黑）
 * - 卡片：圆角16px，背景rgba(255,255,255,0.05)
 * - Before/After滑杆对比
 * - 置信度可视化
 * - 胶片推荐卡片
 * - 哈苏大师参数展示
 * - 大师拍摄建议
 * 
 * 已修复：
 * - 实现分享、保存、导出、一键优化功能
 * - Before/After对比使用实际处理效果
 * - 使用真实分析结果替代模拟数据
 */

interface SceneRecognitionResultProps {
  imageUrl?: string;
  sceneId?: string;
}

export const SceneRecognitionResult: React.FC<SceneRecognitionResultProps> = ({
  imageUrl = '/demo-photo.jpg',
  sceneId = 'landscape-sunset',
}) => {
  const navigate = useNavigate();
  const [sceneProfile, setSceneProfile] = useState<SceneProfile | null>(null);
  const [analysisResult, setAnalysisResult] = useState<AnalysisResult | null>(null);
  const [selectedFilmId, setSelectedFilmId] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(true);
  const [processedImageUrl, setProcessedImageUrl] = useState<string>(imageUrl);
  const [isOptimized, setIsOptimized] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // 真实分析过程
  useEffect(() => {
    const analyze = async () => {
      setIsAnalyzing(true);

      try {
        // 加载图片
        const img = new Image();
        img.crossOrigin = 'anonymous';
        img.src = imageUrl;
        
        await new Promise<void>((resolve, reject) => {
          img.onload = () => resolve();
          img.onerror = () => reject(new Error('图片加载失败'));
        });

        // 使用启发式分析器进行真实场景识别
        const analyzer = getAnalyzer();
        const result = await analyzer.analyze(img);
        
        setAnalysisResult(result);
        setSceneProfile(result.primaryScene);
        setSelectedFilmId(result.primaryScene.recommendedFilm[0]?.id || null);
        
        // 生成处理后的图片（应用哈苏参数）
        const processed = await applyHasselbladEffect(img, result.primaryScene.hasselbladParams);
        setProcessedImageUrl(processed);
      } catch (error) {
        console.error('分析失败:', error);
        // 回退到默认场景
        const profile = getFullSceneProfile(sceneId, 0.92);
        setSceneProfile(profile);
        setSelectedFilmId(profile.recommendedFilm[0]?.id || null);
      }

      setIsAnalyzing(false);
    };

    analyze();
  }, [imageUrl, sceneId]);

  const handleBack = () => navigate(-1);

  /**
   * 分享配方功能
   */
  const handleShare = useCallback(async () => {
    if (!sceneProfile) return;

    const shareData = {
      title: `哈苏大师配方 - ${sceneProfile.name}`,
      text: `我在哈苏之眼发现了一套绝妙的${sceneProfile.name}配方！\n\n推荐胶片：${sceneProfile.recommendedFilm.map(f => f.name).join('、')}\n\n快来试试吧！`,
      url: window.location.href,
    };

    try {
      if (navigator.share) {
        await navigator.share(shareData);
      } else {
        // 降级方案：复制到剪贴板
        const text = `${shareData.title}\n${shareData.text}\n${shareData.url}`;
        await navigator.clipboard.writeText(text);
        alert('配方已复制到剪贴板！');
      }
    } catch (error) {
      console.error('分享失败:', error);
    }
  }, [sceneProfile]);

  /**
   * 重拍功能
   */
  const handleRetake = useCallback(() => {
    // 清除当前结果，返回相机页面
    navigate('/camera');
  }, [navigate]);

  /**
   * 一键哈苏优化
   */
  const handleOptimize = useCallback(async () => {
    if (!sceneProfile || !analysisResult) return;

    setIsOptimized(true);
    
    try {
      // 重新加载原图
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.src = imageUrl;
      await new Promise<void>((resolve) => { img.onload = () => resolve(); });

      // 应用优化后的参数（增强版）
      const optimizedParams: HasselbladParams = {
        ...sceneProfile.hasselbladParams,
        clarity: Math.min((sceneProfile.hasselbladParams.clarity ?? 0) + 10, 30),
        sharpness: Math.min((sceneProfile.hasselbladParams.sharpness ?? 0) + 5, 30),
      };

      const processed = await applyHasselbladEffect(img, optimizedParams);
      setProcessedImageUrl(processed);
      
      // 3秒后重置状态
      setTimeout(() => setIsOptimized(false), 3000);
    } catch (error) {
      console.error('优化失败:', error);
      setIsOptimized(false);
    }
  }, [sceneProfile, analysisResult, imageUrl]);

  /**
   * 保存配方功能
   */
  const handleSave = useCallback(async () => {
    if (!sceneProfile) return;

    setIsSaving(true);
    
    try {
      // 保存到本地存储
      const savedRecipes = JSON.parse(localStorage.getItem('hasselblad_recipes') || '[]');
      const newRecipe = {
        id: Date.now().toString(),
        sceneId: sceneProfile.id,
        sceneName: sceneProfile.name,
        filmId: selectedFilmId,
        params: sceneProfile.hasselbladParams,
        timestamp: Date.now(),
        thumbnail: processedImageUrl,
      };
      
      savedRecipes.unshift(newRecipe);
      // 最多保存50个
      if (savedRecipes.length > 50) savedRecipes.pop();
      
      localStorage.setItem('hasselblad_recipes', JSON.stringify(savedRecipes));
      
      setSaveSuccess(true);
      setTimeout(() => {
        setSaveSuccess(false);
        setIsSaving(false);
      }, 2000);
    } catch (error) {
      console.error('保存失败:', error);
      setIsSaving(false);
    }
  }, [sceneProfile, selectedFilmId, processedImageUrl]);

  /**
   * 导出图片功能
   */
  const handleExport = useCallback(async () => {
    try {
      const link = document.createElement('a');
      link.href = processedImageUrl;
      link.download = `hasselblad_${sceneProfile?.id || 'photo'}_${Date.now()}.jpg`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (error) {
      console.error('导出失败:', error);
    }
  }, [processedImageUrl, sceneProfile]);

  if (isAnalyzing) {
    return (
      <div className="min-h-screen bg-[#0A0A0A] flex flex-col items-center justify-center">
        <div className="relative">
          <div className="w-16 h-16 rounded-full border-2 border-[#FF6B35]/20 border-t-[#FF6B35] animate-spin" />
          <Sparkles className="absolute inset-0 m-auto text-[#FF6B35]" size={24} />
        </div>
        <p className="mt-6 text-white/60 text-sm">哈苏大师正在分析场景...</p>
        <p className="mt-2 text-white/40 text-xs">识别颜色 · 分析光线 · 匹配胶片</p>
      </div>
    );
  }

  if (!sceneProfile || !analysisResult) {
    return (
      <div className="min-h-screen bg-[#0A0A0A] flex items-center justify-center">
        <p className="text-white/60">分析失败，请重试</p>
      </div>
    );
  }

  const confidencePercent = Math.round(analysisResult.confidence * 100);

  return (
    <div className="min-h-screen bg-[#0A0A0A] pb-24">
      {/* 顶部导航栏 */}
      <header className="sticky top-0 z-50 bg-[#0A0A0A]/90 backdrop-blur-md border-b border-white/5">
        <div className="flex items-center justify-between px-4 h-14">
          <button
            onClick={handleBack}
            className="flex items-center gap-1 text-white/80 hover:text-white transition-colors"
          >
            <ArrowLeft size={20} />
            <span className="text-sm">AI 出片</span>
          </button>
          <div className="flex items-center gap-2">
            <button
              onClick={handleExport}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
            >
              <Download size={14} className="text-white/60" />
              <span className="text-white/80 text-xs">导出</span>
            </button>
            <button
              onClick={handleShare}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
            >
              <Share2 size={14} className="text-[#FF6B35]" />
              <span className="text-white/80 text-xs">分享配方</span>
            </button>
          </div>
        </div>
      </header>

      <div className="px-4 py-4 space-y-4">
        {/* Before/After 对比 - 使用实际处理效果 */}
        <section>
          <HasselbladCompareSlider
            original={imageUrl}
            processed={processedImageUrl}
            aspectRatio="4/3"
          />
        </section>

        {/* 哈苏大师识别结果 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-3">
            <Sparkles size={14} className="text-[#FF6B35]" />
            <span className="text-white/60 text-xs font-medium">哈苏大师识别</span>
          </div>

          {/* 主场景 */}
          <div className="mb-4">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-2xl">{getSceneEmoji(sceneProfile.id)}</span>
              <span className="text-white text-lg font-semibold">{sceneProfile.name}</span>
              <span className="text-[#FF6B35] text-sm font-medium">· 置信度 {confidencePercent}%</span>
            </div>

            {/* 置信度条 */}
            <div className="relative h-2 bg-white/10 rounded-full overflow-hidden">
              <div
                className="absolute inset-y-0 left-0 bg-gradient-to-r from-[#FF6B35] to-[#FF8A50] rounded-full transition-all duration-1000"
                style={{ width: `${confidencePercent}%` }}
              />
            </div>

            <p className="mt-2 text-white/50 text-xs">HNCS 自然色彩已优化</p>
          </div>

          {/* 备选场景 */}
          {analysisResult.alternativeScenes.length > 0 && (
            <div className="space-y-2 pt-3 border-t border-white/5">
              <p className="text-white/40 text-xs">备选场景：</p>
              {analysisResult.alternativeScenes.slice(0, 3).map((scene) => (
                <div key={scene.id} className="flex items-center gap-3">
                  <span className="text-white/60 text-xs w-20">{scene.name}</span>
                  <div className="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-white/20 rounded-full"
                      style={{ width: `${(scene.confidence || 0) * 100}%` }}
                    />
                  </div>
                  <span className="text-white/40 text-xs w-10 text-right">
                    {Math.round((scene.confidence || 0) * 100)}%
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* 推荐胶片 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <FilmRecommendationStrip
            films={sceneProfile.recommendedFilm}
            selectedId={selectedFilmId}
            onSelect={setSelectedFilmId}
          />
        </section>

        {/* 哈苏大师参数 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <HasselbladParamsDisplay
            params={sceneProfile.hasselbladParams}
            editable={false}
          />
        </section>

        {/* 大师建议 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-3">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="text-[#FF6B35]">
              <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span className="text-white/60 text-xs font-medium">大师建议</span>
          </div>
          <div className="space-y-2.5">
            {sceneProfile.masterTips.map((tip, index) => (
              <div key={index} className="flex items-start gap-2">
                <span className="text-white/80 text-sm leading-relaxed">{tip}</span>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* 底部操作栏 */}
      <footer className="fixed bottom-0 left-0 right-0 bg-[#0A0A0A]/95 backdrop-blur-md border-t border-white/5">
        <div className="flex items-center justify-between px-4 py-3 max-w-lg mx-auto">
          <button
            onClick={handleRetake}
            className="flex flex-col items-center gap-1 px-4"
          >
            <RefreshCw size={20} className="text-white/60" />
            <span className="text-white/60 text-[10px]">重拍</span>
          </button>

          <button
            onClick={handleOptimize}
            disabled={isOptimized}
            className={`flex items-center gap-2 px-6 py-2.5 rounded-full shadow-lg transition-all ${
              isOptimized
                ? 'bg-green-500 shadow-green-500/20'
                : 'bg-gradient-to-r from-[#FF6B35] to-[#FF8A50] shadow-[#FF6B35]/20'
            }`}
          >
            {isOptimized ? (
              <>
                <CheckCircle size={18} className="text-white" />
                <span className="text-white font-medium text-sm">已优化</span>
              </>
            ) : (
              <>
                <Sparkles size={18} className="text-white" />
                <span className="text-white font-medium text-sm">一键哈苏优化</span>
              </>
            )}
          </button>

          <button
            onClick={handleSave}
            disabled={isSaving}
            className="flex flex-col items-center gap-1 px-4"
          >
            {saveSuccess ? (
              <CheckCircle size={20} className="text-green-500" />
            ) : (
              <Save size={20} className={isSaving ? 'text-white/30' : 'text-white/60'} />
            )}
            <span className={`text-[10px] ${saveSuccess ? 'text-green-500' : 'text-white/60'}`}>
              {saveSuccess ? '已保存' : '保存配方'}
            </span>
          </button>
        </div>
      </footer>
    </div>
  );
};

/**
 * 应用哈苏效果到图片
 * 使用Canvas进行图像处理
 */
async function applyHasselbladEffect(
  image: HTMLImageElement,
  params: HasselbladParams
): Promise<string> {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d')!;
  
  canvas.width = image.naturalWidth || image.width;
  canvas.height = image.naturalHeight || image.height;
  
  // 绘制原图
  ctx.drawImage(image, 0, 0);
  
  // 获取图像数据
  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const data = imageData.data;
  
  // 应用哈苏参数调整
  for (let i = 0; i < data.length; i += 4) {
    let r = data[i];
    let g = data[i + 1];
    let b = data[i + 2];
    
    // 转换为灰度（用于影调调整）
    const gray = 0.2126 * r + 0.7152 * g + 0.0722 * b;
    
    // 影调调整 (tone: -30 ~ +30)
    const toneFactor = 1 + (params.tone / 100);
    r = Math.min(255, Math.max(0, r * toneFactor));
    g = Math.min(255, Math.max(0, g * toneFactor));
    b = Math.min(255, Math.max(0, b * toneFactor));
    
    // 饱和度调整 (saturation: -30 ~ +30)
    const saturationFactor = 1 + (params.saturation / 100);
    r = gray + (r - gray) * saturationFactor;
    g = gray + (g - gray) * saturationFactor;
    b = gray + (b - gray) * saturationFactor;
    
    // 对比度调整 (contrast: -30 ~ +30)
    const contrastFactor = (259 * (params.contrast + 255)) / (255 * (259 - params.contrast));
    r = contrastFactor * (r - 128) + 128;
    g = contrastFactor * (g - 128) + 128;
    b = contrastFactor * (b - 128) + 128;
    
    // 色温调整 (colorTemp: -30 ~ +30)
    const tempFactor = params.colorTemp / 2;
    r += tempFactor;  // 暖调增加红色
    b -= tempFactor;  // 暖调减少蓝色
    
    // 锐度调整（简化版：增强边缘对比）
    if (params.sharpness > 0) {
      const sharpnessBoost = params.sharpness / 100;
      // 简单的锐化效果
      const avg = (r + g + b) / 3;
      r = r + (r - avg) * sharpnessBoost;
      g = g + (g - avg) * sharpnessBoost;
      b = b + (b - avg) * sharpnessBoost;
    }
    
    // 青品调调整 (cyanMagenta: -30 ~ +30)
    const cmFactor = params.cyanMagenta / 2;
    g += cmFactor;  // 品红增加绿色
    b -= cmFactor;  // 青色增加蓝色
    
    // 确保值在有效范围内
    data[i] = Math.min(255, Math.max(0, r));
    data[i + 1] = Math.min(255, Math.max(0, g));
    data[i + 2] = Math.min(255, Math.max(0, b));
  }
  
  // 应用暗角效果 (vignette: -30 ~ +30)
  if (params.vignette > 0) {
    applyVignette(data, canvas.width, canvas.height, params.vignette);
  }
  
  // 将处理后的数据写回canvas
  ctx.putImageData(imageData, 0, 0);
  
  return canvas.toDataURL('image/jpeg', 0.95);
}

/**
 * 应用暗角效果
 */
function applyVignette(
  data: Uint8ClampedArray,
  width: number,
  height: number,
  vignetteStrength: number
): void {
  const centerX = width / 2;
  const centerY = height / 2;
  const maxDistance = Math.sqrt(centerX * centerX + centerY * centerY);
  const strength = vignetteStrength / 30; // 归一化到 0-1
  
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const dx = x - centerX;
      const dy = y - centerY;
      const distance = Math.sqrt(dx * dx + dy * dy);
      const factor = 1 - (distance / maxDistance) * strength * 0.5;
      
      const idx = (y * width + x) * 4;
      data[idx] *= factor;
      data[idx + 1] *= factor;
      data[idx + 2] *= factor;
    }
  }
}

/**
 * 根据场景ID获取对应的emoji
 */
function getSceneEmoji(sceneId: string): string {
  if (sceneId.includes('portrait')) return '👤';
  if (sceneId.includes('landscape')) return '🏔️';
  if (sceneId.includes('night')) return '🌃';
  if (sceneId.includes('food')) return '🍜';
  if (sceneId.includes('urban')) return '🏢';
  if (sceneId.includes('still')) return '🍃';
  if (sceneId.includes('macro')) return '🔍';
  if (sceneId.includes('event')) return '🎉';
  if (sceneId.includes('sunset')) return '🌅';
  return '📷';
}

export default SceneRecognitionResult;
