import React from 'react';
import { useAppStore } from '../../store/appStore';
import { Preset } from '../../services/cloudSyncService';
import { ArrowLeft, Heart, Download, Share2, Star, Eye, Cloud, Settings, Palette, Sun, Contrast, Sparkles, Camera } from 'lucide-react';

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
          <div className="px-2 py-1 bg-[#4CAF50]/80 backdrop-blur-sm rounded-lg text-xs font-bold text-white flex items-center gap-1">
            <Cloud size={12} />
            云端
          </div>
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

        {/* Parameters */}
        <div className="py-4">
          <h3 className="text-white text-sm font-medium mb-3 flex items-center gap-2">
            <Settings size={16} className="text-[#FF6B35]" />
            参数配置
          </h3>
          <div className="space-y-3">
            {/* Saturation */}
            <div className="p-3 rounded-xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Palette size={16} className="text-[#FF6B35]" />
                  <span className="text-white text-sm">饱和度</span>
                </div>
                <span className="text-[#FF6B35] text-sm font-medium">{preset.saturation > 0 ? '+' : ''}{preset.saturation}</span>
              </div>
              <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-gray-500 to-[#FF6B35]"
                  style={{ width: `${50 + preset.saturation}%` }}
                />
              </div>
            </div>

            {/* Contrast */}
            <div className="p-3 rounded-xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Contrast size={16} className="text-[#FF6B35]" />
                  <span className="text-white text-sm">对比度</span>
                </div>
                <span className="text-[#FF6B35] text-sm font-medium">{preset.contrast > 0 ? '+' : ''}{preset.contrast}</span>
              </div>
              <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-gray-500 to-[#FF6B35]"
                  style={{ width: `${50 + preset.contrast}%` }}
                />
              </div>
            </div>

            {/* Warmth */}
            <div className="p-3 rounded-xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Sun size={16} className="text-[#FF6B35]" />
                  <span className="text-white text-sm">色温</span>
                </div>
                <span className="text-[#FF6B35] text-sm font-medium">{preset.warmth > 0 ? '+' : ''}{preset.warmth}</span>
              </div>
              <div className="h-2 bg-white/10 rounded-full overflow-hidden">
                <div 
                  className={`h-full ${preset.warmth >= 0 ? 'bg-gradient-to-r from-blue-500 to-yellow-500' : 'bg-gradient-to-r from-yellow-500 to-blue-500'}`}
                  style={{ width: `${50 + Math.abs(preset.warmth)}%` }}
                />
              </div>
            </div>

            {/* Sharpness */}
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