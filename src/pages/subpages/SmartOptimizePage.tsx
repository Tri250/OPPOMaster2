import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Wand2, RefreshCw, Check, Sparkles, Sun, Moon, Contrast, Palette, Camera, Zap, Image, Download } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { applyImageAdjustments, downloadImage, ImageAdjustParams } from '../../utils/imageProcessor';

// 哈苏大师优化风格 - 真实参数
const hasselbladStyles = [
  {
    id: 'natural',
    name: '哈苏自然',
    desc: '真实还原，细节丰富',
    color: '#4CAF50',
    icon: Sun,
    params: { saturation: 12, contrast: 8, brightness: 5, warmth: 0, cyanMagenta: 0, sharpness: 18, tone: 8, softLight: 15, vignette: false, filter: '原图' }
  },
  {
    id: 'portrait',
    name: '哈苏人像',
    desc: '柔美肤色，光影层次',
    color: '#E91E63',
    icon: Camera,
    params: { saturation: 8, contrast: 5, brightness: 8, warmth: 12, cyanMagenta: 0, sharpness: 12, tone: 5, softLight: 35, vignette: false, filter: '原图' }
  },
  {
    id: 'cinematic',
    name: '哈苏电影',
    desc: '电影质感，氛围感强',
    color: '#FF9800',
    icon: Moon,
    params: { saturation: 18, contrast: 22, brightness: 0, warmth: -5, cyanMagenta: 5, sharpness: 25, tone: 18, softLight: 20, vignette: true, filter: '胶片' }
  },
  {
    id: 'vintage',
    name: '哈苏复古',
    desc: '经典胶片，怀旧质感',
    color: '#795548',
    icon: Palette,
    params: { saturation: -5, contrast: 15, brightness: 0, warmth: 25, cyanMagenta: 0, sharpness: 15, tone: 20, softLight: 25, vignette: true, filter: '胶片' }
  },
];

// 优化选项 - 真实调整参数
const optimizeOptionsInit = [
  { id: 'hdr', name: 'HDR增强', desc: '动态范围优化', enabled: true, params: { contrast: 15, brightness: 8, softLight: 20 } },
  { id: 'noise', name: '智能降噪', desc: '噪点消除', enabled: true, params: { softLight: 30, brightness: 5 } },
  { id: 'sharp', name: '锐化增强', desc: '细节提升', enabled: true, params: { sharpness: 25 } },
  { id: 'color', name: '色彩优化', desc: '色彩校正', enabled: true, params: { saturation: 15, contrast: 8 } },
  { id: 'exposure', name: '曝光调整', desc: '亮度优化', enabled: false, params: { brightness: 15 } },
  { id: 'contrast', name: '对比度增强', desc: '层次感提升', enabled: false, params: { contrast: 25 } },
];

