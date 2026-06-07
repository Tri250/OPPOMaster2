import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Type, Image, AlignLeft, AlignCenter, AlignRight, Check } from 'lucide-react';

const WatermarkPage: React.FC = () => {
  const { watermarkSettings, setWatermarkSetting, goBack } = useAppStore();

  const templates = [
    { id: 'default', name: '标准', preview: 'Shot on OMaster' },
    { id: 'minimal', name: '极简', preview: 'OM' },
    { id: 'detailed', name: '详细', preview: 'Shot on OMaster | f/1.8 1/100s ISO100' },
    { id: 'brand', name: '品牌', preview: 'HASSELBLAD' },
  ];

  const positions = [
    { id: 'top-left', name: '左上', icon: AlignLeft },
    { id: 'top-right', name: '右上', icon: AlignRight },
    { id: 'bottom-left', name: '左下', icon: AlignLeft },
    { id: 'bottom-right', name: '右下', icon: AlignRight },
    { id: 'center', name: '居中', icon: AlignCenter },
  ];

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
      </div>

      {/* Preview */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-cyan-900/50 to-blue-900/50">
          <img 
            src="https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&h=400&fit=crop"
            alt="Preview"
            className="w-full h-full object-cover"
          />
          {watermarkSettings.enabled && (
            <div 
              className={`absolute text-white/80 text-sm font-medium px-3 py-1.5 rounded bg-black/40 backdrop-blur-sm ${
                watermarkSettings.position.includes('bottom') ? 'bottom-4' : 'top-4'
              } ${
                watermarkSettings.position.includes('right') ? 'right-4' : 
                watermarkSettings.position.includes('left') ? 'left-4' : 'left-1/2 -translate-x-1/2'
              }`}
            >
              {watermarkSettings.customText || 'Shot on OMaster'}
            </div>
          )}
        </div>
      </div>

      {/* Enable Toggle */}
      <div className="px-4 pb-4">
        <button
          onClick={() => setWatermarkSetting('enabled', !watermarkSettings.enabled)}
          className={`w-full py-3 rounded-xl flex items-center justify-center gap-2 font-medium transition-all ${
            watermarkSettings.enabled 
              ? 'bg-[#00BCD4] text-white' 
              : 'bg-white/10 text-white/70'
          }`}
        >
          {watermarkSettings.enabled ? <Check size={18} /> : <Type size={18} />}
          <span>{watermarkSettings.enabled ? '水印已启用' : '启用水印'}</span>
        </button>
      </div>

      {/* Settings */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        {/* Custom Text */}
        <div className="mb-6">
          <label className="text-white/50 text-xs mb-2 block">自定义文字</label>
          <input
            type="text"
            value={watermarkSettings.customText}
            onChange={(e) => setWatermarkSetting('customText', e.target.value)}
            placeholder="输入水印文字"
            className="w-full px-4 py-3 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#00BCD4] outline-none transition-colors"
          />
        </div>

        {/* Templates */}
        <div className="mb-6">
          <label className="text-white/50 text-xs mb-2 block">模板</label>
          <div className="grid grid-cols-2 gap-2">
            {templates.map((template) => (
              <button
                key={template.id}
                onClick={() => setWatermarkSetting('template', template.id)}
                className={`p-3 rounded-xl text-left transition-all ${
                  watermarkSettings.template === template.id
                    ? 'bg-[#00BCD4]/20 border border-[#00BCD4]/50'
                    : 'bg-white/5 border border-transparent hover:bg-white/10'
                }`}
              >
                <span className="text-white text-sm font-medium block">{template.name}</span>
                <span className="text-white/40 text-xs truncate block">{template.preview}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Position */}
        <div className="mb-6">
          <label className="text-white/50 text-xs mb-2 block">位置</label>
          <div className="flex gap-2">
            {positions.map((pos) => {
              const Icon = pos.icon;
              return (
                <button
                  key={pos.id}
                  onClick={() => setWatermarkSetting('position', pos.id)}
                  className={`flex-1 py-3 rounded-xl flex flex-col items-center gap-1 transition-all ${
                    watermarkSettings.position === pos.id
                      ? 'bg-[#00BCD4]/20 text-[#00BCD4]'
                      : 'bg-white/5 text-white/50 hover:bg-white/10'
                  }`}
                >
                  <Icon size={18} />
                  <span className="text-xs">{pos.name}</span>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default WatermarkPage;
