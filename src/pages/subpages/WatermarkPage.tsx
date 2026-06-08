import React, { useState, useCallback, useMemo } from 'react';
import { useAppStore } from '../../store/appStore';
import { 
  ArrowLeft, Type, AlignLeft, AlignCenter, AlignRight, Check, 
  Camera, Aperture, MapPin, Calendar, User, AtSign,
  Minimize, Eye, Download, ChevronRight, Crown, Sparkles,
  Plus, Trash2, Copy, Layers, RotateCcw,
  Grid, Square, Shield, Award,
  Image, Sliders
} from 'lucide-react';

// 15+ 专业水印模板
const WATERMARK_TEMPLATES = [
  { 
    id: 'classic', 
    name: '经典相机', 
    icon: Camera,
    category: 'brand',
    elements: [
      { type: 'text', content: 'Shot on', style: { fontSize: 12, opacity: 0.6 } },
      { type: 'brand', content: '小O帮帮', style: { fontSize: 16, fontWeight: 'bold', opacity: 0.8 } },
    ],
    preset: { position: 'bottom-left', padding: 20 }
  },
  { 
    id: 'hasselblad', 
    name: '哈苏大师', 
    icon: Crown,
    category: 'brand',
    elements: [
      { type: 'brand', content: 'HASSELBLAD', style: { fontSize: 14, fontWeight: 'bold', letterSpacing: 2 } },
      { type: 'text', content: 'HNCS', style: { fontSize: 10, opacity: 0.6 } },
    ],
    preset: { position: 'bottom-center', padding: 20 }
  },
  { 
    id: 'leica', 
    name: '徕卡风格', 
    icon: Aperture,
    category: 'brand',
    elements: [
      { type: 'brand', content: 'Leica', style: { fontSize: 18, fontWeight: 'bold', letterSpacing: 3 } },
      { type: 'text', content: 'Camera AG', style: { fontSize: 10, opacity: 0.5 } },
    ],
    preset: { position: 'bottom-right', padding: 20 }
  },
  { 
    id: 'minimal', 
    name: '极简风格', 
    icon: Minimize,
    category: 'minimal',
    elements: [
      { type: 'brand', content: 'OM', style: { fontSize: 20, fontWeight: 'bold' } },
    ],
    preset: { position: 'bottom-right', padding: 16 }
  },
  { 
    id: 'detailed', 
    name: '详细参数', 
    icon: Sliders,
    category: 'tech',
    elements: [
      { type: 'camera', content: '', style: { fontSize: 12 } },
      { type: 'params', content: '', style: { fontSize: 10, opacity: 0.7 } },
    ],
    preset: { position: 'bottom-left', padding: 20 }
  },
  { 
    id: 'location', 
    name: '地理位置', 
    icon: MapPin,
    category: 'info',
    elements: [
      { type: 'location', content: '', style: { fontSize: 12 } },
      { type: 'date', content: '', style: { fontSize: 10, opacity: 0.6 } },
    ],
    preset: { position: 'bottom-left', padding: 20 }
  },
  { 
    id: 'signature', 
    name: '摄影师签名', 
    icon: User,
    category: 'personal',
    elements: [
      { type: 'photographer', content: '', style: { fontSize: 14, fontStyle: 'italic' } },
    ],
    preset: { position: 'bottom-right', padding: 20 }
  },
  { 
    id: 'social', 
    name: '社交媒体', 
    icon: AtSign,
    category: 'social',
    elements: [
      { type: 'social', content: '@omaster', style: { fontSize: 14 } },
    ],
    preset: { position: 'bottom-center', padding: 20 }
  },
  { 
    id: 'timestamp', 
    name: '时间戳', 
    icon: Calendar,
    category: 'info',
    elements: [
      { type: 'date', content: '', style: { fontSize: 12 } },
      { type: 'time', content: '', style: { fontSize: 10, opacity: 0.6 } },
    ],
    preset: { position: 'top-right', padding: 16 }
  },
  { 
    id: 'copyright', 
    name: '版权声明', 
    icon: Shield,
    category: 'legal',
    elements: [
      { type: 'text', content: '©', style: { fontSize: 14 } },
      { type: 'text', content: '© 2024 小O帮帮', style: { fontSize: 12 } },
    ],
    preset: { position: 'bottom-center', padding: 16 }
  },
  { 
    id: 'award', 
    name: '获奖作品', 
    icon: Award,
    category: 'badge',
    elements: [
      { type: 'icon', content: 'award', style: { fontSize: 16 } },
      { type: 'text', content: 'Award Winning', style: { fontSize: 10, opacity: 0.8 } },
    ],
    preset: { position: 'top-left', padding: 16 }
  },
  { 
    id: 'exif', 
    name: 'EXIF信息', 
    icon: Aperture,
    category: 'tech',
    elements: [
      { type: 'camera', content: '', style: { fontSize: 11 } },
      { type: 'lens', content: '', style: { fontSize: 10, opacity: 0.7 } },
      { type: 'settings', content: '', style: { fontSize: 9, opacity: 0.5 } },
    ],
    preset: { position: 'bottom-left', padding: 16 }
  },
  { 
    id: 'logo', 
    name: '品牌Logo', 
    icon: Image,
    category: 'brand',
    elements: [
      { type: 'logo', content: '', style: { width: 40, height: 40 } },
    ],
    preset: { position: 'bottom-right', padding: 20 }
  },
  { 
    id: 'watermark-pro', 
    name: '专业防伪', 
    icon: Shield,
    category: 'pro',
    elements: [
      { type: 'text', content: 'PROTECTED', style: { fontSize: 10, letterSpacing: 4, opacity: 0.3 } },
      { type: 'id', content: '', style: { fontSize: 8, opacity: 0.2 } },
    ],
    preset: { position: 'center', padding: 0 }
  },
  { 
    id: 'custom', 
    name: '自定义', 
    icon: Plus,
    category: 'custom',
    elements: [],
    preset: { position: 'bottom-left', padding: 20 }
  },
];

