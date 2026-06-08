import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Camera, Sparkles, Check, RefreshCw, Wand2, Mountain, User, Moon,
  UtensilsCrossed, Building2, TreePine, Car, Waves, Flower2, Cat, Sun,
  Palette, Film, Coffee, Download, Zap, Focus, Aperture, Timer, Thermometer,
  ChevronRight, Info, X, Eye, Lightbulb, Target, SwitchCamera, StopCircle,
  History
} from 'lucide-react';
import { analyzeImageScene, applyImageAdjustments, SceneAnalysisResult } from '../../utils/imageProcessor';

// 场景预设参数 - 哈苏大师风格
const scenePresets = [
  {
    id: 'portrait',
    name: '人像大师',
    icon: User,
    color: '#E91E63',
    hasselbladStyle: 'portrait' as const,
    desc: '柔美肤色，自然光影',
    tips: ['建议使用f/1.8大光圈', '对焦人物眼睛', '背景虚化增强层次'],
    params: { iso: '200', shutter: '1/125', aperture: 'f/1.8', wb: '5500K', saturation: '+10', warmth: '+8' },
    sampleImage: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=300&fit=crop'
  },
  {
    id: 'landscape',
    name: '风景大师',
    icon: Mountain,
    color: '#4CAF50',
    hasselbladStyle: 'natural' as const,
    desc: '通透质感，色彩饱满',
    tips: ['建议使用f/8小光圈', '开启HDR增强', '注意构图三分法'],
    params: { iso: '100', shutter: '1/60', aperture: 'f/8', wb: '5600K', saturation: '+20', warmth: '-5' },
    sampleImage: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop'
  },
  {
    id: 'night',
    name: '夜景大师',
    icon: Moon,
    color: '#3F51B5',
    hasselbladStyle: 'cinematic' as const,
    desc: '降噪增强，氛围感强',
    tips: ['建议ISO 1600-3200', '使用三脚架稳定', '注意光源曝光'],
    params: { iso: '3200', shutter: '1/15', aperture: 'f/1.6', wb: '4000K', saturation: '+25', warmth: '-10' },
    sampleImage: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop'
  },
  {
    id: 'food',
    name: '美食大师',
    icon: UtensilsCrossed,
    color: '#FF9800',
    hasselbladStyle: 'natural' as const,
    desc: '暖色调，食欲感强',
    tips: ['建议45度俯拍', '使用暖色光源', '注意食物纹理'],
    params: { iso: '200', shutter: '1/60', aperture: 'f/2.8', wb: '5200K', saturation: '+15', warmth: '+20' },
    sampleImage: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400&h=300&fit=crop'
  },
  {
    id: 'sunset',
    name: '日落大师',
    icon: Sun,
    color: '#FF5722',
    hasselbladStyle: 'cinematic' as const,
    desc: '金色暖调，氛围浪漫',
    tips: ['拍摄时机黄金时刻', '注意剪影构图', '开启HDR保留细节'],
    params: { iso: '100', shutter: '1/125', aperture: 'f/5.6', wb: '6000K', saturation: '+30', warmth: '+25' },
    sampleImage: 'https://images.unsplash.com/photo-1495616811223-4d98d6e944aa?w=400&h=300&fit=crop'
  },
  {
    id: 'street',
    name: '街拍大师',
    icon: Car,
    color: '#FF5722',
    hasselbladStyle: 'cinematic' as const,
    desc: '人文气息，故事感',
    tips: ['快速抓拍瞬间', '注意光影对比', '胶片质感增强'],
    params: { iso: '400', shutter: '1/250', aperture: 'f/5.6', wb: '5500K', saturation: '+12', warmth: '+10' },
    sampleImage: 'https://images.unsplash.com/photo-1517242810446-cc8951b2be40?w=400&h=300&fit=crop'
  },
  {
    id: 'flower',
    name: '花卉大师',
    icon: Flower2,
    color: '#E91E63',
    hasselbladStyle: 'natural' as const,
    desc: '色彩鲜艳，细节丰富',
    tips: ['微距拍摄细节', '注意背景简洁', '自然光最佳'],
    params: { iso: '100', shutter: '1/200', aperture: 'f/2.8', wb: '5500K', saturation: '+25', warmth: '+5' },
    sampleImage: 'https://images.unsplash.com/photo-1490750967868-5aa43378c200?w=400&h=300&fit=crop'
  },
  {
    id: 'architecture',
    name: '建筑大师',
    icon: Building2,
    color: '#607D8B',
    hasselbladStyle: 'natural' as const,
    desc: '线条清晰，质感强',
    tips: ['注意对称构图', '控制曝光平衡', '强调几何美感'],
    params: { iso: '100', shutter: '1/125', aperture: 'f/8', wb: '5600K', saturation: '+8', warmth: '0' },
    sampleImage: 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=400&h=300&fit=crop'
  },
  {
    id: 'pet',
    name: '宠物大师',
    icon: Cat,
    color: '#9C27B0',
    hasselbladStyle: 'natural' as const,
    desc: '毛发细节，眼神灵动',
    tips: ['捕捉眼神光', '低角度拍摄', '注意动态瞬间'],
    params: { iso: '200', shutter: '1/250', aperture: 'f/2.8', wb: '5500K', saturation: '+12', warmth: '+8' },
    sampleImage: 'https://images.unsplash.com/photo-1543466835-00a7907e9cf1?w=400&h=300&fit=crop'
  },
  {
    id: 'cafe',
    name: '咖啡馆大师',
    icon: Coffee,
    color: '#795548',
    hasselbladStyle: 'vintage' as const,
    desc: '文艺氛围，复古质感',
    tips: ['利用室内暖光', '注意环境氛围', '胶片风格增强'],
    params: { iso: '400', shutter: '1/60', aperture: 'f/2.8', wb: '4800K', saturation: '+5', warmth: '+15' },
    sampleImage: 'https://images.unsplash.com/photo-1495474476394-6091dc7a1b9d?w=400&h=300&fit=crop'
  },
  {
    id: 'beach',
    name: '海滩大师',
    icon: Waves,
    color: '#00BCD4',
    hasselbladStyle: 'cinematic' as const,
    desc: '蔚蓝水域，清新通透',
    tips: ['注意水面反光', '低角度拍摄', 'HDR增强天空'],
    params: { iso: '100', shutter: '1/200', aperture: 'f/5.6', wb: '5600K', saturation: '+15', warmth: '-8' },
    sampleImage: 'https://images.unsplash.com/photo-1507525428034-b27764b039e8?w=400&h=300&fit=crop'
  },
  {
    id: 'forest',
    name: '森林大师',
    icon: TreePine,
    color: '#388E3C',
    hasselbladStyle: 'natural' as const,
    desc: '绿意盎然，生机勃勃',
    tips: ['注意光线穿透', '强调绿色层次', '广角展现规模'],
    params: { iso: '100', shutter: '1/60', aperture: 'f/8', wb: '5600K', saturation: '+18', warmth: '+5' },
    sampleImage: 'https://images.unsplash.com/photo-1448375240586-882707db888b?w=400&h=300&fit=crop'
  },
];

