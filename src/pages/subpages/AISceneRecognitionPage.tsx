import React, { useState, useCallback, useRef, useMemo } from 'react';
import { useAppStore } from '../../store/appStore';
import { 
  ArrowLeft, Camera, Zap, Sun, Moon, Mountain, Users, Utensils, Car, Building, 
  Flower, Waves, Sparkles, Check, Target, Wand2, ChevronRight, Info,
  CloudRain, Leaf, Dog, Cat, Bird, Fish as FishIcon,
  Coffee, Wine, Cake, Pizza, Salad, IceCream, Gift, Music,
  Book, Home, TreePalm, Sunrise, Sunset, Cloud,
  Snowflake, Droplets, Eye, Layers,
  Grid, List, Search, X, Heart, Clock
} from 'lucide-react';

// 50+ 精细场景类型定义
const SCENE_TYPES = [
  // 人像系列 (8种)
  { id: 'portrait', name: '人像', category: 'portrait', icon: Users, color: '#FF6B9D', confidence: 0, params: { saturation: 5, contrast: 8, warmth: 3, sharpness: 15, skinSmooth: 20 }, desc: '优化肤色，自然美化' },
  { id: 'portrait-backlit', name: '逆光人像', category: 'portrait', icon: Sun, color: '#FFB347', confidence: 0, params: { saturation: 8, contrast: 15, warmth: -5, sharpness: 12, highlights: -20 }, desc: '保留逆光氛围，提亮面部' },
  { id: 'portrait-studio', name: '棚拍人像', category: 'portrait', icon: Camera, color: '#C9A0DC', confidence: 0, params: { saturation: 0, contrast: 12, warmth: 0, sharpness: 25, clarity: 15 }, desc: '专业影棚质感' },
  { id: 'portrait-group', name: '合影', category: 'portrait', icon: Users, color: '#DDA0DD', confidence: 0, params: { saturation: 8, contrast: 10, warmth: 5, sharpness: 18 }, desc: '多人合影优化' },
  { id: 'portrait-candid', name: '抓拍人像', category: 'portrait', icon: Zap, color: '#FF69B4', confidence: 0, params: { saturation: 10, contrast: 12, warmth: 3, sharpness: 20, grain: 5 }, desc: '保留自然瞬间感' },
  { id: 'portrait-silhouette', name: '剪影人像', category: 'portrait', icon: Moon, color: '#483D8B', confidence: 0, params: { saturation: 15, contrast: 40, warmth: 10, shadows: -30 }, desc: '强化剪影效果' },
  { id: 'portrait-bw', name: '黑白人像', category: 'portrait', icon: Eye, color: '#808080', confidence: 0, params: { saturation: -100, contrast: 20, warmth: 0, sharpness: 25, clarity: 10 }, desc: '经典黑白人像风格' },
  { id: 'portrait-beauty', name: '美妆人像', category: 'portrait', icon: Sparkles, color: '#FF1493', confidence: 0, params: { saturation: 12, contrast: 8, warmth: 5, sharpness: 15, skinSmooth: 35, vibrance: 10 }, desc: '美妆广告风格' },
  
  // 风景系列 (10种)
  { id: 'landscape', name: '风景', category: 'landscape', icon: Mountain, color: '#4ECDC4', confidence: 0, params: { saturation: 15, contrast: 10, warmth: 5, sharpness: 20, clarity: 10 }, desc: '增强自然色彩' },
  { id: 'landscape-sunset', name: '日落', category: 'landscape', icon: Sunset, color: '#FF7F50', confidence: 0, params: { saturation: 25, contrast: 15, warmth: 20, sharpness: 15, vibrance: 10 }, desc: '强化日落暖调' },
  { id: 'landscape-sunrise', name: '日出', category: 'landscape', icon: Sunrise, color: '#FFD700', confidence: 0, params: { saturation: 20, contrast: 12, warmth: 15, sharpness: 18, dehaze: 10 }, desc: '清新日出氛围' },
  { id: 'landscape-blue-sky', name: '蓝天白云', category: 'landscape', icon: Cloud, color: '#87CEEB', confidence: 0, params: { saturation: 12, contrast: 8, warmth: -10, sharpness: 18, dehaze: 15 }, desc: '增强天空通透感' },
  { id: 'landscape-forest', name: '森林', category: 'landscape', icon: TreePalm, color: '#228B22', confidence: 0, params: { saturation: 18, contrast: 12, warmth: 5, sharpness: 22, vibrance: 15 }, desc: '浓郁森林绿意' },
  { id: 'landscape-mountain', name: '山峦', category: 'landscape', icon: Mountain, color: '#6B8E23', confidence: 0, params: { saturation: 10, contrast: 15, warmth: 0, sharpness: 25, clarity: 20 }, desc: '山峦层次感' },
  { id: 'landscape-desert', name: '沙漠', category: 'landscape', icon: Sun, color: '#DEB887', confidence: 0, params: { saturation: 5, contrast: 20, warmth: 25, sharpness: 18 }, desc: '沙漠金色质感' },
  { id: 'landscape-autumn', name: '秋景', category: 'landscape', icon: Leaf, color: '#D2691E', confidence: 0, params: { saturation: 30, contrast: 15, warmth: 15, sharpness: 20 }, desc: '浓郁秋色' },
  { id: 'landscape-spring', name: '春景', category: 'landscape', icon: Flower, color: '#90EE90', confidence: 0, params: { saturation: 20, contrast: 8, warmth: 5, sharpness: 15, vibrance: 15 }, desc: '清新春日气息' },
  { id: 'landscape-panorama', name: '全景', category: 'landscape', icon: Grid, color: '#4682B4', confidence: 0, params: { saturation: 12, contrast: 10, warmth: 0, sharpness: 20, dehaze: 10 }, desc: '全景风光优化' },
  
  // 夜景系列 (6种)
  { id: 'night', name: '夜景', category: 'night', icon: Moon, color: '#483D8B', confidence: 0, params: { saturation: -5, contrast: 20, warmth: 10, sharpness: 25, noise: -15 }, desc: '夜景降噪增强' },
  { id: 'night-city', name: '城市夜景', category: 'night', icon: Building, color: '#9370DB', confidence: 0, params: { saturation: 10, contrast: 25, warmth: 15, sharpness: 20, highlights: -10 }, desc: '城市灯光璀璨' },
  { id: 'night-portrait', name: '夜景人像', category: 'night', icon: Users, color: '#DDA0DD', confidence: 0, params: { saturation: 5, contrast: 15, warmth: 8, sharpness: 18, skinSmooth: 15 }, desc: '夜景人像补光' },
  { id: 'night-starry', name: '星空', category: 'night', icon: Sparkles, color: '#191970', confidence: 0, params: { saturation: 10, contrast: 30, warmth: -5, sharpness: 15, noise: -20, clarity: 25 }, desc: '银河星空增强' },
  { id: 'night-aurora', name: '极光', category: 'night', icon: Waves, color: '#00FF7F', confidence: 0, params: { saturation: 40, contrast: 20, warmth: -10, sharpness: 25, vibrance: 30 }, desc: '极光色彩强化' },
  { id: 'night-light-trail', name: '光轨', category: 'night', icon: Zap, color: '#FF4500', confidence: 0, params: { saturation: 15, contrast: 25, warmth: 5, sharpness: 20, highlights: -5 }, desc: '车流光轨增强' },
  
  // 美食系列 (8种)
  { id: 'food', name: '美食', category: 'food', icon: Utensils, color: '#FF6347', confidence: 0, params: { saturation: 20, contrast: 5, warmth: 10, sharpness: 30, brightness: 5 }, desc: '美食诱人色彩' },
  { id: 'food-restaurant', name: '餐厅美食', category: 'food', icon: Coffee, color: '#CD853F', confidence: 0, params: { saturation: 15, contrast: 8, warmth: 15, sharpness: 25, vignette: -10 }, desc: '餐厅氛围感' },
  { id: 'food-dessert', name: '甜点', category: 'food', icon: Cake, color: '#FFB6C1', confidence: 0, params: { saturation: 25, contrast: 5, warmth: 8, sharpness: 20, brightness: 10 }, desc: '甜点清新风格' },
  { id: 'food-drink', name: '饮品', category: 'food', icon: Wine, color: '#8B4513', confidence: 0, params: { saturation: 18, contrast: 10, warmth: 5, sharpness: 22, clarity: 10 }, desc: '饮品通透感' },
  { id: 'food-chinese', name: '中餐', category: 'food', icon: Utensils, color: '#DC143C', confidence: 0, params: { saturation: 22, contrast: 12, warmth: 15, sharpness: 28 }, desc: '中餐热气腾腾' },
  { id: 'food-japanese', name: '日料', category: 'food', icon: Salad, color: '#FF6347', confidence: 0, params: { saturation: 15, contrast: 8, warmth: 0, sharpness: 25, clarity: 15 }, desc: '日料精致质感' },
  { id: 'food-western', name: '西餐', category: 'food', icon: Pizza, color: '#DAA520', confidence: 0, params: { saturation: 18, contrast: 10, warmth: 10, sharpness: 22 }, desc: '西餐优雅风格' },
  { id: 'food-icecream', name: '冰淇淋', category: 'food', icon: IceCream, color: '#87CEEB', confidence: 0, params: { saturation: 30, contrast: 5, warmth: -5, sharpness: 18, brightness: 8 }, desc: '冰淇淋清爽感' },
  
  // 动物系列 (6种)
  { id: 'pet', name: '宠物', category: 'animal', icon: Dog, color: '#D2691E', confidence: 0, params: { saturation: 10, contrast: 8, warmth: 5, sharpness: 22, clarity: 10 }, desc: '宠物毛发细节' },
  { id: 'pet-dog', name: '狗狗', category: 'animal', icon: Dog, color: '#8B4513', confidence: 0, params: { saturation: 12, contrast: 10, warmth: 8, sharpness: 25 }, desc: '狗狗活力感' },
  { id: 'pet-cat', name: '猫咪', category: 'animal', icon: Cat, color: '#FF8C00', confidence: 0, params: { saturation: 8, contrast: 8, warmth: 5, sharpness: 22, skinSmooth: 10 }, desc: '猫咪柔和感' },
  { id: 'bird', name: '鸟类', category: 'animal', icon: Bird, color: '#1E90FF', confidence: 0, params: { saturation: 20, contrast: 12, warmth: 0, sharpness: 30, clarity: 15 }, desc: '鸟类羽毛细节' },
  { id: 'fish', name: '鱼类', category: 'animal', icon: FishIcon, color: '#00CED1', confidence: 0, params: { saturation: 25, contrast: 10, warmth: -5, sharpness: 20 }, desc: '水中鱼类通透' },
  { id: 'wildlife', name: '野生动物', category: 'animal', icon: Leaf, color: '#556B2F', confidence: 0, params: { saturation: 15, contrast: 15, warmth: 5, sharpness: 28, clarity: 20 }, desc: '野性自然感' },
  
  // 建筑/城市系列 (6种)
  { id: 'architecture', name: '建筑', category: 'architecture', icon: Building, color: '#4682B4', confidence: 0, params: { saturation: 5, contrast: 15, warmth: -5, sharpness: 25, clarity: 20 }, desc: '建筑线条清晰' },
  { id: 'architecture-modern', name: '现代建筑', category: 'architecture', icon: Building, color: '#708090', confidence: 0, params: { saturation: 0, contrast: 18, warmth: -10, sharpness: 30, clarity: 25 }, desc: '现代简约风格' },
  { id: 'architecture-classic', name: '古典建筑', category: 'architecture', icon: Home, color: '#8B4513', confidence: 0, params: { saturation: 10, contrast: 12, warmth: 10, sharpness: 22 }, desc: '古典韵味' },
  { id: 'street', name: '街拍', category: 'architecture', icon: Car, color: '#708090', confidence: 0, params: { saturation: 8, contrast: 12, warmth: 0, sharpness: 20, grain: 10 }, desc: '街拍胶片感' },
  { id: 'urban', name: '城市风光', category: 'architecture', icon: Building, color: '#2F4F4F', confidence: 0, params: { saturation: 10, contrast: 15, warmth: 5, sharpness: 22, dehaze: 10 }, desc: '城市大气感' },
  { id: 'interior', name: '室内', category: 'architecture', icon: Home, color: '#DEB887', confidence: 0, params: { saturation: 5, contrast: 8, warmth: 10, sharpness: 18, brightness: 5 }, desc: '室内温馨感' },
  
  // 自然系列 (6种)
  { id: 'flower', name: '花卉', category: 'nature', icon: Flower, color: '#FF69B4', confidence: 0, params: { saturation: 25, contrast: 5, warmth: 5, sharpness: 15, vibrance: 15 }, desc: '花卉鲜艳色彩' },
  { id: 'beach', name: '海滩', category: 'nature', icon: Waves, color: '#00CED1', confidence: 0, params: { saturation: 18, contrast: 10, warmth: -5, sharpness: 15, dehaze: 10 }, desc: '海滩清爽感' },
  { id: 'snow', name: '雪景', category: 'nature', icon: Snowflake, color: '#E0FFFF', confidence: 0, params: { saturation: -10, contrast: 15, warmth: -15, sharpness: 20, brightness: 5 }, desc: '雪景纯净感' },
  { id: 'rain', name: '雨天', category: 'nature', icon: CloudRain, color: '#708090', confidence: 0, params: { saturation: 5, contrast: 15, warmth: -5, sharpness: 18, dehaze: 15 }, desc: '雨天氛围感' },
  { id: 'waterfall', name: '瀑布', category: 'nature', icon: Droplets, color: '#4169E1', confidence: 0, params: { saturation: 15, contrast: 12, warmth: 0, sharpness: 20, clarity: 15 }, desc: '瀑布动感' },
  { id: 'lake', name: '湖泊', category: 'nature', icon: Waves, color: '#5F9EA0', confidence: 0, params: { saturation: 12, contrast: 10, warmth: -5, sharpness: 18, dehaze: 10 }, desc: '湖泊宁静感' },
  
  // 其他系列 (6种)
  { id: 'document', name: '文档', category: 'other', icon: Book, color: '#A9A9A9', confidence: 0, params: { saturation: -100, contrast: 30, warmth: 0, sharpness: 40, brightness: 10 }, desc: '文档清晰扫描' },
  { id: 'product', name: '产品', category: 'other', icon: Gift, color: '#FFD700', confidence: 0, params: { saturation: 15, contrast: 10, warmth: 0, sharpness: 30, brightness: 5 }, desc: '产品商业质感' },
  { id: 'vehicle', name: '车辆', category: 'other', icon: Car, color: '#C0C0C0', confidence: 0, params: { saturation: 10, contrast: 15, warmth: 0, sharpness: 28, clarity: 15 }, desc: '车辆金属质感' },
  { id: 'sports', name: '运动', category: 'other', icon: Zap, color: '#FF4500', confidence: 0, params: { saturation: 20, contrast: 15, warmth: 5, sharpness: 25 }, desc: '运动活力感' },
  { id: 'concert', name: '演唱会', category: 'other', icon: Music, color: '#9400D3', confidence: 0, params: { saturation: 25, contrast: 20, warmth: 10, sharpness: 22, noise: -10 }, desc: '演唱会灯光' },
  { id: 'wedding', name: '婚礼', category: 'other', icon: Heart, color: '#FFB6C1', confidence: 0, params: { saturation: 10, contrast: 8, warmth: 8, sharpness: 18, skinSmooth: 20 }, desc: '婚礼浪漫感' },
];

