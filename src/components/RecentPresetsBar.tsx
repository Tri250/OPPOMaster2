import React from 'react';
import { Clock, Trash2 } from 'lucide-react';
import { useAppStore, Preset, HASSelBLAD_ORANGE } from '../store/appStore';

/**
 * 最近使用预设横向滚动列表
 * 显示预设缩略图和名称，点击快速应用
 * 使用哈苏橙高亮当前项
 */
const RecentPresetsBar: React.FC = () => {
  const { 
    recentPresets, 
    addToRecent, 
    clearRecent, 
    applyPreset,
    selectedPreset 
  } = useAppStore();

  if (recentPresets.length === 0) {
    return null;
  }

  const handlePresetClick = (preset: Preset) => {
    addToRecent(preset);
    applyPreset(preset.id);
  };

  return (
    <div className="mb-4">
      {/* 标题栏 */}
      <div className="flex items-center justify-between px-4 mb-3">
        <div className="flex items-center gap-2">
          <Clock size={16} style={{ color: HASSelBLAD_ORANGE }} />
          <span 
            className="text-sm font-medium"
            style={{ color: '#FFFFFF' }}
          >
            最近使用
          </span>
        </div>
        <button
          onClick={clearRecent}
          className="flex items-center gap-1 px-2 py-1 rounded-md transition-colors"
          style={{ color: 'rgba(255, 255, 255, 0.5)' }}
          aria-label="清空历史"
        >
          <Trash2 size={14} />
          <span className="text-xs">清空</span>
        </button>
      </div>

      {/* 横向滚动列表 */}
      <div 
        className="flex gap-3 px-4 overflow-x-auto pb-2"
        style={{
          scrollbarWidth: 'none',
          msOverflowStyle: 'none',
        }}
      >
        {recentPresets.map((preset) => {
          const isSelected = selectedPreset?.id === preset.id;
          
          return (
            <button
              key={preset.id}
              onClick={() => handlePresetClick(preset)}
              className="flex-shrink-0 group relative"
              aria-label={`应用预设: ${preset.name}`}
            >
              {/* 缩略图容器 */}
              <div 
                className="relative w-16 h-16 rounded-xl overflow-hidden transition-all"
                style={{
                  border: isSelected 
                    ? `2px solid ${HASSelBLAD_ORANGE}` 
                    : '2px solid transparent',
                  boxShadow: isSelected 
                    ? `0 0 12px ${HASSelBLAD_ORANGE}40` 
                    : 'none',
                }}
              >
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover"
                  loading="lazy"
                  onError={(e) => {
                    const img = e.currentTarget;
                    img.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="%23333"><rect width="64" height="64"/><text x="50%" y="50%" fill="%23666" font-size="10" text-anchor="middle" dy=".3em">?</text></svg>');
                  }}
                />
                
                {/* HNCS标识 */}
                {preset.isHncs && (
                  <div 
                    className="absolute top-0.5 right-0.5 w-2 h-2 rounded-full"
                    style={{ background: HASSelBLAD_ORANGE }}
                  />
                )}
              </div>

              {/* 预设名称 */}
              <p 
                className="mt-1.5 text-xs text-center truncate w-16"
                style={{ 
                  color: isSelected ? HASSelBLAD_ORANGE : 'rgba(255, 255, 255, 0.7)',
                  fontWeight: isSelected ? 600 : 400,
                }}
              >
                {preset.name}
              </p>
            </button>
          );
        })}
      </div>
    </div>
  );
};

export default RecentPresetsBar;