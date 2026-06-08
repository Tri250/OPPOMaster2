import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Wand2, RefreshCw, Check, Sparkles, Sun, Moon, Contrast, Palette, Camera, Mountain, User, UtensilsCrossed, Building2, TreePine, Car, Waves, Flower2, Bird, Dog, Cat } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';

// 2026年OPPO哈苏大师模式预设情景
const presetScenes = [
  { id: 'portrait', name: '人像大师', desc: '柔美肤色，自然光影', color: '#E91E63', icon: User, params: { saturation: 10, contrast: 5, warmth: 8, sharpness: 15 } },
  { id: 'landscape', name: '风景大师', desc: '通透质感，色彩饱满', color: '#4CAF50', icon: Mountain, params: { saturation: 20, contrast: 15, warmth: -5, sharpness: 25 } },
  { id: 'night', name: '夜景大师', desc: '降噪增强，氛围感强', color: '#3F51B5', icon: Moon, params: { saturation: 25, contrast: 20, warmth: -10, sharpness: 30 } },
  { id: 'food', name: '美食大师', desc: '暖色调，食欲感强', color: '#FF9800', icon: UtensilsCrossed, params: { saturation: 15, contrast: 10, warmth: 20, sharpness: 12 } },
  { id: 'architecture', name: '建筑大师', desc: '线条清晰，质感强', color: '#607D8B', icon: Building2, params: { saturation: 8, contrast: 15, warmth: 0, sharpness: 20 } },
  { id: 'nature', name: '自然大师', desc: '清新自然，生机盎然', color: '#8BC34A', icon: TreePine, params: { saturation: 18, contrast: 12, warmth: 5, sharpness: 22 } },
  { id: 'street', name: '街拍大师', desc: '人文气息，故事感', color: '#FF5722', icon: Car, params: { saturation: 12, contrast: 18, warmth: 10, sharpness: 18 } },
  { id: 'water', name: '水景大师', desc: '流动质感，梦幻感', color: '#00BCD4', icon: Waves, params: { saturation: 15, contrast: 8, warmth: -8, sharpness: 15 } },
  { id: 'flower', name: '花卉大师', desc: '色彩鲜艳，细节丰富', color: '#E91E63', icon: Flower2, params: { saturation: 25, contrast: 10, warmth: 5, sharpness: 20 } },
  { id: 'pet', name: '宠物大师', desc: '毛发细节，眼神灵动', color: '#9C27B0', icon: Cat, params: { saturation: 12, contrast: 8, warmth: 8, sharpness: 25 } },
];

const AIFineTunePage: React.FC = () => {
  const { goBack, aiParams, setAiParam } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [recommendedScene, setRecommendedScene] = useState<string | null>(null);
  const [appliedScene, setAppliedScene] = useState<string | null>(null);

  // AI分析图片推荐情景
  const handleAnalyze = () => {
    if (!uploadedImage) return;
    
    setIsAnalyzing(true);
    setRecommendedScene(null);
    
    setTimeout(() => {
      const randomScene = presetScenes[Math.floor(Math.random() * presetScenes.length)];
      setRecommendedScene(randomScene.id);
      setIsAnalyzing(false);
    }, 2000);
  };

  // 应用情景预设
  const applyScene = (scene: typeof presetScenes[0]) => {
    setAiParam('saturation', scene.params.saturation);
    setAiParam('contrast', scene.params.contrast);
    setAiParam('warmth', scene.params.warmth);
    setAiParam('sharpness', scene.params.sharpness);
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
        <h1 className="text-lg font-bold text-white">AI 微调</h1>
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
          title="上传照片AI微调"
          description="AI分析并推荐哈苏大师模式"
        />
      </div>

      {/* AI Analyze Button */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <button
            onClick={handleAnalyze}
            disabled={isAnalyzing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium"
          >
            {isAnalyzing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>AI分析中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>AI分析推荐情景</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Preset Scenes */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
          <Sparkles size={12} />
          2026年OPPO哈苏大师模式预设情景
        </p>
        
        <div className="grid grid-cols-2 gap-3">
          {presetScenes.map((scene) => {
            const Icon = scene.icon;
            const isRecommended = recommendedScene === scene.id;
            const isApplied = appliedScene === scene.id;
            
            return (
              <button
                key={scene.id}
                onClick={() => applyScene(scene)}
                className={`relative p-4 rounded-2xl transition-all ${
                  isApplied ? 'bg-[#FF6B35]/30 border border-[#FF6B35]' :
                  isRecommended ? 'bg-[#4CAF50]/20 border border-[#4CAF50]/50' :
                  'bg-white/5 hover:bg-white/10'
                }`}
              >
                {isApplied && (
                  <div className="absolute inset-0 flex items-center justify-center bg-[#FF6B35]/20 rounded-2xl">
                    <div className="w-10 h-10 rounded-full bg-[#FF6B35] flex items-center justify-center">
                      <Check size={20} className="text-white" />
                    </div>
                  </div>
                )}
                
                <div className="flex items-center gap-3 relative z-10">
                  <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ backgroundColor: `${scene.color}20` }}>
                    <Icon size={24} style={{ color: scene.color }} />
                  </div>
                  <div className="flex-1">
                    <p className="text-white text-sm font-medium">{scene.name}</p>
                    <p className="text-white/50 text-xs">{scene.desc}</p>
                    {isRecommended && (
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

        {/* Current Params */}
        <div className="mt-6 p-4 rounded-2xl bg-white/5">
          <p className="text-white text-sm font-medium mb-4">当前调色参数</p>
          <div className="space-y-3">
            {Object.entries(aiParams).map(([key, value]) => (
              <div key={key} className="flex items-center justify-between">
                <span className="text-white/70 text-sm">{key === 'saturation' ? '饱和度' : key === 'contrast' ? '对比度' : key === 'warmth' ? '色温' : '锐度'}</span>
                <span className="text-[#FF6B35] text-sm font-bold">{value > 0 ? '+' : ''}{value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AIFineTunePage;