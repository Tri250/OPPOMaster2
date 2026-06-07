import React from 'react';
import { X, Camera, Aperture, Gauge, Timer, Star, Download, Heart } from 'lucide-react';
import { CloudPreset } from '../services/presetCloudService';

interface PresetDetailModalProps {
  preset: CloudPreset;
  onClose: () => void;
}

const PresetDetailModal: React.FC<PresetDetailModalProps> = ({ preset, onClose }) => {
  const { cameraParams, shotInfo } = preset;

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-end justify-center">
      <div className="bg-[#1A1A1A] rounded-t-3xl w-full max-w-md max-h-[90vh] overflow-hidden animate-slide-up">
        {/* Header Image */}
        <div className="relative h-48">
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
                <span className="px-2 py-0.5 rounded-full bg-[#FF6B35] text-xs font-bold">NEW</span>
              )}
              {preset.isHncs && (
                <span className="px-2 py-0.5 rounded-full bg-amber-500 text-xs font-bold">HNCS</span>
              )}
            </div>
            <h2 className="text-2xl font-bold text-white">{preset.name}</h2>
            <p className="text-white/70 text-sm">{preset.author} · {preset.brand}</p>
          </div>
        </div>

        {/* Content */}
        <div className="p-4 overflow-y-auto max-h-[calc(90vh-12rem)]">
          {/* Stats */}
          <div className="flex items-center gap-6 mb-6">
            <div className="flex items-center gap-2">
              <Star size={18} className="text-yellow-400 fill-yellow-400" />
              <span className="text-lg font-bold">{preset.rating}</span>
            </div>
            <div className="flex items-center gap-2">
              <Download size={18} className="text-white/60" />
              <span className="text-white/70">{(preset.downloadCount / 1000).toFixed(1)}K</span>
            </div>
            <button className="ml-auto flex items-center gap-2 px-4 py-2 rounded-xl bg-[#FF6B35]">
              <Heart size={18} className="text-white" />
              <span className="text-sm font-semibold">收藏</span>
            </button>
          </div>

          {/* Camera Parameters */}
          <div className="bg-white/5 rounded-2xl p-4 mb-4">
            <h3 className="text-sm font-semibold mb-4 flex items-center gap-2">
              <Camera size={16} className="text-[#FF6B35]" />
              影像参数
            </h3>
            <div className="grid grid-cols-2 gap-3">
              <ParamItem label="饱和度" value={cameraParams.saturation} unit="" />
              <ParamItem label="对比度" value={cameraParams.contrast} unit="" />
              <ParamItem label="亮度" value={cameraParams.brightness} unit="" />
              <ParamItem label="色温" value={cameraParams.warmth} unit="" />
              <ParamItem label="锐度" value={cameraParams.sharpness} unit="" />
              <ParamItem label="清晰度" value={cameraParams.clarity} unit="" />
              <ParamItem label="高光" value={cameraParams.highlights} unit="" />
              <ParamItem label="阴影" value={cameraParams.shadows} unit="" />
              <ParamItem label="色相" value={cameraParams.hue} unit="°" />
              <ParamItem label="自然饱和度" value={cameraParams.vibrance} unit="" />
            </div>
          </div>

          {/* Shot Info */}
          {shotInfo && (
            <div className="bg-white/5 rounded-2xl p-4 mb-4">
              <h3 className="text-sm font-semibold mb-4 flex items-center gap-2">
                <Aperture size={16} className="text-[#FF6B35]" />
                拍摄信息
              </h3>
              <div className="grid grid-cols-2 gap-3">
                {shotInfo.device && (
                  <div className="col-span-2 bg-white/5 rounded-xl p-3">
                    <p className="text-xs text-white/40 mb-1">设备</p>
                    <p className="text-sm font-medium">{shotInfo.device}</p>
                  </div>
                )}
                {shotInfo.iso && (
                  <div className="bg-white/5 rounded-xl p-3">
                    <div className="flex items-center gap-2 mb-1">
                      <Gauge size={12} className="text-white/40" />
                      <p className="text-xs text-white/40">ISO</p>
                    </div>
                    <p className="text-sm font-medium">{shotInfo.iso}</p>
                  </div>
                )}
                {shotInfo.aperture && (
                  <div className="bg-white/5 rounded-xl p-3">
                    <div className="flex items-center gap-2 mb-1">
                      <Aperture size={12} className="text-white/40" />
                      <p className="text-xs text-white/40">光圈</p>
                    </div>
                    <p className="text-sm font-medium">{shotInfo.aperture}</p>
                  </div>
                )}
                {shotInfo.shutter && (
                  <div className="bg-white/5 rounded-xl p-3">
                    <div className="flex items-center gap-2 mb-1">
                      <Timer size={12} className="text-white/40" />
                      <p className="text-xs text-white/40">快门</p>
                    </div>
                    <p className="text-sm font-medium">{shotInfo.shutter}</p>
                  </div>
                )}
                {shotInfo.focalLength && (
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-xs text-white/40 mb-1">焦距</p>
                    <p className="text-sm font-medium">{shotInfo.focalLength}</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Tags */}
          <div className="flex flex-wrap gap-2 mb-6">
            {preset.tags.map((tag, idx) => (
              <span 
                key={idx}
                className="px-3 py-1.5 rounded-full bg-white/5 text-sm text-white/70"
              >
                #{tag}
              </span>
            ))}
          </div>

          {/* Action Buttons */}
          <div className="flex gap-3">
            <button 
              onClick={onClose}
              className="flex-1 py-4 rounded-xl border border-white/20 text-sm font-semibold"
            >
              关闭
            </button>
            <button className="flex-1 py-4 rounded-xl bg-[#FF6B35] text-sm font-semibold">
              应用预设
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// 参数项组件
const ParamItem: React.FC<{ label: string; value: number; unit: string }> = ({ label, value, unit }) => (
  <div className="bg-white/5 rounded-xl p-3">
    <p className="text-xs text-white/40 mb-1">{label}</p>
    <p className={`text-sm font-bold ${value > 0 ? 'text-green-400' : value < 0 ? 'text-red-400' : 'text-white'}`}>
      {value > 0 ? '+' : ''}{value}{unit}
    </p>
  </div>
);

export default PresetDetailModal;
