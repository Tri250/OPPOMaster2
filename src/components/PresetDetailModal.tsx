import React from 'react';
import { X, Camera, Aperture, Gauge, Timer, Sparkles, Crown, Sun, Droplets } from 'lucide-react';
import { RemotePreset, getFormattedParams, getShootingTips } from '../services/remotePresetService';
import { CloudPreset as CloudPresetType } from '../services/presetCloudService';

// CloudPreset 转 RemotePreset 格式
function convertToRemotePreset(preset: CloudPresetType): RemotePreset & { brand?: string; brandName?: string } {
  return {
    name: preset.name,
    coverPath: preset.coverPath,
    galleryImages: [],
    author: preset.author,
    sections: [
      {
        title: '基本参数',
        items: [
          { label: '饱和度', value: String(preset.cameraParams.saturation) },
          { label: '对比度', value: String(preset.cameraParams.contrast) },
          { label: '亮度', value: String(preset.cameraParams.brightness) },
          { label: '暖度', value: String(preset.cameraParams.warmth) },
          { label: '锐度', value: String(preset.cameraParams.sharpness) },
          { label: '高光', value: String(preset.cameraParams.highlights) },
          { label: '阴影', value: String(preset.cameraParams.shadows) },
          { label: '清晰度', value: String(preset.cameraParams.clarity) },
        ],
      },
    ],
    tags: preset.tags,
    brand: preset.brand,
    brandName: preset.brand,
    isNew: preset.isNew,
    description: {
      title: '拍摄建议',
      content: '该预设来自本地云同步相册',
    },
  };
}

// 检查是否是 CloudPreset 类型（有 cameraParams 属性）
function isCloudPreset(preset: unknown): preset is CloudPresetType {
  return typeof preset === 'object' && preset !== null && 'cameraParams' in preset;
}

interface PresetDetailModalProps {
  preset: RemotePreset & { brand?: string; brandName?: string };
  onClose: () => void;
}

