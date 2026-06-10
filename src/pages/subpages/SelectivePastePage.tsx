import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ClipboardCopy,
  CheckCircle,
  X,
  ChevronRight,
  Layers,
} from 'lucide-react';

/**
 * 选择性粘贴页面
 * 支持勾选只粘贴特定参数（如饱和度+对比度）
 */
const SelectivePastePage: React.FC = () => {
  const { navigateToSubPage } = useAppStore();

  // 可粘贴的参数列表
  const pasteableParams = [
    { id: 'saturation', name: '饱和度', selected: true },
    { id: 'contrast', name: '对比度', selected: true },
    { id: 'brightness', name: '亮度', selected: false },
    { id: 'warmth', name: '冷暖', selected: false },
    { id: 'sharpness', name: '锐度', selected: false },
    { id: 'clarity', name: '清晰度', selected: false },
    { id: 'highlights', name: '高光', selected: false },
    { id: 'shadows', name: '阴影', selected: false },
    { id: 'noiseReduction', name: '降噪', selected: false },
    { id: 'skinSmooth', name: '美肤', selected: false },
    { id: 'detail', name: '细节', selected: false },
    { id: 'vignette', name: '暗角', selected: false },
  ];

  const [selectedParams, setSelectedParams] = useState(
    pasteableParams.map(p => p.selected)
  );

  // 快速选择预设
  const presets = [
    { name: '饱和度+对比度', params: ['saturation', 'contrast'] },
    { name: '色彩相关', params: ['saturation', 'contrast', 'brightness', 'warmth'] },
    { name: '细节相关', params: ['sharpness', 'clarity', 'detail', 'noiseReduction'] },
    { name: '光影相关', params: ['highlights', 'shadows', 'brightness', 'vignette'] },
    { name: '人像相关', params: ['skinSmooth', 'sharpness', 'clarity'] },
  ];

  // 剪贴板数据（模拟）
  const clipboardData: Record<string, number> = {
    saturation: 15,
    contrast: 8,
    brightness: 5,
    warmth: -3,
    sharpness: 10,
    clarity: 6,
    highlights: -5,
    shadows: 8,
  };

  const toggleParam = (index: number) => {
    const newSelected = [...selectedParams];
    newSelected[index] = !newSelected[index];
    setSelectedParams(newSelected);
  };

  const selectPreset = (preset: typeof presets[0]) => {
    const newSelected = pasteableParams.map(p =>
      preset.params.includes(p.id)
    );
    setSelectedParams(newSelected);
  };

  const selectedCount = selectedParams.filter(Boolean).length;
  const pasteableCount = pasteableParams.filter((p, i) =>
    selectedParams[i] && clipboardData[p.id] !== undefined
  ).length;

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
        <h1 className="text-white font-semibold">选择性粘贴</h1>
        <button className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
          <CheckCircle size={18} className="text-white" />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
        {/* 剪贴板状态卡片 */}
        <div className="rounded-2xl p-4 bg-gradient-to-br from-[#FF6B35]/20 to-[#FF6B35]/5 border border-[#FF6B35]/30">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/30 flex items-center justify-center">
              <ClipboardCopy size={20} className="text-[#FF6B35]" />
            </div>
            <div>
              <p className="text-white font-medium">剪贴板有数据</p>
              <p className="text-white/50 text-xs">来源: 胶片风格预设</p>
            </div>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <span className="px-2 py-1 rounded-full bg-white/10 text-white/70 text-xs">
              共 {Object.keys(clipboardData).length} 个参数
            </span>
          </div>
        </div>

        {/* 快速选择预设 */}
        <div>
          <p className="text-white/50 text-xs mb-2">快速选择</p>
          <div className="flex flex-wrap gap-2">
            {presets.map((preset, i) => (
              <button
                key={i}
                onClick={() => selectPreset(preset)}
                className="px-3 py-1.5 rounded-full bg-white/10 text-white/70 text-xs hover:bg-white/20 transition-colors"
              >
                {preset.name}
              </button>
            ))}
          </div>
        </div>

        {/* 参数选择列表 */}
        <div>
          <p className="text-white/50 text-xs mb-2">选择要粘贴的参数</p>
          <div className="space-y-2">
            {pasteableParams.map((param, index) => {
              const hasValue = clipboardData[param.id] !== undefined;
              const value = clipboardData[param.id];

              return (
                <button
                  key={param.id}
                  onClick={() => toggleParam(index)}
                  className={`w-full rounded-xl p-3 flex items-center justify-between transition-all ${
                    selectedParams[index]
                      ? 'bg-[#FF6B35]/20 border border-[#FF6B35]/40'
                      : 'bg-white/5 border border-white/10'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-colors ${
                      selectedParams[index]
                        ? 'bg-[#FF6B35] border-[#FF6B35]'
                        : 'border-white/30'
                    }`}>
                      {selectedParams[index] && (
                        <CheckCircle size={12} className="text-white" />
                      )}
                    </div>
                    <span className="text-white text-sm">{param.name}</span>
                  </div>
                  {hasValue ? (
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                      selectedParams[index]
                        ? 'bg-[#FF6B35] text-white'
                        : 'bg-white/10 text-white/50'
                    }`}>
                      {value >= 0 ? `+${value}` : value}
                    </span>
                  ) : (
                    <span className="text-white/30 text-xs">无数据</span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* 底部操作栏 */}
      <div className="px-4 py-3 border-t border-white/10 bg-[#0a0a0a]">
        <div className="flex gap-2">
          <button className="flex-1 py-2.5 rounded-xl bg-white/10 text-white/70 text-sm font-medium flex items-center justify-center gap-2">
            <ClipboardCopy size={16} />
            复制
          </button>
          <button className="flex-1 py-2.5 rounded-xl bg-[#FF6B35] text-white text-sm font-medium flex items-center justify-center gap-2">
            <Layers size={16} />
            粘贴({pasteableCount})
          </button>
          <button className="flex-1 py-2.5 rounded-xl bg-white/20 text-white text-sm font-medium flex items-center justify-center gap-2">
            <CheckCircle size={16} />
            全部
          </button>
        </div>
      </div>
    </div>
  );
};

export default SelectivePastePage;
