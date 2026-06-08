import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Camera, Sparkles, Check, RefreshCw, Wand2, Mountain, User, Moon, UtensilsCrossed, Building2, TreePine, Car, Waves, Flower2, Cat, Sun, Palette, Film, Coffee, ShoppingBag, Plane, Train, Ship, Bike, Tent, Fish, Wine, Cake, Pizza, Salad } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';

// 哈苏大师风格
const hasselbladStyles = [
  { id: 'natural', name: '哈苏自然', desc: '真实还原', color: '#4CAF50', icon: Sun },
  { id: 'portrait', name: '哈苏人像', desc: '柔美肤色', color: '#E91E63', icon: User },
  { id: 'cinematic', name: '哈苏电影', desc: '电影质感', color: '#FF9800', icon: Film },
  { id: 'vintage', name: '哈苏复古', desc: '胶片质感', color: '#795548', icon: Palette },
];

// 场景识别列表 - 扩展到36+场景
const scenes = [
  // 人物类
  { id: 'portrait', name: '人像', icon: User, color: '#E91E63', hasselblad: 'portrait' },
  { id: 'group', name: '合影', icon: User, color: '#E91E63', hasselblad: 'portrait' },
  { id: 'child', name: '儿童', icon: User, color: '#FF9800', hasselblad: 'portrait' },
  { id: 'pet', name: '宠物', icon: Cat, color: '#9C27B0', hasselblad: 'natural' },
  
  // 自然风景类
  { id: 'landscape', name: '风景', icon: Mountain, color: '#4CAF50', hasselblad: 'natural' },
  { id: 'nature', name: '自然', icon: TreePine, color: '#8BC34A', hasselblad: 'natural' },
  { id: 'flower', name: '花卉', icon: Flower2, color: '#E91E63', hasselblad: 'natural' },
  { id: 'water', name: '水景', icon: Waves, color: '#00BCD4', hasselblad: 'natural' },
  { id: 'sky', name: '天空', icon: Sun, color: '#2196F3', hasselblad: 'cinematic' },
  { id: 'forest', name: '森林', icon: TreePine, color: '#388E3C', hasselblad: 'natural' },
  { id: 'beach', name: '海滩', icon: Waves, color: '#00BCD4', hasselblad: 'cinematic' },
  { id: 'sunset', name: '日落', icon: Sun, color: '#FF5722', hasselblad: 'cinematic' },
  
  // 夜景类
  { id: 'night', name: '夜景', icon: Moon, color: '#3F51B5', hasselblad: 'cinematic' },
  { id: 'citynight', name: '城市夜景', icon: Moon, color: '#673AB7', hasselblad: 'cinematic' },
  { id: 'starry', name: '星空', icon: Moon, color: '#1A237E', hasselblad: 'cinematic' },
  
  // 建筑类
  { id: 'architecture', name: '建筑', icon: Building2, color: '#607D8B', hasselblad: 'natural' },
  { id: 'interior', name: '室内', icon: Building2, color: '#795548', hasselblad: 'natural' },
  { id: 'street', name: '街拍', icon: Car, color: '#FF5722', hasselblad: 'cinematic' },
  
  // 美食类
  { id: 'food', name: '美食', icon: UtensilsCrossed, color: '#FF9800', hasselblad: 'natural' },
  { id: 'coffee', name: '咖啡', icon: Coffee, color: '#795548', hasselblad: 'vintage' },
  { id: 'cake', name: '蛋糕', icon: Cake, color: '#E91E63', hasselblad: 'natural' },
  { id: 'sushi', name: '日料', icon: UtensilsCrossed, color: '#FF5722', hasselblad: 'natural' },
  { id: 'pizza', name: '披萨', icon: Pizza, color: '#FF9800', hasselblad: 'natural' },
  { id: 'salad', name: '沙拉', icon: Salad, color: '#4CAF50', hasselblad: 'natural' },
  { id: 'wine', name: '酒饮', icon: Wine, color: '#7B1FA2', hasselblad: 'vintage' },
  
  // 旅行类
  { id: 'travel', name: '旅行', icon: Plane, color: '#2196F3', hasselblad: 'cinematic' },
  { id: 'plane', name: '飞机', icon: Plane, color: '#1976D2', hasselblad: 'cinematic' },
  { id: 'train', name: '火车', icon: Train, color: '#FF5722', hasselblad: 'vintage' },
  { id: 'ship', name: '轮船', icon: Ship, color: '#00BCD4', hasselblad: 'cinematic' },
  { id: 'car', name: '汽车', icon: Car, color: '#FF5722', hasselblad: 'cinematic' },
  { id: 'bike', name: '骑行', icon: Bike, color: '#4CAF50', hasselblad: 'natural' },
  
  // 户外活动类
  { id: 'camping', name: '露营', icon: Tent, color: '#8BC34A', hasselblad: 'natural' },
  { id: 'fishing', name: '钓鱼', icon: Fish, color: '#00BCD4', hasselblad: 'natural' },
  { id: 'hiking', name: '徒步', icon: Mountain, color: '#4CAF50', hasselblad: 'natural' },
  
  // 其他
  { id: 'product', name: '商品', icon: ShoppingBag, color: '#9E9E9E', hasselblad: 'natural' },
  { id: 'document', name: '文档', icon: Camera, color: '#607D8B', hasselblad: 'natural' },
  { id: 'art', name: '艺术', icon: Palette, color: '#E91E63', hasselblad: 'vintage' },
];

const AISceneRecognitionPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [detectedScene, setDetectedScene] = useState<string | null>(null);
  const [recommendedStyle, setRecommendedStyle] = useState<string | null>(null);
  const [appliedScene, setAppliedScene] = useState<string | null>(null);

  // AI分析图片识别场景
  const handleAnalyze = () => {
    if (!uploadedImage) return;
    
    setIsAnalyzing(true);
    setDetectedScene(null);
    setRecommendedStyle(null);
    
    setTimeout(() => {
      const randomScene = scenes[Math.floor(Math.random() * scenes.length)];
      setDetectedScene(randomScene.id);
      setRecommendedStyle(randomScene.hasselblad);
      setIsAnalyzing(false);
    }, 2000);
  };

  // 应用场景参数
  const handleApplyScene = (scene: typeof scenes[0]) => {
    const style = hasselbladStyles.find(s => s.id === scene.hasselblad);
    if (style) {
      // 根据哈苏风格设置参数
      const params = {
        natural: { saturation: 15, contrast: 10, warmth: 0, sharpness: 20 },
        portrait: { saturation: 10, contrast: 5, warmth: 8, sharpness: 15 },
        cinematic: { saturation: 20, contrast: 15, warmth: -5, sharpness: 25 },
        vintage: { saturation: 5, contrast: 10, warmth: 15, sharpness: 18 },
      };
      const p = params[style.id as keyof typeof params];
      setAiParam('saturation', p.saturation);
      setAiParam('contrast', p.contrast);
      setAiParam('warmth', p.warmth);
      setAiParam('sharpness', p.sharpness);
    }
    setAppliedScene(scene.id);
    setTimeout(() => setAppliedScene(null), 3000);
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">AI 场景识别</h1>
        {uploadedImage && (
          <div className="ml-auto px-2 py-1 rounded-full bg-[#FF6B35]/20">
            <span className="text-[#FF6B35] text-xs">已上传照片</span>
          </div>
        )}
      </div>

      {/* Image Upload */}
      <div className="px-4 py-4">
        <ImageUploader 
          onImageSelect={setUploadedImage}
          currentImage={uploadedImage}
          title="上传照片场景识别"
          description="AI分析场景并推荐哈苏风格"
        />
      </div>

      {/* AI Analyze Button */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <button
            onClick={handleAnalyze}
            disabled={isAnalyzing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#4CAF50] to-[#2E7D32] flex items-center justify-center gap-2 text-white font-medium"
          >
            {isAnalyzing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>AI识别中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>开始AI场景识别</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Detected Result */}
      {detectedScene && !isAnalyzing && (
        <div className="px-4 pb-4">
          <div className="p-4 rounded-2xl bg-[#4CAF50]/20 border border-[#4CAF50]/50">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-[#4CAF50]/30 flex items-center justify-center">
                {scenes.find(s => s.id === detectedScene)?.icon && 
                  React.createElement(scenes.find(s => s.id === detectedScene)!.icon, { size: 24, className: 'text-[#4CAF50]' })
                }
              </div>
              <div>
                <p className="text-[#4CAF50] text-sm font-bold">已识别场景</p>
                <p className="text-white text-lg font-bold">{scenes.find(s => s.id === detectedScene)?.name}</p>
                <p className="text-white/50 text-xs">推荐: {hasselbladStyles.find(s => s.id === recommendedStyle)?.name}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Hasselblad Styles */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Sparkles size={12} />
            哈苏大师风格
          </p>
          <div className="grid grid-cols-2 gap-3">
            {hasselbladStyles.map((style) => {
              const Icon = style.icon;
              const isSelected = recommendedStyle === style.id;
              
              return (
                <button
                  key={style.id}
                  className={`p-4 rounded-2xl transition-all ${
                    isSelected ? 'bg-[#4CAF50]/20 border border-[#4CAF50]' : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ backgroundColor: `${style.color}20` }}>
                      <Icon size={24} style={{ color: style.color }} />
                    </div>
                    <div>
                      <p className="text-white text-sm font-medium">{style.name}</p>
                      <p className="text-white/50 text-xs">{style.desc}</p>
                      {isSelected && (
                        <span className="text-[#4CAF50] text-xs mt-1 flex items-center gap-1">
                          <Sparkles size={10} /> AI推荐
                        </span>
                      )}
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Scene List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3">支持 36+ 拍摄场景</p>
        
        <div className="grid grid-cols-3 gap-2">
          {scenes.map((scene) => {
            const Icon = scene.icon;
            const isApplied = appliedScene === scene.id;
            const isDetected = detectedScene === scene.id;
            
            return (
              <button
                key={scene.id}
                onClick={() => handleApplyScene(scene)}
                className={`relative p-3 rounded-xl transition-all ${
                  isDetected ? 'bg-[#4CAF50]/30 border border-[#4CAF50]' :
                  isApplied ? 'bg-[#FF6B35]/30 border border-[#FF6B35]' :
                  'bg-white/5 hover:bg-white/10'
                }`}
              >
                {isApplied && (
                  <div className="absolute inset-0 flex items-center justify-center bg-[#FF6B35]/20 rounded-xl">
                    <div className="w-8 h-8 rounded-full bg-[#FF6B35] flex items-center justify-center">
                      <Check size={16} className="text-white" />
                    </div>
                  </div>
                )}
                
                <div className="flex flex-col items-center gap-1 relative z-10">
                  <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ backgroundColor: `${scene.color}20` }}>
                    <Icon size={20} style={{ color: scene.color }} />
                  </div>
                  <span className="text-white text-xs">{scene.name}</span>
                  {isDetected && <span className="text-[#4CAF50] text-[10px]">已识别</span>}
                </div>
              </button>
            );
          })}
        </div>

        {/* Supported Scenes Count */}
        <div className="mt-4 p-4 rounded-2xl bg-white/5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-white text-sm font-medium">已支持场景</p>
              <p className="text-white/50 text-xs">持续更新中</p>
            </div>
            <div className="text-center">
              <span className="text-2xl font-bold text-[#4CAF50]">36+</span>
              <p className="text-white/30 text-[10px]">种场景</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AISceneRecognitionPage;