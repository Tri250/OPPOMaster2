import React from 'react';
import { HasselbladParams, SoftLightMode, formatHasselbladParamValue } from '../store/sceneProfile';

/**
 * Layer 3: 大师呈现层 - 哈苏大师参数可视化组件
 *
 * 设计规范：
 * - 参数名称 + 数值 + 可视化滑杆
 * - 哈苏橙轨道 + 白色滑块
 * - 参数范围：-30 ~ +30
 * - 柔光模式：无/柔/梦幻 三选一
 */

interface HasselbladParamsDisplayProps {
  params: HasselbladParams;
  onParamChange?: (param: keyof HasselbladParams, value: number | SoftLightMode) => void;
  editable?: boolean;
}

interface ParamConfig {
  key: keyof HasselbladParams;
  name: string;
  min: number;
  max: number;
  defaultValue: number;
}

const PARAMS_CONFIG: ParamConfig[] = [
  { key: 'tone', name: '影调', min: -30, max: 30, defaultValue: 0 },
  { key: 'saturation', name: '饱和度', min: -30, max: 30, defaultValue: 0 },
  { key: 'contrast', name: '对比度', min: -30, max: 30, defaultValue: 0 },
  { key: 'colorTemp', name: '色温', min: -30, max: 30, defaultValue: 0 },
  { key: 'sharpness', name: '锐度', min: -30, max: 30, defaultValue: 0 },
  { key: 'vignette', name: '暗角', min: -30, max: 30, defaultValue: 0 },
  { key: 'cyanMagenta', name: '青品调', min: -30, max: 30, defaultValue: 0 },
];

const SoftLightOptions: { value: SoftLightMode; label: string }[] = [
  { value: SoftLightMode.NONE, label: '无' },
  { value: SoftLightMode.SOFT, label: '柔' },
  { value: SoftLightMode.DREAMY, label: '梦幻' },
];

export const HasselbladParamsDisplay: React.FC<HasselbladParamsDisplayProps> = ({
  params,
  onParamChange,
  editable = false,
}) => {
  const handleParamChange = (key: keyof HasselbladParams, value: number) => {
    if (onParamChange && editable) {
      onParamChange(key, value);
    }
  };

  const handleSoftLightChange = (value: SoftLightMode) => {
    if (onParamChange && editable) {
      onParamChange('softLight', value);
    }
  };

  return (
    <div className="space-y-3">
      {/* 标题 */}
      <div className="flex items-center gap-2 mb-4">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="text-[#FF6B35]">
          <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="2" />
          <path d="M12 2V5M12 19V22M2 12H5M19 12H22" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </svg>
        <span className="text-white/60 text-xs font-medium">哈苏大师参数</span>
      </div>

      {/* 参数列表 */}
      <div className="space-y-3.5">
        {PARAMS_CONFIG.map((config) => {
          const value = (params[config.key] as number) ?? config.defaultValue;
          const percentage = ((value - config.min) / (config.max - config.min)) * 100;

          return (
            <div key={config.key} className="flex items-center gap-3">
              {/* 参数名称 */}
              <div className="w-12 text-white/70 text-xs shrink-0">{config.name}</div>

              {/* 数值 */}
              <div className={`w-8 text-right text-xs font-medium shrink-0 ${
                value !== 0 ? 'text-[#FF6B35]' : 'text-white/40'
              }`}>
                {formatHasselbladParamValue(value)}
              </div>

              {/* 可视化滑杆 */}
              <div className="flex-1 relative h-5 flex items-center">
                {/* 轨道背景 */}
                <div className="absolute inset-y-0 left-0 right-0 flex items-center">
                  <div className="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                    {/* 填充部分 */}
                    <div
                      className="h-full bg-gradient-to-r from-[#FF6B35]/60 to-[#FF6B35] rounded-full"
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>

                {/* 滑块 */}
                <div
                  className="absolute w-3 h-3 bg-white rounded-full shadow-md border border-white/30"
                  style={{ left: `calc(${percentage}% - 6px)` }}
                />

                {/* 点击区域（仅编辑模式） */}
                {editable && (
                  <input
                    type="range"
                    min={config.min}
                    max={config.max}
                    value={value}
                    onChange={(e) => handleParamChange(config.key, parseInt(e.target.value))}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                  />
                )}
              </div>
            </div>
          );
        })}

        {/* 柔光模式 */}
        <div className="flex items-center gap-3 pt-1">
          <div className="w-12 text-white/70 text-xs shrink-0">柔光</div>
          <div className="w-8 shrink-0" />
          <div className="flex-1 flex gap-1">
            {SoftLightOptions.map((option) => (
              <button
                key={option.value}
                onClick={() => handleSoftLightChange(option.value)}
                disabled={!editable}
                className={`flex-1 py-1.5 px-2 rounded-md text-[10px] font-medium transition-all
                  ${params.softLight === option.value
                    ? 'bg-[#FF6B35] text-white'
                    : 'bg-white/5 text-white/50 hover:bg-white/10'
                  }
                  ${!editable ? 'cursor-default' : 'cursor-pointer'}
                `}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default HasselbladParamsDisplay;
