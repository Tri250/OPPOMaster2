import { motion } from 'framer-motion';
import { LucideIcon } from 'lucide-react';

interface FeatureCardProps {
  icon: LucideIcon;
  title: string;
  description: string;
  features: string[];
  index: number;
}

export function FeatureCard({ icon: Icon, title, description, features, index }: FeatureCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.1 }}
      whileHover={{ y: -5 }}
      className="group relative bg-zinc-800/50 border border-zinc-700/50 rounded-2xl p-6 hover:border-orange-500/30 transition-all duration-300"
    >
      {/* 图标 */}
      <div className="w-12 h-12 bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
        <Icon className="w-6 h-6 text-white" />
      </div>
      
      {/* 标题 */}
      <h3 className="text-xl font-bold text-white mb-2">{title}</h3>
      
      {/* 描述 */}
      <p className="text-zinc-400 text-sm mb-4 leading-relaxed">{description}</p>
      
      {/* 功能列表 */}
      <div className="flex flex-wrap gap-2">
        {features.map((feature) => (
          <span
            key={feature}
            className="px-2 py-1 bg-zinc-700/50 text-zinc-300 text-xs rounded-md"
          >
            {feature}
          </span>
        ))}
      </div>
      
      {/* 悬停光效 */}
      <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-orange-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
    </motion.div>
  );
}