// 哈苏风格标签配置
const hasselbladStyleConfig: Record<string, { label: string; color: string; bgColor: string; icon: typeof Film }> = {
  portrait: { label: 'HASSELBLAD PORTRAIT', color: '#E91E63', bgColor: 'rgba(233,30,99,0.15)', icon: User },
  natural: { label: 'HASSELBLAD NATURAL', color: '#4CAF50', bgColor: 'rgba(76,175,80,0.15)', icon: Palette },
  cinematic: { label: 'HASSELBLAD CINEMATIC', color: '#FF6B35', bgColor: 'rgba(255,107,53,0.15)', icon: Film },
  vintage: { label: 'HASSELBLAD VINTAGE', color: '#795548', bgColor: 'rgba(121,85,72,0.15)', icon: Coffee },
};

// 场景识别映射
const sceneMapping: Record<string, string> = {
  '人像': 'portrait',
  '风景': 'landscape',
  '夜景': 'night',
  '美食': 'food',
  '日落黄昏': 'sunset',
  '街拍': 'street',
  '花卉': 'flower',
  '建筑': 'architecture',
  '宠物': 'pet',
  '海景水域': 'beach',
  '自然': 'forest',
};

// 场景历史记录条目类型
interface SceneHistoryEntry {
  scene: string;
  presetId: string;
  confidence: number;
  timestamp: number;
}

// 根据哈苏风格预设生成CSS滤镜字符串
function buildHasselbladCSSFilter(preset: typeof scenePresets[0]): string {
  const sat = parseInt(preset.params.saturation.replace('+', '')) || 0;
  const warmth = parseInt(preset.params.warmth.replace('+', '').replace('-', '')) || 0;
  const isWarm = preset.params.warmth.startsWith('+') || preset.params.warmth === '0';

  const filters: string[] = [];
  filters.push(`saturate(${1 + sat / 100})`);

  if (isWarm && warmth > 0) {
    filters.push(`sepia(${warmth / 200})`);
    filters.push(`hue-rotate(${warmth / 20}deg)`);
  } else if (!isWarm && warmth > 0) {
    filters.push(`hue-rotate(-${warmth / 10}deg)`);
  }

  if (preset.hasselbladStyle === 'cinematic') {
    filters.push('contrast(1.08)');
  } else if (preset.hasselbladStyle === 'vintage') {
    filters.push('sepia(0.15)');
    filters.push('contrast(1.05)');
  } else if (preset.hasselbladStyle === 'portrait') {
    filters.push('brightness(1.03)');
    filters.push('contrast(0.98)');
  }

  return filters.join(' ');
}

const CameraSceneRecognitionPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();

  // 相机状态
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  // 状态
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState<string>('');
  const [isRecognizing, setIsRecognizing] = useState(false);
  const [recognitionResult, setRecognitionResult] = useState<SceneAnalysisResult | null>(null);
  const [recommendedPreset, setRecommendedPreset] = useState<typeof scenePresets[0] | null>(null);
  const [recognitionCount, setRecognitionCount] = useState(0);
  const [showPresetDetail, setShowPresetDetail] = useState(false);
  const [showTips, setShowTips] = useState(false);
  const [autoRecognize, setAutoRecognize] = useState(true);
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('environment');
  const [capturedImage, setCapturedImage] = useState<string>('');
  const [processedImage, setProcessedImage] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);

  // 新增状态：场景切换过渡动画
  const [sceneTransitionKey, setSceneTransitionKey] = useState(0);
  const [isSceneFadingOut, setIsSceneFadingOut] = useState(false);
  const [displayedPreset, setDisplayedPreset] = useState<typeof scenePresets[0] | null>(null);
  const [displayedResult, setDisplayedResult] = useState<SceneAnalysisResult | null>(null);

  // 新增状态：场景历史记录
  const [sceneHistory, setSceneHistory] = useState<SceneHistoryEntry[]>([]);

  // 新增状态：实时滤镜预览
  const [liveFilterEnabled, setLiveFilterEnabled] = useState(true);

  // 计算实时CSS滤镜
  const liveFilterStyle = useMemo(() => {
    if (!liveFilterEnabled || !recommendedPreset || !isCameraActive) return {};
    return { filter: buildHasselbladCSSFilter(recommendedPreset) };
  }, [recommendedPreset, liveFilterEnabled, isCameraActive]);

  // 启动相机
  const startCamera = useCallback(async () => {
    try {
      setCameraError('');

      // 停止之前的流
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
      }

      // 获取相机流
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: facingMode,
          width: { ideal: 1920 },
          height: { ideal: 1080 },
          frameRate: { ideal: 30 }
        },
        audio: false
      });

      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
      }

      setIsCameraActive(true);
    } catch (err: any) {
      console.error('相机启动失败:', err);
      setCameraError(err.message || '无法访问相机，请检查权限设置');
      setIsCameraActive(false);
    }
  }, [facingMode]);

  // 停止相机
  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setIsCameraActive(false);
    setRecognitionResult(null);
    setRecommendedPreset(null);
    setDisplayedPreset(null);
    setDisplayedResult(null);
  }, []);

  // 切换相机（前置/后置）
  const toggleCamera = useCallback(() => {
    setFacingMode(prev => prev === 'user' ? 'environment' : 'user');
    if (isCameraActive) {
      stopCamera();
      setTimeout(startCamera, 100);
    }
  }, [isCameraActive, stopCamera, startCamera]);

  // 从视频流截取帧进行分析
  const captureAndAnalyze = useCallback(async () => {
    if (!videoRef.current || !canvasRef.current || !isCameraActive) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');

    if (!ctx) return;

    // 设置canvas尺寸
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;

    // 截取当前帧
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    // 获取图片数据
    const imageData = canvas.toDataURL('image/jpeg', 0.8);

    setIsRecognizing(true);

    try {
      // 真实调用像素级场景分析
      const result = await analyzeImageScene(imageData);
      setRecognitionResult(result);
      setRecognitionCount(prev => prev + 1);

      // 匹配预设
      const presetId = sceneMapping[result.scene] || 'landscape';
      const preset = scenePresets.find(p => p.id === presetId) || scenePresets[2];

      // 场景切换过渡动画逻辑
      if (recommendedPreset && recommendedPreset.id !== preset.id) {
        // 场景发生变化，先淡出旧场景
        setIsSceneFadingOut(true);
        setTimeout(() => {
          setIsSceneFadingOut(false);
          setRecommendedPreset(preset);
          setDisplayedPreset(preset);
          setDisplayedResult(result);
          setSceneTransitionKey(prev => prev + 1);
        }, 300);
      } else {
        setRecommendedPreset(preset);
        setDisplayedPreset(preset);
        setDisplayedResult(result);
        setSceneTransitionKey(prev => prev + 1);
      }

      // 添加到历史记录（去重：同一场景连续出现不重复添加）
      setSceneHistory(prev => {
        const newEntry: SceneHistoryEntry = {
          scene: result.scene,
          presetId: preset.id,
          confidence: result.confidence,
          timestamp: Date.now(),
        };
        // 如果最近一条是同一场景，则更新而非新增
        if (prev.length > 0 && prev[0].presetId === preset.id) {
          return [newEntry, ...prev.slice(1)];
        }
        return [newEntry, ...prev].slice(0, 5);
      });
    } catch (e) {
      console.error('场景识别失败:', e);
    } finally {
      setIsRecognizing(false);
    }
  }, [isCameraActive, recommendedPreset]);

  // 自动识别模式 - 每2秒分析一次
  useEffect(() => {
    if (!autoRecognize || !isCameraActive) return;

    // 首次立即识别
    captureAndAnalyze();

    // 定时识别
    const interval = setInterval(captureAndAnalyze, 2000);

    return () => clearInterval(interval);
  }, [autoRecognize, isCameraActive, captureAndAnalyze]);

  // 切换相机方向时重启
  useEffect(() => {
    if (isCameraActive) {
      startCamera();
    }
  }, [facingMode]);

  // 清理
  useEffect(() => {
    return () => {
      stopCamera();
    };
  }, [stopCamera]);

  // 拍照
  const capturePhoto = useCallback(() => {
    if (!videoRef.current || !canvasRef.current) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');

    if (!ctx) return;

    canvas.width = video.videoWidth || 1920;
    canvas.height = video.videoHeight || 1080;

    // 如果有实时滤镜，应用到拍摄画面
    if (liveFilterEnabled && recommendedPreset) {
      ctx.filter = buildHasselbladCSSFilter(recommendedPreset);
    }

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    ctx.filter = 'none';

    const photo = canvas.toDataURL('image/jpeg', 0.95);
    setCapturedImage(photo);

    // 停止相机
    stopCamera();
  }, [stopCamera, liveFilterEnabled, recommendedPreset]);

  // 应用预设参数到拍摄的照片
  const applyPresetToPhoto = useCallback(async (preset: typeof scenePresets[0]) => {
    if (!capturedImage) return;

    setIsProcessing(true);
    try {
      const params = {
        saturation: parseInt(preset.params.saturation.replace('+', '')),
        contrast: 10,
        brightness: 5,
        warmth: parseInt(preset.params.warmth.replace('+', '').replace('-', '')),
        cyanMagenta: 0,
        sharpness: 20,
        tone: 10,
        softLight: 20,
        vignette: preset.hasselbladStyle === 'cinematic',
        filter: preset.hasselbladStyle === 'vintage' ? '胶片' : '原图'
      };

      const result = await applyImageAdjustments(capturedImage, params);
      setProcessedImage(result);
    } catch (e) {
      console.error('应用参数失败:', e);
    } finally {
      setIsProcessing(false);
    }
  }, [capturedImage]);

  // 应用预设参数（实时模式）
  const applyPreset = (preset: typeof scenePresets[0]) => {
    setAiParam('saturation', parseInt(preset.params.saturation.replace('+', '')));
    setAiParam('warmth', parseInt(preset.params.warmth.replace('+', '').replace('-', '')));
    setShowPresetDetail(true);
  };

  // 获取哈苏风格配置
  const getHasselbladConfig = (style: string) => {
    return hasselbladStyleConfig[style] || hasselbladStyleConfig.natural;
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] relative overflow-hidden">
      {/* Header */}
      <div className="absolute top-0 left-0 right-0 z-30 px-4 py-3 bg-gradient-to-b from-black/80 to-transparent">
        <div className="flex items-center justify-between">
          <button onClick={goBack} className="p-2 rounded-full bg-white/10">
            <ArrowLeft size={20} className="text-white" />
          </button>

          <div className="flex items-center gap-2">
            {isCameraActive && (
              <div className={`px-3 py-1.5 rounded-full flex items-center gap-2 ${
                autoRecognize ? 'bg-[#4CAF50]/30' : 'bg-white/10'
              }`}>
                <Zap size={14} className={autoRecognize ? 'text-[#4CAF50]' : 'text-white/50'} />
                <span className={`text-sm ${autoRecognize ? 'text-[#4CAF50]' : 'text-white/50'}`}>
                  {autoRecognize ? 'AI实时识别' : '手动识别'}
                </span>
              </div>
            )}

            {/* 实时滤镜开关 */}
            {isCameraActive && (
              <button
                onClick={() => setLiveFilterEnabled(!liveFilterEnabled)}
                className={`p-2 rounded-full ${liveFilterEnabled ? 'bg-[#FF6B35]/30' : 'bg-white/10'}`}
              >
                <Film size={16} className={liveFilterEnabled ? 'text-[#FF6B35]' : 'text-white/50'} />
              </button>
            )}

            <button
              onClick={() => setAutoRecognize(!autoRecognize)}
              className="p-2 rounded-full bg-white/10"
            >
              <RefreshCw size={16} className={`text-white ${autoRecognize && isCameraActive ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </div>

      {/* 相机预览区域 */}
      <div className="relative flex-1">
        {/* 视频预览 - 叠加CSS滤镜 */}
        {isCameraActive ? (
          <video
            ref={videoRef}
            className="w-full h-full object-cover"
            style={liveFilterStyle}
            autoPlay
            playsInline
            muted
          />
        ) : capturedImage ? (
          <img src={capturedImage} alt="拍摄照片" className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full flex flex-col items-center justify-center bg-gradient-to-b from-[#1a1a2e] to-[#0a0a0a]">
            <div className="w-24 h-24 rounded-full bg-white/5 flex items-center justify-center mb-4">
              <Camera size={48} className="text-white/30" />
            </div>
            <p className="text-white/50 text-sm mb-2">点击下方按钮启动相机</p>
            <p className="text-white/30 text-xs mb-4">AI将实时识别场景并推荐哈苏大师参数</p>

            {cameraError && (
              <div className="px-4 py-2 rounded-xl bg-red-500/20 border border-red-500/50 mb-4">
                <p className="text-red-400 text-sm">{cameraError}</p>
              </div>
            )}
          </div>
        )}

        {/* 隐藏的Canvas用于帧分析 */}
        <canvas ref={canvasRef} className="hidden" />

        {/* AI识别动画 */}
        {isRecognizing && isCameraActive && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="relative">
              <div className="w-64 h-64 rounded-full border-2 border-[#4CAF50]/50 animate-pulse">
                <div className="absolute inset-0 rounded-full border-2 border-[#4CAF50] animate-spin"
                  style={{ animationDuration: '2s' }} />
              </div>
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="px-4 py-2 rounded-full bg-[#4CAF50]/80 backdrop-blur-sm">
                  <span className="text-white text-sm font-medium flex items-center gap-2">
                    <Sparkles size={14} className="animate-pulse" />
                    AI识别中...
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* 识别成功指示器 - 场景切换过渡动画 */}
        {displayedResult && !isRecognizing && isCameraActive && (
          <div
            key={`scene-label-${sceneTransitionKey}`}
            className={`absolute top-20 left-1/2 -translate-x-1/2 z-20 ${
              isSceneFadingOut ? 'animate-scene-fade-out' : 'animate-scene-fade-in'
            }`}
          >
            <div className="px-4 py-2 rounded-full bg-[#4CAF50]/90 backdrop-blur-sm flex items-center gap-2">
              <Check size={16} className="text-white" />
              <span className="text-white text-sm font-medium">
                {displayedResult.scene} · {displayedResult.confidence}%
              </span>
            </div>
          </div>
        )}

        {/* 哈苏风格标签 */}
        {displayedPreset && isCameraActive && !isSceneFadingOut && (
          <div
            key={`hasselblad-tag-${sceneTransitionKey}`}
            className="absolute top-32 left-1/2 -translate-x-1/2 z-20 animate-scene-fade-in"
          >
            {(() => {
              const config = getHasselbladConfig(displayedPreset.hasselbladStyle);
              const TagIcon = config.icon;
              return (
                <div
                  className="px-3 py-1.5 rounded-full backdrop-blur-sm flex items-center gap-2"
                  style={{ backgroundColor: config.bgColor, border: `1px solid ${config.color}40` }}
                >
                  <TagIcon size={12} style={{ color: config.color }} />
                  <span className="text-xs font-bold tracking-wider" style={{ color: config.color }}>
                    {config.label}
                  </span>
                </div>
              );
            })()}
          </div>
        )}

        {/* 参数实时预览增强 - 半透明叠加面板 */}
        {displayedPreset && isCameraActive && !isSceneFadingOut && (
          <div
            key={`params-${sceneTransitionKey}`}
            className="absolute top-44 right-3 z-20 animate-slide-in-right"
          >
            <div className="p-3 rounded-xl bg-black/50 backdrop-blur-md border border-white/10 animate-param-pulse">
              <div className="flex items-center gap-1.5 mb-2">
                <Aperture size={10} className="text-[#FF6B35]" />
                <span className="text-white/50 text-[10px] tracking-wider">哈苏推荐参数</span>
              </div>
              <div className="grid grid-cols-2 gap-x-4 gap-y-1.5">
                <div className="flex items-center gap-1.5">
                  <div className="w-4 h-4 rounded bg-[#FF6B35]/20 flex items-center justify-center">
                    <Focus size={8} className="text-[#FF6B35]" />
                  </div>
                  <div>
                    <p className="text-white/40 text-[8px]">ISO</p>
                    <p className="text-white text-xs font-medium">{displayedPreset.params.iso}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-4 h-4 rounded bg-[#FF6B35]/20 flex items-center justify-center">
                    <Timer size={8} className="text-[#FF6B35]" />
                  </div>
                  <div>
                    <p className="text-white/40 text-[8px]">快门</p>
                    <p className="text-white text-xs font-medium">{displayedPreset.params.shutter}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-4 h-4 rounded bg-[#FF6B35]/20 flex items-center justify-center">
                    <Aperture size={8} className="text-[#FF6B35]" />
                  </div>
                  <div>
                    <p className="text-white/40 text-[8px]">光圈</p>
                    <p className="text-white text-xs font-medium">{displayedPreset.params.aperture}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <div className="w-4 h-4 rounded bg-[#FF6B35]/20 flex items-center justify-center">
                    <Thermometer size={8} className="text-[#FF6B35]" />
                  </div>
                  <div>
                    <p className="text-white/40 text-[8px]">白平衡</p>
                    <p className="text-white text-xs font-medium">{displayedPreset.params.wb}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Top3场景置信度条 */}
        {displayedResult && isCameraActive && !isSceneFadingOut && (
          <div
            key={`confidence-${sceneTransitionKey}`}
            className="absolute left-3 top-[55%] -translate-y-1/2 z-20 animate-slide-in-right"
          >
            <div className="p-2.5 rounded-xl bg-black/50 backdrop-blur-md border border-white/10 w-36">
              <p className="text-white/40 text-[9px] mb-2 tracking-wider">场景置信度 TOP3</p>
              <div className="space-y-2">
                {displayedResult.topScenes.map((scene, i) => {
                  const presetId = sceneMapping[scene.name] || 'landscape';
                  const preset = scenePresets.find(p => p.id === presetId);
                  const barColor = preset?.color || '#4CAF50';
                  return (
                    <div key={`${scene.name}-${i}`}>
                      <div className="flex items-center justify-between mb-0.5">
                        <span className="text-white text-[10px] font-medium">{scene.name}</span>
                        <span className="text-white/60 text-[10px]">{scene.confidence}%</span>
                      </div>
                      <div className="h-1.5 rounded-full bg-white/10 overflow-hidden">
                        <div
                          className="h-full rounded-full animate-confidence-grow"
                          style={{
                            width: `${scene.confidence}%`,
                            backgroundColor: barColor,
                            animationDelay: `${i * 0.15}s`,
                          }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* 识别次数统计 */}
        {isCameraActive && (
          <div className="absolute bottom-4 left-4 z-10 px-2 py-1 rounded-full bg-black/50">
            <span className="text-white/40 text-xs">识别 #{recognitionCount}</span>
          </div>
        )}

        {/* 实时滤镜指示器 */}
        {isCameraActive && liveFilterEnabled && recommendedPreset && (
          <div className="absolute bottom-4 right-4 z-10 px-2 py-1 rounded-full bg-[#FF6B35]/30 backdrop-blur-sm">
            <span className="text-[#FF6B35] text-[10px] font-medium flex items-center gap-1">
              <Film size={10} />
              滤镜预览
            </span>
          </div>
        )}
      </div>

      {/* 底部控制区域 */}
      <div className="bg-gradient-to-t from-black/90 via-black/70 to-transparent p-4 pt-12">
        {/* 场景历史记录 */}
        {sceneHistory.length > 0 && isCameraActive && (
          <div className="mb-3">
            <div className="flex items-center gap-1.5 mb-2">
              <History size={10} className="text-white/40" />
              <span className="text-white/40 text-[10px] tracking-wider">场景历史</span>
            </div>
            <div className="flex gap-2 overflow-x-auto scrollbar-hide">
              {sceneHistory.map((entry, i) => {
                const preset = scenePresets.find(p => p.id === entry.presetId);
                if (!preset) return null;
                const Icon = preset.icon;
                return (
                  <div
                    key={`${entry.timestamp}-${i}`}
                    className="flex-shrink-0 animate-history-slide-in"
                    style={{ animationDelay: `${i * 0.05}s` }}
                  >
                    <div
                      className={`px-2.5 py-1.5 rounded-lg flex items-center gap-1.5 border ${
                        i === 0
                          ? 'bg-white/10 border-white/20'
                          : 'bg-white/5 border-white/5'
                      }`}
                    >
                      <Icon size={10} style={{ color: preset.color }} />
                      <span className={`text-[10px] ${i === 0 ? 'text-white/80' : 'text-white/40'}`}>
                        {entry.scene}
                      </span>
                      <span className="text-white/25 text-[8px]">{entry.confidence}%</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 相机控制按钮 */}
        {!isCameraActive && !capturedImage && (
          <button
            onClick={startCamera}
            className="w-full py-4 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-3 text-white font-medium"
          >
            <Camera size={24} />
            <span>启动相机</span>
          </button>
        )}

        {/* 相机活跃时的控制 */}
        {isCameraActive && (
          <div className="flex items-center justify-center gap-4">
            {/* 切换相机 */}
            <button
              onClick={toggleCamera}
              className="p-3 rounded-full bg-white/10"
            >
              <SwitchCamera size={24} className="text-white" />
            </button>

            {/* 拍照按钮 */}
            <button
              onClick={capturePhoto}
              className="w-16 h-16 rounded-full bg-white border-4 border-white/30 flex items-center justify-center"
            >
              <div className="w-12 h-12 rounded-full bg-white/90" />
            </button>

            {/* 停止相机 */}
            <button
              onClick={stopCamera}
              className="p-3 rounded-full bg-white/10"
            >
              <StopCircle size={24} className="text-white" />
            </button>
          </div>
        )}

        {/* 拍摄后的控制 */}
        {capturedImage && !isCameraActive && (
          <div className="space-y-3">
            {/* 推荐预设卡片 */}
            {recommendedPreset && (
              <div
                className="flex items-center gap-4 p-4 rounded-xl bg-white/5 cursor-pointer"
                onClick={() => setShowPresetDetail(true)}
              >
                <div className="w-16 h-16 rounded-xl overflow-hidden border-2 border-[#FF6B35]/50">
                  <img src={recommendedPreset.sampleImage} alt={recommendedPreset.name} className="w-full h-full object-cover" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                      style={{ backgroundColor: `${recommendedPreset.color}30` }}>
                      {React.createElement(recommendedPreset.icon, { size: 16, style: { color: recommendedPreset.color } })}
                    </div>
                    <span className="text-white font-bold">{recommendedPreset.name}</span>
                    <span className="px-2 py-0.5 rounded-full bg-[#4CAF50]/20 text-[#4CAF50] text-xs">AI推荐</span>
                  </div>
                  <p className="text-white/60 text-sm">{recommendedPreset.desc}</p>
                </div>
                <ChevronRight size={20} className="text-white/40" />
              </div>
            )}

            {/* 操作按钮 */}
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setCapturedImage('');
                  setProcessedImage('');
                  startCamera();
                }}
                className="flex-1 py-3 rounded-xl bg-white/10 flex items-center justify-center gap-2 text-white"
              >
                <Camera size={18} />
                <span>重新拍摄</span>
              </button>
              <button
                onClick={() => recommendedPreset && applyPresetToPhoto(recommendedPreset)}
                disabled={!recommendedPreset || isProcessing}
                className="flex-1 py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium disabled:opacity-50"
              >
                {isProcessing ? (
                  <>
                    <RefreshCw size={18} className="animate-spin" />
                    <span>处理中...</span>
                  </>
                ) : (
                  <>
                    <Wand2 size={18} />
                    <span>应用哈苏参数</span>
                  </>
                )}
              </button>
            </div>

            {/* 处理后的图片 */}
            {processedImage && (
              <div className="mt-3">
                <p className="text-white/50 text-xs mb-2 flex items-center gap-2">
                  <Sparkles size={12} />
                  哈苏大师出片
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <div className="rounded-xl overflow-hidden">
                    <img src={capturedImage} alt="原图" className="w-full aspect-video object-cover" />
                    <div className="p-2 bg-white/5 text-center">
                      <span className="text-white/50 text-xs">原图</span>
                    </div>
                  </div>
                  <div className="rounded-xl overflow-hidden border border-[#FF6B35]/50">
                    <img src={processedImage} alt="哈苏处理" className="w-full aspect-video object-cover" />
                    <div className="p-2 bg-[#FF6B35]/20 text-center">
                      <span className="text-[#FF6B35] text-xs">{recommendedPreset?.name}</span>
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => {
                    const link = document.createElement('a');
                    link.href = processedImage;
                    link.download = `OMaster_${recommendedPreset?.name}_${Date.now()}.jpg`;
                    link.click();
                  }}
                  className="w-full mt-2 py-2 rounded-xl bg-white/10 flex items-center justify-center gap-2 text-white text-sm"
                >
                  <Download size={16} />
                  <span>保存哈苏出片</span>
                </button>
              </div>
            )}
          </div>
        )}

        {/* 实时推荐预设卡片 */}
        {displayedPreset && isCameraActive && !isSceneFadingOut && (
          <div
            key={`preset-card-${sceneTransitionKey}`}
            className="mt-2 animate-slide-in-up"
          >
            <div
              className="flex items-center gap-3 p-3 rounded-xl bg-white/5 cursor-pointer"
              onClick={() => setShowPresetDetail(true)}
            >
              <div className="w-14 h-14 rounded-xl overflow-hidden border-2 border-[#FF6B35]/50">
                <img src={displayedPreset.sampleImage} alt={displayedPreset.name} className="w-full h-full object-cover" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-0.5">
                  <div className="w-6 h-6 rounded-lg flex items-center justify-center"
                    style={{ backgroundColor: `${displayedPreset.color}30` }}>
                    {React.createElement(displayedPreset.icon, { size: 12, style: { color: displayedPreset.color } })}
                  </div>
                  <span className="text-white font-bold text-sm">{displayedPreset.name}</span>
                  <span className="px-1.5 py-0.5 rounded-full bg-[#4CAF50]/20 text-[#4CAF50] text-[10px]">AI推荐</span>
                </div>
                <p className="text-white/60 text-xs truncate">{displayedPreset.desc}</p>
                {/* 哈苏风格标签 */}
                {(() => {
                  const config = getHasselbladConfig(displayedPreset.hasselbladStyle);
                  const TagIcon = config.icon;
                  return (
                    <div className="flex items-center gap-1 mt-1">
                      <TagIcon size={8} style={{ color: config.color }} />
                      <span className="text-[9px] font-bold tracking-wider" style={{ color: config.color }}>
                        {config.label}
                      </span>
                    </div>
                  );
                })()}
              </div>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  applyPreset(displayedPreset);
                }}
                className="px-3 py-1.5 rounded-xl bg-[#FF6B35] text-white text-xs font-medium flex-shrink-0"
              >
                应用
              </button>
            </div>

            {/* 拍摄技巧按钮 */}
            <button
              onClick={() => setShowTips(true)}
              className="mt-2 w-full py-2 rounded-xl bg-white/10 flex items-center justify-center gap-2 text-white/70 text-sm"
            >
              <Lightbulb size={14} />
              <span>查看拍摄技巧</span>
            </button>
          </div>
        )}
      </div>

      {/* 预设详情弹窗 */}
      {showPresetDetail && recommendedPreset && (
        <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-sm flex items-end">
          <div className="w-full bg-[#1a1a1a] rounded-t-3xl p-6 animate-slide-up">
            <button onClick={() => setShowPresetDetail(false)} className="absolute top-4 right-4 p-2 rounded-full bg-white/10">
              <X size={20} className="text-white" />
            </button>

            {/* 哈苏风格标签 */}
            {(() => {
              const config = getHasselbladConfig(recommendedPreset.hasselbladStyle);
              const TagIcon = config.icon;
              return (
                <div
                  className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full mb-4"
                  style={{ backgroundColor: config.bgColor, border: `1px solid ${config.color}40` }}
                >
                  <TagIcon size={14} style={{ color: config.color }} />
                  <span className="text-xs font-bold tracking-wider" style={{ color: config.color }}>
                    {config.label}
                  </span>
                </div>
              );
            })()}

            <div className="mb-4">
              <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Eye size={12} />
                大师样张效果对比
              </p>
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl overflow-hidden">
                  <div className="w-full aspect-[4/3] bg-white/5 flex items-center justify-center">
                    <Camera size={32} className="text-white/30" />
                    <span className="text-white/50 text-xs ml-2">当前取景</span>
                  </div>
                </div>
                <div className="rounded-xl overflow-hidden border border-[#FF6B35]/50">
                  <img src={recommendedPreset.sampleImage} alt="大师样张" className="w-full aspect-[4/3] object-cover" />
                  <div className="p-2 bg-[#FF6B35]/20 text-center">
                    <span className="text-[#FF6B35] text-xs">{recommendedPreset.name}样张</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="mb-4">
              <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Target size={12} />
                推荐拍摄参数
              </p>
              <div className="grid grid-cols-3 gap-2">
                {Object.entries(recommendedPreset.params).slice(0, 6).map(([key, value]) => (
                  <div key={key} className="p-3 rounded-xl bg-white/5 text-center">
                    <p className="text-white/50 text-xs mb-1">
                      {key === 'iso' ? 'ISO' : key === 'shutter' ? '快门' : key === 'aperture' ? '光圈' : key === 'wb' ? '白平衡' : key === 'saturation' ? '饱和度' : '色温'}
                    </p>
                    <p className="text-[#FF6B35] text-sm font-bold">{value}</p>
                  </div>
                ))}
              </div>
            </div>

            <button
              onClick={() => setShowPresetDetail(false)}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium"
            >
              <Wand2 size={18} />
              <span>应用{recommendedPreset.name}参数</span>
            </button>
          </div>
        </div>
      )}

      {/* 拍摄技巧弹窗 */}
      {showTips && recommendedPreset && (
        <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-sm flex items-end">
          <div className="w-full bg-[#1a1a1a] rounded-t-3xl p-6 animate-slide-up">
            <button onClick={() => setShowTips(false)} className="absolute top-4 right-4 p-2 rounded-full bg-white/10">
              <X size={20} className="text-white" />
            </button>

            <div className="mb-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-xl flex items-center justify-center"
                  style={{ backgroundColor: `${recommendedPreset.color}30` }}>
                  {React.createElement(recommendedPreset.icon, { size: 24, style: { color: recommendedPreset.color } })}
                </div>
                <div>
                  <p className="text-white font-bold">{recommendedPreset.name}</p>
                  <p className="text-white/50 text-sm">{recommendedPreset.desc}</p>
                </div>
              </div>
            </div>

            <div className="space-y-3 mb-4">
              <p className="text-white/50 text-xs flex items-center gap-2">
                <Lightbulb size={12} />
                专业拍摄技巧
              </p>
              {recommendedPreset.tips.map((tip, i) => (
                <div key={i} className="p-4 rounded-xl bg-white/5 flex items-start gap-3">
                  <div className="w-6 h-6 rounded-full bg-[#4CAF50]/20 flex items-center justify-center text-[#4CAF50] text-xs font-bold">
                    {i + 1}
                  </div>
                  <p className="text-white text-sm">{tip}</p>
                </div>
              ))}
            </div>

            <button onClick={() => setShowTips(false)} className="w-full py-3 rounded-xl bg-white/10 text-white font-medium">
              开始拍摄
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default CameraSceneRecognitionPage;
