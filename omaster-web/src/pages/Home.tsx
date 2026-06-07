import { motion } from 'framer-motion';
import { Hero } from '../components/Hero';
import { Features } from '../components/Features';
import { PresetCard } from '../components/PresetCard';
import { WatermarkCard } from '../components/WatermarkCard';
import { presets } from '../data/presets';
import { watermarkTemplates } from '../data/watermarks';
import { Download, Github, Shield } from 'lucide-react';

export function Home() {
  return (
    <div className="min-h-screen bg-zinc-900">
      {/* Hero区域 */}
      <Hero />
      
      {/* 功能展示 */}
      <Features />
      
      {/* 预设展示 */}
      <section className="py-24 bg-zinc-800">
        <div className="max-w-6xl mx-auto px-6">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
              精选预设
            </h2>
            <p className="text-zinc-400 text-lg max-w-2xl mx-auto">
              专业摄影师精心调校的参数预设，一键获得大片效果
            </p>
          </motion.div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {presets.slice(0, 6).map((preset, index) => (
              <PresetCard key={preset.id} preset={preset} index={index} />
            ))}
          </div>
        </div>
      </section>
      
      {/* 水印模板展示 */}
      <section className="py-24 bg-zinc-900">
        <div className="max-w-6xl mx-auto px-6">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
              水印模板
            </h2>
            <p className="text-zinc-400 text-lg max-w-2xl mx-auto">
              品牌认证、功能水印、开源模板，满足各种创作需求
            </p>
          </motion.div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {watermarkTemplates.map((template, index) => (
              <WatermarkCard key={template.id} template={template} index={index} />
            ))}
          </div>
        </div>
      </section>
      
      {/* AI功能展示 */}
      <section className="py-24 bg-zinc-800">
        <div className="max-w-6xl mx-auto px-6">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
            >
              <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
                AI场景识别
              </h2>
              <p className="text-zinc-400 text-lg mb-6 leading-relaxed">
                基于深度学习的场景识别引擎，支持35+拍摄场景自动识别。
                智能推荐最佳参数配置，让每个人都能拍出专业级照片。
              </p>
              
              <div className="space-y-4">
                {[
                  { label: '人像场景', desc: '智能美颜、肤色优化、背景虚化' },
                  { label: '夜景场景', desc: '降噪增强、高光抑制、暗部提亮' },
                  { label: '美食场景', desc: '色彩增强、饱和度优化、暖色调' },
                  { label: '风景场景', desc: 'HDR增强、广角优化、动态范围' }
                ].map((item) => (
                  <div key={item.label} className="flex items-start gap-3">
                    <div className="w-2 h-2 bg-orange-500 rounded-full mt-2" />
                    <div>
                      <div className="text-white font-medium">{item.label}</div>
                      <div className="text-zinc-500 text-sm">{item.desc}</div>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
            
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              className="relative"
            >
              <div className="aspect-square bg-gradient-to-br from-zinc-700 to-zinc-800 rounded-2xl p-8 flex items-center justify-center">
                <div className="text-center">
                  <Shield className="w-24 h-24 text-orange-500 mx-auto mb-4" />
                  <div className="text-4xl font-bold text-white mb-2">35+</div>
                  <div className="text-zinc-400">支持场景类型</div>
                </div>
              </div>
              
              {/* 装饰元素 */}
              <div className="absolute -top-4 -right-4 w-24 h-24 bg-orange-500/20 rounded-full blur-2xl" />
              <div className="absolute -bottom-4 -left-4 w-32 h-32 bg-orange-600/10 rounded-full blur-2xl" />
            </motion.div>
          </div>
        </div>
      </section>
      
      {/* CTA区域 */}
      <section className="py-24 bg-zinc-900">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
              开始你的专业摄影之旅
            </h2>
            <p className="text-zinc-400 text-lg mb-8">
              下载OMaster，体验哈苏色彩科学的魅力
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="px-8 py-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white font-semibold rounded-xl shadow-lg shadow-orange-500/25 hover:shadow-orange-500/40 transition-shadow"
              >
                <Download className="w-5 h-5 inline-block mr-2" />
                下载 Android 版
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className="px-8 py-4 bg-zinc-800 text-white font-semibold rounded-xl border border-zinc-700 hover:border-zinc-600 transition-colors"
              >
                <Github className="w-5 h-5 inline-block mr-2" />
                GitHub
              </motion.button>
            </div>
          </motion.div>
        </div>
      </section>
      
      {/* 页脚 */}
      <footer className="py-12 bg-zinc-900 border-t border-zinc-800">
        <div className="max-w-6xl mx-auto px-6">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="text-zinc-500 text-sm">
              © 2025 OMaster. 专业摄影参数预设管理应用
            </div>
            <div className="flex items-center gap-4 text-zinc-500 text-sm">
              <span>哈苏 HNCS 官方认证</span>
              <span>·</span>
              <span>OPPO Find 系列</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
