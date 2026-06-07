import { motion } from 'framer-motion';
import { Star, Download, Shield } from 'lucide-react';
import { Preset } from '../data/presets';

interface PresetCardProps {
  preset: Preset;
  index: number;
}

export function PresetCard({ preset, index }: PresetCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      whileInView={{ opacity: 1, scale: 1 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.1 }}
      whileHover={{ y: -8 }}
      className="group relative bg-zinc-800/50 border border-zinc-700/50 rounded-2xl overflow-hidden hover:border-orange-500/30 transition-all duration-300"
    >
      {/* 封面图 */}
      <div className="relative h-48 overflow-hidden">
        <img
          src={preset.coverUrl}
          alt={preset.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-zinc-900 via-transparent to-transparent" />
        
        {/* HNCS认证徽章 */}
        {preset.isHncsCertified && (
          <div className="absolute top-3 right-3 flex items-center gap-1 px-2 py-1 bg-orange-500/90 rounded-md">
            <Shield className="w-3 h-3 text-white" />
            <span className="text-white text-xs font-medium">HNCS</span>
          </div>
        )}
        
        {/* 评分 */}
        <div className="absolute top-3 left-3 flex items-center gap-1 px-2 py-1 bg-black/50 rounded-md">
          <Star className="w-3 h-3 text-yellow-400 fill-yellow-400" />
          <span className="text-white text-xs font-medium">{preset.rating}</span>
        </div>
      </div>
      
      {/* 内容 */}
      <div className="p-4">
        <h3 className="text-lg font-bold text-white mb-1">{preset.name}</h3>
        <p className="text-zinc-500 text-xs mb-2">{preset.author} · {preset.deviceModel}</p>
        <p className="text-zinc-400 text-sm mb-3 line-clamp-2">{preset.description}</p>
        
        {/* 标签 */}
        <div className="flex flex-wrap gap-1 mb-3">
          {preset.tags.slice(0, 3).map((tag) => (
            <span
              key={tag}
              className="px-2 py-0.5 bg-zinc-700/50 text-zinc-300 text-xs rounded"
            >
              {tag}
            </span>
          ))}
        </div>
        
        {/* 参数预览 */}
        <div className="flex items-center justify-between text-xs text-zinc-500">
          <span>ISO {preset.cameraParams.iso}</span>
          <span>{preset.cameraParams.shutter}</span>
          <span>{preset.cameraParams.aperture}</span>
        </div>
        
        {/* 下载量 */}
        <div className="flex items-center gap-1 mt-3 text-zinc-500 text-xs">
          <Download className="w-3 h-3" />
          <span>{preset.downloadCount.toLocaleString()} 次下载</span>
        </div>
      </div>
    </motion.div>
  );
}
