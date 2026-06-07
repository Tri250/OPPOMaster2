import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Camera, Sparkles, Check, RefreshCw, Mountain, User, Moon, UtensilsCrossed, Building2, TreePine } from 'lucide-react';

const scenes = [
  { id: 'portrait', name: '人像', icon: User, color: '#E91E63', params: { saturation: 10, contrast: 5, warmth: 8 } },
  { id: 'landscape', name: '风景', icon: Mountain, color: '#4CAF50', params: { saturation: 20, contrast: 15, warmth: -5 } },
  { id: 'night', name: '夜景', icon: Moon, color: '#3F51B5', params: { saturation: 25, contrast: 20, warmth: -10 } },
  { id: 'food', name: '美食', icon: UtensilsCrossed, color: '#FF9800', params: { saturation: 15, contrast: 10, warmth: 20 } },
  { id: 'architecture', name: '建筑', icon: Building2, color: '#607D8B', params: { saturation: 8, contrast: 15, warmth: 0 } },
  { id: 'nature', name: '自然', icon: TreePine, color: '#8BC34A', params: { saturation: 18, contrast: 12, warmth: 5 } },
];

const AISceneRecognitionPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [isScanning, setIsScanning] = useState(false);
  const [detectedScene, setDetectedScene] = useState<string | null>(null);
  const [appliedScenes, setAppliedScenes] = useState<string[]>([]);

  const handleScan = () => {
    setIsScanning(true);
    setDetectedScene(null);
    
    // 模拟AI场景识别
    setTimeout(() => {
      const randomScene = scenes[Math.floor(Math.random() * scenes.length)];
      setDetectedScene(randomScene.id);
      setIsScanning(false);
    }, 2000);
  };

  const handleApplyScene = (scene: typeof scenes[0]) => {
    setAiParam('saturation', scene.params.saturation);
    setAiParam('contrast', scene.params.contrast);
    setAiParam('warmth', scene.params.warmth);
    setAppliedScenes(prev => [...prev, scene.id]);
    
    // 3秒后移除应用状态
    setTimeout(() => {
      setAppliedScenes(prev => prev.filter(id => id !== scene.id));
    }, 3000);
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button 
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">AI 场景识别</h1>
      </div>

      {/* Camera Preview */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-gray-900 to-gray-800">
          <img 
            src="https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&h=400&fit=crop"
            alt="Camera Preview"
            className="w-full h-full object-cover"
          />
          
          {/* Scanning Overlay */}
          {isScanning && (
            <div className="absolute inset-0 bg-black/60 flex flex-col items-center justify-center">
              <div className="relative w-48 h-48">
                {/* Scanning Frame */}
                <div className="absolute inset-0 border-2 border-[#4CAF50] rounded-lg animate-pulse" />
                <div className="absolute top-0 left-0 w-6 h-6 border-t-4 border-l-4 border-[#4CAF50] rounded-tl-lg" />
                <div className="absolute top-0 right-0 w-6 h-6 border-t-4 border-r-4 border-[#4CAF50] rounded-tr-lg" />
                <div className="absolute bottom-0 left-0 w-6 h-6 border-b-4 border-l-4 border-[#4CAF50] rounded-bl-lg" />
                <div className="absolute bottom-0 right-0 w-6 h-6 border-b-4 border-r-4 border-[#4CAF50] rounded-br-lg" />
                
                {/* Scanning Line */}
                <div className="absolute left-2 right-2 h-0.5 bg-gradient-to-r from-transparent via-[#4CAF50] to-transparent animate-bounce" 
                     style={{ top: '50%', animationDuration: '1.5s' }} />
              </div>
              <div className="flex items-center gap-2 mt-4">
                <RefreshCw size={20} className="text-[#4CAF50] animate-spin" />
                <span className="text-white text-sm">正在识别场景...</span>
              </div>
            </div>
          )}

          {/* Detected Scene Overlay */}
          {detectedScene && !isScanning && (
            <div className="absolute inset-0 bg-black/70 flex items-center justify-center">
              <div className="flex flex-col items-center gap-3">
                <div className="w-16 h-16 rounded-full bg-[#4CAF50]/20 flex items-center justify-center">
                  {scenes.find(s => s.id === detectedScene)?.icon && 
                    React.createElement(scenes.find(s => s.id === detectedScene)!.icon, { size: 32, className: 'text-[#4CAF50]' })
                  }
                </div>
                <span className="text-[#4CAF50] text-lg font-bold">
                  已识别: {scenes.find(s => s.id === detectedScene)?.name}
                </span>
              </div>
            </div>
          )}

          {/* Scene Tag on Preview */}
          {detectedScene && !isScanning && (
            <div className="absolute top-3 left-3 px-3 py-1.5 rounded-full bg-black/50 backdrop-blur-sm flex items-center gap-2">
              <Camera size={14} className="text-[#4CAF50]" />
              <span className="text-white text-xs">AI 场景</span>
            </div>
          )}
        </div>
      </div>

      {/* Scan Button */}
      <div className="px-4 pb-4">
        <button
          onClick={handleScan}
          disabled={isScanning}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-[#4CAF50] to-[#2E7D32] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98 disabled:opacity-50"
        >
          {isScanning ? (
            <>
              <RefreshCw size={18} className="animate-spin" />
              <span>识别中...</span>
            </>
          ) : (
            <>
              <Sparkles size={18} />
              <span>开始 AI 场景识别</span>
            </>
          )}
        </button>
      </div>

      {/* Scene List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3">支持 36+ 拍摄场景</p>
        
        <div className="grid grid-cols-2 gap-3">
          {scenes.map((scene) => {
            const Icon = scene.icon;
            const isApplied = appliedScenes.includes(scene.id);
            const isDetected = detectedScene === scene.id;
            
            return (
              <button
                key={scene.id}
                onClick={() => handleApplyScene(scene)}
                className={`relative p-4 rounded-2xl transition-all duration-300 active:scale-95 ${
                  isDetected 
                    ? 'bg-gradient-to-br from-[#4CAF50]/30 to-[#2E7D32]/20 border border-[#4CAF50]/50'
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                {/* Applied Overlay */}
                {isApplied && (
                  <div className="absolute inset-0 bg-[#4CAF50]/20 rounded-2xl flex items-center justify-center">
                    <div className="w-8 h-8 rounded-full bg-[#4CAF50] flex items-center justify-center">
                      <Check size={18} className="text-white" />
                    </div>
                  </div>
                )}
                
                <div className="flex flex-col items-center gap-2 relative z-10">
                  <div 
                    className="w-12 h-12 rounded-xl flex items-center justify-center"
                    style={{ backgroundColor: `${scene.color}20` }}
                  >
                    <Icon size={24} style={{ color: scene.color }} />
                  </div>
                  <span className="text-white text-sm font-medium">{scene.name}</span>
                  {isDetected && (
                    <span className="text-[#4CAF50] text-[10px]">已识别</span>
                  )}
                </div>
              </button>
            );
          })}
        </div>

        {/* Supported Scenes Count */}
        <div className="mt-6 p-4 rounded-2xl bg-white/5">
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
