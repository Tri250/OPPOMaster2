import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  Palette,
  Sliders,
  X,
  CheckCircle,
  Download,
  Layers,
  Circle,
  Square,
  Move,
} from 'lucide-react';

/**
 * LUT 精细调节 + 暗角与畸变控制页面
 */
const LUTAndVignettePage: React.FC = () => {
  const { navigateToSubPage } = useAppStore();

  // LUT 状态
  const [lutIntensity, setLutIntensity] = useState(75);
  const [selectedLut, setSelectedLut] = useState('film-01');

  // 暗角状态
  const [vignetteIntensity, setVignetteIntensity] = useState(30);
  const [vignetteShape, setVignetteShape] = useState<'circle' | 'ellipse' | 'square'>('circle');
  const [vignetteCenterX, setVignetteCenterX] = useState(50);
  const [vignetteCenterY, setVignetteCenterY] = useState(50);

  // 畸变状态
  const [distortion, setDistortion] = useState(0);

  // LUT 列表
  const luts = [
    { id: 'film-01', name: '电影色调', color: '#8B4513' },
    { id: 'film-02', name: '胶片风格', color: '#D2691E' },
    { id: 'film-03', name: '日系清新', color: '#87CEEB' },
    { id: 'film-04', name: '欧美复古', color: '#CD853F' },
    { id: 'film-05', name: '黑白经典', color: '#696969' },
  ];

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
        <button
          onClick={() => navigateToSubPage(null)}
          className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center"
        >
          <X size={18} className="text-white" />
        </button>
        <h1 className="text-white font-semibold">LUT & 暗角</h1>
        <button className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
          <Download size={18} className="text-white" />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
        {/* LUT 强度调节 */}
        <div className="rounded-2xl p-4 bg-white/5 border border-white/10">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Palette size={18} className="text-[#FF6B35]" />
              <span className="text-white font-medium">LUT 强度</span>
            </div>
            <span className="text-[#FF6B35] font-bold">{lutIntensity}%</span>
          </div>

          {/* 强度滑块 */}
          <input
            type="range"
            min="0"
            max="100"
            value={lutIntensity}
            onChange={(e) => setLutIntensity(Number(e.target.value))}
            className="w-full h-2 rounded-full appearance-none bg-white/10 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#FF6B35]"
          />

          {/* 快捷按钮 */}
          <div className="flex gap-2 mt-3">
            {[0, 25, 50, 75, 100].map((val) => (
              <button
                key={val}
                onClick={() => setLutIntensity(val)}
                className={`flex-1 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  lutIntensity === val
                    ? 'bg-[#FF6B35] text-white'
                    : 'bg-white/10 text-white/50'
                }`}
              >
                {val}%
              </button>
            ))}
          </div>
        </div>

        {/* LUT 选择 */}
        <div className="rounded-2xl p-4 bg-white/5 border border-white/10">
          <div className="flex items-center gap-2 mb-3">
            <Layers size={18} className="text-[#FF6B35]" />
            <span className="text-white font-medium">LUT 选择</span>
          </div>
          <div className="space-y-2">
            {luts.map((lut) => (
              <button
                key={lut.id}
                onClick={() => setSelectedLut(lut.id)}
                className={`w-full rounded-xl p-3 flex items-center justify-between transition-all ${
                  selectedLut === lut.id
                    ? 'bg-[#FF6B35]/20 border border-[#FF6B35]/40'
                    : 'bg-white/5 border border-white/10'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-8 h-8 rounded-lg"
                    style={{ backgroundColor: lut.color }}
                  />
                  <span className="text-white text-sm">{lut.name}</span>
                </div>
                {selectedLut === lut.id && (
                  <CheckCircle size={16} className="text-[#FF6B35]" />
                )}
              </button>
            ))}
          </div>
        </div>

        {/* 暗角控制 */}
        <div className="rounded-2xl p-4 bg-white/5 border border-white/10">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Circle size={18} className="text-[#FF6B35]" />
              <span className="text-white font-medium">暗角</span>
            </div>
            <span className="text-[#FF6B35] font-bold">{vignetteIntensity}</span>
          </div>

          {/* 暗角强度滑块 */}
          <input
            type="range"
            min="0"
            max="100"
            value={vignetteIntensity}
            onChange={(e) => setVignetteIntensity(Number(e.target.value))}
            className="w-full h-2 rounded-full appearance-none bg-white/10 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#FF6B35]"
          />

          {/* 暗角形状选择 */}
          <div className="mt-3">
            <p className="text-white/50 text-xs mb-2">形状</p>
            <div className="flex gap-2">
              {[
                { id: 'circle', name: '圆形', icon: Circle },
                { id: 'ellipse', name: '椭圆', icon: Circle },
                { id: 'square', name: '方形', icon: Square },
              ].map((shape) => (
                <button
                  key={shape.id}
                  onClick={() => setVignetteShape(shape.id as any)}
                  className={`flex-1 py-2 rounded-lg flex flex-col items-center gap-1 transition-colors ${
                    vignetteShape === shape.id
                      ? 'bg-[#FF6B35] text-white'
                      : 'bg-white/10 text-white/50'
                  }`}
                >
                  <shape.icon size={16} />
                  <span className="text-xs">{shape.name}</span>
                </button>
              ))}
            </div>
          </div>

          {/* 暗角中心点 */}
          <div className="mt-3">
            <div className="flex items-center gap-2 mb-2">
              <Move size={14} className="text-white/50" />
              <p className="text-white/50 text-xs">中心点位置</p>
            </div>
            <div className="flex gap-2">
              <div className="flex-1">
                <p className="text-white/30 text-xs mb-1">X: {vignetteCenterX}%</p>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={vignetteCenterX}
                  onChange={(e) => setVignetteCenterX(Number(e.target.value))}
                  className="w-full h-1.5 rounded-full appearance-none bg-white/10 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white/50"
                />
              </div>
              <div className="flex-1">
                <p className="text-white/30 text-xs mb-1">Y: {vignetteCenterY}%</p>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={vignetteCenterY}
                  onChange={(e) => setVignetteCenterY(Number(e.target.value))}
                  className="w-full h-1.5 rounded-full appearance-none bg-white/10 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-white/50"
                />
              </div>
            </div>
          </div>
        </div>

        {/* 畸变校正 */}
        <div className="rounded-2xl p-4 bg-white/5 border border-white/10">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Sliders size={18} className="text-[#FF6B35]" />
              <span className="text-white font-medium">畸变校正</span>
            </div>
            <span className="text-[#FF6B35] font-bold">
              {distortion >= 0 ? `+${distortion}` : distortion}
            </span>
          </div>

          <input
            type="range"
            min="-100"
            max="100"
            value={distortion}
            onChange={(e) => setDistortion(Number(e.target.value))}
            className="w-full h-2 rounded-full appearance-none bg-white/10 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#FF6B35]"
          />

          <div className="flex justify-between mt-2 text-white/30 text-xs">
            <span>桶形</span>
            <span>正常</span>
            <span>枕形</span>
          </div>
        </div>
      </div>

      {/* 底部操作栏 */}
      <div className="px-4 py-3 border-t border-white/10 bg-[#0a0a0a]">
        <button className="w-full py-3 rounded-xl bg-[#FF6B35] text-white text-sm font-medium flex items-center justify-center gap-2">
          <CheckCircle size={18} />
          应用调节
        </button>
      </div>
    </div>
  );
};

export default LUTAndVignettePage;
