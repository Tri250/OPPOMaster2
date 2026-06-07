import { motion } from 'framer-motion';
import { Camera, Sparkles, Palette, Layers, Wand2, Share2 } from 'lucide-react';

const features = [
  {
    icon: Camera,
    title: 'AI场景识别',
    description: '智能识别35+拍摄场景，自动推荐最佳参数配置',
    features: ['实时识别', '参数推荐', '场景优化']
  },
  {
    icon: Palette,
    title: '哈苏色彩科学',
    description: '官方HNCS认证，还原专业级哈苏色彩表现',
    features: ['HNCS 3.0', '自然色彩', '大师风格']
  },
  {
    icon: Layers,
    title: '预设管理',
    description: '500+专业预设，支持导入导出与云端同步',
    features: ['分类筛选', '收藏管理', '一键应用']
  },
  {
    icon: Wand2,
    title: '参数精细调节',
    description: '专业级参数控制，实时预览调节效果',
    features: ['ISO/快门', '白平衡', '曝光补偿']
  },
  {
    icon: Share2,
    title: '水印编辑器',
    description: '12+水印模板，支持品牌、功能、开源多种风格',
    features: ['品牌水印', '版权保护', '自定义样式']
  },
  {
    icon: Sparkles,
    title: '智能优化',
    description: 'AI一键优化，根据图片特征智能调整参数',
    features: ['自动优化', '风格迁移', '智能蒙版']
  }
];

export function Features() {
  return (
    <section className="py-24 bg-zinc-900">
      <div className="max-w-6xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
            核心功能
          </h2>
          <p className="text-zinc-400 text-lg max-w-2xl mx-auto">
            专业级摄影工具，为创作者打造极致拍摄体验
          </p>
        </motion.div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              whileHover={{ y: -5 }}
              className="group relative bg-zinc-800/50 border border-zinc-700/50 rounded-2xl p-6 hover:border-orange-500/30 transition-all duration-300"
            >
              {/* 图标 */}
              <div className="w-12 h-12 bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                <feature.icon className="w-6 h-6 text-white" />
              </div>
              
              {/* 标题 */}
              <h3 className="text-xl font-bold text-white mb-2">{feature.title}</h3>
              
              {/* 描述 */}
              <p className="text-zinc-400 text-sm mb-4 leading-relaxed">{feature.description}</p>
              
              {/* 功能列表 */}
              <div className="flex flex-wrap gap-2">
                {feature.features.map((f) => (
                  <span
                    key={f}
                    className="px-2 py-1 bg-zinc-700/50 text-zinc-300 text-xs rounded-md"
                  >
                    {f}
                  </span>
                ))}
              </div>
              
              {/* 悬停光效 */}
              <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-orange-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