const SmartOptimizePage: React.FC = () => {
  const { goBack } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [isOptimizing, setIsOptimizing] = useState(false);
  const [optimizedImage, setOptimizedImage] = useState<string>('');
  const [selectedStyle, setSelectedStyle] = useState<string | null>(null);
  const [options, setOptions] = useState(optimizeOptionsInit);
  const [showComparison, setShowComparison] = useState(false);

  // 智能优化处理 - 真实算法
  const handleOptimize = async () => {
    if (!uploadedImage || !selectedStyle) return;

    setIsOptimizing(true);

    try {
      // 合并哈苏风格参数和优化选项参数
      const style = hasselbladStyles.find(s => s.id === selectedStyle);
      if (!style) return;

      // 基础参数
      const mergedParams: ImageAdjustParams = {
        saturation: style.params.saturation,
        contrast: style.params.contrast,
        brightness: style.params.brightness,
        warmth: style.params.warmth,
        cyanMagenta: style.params.cyanMagenta,
        sharpness: style.params.sharpness,
        tone: style.params.tone,
        softLight: style.params.softLight,
        vignette: style.params.vignette,
        filter: style.params.filter
      };

      // 叠加启用的优化选项参数
      options.filter(o => o.enabled).forEach(opt => {
        Object.entries(opt.params).forEach(([key, value]) => {
          if (key in mergedParams && typeof value === 'number') {
            (mergedParams as any)[key] = (mergedParams as any)[key] + (value as number);
          } else if (key === 'vignette' && value) {
            (mergedParams as any)[key] = true;
          }
        });
      });

      // 真实调用图片处理算法
      const result = await applyImageAdjustments(uploadedImage, mergedParams);
      setOptimizedImage(result);
    } catch (e) {
      console.error('优化失败:', e);
    } finally {
      setIsOptimizing(false);
    }
  };

  // 切换优化选项
  const toggleOption = (id: string) => {
    setOptions(prev => prev.map(opt => 
      opt.id === id ? { ...opt, enabled: !opt.enabled } : opt
    ));
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
        <h1 className="text-lg font-bold text-white">智能优化</h1>
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
          title="上传照片智能优化"
          description="AI分析并优化至哈苏大师出片"
        />
      </div>

      {/* Hasselblad Styles */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Sparkles size={12} />
            哈苏大师优化风格
          </p>
          <div className="grid grid-cols-2 gap-3">
            {hasselbladStyles.map((style) => {
              const Icon = style.icon;
              const isSelected = selectedStyle === style.id;
              
              return (
                <button
                  key={style.id}
                  onClick={() => setSelectedStyle(style.id)}
                  className={`p-4 rounded-2xl transition-all ${
                    isSelected 
                      ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' 
                      : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div 
                      className="w-12 h-12 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${style.color}20` }}
                    >
                      <Icon size={24} style={{ color: style.color }} />
                    </div>
                    <div>
                      <p className="text-white text-sm font-medium">{style.name}</p>
                      <p className="text-white/50 text-xs">{style.desc}</p>
                    </div>
                    {isSelected && (
                      <div className="ml-auto w-6 h-6 rounded-full bg-[#FF6B35] flex items-center justify-center">
                        <Check size={14} className="text-white" />
                      </div>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Optimize Options */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3">优化选项</p>
          <div className="space-y-2">
            {options.map((option) => (
              <div 
                key={option.id}
                onClick={() => toggleOption(option.id)}
                className="p-3 rounded-xl bg-white/5 flex items-center justify-between cursor-pointer hover:bg-white/10 transition-all"
              >
                <div>
                  <p className="text-white text-sm font-medium">{option.name}</p>
                  <p className="text-white/50 text-xs">{option.desc}</p>
                </div>
                <div className={`w-10 h-6 rounded-full transition-all ${
                  option.enabled ? 'bg-[#FF6B35]' : 'bg-white/10'
                }`}>
                  <div className={`w-5 h-5 rounded-full bg-white transition-all ${
                    option.enabled ? 'translate-x-5' : 'translate-x-0.5'
                  }`} />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Optimize Button */}
      {uploadedImage && selectedStyle && (
        <div className="px-4 pb-4">
          <button
            onClick={handleOptimize}
            disabled={isOptimizing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 disabled:opacity-50"
          >
            {isOptimizing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>AI优化中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>开始智能优化</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Result Preview */}
      {optimizedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3">优化结果</p>
          
          {/* Comparison Toggle */}
          <div className="flex gap-2 mb-3">
            <button
              onClick={() => setShowComparison(false)}
              className={`px-3 py-1.5 rounded-lg text-xs ${
                !showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/50'
              }`}
            >
              优化后
            </button>
            <button
              onClick={() => setShowComparison(true)}
              className={`px-3 py-1.5 rounded-lg text-xs ${
                showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/50'
              }`}
            >
              对比原图
            </button>
          </div>

          {/* Image Preview */}
          {showComparison ? (
            <div className="grid grid-cols-2 gap-2">
              <div className="rounded-xl overflow-hidden">
                <img src={uploadedImage} alt="Original" className="w-full aspect-video object-cover" />
                <div className="p-2 bg-white/5">
                  <span className="text-white/50 text-xs">原图</span>
                </div>
              </div>
              <div className="rounded-xl overflow-hidden">
                <img src={optimizedImage} alt="Optimized" className="w-full aspect-video object-cover" />
                <div className="p-2 bg-[#FF6B35]/10">
                  <span className="text-[#FF6B35] text-xs">哈苏大师优化</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl overflow-hidden relative">
              <img src={optimizedImage} alt="Optimized" className="w-full aspect-video object-cover" />
              <div className="absolute bottom-2 left-2 px-2 py-1 rounded-lg bg-[#FF6B35]/80 backdrop-blur-sm">
                <span className="text-white text-xs font-medium">哈苏大师出片</span>
              </div>
            </div>
          )}

          {/* Download Button */}
          <button
            onClick={() => {
              const style = hasselbladStyles.find(s => s.id === selectedStyle);
              downloadImage(optimizedImage, `OMaster_${style?.name || 'optimized'}_${Date.now()}.jpg`);
            }}
            className="w-full mt-3 py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90"
          >
            <Download size={18} />
            <span>保存哈苏大师出片</span>
          </button>
        </div>
      )}

      {/* Tips */}
      <div className="flex-1 px-4 pb-4">
        <div className="p-4 rounded-2xl bg-white/5">
          <div className="flex items-start gap-3">
            <Zap size={18} className="text-[#FF6B35] mt-0.5" />
            <div>
              <p className="text-white text-sm font-medium">智能优化说明</p>
              <ul className="text-white/50 text-xs mt-2 space-y-1">
                <li>• AI分析照片内容，自动选择最佳优化方案</li>
                <li>• 基于哈苏大师调色经验，呈现专业质感</li>
                <li>• 支持HDR增强、智能降噪、锐化增强等</li>
                <li>• 一键优化，轻松获得大师级出片效果</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SmartOptimizePage;