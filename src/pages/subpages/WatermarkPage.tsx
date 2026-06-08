import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Type, Calendar, MapPin, User, Camera, Palette, Grid, Sparkles, Check, Wand2, RefreshCw, Image, Frame, Layout } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';

// 水印模板
const watermarkTemplates = [
  { id: 'classic', name: '经典水印', icon: Camera, color: '#FF6B35' },
  { id: 'minimal', name: '极简水印', icon: Type, color: '#607D8B' },
  { id: 'date', name: '日期水印', icon: Calendar, color: '#4CAF50' },
  { id: 'location', name: '地点水印', icon: MapPin, color: '#2196F3' },
  { id: 'author', name: '作者水印', icon: User, color: '#E91E63' },
  { id: 'custom', name: '自定义', icon: Palette, color: '#FF9800' },
];

// 边框风格 - 2026年新增
const frameStyles = [
  { id: 'classic', name: '经典白框', color: '#FFFFFF', width: 20 },
  { id: 'film', name: '胶片边框', color: '#1a1a1a', width: 15 },
  { id: 'polaroid', name: '拍立得', color: '#F5F5DC', width: 25 },
  { id: 'minimal', name: '极简细框', color: '#E0E0E0', width: 5 },
  { id: 'art', name: '艺术画框', color: '#8B4513', width: 30 },
  { id: 'gradient', name: '渐变边框', color: 'gradient', width: 15 },
];

// 文学治愈系边框
const literaryFrames = [
  { id: 'poetry', name: '诗意边框', desc: '淡雅文字装饰', color: '#F0E6D3' },
  { id: 'healing', name: '治愈系', desc: '温暖柔和色调', color: '#FFE4E1' },
  { id: 'vintage', name: '复古文艺', desc: '怀旧质感', color: '#D4A574' },
  { id: 'nature', name: '自然清新', desc: '植物元素', color: '#E8F5E9' },
  { id: 'dream', name: '梦幻浪漫', desc: '星空元素', color: '#E3F2FD' },
  { id: 'zen', name: '禅意简约', desc: '东方美学', color: '#F5F5F5' },
];

