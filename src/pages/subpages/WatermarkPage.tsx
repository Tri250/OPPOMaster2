import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Type, Image, AlignLeft, AlignCenter, AlignRight, Check, Camera, Aperture, Clock, MapPin, Hash } from 'lucide-react';

interface WatermarkTemplate {
  id: string;
  name: string;
  brand: string;
  description: string;
  layers: WatermarkLayer[];
}

interface WatermarkLayer {
  type: 'brand' | 'device' | 'params' | 'date' | 'location' | 'logo';
  position: 'top-left' | 'top-center' | 'top-right' | 'middle-left' | 'middle-center' | 'middle-right' | 'bottom-left' | 'bottom-center' | 'bottom-right';
  text: string;
  size: 'small' | 'medium' | 'large';
  weight: 'normal' | 'bold';
  opacity: number;
  visible: boolean;
}

const watermarkTemplates: WatermarkTemplate[] = [
  {
    id: 'hasselblad_official',
    name: 'Hasselblad 官方',
    brand: '哈苏',
    description: 'HASSELBLAD 顶部标识 + 设备型号 + 拍摄参数',
    layers: [
      { type: 'brand', position: 'top-center', text: 'HASSELBLAD', size: 'medium', weight: 'bold', opacity: 0.9, visible: true },
      { type: 'device', position: 'middle-center', text: 'OPPO Find X8 Pro', size: 'small', weight: 'normal', opacity: 0.8, visible: true },
      { type: 'params', position: 'bottom-left', text: 'f/1.6  1/500s  ISO100', size: 'small', weight: 'normal', opacity: 0.7, visible: true },
    ],
  },
  {
    id: 'minimal',
    name: '极简风格',
    brand: '通用',
    description: '简洁单行水印',
    layers: [
      { type: 'brand', position: 'bottom-left', text: 'OM', size: 'small', weight: 'bold', opacity: 0.9, visible: true },
    ],
  },
  {
    id: 'detailed',
    name: '详细参数',
    brand: '专业',
    description: '完整拍摄参数信息',
    layers: [
      { type: 'brand', position: 'top-left', text: 'Shot on OMaster', size: 'small', weight: 'normal', opacity: 0.8, visible: true },
      { type: 'params', position: 'bottom-left', text: 'f/1.6  1/500s  ISO100  35mm', size: 'small', weight: 'normal', opacity: 0.7, visible: true },
      { type: 'date', position: 'bottom-right', text: '2026.06.07', size: 'small', weight: 'normal', opacity: 0.6, visible: true },
    ],
  },
  {
    id: 'oppo_find',
    name: 'OPPO Find 系列',
    brand: 'OPPO',
    description: 'OPPO Find 系列专属水印',
    layers: [
      { type: 'brand', position: 'top-center', text: 'OPPO Find', size: 'medium', weight: 'bold', opacity: 0.9, visible: true },
      { type: 'device', position: 'middle-center', text: 'Find X8 Pro | Hasselblad', size: 'small', weight: 'normal', opacity: 0.8, visible: true },
    ],
  },
  {
    id: 'oneplus_hasselblad',
    name: '一加哈苏',
    brand: '一加',
    description: '一加×哈苏联名水印',
    layers: [
      { type: 'brand', position: 'top-center', text: 'HASSELBLAD', size: 'medium', weight: 'bold', opacity: 0.9, visible: true },
      { type: 'device', position: 'middle-center', text: 'OnePlus 12', size: 'small', weight: 'normal', opacity: 0.8, visible: true },
    ],
  },
  {
    id: 'photographer_signature',
    name: '摄影师签名',
    brand: '通用',
    description: '自定义文字签名',
    layers: [
      { type: 'brand', position: 'bottom-right', text: 'Shot by @XXX', size: 'small', weight: 'normal', opacity: 0.8, visible: true },
    ],
  },
];

const positions = [
  { id: 'top-left', name: '左上' },
  { id: 'top-center', name: '上中' },
  { id: 'top-right', name: '右上' },
  { id: 'middle-left', name: '左中' },
  { id: 'middle-center', name: '正中' },
  { id: 'middle-right', name: '右中' },
  { id: 'bottom-left', name: '左下' },
  { id: 'bottom-center', name: '下中' },
  { id: 'bottom-right', name: '右下' },
];

const WatermarkPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [enabled, setEnabled] = useState(true);
  const [activeTemplate, setActiveTemplate] = useState('hasselblad_official');
  const [customText, setCustomText] = useState('HASSELBLAD');
  const [position, setPosition] = useState('bottom-left');
  const [showVignette, setShowVignette] = useState(false);
  const [showFrame, setShowFrame] = useState(true);

  const activeTemplateData = watermarkTemplates.find(t => t.id === activeTemplate);

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">水印编辑器</h1>
        <div className="ml-auto text-[10px] text-white/50">
          OPPO 专属
        </div>
      </div>

      {/* Preview */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-cyan-900/50 to-blue-900/50">
          <img 
            src="https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&h=400&fit=crop"
            alt="Preview"
            className="w-full h-full object-cover"
          />
          
          {/* Vignette Effect */}
          {showVignette && (
            <div className="absolute inset-0" 
                 style={{ background: 'radial-gradient(circle, transparent 50%, rgba(0,0,0,0.4) 100%)' }} />
          )}

          {/* Frame Border */}
          {showFrame && (
            <div className="absolute inset-3 border border-white/30 rounded-lg pointer-events-none" />
          )}

          {/* Watermark Layers */}
          {enabled && activeTemplateData && activeTemplateData.layers.map((layer, index) => {
            if (!layer.visible) return null;
            
            const sizeMap = { small: 'text-[10px]', medium: 'text-xs', large: 'text-sm' };
            const weightMap = { normal: 'font-normal', bold: 'font-bold' };
            const posClasses = {
              'top-left': 'top-3 left-3',
              'top-center': 'top-3 left-1/2 -translate-x-1/2',
              'top-right': 'top-3 right-3',
              'middle-left': 'top-1/2 -translate-y-1/2 left-3',
              'middle-center': 'top-1/2 -translate-y-1/2 left-1/2 -translate-x-1/2',
              'middle-right': 'top-1/2 -translate-y-1/2 right-3',
              'bottom-left': 'bottom-3 left-3',
              'bottom-center': 'bottom-3 left-1/2 -translate-x-1/2',
              'bottom-right': 'bottom-3 right-3',
            };
            
            return (
              <div 
                key={index}
                className={`absolute ${posClasses[layer.position]} px-2 py-1 rounded bg-black/40 backdrop-blur-sm text-white ${sizeMap[layer.size]} ${weightMap[layer.weight]}`}
                style={{ opacity: layer.opacity }}
              >
                {layer.text}
              </div>
            );
          })}
        </div>
      </div>

      {/* Enable Toggle */}
      <div className="px-4 pb-4">
        <button
          onClick={() => setEnabled(!enabled)}
          className={`w-full py-3 rounded-xl flex items-center justify-center gap-2 font-medium transition-all ${
            enabled 
              ? 'bg-gradient-to-r from-[#00BCD4] to-[#0097A7] text-white' 
              : 'bg-white/10 text-white/70'
          }`}
        >
          {enabled ? <Check size={18} /> : <Type size={18} />}
          <span>{enabled ? '水印已启用' : '启用水印'}</span>
        </button>
      </div>

      {/* Settings */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        {/* Templates */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-white text-sm font-bold">专业模板</h3>
            <span className="text-white/40 text-xs">{watermarkTemplates.length} 款</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {watermarkTemplates.map((template) => (
              <button
                key={template.id}
                onClick={() => setActiveTemplate(template.id)}
                className={`p-3 rounded-xl text-left transition-all ${
                  activeTemplate === template.id
                    ? 'bg-gradient-to-br from-[#00BCD4]/20 to-[#0097A7]/10 border border-[#00BCD4]/50'
                    : 'bg-white/5 border border-transparent hover:bg-white/10'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="text-white text-sm font-medium">{template.name}</span>
                  <span className="text-[10px] px-1.5 py-0.5 rounded bg-white/10 text-white/70">
                    {template.brand}
                  </span>
                </div>
                <span className="text-white/40 text-xs line-clamp-2">{template.description}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Custom Text */}
        <div className="mb-6">
          <label className="text-white/50 text-xs mb-2 block">自定义文字</label>
          <input
            type="text"
            value={customText}
            onChange={(e) => setCustomText(e.target.value)}
            placeholder="输入水印文字"
            className="w-full px-4 py-3 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#00BCD4] outline-none transition-colors"
          />
        </div>

        {/* Position Grid (9宫格) */}
        <div className="mb-6">
          <label className="text-white/50 text-xs mb-2 block">位置</label>
          <div className="grid grid-cols-3 gap-1.5 p-2 rounded-xl bg-white/5">
            {positions.map((pos) => (
              <button
                key={pos.id}
                onClick={() => setPosition(pos.id)}
                className={`py-3 rounded-lg flex items-center justify-center text-xs transition-all ${
                  position === pos.id
                    ? 'bg-[#00BCD4] text-white'
                    : 'bg-white/5 text-white/60 hover:bg-white/10'
                }`}
              >
                {pos.name}
              </button>
            ))}
          </div>
        </div>

        {/* Effects */}
        <div className="mb-6">
          <label className="text-white/50 text-xs mb-2 block">特效</label>
          <div className="space-y-2">
            <label className="flex items-center justify-between p-3 rounded-xl bg-white/5 cursor-pointer">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-[#00BCD4]/20 flex items-center justify-center">
                  <Image size={16} className="text-[#00BCD4]" />
                </div>
                <div>
                  <p className="text-white text-sm">相框装饰</p>
                  <p className="text-white/40 text-xs">添加白色边框</p>
                </div>
              </div>
              <input
                type="checkbox"
                checked={showFrame}
                onChange={(e) => setShowFrame(e.target.checked)}
                className="w-5 h-5 accent-[#00BCD4]"
              />
            </label>
            <label className="flex items-center justify-between p-3 rounded-xl bg-white/5 cursor-pointer">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-[#00BCD4]/20 flex items-center justify-center">
                  <Aperture size={16} className="text-[#00BCD4]" />
                </div>
                <div>
                  <p className="text-white text-sm">暗角效果</p>
                  <p className="text-white/40 text-xs">四周渐变暗角</p>
                </div>
              </div>
              <input
                type="checkbox"
                checked={showVignette}
                onChange={(e) => setShowVignette(e.target.checked)}
                className="w-5 h-5 accent-[#00BCD4]"
              />
            </label>
          </div>
        </div>

        {/* Info Card */}
        <div className="p-4 rounded-2xl bg-gradient-to-br from-[#00BCD4]/10 to-[#0097A7]/5 border border-[#00BCD4]/20">
          <div className="flex items-center gap-2 mb-2">
            <Camera size={14} className="text-[#00BCD4]" />
            <h4 className="text-white text-sm font-medium">拍摄信息自动识别</h4>
          </div>
          <p className="text-white/50 text-xs leading-relaxed">
            水印将自动从 EXIF 信息中读取光圈、快门、ISO、拍摄时间、GPS 位置等参数，呈现完整专业信息。
          </p>
          <div className="mt-3 grid grid-cols-2 gap-2">
            <div className="bg-black/30 rounded-lg p-2 flex items-center gap-1.5">
              <Aperture size={10} className="text-white/40" />
              <span className="text-white/70 text-[10px]">f/1.6</span>
            </div>
            <div className="bg-black/30 rounded-lg p-2 flex items-center gap-1.5">
              <Clock size={10} className="text-white/40" />
              <span className="text-white/70 text-[10px]">1/500s</span>
            </div>
            <div className="bg-black/30 rounded-lg p-2 flex items-center gap-1.5">
              <Hash size={10} className="text-white/40" />
              <span className="text-white/70 text-[10px]">ISO 100</span>
            </div>
            <div className="bg-black/30 rounded-lg p-2 flex items-center gap-1.5">
              <MapPin size={10} className="text-white/40" />
              <span className="text-white/70 text-[10px]">北京·朝阳</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WatermarkPage;
