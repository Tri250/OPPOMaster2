import { motion } from 'framer-motion';
import { WatermarkTemplate } from '../data/watermarks';

interface WatermarkCardProps {
  template: WatermarkTemplate;
  index: number;
}

const categoryColors = {
  brand: 'from-orange-500 to-orange-600',
  functional: 'from-blue-500 to-blue-600',
  free: 'from-green-500 to-green-600'
};

const categoryLabels = {
  brand: '品牌',
  functional: '功能',
  free: '免费'
};

export function WatermarkCard({ template, index }: WatermarkCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      whileInView={{ opacity: 1, scale: 1 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.1 }}
      whileHover={{ y: -8 }}
      className="group relative bg-zinc-800/50 border border-zinc-700/50 rounded-2xl overflow-hidden hover:border-orange-500/30 transition-all duration-300"
    >
      {/* 预览图 */}
      <div className="relative h-40 overflow-hidden">
        <img
          src={template.previewUrl}
          alt={template.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 opacity-80"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-zinc-900 via-zinc-900/50 to-transparent" />
        
        {/* 分类标签 */}
        <div className={`absolute top-3 left-3 px-2 py-1 bg-gradient-to-r ${categoryColors[template.category]} rounded-md`}>
          <span className="text-white text-xs font-medium">{categoryLabels[template.category]}</span>
        </div>
        
        {/* 来源品牌 */}
        {template.source && (
          <div className="absolute top-3 right-3 px-2 py-1 bg-black/50 rounded-md">
            <span className="text-zinc-300 text-xs">{template.source}</span>
          </div>
        )}
      </div>
      
      {/* 内容 */}
      <div className="p-4">
        <h3 className="text-lg font-bold text-white mb-1">{template.name}</h3>
        <p className="text-zinc-400 text-sm mb-3">{template.description}</p>
        
        {/* 功能列表 */}
        <div className="flex flex-wrap gap-1">
          {template.features.map((feature) => (
            <span
              key={feature}
              className="px-2 py-0.5 bg-zinc-700/50 text-zinc-300 text-xs rounded"
            >
              {feature}
            </span>
          ))}
        </div>
      </div>
    </motion.div>
  );
}
