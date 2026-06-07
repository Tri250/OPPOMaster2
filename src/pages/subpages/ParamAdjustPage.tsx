import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Aperture, Timer, Sun, Thermometer, Sliders, Sparkles, Eye, Zap } from 'lucide-react';

interface ParamConfig {
  key: string;
  label: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
  min: number;
  max: number;
  step: number;
  format?: (v: number) => string;
  marks?: number[];
  unit?: string;
  description: string;
}

const ParamAdjustPage: React.FC = () => {
  const { cameraParams, setCameraParam, goBack, aiParams, setAiParam } = useAppStore();
  const [activeTab, setActiveTab] = useState<'camera' | 'color' | 'effects'>('camera');
  const [compareMode, setCompareMode] = useState(false);

  // 相机参数
  const cameraParamsConfig: ParamConfig[] = [
    { 
      key: 'iso', label: 'ISO 感光度', icon: Aperture,
      min: 50, max: 12800, step: 50,
      marks: [50, 100, 200, 400, 800, 1600, 3200, 6400, 12800],
      unit: '',
      description: '感光度越高，画面越亮但噪点越多'
    },
    { 
      key: 'shutter', label: '快门速度', icon: Timer,
      min: 1, max: 4000, step: 1,
      format: (v: number) => v >= 1000 ? `${(v/1000).toFixed(1)}s` : `1/${v}s`,
      description: '快门越快，运动越清晰'
    },
    { 
      key: 'aperture', label: '光圈', icon: Aperture,
      min: 1.4, max: 22, step: 0.1,
      format: (v: number) => `f/${v.toFixed(1)}`,
      description: '光圈越大，背景虚化越明显'
    },
    { 
      key: 'wb', label: '白平衡', icon: Thermometer,
      min: 2000, max: 10000, step: 100,
      format: (v: number) => `${v}K`,
      description: '色温越低画面越暖，越高越冷'
    },
  ];

  // 调色参数
  const colorParamsConfig: ParamConfig[] = [
    { key: 'saturation', label: '饱和度', icon: Sparkles, min: -100, max: 100, step: 1, unit: '', description: '影响色彩鲜艳程度' },
    { key: 'contrast', label: '对比度', icon: Sun, min: -100, max: 100, step: 1, unit: '', description: '影响明暗反差' },
    { key: 'brightness', label: '亮度', icon: Sun, min: -100, max: 100, step: 1, unit: '', description: '影响整体明暗' },
    { key: 'warmth', label: '色温', icon: Thermometer, min: -100, max: 100, step: 1, unit: '', description: '冷暖色调调整' },
  ];

  // 效果参数
  const effectsParamsConfig: ParamConfig[] = [
    { key: 'sharpness', label: '锐度', icon: Zap, min: 0, max: 100, step: 1, unit: '', description: '影响画面清晰程度' },
    { key: 'clarity', label: '清晰度', icon: Eye, min: 0, max: 100, step: 1, unit: '', description: '中等对比度增强' },
    { key: 'highlights', label: '高光', icon: Sun, min: -100, max: 100, step: 1, unit: '', description: '亮部细节调整' },
    { key: 'shadows', label: '阴影', icon: Sun, min: -100, max: 100, step: 1, unit: '', description: '暗部细节调整' },
  ];

  // 快捷档位
  const quickPresets = [
    { name: '人像', icon: '👤', params: { iso: 200, shutter: 125, aperture: 2.8, wb: 5500 } },
    { name: '风景', icon: '🏔️', params: { iso: 100, shutter: 60, aperture: 8, wb: 5600 } },
    { name: '夜景', icon: '🌃', params: { iso: 3200, shutter: 30, aperture: 2.8, wb: 4000 } },
    { name: '美食', icon: '🍜', params: { iso: 400, shutter: 80, aperture: 4, wb: 4500 } },
    { name: '街拍', icon: '🚶', params: { iso: 400, shutter: 250, aperture: 5.6, wb: 5500 } },
  ];

  // 档位强度
  const strengthLevels = [
    { name: '原图', multiplier: 0, color: '#9E9E9E' },
    { name: '轻微', multiplier: 0.3, color: '#4CAF50' },
    { name: '中等', multiplier: 0.6, color: '#FF9800' },
    { name: '强力', multiplier: 1.0, color: '#F44336' },
  ];

  const handleApplyPreset = (preset: typeof quickPresets[0]) => {
    Object.entries(preset.params).forEach(([key, val]) => {
      setCameraParam(key, val);
    });
  };

  const tabs = [
    { key: 'camera' as const, label: '相机参数', icon: Aperture, count: cameraParamsConfig.length },
    { key: 'color' as const, label: '调色', icon: Sparkles, count: colorParamsConfig.length },
    { key: 'effects' as const, label: '效果', icon: Sliders, count: effectsParamsConfig.length },
  ];

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">参数精细调节</h1>
        <button
          onClick={() => setCompareMode(!compareMode)}
          className={`ml-auto p-2 rounded-full transition-colors ${
            compareMode ? 'bg-[#FF6B35] text-white' : 'hover:bg-white/10 text-white/70'
          }`}
        >
          <Eye size={16} />
        </button>
      </div>

      {/* Preview */}
      <div className="px-4 py-4">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-amber-900/50 to-orange-900/50">
          <img 
            src="https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&h=400&fit=crop"
            alt="Preview"
            className="w-full h-full object-cover"
            style={{
              filter: `
                saturate(${100 + aiParams.saturation}%) 
                contrast(${100 + aiParams.contrast}%) 
                brightness(${100 + aiParams.brightness}%)
              `,
              opacity: compareMode ? 0.5 : 1,
            }}
          />
          
          {/* Compare overlay (show original) */}
          {compareMode && (
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="px-3 py-1.5 rounded-full bg-black/70 text-white text-xs">
                原图对比
              </span>
            </div>
          )}

          {/* Current Settings Badge */}
          <div className="absolute top-3 left-3 right-3 flex flex-wrap gap-1.5">
            {Object.entries(cameraParams).slice(0, 3).map(([key, val]) => {
              const formatMap: Record<string, (v: number) => string> = {
                iso: (v) => `ISO ${v}`,
                shutter: (v) => v >= 1000 ? `${(v/1000).toFixed(1)}s` : `1/${v}s`,
                aperture: (v) => `f/${v.toFixed(1)}`,
                wb: (v) => `${v}K`,
              };
              return (
                <span 
                  key={key}
                  className="px-2 py-1 rounded bg-black/50 backdrop-blur-sm text-white text-[10px]"
                >
                  {formatMap[key]?.(val as number) || val}
                </span>
              );
            })}
          </div>
        </div>
      </div>

      {/* Quick Presets */}
      <div className="px-4 pb-3">
        <div className="flex items-center gap-2 overflow-x-auto scrollbar-hide">
          {quickPresets.map(preset => (
            <button
              key={preset.name}
              onClick={() => handleApplyPreset(preset)}
              className="flex-shrink-0 flex flex-col items-center gap-1 px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors"
            >
              <span className="text-xl">{preset.icon}</span>
              <span className="text-white text-xs">{preset.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Tabs */}
      <div className="px-4 pb-3">
        <div className="flex gap-1 p-1 rounded-xl bg-white/5">
          {tabs.map(tab => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1.5 text-xs font-medium transition-all ${
                  activeTab === tab.key
                    ? 'bg-[#FF6B35] text-white'
                    : 'text-white/60 hover:text-white/80'
                }`}
              >
                <Icon size={14} />
                <span>{tab.label}</span>
                <span className={`text-[10px] ${activeTab === tab.key ? 'text-white/80' : 'text-white/40'}`}>
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Params List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        {activeTab === 'camera' && cameraParamsConfig.map(param => {
          const Icon = param.icon;
          const value = cameraParams[param.key as keyof typeof cameraParams] as number;
          return (
            <div key={param.key} className="mb-3 p-3 rounded-xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Icon size={14} className="text-[#FF6B35]" />
                  <span className="text-white text-sm font-medium">{param.label}</span>
                </div>
                <span className="text-[#FF6B35] text-sm font-bold">
                  {param.format ? param.format(value) : `${value}${param.unit || ''}`}
                </span>
              </div>
              <input
                type="range"
                min={param.min}
                max={param.max}
                step={param.step}
                value={value}
                onChange={(e) => setCameraParam(param.key, parseFloat(e.target.value))}
                className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
              />
              <p className="text-white/40 text-[10px] mt-1.5">{param.description}</p>
              {param.marks && (
                <div className="flex justify-between mt-1">
                  {param.marks.map(mark => (
                    <button
                      key={mark}
                      onClick={() => setCameraParam(param.key, mark)}
                      className="text-white/30 text-[9px] hover:text-[#FF6B35] transition-colors"
                    >
                      {mark}
                    </button>
                  ))}
                </div>
              )}
            </div>
          );
        })}

        {(activeTab === 'color' || activeTab === 'effects') && (
          <div>
            {/* Strength Level */}
            <div className="mb-4 p-3 rounded-xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white text-sm font-medium">档位强度</span>
                <span className="text-white/40 text-[10px]">点击快速选择</span>
              </div>
              <div className="grid grid-cols-4 gap-1.5">
                {strengthLevels.map(level => (
                  <button
                    key={level.name}
                    className="py-2 rounded-lg text-xs font-medium transition-all hover:scale-105"
                    style={{ backgroundColor: `${level.color}30`, color: level.color }}
                  >
                    {level.name}
                  </button>
                ))}
              </div>
            </div>

            {/* Params */}
            {(activeTab === 'color' ? colorParamsConfig : effectsParamsConfig).map(param => {
              const Icon = param.icon;
              const value = aiParams[param.key as keyof typeof aiParams] as number;
              return (
                <div key={param.key} className="mb-3 p-3 rounded-xl bg-white/5">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <Icon size={14} className="text-[#FF6B35]" />
                      <span className="text-white text-sm font-medium">{param.label}</span>
                    </div>
                    <span className="text-[#FF6B35] text-sm font-bold">
                      {value > 0 ? '+' : ''}{value}{param.unit || ''}
                    </span>
                  </div>
                  <input
                    type="range"
                    min={param.min}
                    max={param.max}
                    step={param.step}
                    value={value}
                    onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                    className="w-full h-1.5 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                  />
                  <p className="text-white/40 text-[10px] mt-1.5">{param.description}</p>
                </div>
              );
            })}

            {/* Linked Params Notice */}
            <div className="mt-4 p-3 rounded-xl bg-[#FF6B35]/10 border border-[#FF6B35]/30">
              <div className="flex items-center gap-2 mb-1">
                <Zap size={12} className="text-[#FF6B35]" />
                <span className="text-white text-xs font-medium">联动调整</span>
              </div>
              <p className="text-white/50 text-[10px]">
                锐度与清晰度会自动联动调节，避免过度处理
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ParamAdjustPage;
