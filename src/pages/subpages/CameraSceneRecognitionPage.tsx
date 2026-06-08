import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Camera, Sparkles, Check, RefreshCw, Wand2, Mountain, User, Moon,
  UtensilsCrossed, Building2, TreePine, Car, Waves, Flower2, Cat, Sun,
  Palette, Film, Coffee, ShoppingBag, Plane, Train, Ship, Bike, Tent,
  Fish, Wine, Cake, Pizza, Salad, Zap, Focus, Aperture, Timer, Thermometer,
  ChevronRight, Info, X, Eye, Lightbulb, Target
} from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { analyzeImageScene, SceneAnalysisResult } from '../../utils/imageProcessor';

// 场景预设参数 - 哈苏大师风格
const scenePresets = [
  {
    id: 'portrait',
    name: '人像大师',
    icon: User,
    color: '#E91E63',
    hasselbladStyle: 'portrait',
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
    hasselbladStyle: 'natural',
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
    hasselbladStyle: 'cinematic',
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
    hasselbladStyle: 'natural',
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
    hasselbladStyle: 'cinematic',
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
    hasselbladStyle: 'cinematic',
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
    hasselbladStyle: 'natural',
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
    hasselbladStyle: 'natural',
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
    hasselbladStyle: 'natural',
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
    hasselbladStyle: 'vintage',
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
    hasselbladStyle: 'cinematic',
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
    hasselbladStyle: 'natural',
    desc: '绿意盎然，生机勃勃',
    tips: ['注意光线穿透', '强调绿色层次', '广角展现规模'],
    params: { iso: '100', shutter: '1/60', aperture: 'f/8', wb: '5600K', saturation: '+18', warmth: '+5' },
    sampleImage: 'https://images.unsplash.com/photo-1448375240586-882707db888b?w=400&h=300&fit=crop'
  },
];

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

const CameraSceneRecognitionPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [cameraPreview, setCameraPreview] = useState<string>('');
  const [isRecognizing, setIsRecognizing] = useState(false);
  const [recognitionResult, setRecognitionResult] = useState<SceneAnalysisResult | null>(null);
  const [recommendedPreset, setRecommendedPreset] = useState<typeof scenePresets[0] | null>(null);
  const [showPresetDetail, setShowPresetDetail] = useState(false);
  const [showTips, setShowTips] = useState(false);
  const [autoRecognize, setAutoRecognize] = useState(true);
  const [recognitionCount, setRecognitionCount] = useState(0);
  const [lastRecognizeTime, setLastRecognizeTime] = useState(0);
  const recognitionIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // 实时场景识别 - 模拟相机预览刷新
  const recognizeScene = useCallback(async () => {
    if (!cameraPreview || isRecognizing) return;

    const now = Date.now();
    if (now - lastRecognizeTime < 500) return; // 500ms刷新间隔

    setIsRecognizing(true);
    setLastRecognizeTime(now);

    try {
      const result = await analyzeImageScene(cameraPreview);
      setRecognitionResult(result);
      setRecognitionCount(prev => prev + 1);

      // 匹配预设
      const presetId = sceneMapping[result.scene] || 'landscape';
      const preset = scenePresets.find(p => p.id === presetId) || scenePresets[2];
      setRecommendedPreset(preset);
    } catch (e) {
      console.error('场景识别失败:', e);
    } finally {
      setIsRecognizing(false);
    }
  }, [cameraPreview, isRecognizing, lastRecognizeTime]);

  // 自动识别模式
  useEffect(() => {
    if (autoRecognize && cameraPreview) {
      // 首次立即识别
      recognizeScene();

      // 设置定时识别（模拟相机实时预览）
      recognitionIntervalRef.current = setInterval(() => {
        recognizeScene();
      }, 2000); // 每2秒识别一次

      return () => {
        if (recognitionIntervalRef.current) {
          clearInterval(recognitionIntervalRef.current);
        }
      };
    } else {
      if (recognitionIntervalRef.current) {
        clearInterval(recognitionIntervalRef.current);
      }
    }
  }, [autoRecognize, cameraPreview, recognizeScene]);

  // 应用预设参数
  const applyPreset = (preset: typeof scenePresets[0]) => {
    // 设置AI参数
    const params = preset.params;
    setAiParam('saturation', parseInt(params.saturation.replace('+', '')));
    setAiParam('warmth', parseInt(params.warmth.replace('+', '').replace('-', '')));
    setShowPresetDetail(false);
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] relative">
      {/* Header */}
      <div className="absolute top-0 left-0 right-0 z-30 px-4 py-3 bg-gradient-to-b from-black/80 to-transparent">
        <div className="flex items-center justify-between">
          <button
            onClick={goBack}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition-colors"
          >
            <ArrowLeft size={20} className="text-white" />
          </button>
          <div className="flex items-center gap-2">
            <div className={`px-3 py-1.5 rounded-full flex items-center gap-2 ${
              autoRecognize ? 'bg-[#4CAF50]/30' : 'bg-white/10'
            }`}>
              <Zap size={14} className={autoRecognize ? 'text-[#4CAF50]' : 'text-white/50'} />
              <span className={`text-sm ${autoRecognize ? 'text-[#4CAF50]' : 'text-white/50'}`}>
                {autoRecognize ? 'AI实时识别' : '手动识别'}
              </span>
            </div>
            <button
              onClick={() => setAutoRecognize(!autoRecognize)}
              className="p-2 rounded-full bg-white/10 hover:bg-white/20 transition-colors"
            >
              <RefreshCw size={16} className={`text-white ${autoRecognize ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Camera Preview Area */}
      <div className="relative flex-1">
        {/* 相机取景器 */}
        <div className="absolute inset-0 bg-black">
          {cameraPreview ? (
            <img
              src={cameraPreview}
              alt="相机预览"
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-center bg-gradient-to-b from-[#1a1a2e] to-[#0a0a0a]">
              <div className="w-24 h-24 rounded-full bg-white/5 flex items-center justify-center mb-4">
                <Camera size={48} className="text-white/30" />
              </div>
              <p className="text-white/50 text-sm mb-4">点击下方按钮模拟相机取景</p>
              <ImageUploader
                onImageSelect={setCameraPreview}
                currentImage={cameraPreview}
                title="模拟相机预览"
                description="上传照片模拟相机实时取景"
              />
            </div>
          )}

          {/* 场景识别动画层 */}
          {isRecognizing && cameraPreview && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="relative">
                {/* 扫描动画 */}
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

          {/* 识别成功指示器 */}
          {recognitionResult && !isRecognizing && (
            <div className="absolute top-20 left-1/2 -translate-x-1/2 z-20">
              <div className="px-4 py-2 rounded-full bg-[#4CAF50]/90 backdrop-blur-sm flex items-center gap-2 animate-bounce"
                style={{ animationDuration: '1s' }}>
                <Check size={16} className="text-white" />
                <span className="text-white text-sm font-medium">
                  {recognitionResult.scene} · {recognitionResult.confidence}%
                </span>
              </div>
            </div>
          )}

          {/* 相机参数显示 */}
          {cameraPreview && recommendedPreset && (
            <div className="absolute top-32 right-4 z-20">
              <div className="p-3 rounded-xl bg-black/60 backdrop-blur-sm">
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="flex items-center gap-1">
                    <Aperture size={12} className="text-[#FF6B35]" />
                    <span className="text-white/70">{recommendedPreset.params.aperture}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Timer size={12} className="text-[#FF6B35]" />
                    <span className="text-white/70">{recommendedPreset.params.shutter}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Focus size={12} className="text-[#FF6B35]" />
                    <span className="text-white/70">ISO {recommendedPreset.params.iso}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Thermometer size={12} className="text-[#FF6B35]" />
                    <span className="text-white/70">{recommendedPreset.params.wb}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 底部推荐预设卡片 */}
        {recommendedPreset && cameraPreview && (
          <div className="absolute bottom-0 left-0 right-0 z-20">
            {/* 预设卡片 */}
            <div
              className="bg-gradient-to-t from-black/90 via-black/70 to-transparent p-4 pt-16"
              onClick={() => setShowPresetDetail(true)}
            >
              <div className="flex items-center gap-4">
                {/* 样张预览 */}
                <div className="w-20 h-20 rounded-xl overflow-hidden border-2 border-[#FF6B35]/50">
                  <img
                    src={recommendedPreset.sampleImage}
                    alt={recommendedPreset.name}
                    className="w-full h-full object-cover"
                  />
                </div>

                {/* 预设信息 */}
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                      style={{ backgroundColor: `${recommendedPreset.color}30` }}>
                      {React.createElement(recommendedPreset.icon, {
                        size: 16,
                        style: { color: recommendedPreset.color }
                      })}
                    </div>
                    <span className="text-white font-bold">{recommendedPreset.name}</span>
                    <span className="px-2 py-0.5 rounded-full bg-[#4CAF50]/20 text-[#4CAF50] text-xs">
                      AI推荐
                    </span>
                  </div>
                  <p className="text-white/60 text-sm">{recommendedPreset.desc}</p>
                  <div className="flex items-center gap-2 mt-2">
                    <span className="text-white/40 text-xs">哈苏{recommendedPreset.hasselbladStyle}风格</span>
                    <ChevronRight size={14} className="text-white/40" />
                  </div>
                </div>

                {/* 应用按钮 */}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    applyPreset(recommendedPreset);
                  }}
                  className="px-4 py-2 rounded-xl bg-[#FF6B35] text-white text-sm font-medium"
                >
                  应用
                </button>
              </div>

              {/* 拍摄提示按钮 */}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setShowTips(true);
                }}
                className="mt-3 w-full py-2 rounded-xl bg-white/10 flex items-center justify-center gap-2 text-white/70 text-sm"
              >
                <Lightbulb size={14} />
                <span>查看拍摄技巧</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 预设详情弹窗 */}
      {showPresetDetail && recommendedPreset && (
        <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-sm flex items-end">
          <div className="w-full bg-[#1a1a1a] rounded-t-3xl p-6 animate-slide-up">
            {/* 关闭按钮 */}
            <button
              onClick={() => setShowPresetDetail(false)}
              className="absolute top-4 right-4 p-2 rounded-full bg-white/10"
            >
              <X size={20} className="text-white" />
            </button>

            {/* 样张对比 */}
            <div className="mb-6">
              <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Eye size={12} />
                大师样张效果对比
              </p>
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-xl overflow-hidden">
                  <img src={cameraPreview} alt="当前取景" className="w-full aspect-[4/3] object-cover" />
                  <div className="p-2 bg-white/5 text-center">
                    <span className="text-white/50 text-xs">当前取景</span>
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

            {/* 预设参数详情 */}
            <div className="mb-6">
              <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Target size={12} />
                推荐拍摄参数
              </p>
              <div className="grid grid-cols-3 gap-2">
                {Object.entries(recommendedPreset.params).map(([key, value]) => (
                  <div key={key} className="p-3 rounded-xl bg-white/5 text-center">
                    <p className="text-white/50 text-xs mb-1">
                      {key === 'iso' ? 'ISO' : key === 'shutter' ? '快门' : key === 'aperture' ? '光圈' :
                        key === 'wb' ? '白平衡' : key === 'saturation' ? '饱和度' : '色温'}
                    </p>
                    <p className="text-[#FF6B35] text-sm font-bold">{value}</p>
                  </div>
                ))}
              </div>
            </div>

            {/* 应用按钮 */}
            <button
              onClick={() => {
                applyPreset(recommendedPreset);
                setShowPresetDetail(false);
              }}
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
            <button
              onClick={() => setShowTips(false)}
              className="absolute top-4 right-4 p-2 rounded-full bg-white/10"
            >
              <X size={20} className="text-white" />
            </button>

            <div className="mb-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-xl flex items-center justify-center"
                  style={{ backgroundColor: `${recommendedPreset.color}30` }}>
                  {React.createElement(recommendedPreset.icon, {
                    size: 24,
                    style: { color: recommendedPreset.color }
                  })}
                </div>
                <div>
                  <p className="text-white font-bold">{recommendedPreset.name}</p>
                  <p className="text-white/50 text-sm">{recommendedPreset.desc}</p>
                </div>
              </div>
            </div>

            {/* 拍摄技巧列表 */}
            <div className="space-y-3 mb-6">
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

            <button
              onClick={() => setShowTips(false)}
              className="w-full py-3 rounded-xl bg-white/10 text-white font-medium"
            >
              开始拍摄
            </button>
          </div>
        </div>
      )}

      {/* 识别次数统计 */}
      {cameraPreview && (
        <div className="absolute bottom-4 left-4 z-10 px-2 py-1 rounded-full bg-black/50">
          <span className="text-white/40 text-xs">识别 #{recognitionCount}</span>
        </div>
      )}
    </div>
  );
};

export default CameraSceneRecognitionPage;