import React, { useState } from 'react';
import { ArrowLeft, RefreshCw } from 'lucide-react';

const HSLAdjustmentPage: React.FC = () => {
  const [selectedColor, setSelectedColor] = useState('red');

  const colors = [
    { id: 'red', name: '红色', color: '#FF6B6B' },
    { id: 'orange', name: '橙色', color: '#FFA94D' },
    { id: 'yellow', name: '黄色', color: '#FFE066' },
    { id: 'green', name: '绿色', color: '#69DB7C' },
    { id: 'cyan', name: '青色', color: '#4BC9C4' },
    { id: 'blue', name: '蓝色', color: '#4DABF7' },
    { id: 'purple', name: '紫色', color: '#DA77F2' },
    { id: 'pink', name: '粉色', color: '#F783AC' },
  ];

  const presets = [
    { id: 1, name: '鲜艳红' },
    { id: 2, name: '金色时刻' },
    { id: 3, name: '清新绿' },
    { id: 4, name: '梦幻蓝' },
    { id: 5, name: '浓郁紫' },
  ];

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">HSL 调节</h1>
          <p className="text-xs text-white/50">8色独立调节</p>
        </div>
        <button className="text-[#FF6B35]">
          <RefreshCw size={24} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {/* Hint Card */}
        <div className="bg-white/5 rounded-2xl p-4 mb-4">
          <p className="text-xs text-white/60">
            分别调整每个颜色的色相、饱和度和明度，创造独特的色彩风格
          </p>
        </div>

        {/* Color Channel Selection */}
        <h3 className="text-sm font-semibold mb-3 text-white/90">颜色通道</h3>
        <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
          {colors.map((color) => (
            <button
              key={color.id}
              onClick={() => setSelectedColor(color.id)}
              className={`flex-shrink-0 flex flex-col items-center gap-2 px-4 py-3 rounded-xl transition-all ${
                selectedColor === color.id ? 'bg-[#FF6B35]/20 border border-[#FF6B35]' : 'bg-white/5'
              }`}
            >
              <div
                className="w-10 h-10 rounded-full"
                style={{ backgroundColor: color.color }}
              />
              <span className="text-xs">{color.name}</span>
            </button>
          ))}
        </div>

        {/* Sliders */}
        <div className="space-y-6 mb-6">
          {/* Hue */}
          <div>
            <div className="flex justify-between mb-2">
              <span className="text-sm">色相</span>
              <span className="text-sm text-[#FF6B35]">0°</span>
            </div>
            <input type="range" className="w-full" />
          </div>

          {/* Saturation */}
          <div>
            <div className="flex justify-between mb-2">
              <span className="text-sm">饱和度</span>
              <span className="text-sm text-[#FF6B35]">+0</span>
            </div>
            <input type="range" className="w-full" />
          </div>

          {/* Lightness */}
          <div>
            <div className="flex justify-between mb-2">
              <span className="text-sm">明度</span>
              <span className="text-sm text-[#FF6B35]">+0</span>
            </div>
            <input type="range" className="w-full" />
          </div>
        </div>

        {/* Presets */}
        <h3 className="text-sm font-semibold mb-3 text-white/90">快速预设</h3>
        <div className="grid grid-cols-2 gap-3">
          {presets.map((preset) => (
            <button
              key={preset.id}
              className="bg-gradient-to-br from-white/10 to-white/5 rounded-xl p-4 text-left hover:from-white/15 transition-all"
            >
              <span className="text-sm">{preset.name}</span>
            </button>
          ))}
        </div>

        {/* Action Buttons */}
        <div className="mt-8 flex gap-3">
          <button className="flex-1 py-4 rounded-xl border border-white/20 text-sm font-semibold">
            重置全部
          </button>
          <button className="flex-1 py-4 rounded-xl bg-[#FF6B35] text-sm font-semibold">
            应用
          </button>
        </div>
      </div>
    </div>
  );
};

export default HSLAdjustmentPage;
