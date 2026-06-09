import React, { useState, useEffect } from 'react';
import { ArrowLeft, Share2, RefreshCw, Save, Sparkles } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { HasselbladCompareSlider } from '../components/HasselbladCompareSlider';
import { FilmRecommendationStrip } from '../components/FilmRecommendationStrip';
import { HasselbladParamsDisplay } from '../components/HasselbladParamsDisplay';
import { SceneProfile, FilmPreset, HasselbladParams } from '../store/sceneProfile';
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

  // 模拟分析过程
  useEffect(() => {
    const analyze = async () => {
      setIsAnalyzing(true);

      // 模拟延迟
      await new Promise(resolve => setTimeout(resolve, 1500));

      // 获取场景画像
      const profile = getFullSceneProfile(sceneId, 0.92);
      setSceneProfile(profile);
      setSelectedFilmId(profile.recommendedFilm[0]?.id || null);

      // 模拟分析结果
      setAnalysisResult({
        primaryScene: profile,
        confidence: 0.92,
        alternativeScenes: [
          { ...profile, id: 'landscape', name: '风景', confidence: 0.18 },
          { ...profile, id: 'portrait-backlit', name: '逆光人像', confidence: 0.08 },
        ] as SceneProfile[],
        colorProfile: {
          avgRed: 180, avgGreen: 120, avgBlue: 80,
          warmthRatio: 0.65, greenDominance: 0.9, blueDominance: 0.7, redDominance: 1.2,
          skinToneRatio: 0.02, darkPixelRatio: 0.15, highlightRatio: 0.25,
        },
        brightnessLevel: 'BRIGHT' as const,
        faceCount: 0,
        edgeDensity: 0.22,
        analysisDetails: {},
      });

      setIsAnalyzing(false);
    };

    analyze();
  }, [sceneId]);

  const handleBack = () => navigate(-1);
  const handleShare = () => {
    // TODO: 实现分享功能
    console.log('分享配方');
  };
  const handleRetake = () => {
    // TODO: 实现重拍功能
    console.log('重拍');
  };
  const handleOptimize = () => {
    // TODO: 实现一键优化
    console.log('一键哈苏优化');
  };
  const handleSave = () => {
    // TODO: 实现保存配方
    console.log('保存配方');
  };

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
          <button
            onClick={handleShare}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
          >
            <Share2 size={14} className="text-[#FF6B35]" />
            <span className="text-white/80 text-xs">分享配方</span>
          </button>
        </div>
      </header>

      <div className="px-4 py-4 space-y-4">
        {/* Before/After 对比 */}
        <section>
          <HasselbladCompareSlider
            original={imageUrl}
            processed={imageUrl} // 实际应用中应该是处理后的图片
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
              {analysisResult.alternativeScenes.map((scene) => (
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
            className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF8A50] shadow-lg shadow-[#FF6B35]/20"
          >
            <Sparkles size={18} className="text-white" />
            <span className="text-white font-medium text-sm">一键哈苏优化</span>
          </button>

          <button
            onClick={handleSave}
            className="flex flex-col items-center gap-1 px-4"
          >
            <Save size={20} className="text-white/60" />
            <span className="text-white/60 text-[10px]">保存配方</span>
          </button>
        </div>
      </footer>
    </div>
  );
};

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
