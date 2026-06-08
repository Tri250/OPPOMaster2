import React from 'react';
import { useAppStore } from '../../store/appStore';
import { Preset } from '../../services/cloudSyncService';
import { ArrowLeft, Heart, Download, Share2, Star, Eye, Cloud, Settings, Palette, Sun, Contrast, Sparkles, Camera, Circle, Thermometer } from 'lucide-react';

interface PresetDetailPageProps {
  preset: Preset;
}

const PresetDetailPage: React.FC<PresetDetailPageProps> = ({ preset }) => {
  const { goBack } = useAppStore();
  const [isFavorite, setIsFavorite] = React.useState(false);

  // 格式化下载量
  const formatDownloadCount = (count: number) => {
    if (count >= 10000) {
      return `${(count / 10000).toFixed(1)}万`;
    }
    return count.toString();
  };

  // 格式化参数值（带正负号）
  const formatValue = (value: number) => {
    return value > 0 ? `+${value}` : `${value}`;
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
        <h1 className="text-lg font-bold text-white flex-1">预设详情</h1>
        <button
          onClick={() => setIsFavorite(!isFavorite)}
          className="p-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <Heart size={20} className={isFavorite ? 'text-red-500 fill-red-500' : 'text-white'} />
        </button>
        <button className="p-2 rounded-full hover:bg-white/10 transition-colors">
          <Share2 size={20} className="text-white" />
        </button>
      </div>

      {/* Cover Image */}
      <div className="relative aspect-[4/3] overflow-hidden">
        <img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#0a0a0a] via-transparent to-transparent" />
        
        {/* Badges */}
        <div className="absolute top-4 left-4 flex gap-2">
          {preset.isHncs && (
            <div className="px-2 py-1 bg-[#FF6B35]/80 backdrop-blur-sm rounded-lg text-xs font-bold text-white">
              HNCS
            </div>
          )}
          {preset.isNew && (
            <div className="px-2 py-1 bg-[#4CAF50]/80 backdrop-blur-sm rounded-lg text-xs font-bold text-white flex items-center gap-1">
              <Cloud size={12} />
              云端
            </div>
          )}
          {preset.mode === 'pro' && (
            <div className="px-2 py-1 bg-[#FFC107]/80 backdrop-blur-sm rounded-lg text-xs font-bold text-white">
              Pro
            </div>
          )}
        </div>

        {/* Brand Tags */}
        <div className="absolute bottom-4 left-4 flex gap-2">
          {preset.brand.split(', ').map((b, i) => (
            <div key={i} className="px-2 py-1 bg-white/20 backdrop-blur-sm rounded-lg text-xs text-white/70">
              {b}
            </div>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        {/* Title & Author */}
        <div className="py-4">
          <h2 className="text-xl font-bold text-white mb-2">{preset.name}</h2>
          <p className="text-white/50 text-sm">{preset.author}</p>
        </div>

        {/* Stats */}
        <div className="flex items-center gap-4 py-3 border-b border-white/5">
          <div className="flex items-center gap-1">
            <Star size={16} className="text-[#FF6B35]" fill="#FF6B35" />
            <span className="text-white text-sm font-medium">{preset.rating || 4.5}</span>
          </div>
          <div className="flex items-center gap-1">
            <Download size={16} className="text-white/50" />
            <span className="text-white/50 text-sm">{formatDownloadCount(preset.downloadCount || 0)}</span>
          </div>
          <div className="flex items-center gap-1">
            <Eye size={16} className="text-white/50" />
            <span className="text-white/50 text-sm">预览</span>
          </div>
        </div>

        {/* Description */}
        <div className="py-4">
          <h3 className="text-white text-sm font-medium mb-2">简介</h3>
          <p className="text-white/70 text-sm leading-relaxed">{preset.description || '暂无描述'}</p>
        </div>

        {/* Tags */}
        <div className="py-3">
          <h3 className="text-white text-sm font-medium mb-2">标签</h3>
          <div className="flex gap-2">
            {preset.tags.map((tag, i) => (
              <div key={i} className="px-3 py-1 bg-white/10 rounded-full text-xs text-white/70">
                {tag}
              </div>
            ))}
          </div>
        </div>

        {/* Pro Mode Parameters */}
        {preset.mode === 'pro' && (
          <div className="py-4">
            <h3 className="text-white text-sm font-medium mb-3 flex items-center gap-2">
              <Settings size={16} className="text-[#FFC107]" />
              专业参数
            </h3>
            <div className="space-y-2">
              {preset.iso && (
                <div className="p-3 rounded-xl bg-white/5 flex items-center justify-between">
                  <span className="text-white/70 text-sm">ISO 感光度</span>
                  <span className="text-white text-sm font-medium">{preset.iso}</span>
                </div>
              )}
              {preset.shutterSpeed && (
                <div className="p-3 rounded-xl bg-white/5 flex items-center justify-between">
                  <span className="text-white/70 text-sm">快门速度</span>
                  <span className="text-white text-sm font-medium">{preset.shutterSpeed}</span>
                </div>
              )}
              {preset.exposureCompensation && (
                <div className="p-3 rounded-xl bg-white/5 flex items-center justify-between">
                  <span className="text-white/70 text-sm">曝光补偿</span>
                  <span className="text-white text-sm font-medium">{preset.exposureCompensation}</span>
                </div>
              )}
              {preset.colorTemperature && (
                <div className="p-3 rounded-xl bg-white/5 flex items-center justify-between">
                  <span className="text-white/70 text-sm">色温</span>
                  <span className="text-white text-sm font-medium">{preset.colorTemperature}K</span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Color Grading Parameters */}
        <div className="py-4">
          <h3 className="text-white text-sm font-medium mb-3 flex items-center gap-2">
            <Palette size={16} className="text-[#FF6B35]" />
            调色参数
          </h3>
          <div className="space-y-3">
            {/* Filter */}
            {preset.filter && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Circle size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">滤镜</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{preset.filter}</span>
                </div>
              </div>
            )}

            {/* Soft Light */}
            {preset.softLight !== undefined && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Sun size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">柔光</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{preset.softLight}</span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-gradient-to-r from-gray-500 to-[#FF6B35]"
                    style={{ width: `${preset.softLight}%` }}
                  />
                </div>
              </div>
            )}

            {/* Tone */}
            {preset.tone !== undefined && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Contrast size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">影调</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{formatValue(preset.tone)}</span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-gradient-to-r from-gray-500 to-[#FF6B35]"
                    style={{ width: `${50 + preset.tone}%` }}
                  />
                </div>
              </div>
            )}

            {/* Saturation */}
            {preset.saturation !== undefined && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Palette size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">饱和度</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{formatValue(preset.saturation)}</span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-gradient-to-r from-gray-500 via-[#FF6B35] to-[#FF6B35]"
                    style={{ width: `${50 + preset.saturation}%` }}
                  />
                </div>
              </div>
            )}

            {/* Warm Cool */}
            {preset.warmCool !== undefined && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Thermometer size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">冷暖色调</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{formatValue(preset.warmCool)}</span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className={`h-full ${preset.warmCool >= 0 ? 'bg-gradient-to-r from-blue-400 to-yellow-500' : 'bg-gradient-to-r from-yellow-500 to-blue-400'}`}
                    style={{ width: `${50 + Math.abs(preset.warmCool)}%` }}
                  />
                </div>
              </div>
            )}

            {/* Cyan Magenta */}
            {preset.cyanMagenta !== undefined && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Palette size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">青品色调</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{formatValue(preset.cyanMagenta)}</span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className={`h-full ${preset.cyanMagenta >= 0 ? 'bg-gradient-to-r from-cyan-400 to-pink-500' : 'bg-gradient-to-r from-pink-500 to-cyan-400'}`}
                    style={{ width: `${50 + Math.abs(preset.cyanMagenta)}%` }}
                  />
                </div>
              </div>
            )}

            {/* Sharpness */}
            {preset.sharpness !== undefined && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Sparkles size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">锐度</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{preset.sharpness}</span>
                </div>
                <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-gradient-to-r from-gray-500 to-[#FF6B35]"
                    style={{ width: `${preset.sharpness}%` }}
                  />
                </div>
              </div>
            )}

            {/* Vignette */}
            {preset.vignette && (
              <div className="p-3 rounded-xl bg-white/5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Circle size={16} className="text-[#FF6B35]" />
                    <span className="text-white text-sm">暗角</span>
                  </div>
                  <span className="text-[#FF6B35] text-sm font-medium">{preset.vignette}</span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Apply Button */}
        <div className="py-4">
          <button className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98">
            <Camera size={18} />
            <span>应用预设</span>
          </button>
        </div>

        {/* Tips */}
        <div className="p-4 rounded-xl bg-white/5">
          <p className="text-white/50 text-xs">
            提示：此预设来自 {preset.brand} CDN 云端同步。应用预设后可在相机设置中进一步调整参数。
          </p>
        </div>
      </div>
    </div>
  );
};

export default PresetDetailPage;