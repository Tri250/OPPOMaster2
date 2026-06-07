import React, { useState } from 'react';
import { ArrowLeft, Upload, Sparkles, Camera, Sun, Cloud, Moon } from 'lucide-react';

const SceneDetailPage: React.FC = () => {
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedScene, setSelectedScene] = useState<string | null>(null);

  const categories = [
    { id: 'all', name: '全部', icon: Sparkles },
    { id: 'light', name: '光线', icon: Sun },
    { id: 'weather', name: '天气', icon: Cloud },
    { id: 'time', name: '时间', icon: Moon },
  ];

  const scenes = [
    { 
      id: 'portrait', 
      name: '人像', 
      desc: '柔和的肤色表现',
      params: { exposure: '+0.3', saturation: '+10', contrast: '+5' },
      tips: '使用大光圈虚化背景'
    },
    { 
      id: 'landscape', 
      name: '风景', 
      desc: '丰富的色彩层次',
      params: { exposure: '0.0', saturation: '+15', contrast: '+10' },
      tips: '使用三脚架保持稳定'
    },
    { 
      id: 'sunset', 
      name: '日落', 
      desc: '温暖的金色调',
      params: { exposure: '-0.7', saturation: '+20', warmth: '+30' },
      tips: '黄金时间拍摄最佳'
    },
    { 
      id: 'night', 
      name: '夜景', 
      desc: '纯净的暗部表现',
      params: { exposure: '+1.0', noise: '高', contrast: '+15' },
      tips: '使用低ISO减少噪点'
    },
    { 
      id: 'food', 
      name: '美食', 
      desc: '诱人的食欲色彩',
      params: { exposure: '+0.5', saturation: '+12', warmth: '+10' },
      tips: '自然光线下拍摄'
    },
    { 
      id: 'street', 
      name: '街拍', 
      desc: '纪实的街头风格',
      params: { exposure: '0.0', contrast: '+8', clarity: '+5' },
      tips: '保持相机随身携带'
    },
  ];

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">场景细分</h1>
          <p className="text-xs text-white/50">AI 场景识别与优化</p>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* Upload Section */}
        <div className="p-4 border-b border-white/5">
          <button className="w-full border-2 border-dashed border-white/20 rounded-2xl p-6 flex flex-col items-center gap-2 hover:border-[#FF6B35] transition-all">
            <Upload size={28} className="text-white/40" />
            <span className="text-sm text-white/60">上传图片 AI 识别</span>
          </button>
        </div>

        {/* Categories */}
        <div className="p-4 border-b border-white/5">
          <div className="flex gap-2 overflow-x-auto pb-2">
            {categories.map((cat) => {
              const Icon = cat.icon;
              return (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategory(cat.id)}
                  className={`flex-shrink-0 flex items-center gap-2 px-4 py-3 rounded-xl transition-all ${
                    selectedCategory === cat.id ? 'bg-[#FF6B35]' : 'bg-white/5'
                  }`}
                >
                  <Icon size={16} />
                  <span className="text-sm font-medium">{cat.name}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Scene List */}
        <div className="p-4">
          <div className="space-y-3">
            {scenes.map((scene) => (
              <button
                key={scene.id}
                className="w-full bg-white/5 rounded-2xl p-4 text-left"
                onClick={() => setSelectedScene(scene.id)}
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#FF6B35]/20 to-[#4DABF7]/20 flex items-center justify-center">
                    <Camera size={20} className="text-[#FF6B35]" />
                  </div>
                  <div className="flex-1">
                    <h4 className="font-semibold mb-1">{scene.name}</h4>
                    <p className="text-sm text-white/50">{scene.desc}</p>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Scene Detail Sheet */}
      {selectedScene && (
        <div className="absolute inset-x-0 bottom-0 bg-[#1A1A1A] rounded-t-3xl p-6 z-50">
          <div className="w-12 h-1 bg-white/20 rounded-full mx-auto mb-6" />
          {(() => {
            const scene = scenes.find(s => s.id === selectedScene)!;
            return (
              <>
                <h3 className="text-xl font-bold mb-2">{scene.name}</h3>
                <p className="text-sm text-white/60 mb-6">{scene.desc}</p>
                
                <div className="bg-white/5 rounded-2xl p-4 mb-4">
                  <h4 className="text-sm font-semibold mb-3 text-white/80">推荐参数</h4>
                  <div className="grid grid-cols-3 gap-3">
                    {Object.entries(scene.params).map(([key, val]) => (
                      <div key={key} className="bg-white/5 rounded-xl p-3 text-center">
                        <p className="text-xs text-white/40 mb-1">{
                          key === 'exposure' ? '曝光' :
                          key === 'saturation' ? '饱和度' :
                          key === 'contrast' ? '对比度' :
                          key === 'warmth' ? '色温' :
                          key === 'noise' ? '降噪' : '清晰度'
                        }</p>
                        <p className="text-sm font-bold text-[#FF6B35]">{val}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="bg-blue-500/10 border border-blue-500/30 rounded-2xl p-4 mb-6">
                  <div className="flex items-start gap-3">
                    <Sparkles size={20} className="text-blue-400 mt-0.5" />
                    <div>
                      <h4 className="text-sm font-semibold mb-1 text-blue-400">拍摄技巧</h4>
                      <p className="text-sm text-white/70">{scene.tips}</p>
                    </div>
                  </div>
                </div>

                <div className="flex gap-3">
                  <button 
                    className="flex-1 py-4 rounded-xl border border-white/20 text-sm"
                    onClick={() => setSelectedScene(null)}
                  >
                    关闭
                  </button>
                  <button className="flex-1 py-4 rounded-xl bg-[#FF6B35] text-sm font-semibold">
                    应用场景
                  </button>
                </div>
              </>
            );
          })()}
        </div>
      )}
    </div>
  );
};

export default SceneDetailPage;
