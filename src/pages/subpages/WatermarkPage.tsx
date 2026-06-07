import React, { useState, useEffect, useRef } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Type, Image as ImageIcon, Check, Camera, Aperture, Clock, MapPin, Hash, Layout, Grid3x3, Plus, Download, Share2, Heart, X, Layers } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { userImageStore, UserImage } from '../../store/userImageStore';
import { frameTemplates, frameCategories, collageLayouts, FrameTemplate } from '../../data/frameTemplates';

type TabType = 'frame' | 'watermark' | 'collage' | 'params';

const WatermarkPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [userImage, setUserImage] = useState<UserImage | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>('frame');
  const [activeCategory, setActiveCategory] = useState('all');
  const [selectedFrame, setSelectedFrame] = useState<string | null>(null);
  const [customText, setCustomText] = useState('HASSELBLAD');
  const [customDevice, setCustomDevice] = useState('OPPO Find X8 Pro');
  const [position, setPosition] = useState('bottom-left');
  const [showVignette, setShowVignette] = useState(false);
  const [showFrame, setShowFrame] = useState(true);
  
  // 拼图状态
  const [collageImages, setCollageImages] = useState<string[]>([]);
  const [activeLayout, setActiveLayout] = useState('grid_2');
  const [collageSpacing, setCollageSpacing] = useState(8);
  const [collageBg, setCollageBg] = useState('#000000');

  useEffect(() => {
    const unsubscribe = userImageStore.subscribe(image => {
      setUserImage(image);
    });
    return unsubscribe;
  }, []);

  const filteredFrames = activeCategory === 'all' 
    ? frameTemplates 
    : frameTemplates.filter(f => f.category === activeCategory);

  const selectedFrameData = selectedFrame ? frameTemplates.find(f => f.id === selectedFrame) : null;

  const handleAddToCollage = () => {
    if (userImage) {
      setCollageImages(prev => [...prev, userImage.dataUrl].slice(0, 9));
    }
  };

  const handleRemoveFromCollage = (index: number) => {
    setCollageImages(prev => prev.filter((_, i) => i !== index));
  };

  // 渲染水印预览
  const renderWatermark = () => {
    if (!userImage) return null;
    return (
      <div className="absolute inset-0 pointer-events-none">
        {/* 边框 */}
        {showFrame && (
          <div 
            className="absolute inset-0 border-8"
            style={{ borderColor: '#FFFFFF' }}
          />
        )}
        
        {/* 品牌文字（顶部/底部） */}
        {position === 'top-left' && (
          <div className="absolute top-3 left-3 px-2 py-1 rounded bg-black/50 backdrop-blur-sm text-white text-[10px] font-medium">
            {customText}
          </div>
        )}
        {position === 'top-right' && (
          <div className="absolute top-3 right-3 px-2 py-1 rounded bg-black/50 backdrop-blur-sm text-white text-[10px] font-medium">
            {customText}
          </div>
        )}
        {position === 'bottom-left' && (
          <div className="absolute bottom-3 left-3 right-12">
            <div className="px-2 py-1.5 rounded bg-black/50 backdrop-blur-sm">
              <p className="text-white text-[11px] font-bold">{customText}</p>
              <p className="text-white/80 text-[9px]">{customDevice}</p>
            </div>
          </div>
        )}
        {position === 'bottom-right' && (
          <div className="absolute bottom-3 right-3 left-12 text-right">
            <div className="inline-block px-2 py-1.5 rounded bg-black/50 backdrop-blur-sm">
              <p className="text-white text-[11px] font-bold">{customText}</p>
              <p className="text-white/80 text-[9px]">{customDevice}</p>
            </div>
          </div>
        )}
        {position === 'middle-center' && (
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 px-3 py-2 rounded bg-black/60 backdrop-blur-sm text-center">
            <p className="text-white text-sm font-bold">{customText}</p>
            <p className="text-white/80 text-[10px]">{customDevice}</p>
          </div>
        )}

        {/* 暗角 */}
        {showVignette && (
          <div className="absolute inset-0" 
               style={{ background: 'radial-gradient(circle, transparent 50%, rgba(0,0,0,0.4) 100%)' }} />
        )}
      </div>
    );
  };

  // 渲染拼图
  const renderCollage = () => {
    const layout = collageLayouts.find(l => l.id === activeLayout);
    if (!layout) return null;
    
    const slots = layout.rows * layout.cols;
    const cells = Array.from({ length: slots }, (_, i) => collageImages[i] || null);

    return (
      <div 
        className="aspect-square w-full rounded-2xl p-3"
        style={{ backgroundColor: collageBg }}
      >
        <div 
          className="w-full h-full grid gap-2"
          style={{
            gridTemplateColumns: `repeat(${layout.cols}, 1fr)`,
            gridTemplateRows: `repeat(${layout.rows}, 1fr)`,
            gap: `${collageSpacing}px`,
          }}
        >
          {cells.map((img, i) => (
            <div 
              key={i} 
              className="relative rounded-lg overflow-hidden bg-white/5 flex items-center justify-center group"
            >
              {img ? (
                <>
                  <img src={img} alt={`拼图 ${i+1}`} className="w-full h-full object-cover" />
                  <button
                    onClick={() => handleRemoveFromCollage(i)}
                    className="absolute top-1 right-1 p-1 rounded-full bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <X size={12} className="text-white" />
                  </button>
                </>
              ) : (
                <button
                  onClick={handleAddToCollage}
                  className="w-full h-full flex flex-col items-center justify-center text-white/40 hover:text-white/60 transition-colors"
                  disabled={!userImage}
                >
                  <Plus size={20} />
                  <span className="text-[9px] mt-0.5">添加</span>
                </button>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  };

  // 渲染边框
  const renderFrame = () => {
    if (!userImage || !selectedFrameData) return null;
    
    const frame = selectedFrameData;
    const borderPercent = frame.borderWidth * 100;
    
    return (
      <div 
        className="relative aspect-video rounded-2xl overflow-hidden p-3"
        style={{ backgroundColor: frame.backgroundColor }}
      >
        <div className="relative w-full h-full rounded overflow-hidden">
          <img src={userImage.dataUrl} alt="预览" className="w-full h-full object-cover" />
          
          {frame.showText && (
            <div 
              className={`absolute left-0 right-0 px-4 ${
                frame.textPosition === 'top' ? 'top-3' : 'bottom-3'
              }`}
            >
              <p 
                className="text-center text-sm font-medium"
                style={{ color: frame.borderColor === '#FFFFFF' || frame.borderColor === '#F5F0E8' ? '#333' : frame.borderColor }}
              >
                {frame.textContent}
              </p>
            </div>
          )}

          {frame.showLogo && frame.logoText && (
            <div className="absolute top-3 right-3 px-2 py-1 rounded bg-black/30 text-white/80 text-[9px]">
              {frame.logoText}
            </div>
          )}

          {/* 装饰元素 */}
          {frame.decoration === 'dots' && (
            <div className="absolute top-3 left-3 flex gap-1">
              <div className="w-1.5 h-1.5 rounded-full bg-white" />
              <div className="w-1.5 h-1.5 rounded-full bg-white" />
              <div className="w-1.5 h-1.5 rounded-full bg-white" />
            </div>
          )}
          {frame.decoration === 'lines' && (
            <>
              <div className="absolute top-0 left-1/4 w-px h-full" style={{ backgroundColor: frame.borderColor, opacity: 0.3 }} />
              <div className="absolute top-0 right-1/4 w-px h-full" style={{ backgroundColor: frame.borderColor, opacity: 0.3 }} />
            </>
          )}
          {frame.decoration === 'film-strip' && (
            <div className="absolute top-0 left-0 right-0 h-4 flex justify-around" style={{ backgroundColor: '#000' }}>
              {Array.from({ length: 12 }).map((_, i) => (
                <div key={i} className="w-2 h-2 bg-white/20 rounded-sm self-center" />
              ))}
            </div>
          )}
          {frame.decoration === 'polaroid' && (
            <div className="absolute bottom-0 left-0 right-0 h-12 bg-white flex items-center justify-center">
              <p className="text-xs text-gray-700" style={{ fontFamily: 'cursive' }}>
                {frame.textContent}
              </p>
            </div>
          )}
          {frame.decoration === 'tape' && (
            <>
              <div className="absolute -top-2 left-1/4 w-12 h-4 bg-yellow-200/70 rotate-[-3deg]" />
              <div className="absolute -top-2 right-1/4 w-12 h-4 bg-yellow-200/70 rotate-[3deg]" />
            </>
          )}
        </div>
      </div>
    );
  };

  // 渲染相机参数水印
  const renderParamsWatermark = () => {
    if (!userImage) return null;
    return (
      <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-cyan-900/50 to-blue-900/50">
        <img src={userImage.dataUrl} alt="Preview" className="w-full h-full object-cover" />
        
        {/* 详细参数水印 */}
        <div className="absolute bottom-3 left-3 right-3 flex items-end justify-between">
          <div className="px-2 py-1.5 rounded bg-black/60 backdrop-blur-sm">
            <div className="flex items-center gap-2 text-white text-[10px]">
              <span>f/1.6</span>
              <span>·</span>
              <span>1/500s</span>
              <span>·</span>
              <span>ISO 100</span>
            </div>
            <p className="text-white/80 text-[9px] mt-0.5">OPPO Find X8 Pro · Hasselblad</p>
          </div>
          <div className="px-2 py-1.5 rounded bg-black/60 backdrop-blur-sm">
            <p className="text-white text-[10px]">2026.06.07 · 18:30</p>
            <p className="text-white/80 text-[9px] mt-0.5">📍 北京·朝阳</p>
          </div>
        </div>

        {showVignette && (
          <div className="absolute inset-0" 
               style={{ background: 'radial-gradient(circle, transparent 50%, rgba(0,0,0,0.4) 100%)' }} />
        )}
      </div>
    );
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">水印编辑器</h1>
        <div className="ml-auto flex items-center gap-1.5 text-[10px] text-white/50">
          <span>{frameTemplates.length}+ 边框</span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto pb-4 scrollbar-hide">
        {/* 上传区域 */}
        <div className="px-4 pt-4">
          <ImageUploader
            onImageLoaded={(img) => setUserImage(img)}
            buttonText="上传您的照片"
            hint="上传后可添加水印/边框/拼图"
            sampleImages={[
              { url: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=200&h=200&fit=crop', label: '风景', tag: '山' },
              { url: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=200&h=200&fit=crop', label: '人像', tag: '人物' },
              { url: 'https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=200&h=200&fit=crop', label: '胶片', tag: '复古' },
              { url: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=200&h=200&fit=crop', label: '美食', tag: '食物' },
            ]}
          />
        </div>

        {/* Tabs */}
        <div className="px-4 pt-3">
          <div className="flex gap-1 p-1 rounded-xl bg-white/5">
            {[
              { key: 'frame' as const, label: '边框', icon: Layout, count: frameTemplates.length },
              { key: 'watermark' as const, label: '水印', icon: Type, count: 0 },
              { key: 'collage' as const, label: '拼图', icon: Grid3x3, count: collageLayouts.length },
              { key: 'params' as const, label: '参数水印', icon: Camera, count: 0 },
            ].map(tab => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1 text-[11px] font-medium transition-all ${
                    activeTab === tab.key ? 'bg-[#00BCD4] text-white' : 'text-white/60'
                  }`}
                >
                  <Icon size={12} />
                  <span>{tab.label}</span>
                  {tab.count > 0 && (
                    <span className={`text-[9px] ${activeTab === tab.key ? 'text-white/80' : 'text-white/40'}`}>
                      {tab.count}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'frame' && (
          <>
            {/* 边框预览 */}
            <div className="px-4 pt-4">
              {selectedFrameData && userImage ? renderFrame() : userImage ? (
                <div className="aspect-video rounded-2xl overflow-hidden bg-white/5 flex items-center justify-center">
                  <p className="text-white/40 text-xs">从下方选择一款边框</p>
                </div>
              ) : null}
            </div>

            {/* 分类 */}
            <div className="px-4 pt-4">
              <div className="flex gap-1.5 overflow-x-auto scrollbar-hide pb-1">
                {frameCategories.map(cat => (
                  <button
                    key={cat.key}
                    onClick={() => setActiveCategory(cat.key)}
                    className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                      activeCategory === cat.key ? 'bg-[#00BCD4] text-white' : 'bg-white/5 text-white/60'
                    }`}
                  >
                    <span className="mr-1">{cat.icon}</span>
                    {cat.label}
                  </button>
                ))}
              </div>
            </div>

            {/* 边框列表 */}
            <div className="px-4 pt-3">
              <p className="text-white/40 text-xs mb-2">共 {filteredFrames.length} 款边框</p>
              <div className="grid grid-cols-3 gap-2">
                {filteredFrames.map(frame => (
                  <button
                    key={frame.id}
                    onClick={() => setSelectedFrame(frame.id)}
                    className={`relative rounded-xl overflow-hidden transition-all ${
                      selectedFrame === frame.id
                        ? 'ring-2 ring-[#00BCD4]'
                        : 'ring-1 ring-white/5'
                    }`}
                  >
                    <div 
                      className="aspect-square p-1.5"
                      style={{ backgroundColor: frame.backgroundColor }}
                    >
                      <div 
                        className="w-full h-full rounded bg-cover bg-center"
                        style={{ 
                          backgroundImage: `url(${userImage?.dataUrl || frame.preview})`,
                        }}
                      />
                    </div>
                    <div className="absolute bottom-0 left-0 right-0 px-1.5 py-1 bg-gradient-to-t from-black/80 to-transparent">
                      <p className="text-white text-[10px] font-medium truncate">{frame.name}</p>
                      <p className="text-white/50 text-[9px] truncate">{frame.author}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          </>
        )}

        {activeTab === 'watermark' && (
          <>
            {/* 水印预览 */}
            <div className="px-4 pt-4">
              {userImage ? (
                <div className="relative aspect-video rounded-2xl overflow-hidden">
                  <img src={userImage.dataUrl} alt="Preview" className="w-full h-full object-cover" />
                  {renderWatermark()}
                </div>
              ) : (
                <div className="aspect-video rounded-2xl bg-white/5 flex items-center justify-center">
                  <p className="text-white/40 text-xs">请先上传图片</p>
                </div>
              )}
            </div>

            {/* 自定义文字 */}
            <div className="px-4 pt-4 space-y-3">
              <div>
                <label className="text-white/50 text-xs mb-1.5 block">品牌文字</label>
                <input
                  type="text"
                  value={customText}
                  onChange={(e) => setCustomText(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-white/5 text-white text-sm border border-white/10 focus:border-[#00BCD4] outline-none"
                />
              </div>
              <div>
                <label className="text-white/50 text-xs mb-1.5 block">设备型号</label>
                <input
                  type="text"
                  value={customDevice}
                  onChange={(e) => setCustomDevice(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-white/5 text-white text-sm border border-white/10 focus:border-[#00BCD4] outline-none"
                />
              </div>
            </div>

            {/* 9宫格位置 */}
            <div className="px-4 pt-4">
              <label className="text-white/50 text-xs mb-2 block">水印位置</label>
              <div className="grid grid-cols-3 gap-1.5 p-2 rounded-xl bg-white/5">
                {[
                  { id: 'top-left', name: '左上' },
                  { id: 'top-center', name: '上中' },
                  { id: 'top-right', name: '右上' },
                  { id: 'middle-left', name: '左中' },
                  { id: 'middle-center', name: '正中' },
                  { id: 'middle-right', name: '右中' },
                  { id: 'bottom-left', name: '左下' },
                  { id: 'bottom-center', name: '下中' },
                  { id: 'bottom-right', name: '右下' },
                ].map(pos => (
                  <button
                    key={pos.id}
                    onClick={() => setPosition(pos.id)}
                    className={`py-2.5 rounded-lg text-xs transition-all ${
                      position === pos.id ? 'bg-[#00BCD4] text-white' : 'bg-white/5 text-white/60'
                    }`}
                  >
                    {pos.name}
                  </button>
                ))}
              </div>
            </div>

            {/* 特效 */}
            <div className="px-4 pt-4 space-y-2">
              <label className="flex items-center justify-between p-3 rounded-xl bg-white/5 cursor-pointer">
                <div className="flex items-center gap-2">
                  <Aperture size={16} className="text-[#00BCD4]" />
                  <span className="text-white text-sm">暗角特效</span>
                </div>
                <input
                  type="checkbox"
                  checked={showVignette}
                  onChange={(e) => setShowVignette(e.target.checked)}
                  className="w-5 h-5 accent-[#00BCD4]"
                />
              </label>
            </div>
          </>
        )}

        {activeTab === 'collage' && (
          <>
            {/* 拼图预览 */}
            <div className="px-4 pt-4">
              {renderCollage()}
            </div>

            {/* 添加当前图片 */}
            <div className="px-4 pt-3">
              <button
                onClick={handleAddToCollage}
                disabled={!userImage || collageImages.length >= 9}
                className="w-full py-2.5 rounded-xl bg-[#00BCD4]/20 border border-[#00BCD4]/40 text-white text-sm font-medium flex items-center justify-center gap-2 disabled:opacity-40"
              >
                <Plus size={16} />
                <span>添加当前图片到拼图 ({collageImages.length}/9)</span>
              </button>
            </div>

            {/* 拼图布局选择 */}
            <div className="px-4 pt-4">
              <p className="text-white/50 text-xs mb-2">选择布局</p>
              <div className="grid grid-cols-4 gap-2">
                {collageLayouts.map(layout => (
                  <button
                    key={layout.id}
                    onClick={() => setActiveLayout(layout.id)}
                    className={`p-2 rounded-xl transition-all ${
                      activeLayout === layout.id
                        ? 'bg-[#00BCD4]/20 border border-[#00BCD4]/50'
                        : 'bg-white/5 border border-transparent'
                    }`}
                  >
                    <div className="text-center">
                      <div className="text-white text-lg mb-0.5">{layout.icon}</div>
                      <p className="text-white/60 text-[9px]">{layout.name}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* 拼图设置 */}
            <div className="px-4 pt-4 space-y-3">
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-white text-xs">间距</span>
                  <span className="text-[#00BCD4] text-xs font-bold">{collageSpacing}px</span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={20}
                  value={collageSpacing}
                  onChange={(e) => setCollageSpacing(parseInt(e.target.value))}
                  className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#00BCD4]"
                />
              </div>
              <div className="p-3 rounded-xl bg-white/5">
                <span className="text-white text-xs block mb-2">背景色</span>
                <div className="flex gap-2">
                  {['#000000', '#FFFFFF', '#F5F0E8', '#FFE5C4', '#E8F0E5', '#DDE9F0'].map(color => (
                    <button
                      key={color}
                      onClick={() => setCollageBg(color)}
                      className={`w-8 h-8 rounded-full border-2 transition-all ${
                        collageBg === color ? 'border-[#00BCD4] scale-110' : 'border-white/20'
                      }`}
                      style={{ backgroundColor: color }}
                    />
                  ))}
                </div>
              </div>
            </div>
          </>
        )}

        {activeTab === 'params' && (
          <>
            <div className="px-4 pt-4">
              {renderParamsWatermark()}
            </div>
            <div className="px-4 pt-3">
              <p className="text-white/50 text-xs mb-2">相机参数水印 - 自动从 EXIF 读取</p>
              <div className="space-y-2">
                {[
                  { icon: Aperture, label: '光圈值', value: 'f/1.6' },
                  { icon: Clock, label: '快门速度', value: '1/500s' },
                  { icon: Hash, label: 'ISO 感光度', value: 'ISO 100' },
                  { icon: Camera, label: '设备型号', value: 'Find X8 Pro · Hasselblad' },
                  { icon: MapPin, label: 'GPS 位置', value: '北京·朝阳·798艺术区' },
                  { icon: Clock, label: '拍摄时间', value: '2026.06.07 18:30' },
                ].map((item, i) => {
                  const Icon = item.icon;
                  return (
                    <div key={i} className="flex items-center justify-between p-3 rounded-xl bg-white/5">
                      <div className="flex items-center gap-2">
                        <Icon size={14} className="text-[#00BCD4]" />
                        <span className="text-white/70 text-xs">{item.label}</span>
                      </div>
                      <span className="text-white text-xs font-medium">{item.value}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          </>
        )}

        {/* Action Buttons */}
        {userImage && (
          <div className="px-4 pt-6 grid grid-cols-3 gap-2">
            <button className="py-2.5 rounded-xl bg-white/5 text-white text-xs font-medium flex items-center justify-center gap-1.5">
              <Heart size={14} />
              <span>收藏</span>
            </button>
            <button className="py-2.5 rounded-xl bg-white/5 text-white text-xs font-medium flex items-center justify-center gap-1.5">
              <Share2 size={14} />
              <span>分享</span>
            </button>
            <button className="py-2.5 rounded-xl bg-gradient-to-r from-[#00BCD4] to-[#0097A7] text-white text-xs font-medium flex items-center justify-center gap-1.5">
              <Download size={14} />
              <span>保存</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default WatermarkPage;
