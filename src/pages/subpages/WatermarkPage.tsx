import React, { useState, useRef } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Type, Calendar, MapPin, User, Camera, Palette, Grid, Sparkles, Check, Wand2, RefreshCw, Image as ImageIcon, Frame, Layout, Download, Plus, X } from 'lucide-react';
import { Film as FilmIcon } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { addWatermarkToImage, addFrameToImage, createCollage, downloadImage, FrameStyle, WatermarkOptions } from '../../utils/imageProcessor';

// 水印模板
const watermarkTemplates = [
  { id: 'classic', name: '经典水印', icon: Camera, color: '#FF6B35' },
  { id: 'minimal', name: '极简水印', icon: Type, color: '#607D8B' },
  { id: 'date', name: '日期水印', icon: Calendar, color: '#4CAF50' },
  { id: 'location', name: '地点水印', icon: MapPin, color: '#2196F3' },
  { id: 'author', name: '作者水印', icon: User, color: '#E91E63' },
  { id: 'custom', name: '自定义', icon: Palette, color: '#FF9800' },
];

// 边框风格 - 真实实现
const frameStyles: (FrameStyle & { desc: string; icon?: any })[] = [
  { id: 'classic', name: '经典白框', type: 'solid', width: 40, color: '#FFFFFF', desc: '简约白色边框' },
  { id: 'film', name: '胶片边框', type: 'solid', width: 50, color: '#1a1a1a', desc: '复古胶片质感' },
  { id: 'polaroid', name: '拍立得', type: 'solid', width: 80, color: '#F5F5DC', desc: '拍立得风格' },
  { id: 'minimal', name: '极简细框', type: 'solid', width: 20, color: '#E0E0E0', desc: '细线极简' },
  { id: 'art', name: '艺术画框', type: 'solid', width: 60, color: '#8B4513', desc: '古典艺术' },
  { id: 'gradient', name: '渐变边框', type: 'gradient', width: 40, gradient: ['#FF6B35', '#FFC107', '#4CAF50'], desc: '彩色渐变' },
  { id: 'literary', name: '诗意边框', type: 'literary', width: 50, background: '#F0E6D3', decorations: ['poetry'], desc: '淡雅文学' },
  { id: 'healing', name: '治愈系', type: 'literary', width: 50, background: '#FFE4E1', decorations: ['healing'], desc: '温暖柔和' },
  { id: 'vintage', name: '复古文艺', type: 'literary', width: 50, background: '#D4A574', decorations: ['vintage'], desc: '怀旧质感' },
  { id: 'nature', name: '自然清新', type: 'literary', width: 50, background: '#E8F5E9', decorations: ['nature'], desc: '植物元素' },
  { id: 'dream', name: '梦幻浪漫', type: 'literary', width: 50, background: '#E3F2FD', decorations: ['dream'], desc: '星空元素' },
  { id: 'zen', name: '禅意简约', type: 'literary', width: 50, background: '#F5F5F5', decorations: ['zen'], desc: '东方美学' },
];

// 拼图方式
const collageStyles = [
  { id: 'grid2', name: '二宫格', layout: 'grid2' as const, icon: Grid, count: 2 },
  { id: 'grid3', name: '三宫格', layout: 'grid3' as const, icon: Grid, count: 3 },
  { id: 'grid4', name: '四宫格', layout: 'grid4' as const, icon: Grid, count: 4 },
  { id: 'film', name: '胶片条', layout: 'film' as const, icon: FilmIcon, count: 6 },
  { id: 'story', name: '故事板', layout: 'story' as const, icon: Layout, count: 4 },
  { id: 'free', name: '自由拼', layout: 'free' as const, icon: Sparkles, count: 4 },
];

// 品牌水印
const brandWatermarks = [
  { id: 'oppo', name: 'OPPO', color: '#1BAA52' },
  { id: 'hasselblad', name: 'HASSELBLAD', color: '#FFD700' },
  { id: 'vivo', name: 'vivo', color: '#415FFF' },
  { id: 'huawei', name: 'HUAWEI', color: '#CF0A2C' },
  { id: 'xiaomi', name: '小米', color: '#FF6900' },
  { id: 'apple', name: 'Shot on iPhone', color: '#FFFFFF' },
];

const WatermarkPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [processedImage, setProcessedImage] = useState<string>('');
  const [selectedWatermark, setSelectedWatermark] = useState<string>('classic');
  const [selectedFrame, setSelectedFrame] = useState<string | null>(null);
  const [selectedCollage, setSelectedCollage] = useState<string | null>(null);
  const [selectedBrand, setSelectedBrand] = useState<string | null>('oppo');
  const [customText, setCustomText] = useState('');
  const [showYear, setShowYear] = useState(true);
  const [watermarkPosition, setWatermarkPosition] = useState<WatermarkOptions['position']>('bottom-right');
  const [isProcessing, setIsProcessing] = useState(false);
  
  // 拼图模式
  const [collageImages, setCollageImages] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const year = '2026';

  // 单图处理 - 应用水印和边框
  const handleProcess = async () => {
    if (!uploadedImage) return;
    setIsProcessing(true);
    
    try {
      let result = uploadedImage;
      
      // 应用水印
      if (selectedWatermark) {
        result = await addWatermarkToImage(result, {
          text: customText,
          brand: selectedBrand ? brandWatermarks.find(b => b.id === selectedBrand)?.name : undefined,
          year: showYear ? year : undefined,
          position: watermarkPosition,
          fontSize: 24,
          color: '#FFFFFF',
          opacity: 0.95,
          showYear,
        });
      }
      
      // 应用边框
      if (selectedFrame) {
        const frame = frameStyles.find(f => f.id === selectedFrame);
        if (frame) {
          result = await addFrameToImage(result, frame);
        }
      }
      
      setProcessedImage(result);
    } catch (e) {
      console.error('处理失败', e);
      alert('图片处理失败：' + (e as Error).message);
    } finally {
      setIsProcessing(false);
    }
  };

  // 拼图处理
  const handleCollage = async () => {
    if (collageImages.length < 2) {
      alert('请至少上传2张图片');
      return;
    }
    if (!selectedCollage) {
      alert('请选择拼图样式');
      return;
    }
    
    setIsProcessing(true);
    try {
      const layout = collageStyles.find(c => c.id === selectedCollage)?.layout || 'grid2';
      const result = await createCollage(collageImages, layout, {
        gap: 10,
        background: '#1a1a1a'
      });
      setProcessedImage(result);
    } catch (e) {
      console.error('拼图失败', e);
      alert('拼图生成失败：' + (e as Error).message);
    } finally {
      setIsProcessing(false);
    }
  };

  // 拼图上传图片
  const handleCollageImageAdd = (url: string) => {
    setCollageImages(prev => [...prev, url].slice(0, 9));
  };

  const removeCollageImage = (index: number) => {
    setCollageImages(prev => prev.filter((_, i) => i !== index));
  };

  // 下载处理后的图片
  const handleDownload = () => {
    if (processedImage) {
      downloadImage(processedImage, `omaster_${Date.now()}.jpg`);
    }
  };

  // 重置
  const handleReset = () => {
    setProcessedImage('');
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
        <h1 className="text-lg font-bold text-white">水印编辑器</h1>
        {uploadedImage && (
          <div className="ml-auto px-2 py-1 rounded-full bg-[#FF6B35]/20">
            <span className="text-[#FF6B35] text-xs">已上传照片</span>
          </div>
        )}
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* 单图模式 */}
        <div className="px-4 py-4">
          <ImageUploader 
            onImageSelect={(url) => { setUploadedImage(url); setProcessedImage(''); }}
            currentImage={uploadedImage}
            title="上传照片添加水印"
            description="支持水印、边框、拼图"
          />
        </div>

        {/* 预览与下载 */}
        {(uploadedImage || processedImage) && (
          <div className="px-4 pb-4">
            <div className="flex items-center justify-between mb-3">
              <p className="text-white/50 text-xs">预览</p>
              <div className="flex gap-2">
                {processedImage && (
                  <button
                    onClick={handleReset}
                    className="p-2 rounded-lg bg-white/10 hover:bg-white/20"
                  >
                    <RefreshCw size={16} className="text-white" />
                  </button>
                )}
                {processedImage && (
                  <button
                    onClick={handleDownload}
                    className="p-2 rounded-lg bg-[#4CAF50]/20 hover:bg-[#4CAF50]/30"
                  >
                    <Download size={16} className="text-[#4CAF50]" />
                  </button>
                )}
              </div>
            </div>
            <div className="rounded-2xl overflow-hidden bg-[#1a1a1a]">
              <img
                src={processedImage || uploadedImage}
                alt="Preview"
                className="w-full max-h-96 object-contain mx-auto"
              />
            </div>
            {processedImage && (
              <div className="mt-2 p-2 rounded-lg bg-[#4CAF50]/10 border border-[#4CAF50]/30">
                <p className="text-[#4CAF50] text-xs text-center">✓ 已合成最终效果</p>
              </div>
            )}
          </div>
        )}

        {/* 单图处理按钮 */}
        {uploadedImage && (
          <div className="px-4 pb-4">
            <button
              onClick={handleProcess}
              disabled={isProcessing}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium disabled:opacity-50"
            >
              {isProcessing ? (
                <>
                  <RefreshCw size={18} className="animate-spin" />
                  <span>处理中...</span>
                </>
              ) : (
                <>
                  <Wand2 size={18} />
                  <span>应用水印和边框</span>
                </>
              )}
            </button>
          </div>
        )}

        {/* 边框风格 */}
        {uploadedImage && (
          <div className="px-4 py-3">
            <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
              <Frame size={12} />
              边框风格 (真实合成)
            </p>
            <div className="grid grid-cols-3 gap-2">
              {frameStyles.map((frame) => {
                const isSelected = selectedFrame === frame.id;
                return (
                  <button
                    key={frame.id}
                    onClick={() => setSelectedFrame(frame.id)}
                    className={`p-3 rounded-xl flex flex-col items-center gap-1 transition-all ${
                      isSelected ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' : 'bg-white/5 hover:bg-white/10'
                    }`}
                  >
                    <div 
                      className="w-10 h-10 rounded-lg"
                      style={{
                        backgroundColor: frame.gradient ? '#FF6B35' : 
                                         frame.background || frame.color || '#FFFFFF',
                        background: frame.gradient ? `linear-gradient(45deg, ${frame.gradient.join(', ')})` : undefined,
                        border: `2px solid ${frame.color || '#FFFFFF'}`,
                      }}
                    />
                    <span className="text-white text-xs">{frame.name}</span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* 品牌水印 */}
        {uploadedImage && (
          <div className="px-4 py-3">
            <p className="text-white/50 text-xs mb-3">品牌水印</p>
            <div className="flex gap-2 flex-wrap">
              {brandWatermarks.map((brand) => {
                const isSelected = selectedBrand === brand.id;
                return (
                  <button
                    key={brand.id}
                    onClick={() => setSelectedBrand(brand.id)}
                    className={`px-3 py-2 rounded-xl text-xs font-medium transition-all ${
                      isSelected ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' : 'bg-white/5 hover:bg-white/10'
                    }`}
                    style={{ color: isSelected ? '#FF6B35' : brand.color }}
                  >
                    {brand.name}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* 水印位置 */}
        {uploadedImage && (
          <div className="px-4 py-3">
            <p className="text-white/50 text-xs mb-3">水印位置</p>
            <div className="grid grid-cols-3 gap-2">
              {(['top-left', 'top-right', 'bottom-left', 'bottom-right', 'center'] as const).map((pos) => {
                const labels: Record<string, string> = {
                  'top-left': '左上',
                  'top-right': '右上',
                  'bottom-left': '左下',
                  'bottom-right': '右下',
                  'center': '居中',
                };
                const isSelected = watermarkPosition === pos;
                return (
                  <button
                    key={pos}
                    onClick={() => setWatermarkPosition(pos)}
                    className={`p-2 rounded-xl text-xs transition-all ${
                      isSelected ? 'bg-[#FF6B35]/20 border border-[#FF6B35] text-[#FF6B35]' : 'bg-white/5 text-white/70'
                    }`}
                  >
                    {labels[pos]}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* 年份和自定义文字 */}
        {uploadedImage && (
          <div className="px-4 py-3 space-y-3">
            <div className="flex items-center justify-between p-3 rounded-xl bg-white/5">
              <div className="flex items-center gap-2">
                <Calendar size={16} className="text-[#FF6B35]" />
                <span className="text-white text-sm">显示2026年份</span>
              </div>
              <button
                onClick={() => setShowYear(!showYear)}
                className={`w-12 h-6 rounded-full transition-all ${showYear ? 'bg-[#FF6B35]' : 'bg-white/10'}`}
              >
                <div className={`w-5 h-5 rounded-full bg-white transition-all ${showYear ? 'translate-x-6' : 'translate-x-0.5'}`} />
              </button>
            </div>
            
            <div className="p-3 rounded-xl bg-white/5">
              <div className="flex items-center gap-2 mb-2">
                <Type size={16} className="text-[#FF6B35]" />
                <span className="text-white text-sm">自定义文字</span>
              </div>
              <input
                type="text"
                value={customText}
                onChange={(e) => setCustomText(e.target.value)}
                placeholder="输入自定义水印文字..."
                className="w-full px-3 py-2 rounded-lg bg-white/10 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none"
              />
            </div>
          </div>
        )}

        {/* 拼图模式 */}
        <div className="px-4 py-4 border-t border-white/5">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Layout size={12} />
            拼图模式 (真实合成)
          </p>
          
          {/* 上传拼图图片 */}
          <div className="grid grid-cols-3 gap-2 mb-3">
            {collageImages.map((img, idx) => (
              <div key={idx} className="relative aspect-square rounded-lg overflow-hidden bg-white/5">
                <img src={img} alt={`Collage ${idx}`} className="w-full h-full object-cover" />
                <button
                  onClick={() => removeCollageImage(idx)}
                  className="absolute top-1 right-1 p-1 rounded-full bg-red-500/80"
                >
                  <X size={12} className="text-white" />
                </button>
              </div>
            ))}
            {collageImages.length < 9 && (
              <button
                onClick={() => fileInputRef.current?.click()}
                className="aspect-square rounded-lg border-2 border-dashed border-white/20 hover:border-[#FF6B35] flex items-center justify-center"
              >
                <Plus size={24} className="text-white/40" />
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  multiple
                  className="hidden"
                  onChange={(e) => {
                    const files = e.target.files;
                    if (files) {
                      Array.from(files).slice(0, 9 - collageImages.length).forEach(file => {
                        const reader = new FileReader();
                        reader.onload = (ev) => {
                          if (ev.target?.result) handleCollageImageAdd(ev.target.result as string);
                        };
                        reader.readAsDataURL(file);
                      });
                    }
                  }}
                />
              </button>
            )}
          </div>
          
          {/* 拼图样式选择 */}
          <div className="grid grid-cols-3 gap-2 mb-3">
            {collageStyles.map((style) => {
              const Icon = style.icon;
              const isSelected = selectedCollage === style.id;
              return (
                <button
                  key={style.id}
                  onClick={() => setSelectedCollage(style.id)}
                  className={`p-3 rounded-xl flex flex-col items-center gap-1 transition-all ${
                    isSelected ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <Icon size={20} className="text-[#FF6B35]" />
                  <span className="text-white text-xs">{style.name}</span>
                  <span className="text-white/40 text-[10px]">{style.count}张</span>
                </button>
              );
            })}
          </div>

          {/* 生成拼图按钮 */}
          <button
            onClick={handleCollage}
            disabled={collageImages.length < 2 || isProcessing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#4CAF50] to-[#2E7D32] flex items-center justify-center gap-2 text-white font-medium disabled:opacity-50"
          >
            {isProcessing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>生成中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>生成拼图 ({collageImages.length}张)</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

const Film = ({ size, className }: { size: number; className?: string }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={className}>
    <rect x="2" y="6" width="20" height="12" rx="2" />
    <line x1="6" y1="6" x2="6" y2="18" />
    <line x1="10" y1="6" x2="10" y2="18" />
    <line x1="14" y1="6" x2="14" y2="18" />
    <line x1="18" y1="6" x2="18" y2="18" />
  </svg>
);

export default WatermarkPage;