// 场景分类
const SCENE_CATEGORIES = [
  { id: 'all', name: '全部', icon: Grid, count: SCENE_TYPES.length },
  { id: 'portrait', name: '人像', icon: Users, count: SCENE_TYPES.filter(s => s.category === 'portrait').length },
  { id: 'landscape', name: '风景', icon: Mountain, count: SCENE_TYPES.filter(s => s.category === 'landscape').length },
  { id: 'night', name: '夜景', icon: Moon, count: SCENE_TYPES.filter(s => s.category === 'night').length },
  { id: 'food', name: '美食', icon: Utensils, count: SCENE_TYPES.filter(s => s.category === 'food').length },
  { id: 'animal', name: '动物', icon: Dog, count: SCENE_TYPES.filter(s => s.category === 'animal').length },
  { id: 'architecture', name: '建筑', icon: Building, count: SCENE_TYPES.filter(s => s.category === 'architecture').length },
  { id: 'nature', name: '自然', icon: Flower, count: SCENE_TYPES.filter(s => s.category === 'nature').length },
  { id: 'other', name: '其他', icon: Sparkles, count: SCENE_TYPES.filter(s => s.category === 'other').length },
];

// 识别历史记录
interface RecognitionHistory {
  id: string;
  sceneName: string;
  sceneId: string;
  timestamp: Date;
  imageUrl: string;
  confidence: number;
  params: Record<string, number>;
}

