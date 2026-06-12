import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Aperture, Timer, Thermometer } from 'lucide-react';

const ParamAdjustPage: React.FC = () => {
  const { cameraParams, setCameraParam, goBack } = useAppStore();

  const params = [
    { 
      key: 'iso', 
      label: 'ISO 感光度', 
      icon: Aperture,
      min: 50, 
      max: 12800, 
      step: 50,
      marks: [50, 100, 200, 400, 800, 1600, 3200, 6400, 12800]
    },
    { 
      key: 'shutter', 
      label: '快门速度', 
      icon: Timer,
      min: 1, 
      max: 1000, 
      step: 1,
      format: (v: number) => v >= 1000 ? `${v/1000}s` : `1/${v}s`
    },
    { 
      key: 'aperture', 
      label: '光圈', 
      icon: Aperture,
      min: 1.4, 
      max: 22, 
      step: 0.1,
      format: (v: number) => `f/${v.toFixed(1)}`
    },
    { 
      key: 'wb', 
      label: '白平衡', 
      icon: Thermometer,
      min: 2000, 
      max: 10000, 
      step: 100,
      format: (v: number) => `${v}K`
    },
  ];

  const quickPresets = [
    { name: '人像', iso: 200, shutter: 125, aperture: 2.8, wb: 5500 },
    { name: '风景', iso: 100, shutter: 60, aperture: 8, wb: 5600 },
    { name: '夜景', iso: 3200, shutter: 30, aperture: 2.8, wb: 4000 },
    { name: '运动', iso: 800, shutter: 500, aperture: 4, wb: 5500 },
  ];

  const applyPreset = (preset: typeof quickPresets[0]) => {
    setCameraParam('iso', preset.iso);
    setCameraParam('shutter', preset.shutter);
    setCameraParam('aperture', preset.aperture);
    setCameraParam('wb', preset.wb);
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
        <h1 className="text-lg font-bold text-white">参数精细调节</h1>
      </div>

      {/* Quick Presets */}
      <div className="px-4 py-4">
        <p className="text-white/50 text-xs mb-3">快捷档位</p>
        <div className="flex gap-2">
          {quickPresets.map((preset) => (
            <button
              key={preset.name}
              onClick={() => applyPreset(preset)}
              className="flex-1 py-2 rounded-xl bg-white/5 text-white text-sm font-medium transition-all hover:bg-white/10 active:scale-95"
            >
              {preset.name}
            </button>
          ))}
        </div>
      </div>

      {/* Param Controls */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="space-y-6">
          {params.map((param) => {
            const Icon = param.icon;
            const value = cameraParams[param.key as keyof typeof cameraParams];
            return (
              <div key={param.key} className="bg-white/5 rounded-2xl p-4">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-[#E91E63]/20 flex items-center justify-center">
                    <Icon size={20} className="text-[#E91E63]" />
                  </div>
                  <div className="flex-1">
                    <span className="text-white text-sm font-medium">{param.label}</span>
                    <span className="text-[#E91E63] text-lg font-bold ml-2">
                      {param.format ? param.format(value) : value}
                    </span>
                  </div>
                </div>

                {/* Slider */}
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  step={param.step}
                  value={value}
                  onChange={(e) => setCameraParam(param.key, parseFloat(e.target.value))}
                  className="w-full h-3 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#E91E63]"
                />

                {/* Marks */}
                {param.marks && (
                  <div className="flex justify-between mt-2">
                    {param.marks.map((mark) => (
                      <button
                        key={mark}
                        onClick={() => setCameraParam(param.key, mark)}
                        className="text-white/30 text-[10px] hover:text-white/60 transition-colors"
                      >
                        {mark}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Exposure Meter */}
        <div className="mt-6 p-4 bg-white/5 rounded-2xl">
          <p className="text-white/50 text-xs mb-3">曝光指示器</p>
          <div className="relative h-8 bg-gradient-to-r from-blue-900 via-green-900 to-red-900 rounded-full overflow-hidden">
            <div 
              className="absolute top-0 bottom-0 w-1 bg-white shadow-lg transition-all duration-300"
              style={{ 
                left: `${Math.min(100, Math.max(0, 50 + (cameraParams.iso - 100) / 100))}%`,
                transform: 'translateX(-50%)'
              }}
            />
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-white/50 text-xs font-mono">0</span>
            </div>
          </div>
          <div className="flex justify-between mt-1">
            <span className="text-white/30 text-xs">-3</span>
            <span className="text-white/30 text-xs">+3</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ParamAdjustPage;
