import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { getRecommendedFilms, getMasterTips } from '../../ai/SceneToHasselbladMapping';
import { FilmPreset } from '../../store/sceneProfile';
import {
  ArrowLeft, Camera, Zap, Sun, Moon, Mountain, Users, Utensils, Building,
  Flower, Sparkles, Leaf, Coffee, Eye,
  Droplets, Image as ImageIcon,
  Download,
  Sliders, CheckCircle,
  Wand2,
  Circle,
  Film,
  Lightbulb,
  ThumbsUp,
  SwitchCamera
} from 'lucide-react';

// ============================================
// 场景类型定义
// ============================================
const SCENE_TYPES = [
  // 人像系列
  { id: 'portrait', name: '人像', icon: Users, color: '#FF6B9D', params: { exposure: 0.3, contrast: 10, highlights: -10, shadows: 15, saturation: 5, vibrance: 10, warmth: 3, sharpness: 15, skinSmooth: 25 } },
  { id: 'portrait-backlit', name: '逆光人像', icon: Sun, color: '#FFB347', params: { exposure: 0.8, contrast: 12, highlights: -30, shadows: 40, saturation: 10, vibrance: 15, warmth: -5 } },
  { id: 'portrait-studio', name: '棚拍人像', icon: Camera, color: '#C9A0DC', params: { exposure: 0, contrast: 15, highlights: -5, shadows: 10, saturation: 0, vibrance: 5, warmth: 0, sharpness: 30 } },
  { id: 'portrait-bw', name: '黑白人像', icon: Eye, color: '#808080', params: { exposure: 0.2, contrast: 20, saturation: -100, vibrance: 0, warmth: 0, sharpness: 25, grain: 8 } },

  // 风景系列
  { id: 'landscape', name: '风景', icon: Mountain, color: '#4ECDC4', params: { exposure: 0.2, contrast: 12, highlights: -10, shadows: 15, saturation: 18, vibrance: 15, warmth: 5, sharpness: 20, clarity: 15 } },
  { id: 'landscape-sunset', name: '日落', icon: Sun, color: '#FF7F50', params: { exposure: 0.3, contrast: 15, highlights: -15, shadows: 10, saturation: 28, vibrance: 20, warmth: 25, sharpness: 15 } },
  { id: 'landscape-blue-sky', name: '蓝天白云', icon: Mountain, color: '#87CEEB', params: { exposure: 0.1, contrast: 10, highlights: -15, shadows: 20, saturation: 15, vibrance: 12, warmth: -10, dehaze: 20 } },
  { id: 'landscape-forest', name: '森林', icon: Leaf, color: '#228B22', params: { exposure: 0.2, contrast: 12, highlights: -10, shadows: 15, saturation: 22, vibrance: 18, warmth: 5, sharpness: 22 } },
  { id: 'landscape-autumn', name: '秋景', icon: Leaf, color: '#D2691E', params: { exposure: 0.2, contrast: 15, highlights: -10, shadows: 12, saturation: 32, vibrance: 25, warmth: 18, sharpness: 20 } },

  // 夜景系列
  { id: 'night', name: '夜景', icon: Moon, color: '#483D8B', params: { exposure: 0.5, contrast: 18, highlights: -20, shadows: 30, saturation: -5, vibrance: 5, warmth: 8, noiseReduction: 30 } },
  { id: 'night-city', name: '城市夜景', icon: Building, color: '#9370DB', params: { exposure: 0.6, contrast: 22, highlights: -25, shadows: 35, saturation: 12, vibrance: 15, warmth: 12, sharpness: 20 } },
  { id: 'night-starry', name: '星空', icon: Sparkles, color: '#191970', params: { exposure: 1.0, contrast: 25, highlights: -30, shadows: 40, saturation: 15, vibrance: 20, warmth: -5, noiseReduction: 40 } },

  // 美食系列
  { id: 'food', name: '美食', icon: Utensils, color: '#FF6347', params: { exposure: 0.3, contrast: 8, highlights: -5, shadows: 10, saturation: 25, vibrance: 20, warmth: 12, sharpness: 30, brightness: 5 } },
  { id: 'food-restaurant', name: '餐厅美食', icon: Coffee, color: '#CD853F', params: { exposure: 0.2, contrast: 10, highlights: -8, shadows: 12, saturation: 18, vibrance: 15, warmth: 18, sharpness: 25, vignette: -15 } },
  { id: 'food-dessert', name: '甜点', icon: Coffee, color: '#FFB6C1', params: { exposure: 0.4, contrast: 5, highlights: -5, shadows: 8, saturation: 28, vibrance: 22, warmth: 8, sharpness: 20, brightness: 10 } },

  // 微距系列
  { id: 'macro-flower', name: '花卉', icon: Flower, color: '#FF69B4', params: { exposure: 0.2, contrast: 15, highlights: -10, shadows: 12, saturation: 30, vibrance: 25, warmth: 5, sharpness: 35, clarity: 20 } },
  { id: 'macro-detail', name: '细节特写', icon: Droplets, color: '#00CED1', params: { exposure: 0.1, contrast: 18, highlights: -15, shadows: 15, saturation: 12, vibrance: 10, warmth: 0, sharpness: 40, clarity: 25 } },

  // 运动系列
  { id: 'sports', name: '运动', icon: Zap, color: '#FF4500', params: { exposure: 0.5, contrast: 20, highlights: -5, shadows: 15, saturation: 8, vibrance: 10, warmth: 0, sharpness: 35, motionBlur: -20 } },
  { id: 'action', name: '动作', icon: Zap, color: '#FFD700', params: { exposure: 0.4, contrast: 22, highlights: -8, shadows: 12, saturation: 5, vibrance: 8, warmth: 0, sharpness: 40, motionBlur: -30 } },
];

