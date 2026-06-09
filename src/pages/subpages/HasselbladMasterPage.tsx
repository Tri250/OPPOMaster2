import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Camera, Zap, Check, Target, Wand2, Layers,
  Sun, Moon, Mountain, Users, Utensils, Building,
  Flower, Waves, Sparkles, Leaf, Coffee, Eye,
  Snowflake, Droplets, ChevronRight, X, Image,
  Download, Share2, RefreshCw, Aperture, Grid3X3,
  Timer, Settings, ChevronUp, ChevronDown,
  Plus, Minus, Sliders, CheckCircle, AlertCircle,
  Flashlight, FlashlightOff, Circle, Film,
  Heart, Bookmark, Copy, QrCode
} from 'lucide-react';
import {
  SCENE_PROFILES,
  FILM_PRESETS,
  SceneCategory,
  SceneCategoryInfo,
  HasselbladParams,
  SoftLightMode,
  SoftLightModeInfo,
  FilmSeriesInfo,
  SceneProfile,
  FilmPreset,
  HASSELBLAD_ORANGE,
  AnalysisResult,
  ColorProfile,
  BrightnessLevel,
  RecipeProfile
} from '../../lib/hasselbladModels';

// 哈苏橙主题色
const HASSELBLAD_ORANGE_COLOR = '#FF6B35';

// 简化场景列表（用于快速选择）
const QUICK_SCENES = SCENE_PROFILES.slice(0, 20);

const HasselbladMasterPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  // 状态
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [analysisResult, setAnalysisResult] = useState<AnalysisResult | null>(null);
  const [selectedFilm, setSelectedFilm] = useState<FilmPreset | null>(null);
  const [showRecipeDialog, setShowRecipeDialog] = useState(false);
  const [comparePosition, setComparePosition] = useState(50);
  const [flashMode, setFlashMode] = useState<'off' | 'on' | 'auto'>('off');
  const [cameraFacing, setCameraFacing] = useState<'environment' | 'user'>('environment');

  // 启动相机
  const startCamera = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: cameraFacing,
          width: { ideal: 1920 },
          height: { ideal: 1080 }
        }
      });
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        setIsCameraActive(true);
      }
    } catch (err) {
      console.error('相机启动失败:', err);
      alert('无法访问相机，请检查权限设置');
    }
  }, [cameraFacing]);

  // 停止相机
  const stopCamera = useCallback(() => {
    if (videoRef.current && videoRef.current.srcObject) {
      const tracks = (videoRef.current.srcObject as MediaStream).getTracks();
      tracks.forEach(track => track.stop());
      setIsCameraActive(false);
    }
  }, []);

  // 切换相机
  const toggleCamera = useCallback(() => {
    stopCamera();
    setCameraFacing(prev => prev === 'environment' ? 'user' : 'environment');
  }, [stopCamera]);

  // 模拟AI分析
  const simulateAnalysis = useCallback(() => {
    // 随机选择场景
    const randomScene = SCENE_PROFILES[Math.floor(Math.random() * SCENE_PROFILES.length)];
    const confidence = 0.75 + Math.random() * 0.24;

    // 模拟颜色分析
    const colorProfile: ColorProfile = {
      avgRed: 150 + Math.floor(Math.random() * 50),
      avgGreen: 120 + Math.floor(Math.random() * 40),
      avgBlue: 100 + Math.floor(Math.random() * 60),
      warmthRatio: 0.3 + Math.random() * 0.4,
      coolRatio: 0.1 + Math.random() * 0.3,
      greenDominance: 0.8 + Math.random() * 0.4,
      blueDominance: 0.6 + Math.random() * 0.4,
      redDominance: 1.0 + Math.random() * 0.5,
      colorVariance: 0.2 + Math.random() * 0.3,
      dominantTone: Math.random() > 0.5 ? 'warm' : 'cool'
    };

    // 模拟备选场景
    const alternatives = SCENE_PROFILES
      .filter(p => p.category === randomScene.category && p.id !== randomScene.id)
      .slice(0, 2)
      .map(p => ({ ...p, confidence: 0.1 + Math.random() * 0.2 }));

    return {
      primaryScene: { ...randomScene, confidence },
      confidence,
      alternativeScenes: alternatives,
      colorProfile,
      brightnessLevel: 'normal' as BrightnessLevel,
      faceCount: Math.floor(Math.random() * 3),
      analysisTimeMs: 150 + Math.floor(Math.random() * 100)
    };
  }, []);

  // 拍照并分析
  const captureAndAnalyze = useCallback(() => {
    if (!videoRef.current || !canvasRef.current) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');

    if (!ctx) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    ctx.drawImage(video, 0, 0);

    const imageData = canvas.toDataURL('image/jpeg');
    setCapturedImage(imageData);

    setIsAnalyzing(true);

    setTimeout(() => {
      const result = simulateAnalysis();
      setAnalysisResult(result);
      setSelectedFilm(result.primaryScene.recommendedFilm[0]);
      setIsAnalyzing(false);
      stopCamera();
    }, 2000);
  }, [stopCamera, simulateAnalysis]);

  // 重新拍摄
  const retake = useCallback(() => {
    setCapturedImage(null);
    setAnalysisResult(null);
    setSelectedFilm(null);
    startCamera();
  }, [startCamera]);

  // 导出图片
  const exportImage = useCallback(() => {
    if (!capturedImage) return;

    const link = document.createElement('a');
    link.href = capturedImage;
    link.download = `OMaster_Hasselblad_${analysisResult?.primaryScene.name || 'photo'}_${Date.now()}.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [capturedImage, analysisResult]);

  // 切换闪光灯
  const toggleFlash = useCallback(() => {
    setFlashMode(prev => {
      if (prev === 'off') return 'on';
      if (prev === 'on') return 'auto';
      return 'off';
    });
  }, []);

  // 清理
  useEffect(() => {
    return () => stopCamera();
  }, [stopCamera]);

  // 渲染相机模式
  const renderCameraMode = () => (
    <div className="relative h-full bg-black flex flex-col">
      <div className="flex-1 relative">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="w-full h-full object-cover"
        />
        <canvas ref={canvasRef} className="hidden" />

        {/* 顶部控制 */}
        <div className="absolute top-0 left-0 right-0 p-4 flex justify-between items-start">
          <button
            onClick={() => setCurrentSubPage(null)}
            className="w-10 h-10 rounded-full bg-black/50 flex items-center justify-center text-white"
          >
            <ArrowLeft size={20} />
          </button>

          <div className="flex gap-2">
            <button
              onClick={toggleFlash}
              className="w-10 h-10 rounded-full bg-black/50 flex items-center justify-center text-white"
            >
              {flashMode === 'off' && <FlashlightOff size={20} />}
              {flashMode === 'on' && <Flashlight size={20} className="text-yellow-400" />}
              {flashMode === 'auto' && <Circle size={20} />}
            </button>
            <button
              onClick={toggleCamera}
              className="w-10 h-10 rounded-full bg-black/50 flex items-center justify-center text-white"
            >
              <RefreshCw size={20} />
            </button>
          </div>
        </div>

        {/* 哈苏大师提示 */}
        <div className="absolute bottom-32 left-4 right-4">
          <div className="bg-black/60 backdrop-blur-sm rounded-2xl p-4 border border-orange-500/30">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center">
                <Aperture size={20} className="text-white" />
              </div>
              <div>
                <h3 className="text-white font-bold">哈苏大师之眼</h3>
                <p className="text-orange-200 text-xs">50+场景 · 9款胶片 · 大师参数</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs text-white/80">
              <div className="flex items-center gap-2">
                <CheckCircle size={14} className="text-orange-400" />
                <span>混合推理 TFLite+启发式</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle size={14} className="text-orange-400" />
                <span>HNCS 自然色彩优化</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 拍摄按钮 */}
      <div className="h-32 bg-black/80 backdrop-blur-lg px-6 flex items-center justify-between">
        <div className="w-14" />
        <button
          onClick={captureAndAnalyze}
          className="w-20 h-20 rounded-full bg-white border-8 border-orange-500 hover:scale-95 transition-transform flex items-center justify-center"
        >
          <div className="w-14 h-14 rounded-full bg-gradient-to-br from-orange-500 to-orange-600" />
        </button>
        <button className="w-14 h-14 rounded-xl bg-white/10 flex items-center justify-center text-white">
          <Image size={24} />
        </button>
      </div>
    </div>
  );

  // 渲染分析中状态
  const renderAnalyzingMode = () => (
    <div className="h-full bg-[#0a0a0a] flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-xs">
        {capturedImage && (
          <div className="relative mb-8 rounded-2xl overflow-hidden">
            <img src={capturedImage} alt="Captured" className="w-full aspect-[4/3] object-cover" />
            <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
              <div className="text-center">
                <div className="w-16 h-16 mx-auto mb-4 border-4 border-orange-500 border-t-transparent rounded-full animate-spin" />
                <p className="text-white font-medium">哈苏大师分析中...</p>
                <p className="text-orange-300 text-xs mt-1">混合推理策略</p>
              </div>
            </div>
          </div>
        )}

        {/* 分析进度 */}
        <div className="space-y-4">
          <div className="bg-white/10 rounded-xl p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-8 h-8 rounded-lg bg-orange-500/20 flex items-center justify-center">
                <Eye size={16} className="text-orange-400" />
              </div>
              <span className="text-white text-sm">场景识别</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-orange-500 to-orange-400 rounded-full animate-pulse w-full" />
            </div>
          </div>

          <div className="bg-white/10 rounded-xl p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-8 h-8 rounded-lg bg-orange-500/20 flex items-center justify-center">
                <Film size={16} className="text-orange-400" />
              </div>
              <span className="text-white text-sm">胶片匹配</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-orange-500 to-orange-400 rounded-full animate-pulse" style={{ width: '0%', animation: 'progress 2s ease-out forwards', animationDelay: '1s' }} />
            </div>
          </div>

          <div className="bg-white/10 rounded-xl p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-8 h-8 rounded-lg bg-orange-500/20 flex items-center justify-center">
                <Sliders size={16} className="text-orange-400" />
              </div>
              <span className="text-white text-sm">哈苏参数优化</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-orange-500 to-orange-400 rounded-full animate-pulse" style={{ width: '0%', animation: 'progress 2s ease-out forwards', animationDelay: '2s' }} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // 渲染结果模式（哈苏大师呈现层）
  const renderResultMode = () => (
    <div className="h-full bg-[#0a0a0a] flex flex-col overflow-y-auto">
      {/* 顶部栏 */}
      <div className="sticky top-0 z-10 h-14 px-4 flex items-center justify-between bg-[#0a0a0a]/95 backdrop-blur-lg border-b border-white/5">
        <button
          onClick={() => {
            setCapturedImage(null);
            setAnalysisResult(null);
          }}
          className="text-white/70 hover:text-white"
        >
          <ArrowLeft size={20} />
        </button>
        <span className="text-white font-medium">AI 出片</span>
        <button onClick={() => setShowRecipeDialog(true)} className="text-orange-500 font-medium text-sm flex items-center gap-1">
          <Share2 size={16} />
          分享配方
        </button>
      </div>

      <div className="flex-1 p-4 space-y-4">
        {/* Before/After 对比滑杆 */}
        <div className="relative rounded-2xl overflow-hidden bg-black">
          <img src={capturedImage!} alt="Result" className="w-full aspect-[4/3] object-cover" />

          {/* 滑杆控制 */}
          <div className="absolute inset-0 flex items-center">
            <div
              className="absolute top-0 bottom-0 w-1 bg-white shadow-[0_0_12px_rgba(255,107,53,0.5)]"
              style={{ left: `${comparePosition}%` }}
            >
              <div className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-8 h-8 rounded-full bg-white shadow-lg border-2 border-orange-500 flex items-center justify-center">
                <span className="text-orange-500 text-xs">◀▶</span>
              </div>
            </div>
          </div>

          {/* 标签 */}
          <div className="absolute top-3 left-3 px-3 py-1.5 rounded-full bg-black/60 text-white text-xs font-medium">
            Before
          </div>
          <div className="absolute top-3 right-3 px-3 py-1.5 rounded-full bg-orange-500/80 text-white text-xs font-medium">
            After
          </div>

          {/* HNCS水印 */}
          <div className="absolute bottom-3 right-3 px-2 py-1 rounded bg-black/60 text-white/60 text-xs">
            HNCS · OMaster
          </div>
        </div>

        {/* 哈苏大师识别结果卡片 */}
        {analysisResult && (
          <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-orange-500/20 to-orange-600/20 flex items-center justify-center">
                <span className="text-2xl">{SceneCategoryInfo[analysisResult.primaryScene.category].icon}</span>
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <h3 className="text-white font-bold">{analysisResult.primaryScene.name}</h3>
                  <span className="text-orange-500 text-sm">置信度 {(analysisResult.confidence * 100).toFixed(0)}%</span>
                </div>
                <p className="text-orange-400 text-xs">HNCS 自然色彩已优化</p>
              </div>
            </div>

            {/* 置信度进度条 */}
            <div className="h-2 bg-white/10 rounded-full overflow-hidden mb-3">
              <div
                className="h-full bg-gradient-to-r from-orange-400 to-orange-500 rounded-full"
                style={{ width: `${analysisResult.confidence * 100}%` }}
              />
            </div>

            {/* 备选场景 */}
            {analysisResult.alternativeScenes.length > 0 && (
              <div className="mt-3">
                <p className="text-white/60 text-xs mb-2">备选场景：</p>
                {analysisResult.alternativeScenes.map((alt, i) => (
                  <div key={i} className="flex items-center justify-between py-1">
                    <span className="text-white/50 text-xs">{alt.name}</span>
                    <div className="flex items-center gap-2">
                      <span className="text-white/40 text-xs">{(alt.confidence * 100).toFixed(0)}%</span>
                      <div className="w-20 h-1.5 bg-white/10 rounded-full overflow-hidden">
                        <div className="h-full bg-white/30 rounded-full" style={{ width: `${alt.confidence * 100}%` }} />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 推荐胶片卡片 */}
        {analysisResult && (
          <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
            <div className="flex items-center gap-2 mb-3">
              <Film size={16} className="text-orange-500" />
              <h3 className="text-white font-medium text-sm">推荐胶片</h3>
            </div>

            <div className="flex gap-3 overflow-x-auto pb-2">
              {analysisResult.primaryScene.recommendedFilm.map((film, i) => (
                <button
                  key={film.id}
                  onClick={() => setSelectedFilm(film)}
                  className={`flex-shrink-0 w-24 p-3 rounded-xl border-2 transition-all ${
                    selectedFilm?.id === film.id
                      ? 'border-orange-500 bg-orange-500/10'
                      : 'border-white/10 bg-white/5'
                  }`}
                >
                  <div className="text-center">
                    <p className={`text-sm font-medium ${selectedFilm?.id === film.id ? 'text-orange-400' : 'text-white'}`}>
                      {film.displayName}
                    </p>
                    <p className="text-xs text-white/50 mt-1">{film.matchScore * 100}%匹配</p>
                    <div className="h-1.5 bg-white/10 rounded-full overflow-hidden mt-2">
                      <div
                        className="h-full rounded-full"
                        style={{
                          width: `${film.matchScore * 100}%`,
                          backgroundColor: FilmSeriesInfo[film.series].color
                        }}
                      />
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* 哈苏大师参数 */}
        {analysisResult && (
          <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
            <div className="flex items-center gap-2 mb-3">
              <Sliders size={16} className="text-orange-500" />
              <h3 className="text-white font-medium text-sm">哈苏大师参数</h3>
            </div>

            <div className="space-y-3">
              {Object.entries(analysisResult.primaryScene.hasselbladParams)
                .filter(([key]) => key !== 'softLight')
                .map(([key, value]) => {
                  const labelMap: Record<string, string> = {
                    tone: '影调',
                    saturation: '饱和度',
                    contrast: '对比度',
                    colorTemp: '色温',
                    sharpness: '锐度',
                    vignette: '暗角',
                    cyanMagenta: '青品调'
                  };
                  const numValue = value as number;
                  if (numValue === 0) return null;

                  return (
                    <div key={key} className="flex items-center justify-between">
                      <span className="text-white/60 text-xs">{labelMap[key]}</span>
                      <div className="flex items-center gap-2">
                        <span className={`text-sm font-medium ${numValue > 0 ? 'text-orange-400' : 'text-white'}`}>
                          {numValue > 0 ? `+${numValue}` : numValue}
                        </span>
                        <div className="w-24 h-1.5 bg-white/10 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-orange-500 rounded-full"
                            style={{ width: `${Math.abs(numValue) / 30 * 100}%` }}
                          />
                        </div>
                      </div>
                    </div>
                  );
                })}

              {/* 柔光模式 */}
              <div className="flex items-center justify-between">
                <span className="text-white/60 text-xs">柔光</span>
                <div className="flex gap-2">
                  {Object.entries(SoftLightModeInfo).map(([mode, info]) => (
                    <button
                      key={mode}
                      className={`px-3 py-1 rounded-lg text-xs ${
                        analysisResult.primaryScene.hasselbladParams.softLight === mode
                          ? 'bg-orange-500/20 text-orange-400 border border-orange-500/50'
                          : 'bg-white/10 text-white/50'
                      }`}
                    >
                      {info.displayName}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 大师建议 */}
        {analysisResult && analysisResult.primaryScene.masterTips.length > 0 && (
          <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
            <div className="flex items-center gap-2 mb-3">
              <Sparkles size={16} className="text-orange-500" />
              <h3 className="text-white font-medium text-sm">大师建议</h3>
            </div>

            <div className="space-y-2">
              {analysisResult.primaryScene.masterTips.map((tip, i) => (
                <div key={i} className="flex items-start gap-2 p-2 bg-white/5 rounded-lg">
                  <span className="text-orange-400">💡</span>
                  <p className="text-white/70 text-xs">{tip}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* HNCS水印 */}
        <div className="text-center py-4">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10">
            <Aperture size={14} className="text-orange-500" />
            <span className="text-white/50 text-xs">HNCS · OMaster · 哈苏自然色彩认证</span>
          </div>
        </div>
      </div>

      {/* 底部操作 */}
      <div className="sticky bottom-0 z-10 p-4 bg-[#0a0a0a]/95 backdrop-blur-lg border-t border-white/5">
        <div className="flex gap-3">
          <button
            onClick={retake}
            className="flex-1 py-3 px-4 bg-white/10 text-white rounded-xl font-medium flex items-center justify-center gap-2"
          >
            <Camera size={18} />
            重拍
          </button>
          <button
            onClick={() => {}}
            className="flex-1 py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-xl font-medium flex items-center justify-center gap-2"
          >
            <Wand2 size={18} />
            一键哈苏优化
          </button>
          <button
            onClick={() => setShowRecipeDialog(true)}
            className="py-3 px-4 bg-white/10 text-orange-500 rounded-xl font-medium flex items-center justify-center gap-2 border border-orange-500/30"
          >
            <Bookmark size={18} />
            保存配方
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="h-full w-full bg-[#0a0a0a]">
      {!capturedImage && !isAnalyzing && renderCameraMode()}
      {isAnalyzing && renderAnalyzingMode()}
      {capturedImage && !isAnalyzing && analysisResult && renderResultMode()}

      {/* 配方分享弹窗 */}
      {showRecipeDialog && analysisResult && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm">
          <div className="w-[90%] max-w-sm bg-[#0a0a0a] rounded-2xl p-5 border border-white/10">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-white font-bold">保存配方</h3>
              <button onClick={() => setShowRecipeDialog(false)} className="text-white/50">
                <X size={20} />
              </button>
            </div>

            <div className="space-y-4">
              <div className="bg-white/5 rounded-xl p-3">
                <p className="text-white text-sm font-medium">{analysisResult.primaryScene.name} - {selectedFilm?.displayName}</p>
                <p className="text-white/50 text-xs mt-1">{analysisResult.primaryScene.description}</p>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={() => setShowRecipeDialog(false)}
                  className="flex-1 py-3 bg-white/10 text-white rounded-xl"
                >
                  取消
                </button>
                <button
                  onClick={() => {
                    setShowRecipeDialog(false);
                    // 保存配方逻辑
                  }}
                  className="flex-1 py-3 bg-orange-500 text-white rounded-xl flex items-center justify-center gap-2"
                >
                  <Check size={18} />
                  保存
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 动画样式 */}
      <style>{`
        @keyframes progress {
          from { width: 0%; }
          to { width: 100%; }
        }
      `}</style>
    </div>
  );
};

export default HasselbladMasterPage;