// 拼图方式
const collageStyles = [
  { id: 'grid2', name: '二宫格', layout: 'grid-cols-2' },
  { id: 'grid3', name: '三宫格', layout: 'grid-cols-3' },
  { id: 'grid4', name: '四宫格', layout: 'grid-cols-2 grid-rows-2' },
  { id: 'film', name: '胶片条', layout: 'flex-col' },
  { id: 'story', name: '故事板', layout: 'grid-cols-1' },
  { id: 'free', name: '自由拼', layout: 'free' },
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
  const [selectedWatermark, setSelectedWatermark] = useState<string>('classic');
  const [selectedFrame, setSelectedFrame] = useState<string | null>(null);
  const [selectedLiteraryFrame, setSelectedLiteraryFrame] = useState<string | null>(null);
  const [selectedCollage, setSelectedCollage] = useState<string | null>(null);
  const [selectedBrand, setSelectedBrand] = useState<string | null>(null);
  const [customText, setCustomText] = useState('');
  const [showYear, setShowYear] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  const year = '2026';

  // 模拟处理图片
  const handleProcess = () => {
    if (!uploadedImage) return;
    setIsProcessing(true);
    setTimeout(() => setIsProcessing(false), 1500);
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

      {/* Image Upload */}
      <div className="px-4 py-4">
        <ImageUploader 
          onImageSelect={setUploadedImage}
          currentImage={uploadedImage}
          title="上传照片添加水印"
          description="支持自定义水印、边框、拼图"
        />
      </div>

      {/* Preview with Effects */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <div className={`relative rounded-2xl overflow-hidden ${
            selectedLiteraryFrame ? 'p-4' : ''
          }`} style={{
            backgroundColor: selectedLiteraryFrame 
              ? literaryFrames.find(f => f.id === selectedLiteraryFrame)?.color 
              : undefined
          }}>
            <img
              src={uploadedImage}
              alt="Preview"
              className="w-full aspect-video object-cover"
              style={{
                borderWidth: selectedFrame ? `${frameStyles.find(f => f.id === selectedFrame)?.width}px` : 0,
                borderColor: selectedFrame && frameStyles.find(f => f.id === selectedFrame)?.color !== 'gradient'
                  ? frameStyles.find(f => f.id === selectedFrame)?.color
                  : undefined,
                borderImage: selectedFrame === 'gradient' 
                  ? 'linear-gradient(45deg, #FF6B35, #FFC107, #4CAF50) 1' 
                  : undefined,
              }}
            />
            
            {/* Watermark Overlay */}
            {selectedWatermark && (
              <div className="absolute bottom-3 right-3 flex items-center gap-2 px-3 py-1.5 rounded-lg bg-black/50 backdrop-blur-sm">
                {showYear && <span className="text-white/70 text-xs">{year}</span>}
                {selectedBrand && (
                  <span className="text-white text-xs font-bold">
                    {brandWatermarks.find(b => b.id === selectedBrand)?.name}
                  </span>
                )}
                {customText && (
                  <span className="text-white/70 text-xs">{customText}</span>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Process Button */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <button
            onClick={handleProcess}
            disabled={isProcessing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 disabled:opacity-50"
          >
            {isProcessing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>处理中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>应用水印效果</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Watermark Templates */}
      <div className="px-4 py-3">
        <p className="text-white/50 text-xs mb-3">水印模板</p>
        <div className="grid grid-cols-3 gap-2">
          {watermarkTemplates.map((template) => {
            const Icon = template.icon;
            const isSelected = selectedWatermark === template.id;
            
            return (
              <button
                key={template.id}
                onClick={() => setSelectedWatermark(template.id)}
                className={`p-3 rounded-xl flex flex-col items-center gap-2 transition-all ${
                  isSelected 
                    ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' 
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <Icon size={20} style={{ color: template.color }} />
                <span className="text-white text-xs">{template.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Brand Watermarks */}
      <div className="px-4 py-3">
        <p className="text-white/50 text-xs mb-3">品牌水印</p>
        <div className="flex gap-2">
          {brandWatermarks.map((brand) => {
            const isSelected = selectedBrand === brand.id;
            
            return (
              <button
                key={brand.id}
                onClick={() => setSelectedBrand(brand.id)}
                className={`px-3 py-2 rounded-xl text-xs font-medium transition-all ${
                  isSelected 
                    ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' 
                    : 'bg-white/5 hover:bg-white/10'
                }`}
                style={{ color: isSelected ? '#FF6B35' : brand.color }}
              >
                {brand.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Year Toggle */}
      <div className="px-4 py-3">
        <div className="flex items-center justify-between p-3 rounded-xl bg-white/5">
          <div className="flex items-center gap-2">
            <Calendar size={16} className="text-[#FF6B35]" />
            <span className="text-white text-sm">显示年份</span>
          </div>
          <button
            onClick={() => setShowYear(!showYear)}
            className={`w-12 h-6 rounded-full transition-all ${
              showYear ? 'bg-[#FF6B35]' : 'bg-white/10'
            }`}
          >
            <div className={`w-5 h-5 rounded-full bg-white transition-all ${
              showYear ? 'translate-x-6' : 'translate-x-0.5'
            }`} />
          </button>
        </div>
      </div>

      {/* Custom Text */}
      <div className="px-4 py-3">
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

      {/* Frame Styles */}
      <div className="px-4 py-3">
        <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
          <Frame size={12} />
          边框风格 (2026年新增)
        </p>
        <div className="grid grid-cols-3 gap-2">
          {frameStyles.map((frame) => {
            const isSelected = selectedFrame === frame.id;
            
            return (
              <button
                key={frame.id}
                onClick={() => setSelectedFrame(frame.id)}
                className={`p-3 rounded-xl flex flex-col items-center gap-2 transition-all ${
                  isSelected 
                    ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' 
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div 
                  className="w-8 h-8 rounded-lg"
                  style={{
                    backgroundColor: frame.color === 'gradient' ? '#FF6B35' : frame.color,
                    border: `2px solid ${frame.color === 'gradient' ? '#FF6B35' : frame.color}`,
                  }}
                />
                <span className="text-white text-xs">{frame.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Literary Frames */}
      <div className="px-4 py-3">
        <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
          <Sparkles size={12} />
          文学治愈系边框
        </p>
        <div className="grid grid-cols-2 gap-2">
          {literaryFrames.map((frame) => {
            const isSelected = selectedLiteraryFrame === frame.id;
            
            return (
              <button
                key={frame.id}
                onClick={() => setSelectedLiteraryFrame(frame.id)}
                className={`p-3 rounded-xl transition-all ${
                  isSelected 
                    ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' 
                    : 'bg-white/5 hover:bg-white/10'
                }`}
                style={{ backgroundColor: isSelected ? undefined : `${frame.color}20` }}
              >
                <div className="flex items-center gap-3">
                  <div 
                    className="w-10 h-10 rounded-lg"
                    style={{ backgroundColor: frame.color }}
                  />
                  <div>
                    <p className="text-white text-sm font-medium">{frame.name}</p>
                    <p className="text-white/50 text-xs">{frame.desc}</p>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Collage Styles */}
      <div className="px-4 py-3 pb-6">
        <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
          <Layout size={12} />
          拼图方式
        </p>
        <div className="grid grid-cols-3 gap-2">
          {collageStyles.map((collage) => {
            const isSelected = selectedCollage === collage.id;
            
            return (
              <button
                key={collage.id}
                onClick={() => setSelectedCollage(collage.id)}
                className={`p-3 rounded-xl flex flex-col items-center gap-2 transition-all ${
                  isSelected 
                    ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' 
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <Grid size={20} className="text-[#FF6B35]" />
                <span className="text-white text-xs">{collage.name}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default WatermarkPage;