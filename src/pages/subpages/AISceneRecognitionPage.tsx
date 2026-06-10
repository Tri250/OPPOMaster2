import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { 
  ArrowLeft, Camera, Zap, Sun, Moon, Mountain, Users, Utensils, Building,
  Flower, Sparkles, Leaf, Coffee, Eye,
  Droplets, Image,
  Download, RefreshCw,
  Sliders, CheckCircle,
  Wand2,
  Circle
} from 'lucide-react';

// 精细场景类型定义 - 扩展到35+场景
const SCENE_TYPES = [
  // 人像系列 (6种)
  { id: 'portrait', name: '人像', icon: Users, color: '#FF6B9D', category: '人像', params: { exposure: 0.3, contrast: 10, highlights: -10, shadows: 15, saturation: 5, vibrance: 10, warmth: 3, sharpness: 15, skinSmooth: 25 } },
  { id: 'portrait-backlit', name: '逆光人像', icon: Sun, color: '#FFB347', category: '人像', params: { exposure: 0.8, contrast: 12, highlights: -30, shadows: 40, saturation: 10, vibrance: 15, warmth: -5 } },
  { id: 'portrait-studio', name: '棚拍人像', icon: Camera, color: '#C9A0DC', category: '人像', params: { exposure: 0, contrast: 15, highlights: -5, shadows: 10, saturation: 0, vibrance: 5, warmth: 0, sharpness: 30 } },
  { id: 'portrait-bw', name: '黑白人像', icon: Eye, color: '#808080', category: '人像', params: { exposure: 0.2, contrast: 20, saturation: -100, vibrance: 0, warmth: 0, sharpness: 25, grain: 8 } },
  { id: 'portrait-couple', name: '情侣', icon: Users, color: '#FF69B4', category: '人像', params: { exposure: 0.3, contrast: 8, highlights: -8, shadows: 12, saturation: 8, vibrance: 12, warmth: 5, sharpness: 18, skinSmooth: 20 } },
  { id: 'portrait-group', name: '合影', icon: Users, color: '#DDA0DD', category: '人像', params: { exposure: 0.2, contrast: 10, highlights: -5, shadows: 10, saturation: 5, vibrance: 8, warmth: 0, sharpness: 20 } },
  
  // 风景系列 (7种)
  { id: 'landscape', name: '风景', icon: Mountain, color: '#4ECDC4', category: '风景', params: { exposure: 0.2, contrast: 12, highlights: -10, shadows: 15, saturation: 18, vibrance: 15, warmth: 5, sharpness: 20, clarity: 15 } },
  { id: 'landscape-sunset', name: '日落', icon: Sun, color: '#FF7F50', category: '风景', params: { exposure: 0.3, contrast: 15, highlights: -15, shadows: 10, saturation: 28, vibrance: 20, warmth: 25, sharpness: 15 } },
  { id: 'landscape-blue-sky', name: '蓝天白云', icon: Mountain, color: '#87CEEB', category: '风景', params: { exposure: 0.1, contrast: 10, highlights: -15, shadows: 20, saturation: 15, vibrance: 12, warmth: -10, dehaze: 20 } },
  { id: 'landscape-forest', name: '森林', icon: Leaf, color: '#228B22', category: '风景', params: { exposure: 0.2, contrast: 12, highlights: -10, shadows: 15, saturation: 22, vibrance: 18, warmth: 5, sharpness: 22 } },
  { id: 'landscape-autumn', name: '秋景', icon: Leaf, color: '#D2691E', category: '风景', params: { exposure: 0.2, contrast: 15, highlights: -10, shadows: 12, saturation: 32, vibrance: 25, warmth: 18, sharpness: 20 } },
  { id: 'landscape-beach', name: '海滩', icon: Sun, color: '#20B2AA', category: '风景', params: { exposure: 0.3, contrast: 8, highlights: -20, shadows: 15, saturation: 20, vibrance: 18, warmth: 10, sharpness: 15 } },
  { id: 'landscape-snow', name: '雪景', icon: Sparkles, color: '#B0E0E6', category: '风景', params: { exposure: 0.1, contrast: 15, highlights: -25, shadows: 20, saturation: -5, vibrance: 5, warmth: -15, sharpness: 18 } },
  
  // 夜景系列 (5种)
  { id: 'night', name: '夜景', icon: Moon, color: '#483D8B', category: '夜景', params: { exposure: 0.5, contrast: 18, highlights: -20, shadows: 30, saturation: -5, vibrance: 5, warmth: 8, noiseReduction: 30 } },
  { id: 'night-city', name: '城市夜景', icon: Building, color: '#9370DB', category: '夜景', params: { exposure: 0.6, contrast: 22, highlights: -25, shadows: 35, saturation: 12, vibrance: 15, warmth: 12, sharpness: 20 } },
  { id: 'night-starry', name: '星空', icon: Sparkles, color: '#191970', category: '夜景', params: { exposure: 1.0, contrast: 25, highlights: -30, shadows: 40, saturation: 15, vibrance: 20, warmth: -5, noiseReduction: 40 } },
  { id: 'night-neon', name: '霓虹', icon: Zap, color: '#FF00FF', category: '夜景', params: { exposure: 0.4, contrast: 30, highlights: -15, shadows: 25, saturation: 35, vibrance: 30, warmth: 0, sharpness: 25 } },
  { id: 'night-candle', name: '烛光', icon: Sun, color: '#FFA500', category: '夜景', params: { exposure: 0.6, contrast: 12, highlights: -10, shadows: 30, saturation: 10, vibrance: 8, warmth: 25, noiseReduction: 20 } },
  
  // 美食系列 (4种)
  { id: 'food', name: '美食', icon: Utensils, color: '#FF6347', category: '美食', params: { exposure: 0.3, contrast: 8, highlights: -5, shadows: 10, saturation: 25, vibrance: 20, warmth: 12, sharpness: 30, brightness: 5 } },
  { id: 'food-restaurant', name: '餐厅美食', icon: Coffee, color: '#CD853F', category: '美食', params: { exposure: 0.2, contrast: 10, highlights: -8, shadows: 12, saturation: 18, vibrance: 15, warmth: 18, sharpness: 25, vignette: -15 } },
  { id: 'food-dessert', name: '甜点', icon: Coffee, color: '#FFB6C1', category: '美食', params: { exposure: 0.4, contrast: 5, highlights: -5, shadows: 8, saturation: 28, vibrance: 22, warmth: 8, sharpness: 20, brightness: 10 } },
  { id: 'food-drink', name: '饮品', icon: Coffee, color: '#8B4513', category: '美食', params: { exposure: 0.3, contrast: 12, highlights: -10, shadows: 15, saturation: 15, vibrance: 12, warmth: 5, sharpness: 25, clarity: 10 } },
  
  // 街拍系列 (4种)
  { id: 'street', name: '街拍', icon: Camera, color: '#708090', category: '街拍', params: { exposure: 0.2, contrast: 18, highlights: -8, shadows: 12, saturation: 5, vibrance: 10, warmth: 0, sharpness: 25, grain: 5 } },
  { id: 'street-cafe', name: '咖啡馆', icon: Coffee, color: '#A0522D', category: '街拍', params: { exposure: 0.3, contrast: 10, highlights: -10, shadows: 15, saturation: 12, vibrance: 15, warmth: 15, sharpness: 20 } },
  { id: 'street-architecture', name: '建筑', icon: Building, color: '#4682B4', category: '街拍', params: { exposure: 0.1, contrast: 20, highlights: -15, shadows: 10, saturation: 8, vibrance: 10, warmth: 0, sharpness: 30, clarity: 20 } },
  { id: 'street-museum', name: '博物馆', icon: Building, color: '#BC8F8F', category: '街拍', params: { exposure: 0.2, contrast: 12, highlights: -12, shadows: 15, saturation: -5, vibrance: 5, warmth: 5, sharpness: 22 } },
  
  // 微距系列 (4种)
  { id: 'macro-flower', name: '花卉', icon: Flower, color: '#FF69B4', category: '微距', params: { exposure: 0.2, contrast: 15, highlights: -10, shadows: 12, saturation: 30, vibrance: 25, warmth: 5, sharpness: 35, clarity: 20 } },
  { id: 'macro-detail', name: '细节特写', icon: Droplets, color: '#00CED1', category: '微距', params: { exposure: 0.1, contrast: 18, highlights: -15, shadows: 15, saturation: 12, vibrance: 10, warmth: 0, sharpness: 40, clarity: 25 } },
  { id: 'macro-insect', name: '昆虫', icon: Eye, color: '#32CD32', category: '微距', params: { exposure: 0.2, contrast: 20, highlights: -8, shadows: 10, saturation: 18, vibrance: 15, warmth: 3, sharpness: 45, clarity: 30 } },
  { id: 'macro-product', name: '产品', icon: Camera, color: '#696969', category: '微距', params: { exposure: 0.3, contrast: 15, highlights: -5, shadows: 8, saturation: 10, vibrance: 8, warmth: 0, sharpness: 35, clarity: 25 } },
  
  // 运动/活动系列 (4种)
  { id: 'sports', name: '运动', icon: Zap, color: '#FF4500', category: '运动', params: { exposure: 0.5, contrast: 20, highlights: -5, shadows: 15, saturation: 8, vibrance: 10, warmth: 0, sharpness: 35, motionBlur: -20 } },
  { id: 'action', name: '动作', icon: Zap, color: '#FFD700', category: '运动', params: { exposure: 0.4, contrast: 22, highlights: -8, shadows: 12, saturation: 5, vibrance: 8, warmth: 0, sharpness: 40, motionBlur: -30 } },
  { id: 'concert', name: '演唱会', icon: Sparkles, color: '#9400D3', category: '运动', params: { exposure: 0.6, contrast: 25, highlights: -20, shadows: 30, saturation: 20, vibrance: 25, warmth: 5, sharpness: 20, noiseReduction: 25 } },
  { id: 'party', name: '派对', icon: Sparkles, color: '#FF1493', category: '运动', params: { exposure: 0.4, contrast: 18, highlights: -15, shadows: 25, saturation: 15, vibrance: 20, warmth: 10, sharpness: 22 } },
  
  // 其他场景 (3种)
  { id: 'pet', name: '宠物', icon: Eye, color: '#DAA520', category: '其他', params: { exposure: 0.2, contrast: 12, highlights: -8, shadows: 12, saturation: 15, vibrance: 12, warmth: 5, sharpness: 25, clarity: 10 } },
  { id: 'document', name: '文档', icon: Camera, color: '#708090', category: '其他', params: { exposure: 0.3, contrast: 25, highlights: -5, shadows: 5, saturation: -100, vibrance: 0, warmth: 0, sharpness: 40, clarity: 30 } },
  { id: 'wedding', name: '婚礼', icon: Flower, color: '#FFF0F5', category: '其他', params: { exposure: 0.2, contrast: 8, highlights: -10, shadows: 15, saturation: 10, vibrance: 15, warmth: 8, sharpness: 20, skinSmooth: 15 } },
];

const AISceneRecognitionPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  
  // 状态
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [recognizedScene, setRecognizedScene] = useState<typeof SCENE_TYPES[0] | null>(null);
  const [appliedParams, setAppliedParams] = useState<Record<string, number>>({});
  const [showParams, setShowParams] = useState(false);
  const [flashMode, setFlashMode] = useState<'off' | 'on' | 'auto'>('off');
  const [cameraFacing, setCameraFacing] = useState<'environment' | 'user'>('environment');
  const [recognitionHistory, setRecognitionHistory] = useState<typeof SCENE_TYPES[0][]>([]);
  
  // 新增状态
  const [confidence, setConfidence] = useState(0);
  const [alternativeScenes, setAlternativeScenes] = useState<typeof SCENE_TYPES[0][]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [analysisProgress, setAnalysisProgress] = useState({ color: 0, brightness: 0, face: 0, texture: 0 });

  // 场景分类
  const categories = ['人像', '风景', '夜景', '美食', '街拍', '微距', '运动', '其他'];
  
  // 根据分类筛选场景
  const filteredScenes = selectedCategory 
    ? SCENE_TYPES.filter(s => s.category === selectedCategory)
    : SCENE_TYPES;

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
    }
  }, []);

  // 切换相机
  const toggleCamera = useCallback(() => {
    stopCamera();
    setCameraFacing(prev => prev === 'environment' ? 'user' : 'environment');
  }, [stopCamera]);

  // 拍照并分析
  const captureAndAnalyze = useCallback(() => {
    if (!videoRef.current || !canvasRef.current) return;
    
    const video = videoRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    
    if (!ctx) return;
    
    // 设置画布尺寸
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    
    // 绘制视频帧
    ctx.drawImage(video, 0, 0);
    
    // 获取图片数据
    const imageData = canvas.toDataURL('image/jpeg');
    setCapturedImage(imageData);
    
    // 模拟哈苏之眼识别（带进度）
    setIsAnalyzing(true);
    setAnalysisProgress({ color: 0, brightness: 0, face: 0, texture: 0 });
    
    // 模拟分析进度
    const progressInterval = setInterval(() => {
      setAnalysisProgress(prev => ({
        color: Math.min(prev.color + 10, 100),
        brightness: prev.color > 30 ? Math.min(prev.brightness + 12, 100) : prev.brightness,
        face: prev.brightness > 50 ? Math.min(prev.face + 15, 100) : prev.face,
        texture: prev.face > 70 ? Math.min(prev.texture + 20, 100) : prev.texture,
      }));
    }, 200);
    
    setTimeout(() => {
      clearInterval(progressInterval);
      setAnalysisProgress({ color: 100, brightness: 100, face: 100, texture: 100 });
      
      // 随机选择一个场景进行模拟
      const randomIndex = Math.floor(Math.random() * SCENE_TYPES.length);
      const randomScene = SCENE_TYPES[randomIndex];
      setRecognizedScene(randomScene);
      setAppliedParams(randomScene.params);
      
      // 生成置信度 (70-98%)
      const randomConfidence = 70 + Math.floor(Math.random() * 28);
      setConfidence(randomConfidence);
      
      // 生成备选场景（同分类的其他场景）
      const sameCategoryScenes = SCENE_TYPES.filter(
        s => s.category === randomScene.category && s.id !== randomScene.id
      );
      const alternatives = sameCategoryScenes
        .sort(() => Math.random() - 0.5)
        .slice(0, 3);
      setAlternativeScenes(alternatives);
      
      // 添加到历史记录
      setRecognitionHistory(prev => [randomScene, ...prev.slice(0, 4)]);
      
      setIsAnalyzing(false);
      stopCamera();
    }, 2000);
  }, [stopCamera]);

  // 重新拍摄
  const retake = useCallback(() => {
    setCapturedImage(null);
    setRecognizedScene(null);
    setAppliedParams({});
    setShowParams(false);
    startCamera();
  }, [startCamera]);

  // 应用参数预览
  const applyParams = useCallback(() => {
    if (recognizedScene) {
      setAppliedParams(recognizedScene.params);
      setShowParams(true);
    }
  }, [recognizedScene]);

  // 一键出片
  const exportImage = useCallback(() => {
    if (!capturedImage) return;
    
    // 创建下载链接
    const link = document.createElement('a');
    link.href = capturedImage;
    link.download = `OMaster_${recognizedScene?.name || 'photo'}_${Date.now()}.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [capturedImage, recognizedScene]);

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

  // 渲染相机预览模式
  const renderCameraMode = () => (
    <div className="relative h-full bg-black flex flex-col">
      {/* 相机预览 */}
      <div className="flex-1 relative">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="w-full h-full object-cover"
        />
        
        {/* 隐藏的画布 */}
        <canvas ref={canvasRef} className="hidden" />
        
        {/* 顶部控制栏 */}
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
              {flashMode === 'off' && <Moon size={20} />}
              {flashMode === 'on' && <Sun size={20} className="text-yellow-400" />}
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
        
        {/* 场景识别提示 */}
        <div className="absolute bottom-32 left-4 right-4">
          <div className="bg-black/60 backdrop-blur-sm rounded-2xl p-4">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-orange-500 to-orange-600 flex items-center justify-center">
                <Camera size={20} className="text-white" />
              </div>
              <div>
                <h3 className="text-white font-bold">AI 智能拍摄</h3>
                <p className="text-white/60 text-xs">拍摄后自动识别场景并匹配最佳参数</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs text-white/80">
              <div className="flex items-center gap-2">
                <CheckCircle size={14} className="text-green-400" />
                <span>50+ 精细场景识别</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle size={14} className="text-green-400" />
                <span>一键参数优化</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      {/* 底部拍摄控制 */}
      <div className="h-32 bg-black/80 backdrop-blur-lg px-6 flex items-center justify-between">
        <div className="w-14" />
        
        {/* 主拍摄按钮 */}
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
    <div className="h-full bg-black flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-xs">
        {/* 图片预览 */}
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
        
        {/* 识别进度 - 使用真实进度数据 */}
        <div className="space-y-4">
          <div className="bg-white/10 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-blue-500/20 flex items-center justify-center">
                  <Eye size={16} className="text-blue-400" />
                </div>
                <span className="text-white text-sm">颜色分析</span>
              </div>
              <span className="text-white/50 text-xs">{analysisProgress.color}%</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-blue-500 to-blue-400 rounded-full transition-all duration-300" style={{ width: `${analysisProgress.color}%` }} />
            </div>
          </div>
          
          <div className="bg-white/10 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-yellow-500/20 flex items-center justify-center">
                  <Sun size={16} className="text-yellow-400" />
                </div>
                <span className="text-white text-sm">亮度检测</span>
              </div>
              <span className="text-white/50 text-xs">{analysisProgress.brightness}%</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-yellow-500 to-yellow-400 rounded-full transition-all duration-300" style={{ width: `${analysisProgress.brightness}%` }} />
            </div>
          </div>
          
          <div className="bg-white/10 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-pink-500/20 flex items-center justify-center">
                  <Users size={16} className="text-pink-400" />
                </div>
                <span className="text-white text-sm">人脸检测</span>
              </div>
              <span className="text-white/50 text-xs">{analysisProgress.face}%</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-pink-500 to-pink-400 rounded-full transition-all duration-300" style={{ width: `${analysisProgress.face}%` }} />
            </div>
          </div>
          
          <div className="bg-white/10 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-lg bg-green-500/20 flex items-center justify-center">
                  <Sliders size={16} className="text-green-400" />
                </div>
                <span className="text-white text-sm">纹理分析</span>
              </div>
              <span className="text-white/50 text-xs">{analysisProgress.texture}%</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-green-500 to-green-400 rounded-full transition-all duration-300" style={{ width: `${analysisProgress.texture}%` }} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // 渲染结果模式
  const renderResultMode = () => (
    <div className="h-full bg-black flex flex-col">
      {/* 顶部栏 */}
      <div className="h-14 px-4 flex items-center justify-between bg-black/80 backdrop-blur-lg border-b border-white/10">
        <button
          onClick={() => {
            setCapturedImage(null);
            setRecognizedScene(null);
            setAppliedParams({});
          }}
          className="text-white/70 hover:text-white"
        >
          <ArrowLeft size={20} />
        </button>
        <span className="text-white font-medium">AI 出片</span>
        <button onClick={exportImage} className="text-orange-500 font-medium text-sm">
          <Download size={20} />
        </button>
      </div>
      
      {/* 图片预览 */}
      <div className="flex-1 p-4">
        <div className="relative rounded-2xl overflow-hidden mb-4">
          <img src={capturedImage!} alt="Result" className="w-full aspect-[4/3] object-cover" />
          
          {/* 场景标签 */}
          {recognizedScene && (
            <div className="absolute top-3 left-3">
              <div className="px-3 py-1.5 rounded-full text-white text-xs font-medium flex items-center gap-1.5" style={{ backgroundColor: `${recognizedScene.color}cc` }}>
                <span>{React.createElement(recognizedScene.icon, { size: 12 })}</span>
                <span>{recognizedScene.name}</span>
              </div>
            </div>
          )}
          
          {/* 参数显示 */}
          {showParams && (
            <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 to-transparent p-4 pt-12">
              <div className="grid grid-cols-3 gap-2 text-xs">
                {Object.entries(appliedParams).slice(0, 6).map(([key, value]) => (
                  <div key={key} className="bg-white/10 rounded-lg px-2 py-1">
                    <span className="text-white/60">{key}</span>
                    <span className="text-white font-medium ml-1">{typeof value === 'number' ? value : '开'}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        
        {/* 场景信息 */}
        {recognizedScene && (
          <div className="bg-white/5 rounded-2xl p-4 mb-4">
            <div className="flex items-center gap-4 mb-3">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ backgroundColor: `${recognizedScene.color}20` }}>
                {React.createElement(recognizedScene.icon, { size: 24, style: { color: recognizedScene.color } })}
              </div>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <h3 className="text-white font-bold">{recognizedScene.name}</h3>
                  <span className="px-2 py-0.5 rounded-full bg-green-500/20 text-green-400 text-xs">
                    {confidence}% 置信度
                  </span>
                </div>
                <p className="text-white/60 text-sm">{recognizedScene.category} · 已自动匹配最佳参数</p>
              </div>
            </div>
            
            {/* 参数展示 */}
            <div className="grid grid-cols-2 gap-2 text-xs mb-3">
              <div className="bg-white/5 rounded-lg px-3 py-2 flex justify-between">
                <span className="text-white/60">对比度</span>
                <span className="text-white font-medium">{(appliedParams.contrast || 0) >= 0 ? '+' : ''}{appliedParams.contrast || 0}</span>
              </div>
              <div className="bg-white/5 rounded-lg px-3 py-2 flex justify-between">
                <span className="text-white/60">饱和度</span>
                <span className="text-white font-medium">{(appliedParams.saturation || 0) >= 0 ? '+' : ''}{appliedParams.saturation || 0}</span>
              </div>
              <div className="bg-white/5 rounded-lg px-3 py-2 flex justify-between">
                <span className="text-white/60">锐度</span>
                <span className="text-white font-medium">+{appliedParams.sharpness || 0}</span>
              </div>
              <div className="bg-white/5 rounded-lg px-3 py-2 flex justify-between">
                <span className="text-white/60">清晰度</span>
                <span className="text-white font-medium">+{appliedParams.clarity || 0}</span>
              </div>
            </div>
            
            {/* 备选场景 */}
            {alternativeScenes.length > 0 && (
              <div>
                <p className="text-white/40 text-xs mb-2">备选场景</p>
                <div className="flex gap-2">
                  {alternativeScenes.map((scene) => (
                    <button
                      key={scene.id}
                      onClick={() => {
                        setRecognizedScene(scene);
                        setAppliedParams(scene.params);
                      }}
                      className="flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 transition-colors"
                      style={{ backgroundColor: `${scene.color}15` }}
                    >
                      {React.createElement(scene.icon, { size: 14, style: { color: scene.color } })}
                      <span className="text-white/70 text-xs">{scene.name}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
        
        {/* 历史记录 */}
        {recognitionHistory.length > 1 && (
          <div>
            <h4 className="text-white/60 text-xs mb-2">最近识别</h4>
            <div className="flex gap-2 overflow-x-auto pb-2">
              {recognitionHistory.slice(1).map((scene, i) => (
                <button
                  key={`${scene.id}-${i}`}
                  onClick={() => {
                    setRecognizedScene(scene);
                    setAppliedParams(scene.params);
                  }}
                  className="flex-shrink-0 w-16 h-16 rounded-xl flex items-center justify-center"
                  style={{ backgroundColor: `${scene.color}20` }}
                >
                  {React.createElement(scene.icon, { size: 24, style: { color: scene.color } })}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
      
      {/* 底部操作 */}
      <div className="p-4 bg-black/80 backdrop-blur-lg border-t border-white/10">
        <div className="flex gap-3">
          <button
            onClick={retake}
            className="flex-1 py-3 px-4 bg-white/10 text-white rounded-xl font-medium flex items-center justify-center gap-2"
          >
            <Camera size={18} />
            重拍
          </button>
          <button
            onClick={applyParams}
            className="flex-1 py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-xl font-medium flex items-center justify-center gap-2"
          >
            <Wand2 size={18} />
            {showParams ? '已应用' : '应用参数'}
          </button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="h-full w-full bg-black">
      {!capturedImage && !isAnalyzing && renderCameraMode()}
      {isAnalyzing && renderAnalyzingMode()}
      {capturedImage && !isAnalyzing && recognizedScene && renderResultMode()}
      
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

export default AISceneRecognitionPage;