// 多场景识别结果
interface MultiSceneResult {
  primary: typeof SCENE_TYPES[0];
  secondary: typeof SCENE_TYPES[0] | null;
  confidence: number;
  reason: string;
}

const AISceneRecognitionPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [isScanning, setIsScanning] = useState(false);
  const [scanProgress, setScanProgress] = useState(0);
  const [scanStage, setScanStage] = useState('');
  const [recognizedScene, setRecognizedScene] = useState<MultiSceneResult | null>(null);
  const [showResult, setShowResult] = useState(false);
  const [history, setHistory] = useState<RecognitionHistory[]>([]);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [showParams, setShowParams] = useState(false);
  const [showCompare, setShowCompare] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [searchQuery, setSearchQuery] = useState('');
  const [showHistory, setShowHistory] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 过滤场景
  const filteredScenes = useMemo(() => {
    let result = SCENE_TYPES;
    if (selectedCategory !== 'all') {
      result = result.filter(s => s.category === selectedCategory);
    }
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(s => 
        s.name.toLowerCase().includes(q) || 
        s.desc.toLowerCase().includes(q)
      );
    }
    return result;
  }, [selectedCategory, searchQuery]);

  // 模拟场景识别（企业级）
  const simulateRecognition = useCallback(() => {
    setIsScanning(true);
    setScanProgress(0);
    setShowResult(false);
    setRecognizedScene(null);

    const stages = [
      { progress: 15, stage: '分析图像特征...' },
      { progress: 30, stage: '检测主体对象...' },
      { progress: 50, stage: '识别场景类型...' },
      { progress: 70, stage: '分析光照条件...' },
      { progress: 85, stage: '匹配最佳参数...' },
      { progress: 100, stage: '生成优化方案...' },
    ];

    let stageIndex = 0;
    const progressInterval = setInterval(() => {
      if (stageIndex < stages.length) {
        setScanProgress(stages[stageIndex].progress);
        setScanStage(stages[stageIndex].stage);
        stageIndex++;
      }
    }, 250);

    setTimeout(() => {
      clearInterval(progressInterval);
      setScanProgress(100);
      setScanStage('识别完成');
      
      // 随机选择主场景和次场景
      const shuffled = [...SCENE_TYPES].sort(() => Math.random() - 0.5);
      const primary = shuffled[0];
      const secondary = Math.random() > 0.5 ? shuffled[1] : null;
      const randomConfidence = 0.88 + Math.random() * 0.11;
      
      // 生成推荐理由
      const reasons = [
        `检测到${primary.name}特征，建议优化${primary.desc}`,
        `AI分析显示${primary.name}场景，自动调整参数以获得最佳效果`,
        `识别为${primary.name}场景，置信度${(randomConfidence * 100).toFixed(1)}%`,
      ];
      
      setRecognizedScene({
        primary: { ...primary, confidence: randomConfidence },
        secondary: secondary ? { ...secondary, confidence: randomConfidence * 0.6 } : null,
        confidence: randomConfidence,
        reason: reasons[Math.floor(Math.random() * reasons.length)],
      });
      setIsScanning(false);
      
      setTimeout(() => setShowResult(true), 300);
    }, 1800);
  }, []);

  // 选择图片
  const handleImageSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const url = URL.createObjectURL(file);
      setSelectedImage(url);
      simulateRecognition();
    }
  }, [simulateRecognition]);

  // 应用参数
  const handleApplyParams = useCallback(() => {
    if (recognizedScene) {
      const newHistory: RecognitionHistory = {
        id: Date.now().toString(),
        sceneName: recognizedScene.primary.name,
        sceneId: recognizedScene.primary.id,
        timestamp: new Date(),
        imageUrl: selectedImage || 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
        confidence: recognizedScene.confidence,
        params: recognizedScene.primary.params,
      };
      setHistory(prev => [newHistory, ...prev].slice(0, 20));
      setShowParams(true);
      setTimeout(() => setShowParams(false), 2000);
    }
  }, [recognizedScene, selectedImage]);

  // 快速选择示例图片
  const sampleImages = [
    { url: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400', label: '人像' },
    { url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400', label: '风景' },
    { url: 'https://images.unsplash.com/photo-1514525458?w=400', label: '夜景' },
    { url: 'https://images.unsplash.com/photo-1504677307707-6b8a9e87f5c2?w=400', label: '美食' },
    { url: 'https://images.unsplash.com/photo-1518882606490-4051f1b2b536?w=400', label: '建筑' },
    { url: 'https://images.unsplash.com/photo-1518715347-4d7e4c2f1d88?w=400', label: '宠物' },
  ];

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white">
      {/* Header */}
      <div className="sticky top-0 z-50 bg-[#0a0a0a]/95 backdrop-blur-sm border-b border-white/5">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10">
              <ArrowLeft size={20} className="text-white" />
            </button>
            <div>
              <h1 className="text-lg font-bold">AI 场景识别</h1>
              <p className="text-xs text-white/50">智能识别 {SCENE_TYPES.length}+ 场景类型</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowHistory(!showHistory)}
              className={`p-2 rounded-full ${showHistory ? 'bg-[#FF6B35]/20' : 'hover:bg-white/10'}`}
            >
              <Clock size={18} className={showHistory ? 'text-[#FF6B35]' : 'text-white/50'} />
            </button>
            <div className="px-2 py-1 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] text-white text-xs font-bold">
              v4.0 Pro
            </div>
          </div>
        </div>
      </div>

      {/* History Panel */}
      {showHistory && (
        <div className="px-4 py-4 border-b border-white/5 bg-[#0a0a0a]">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-white/70 text-sm font-medium">识别历史 ({history.length})</h3>
            {history.length > 0 && (
              <button onClick={() => setHistory([])} className="text-red-400 text-xs">清空</button>
            )}
          </div>
          {history.length > 0 ? (
            <div className="space-y-2 max-h-48 overflow-y-auto">
              {history.slice(0, 5).map((item) => (
                <div key={item.id} className="flex items-center gap-3 p-2 rounded-xl bg-white/5">
                  <img src={item.imageUrl} alt="" className="w-12 h-12 rounded-lg object-cover" />
                  <div className="flex-1 min-w-0">
                    <p className="text-white text-sm font-medium truncate">{item.sceneName}</p>
                    <p className="text-white/40 text-xs">
                      {(item.confidence * 100).toFixed(1)}% · {item.timestamp.toLocaleTimeString()}
                    </p>
                  </div>
                  <ChevronRight size={16} className="text-white/30" />
                </div>
              ))}
            </div>
          ) : (
            <p className="text-white/30 text-xs text-center py-4">暂无识别历史</p>
          )}
        </div>
      )}

      {/* Camera Preview Area */}
      <div className="px-4 py-4">
        <div className="relative aspect-[4/3] rounded-2xl overflow-hidden bg-[#1a1a1a]">
          {/* Preview Image */}
          {selectedImage ? (
            <img src={selectedImage} alt="Preview" className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-gray-900 to-gray-800">
              <Camera size={48} className="text-white/30 mb-4" />
              <p className="text-white/50 text-sm mb-2">选择或拍摄照片开始识别</p>
              <p className="text-white/30 text-xs">支持 50+ 场景智能识别</p>
            </div>
          )}

          {/* Compare Mode */}
          {showCompare && selectedImage && (
            <div className="absolute inset-0 flex">
              <div className="w-1/2 border-r-2 border-white overflow-hidden">
                <img src={selectedImage} alt="Original" className="w-full h-full object-cover" />
                <div className="absolute bottom-2 left-2 px-2 py-1 rounded bg-black/50 text-xs">原图</div>
              </div>
              <div className="w-1/2 overflow-hidden">
                <img src={selectedImage} alt="Effect" className="w-full h-full object-cover" style={{ filter: 'saturate(1.2) contrast(1.1)' }} />
                <div className="absolute bottom-2 right-2 px-2 py-1 rounded bg-black/50 text-xs">效果</div>
              </div>
            </div>
          )}

          {/* Scanning Overlay */}
          {isScanning && (
            <div className="absolute inset-0 bg-black/70 flex flex-col items-center justify-center">
              {/* Scan Animation */}
              <div className="relative w-32 h-32 mb-4">
                <div className="absolute inset-0 rounded-full border-4 border-white/20" />
                <div className="absolute inset-0 rounded-full border-4 border-transparent border-t-[#FF6B35] animate-spin" style={{ animationDuration: '0.8s' }} />
                <div className="absolute inset-2 rounded-full border-4 border-transparent border-b-[#FF8C42] animate-spin" style={{ animationDuration: '1.2s', animationDirection: 'reverse' }} />
                <div className="absolute inset-4 rounded-full bg-[#FF6B35]/20 flex items-center justify-center">
                  <Target size={32} className="text-[#FF6B35]" />
                </div>
              </div>
              <p className="text-white text-sm font-medium mb-2">{scanStage}</p>
              <div className="w-48 h-2 bg-white/10 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] transition-all duration-200 rounded-full"
                  style={{ width: `${scanProgress}%` }}
                />
              </div>
              <p className="text-white/50 text-xs mt-2">{Math.round(scanProgress)}%</p>
              
              {/* AI Processing Steps */}
              <div className="mt-4 flex gap-2">
                {['特征', '主体', '场景', '光照', '参数'].map((step, idx) => (
                  <div 
                    key={step}
                    className={`px-2 py-1 rounded-full text-[10px] transition-all ${
                      scanProgress > idx * 20 
                        ? 'bg-[#FF6B35]/20 text-[#FF6B35]' 
                        : 'bg-white/5 text-white/30'
                    }`}
                  >
                    {step}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Recognition Result Overlay */}
          {showResult && recognizedScene && (
            <div className="absolute inset-0 bg-gradient-to-t from-black/95 via-black/60 to-transparent">
              {/* Scene Badge */}
              <div className="absolute top-4 left-4 right-4">
                <div className="p-4 rounded-2xl bg-white/10 backdrop-blur-md border border-white/10">
                  {/* Primary Scene */}
                  <div className="flex items-center gap-3 mb-3">
                    <div 
                      className="w-14 h-14 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${recognizedScene.primary.color}30` }}
                    >
                      <recognizedScene.primary.icon size={28} style={{ color: recognizedScene.primary.color }} />
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <h3 className="text-white font-bold text-lg">{recognizedScene.primary.name}</h3>
                        <span className="px-2 py-0.5 rounded-full bg-[#FF6B35]/20 text-[#FF6B35] text-xs font-medium">
                          主场景
                        </span>
                      </div>
                      <p className="text-white/50 text-xs mt-0.5">{recognizedScene.primary.desc}</p>
                    </div>
                  </div>
                  
                  {/* Confidence Bar */}
                  <div className="mb-3">
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-white/50 text-xs">识别置信度</span>
                      <span className="text-[#FF6B35] text-sm font-bold">{(recognizedScene.confidence * 100).toFixed(1)}%</span>
                    </div>
                    <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                      <div 
                        className="h-full rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF8C42]"
                        style={{ width: `${recognizedScene.confidence * 100}%` }}
                      />
                    </div>
                  </div>
                  
                  {/* Secondary Scene */}
                  {recognizedScene.secondary && (
                    <div className="flex items-center gap-2 p-2 rounded-xl bg-white/5 mb-3">
                      <recognizedScene.secondary.icon size={16} style={{ color: recognizedScene.secondary.color }} />
                      <span className="text-white/70 text-xs">叠加场景: {recognizedScene.secondary.name}</span>
                      <span className="text-white/40 text-xs ml-auto">
                        {(recognizedScene.secondary.confidence * 100).toFixed(0)}%
                      </span>
                    </div>
                  )}
                  
                  {/* Reason */}
                  <div className="p-2 rounded-lg bg-[#FF6B35]/10 border border-[#FF6B35]/20">
                    <p className="text-white/80 text-xs flex items-start gap-2">
                      <Info size={14} className="text-[#FF6B35] flex-shrink-0 mt-0.5" />
                      {recognizedScene.reason}
                    </p>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="absolute bottom-4 left-4 right-4">
                <div className="flex gap-3">
                  <button
                    onClick={() => setShowParams(true)}
                    className="flex-1 py-3 rounded-xl bg-white/10 backdrop-blur-sm flex items-center justify-center gap-2 text-white font-medium border border-white/10"
                  >
                    <Wand2 size={18} />
                    <span>查看参数</span>
                  </button>
                  <button
                    onClick={handleApplyParams}
                    className="flex-1 py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium"
                  >
                    <Check size={18} />
                    <span>应用参数</span>
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Camera Frame Guide */}
          {!selectedImage && (
            <div className="absolute inset-4 border-2 border-dashed border-white/20 rounded-xl pointer-events-none">
              <div className="absolute top-0 left-0 w-8 h-8 border-l-2 border-t-2 border-[#FF6B35] rounded-tl-lg" />
              <div className="absolute top-0 right-0 w-8 h-8 border-r-2 border-t-2 border-[#FF6B35] rounded-tr-lg" />
              <div className="absolute bottom-0 left-0 w-8 h-8 border-l-2 border-b-2 border-[#FF6B35] rounded-bl-lg" />
              <div className="absolute bottom-0 right-0 w-8 h-8 border-r-2 border-b-2 border-[#FF6B35] rounded-br-lg" />
            </div>
          )}
          
          {/* Compare Toggle */}
          {selectedImage && !isScanning && !showResult && (
            <button
              onClick={() => setShowCompare(!showCompare)}
              className={`absolute top-3 right-3 p-2 rounded-full ${showCompare ? 'bg-[#FF6B35]' : 'bg-black/50'}`}
            >
              <Layers size={18} className="text-white" />
            </button>
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="px-4 pb-4">
        <div className="flex gap-3">
          <button
            onClick={() => fileInputRef.current?.click()}
            className="flex-1 py-3 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center gap-2 text-white/70 font-medium hover:bg-white/10 transition-colors"
          >
            <Camera size={18} />
            <span>选择照片</span>
          </button>
          <button
            onClick={simulateRecognition}
            disabled={!selectedImage || isScanning}
            className="flex-1 py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium disabled:opacity-50"
          >
            <Zap size={18} />
            <span>开始识别</span>
          </button>
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleImageSelect}
          className="hidden"
        />
      </div>

      {/* Sample Images */}
      <div className="px-4 pb-4">
        <h3 className="text-white/50 text-xs mb-3">快速体验示例</h3>
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          {sampleImages.map((img, idx) => (
            <button
              key={idx}
              onClick={() => {
                setSelectedImage(img.url);
                simulateRecognition();
              }}
              className="flex-shrink-0 relative w-20 h-20 rounded-xl overflow-hidden border-2 border-transparent hover:border-[#FF6B35] transition-colors"
            >
              <img src={img.url} alt={img.label} className="w-full h-full object-cover" />
              <div className="absolute bottom-0 inset-x-0 bg-black/50 text-[10px] text-center py-0.5">
                {img.label}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Scene Categories */}
      <div className="px-4 pb-3">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          {SCENE_CATEGORIES.map((cat) => {
            const Icon = cat.icon;
            return (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                className={`flex-shrink-0 px-3 py-2 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all ${
                  selectedCategory === cat.id
                    ? 'bg-[#FF6B35] text-white'
                    : 'bg-white/5 text-white/60 hover:bg-white/10'
                }`}
              >
                <Icon size={14} />
                {cat.name}
                <span className="text-white/40">({cat.count})</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Search */}
      <div className="px-4 pb-3">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索场景类型..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery('')} className="absolute right-3 top-1/2 -translate-y-1/2">
              <X size={14} className="text-white/40" />
            </button>
          )}
        </div>
      </div>

      {/* Scene Types Grid */}
      <div className="px-4 pb-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-white/50 text-xs">支持的场景类型 ({filteredScenes.length}种)</h3>
          <div className="flex gap-1">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-1.5 rounded-lg ${viewMode === 'grid' ? 'bg-white/10' : 'hover:bg-white/5'}`}
            >
              <Grid size={14} className="text-white/50" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-1.5 rounded-lg ${viewMode === 'list' ? 'bg-white/10' : 'hover:bg-white/5'}`}
            >
              <List size={14} className="text-white/50" />
            </button>
          </div>
        </div>
        
        {viewMode === 'grid' ? (
          <div className="grid grid-cols-4 gap-2">
            {filteredScenes.map((scene) => {
              const Icon = scene.icon;
              const isRecognized = recognizedScene?.primary.id === scene.id;
              return (
                <div
                  key={scene.id}
                  className={`p-3 rounded-xl text-center transition-all ${
                    isRecognized 
                      ? 'bg-[#FF6B35]/20 border border-[#FF6B35]/50' 
                      : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <Icon size={20} className="mx-auto mb-1" style={{ color: scene.color }} />
                  <span className="text-[10px] text-white/70">{scene.name}</span>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="space-y-2">
            {filteredScenes.map((scene) => {
              const Icon = scene.icon;
              const isRecognized = recognizedScene?.primary.id === scene.id;
              return (
                <div
                  key={scene.id}
                  className={`flex items-center gap-3 p-3 rounded-xl transition-all ${
                    isRecognized 
                      ? 'bg-[#FF6B35]/20 border border-[#FF6B35]/50' 
                      : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ backgroundColor: `${scene.color}20` }}>
                    <Icon size={20} style={{ color: scene.color }} />
                  </div>
                  <div className="flex-1">
                    <p className="text-white text-sm font-medium">{scene.name}</p>
                    <p className="text-white/40 text-xs">{scene.desc}</p>
                  </div>
                  {isRecognized && (
                    <span className="px-2 py-1 rounded-full bg-[#FF6B35] text-white text-xs">已识别</span>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Params Detail Modal */}
      {showParams && recognizedScene && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/80 backdrop-blur-sm" onClick={() => setShowParams(false)}>
          <div 
            className="w-full max-w-md bg-[#1a1a1a] rounded-t-3xl p-6 max-h-[80vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="w-12 h-1 bg-white/20 rounded-full mx-auto mb-4" />
            
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ backgroundColor: `${recognizedScene.primary.color}30` }}>
                <recognizedScene.primary.icon size={24} style={{ color: recognizedScene.primary.color }} />
              </div>
              <div>
                <h2 className="text-lg font-bold">{recognizedScene.primary.name}</h2>
                <p className="text-white/50 text-xs">{recognizedScene.primary.desc}</p>
              </div>
            </div>
            
            <h3 className="text-white/70 text-sm mb-3">推荐参数</h3>
            <div className="space-y-2">
              {Object.entries(recognizedScene.primary.params).map(([key, value]) => (
                <div key={key} className="flex items-center justify-between p-3 rounded-xl bg-white/5">
                  <span className="text-white/70 text-sm capitalize">{key}</span>
                  <span className="text-[#FF6B35] font-bold">{value > 0 ? '+' : ''}{value}</span>
                </div>
              ))}
            </div>

            <button
              onClick={() => {
                handleApplyParams();
                setShowParams(false);
              }}
              className="w-full mt-4 py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] text-white font-medium"
            >
              应用到当前照片
            </button>
          </div>
        </div>
      )}

      {/* Success Toast */}
      {showParams && (
        <div className="fixed top-20 left-1/2 -translate-x-1/2 z-[60] px-4 py-2 rounded-full bg-green-500 text-white text-sm font-medium flex items-center gap-2">
          <Check size={16} />
          参数已应用
        </div>
      )}

      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default AISceneRecognitionPage;
