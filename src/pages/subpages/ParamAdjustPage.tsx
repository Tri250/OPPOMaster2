import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft,
  RotateCcw,
  Check,
  Palette,
  Sparkles,
  Sun,
  Moon,
  Waves,
  Circle,
  Grid3X3,
  Layers,
  Sliders,
  History,
  ClipboardCopy,
  Cloud,
} from 'lucide-react';

/**
 * 参数精细调节页面（增强版）
 * 整合所有参数调节功能
 */
const ParamAdjustPage: React.FC = () => {
  const { goBack } = useAppStore();

  // 参数状态
  const [params, setParams] = useState<Record<string, number>>({
    saturation: 10,
    contrast: 5,
    brightness: 0,
    warmth: 3,
    sharpness: 15,
    clarity: 10,
    detail: 8,
    highlights: -5,
    shadows: 8,
    noiseReduction: 0,
    skinSmooth: 0,
    vignette: 0,
    distortion: 0,
    lutIntensity: 100,
  });

  // 当前展开的分组
  const [expandedGroup, setExpandedGroup] = useState<string>('color');

  // 快捷预设
  const quickPresets = [
    { name: '原图', params: { saturation: 0, contrast: 0, brightness: 0, warmth: 0, sharpness: 0, clarity: 0 } },
    { name: '轻微', params: { saturation: 5, contrast: 3, brightness: 2, warmth: 0, sharpness: 5, clarity: 3 } },
    { name: '中等', params: { saturation: 10, contrast: 8, brightness: 5, warmth: 0, sharpness: 10, clarity: 8 } },
    { name: '强力', params: { saturation: 15, contrast: 12, brightness: 8, warmth: 0, sharpness: 15, clarity: 12 } },
  ];

  // 参数分组定义
  const paramGroups = [
    {
      id: 'color',
      name: '色彩',
      icon: Palette,
      color: '#FF6B35',
      params: [
        { key: 'saturation', name: '饱和度', min: -100, max: 100 },
        { key: 'contrast', name: '对比度', min: -100, max: 100 },
        { key: 'brightness', name: '亮度', min: -100, max: 100 },
        { key: 'warmth', name: '冷暖', min: -100, max: 100 },
      ],
    },
    {
      id: 'detail',
      name: '细节',
      icon: Sparkles,
      color: '#9C27B0',
      params: [
        { key: 'sharpness', name: '锐度', min: 0, max: 100 },
        { key: 'clarity', name: '清晰度', min: 0, max: 100 },
        { key: 'detail', name: '细节', min: 0, max: 100 },
      ],
    },
    {
      id: 'light',
      name: '光影',
      icon: Sun,
      color: '#2196F3',
      params: [
        { key: 'highlights', name: '高光', min: -100, max: 100 },
        { key: 'shadows', name: '阴影', min: -100, max: 100 },
      ],
    },
    {
      id: 'noise',
      name: '降噪',
      icon: Waves,
      color: '#00BCD4',
      params: [
        { key: 'noiseReduction', name: '降噪', min: 0, max: 100 },
        { key: 'skinSmooth', name: '美肤', min: 0, max: 100 },
      ],
    },
    {
      id: 'effect',
      name: '效果',
      icon: Layers,
      color: '#E91E63',
      params: [
        { key: 'vignette', name: '暗角', min: 0, max: 100 },
        { key: 'distortion', name: '畸变', min: -100, max: 100 },
        { key: 'lutIntensity', name: 'LUT强度', min: 0, max: 100 },
      ],
    },
  ];

  const updateParam = (key: string, value: number) => {
    setParams((prev) => ({ ...prev, [key]: value }));
  };

  const applyPreset = (preset: typeof quickPresets[0]) => {
    setParams((prev) => ({ ...prev, ...preset.params }));
  };

  const resetAll = () => {
    setParams({
      saturation: 0,
      contrast: 0,
      brightness: 0,
      warmth: 0,
      sharpness: 0,
      clarity: 0,
      detail: 0,
      highlights: 0,
      shadows: 0,
      noiseReduction: 0,
      skinSmooth: 0,
      vignette: 0,
      distortion: 0,
      lutIntensity: 100,
    });
  };

  // 计算非零参数数量
  const nonZeroCount = Object.values(params).filter((v) => v !== 0 && v !== 100).length;

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
        <div className="flex items-center gap-3">
          <button
            onClick={goBack}
            className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center"
          >
            <ArrowLeft size={18} className="text-white" />
          </button>
          <div>
            <h1 className="text-white font-semibold">参数精细调节</h1>
            <p className="text-white/50 text-xs">{nonZeroCount} 个参数已调节</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={resetAll}
            className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center"
          >
            <RotateCcw size={16} className="text-white" />
          </button>
          <button className="w-8 h-8 rounded-full bg-[#FF6B35] flex items-center justify-center">
            <Check size={16} className="text-white" />
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        {/* 快捷预设 */}
        <div className="px-4 py-3 border-b border-white/5">
          <p className="text-white/50 text-xs mb-2">快捷档位</p>
          <div className="flex gap-2">
            {quickPresets.map((preset) => (
              <button
                key={preset.name}
                onClick={() => applyPreset(preset)}
                className="flex-1 py-2 rounded-xl bg-white/5 text-white/70 text-sm font-medium hover:bg-white/10 transition-colors"
              >
                {preset.name}
              </button>
            ))}
          </div>
        </div>

        {/* 功能入口 */}
        <div className="px-4 py-3 border-b border-white/5">
          <div className="grid grid-cols-4 gap-2">
            <button className="flex flex-col items-center gap-1 p-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors">
              <ClipboardCopy size={18} className="text-[#FF6B35]" />
              <span className="text-white/70 text-[10px]">选择性粘贴</span>
            </button>
            <button className="flex flex-col items-center gap-1 p-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors">
              <Cloud size={18} className="text-[#00BCD4]" />
              <span className="text-white/70 text-[10px]">跨设备同步</span>
            </button>
            <button className="flex flex-col items-center gap-1 p-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors">
              <Layers size={18} className="text-[#9C27B0]" />
              <span className="text-white/70 text-[10px]">LUT调节</span>
            </button>
            <button className="flex flex-col items-center gap-1 p-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors">
              <History size={18} className="text-[#607D8B]" />
              <span className="text-white/70 text-[10px]">编辑历史</span>
            </button>
          </div>
        </div>

        {/* 参数分组列表 */}
        <div className="px-4 py-3 space-y-3">
          {paramGroups.map((group) => {
            const Icon = group.icon;
            const isExpanded = expandedGroup === group.id;

            return (
              <div
                key={group.id}
                className="rounded-2xl overflow-hidden bg-white/5 border border-white/10"
              >
                {/* 分组标题 */}
                <button
                  onClick={() => setExpandedGroup(isExpanded ? '' : group.id)}
                  className="w-full px-4 py-3 flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div
                      className="w-10 h-10 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${group.color}20` }}
                    >
                      <Icon size={20} style={{ color: group.color }} />
                    </div>
                    <span className="text-white font-medium">{group.name}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {/* 显示非零参数数量 */}
                    <span className="text-white/30 text-xs">
                      {group.params.filter((p) => params[p.key] !== 0).length} / {group.params.length}
                    </span>
                    <div
                      className={`w-6 h-6 rounded-full bg-white/10 flex items-center justify-center transition-transform ${
                        isExpanded ? 'rotate-180' : ''
                      }`}
                    >
                      <svg
                        width="12"
                        height="12"
                        viewBox="0 0 12 12"
                        className="text-white/50"
                      >
                        <path d="M3 5L6 8L9 5" stroke="currentColor" strokeWidth="1.5" fill="none" />
                      </svg>
                    </div>
                  </div>
                </button>

                {/* 参数列表 */}
                {isExpanded && (
                  <div className="px-4 pb-4 space-y-4">
                    {group.params.map((param) => {
                      const value = params[param.key];
                      const isNegative = value < 0;

                      return (
                        <div key={param.key}>
                          {/* 参数标题 */}
                          <div className="flex items-center justify-between mb-2">
                            <span className="text-white/70 text-sm">{param.name}</span>
                            <span
                              className="text-sm font-bold"
                              style={{ color: group.color }}
                            >
                              {value >= 0 ? `+${value}` : value}
                            </span>
                          </div>

                          {/* 滑块 */}
                          <div className="relative">
                            <input
                              type="range"
                              min={param.min}
                              max={param.max}
                              value={value}
                              onChange={(e) => updateParam(param.key, Number(e.target.value))}
                              className="w-full h-2 rounded-full appearance-none cursor-pointer"
                              style={{
                                background: `linear-gradient(to right, ${group.color}40 0%, ${group.color} ${
                                  ((value - param.min) / (param.max - param.min)) * 100
                                }%, white/10 ${
                                  ((value - param.min) / (param.max - param.min)) * 100
                                }%, white/10 100%)`,
                              }}
                            />
                            {/* 自定义滑块样式 */}
                            <style jsx>{`
                              input[type='range']::-webkit-slider-thumb {
                                appearance: none;
                                width: 16px;
                                height: 16px;
                                border-radius: 50%;
                                background: ${group.color};
                                cursor: pointer;
                                box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
                              }
                            `}</style>
                          </div>

                          {/* 快捷按钮 */}
                          <div className="flex gap-1 mt-2">
                            {[-50, -25, 0, 25, 50]
                              .filter((v) => v >= param.min && v <= param.max)
                              .map((v) => (
                                <button
                                  key={v}
                                  onClick={() => updateParam(param.key, v)}
                                  className={`flex-1 py-1 rounded text-[10px] font-medium transition-colors ${
                                    value === v
                                      ? 'text-white'
                                      : 'bg-white/5 text-white/50 hover:bg-white/10'
                                  }`}
                                  style={value === v ? { backgroundColor: group.color } : {}}
                                >
                                  {v >= 0 ? `+${v}` : v}
                                </button>
                              ))}
                          </div>
                        </div>
                      );
                    })}

                    {/* 分组重置按钮 */}
                    <button
                      onClick={() => {
                        const newParams = { ...params };
                        group.params.forEach((p) => {
                          newParams[p.key] = p.key === 'lutIntensity' ? 100 : 0;
                        });
                        setParams(newParams);
                      }}
                      className="w-full py-2 rounded-xl bg-white/5 text-white/50 text-xs hover:bg-white/10 transition-colors"
                    >
                      重置{group.name}参数
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* 参数摘要 */}
        <div className="px-4 py-3">
          <div className="rounded-2xl p-4 bg-gradient-to-r from-[#FF6B35]/10 to-[#FF6B35]/5 border border-[#FF6B35]/20">
            <p className="text-white/50 text-xs mb-3">当前参数摘要</p>
            <div className="grid grid-cols-4 gap-2">
              {Object.entries(params)
                .filter(([_, v]) => v !== 0 && v !== 100)
                .slice(0, 8)
                .map(([key, value]) => {
                  const group = paramGroups.find((g) => g.params.some((p) => p.key === key));
                  const param = group?.params.find((p) => p.key === key);
                  return (
                    <div key={key} className="text-center">
                      <p className="text-white/50 text-[10px]">{param?.name || key}</p>
                      <p className="text-white text-sm font-bold">
                        {value >= 0 ? `+${value}` : value}
                      </p>
                    </div>
                  );
                })}
            </div>
          </div>
        </div>
      </div>

      {/* 底部操作栏 */}
      <div className="px-4 py-3 border-t border-white/10 bg-[#0a0a0a]">
        <div className="flex gap-2">
          <button className="flex-1 py-3 rounded-xl bg-white/10 text-white/70 text-sm font-medium flex items-center justify-center gap-2">
            <ClipboardCopy size={16} />
            复制参数
          </button>
          <button className="flex-1 py-3 rounded-xl bg-[#FF6B35] text-white text-sm font-medium flex items-center justify-center gap-2">
            <Check size={16} />
            应用参数
          </button>
        </div>
      </div>
    </div>
  );
};

export default ParamAdjustPage;
