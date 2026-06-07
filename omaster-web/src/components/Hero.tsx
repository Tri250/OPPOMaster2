import { motion } from 'framer-motion';
import { Camera, Sparkles, Download, Shield } from 'lucide-react';

export function Hero() {
  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden">
      {/* 背景渐变 */}
      <div className="absolute inset-0 bg-gradient-to-br from-zinc-900 via-zinc-800 to-zinc-900" />
      
      {/* 装饰性光晕 */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-orange-500/20 rounded-full blur-3xl" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-orange-600/10 rounded-full blur-3xl" />
      
      <div className="relative z-10 max-w-6xl mx-auto px-6 text-center">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
        >
          {/* 哈苏认证徽章 */}
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-orange-500/10 border border-orange-500/30 rounded-full mb-8">
            <Shield className="w-4 h-4 text-orange-500" />
            <span className="text-orange-400 text-sm font-medium">哈苏 HNCS 官方认证</span>
          </div>
          
          {/* 主标题 */}
          <h1 className="text-5xl md:text-7xl font-bold text-white mb-6 leading-tight">
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-orange-400 to-orange-600">
              OMaster
            </span>
            <br />
            专业摄影参数预设
          </h1>
          
          {/* 副标题 */}
          <p className="text-xl md:text-2xl text-zinc-400 mb-8 max-w-3xl mx-auto">
            为 <span className="text-white font-semibold">OPPO Find 系列</span> 打造的专业级摄影工具
            <br />
            哈苏色彩科学 · 智能场景识别 · 一键参数优化
          </p>
          
          {/* CTA 按钮 */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="px-8 py-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg shadow-orange-500/25 hover:shadow-orange-500/40 transition-shadow"
            >
              <Download className="w-5 h-5 inline-block mr-2" />
              下载 App
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="px-8 py-4 bg-zinc-800 text-white font-semibold rounded-xl border border-zinc-700 hover:border-zinc-600 transition-colors"
            >
              了解更多
            </motion.button>
          </div>
          
          {/* 统计数据 */}
          <div className="grid grid-cols-3 gap-8 max-w-2xl mx-auto">
            {[
              { icon: Camera, value: '500+', label: '专业预设' },
              { icon: Sparkles, value: '35+', label: '场景识别' },
              { icon: Download, value: '100万+', label: '用户下载' }
            ].map((stat, index) => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.3 + index * 0.1 }}
                className="text-center"
              >
                <stat.icon className="w-6 h-6 text-orange-500 mx-auto mb-2" />
                <div className="text-2xl font-bold text-white">{stat.value}</div>
                <div className="text-zinc-500 text-sm">{stat.label}</div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
      
      {/* 底部渐变 */}
      <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-zinc-900 to-transparent" />
    </section>
  );
}