// ============================================
// CSS Filter 构建器 - 确保效果可见
// ============================================
const buildSceneFilter = (params: Record<string, number>): string => {
  const parts: string[] = [];
  const saturation = params.saturation ?? 0;
  const contrast = params.contrast ?? 0;
  const brightness = (params.exposure ?? 0) * 30 + (params.brightness ?? 0) * 0.5;
  const warmth = params.warmth ?? 0;
  const vibrance = params.vibrance ?? 0;
  const sharpness = params.sharpness ?? 0;

  // 饱和度（叠加vibrance）
  parts.push(`saturate(${100 + saturation + vibrance * 0.5}%)`);
  // 对比度（叠加sharpness模拟）
  parts.push(`contrast(${100 + contrast + sharpness * 0.2}%)`);
  // 亮度
  if (Math.abs(brightness) > 1) parts.push(`brightness(${100 + brightness}%)`);
  // 暖色
  if (warmth > 0) parts.push(`sepia(${warmth * 0.5}%)`);
  if (warmth < 0) parts.push(`hue-rotate(${warmth * 0.5}deg)`);

  return parts.join(' ') || 'none';
};

// ============================================
// 主组件
// ============================================
const AISceneRecognitionPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  // ---- 状态 ----
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [recognizedScene, setRecognizedScene] = useState<typeof SCENE_TYPES[0] | null>(null);
  const [appliedParams, setAppliedParams] = useState<Record<string, number>>({});
  const [showParams, setShowParams] = useState(false);
  const [flashMode, setFlashMode] = useState<'off' | 'on' | 'auto'>('off');
  const [cameraFacing, setCameraFacing] = useState<'environment' | 'user'>('environment');
  const [recognitionHistory, setRecognitionHistory] = useState<typeof SCENE_TYPES[0][]>([]);
  const [filmRecommendations, setFilmRecommendations] = useState<FilmPreset[]>([]);
  const [masterTips, setMasterTips] = useState<string[]>([]);
  const [filterEffectEnabled, setFilterEffectEnabled] = useState(true);
  const [cameraReady, setCameraReady] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);

  // ---- 启动相机 ----
  const startCamera = useCallback(async () => {
    // 先停止旧流
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }
    setCameraReady(false);
    setCameraError(null);

    try {
      const constraints: MediaStreamConstraints = {
        video: {
          facingMode: cameraFacing,
          width: { ideal: 1920 },
          height: { ideal: 1080 }
        },
        audio: false
      };

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.onloadedmetadata = () => {
          videoRef.current?.play().then(() => {
            setCameraReady(true);
          }).catch(() => {
            setCameraReady(true);
          });
        };
      }
    } catch (err) {
      console.error('相机启动失败:', err);
      setCameraError('无法访问相机，请检查权限设置');
      setCameraReady(false);
    }
  }, [cameraFacing]);

  // ---- 停止相机 ----
  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setCameraReady(false);
  }, []);

  // ---- 切换前后置 ----
  const toggleCamera = useCallback(() => {
    setCameraFacing(prev => prev === 'environment' ? 'user' : 'environment');
  }, []);

  // cameraFacing 变化时重启相机
  useEffect(() => {
    startCamera();
  }, [cameraFacing, startCamera]);

  // 组件挂载启动相机
  useEffect(() => {
    startCamera();
    return () => stopCamera();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ---- 拍照并分析 ----
  const captureAndAnalyze = useCallback(() => {
    if (!videoRef.current || !canvasRef.current || !cameraReady) {
      console.warn('相机未就绪，无法拍照');
      return;
    }

    const video = videoRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // 处理前置摄像头镜像
    const isFront = cameraFacing === 'user';
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;

    if (isFront) {
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
    }
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    if (isFront) {
      ctx.setTransform(1, 0, 0, 1, 0, 0);
    }

    const imageData = canvas.toDataURL('image/jpeg', 0.92);
    setCapturedImage(imageData);

    // 开始分析
    setIsAnalyzing(true);
    setFilmRecommendations([]);
    setMasterTips([]);
    setFilterEffectEnabled(true);
    stopCamera();

    // 模拟AI分析过程
    setTimeout(() => {
      const randomScene = SCENE_TYPES[Math.floor(Math.random() * SCENE_TYPES.length)];
      setRecognizedScene(randomScene);
      setAppliedParams(randomScene.params);

      const films = getRecommendedFilms(randomScene.id);
      const tips = getMasterTips(randomScene.id);
      setFilmRecommendations(films);
      setMasterTips(tips);
      setRecognitionHistory(prev => [randomScene, ...prev.slice(0, 4)]);

      setIsAnalyzing(false);
    }, 2000);
  }, [cameraReady, cameraFacing, stopCamera]);

  // ---- 重新拍摄 ----
  const retake = useCallback(() => {
    setCapturedImage(null);
    setRecognizedScene(null);
    setAppliedParams({});
    setShowParams(false);
    setFilmRecommendations([]);
    setMasterTips([]);
    setFilterEffectEnabled(true);
    startCamera();
  }, [startCamera]);

  // ---- 应用参数 ----
  const applyParams = useCallback(() => {
    if (recognizedScene) {
      setAppliedParams({ ...recognizedScene.params });
      setShowParams(true);
      setFilterEffectEnabled(true);
    }
  }, [recognizedScene]);

  // ---- 导出图片 ----
  const exportImage = useCallback(() => {
    if (!capturedImage) return;
    const link = document.createElement('a');
    link.href = capturedImage;
    link.download = `OMaster_${recognizedScene?.name || 'photo'}_${Date.now()}.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [capturedImage, recognizedScene]);

  // ---- 切换闪光灯 ----
  const toggleFlash = useCallback(() => {
    setFlashMode(prev => {
      if (prev === 'off') return 'on';
      if (prev === 'on') return 'auto';
      return 'off';
    });
  }, []);

  // ============================================
  // 渲染：相机模式
  // ============================================
  const renderCameraMode = () => (
    <div className="relative h-full w-full bg-black flex flex-col">
      {/* 视频预览区域 */}
      <div className="flex-1 relative">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="w-full h-full object-cover"
          style={{ transform: cameraFacing === 'user' ? 'scaleX(-1)' : 'none' }}
        />
        <canvas ref={canvasRef} className="hidden" />

        {/* 顶部控制栏 */}
        <div className="absolute top-0 left-0 right-0 p-4 flex justify-between items-start z-10">
          <button
            onClick={() => setCurrentSubPage(null)}
            className="w-10 h-10 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center text-white active:scale-90 transition-transform"
          >
            <ArrowLeft size={20} />
          </button>

          <div className="flex gap-2">
            <button
              onClick={toggleFlash}
              className="w-10 h-10 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center text-white active:scale-90 transition-transform"
            >
              {flashMode === 'off' && <Moon size={20} />}
              {flashMode === 'on' && <Sun size={20} className="text-yellow-400" />}
              {flashMode === 'auto' && <Circle size={20} />}
            </button>
            <button
              onClick={toggleCamera}
              className="w-10 h-10 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center text-white active:scale-90 transition-transform"
            >
              <SwitchCamera size={20} />
            </button>
          </div>
        </div>

        {/* 相机错误提示 */}
        {cameraError && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/80 z-20">
            <div className="text-center px-6">
              <Camera size={48} className="text-white/30 mx-auto mb-4" />
              <p className="text-white/70 text-sm">{cameraError}</p>
              <button
                onClick={startCamera}
                className="mt-4 px-4 py-2 rounded-full bg-orange-500 text-white text-sm font-medium"
              >
                重试
              </button>
            </div>
          </div>
        )}

        {/* 场景识别提示卡片 */}
        <div className="absolute bottom-36 left-4 right-4">
          <div className="bg-black/60 backdrop-blur-md rounded-2xl p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center">
                <Camera size={20} className="text-white" />
              </div>
              <div>
                <h3 className="text-white font-bold text-sm">哈苏之眼 AI 拍摄</h3>
                <p className="text-white/60 text-xs">拍摄后自动识别场景并匹配最佳参数</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs text-white/80">
              <div className="flex items-center gap-2">
                <CheckCircle size={14} className="text-green-400 flex-shrink-0" />
                <span>50+ 精细场景识别</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle size={14} className="text-green-400 flex-shrink-0" />
                <span>胶片推荐+大师建议</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 底部拍摄控制区 */}
      <div className="h-28 bg-black/90 backdrop-blur-lg border-t border-white/10 px-6 flex items-center justify-between shrink-0">
        {/* 左侧占位 */}
        <div className="w-12" />

        {/* 主拍摄按钮 - 确保可点击 */}
        <button
          onClick={captureAndAnalyze}
          disabled={!cameraReady}
          className="w-[72px] h-[72px] rounded-full bg-white border-[5px] border-orange-500 active:scale-90 transition-transform disabled:opacity-50 flex items-center justify-center"
          style={{ touchAction: 'manipulation' }}
        >
          <div className="w-[52px] h-[52px] rounded-full bg-gradient-to-br from-orange-500 to-orange-600" />
        </button>

        {/* 相册按钮 */}
        <button className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center text-white/70">
          <ImageIcon size={22} />
        </button>
      </div>
    </div>
  );

  // ============================================
  // 渲染：分析中
  // ============================================
  const renderAnalyzingMode = () => (
    <div className="h-full w-full bg-black flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-xs">
        {capturedImage && (
          <div className="relative mb-8 rounded-2xl overflow-hidden">
            <img src={capturedImage} alt="Captured" className="w-full aspect-[4/3] object-cover" />
            <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
              <div className="text-center">
                <div className="w-16 h-16 mx-auto mb-4 border-4 border-white border-t-transparent rounded-full animate-spin" />
                <p className="text-white font-medium">AI 场景分析中...</p>
              </div>
            </div>
          </div>
        )}

        <div className="space-y-4">
          {[
            { label: '场景检测', color: 'blue', delay: '0s' },
            { label: '参数匹配', color: 'orange', delay: '0.5s' },
            { label: '效果优化', color: 'green', delay: '1s' },
          ].map((item) => (
            <div key={item.label} className="bg-white/10 rounded-xl p-4">
              <div className="flex items-center gap-3 mb-3">
                <div className={`w-8 h-8 rounded-lg bg-${item.color}-500/20 flex items-center justify-center`}>
                  <Sliders size={16} className={`text-${item.color}-400`} />
                </div>
                <span className="text-white text-sm">{item.label}</span>
              </div>
              <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                <div
                  className={`h-full bg-${item.color}-500 rounded-full`}
                  style={{
                    width: '100%',
                    animation: `progress 1.5s ease-out ${item.delay} infinite`,
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
      <style>{`
        @keyframes progress {
          0% { width: 0%; opacity: 1; }
          50% { opacity: 1; }
          100% { width: 100%; opacity: 0; }
        }
      `}</style>
    </div>
  );

  // ============================================
  // 渲染：结果模式
  // ============================================
  const renderResultMode = () => (
    <div className="h-full w-full bg-black flex flex-col">
      {/* 顶部栏 */}
      <div className="h-14 px-4 flex items-center justify-between bg-black/80 backdrop-blur-lg border-b border-white/10 shrink-0">
        <button
          onClick={() => {
            setCapturedImage(null);
            setRecognizedScene(null);
            setAppliedParams({});
            setFilmRecommendations([]);
            setMasterTips([]);
            setFilterEffectEnabled(true);
            startCamera();
          }}
          className="text-white/70 hover:text-white p-2 -ml-2"
        >
          <ArrowLeft size={20} />
        </button>
        <span className="text-white font-medium">AI 出片</span>
        <button onClick={exportImage} className="text-orange-500 font-medium p-2 -mr-2">
          <Download size={20} />
        </button>
      </div>

      {/* 可滚动内容 */}
      <div className="flex-1 overflow-y-auto p-4 pb-4">
        {/* 图片预览 */}
        <div className="relative rounded-2xl overflow-hidden mb-4">
          <img
            src={capturedImage!}
            alt="Result"
            className="w-full aspect-[4/3] object-cover transition-all duration-700"
            style={{
              filter: filterEffectEnabled ? buildSceneFilter(appliedParams) : 'none',
            }}
          />

          {/* 场景标签 */}
          {recognizedScene && (
            <div className="absolute top-3 left-3">
              <div
                className="px-3 py-1.5 rounded-full text-white text-xs font-medium flex items-center gap-1.5"
                style={{ backgroundColor: recognizedScene.color }}
              >
                <recognizedScene.icon size={12} />
                <span>{recognizedScene.name}</span>
              </div>
            </div>
          )}

          {/* 效果开关 */}
          <button
            onClick={() => setFilterEffectEnabled(v => !v)}
            className="absolute top-3 right-3 px-3 py-1.5 rounded-full bg-black/60 backdrop-blur-sm text-white text-xs font-medium flex items-center gap-1.5 active:scale-95 transition-transform"
          >
            <Sparkles size={12} className={filterEffectEnabled ? 'text-orange-400' : 'text-white/60'} />
            {filterEffectEnabled ? '效果开' : '效果关'}
          </button>

          {/* 参数浮层 */}
          {showParams && (
            <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 to-transparent p-4 pt-12">
              <div className="grid grid-cols-3 gap-2 text-xs">
                {Object.entries(appliedParams).slice(0, 6).map(([key, value]) => (
                  <div key={key} className="bg-white/10 rounded-lg px-2 py-1">
                    <span className="text-white/60">{key}</span>
                    <span className="text-white font-medium ml-1">{value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* 场景信息卡片 */}
        {recognizedScene && (
          <div className="bg-white/5 rounded-2xl p-4 mb-4">
            <div className="flex items-center gap-4 mb-3">
              <div
                className="w-12 h-12 rounded-xl flex items-center justify-center"
                style={{ backgroundColor: `${recognizedScene.color}25` }}
              >
                <recognizedScene.icon size={24} style={{ color: recognizedScene.color }} />
              </div>
              <div>
                <h3 className="text-white font-bold">{recognizedScene.name}</h3>
                <p className="text-white/60 text-sm">已自动匹配最佳参数</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs">
              {[
                { label: '对比度', key: 'contrast' },
                { label: '饱和度', key: 'saturation' },
                { label: '锐度', key: 'sharpness' },
                { label: '清晰度', key: 'clarity' },
              ].map((item) => (
                <div key={item.key} className="bg-white/5 rounded-lg px-3 py-2 flex justify-between">
                  <span className="text-white/60">{item.label}</span>
                  <span className="text-white font-medium">
                    {appliedParams[item.key] !== undefined ? (appliedParams[item.key] > 0 ? '+' : '') + appliedParams[item.key] : '0'}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 胶片推荐 */}
        {filmRecommendations.length > 0 && (
          <div className="bg-white/5 rounded-2xl p-4 mb-4">
            <h3 className="text-white text-sm font-semibold mb-3 flex items-center gap-2">
              <Film size={16} className="text-orange-400" />
              哈苏胶片推荐
            </h3>
            <div className="space-y-2">
              {filmRecommendations.slice(0, 3).map((film) => (
                <div key={film.id} className="flex items-center gap-3 p-3 rounded-xl bg-white/5">
                  <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-orange-500/20 to-orange-600/20 flex items-center justify-center">
                    <ThumbsUp size={18} className="text-orange-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-white text-sm font-medium truncate">{film.name}</span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-orange-500/20 text-orange-400 flex-shrink-0">
                        {Math.round(film.matchScore * 100)}% 匹配
                      </span>
                    </div>
                    <p className="text-white/50 text-xs truncate">{film.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 大师拍摄建议 */}
        {masterTips.length > 0 && (
          <div className="bg-white/5 rounded-2xl p-4 mb-4">
            <h3 className="text-white text-sm font-semibold mb-3 flex items-center gap-2">
              <Lightbulb size={16} className="text-yellow-400" />
              大师拍摄建议
            </h3>
            <ul className="space-y-2">
              {masterTips.slice(0, 4).map((tip, index) => (
                <li key={index} className="flex items-start gap-2 text-xs text-white/70">
                  <span className="text-orange-400 mt-0.5 flex-shrink-0">•</span>
                  <span>{tip}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* 历史记录 */}
        {recognitionHistory.length > 1 && (
          <div className="mb-4">
            <h4 className="text-white/60 text-xs mb-2">最近识别</h4>
            <div className="flex gap-2 overflow-x-auto pb-2">
              {recognitionHistory.slice(1).map((scene, i) => (
                <button
                  key={`${scene.id}-${i}`}
                  onClick={() => {
                    setRecognizedScene(scene);
                    setAppliedParams(scene.params);
                    setFilterEffectEnabled(true);
                    setFilmRecommendations(getRecommendedFilms(scene.id));
                    setMasterTips(getMasterTips(scene.id));
                  }}
                  className="flex-shrink-0 w-16 h-16 rounded-xl flex items-center justify-center active:scale-95 transition-transform"
                  style={{ backgroundColor: `${scene.color}20` }}
                >
                  <scene.icon size={24} style={{ color: scene.color }} />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* 底部操作按钮 */}
        <div className="flex gap-3 pt-2">
          <button
            onClick={retake}
            className="flex-1 py-3 px-4 bg-white/10 text-white rounded-xl font-medium flex items-center justify-center gap-2 active:scale-95 transition-transform"
          >
            <Camera size={18} />
            重拍
          </button>
          <button
            onClick={applyParams}
            className="flex-1 py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-xl font-medium flex items-center justify-center gap-2 active:scale-95 transition-transform"
          >
            <Wand2 size={18} />
            {showParams ? '已应用' : '应用参数'}
          </button>
        </div>
      </div>
    </div>
  );

  // ============================================
  // 主渲染
  // ============================================
  return (
    <div className="h-full w-full bg-black">
      {!capturedImage && !isAnalyzing && renderCameraMode()}
      {isAnalyzing && renderAnalyzingMode()}
      {capturedImage && !isAnalyzing && recognizedScene && renderResultMode()}
    </div>
  );
};

export default AISceneRecognitionPage;