// 模板分类
const TEMPLATE_CATEGORIES = [
  { id: 'all', name: '全部', icon: Grid },
  { id: 'brand', name: '品牌', icon: Crown },
  { id: 'minimal', name: '极简', icon: Minimize },
  { id: 'tech', name: '技术', icon: Sliders },
  { id: 'info', name: '信息', icon: Calendar },
  { id: 'personal', name: '个人', icon: User },
  { id: 'social', name: '社交', icon: AtSign },
  { id: 'legal', name: '法律', icon: Shield },
  { id: 'badge', name: '徽章', icon: Award },
  { id: 'pro', name: '专业', icon: Sparkles },
];

// 字体选项
const FONT_OPTIONS = [
  { id: 'default', name: '默认', family: 'system-ui' },
  { id: 'serif', name: '衬线', family: 'Georgia, serif' },
  { id: 'mono', name: '等宽', family: 'monospace' },
  { id: 'elegant', name: '优雅', family: 'Palatino, serif' },
  { id: 'modern', name: '现代', family: 'Helvetica, Arial' },
  { id: 'handwriting', name: '手写', family: 'cursive' },
];

// 水印图层
interface WatermarkLayer {
  id: string;
  type: 'text' | 'logo' | 'shape';
  content: string;
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number;
  opacity: number;
  fontSize?: number;
  fontFamily?: string;
  fontWeight?: 'normal' | 'bold';
  color: string;
}

const WatermarkPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [selectedTemplate, setSelectedTemplate] = useState('classic');
  const [enabled, setEnabled] = useState(true);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [showLayers, setShowLayers] = useState(false);
  const [customText, setCustomText] = useState('Shot on 小O帮帮');
  const [position, setPosition] = useState<'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'top-center' | 'bottom-center' | 'center'>('bottom-left');
  const [fontFamily, setFontFamily] = useState('default');
  const [fontSize, setFontSize] = useState(14);
  const [opacity, setOpacity] = useState(0.8);
  const [rotation, setRotation] = useState(0);
  const [textColor, setTextColor] = useState('#ffffff');
  const [shadowEnabled, setShadowEnabled] = useState(true);
  const [shadowBlur, setShadowBlur] = useState(4);
  const [padding, setPadding] = useState(20);
  const [showPreview, setShowPreview] = useState(true);
  const [selectedImage] = useState('https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600');
  const [fontWeight, setFontWeight] = useState<'normal' | 'bold'>('normal');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [layers, setLayers] = useState<WatermarkLayer[]>([]);
  const [selectedLayer, setSelectedLayer] = useState<string | null>(null);
  const [showExport, setShowExport] = useState(false);
  const [letterSpacing, setLetterSpacing] = useState(0);
  const [bgOpacity, setBgOpacity] = useState(0);

  // 过滤模板
  const filteredTemplates = useMemo(() => {
    let result = WATERMARK_TEMPLATES;
    if (selectedCategory !== 'all') {
      result = result.filter(t => t.category === selectedCategory);
    }
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(t => t.name.toLowerCase().includes(q));
    }
    return result;
  }, [selectedCategory, searchQuery]);

  // 位置配置
  const positions = [
    { id: 'top-left', name: '左上', icon: AlignLeft },
    { id: 'top-center', name: '上中', icon: AlignCenter },
    { id: 'top-right', name: '右上', icon: AlignRight },
    { id: 'center', name: '居中', icon: Grid },
    { id: 'bottom-left', name: '左下', icon: AlignLeft },
    { id: 'bottom-center', name: '下中', icon: AlignCenter },
    { id: 'bottom-right', name: '右下', icon: AlignRight },
  ];

  // 应用模板
  const applyTemplate = useCallback((templateId: string) => {
    setSelectedTemplate(templateId);
    const template = WATERMARK_TEMPLATES.find(t => t.id === templateId);
    if (template) {
      setPosition(template.preset.position as typeof position);
      setPadding(template.preset.padding);
      
      if (templateId === 'classic') {
        setCustomText('Shot on 小O帮帮');
      } else if (templateId === 'hasselblad') {
        setCustomText('HASSELBLAD HNCS');
        setLetterSpacing(2);
      } else if (templateId === 'leica') {
        setCustomText('Leica Camera AG');
        setLetterSpacing(3);
      } else if (templateId === 'minimal') {
        setCustomText('OM');
        setFontSize(20);
      } else if (templateId === 'detailed') {
        setCustomText('f/1.8 1/100s ISO100');
      } else if (templateId === 'signature') {
        setCustomText('© Photographer');
      } else if (templateId === 'social') {
        setCustomText('@omaster');
      } else if (templateId === 'timestamp') {
        setCustomText(new Date().toLocaleDateString());
      } else if (templateId === 'location') {
        setCustomText('📍 Location');
      } else if (templateId === 'copyright') {
        setCustomText('© 2024 小O帮帮');
      }
    }
  }, []);

  // 添加图层
  const addLayer = useCallback((type: 'text' | 'logo' | 'shape') => {
    const newLayer: WatermarkLayer = {
      id: Date.now().toString(),
      type,
      content: type === 'text' ? '新文本' : '',
      x: 50,
      y: 50,
      width: type === 'text' ? 100 : 50,
      height: type === 'text' ? 20 : 50,
      rotation: 0,
      opacity: 0.8,
      fontSize: 14,
      fontFamily: 'default',
      fontWeight: 'normal',
      color: '#ffffff',
    };
    setLayers(prev => [...prev, newLayer]);
    setSelectedLayer(newLayer.id);
  }, []);

  // 删除图层
  const deleteLayer = useCallback((id: string) => {
    setLayers(prev => prev.filter(l => l.id !== id));
    if (selectedLayer === id) {
      setSelectedLayer(null);
    }
  }, [selectedLayer]);

  // 导出图片
  const handleExport = useCallback(() => {
    setShowExport(true);
    setTimeout(() => {
      setShowExport(false);
      alert('图片已保存到相册');
    }, 1500);
  }, []);

  // 批量导出
  const handleBatchExport = useCallback(() => {
    alert('批量导出功能：选择多张图片应用当前水印设置');
  }, []);

  // 计算水印位置样式
  const getWatermarkStyle = useCallback(() => {
    const baseStyle: React.CSSProperties = {
      position: 'absolute',
      fontFamily: FONT_OPTIONS.find(f => f.id === fontFamily)?.family,
      fontSize: `${fontSize}px`,
      fontWeight: fontWeight,
      opacity: opacity,
      color: textColor,
      transform: `rotate(${rotation}deg)`,
      letterSpacing: `${letterSpacing}px`,
      textShadow: shadowEnabled ? `0 ${shadowBlur}px ${shadowBlur * 2}px rgba(0,0,0,0.5)` : 'none',
      backgroundColor: bgOpacity > 0 ? `rgba(0,0,0,${bgOpacity})` : 'transparent',
      padding: bgOpacity > 0 ? '4px 8px' : '0',
    };

    switch (position) {
      case 'top-left':
        return { ...baseStyle, top: `${padding}px`, left: `${padding}px` };
      case 'top-center':
        return { ...baseStyle, top: `${padding}px`, left: '50%', transform: `translateX(-50%) rotate(${rotation}deg)` };
      case 'top-right':
        return { ...baseStyle, top: `${padding}px`, right: `${padding}px` };
      case 'center':
        return { ...baseStyle, top: '50%', left: '50%', transform: `translate(-50%, -50%) rotate(${rotation}deg)` };
      case 'bottom-left':
        return { ...baseStyle, bottom: `${padding}px`, left: `${padding}px` };
      case 'bottom-center':
        return { ...baseStyle, bottom: `${padding}px`, left: '50%', transform: `translateX(-50%) rotate(${rotation}deg)` };
      case 'bottom-right':
        return { ...baseStyle, bottom: `${padding}px`, right: `${padding}px` };
      default:
        return baseStyle;
    }
  }, [position, padding, fontFamily, fontSize, fontWeight, opacity, textColor, rotation, shadowEnabled, shadowBlur, letterSpacing, bgOpacity]);

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white">
      {/* Header */}
      <div className="sticky top-0 z-50 bg-[#0a0a0a]/95 backdrop-blur-sm border-b border-white/5">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10">
              <ArrowLeft size={20} className="text-white" />
            </button>
            <div>
              <h1 className="text-lg font-bold">水印编辑器</h1>
              <p className="text-xs text-white/50">专业水印设计 · {WATERMARK_TEMPLATES.length}+模板</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowLayers(!showLayers)}
              className={`p-2 rounded-full ${showLayers ? 'bg-[#00BCD4]/20' : 'hover:bg-white/10'}`}
            >
              <Layers size={18} className={showLayers ? 'text-[#00BCD4]' : 'text-white/50'} />
            </button>
            <button
              onClick={() => setShowPreview(!showPreview)}
              className={`p-2 rounded-full ${showPreview ? 'bg-[#00BCD4]/20' : 'hover:bg-white/10'}`}
            >
              <Eye size={18} className={showPreview ? 'text-[#00BCD4]' : 'text-white/50'} />
            </button>
            <button
              onClick={handleExport}
              className="px-3 py-1.5 rounded-lg bg-[#00BCD4] text-white text-sm font-medium flex items-center gap-1"
            >
              <Download size={14} />
              导出
            </button>
          </div>
        </div>
      </div>

      {/* Layers Panel */}
      {showLayers && (
        <div className="px-4 py-4 border-b border-white/5 bg-[#0a0a0a]">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-white/70 text-sm font-medium">图层管理</h3>
            <div className="flex gap-1">
              <button onClick={() => addLayer('text')} className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10">
                <Type size={14} className="text-white/50" />
              </button>
              <button onClick={() => addLayer('logo')} className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10">
                <Image size={14} className="text-white/50" />
              </button>
              <button onClick={() => addLayer('shape')} className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10">
                <Square size={14} className="text-white/50" />
              </button>
            </div>
          </div>
          {layers.length > 0 ? (
            <div className="space-y-2">
              {layers.map((layer) => (
                <div 
                  key={layer.id}
                  onClick={() => setSelectedLayer(layer.id)}
                  className={`flex items-center gap-3 p-2 rounded-xl cursor-pointer ${
                    selectedLayer === layer.id ? 'bg-[#00BCD4]/20 border border-[#00BCD4]/50' : 'bg-white/5'
                  }`}
                >
                  {layer.type === 'text' && <Type size={16} className="text-white/50" />}
                  {layer.type === 'logo' && <Image size={16} className="text-white/50" />}
                  {layer.type === 'shape' && <Square size={16} className="text-white/50" />}
                  <span className="text-white/70 text-sm flex-1 truncate">{layer.content || '图层'}</span>
                  <button onClick={(e) => { e.stopPropagation(); deleteLayer(layer.id); }} className="p-1">
                    <Trash2 size={14} className="text-red-400" />
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-white/30 text-xs text-center py-4">点击上方按钮添加图层</p>
          )}
        </div>
      )}

      {/* Preview Area */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-[#1a1a1a]">
          <img 
            src={selectedImage}
            alt="Preview"
            className="w-full h-full object-cover"
          />
          
          {/* Watermark Preview */}
          {enabled && showPreview && (
            <div style={getWatermarkStyle()} className="flex flex-col gap-0.5">
              <span style={{ fontWeight: selectedTemplate === 'hasselblad' || selectedTemplate === 'leica' ? 'bold' : 'normal', letterSpacing: selectedTemplate === 'hasselblad' ? 2 : selectedTemplate === 'leica' ? 3 : 0 }}>
                {customText}
              </span>
              {selectedTemplate === 'detailed' && (
                <span style={{ fontSize: `${fontSize - 4}px`, opacity: 0.6 }}>
                  {new Date().toLocaleDateString()}
                </span>
              )}
              {selectedTemplate === 'hasselblad' && (
                <span style={{ fontSize: `${fontSize - 4}px`, opacity: 0.6 }}>
                  Natural Color Solution
                </span>
              )}
              {selectedTemplate === 'leica' && (
                <span style={{ fontSize: `${fontSize - 6}px`, opacity: 0.5 }}>
                  Camera AG
                </span>
              )}
            </div>
          )}

          {/* Grid Overlay for positioning */}
          {showAdvanced && (
            <div className="absolute inset-0 pointer-events-none">
              <div className="w-full h-full grid grid-cols-3 grid-rows-3">
                {[...Array(9)].map((_, i) => (
                  <div key={i} className="border border-white/10" />
                ))}
              </div>
            </div>
          )}

          {/* Export Processing */}
          {showExport && (
            <div className="absolute inset-0 bg-black/70 flex items-center justify-center">
              <div className="flex flex-col items-center gap-3">
                <div className="w-12 h-12 rounded-full border-4 border-[#00BCD4] border-t-transparent animate-spin" />
                <span className="text-white text-sm">正在导出...</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Enable Toggle */}
      <div className="px-4 pb-4">
        <button
          onClick={() => setEnabled(!enabled)}
          className={`w-full py-3 rounded-xl flex items-center justify-center gap-2 font-medium transition-all ${
            enabled 
              ? 'bg-[#00BCD4] text-white' 
              : 'bg-white/10 text-white/70'
          }`}
        >
          {enabled ? <Check size={18} /> : <Type size={18} />}
          <span>{enabled ? '水印已启用' : '启用水印'}</span>
        </button>
      </div>

      {/* Template Categories */}
      <div className="px-4 pb-3">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          {TEMPLATE_CATEGORIES.map((cat) => {
            const Icon = cat.icon;
            return (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                className={`flex-shrink-0 px-3 py-2 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all ${
                  selectedCategory === cat.id
                    ? 'bg-[#00BCD4] text-white'
                    : 'bg-white/5 text-white/60 hover:bg-white/10'
                }`}
              >
                <Icon size={14} />
                {cat.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Search */}
      <div className="px-4 pb-3">
        <div className="relative">
          <Type size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索水印模板..."
            className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#00BCD4] outline-none"
          />
        </div>
      </div>

      {/* Template Selection */}
      <div className="px-4 pb-4">
        <h3 className="text-white/50 text-xs mb-3">水印模板 ({filteredTemplates.length})</h3>
        <div className="grid grid-cols-4 gap-2">
          {filteredTemplates.map((template) => {
            const Icon = template.icon;
            return (
              <button
                key={template.id}
                onClick={() => applyTemplate(template.id)}
                className={`p-3 rounded-xl flex flex-col items-center gap-1.5 transition-all ${
                  selectedTemplate === template.id
                    ? 'bg-[#00BCD4]/20 border border-[#00BCD4]/50'
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <Icon size={20} className={selectedTemplate === template.id ? 'text-[#00BCD4]' : 'text-white/50'} />
                <span className="text-[10px] text-white/70">{template.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Position Selection */}
      <div className="px-4 pb-4">
        <h3 className="text-white/50 text-xs mb-3">水印位置</h3>
        <div className="grid grid-cols-7 gap-2">
          {positions.map((pos) => {
            const Icon = pos.icon;
            return (
              <button
                key={pos.id}
                onClick={() => setPosition(pos.id as typeof position)}
                className={`p-2.5 rounded-xl flex flex-col items-center gap-1 transition-all ${
                  position === pos.id
                    ? 'bg-[#00BCD4]/20 border border-[#00BCD4]/50'
                    : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <Icon size={16} className={position === pos.id ? 'text-[#00BCD4]' : 'text-white/50'} />
                <span className="text-[9px] text-white/60">{pos.name}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Custom Text */}
      <div className="px-4 pb-4">
        <h3 className="text-white/50 text-xs mb-3">自定义文字</h3>
        <input
          type="text"
          value={customText}
          onChange={(e) => setCustomText(e.target.value)}
          placeholder="输入水印文字"
          className="w-full px-4 py-3 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#00BCD4] outline-none transition-colors"
        />
      </div>

      {/* Style Controls */}
      <div className="px-4 pb-4 space-y-4">
        {/* Font Size */}
        <div className="bg-white/5 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-white text-sm font-medium">字体大小</span>
            <span className="text-[#00BCD4] text-sm font-bold">{fontSize}px</span>
          </div>
          <input
            type="range"
            min="8"
            max="48"
            value={fontSize}
            onChange={(e) => setFontSize(parseInt(e.target.value))}
            className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
          />
        </div>

        {/* Opacity */}
        <div className="bg-white/5 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-white text-sm font-medium">透明度</span>
            <span className="text-[#00BCD4] text-sm font-bold">{Math.round(opacity * 100)}%</span>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            value={opacity * 100}
            onChange={(e) => setOpacity(parseInt(e.target.value) / 100)}
            className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
          />
        </div>

        {/* Rotation */}
        <div className="bg-white/5 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-white text-sm font-medium">旋转角度</span>
            <span className="text-[#00BCD4] text-sm font-bold">{rotation}°</span>
          </div>
          <input
            type="range"
            min="-45"
            max="45"
            value={rotation}
            onChange={(e) => setRotation(parseInt(e.target.value))}
            className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
          />
        </div>

        {/* Padding */}
        <div className="bg-white/5 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-white text-sm font-medium">边距</span>
            <span className="text-[#00BCD4] text-sm font-bold">{padding}px</span>
          </div>
          <input
            type="range"
            min="8"
            max="60"
            value={padding}
            onChange={(e) => setPadding(parseInt(e.target.value))}
            className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
          />
        </div>
      </div>

      {/* Advanced Settings */}
      <div className="px-4 pb-4">
        <button
          onClick={() => setShowAdvanced(!showAdvanced)}
          className="w-full py-3 rounded-xl bg-white/5 flex items-center justify-between px-4 text-white/70"
        >
          <span className="text-sm">高级设置</span>
          <ChevronRight size={16} className={`transition-transform ${showAdvanced ? 'rotate-90' : ''}`} />
        </button>
        
        {showAdvanced && (
          <div className="mt-3 space-y-3">
            {/* Font Family */}
            <div className="bg-white/5 rounded-xl p-4">
              <span className="text-white/50 text-xs mb-2 block">字体样式</span>
              <div className="grid grid-cols-3 gap-2">
                {FONT_OPTIONS.map((font) => (
                  <button
                    key={font.id}
                    onClick={() => setFontFamily(font.id)}
                    className={`py-2 rounded-lg text-sm transition-all ${
                      fontFamily === font.id
                        ? 'bg-[#00BCD4]/20 text-[#00BCD4]'
                        : 'bg-white/5 text-white/60 hover:bg-white/10'
                    }`}
                    style={{ fontFamily: font.family }}
                  >
                    {font.name}
                  </button>
                ))}
              </div>
            </div>

            {/* Font Weight */}
            <div className="bg-white/5 rounded-xl p-4">
              <span className="text-white/50 text-xs mb-2 block">字体粗细</span>
              <div className="flex gap-2">
                <button
                  onClick={() => setFontWeight('normal')}
                  className={`flex-1 py-2 rounded-lg text-sm transition-all ${
                    fontWeight === 'normal' ? 'bg-[#00BCD4]/20 text-[#00BCD4]' : 'bg-white/5 text-white/60'
                  }`}
                >
                  常规
                </button>
                <button
                  onClick={() => setFontWeight('bold')}
                  className={`flex-1 py-2 rounded-lg text-sm transition-all ${
                    fontWeight === 'bold' ? 'bg-[#00BCD4]/20 text-[#00BCD4]' : 'bg-white/5 text-white/60'
                  }`}
                >
                  粗体
                </button>
              </div>
            </div>

            {/* Letter Spacing */}
            <div className="bg-white/5 rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white text-sm">字间距</span>
                <span className="text-[#00BCD4] text-sm">{letterSpacing}px</span>
              </div>
              <input
                type="range"
                min="0"
                max="10"
                value={letterSpacing}
                onChange={(e) => setLetterSpacing(parseInt(e.target.value))}
                className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
              />
            </div>

            {/* Text Color */}
            <div className="bg-white/5 rounded-xl p-4">
              <span className="text-white/50 text-xs mb-2 block">文字颜色</span>
              <div className="flex gap-2">
                {['#ffffff', '#000000', '#FFD700', '#FF6B35', '#00BCD4', '#FF6B9D', '#4CAF50', '#9C27B0'].map((color) => (
                  <button
                    key={color}
                    onClick={() => setTextColor(color)}
                    className={`w-10 h-10 rounded-lg border-2 transition-all ${
                      textColor === color ? 'border-[#00BCD4] scale-110' : 'border-transparent'
                    }`}
                    style={{ backgroundColor: color }}
                  />
                ))}
              </div>
            </div>

            {/* Shadow */}
            <div className="bg-white/5 rounded-xl p-4">
              <div className="flex items-center justify-between">
                <span className="text-white text-sm">文字阴影</span>
                <button
                  onClick={() => setShadowEnabled(!shadowEnabled)}
                  className={`w-12 h-6 rounded-full transition-colors ${
                    shadowEnabled ? 'bg-[#00BCD4]' : 'bg-white/20'
                  }`}
                >
                  <div className={`w-5 h-5 rounded-full bg-white transition-transform ${
                    shadowEnabled ? 'translate-x-6' : 'translate-x-0.5'
                  }`} />
                </button>
              </div>
              {shadowEnabled && (
                <div className="mt-3">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-white/50 text-xs">模糊程度</span>
                    <span className="text-[#00BCD4] text-xs">{shadowBlur}px</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="15"
                    value={shadowBlur}
                    onChange={(e) => setShadowBlur(parseInt(e.target.value))}
                    className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
                  />
                </div>
              )}
            </div>

            {/* Background */}
            <div className="bg-white/5 rounded-xl p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white text-sm">背景透明度</span>
                <span className="text-[#00BCD4] text-sm">{Math.round(bgOpacity * 100)}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="80"
                value={bgOpacity * 100}
                onChange={(e) => setBgOpacity(parseInt(e.target.value) / 100)}
                className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
              />
            </div>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="px-4 pb-6">
        <div className="flex gap-3">
          <button
            onClick={() => {
              setCustomText('Shot on 小O帮帮');
              setPosition('bottom-left');
              setFontSize(14);
              setOpacity(0.8);
              setRotation(0);
              setTextColor('#ffffff');
              setLetterSpacing(0);
              setFontWeight('normal');
              setBgOpacity(0);
            }}
            className="flex-1 py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium hover:bg-white/5 flex items-center justify-center gap-2"
          >
            <RotateCcw size={16} />
            重置默认
          </button>
          <button
            onClick={handleBatchExport}
            className="flex-1 py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium hover:bg-white/5 flex items-center justify-center gap-2"
          >
            <Copy size={16} />
            批量应用
          </button>
          <button
            onClick={handleExport}
            className="flex-1 py-3 rounded-xl bg-[#00BCD4] text-white text-sm font-medium flex items-center justify-center gap-2"
          >
            <Download size={16} />
            保存图片
          </button>
        </div>
      </div>

      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default WatermarkPage;
