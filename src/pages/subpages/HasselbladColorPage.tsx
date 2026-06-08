import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Aperture, Palette, Sun, Moon, Contrast, Sparkles,
  Check, RefreshCw, Wand2, Camera, Mountain, User, Info, Download,
  Circle, Layers, Target, Eye, Lightbulb
} from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { applyImageAdjustments, downloadImage, ImageAdjustParams } from '../../utils/imageProcessor';

// HNCS 3.0 哈苏自然色彩解决方案 - 5种风格预设
const hncsStyles = [
  {
    id: 'natural',
    name: 'HNCS 自然',
    fullName: 'Hasselblad Natural Color Solution',
    desc: '真实还原，细节丰富，自然色彩',
    color: '#4CAF50',
    icon: Sun,
    technical: '16-bit色深处理，宽色域覆盖，中性色调映射',
    params: {
      saturation: 12, contrast: 8, brightness: 5, warmth: 0,
      cyanMagenta: 0, sharpness: 18, tone: 8, softLight: 15,
      vignette: false, filter: '原图'
    },
    sampleImage: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    characteristics: ['中性色调', '高动态范围', '细节保留', '真实色彩']
  },
  {
    id: 'portrait',
    name: 'HNCS 人像',
    fullName: 'Hasselblad Portrait Color',
    desc: '柔美肤色，自然光影，专业人像',
    color: '#E91E63',
    icon: User,
    technical: '肤色优化算法，柔和色调过渡，面部细节增强',
    params: {
      saturation: 8, contrast: 5, brightness: 8, warmth: 12,
      cyanMagenta: 0, sharpness: 12, tone: 5, softLight: 35,
      vignette: false, filter: '原图'
    },
    sampleImage: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=300&fit=crop',
    characteristics: ['肤色优化', '柔和光影', '面部增强', '自然过渡']
  },
  {
    id: 'cinematic',
    name: 'HNCS 电影',
    fullName: 'Hasselblad Cinematic Color',
    desc: '电影质感，氛围感强，叙事风格',
    color: '#FF9800',
    icon: Moon,
    technical: '电影级色调映射，暗部细节保留，高对比度处理',
    params: {
      saturation: 18, contrast: 22, brightness: 0, warmth: -5,
      cyanMagenta: 5, sharpness: 25, tone: 18, softLight: 20,
      vignette: true, filter: '胶片'
    },
    sampleImage: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    characteristics: ['电影色调', '暗部细节', '高对比度', '氛围营造']
  },
  {
    id: 'vintage',
    name: 'HNCS 复古',
    fullName: 'Hasselblad Vintage Color',
    desc: '经典胶片，怀旧质感， timeless风格',
    color: '#795548',
    icon: Palette,
    technical: '胶片颗粒模拟，复古色调映射，暖色偏移处理',
    params: {
      saturation: -5, contrast: 15, brightness: 0, warmth: 25,
      cyanMagenta: 0, sharpness: 15, tone: 20, softLight: 25,
      vignette: true, filter: '胶片'
    },
    sampleImage: 'https://images.unsplash.com/photo-1495616811223-4d98d6e944aa?w=400&h=300&fit=crop',
    characteristics: ['胶片质感', '复古色调', '暖色偏移', '经典风格']
  },
  {
    id: 'vivid',
    name: 'HNCS 鲜艳',
    fullName: 'Hasselblad Vivid Color',
    desc: '色彩鲜艳，视觉冲击，活力风格',
    color: '#FF5722',
    icon: Sparkles,
    technical: '高饱和度处理，色彩增强算法，动态范围扩展',
    params: {
      saturation: 30, contrast: 18, brightness: 5, warmth: 8,
      cyanMagenta: 0, sharpness: 22, tone: 12, softLight: 10,
      vignette: false, filter: '原图'
    },
    sampleImage: 'https://images.unsplash.com/photo-1490750967868-5aa43378c200?w=400&h=300&fit=crop',
    characteristics: ['高饱和度', '色彩增强', '视觉冲击', '活力风格']
  },
];

// HNCS技术参数说明
const hncsTechSpecs = [
  { name: '色深处理', value: '16-bit', desc: '专业级色深，保留更多细节' },
  { name: '色域覆盖', value: 'P3/Rec.2020', desc: '宽色域支持，色彩更丰富' },
  { name: '动态范围', value: '14EV', desc: '高动态范围，暗部细节清晰' },
  { name: '色调映射', value: 'HNCS 3.0', desc: '哈苏自然色调映射算法' },
  { name: '肤色优化', value: 'Skin Tone+', desc: '专业肤色处理算法' },
  { name: '降噪算法', value: 'Multi-scale', desc: '多尺度降噪，细节保留' },
];

const HasselbladColorPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [selectedStyle, setSelectedStyle] = useState<string>('natural');
  const [processedImage, setProcessedImage] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [showTechSpecs, setShowTechSpecs] = useState(false);
  const [showComparison, setShowComparison] = useState(false);

  // 应用HNCS风格
  const applyHncsStyle = async (styleId: string) => {
    const style = hncsStyles.find(s => s.id === styleId);
    if (!style || !uploadedImage) return;

    setSelectedStyle(styleId);
    setIsProcessing(true);

    try {
      const result = await applyImageAdjustments(uploadedImage, style.params);
      setProcessedImage(result);

      // 同步参数到全局状态
      setAiParam('saturation', style.params.saturation);
      setAiParam('contrast', style.params.contrast);
      setAiParam('warmth', style.params.warmth);
      setAiParam('sharpness', style.params.sharpness);
    } catch (e) {
      console.error('HNCS处理失败:', e);
    } finally {
      setIsProcessing(false);
    }
  };

  const currentStyle = hncsStyles.find(s => s.id === selectedStyle);

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="px-4 py-3 flex items-center justify-between bg-gradient-to-b from-[#1a1a1a] to-transparent">
        <button onClick={goBack} className="p-2 rounded-full bg-white/10">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-[#FF6B35]/20 flex items-center justify-center">
            <Aperture size={16} className="text-[#FF6B35]" />
          </div>
          <span className="text-white font-bold">哈苏色彩科学</span>
        </div>
        <button
          onClick={() => setShowTechSpecs(true)}
          className="p-2 rounded-full bg-white/10"
        >
          <Info size={18} className="text-white/70" />
        </button>
      </div>

      {/* HNCS介绍 */}
      <div className="px-4 py-3">
        <div className="p-4 rounded-2xl bg-gradient-to-r from-[#FF6B35]/20 to-[#FF8C42]/10 border border-[#FF6B35]/30">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/30 flex items-center justify-center">
              <Layers size={20} className="text-[#FF6B35]" />
            </div>
            <div>
              <p className="text-[#FF6B35] text-sm font-bold">HNCS 3.0</p>
              <p className="text-white text-xs">Hasselblad Natural Color Solution</p>
            </div>
          </div>
          <p className="text-white/70 text-sm leading-relaxed">
            哈苏自然色彩解决方案，源自80年专业影像经验。16-bit色深处理、宽色域覆盖、
            专业色调映射，为每一张照片注入哈苏大师级色彩表现。
          </p>
        </div>
      </div>

      {/* 图片上传 */}
      <div className="px-4 pb-3">
        <ImageUploader
          onImageSelect={setUploadedImage}
          currentImage={uploadedImage}
          title="上传照片体验HNCS"
          description="选择照片应用哈苏色彩科学"
        />
      </div>

      {/* HNCS风格选择 */}
      <div className="px-4 pb-3">
        <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
          <Palette size={12} />
          5种HNCS色彩风格
        </p>
        <div className="grid grid-cols-5 gap-2">
          {hncsStyles.map((style) => {
            const Icon = style.icon;
            const isSelected = selectedStyle === style.id;

            return (
              <button
                key={style.id}
                onClick={() => applyHncsStyle(style.id)}
                disabled={!uploadedImage || isProcessing}
                className={`p-3 rounded-xl transition-all ${
                  isSelected ? 'bg-[#FF6B35]/30 border border-[#FF6B35]' :
                  'bg-white/5 hover:bg-white/10'
                } ${!uploadedImage ? 'opacity-50' : ''}`}
              >
                <div className="flex flex-col items-center gap-1">
                  <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                    style={{ backgroundColor: `${style.color}20` }}>
                    <Icon size={16} style={{ color: style.color }} />
                  </div>
                  <span className={`text-xs ${isSelected ? 'text-[#FF6B35]' : 'text-white/70'}`}>
                    {style.name.replace('HNCS ', '')}
                  </span>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* 当前风格详情 */}
      {currentStyle && (
        <div className="px-4 pb-3">
          <div className="p-4 rounded-2xl bg-white/5">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center"
                style={{ backgroundColor: `${currentStyle.color}20` }}>
                {React.createElement(currentStyle.icon, { size: 24, style: { color: currentStyle.color } })}
              </div>
              <div>
                <p className="text-white font-bold">{currentStyle.name}</p>
                <p className="text-white/50 text-xs">{currentStyle.fullName}</p>
              </div>
            </div>
            <p className="text-white/70 text-sm mb-3">{currentStyle.desc}</p>
            <p className="text-white/40 text-xs mb-3">{currentStyle.technical}</p>

            {/* 特性标签 */}
            <div className="flex gap-2 flex-wrap">
              {currentStyle.characteristics.map((char, i) => (
                <span key={i} className="px-2 py-1 rounded-full bg-[#FF6B35]/10 text-[#FF6B35] text-xs">
                  {char}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 处理结果 */}
      {processedImage && (
        <div className="px-4 pb-3">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Eye size={12} />
            HNCS处理结果
          </p>

          {/* 对比切换 */}
          <div className="flex gap-2 mb-3">
            <button
              onClick={() => setShowComparison(false)}
              className={`flex-1 py-2 rounded-xl text-sm ${
                !showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/70'
              }`}
            >
              单图预览
            </button>
            <button
              onClick={() => setShowComparison(true)}
              className={`flex-1 py-2 rounded-xl text-sm ${
                showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/70'
              }`}
            >
              对比预览
            </button>
          </div>

          {showComparison ? (
            <div className="grid grid-cols-2 gap-2">
              <div className="rounded-xl overflow-hidden">
                <img src={uploadedImage} alt="原图" className="w-full aspect-video object-cover" />
                <div className="p-2 bg-white/5 text-center">
                  <span className="text-white/50 text-xs">原图</span>
                </div>
              </div>
              <div className="rounded-xl overflow-hidden border border-[#FF6B35]/50">
                <img src={processedImage} alt="HNCS处理" className="w-full aspect-video object-cover" />
                <div className="p-2 bg-[#FF6B35]/20 text-center">
                  <span className="text-[#FF6B35] text-xs">{currentStyle?.name}</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl overflow-hidden relative">
              <img src={processedImage} alt="HNCS处理" className="w-full aspect-video object-cover" />
              <div className="absolute bottom-2 left-2 px-3 py-1.5 rounded-lg bg-[#FF6B35]/80 backdrop-blur-sm">
                <span className="text-white text-xs font-medium">{currentStyle?.name}</span>
              </div>
            </div>
          )}

          {/* 下载按钮 */}
          <button
            onClick={() => downloadImage(processedImage, `OMaster_HNCS_${currentStyle?.id}_${Date.now()}.jpg`)}
            className="w-full mt-3 py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium"
          >
            <Download size={18} />
            <span>保存HNCS出片</span>
          </button>
        </div>
      )}

      {/* 样张展示 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
          <Target size={12} />
          HNCS大师样张
        </p>
        <div className="grid grid-cols-2 gap-3">
          {hncsStyles.map((style) => (
            <div key={style.id} className="rounded-xl overflow-hidden">
              <img src={style.sampleImage} alt={style.name} className="w-full aspect-video object-cover" />
              <div className="p-2 bg-white/5">
                <div className="flex items-center gap-2">
                  <div className="w-6 h-6 rounded-lg flex items-center justify-center"
                    style={{ backgroundColor: `${style.color}20` }}>
                    {React.createElement(style.icon, { size: 12, style: { color: style.color } })}
                  </div>
                  <span className="text-white text-xs">{style.name.replace('HNCS ', '')}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 技术规格弹窗 */}
      {showTechSpecs && (
        <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-sm flex items-end">
          <div className="w-full bg-[#1a1a1a] rounded-t-3xl p-6 animate-slide-up">
            <button
              onClick={() => setShowTechSpecs(false)}
              className="absolute top-4 right-4 p-2 rounded-full bg-white/10"
            >
              <ArrowLeft size={20} className="text-white" />
            </button>

            <div className="mb-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-xl bg-[#FF6B35]/20 flex items-center justify-center">
                  <Aperture size={24} className="text-[#FF6B35]" />
                </div>
                <div>
                  <p className="text-white font-bold">HNCS 3.0 技术规格</p>
                  <p className="text-white/50 text-sm">哈苏自然色彩解决方案</p>
                </div>
              </div>
            </div>

            {/* 技术参数列表 */}
            <div className="space-y-3 mb-6">
              {hncsTechSpecs.map((spec, i) => (
                <div key={i} className="p-4 rounded-xl bg-white/5">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-white/70 text-sm">{spec.name}</span>
                    <span className="text-[#FF6B35] text-sm font-bold">{spec.value}</span>
                  </div>
                  <p className="text-white/40 text-xs">{spec.desc}</p>
                </div>
              ))}
            </div>

            {/* 哈苏历史 */}
            <div className="p-4 rounded-xl bg-[#FF6B35]/10 border border-[#FF6B35]/30">
              <p className="text-[#FF6B35] text-sm font-bold mb-2">哈苏80年影像传承</p>
              <p className="text-white/60 text-sm leading-relaxed">
                1941年创立于瑞典哥德堡，哈苏相机曾伴随NASA登月任务，
                记录人类首次踏上月球的历史瞬间。HNCS色彩科学源自数十年
                专业中画幅相机研发经验，为OPPO Find系列注入大师级影像基因。
              </p>
            </div>

            <button
              onClick={() => setShowTechSpecs(false)}
              className="w-full mt-4 py-3 rounded-xl bg-white/10 text-white font-medium"
            >
              了解更多
            </button>
          </div>
        </div>
      )}

      {/* 处理中动画 */}
      {isProcessing && (
        <div className="absolute inset-0 z-30 bg-black/60 flex items-center justify-center">
          <div className="text-center">
            <div className="w-16 h-16 rounded-full bg-[#FF6B35]/20 flex items-center justify-center mb-4 animate-pulse">
              <Aperture size={32} className="text-[#FF6B35] animate-spin" style={{ animationDuration: '2s' }} />
            </div>
            <p className="text-white text-sm">HNCS处理中...</p>
            <p className="text-white/50 text-xs mt-1">16-bit色深 · 宽色域处理</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default HasselbladColorPage;