const PresetDetailModal: React.FC<PresetDetailModalProps> = ({ preset, onClose }) => {
  // 如果是 CloudPreset 类型，转换为 RemotePreset 格式
  const remotePreset = isCloudPreset(preset) 
    ? convertToRemotePreset(preset)
    : preset as RemotePreset & { brand?: string; brandName?: string };
  
  const params = getFormattedParams(remotePreset);
  const tips = getShootingTips(remotePreset);

  // 分类参数
  const basicParams = params.filter(p => ['曝光', '亮度', '对比度', '高光', '阴影'].includes(p.label));
  const colorParams = params.filter(p => ['光感', '饱和度', '色温', '锐度'].includes(p.label));
  const effectParams = params.filter(p => ['滤镜', '柔光', '色调曲线', '暗角', '颗粒', '颗粒大小'].includes(p.label));
  const proParams = params.filter(p => ['ISO', '快门', 'EV', '白平衡', '对焦', '测光'].includes(p.label));

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-end justify-center">
      <div className="bg-[#1A1A1A] rounded-t-3xl w-full max-w-md max-h-[90vh] overflow-hidden animate-slide-up">
        {/* Header Image */}
        <div className="relative h-56">
          <img 
            src={preset.coverPath} 
            alt={preset.name}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-[#1A1A1A] via-transparent to-transparent" />
          
          {/* Close Button */}
          <button 
            onClick={onClose}
            className="absolute top-4 right-4 w-10 h-10 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center"
          >
            <X size={20} className="text-white" />
          </button>

          {/* Title Overlay */}
          <div className="absolute bottom-4 left-4 right-4">
            <div className="flex items-center gap-2 mb-2">
              {preset.isNew && (
                <span className="px-2 py-0.5 rounded-full bg-[#FF6B35] text-xs font-bold text-white flex items-center gap-1">
                  <Sparkles size={10} />
                  NEW
                </span>
              )}
              {preset.brandName && (
                <span className="px-2 py-0.5 rounded-full bg-blue-500/80 text-xs font-bold text-white flex items-center gap-1">
                  <Crown size={10} />
                  {preset.brandName}
                </span>
              )}
            </div>
            <h2 className="text-2xl font-bold text-white">{preset.name}</h2>
            <p className="text-white/70 text-sm">{preset.author}</p>
          </div>
        </div>

        {/* Content */}
        <div className="p-4 overflow-y-auto max-h-[calc(90vh-14rem)]">
          {/* Tags */}
          {preset.tags && preset.tags.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-4">
              {preset.tags.map((tag, idx) => (
                <span 
                  key={idx}
                  className="px-3 py-1.5 rounded-full bg-white/5 text-sm text-white/70"
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}

          {/* Basic Parameters */}
          {basicParams.length > 0 && (
            <div className="bg-white/5 rounded-2xl p-4 mb-4">
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <Droplets size={16} className="text-[#FF6B35]" />
                基本调节
              </h3>
              <div className="grid grid-cols-3 gap-2">
                {basicParams.map((param, idx) => (
                  <div key={idx} className="bg-white/5 rounded-xl p-3 text-center">
                    <p className="text-xs text-white/40 mb-1">{param.label}</p>
                    <p className={`text-sm font-bold ${param.value.includes('+') ? 'text-green-400' : param.value.includes('-') ? 'text-red-400' : 'text-white'}`}>
                      {param.value}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Color Parameters */}
          {colorParams.length > 0 && (
            <div className="bg-white/5 rounded-2xl p-4 mb-4">
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <Sun size={16} className="text-[#FF6B35]" />
                色彩调节
              </h3>
              <div className="grid grid-cols-3 gap-2">
                {colorParams.map((param, idx) => (
                  <div key={idx} className="bg-white/5 rounded-xl p-3 text-center">
                    <p className="text-xs text-white/40 mb-1">{param.label}</p>
                    <p className={`text-sm font-bold ${param.value.includes('+') ? 'text-green-400' : param.value.includes('-') ? 'text-red-400' : 'text-white'}`}>
                      {param.value}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Effect Parameters */}
          {effectParams.length > 0 && (
            <div className="bg-white/5 rounded-2xl p-4 mb-4">
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <Sparkles size={16} className="text-[#FF6B35]" />
                效果调节
              </h3>
              <div className="space-y-2">
                {effectParams.map((param, idx) => (
                  <div key={idx} className="flex items-center justify-between bg-white/5 rounded-xl p-3">
                    <span className="text-sm text-white/60">{param.label}</span>
                    <span className="text-sm font-bold text-[#FF6B35]">{param.value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Pro Parameters */}
          {proParams.length > 0 && (
            <div className="bg-white/5 rounded-2xl p-4 mb-4">
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <Camera size={16} className="text-[#FF6B35]" />
                专业参数
              </h3>
              <div className="grid grid-cols-2 gap-2">
                {proParams.map((param, idx) => (
                  <div key={idx} className="bg-white/5 rounded-xl p-3 flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/20 flex items-center justify-center">
                      {param.label.includes('ISO') && <Gauge size={16} className="text-[#FF6B35]" />}
                      {param.label.includes('快门') && <Timer size={16} className="text-[#FF6B35]" />}
                      {param.label.includes('白平衡') && <Sun size={16} className="text-[#FF6B35]" />}
                      {param.label.includes('EV') && <Aperture size={16} className="text-[#FF6B35]" />}
                    </div>
                    <div>
                      <p className="text-xs text-white/40">{param.label}</p>
                      <p className="text-sm font-bold text-white">{param.value}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Shooting Tips */}
          {tips.environment || tips.scenes || tips.tips ? (
            <div className="bg-gradient-to-r from-blue-500/10 to-purple-500/10 border border-blue-500/20 rounded-2xl p-4 mb-4">
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <Camera size={16} className="text-blue-400" />
                拍摄建议
              </h3>
              <div className="space-y-2">
                {tips.environment && (
                  <div className="flex items-start gap-2">
                    <span className="text-xs text-blue-400 font-medium min-w-[60px]">环境建议</span>
                    <span className="text-xs text-white/70">{tips.environment}</span>
                  </div>
                )}
                {tips.scenes && (
                  <div className="flex items-start gap-2">
                    <span className="text-xs text-blue-400 font-medium min-w-[60px]">场景推荐</span>
                    <span className="text-xs text-white/70">{tips.scenes}</span>
                  </div>
                )}
                {tips.tips && (
                  <div className="flex items-start gap-2">
                    <span className="text-xs text-blue-400 font-medium min-w-[60px]">拍摄要点</span>
                    <span className="text-xs text-white/70">{tips.tips}</span>
                  </div>
                )}
              </div>
            </div>
          ) : preset.description?.content ? (
            <div className="bg-gradient-to-r from-blue-500/10 to-purple-500/10 border border-blue-500/20 rounded-2xl p-4 mb-4">
              <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
                <Camera size={16} className="text-blue-400" />
                {preset.description.title || '拍摄建议'}
              </h3>
              <p className="text-xs text-white/70 whitespace-pre-line leading-relaxed">
                {preset.description.content}
              </p>
            </div>
          ) : null}

          {/* Action Buttons */}
          <div className="flex gap-3">
            <button 
              onClick={onClose}
              className="flex-1 py-4 rounded-xl border border-white/20 text-sm font-semibold"
            >
              关闭
            </button>
            <button className="flex-1 py-4 rounded-xl bg-[#FF6B35] text-sm font-semibold flex items-center justify-center gap-2">
              <Sparkles size={18} />
              应用预设
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PresetDetailModal;
