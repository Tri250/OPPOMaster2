import React, { useState } from 'react';
import { 
  Smartphone, 
  Camera, 
  Sparkles, 
  Cloud, 
  Palette, 
  Sliders, 
  Share2, 
  Crown,
  ChevronRight,
  Heart,
  Search,
  Settings,
  Image,
  Wand2,
  ScanLine,
  Droplets,
  Grid3X3,
  Info
} from 'lucide-react';

interface FeatureCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  color: string;
  screenshot?: string;
}

const FeatureCard: React.FC<FeatureCardProps> = ({ icon, title, description, color, screenshot }) => (
  <div className="group relative bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-sm rounded-2xl p-6 border border-white/10 hover:border-white/20 transition-all duration-500 hover:scale-[1.02]">
    <div className={`w-12 h-12 rounded-xl ${color} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300`}>
      {icon}
    </div>
    <h3 className="text-white font-semibold text-lg mb-2">{title}</h3>
    <p className="text-white/60 text-sm leading-relaxed">{description}</p>
    {screenshot && (
      <div className="mt-4 rounded-lg overflow-hidden border border-white/10">
        <img src={screenshot} alt={title} className="w-full h-32 object-cover opacity-80 group-hover:opacity-100 transition-opacity" />
      </div>
    )}
  </div>
);

interface ScreenPreviewProps {
  title: string;
  description: string;
  children: React.ReactNode;
}

const ScreenPreview: React.FC<ScreenPreviewProps> = ({ title, description, children }) => (
  <div className="flex flex-col lg:flex-row gap-8 items-center">
    <div className="flex-1 space-y-4">
      <h3 className="text-2xl font-bold text-white">{title}</h3>
      <p className="text-white/60 leading-relaxed">{description}</p>
    </div>
    <div className="flex-shrink-0">
      <div className="relative w-[280px] h-[580px] bg-[#1a1a1a] rounded-[40px] shadow-2xl border-4 border-[#2a2a2a] overflow-hidden">
        {/* Phone Screen */}
        <div className="absolute inset-0 bg-[#0a0a0a] overflow-hidden">
          {children}
        </div>
        {/* Home Indicator */}
        <div className="absolute bottom-2 left-1/2 -translate-x-1/2 w-[100px] h-[4px] bg-white/30 rounded-full" />
      </div>
    </div>
  </div>
);

const AndroidAppShowcase: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'screens' | 'features'>('overview');

  const features = [
    {
      icon: <Camera className="w-6 h-6 text-white" />,
      title: "哈苏大师预设",
      description: "精选哈苏自然色彩解决方案(HNCS)预设，专业摄影师调校，一键应用大师级色彩",
      color: "bg-gradient-to-br from-orange-500 to-amber-500"
    },
    {
      icon: <ScanLine className="w-6 h-6 text-white" />,
      title: "AI 场景识别",
      description: "智能识别拍摄场景，自动推荐最佳预设参数，让每张照片都出彩",
      color: "bg-gradient-to-br from-blue-500 to-cyan-500"
    },
    {
      icon: <Wand2 className="w-6 h-6 text-white" />,
      title: "AI 智能调参",
      description: "基于深度学习算法，自动分析照片并优化参数，智能微调达到最佳效果",
      color: "bg-gradient-to-br from-purple-500 to-pink-500"
    },
    {
      icon: <Droplets className="w-6 h-6 text-white" />,
      title: "专业水印编辑",
      description: "支持哈苏大师风格水印，自定义边框、签名、EXIF信息展示",
      color: "bg-gradient-to-br from-emerald-500 to-teal-500"
    },
    {
      icon: <Cloud className="w-6 h-6 text-white" />,
      title: "云端同步",
      description: "预设数据云端备份，多设备同步，随时随地访问你的预设库",
      color: "bg-gradient-to-br from-sky-500 to-blue-500"
    },
    {
      icon: <Share2 className="w-6 h-6 text-white" />,
      title: "LUT 分享社区",
      description: "与全球摄影爱好者分享预设，发现更多创意灵感",
      color: "bg-gradient-to-br from-rose-500 to-orange-500"
    }
  ];

  return (
    <div className="min-h-screen bg-[#0a0a0a]">
      {/* Hero Section */}
      <section className="relative py-20 px-4 overflow-hidden">
        {/* Background Effects */}
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-orange-500/20 rounded-full blur-[128px]" />
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/20 rounded-full blur-[128px]" />
        </div>

        <div className="relative max-w-6xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 mb-8">
            <Smartphone className="w-4 h-4 text-orange-500" />
            <span className="text-white/70 text-sm">Android 专业影像工具</span>
          </div>

          <h1 className="text-5xl md:text-7xl font-bold text-white mb-6">
            <span className="bg-gradient-to-r from-orange-400 via-amber-400 to-orange-500 bg-clip-text text-transparent">
              OMaster
            </span>
            <br />
            <span className="text-3xl md:text-4xl font-light text-white/80">专业影像参数管理</span>
          </h1>

          <p className="text-xl text-white/60 max-w-2xl mx-auto mb-12">
            为摄影爱好者打造的专业预设管理工具，支持哈苏大师模式、AI场景识别、智能调参等强大功能
          </p>

          {/* App Stats */}
          <div className="flex flex-wrap justify-center gap-8 mb-16">
            {[
              { value: "10+", label: "万预设资源" },
              { value: "50+", label: "AI模型" },
              { value: "4.9", label: "用户评分" },
              { value: "100+", label: "万下载量" }
            ].map((stat, index) => (
              <div key={index} className="text-center">
                <div className="text-3xl font-bold text-white mb-1">{stat.value}</div>
                <div className="text-white/50 text-sm">{stat.label}</div>
              </div>
            ))}
          </div>

          {/* Navigation Tabs */}
          <div className="flex justify-center gap-2 mb-12">
            {[
              { id: 'overview', label: '概览', icon: Grid3X3 },
              { id: 'screens', label: '界面预览', icon: Smartphone },
              { id: 'features', label: '功能特性', icon: Sparkles }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center gap-2 px-6 py-3 rounded-full transition-all ${
                  activeTab === tab.id
                    ? 'bg-orange-500 text-white'
                    : 'bg-white/5 text-white/60 hover:bg-white/10'
                }`}
              >
                <tab.icon className="w-4 h-4" />
                <span className="font-medium">{tab.label}</span>
              </button>
            ))}
          </div>
        </div>
      </section>

      {/* Content Sections */}
      <section className="py-16 px-4">
        <div className="max-w-6xl mx-auto">
          {/* Overview Tab */}
          {activeTab === 'overview' && (
            <div className="space-y-20">
              {/* Main Features Grid */}
              <div>
                <h2 className="text-3xl font-bold text-white text-center mb-12">核心功能</h2>
                <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {features.map((feature, index) => (
                    <FeatureCard key={index} {...feature} />
                  ))}
                </div>
              </div>

              {/* App Highlights */}
              <div className="grid md:grid-cols-2 gap-8">
                <div className="bg-gradient-to-br from-orange-500/10 to-transparent rounded-3xl p-8 border border-orange-500/20">
                  <Crown className="w-12 h-12 text-orange-500 mb-4" />
                  <h3 className="text-2xl font-bold text-white mb-4">哈苏大师认证</h3>
                  <p className="text-white/60 leading-relaxed">
                    与哈苏色彩科学团队合作，提供经过严格校准的大师级预设，
                    确保每一张照片都能呈现专业级的色彩表现。
                  </p>
                </div>
                <div className="bg-gradient-to-br from-purple-500/10 to-transparent rounded-3xl p-8 border border-purple-500/20">
                  <Sparkles className="w-12 h-12 text-purple-500 mb-4" />
                  <h3 className="text-2xl font-bold text-white mb-4">AI 智能引擎</h3>
                  <p className="text-white/60 leading-relaxed">
                    基于 TensorFlow Lite 的端侧 AI 推理，无需联网即可实现
                    场景识别、智能调参等专业功能。
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Screens Tab */}
          {activeTab === 'screens' && (
            <div className="space-y-24">
              {/* Home Screen */}
              <ScreenPreview
                title="首页 - 预设发现"
                description="瀑布流展示海量预设资源，支持品牌筛选、搜索、收藏。HNCS 标识代表哈苏大师认证预设，NEW 标识展示最新上架内容。"
              >
                <div className="h-full bg-[#0a0a0a] p-4">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <span className="text-white font-bold">OMaster</span>
                      <span className="text-[8px] px-1.5 py-0.5 rounded-full bg-gradient-to-r from-orange-500 to-amber-500 text-white flex items-center gap-0.5">
                        <Crown size={8} />
                        哈苏大师
                      </span>
                    </div>
                  </div>
                  <div className="relative mb-4">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3 h-3 text-white/40" />
                    <input type="text" placeholder="搜索预设" className="w-full pl-8 pr-3 py-2 rounded-full bg-white/5 text-white text-xs border border-white/10" />
                  </div>
                  <div className="flex gap-2 mb-4 overflow-x-auto">
                    {['全部', 'OPPO', 'vivo', '小米'].map((brand, i) => (
                      <button key={brand} className={`px-3 py-1 rounded-full text-xs whitespace-nowrap ${i === 0 ? 'bg-orange-500 text-white' : 'bg-white/5 text-white/60'}`}>
                        {brand}
                      </button>
                    ))}
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    {[1, 2, 3, 4].map((i) => (
                      <div key={i} className="aspect-[3/4] rounded-xl bg-gradient-to-br from-gray-800 to-gray-900 relative overflow-hidden">
                        <div className="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent" />
                        <div className="absolute top-2 left-2 px-1.5 py-0.5 rounded bg-orange-500 text-[8px] text-white font-bold">HNCS</div>
                        <Heart className="absolute top-2 right-2 w-3 h-3 text-white/70" />
                        <div className="absolute bottom-2 left-2 right-2">
                          <div className="text-white text-xs font-medium truncate">复古胶片</div>
                          <div className="text-white/50 text-[10px]">@摄影师小王</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </ScreenPreview>

              {/* Detail Screen */}
              <ScreenPreview
                title="预设详情页"
                description="展示预设完整信息，包括图片画廊、调色参数、拍摄建议、用户评价等。支持一键应用和收藏功能。"
              >
                <div className="h-full bg-[#0a0a0a] overflow-y-auto">
                  <div className="h-48 bg-gradient-to-br from-gray-800 to-gray-900 relative">
                    <div className="absolute top-2 left-2 px-1.5 py-0.5 rounded bg-orange-500 text-[8px] text-white font-bold flex items-center gap-0.5">
                      <Crown size={8} />
                      HNCS
                    </div>
                  </div>
                  <div className="p-4">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-white font-bold">复古胶片风格</span>
                    </div>
                    <p className="text-white/50 text-xs mb-3">@专业摄影师</p>
                    
                    <div className="flex gap-1 mb-4">
                      {['胶片', '复古', '人像'].map(tag => (
                        <span key={tag} className="px-2 py-0.5 rounded-full bg-white/5 text-[10px] text-white/60">#{tag}</span>
                      ))}
                    </div>

                    <div className="grid grid-cols-3 gap-2 mb-4">
                      {[
                        { label: '下载', value: '12.5k' },
                        { label: '评分', value: '4.9' },
                        { label: '评价', value: '856' }
                      ].map(stat => (
                        <div key={stat.label} className="text-center py-2 rounded-lg bg-white/5">
                          <div className="text-white text-sm font-bold">{stat.value}</div>
                          <div className="text-white/40 text-[10px]">{stat.label}</div>
                        </div>
                      ))}
                    </div>

                    <div className="p-3 rounded-xl bg-gradient-to-br from-gray-900 to-black border border-white/5 mb-4">
                      <div className="flex items-center gap-2 mb-2">
                        <Camera className="w-3 h-3 text-orange-500" />
                        <span className="text-white text-xs">拍摄建议</span>
                      </div>
                      <div className="space-y-1">
                        <div className="flex gap-2 text-[10px]">
                          <span className="text-orange-500">环境:</span>
                          <span className="text-white/60">日间户外</span>
                        </div>
                        <div className="flex gap-2 text-[10px]">
                          <span className="text-orange-500">场景:</span>
                          <span className="text-white/60">街拍、人像</span>
                        </div>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-2">
                      <button className="py-2.5 rounded-xl bg-white/5 text-white text-xs font-medium">收藏</button>
                      <button className="py-2.5 rounded-xl bg-orange-500 text-white text-xs font-medium">一键应用</button>
                    </div>
                  </div>
                </div>
              </ScreenPreview>

              {/* Features Screen */}
              <ScreenPreview
                title="功能中心"
                description="集成所有核心功能入口，包括 AI 场景识别、智能调参、水印编辑、云同步等专业工具。"
              >
                <div className="h-full bg-[#0a0a0a] p-4">
                  <h2 className="text-white font-bold mb-4">功能</h2>
                  <div className="space-y-3">
                    {[
                      { icon: ScanLine, title: 'AI 场景识别', desc: '智能识别拍摄场景', color: 'bg-blue-500' },
                      { icon: Wand2, title: 'AI 智能调参', desc: '自动优化照片参数', color: 'bg-purple-500' },
                      { icon: Droplets, title: '水印编辑器', desc: '专业水印制作工具', color: 'bg-emerald-500' },
                      { icon: Cloud, title: '云端同步', desc: '预设数据云备份', color: 'bg-sky-500' },
                      { icon: Share2, title: 'LUT 分享', desc: '预设分享社区', color: 'bg-rose-500' },
                      { icon: Sliders, title: '参数调节', desc: '精细调整各项参数', color: 'bg-amber-500' }
                    ].map((feature, i) => (
                      <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/5">
                        <div className={`w-10 h-10 rounded-lg ${feature.color} flex items-center justify-center`}>
                          <feature.icon className="w-5 h-5 text-white" />
                        </div>
                        <div className="flex-1">
                          <div className="text-white text-sm font-medium">{feature.title}</div>
                          <div className="text-white/50 text-xs">{feature.desc}</div>
                        </div>
                        <ChevronRight className="w-4 h-4 text-white/30" />
                      </div>
                    ))}
                  </div>
                </div>
              </ScreenPreview>

              {/* Settings Screen */}
              <ScreenPreview
                title="设置中心"
                description="个性化配置应用主题、深色模式、通知设置、预设源管理等选项。"
              >
                <div className="h-full bg-[#0a0a0a] p-4 overflow-y-auto">
                  <h2 className="text-white font-bold mb-4">设置</h2>
                  
                  <div className="space-y-4">
                    <div>
                      <div className="text-white/50 text-xs mb-2 px-1">通用</div>
                      <div className="rounded-xl bg-white/5 overflow-hidden">
                        {['振动反馈', '默认启动页'].map((item, i) => (
                          <div key={item} className="flex items-center justify-between p-3 border-b border-white/5 last:border-0">
                            <span className="text-white text-sm">{item}</span>
                            {i === 0 ? (
                              <div className="w-10 h-5 rounded-full bg-orange-500 relative">
                                <div className="absolute right-0.5 top-0.5 w-4 h-4 rounded-full bg-white" />
                              </div>
                            ) : (
                              <span className="text-white/50 text-xs">发现</span>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="text-white/50 text-xs mb-2 px-1">外观</div>
                      <div className="rounded-xl bg-white/5 overflow-hidden">
                        {['深色模式', '主题颜色'].map((item, i) => (
                          <div key={item} className="flex items-center justify-between p-3 border-b border-white/5 last:border-0">
                            <span className="text-white text-sm">{item}</span>
                            {i === 0 ? (
                              <span className="text-white/50 text-xs">跟随系统</span>
                            ) : (
                              <div className="w-5 h-5 rounded-full bg-orange-500" />
                            )}
                          </div>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="text-white/50 text-xs mb-2 px-1">其他</div>
                      <div className="rounded-xl bg-white/5 overflow-hidden">
                        {['云同步', '更新设置', '隐私政策'].map((item) => (
                          <div key={item} className="flex items-center justify-between p-3 border-b border-white/5 last:border-0">
                            <span className="text-white text-sm">{item}</span>
                            <ChevronRight className="w-4 h-4 text-white/30" />
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </ScreenPreview>
            </div>
          )}

          {/* Features Tab */}
          {activeTab === 'features' && (
            <div className="space-y-16">
              {/* AI Features */}
              <div>
                <h2 className="text-3xl font-bold text-white mb-8 flex items-center gap-3">
                  <Sparkles className="w-8 h-8 text-purple-500" />
                  AI 智能功能
                </h2>
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="bg-gradient-to-br from-purple-500/10 to-transparent rounded-2xl p-6 border border-purple-500/20">
                    <ScanLine className="w-10 h-10 text-purple-500 mb-4" />
                    <h3 className="text-xl font-bold text-white mb-2">场景识别引擎</h3>
                    <p className="text-white/60 text-sm leading-relaxed mb-4">
                      基于深度学习的场景分类器，可识别风景、人像、街拍、夜景等 20+ 种拍摄场景，
                      自动推荐最适合的预设参数。
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {['风景', '人像', '街拍', '夜景', '美食', '建筑'].map(tag => (
                        <span key={tag} className="px-2 py-1 rounded-full bg-purple-500/20 text-purple-300 text-xs">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="bg-gradient-to-br from-blue-500/10 to-transparent rounded-2xl p-6 border border-blue-500/20">
                    <Wand2 className="w-10 h-10 text-blue-500 mb-4" />
                    <h3 className="text-xl font-bold text-white mb-2">智能参数优化</h3>
                    <p className="text-white/60 text-sm leading-relaxed mb-4">
                      分析照片直方图和色彩分布，智能调整饱和度、对比度、锐度等参数，
                      让每张照片都达到最佳视觉效果。
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {['饱和度', '对比度', '锐度', '色温', '暗角', '颗粒'].map(tag => (
                        <span key={tag} className="px-2 py-1 rounded-full bg-blue-500/20 text-blue-300 text-xs">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

              {/* Hasselblad Features */}
              <div>
                <h2 className="text-3xl font-bold text-white mb-8 flex items-center gap-3">
                  <Crown className="w-8 h-8 text-orange-500" />
                  哈苏大师系统
                </h2>
                <div className="bg-gradient-to-br from-orange-500/10 via-amber-500/5 to-transparent rounded-3xl p-8 border border-orange-500/20">
                  <div className="grid md:grid-cols-3 gap-8">
                    <div>
                      <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-orange-500 to-amber-500 flex items-center justify-center mb-4">
                        <Crown className="w-6 h-6 text-white" />
                      </div>
                      <h3 className="text-lg font-bold text-white mb-2">HNCS 预设</h3>
                      <p className="text-white/60 text-sm">
                        哈苏自然色彩解决方案官方预设，真实还原哈苏相机的独特色彩科学
                      </p>
                    </div>
                    <div>
                      <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-orange-500 to-amber-500 flex items-center justify-center mb-4">
                        <Palette className="w-6 h-6 text-white" />
                      </div>
                      <h3 className="text-lg font-bold text-white mb-2">色彩校准</h3>
                      <p className="text-white/60 text-sm">
                        专业级色彩校准工具，确保预设色彩在不同设备上的一致性
                      </p>
                    </div>
                    <div>
                      <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-orange-500 to-amber-500 flex items-center justify-center mb-4">
                        <Image className="w-6 h-6 text-white" />
                      </div>
                      <h3 className="text-lg font-bold text-white mb-2">样张参考</h3>
                      <p className="text-white/60 text-sm">
                        每个预设都配有专业摄影师拍摄的样张，直观展示预设效果
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Cloud & Share */}
              <div>
                <h2 className="text-3xl font-bold text-white mb-8 flex items-center gap-3">
                  <Cloud className="w-8 h-8 text-sky-500" />
                  云端与分享
                </h2>
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-4">
                    <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5">
                      <div className="w-10 h-10 rounded-lg bg-sky-500/20 flex items-center justify-center flex-shrink-0">
                        <Cloud className="w-5 h-5 text-sky-500" />
                      </div>
                      <div>
                        <h4 className="text-white font-medium mb-1">云端同步</h4>
                        <p className="text-white/50 text-sm">预设数据自动备份到云端，换机不丢失</p>
                      </div>
                    </div>
                    <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5">
                      <div className="w-10 h-10 rounded-lg bg-rose-500/20 flex items-center justify-center flex-shrink-0">
                        <Share2 className="w-5 h-5 text-rose-500" />
                      </div>
                      <div>
                        <h4 className="text-white font-medium mb-1">LUT 分享</h4>
                        <p className="text-white/50 text-sm">将预设导出为 LUT 文件，与好友分享</p>
                      </div>
                    </div>
                  </div>
                  <div className="space-y-4">
                    <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5">
                      <div className="w-10 h-10 rounded-lg bg-emerald-500/20 flex items-center justify-center flex-shrink-0">
                        <Settings className="w-5 h-5 text-emerald-500" />
                      </div>
                      <div>
                        <h4 className="text-white font-medium mb-1">预设源管理</h4>
                        <p className="text-white/50 text-sm">订阅多个预设源，自动同步更新</p>
                      </div>
                    </div>
                    <div className="flex items-start gap-4 p-4 rounded-xl bg-white/5">
                      <div className="w-10 h-10 rounded-lg bg-amber-500/20 flex items-center justify-center flex-shrink-0">
                        <Heart className="w-5 h-5 text-amber-500" />
                      </div>
                      <div>
                        <h4 className="text-white font-medium mb-1">收藏管理</h4>
                        <p className="text-white/50 text-sm">收藏喜欢的预设，快速访问常用配方</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4">
        <div className="max-w-4xl mx-auto text-center">
          <div className="bg-gradient-to-br from-orange-500/20 via-amber-500/10 to-transparent rounded-3xl p-12 border border-orange-500/20">
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
              开启专业影像之旅
            </h2>
            <p className="text-white/60 mb-8 max-w-xl mx-auto">
              立即下载 OMaster，体验哈苏大师级预设和 AI 智能调参功能，让你的每张照片都更加出色
            </p>
            <div className="flex flex-wrap justify-center gap-4">
              <button className="flex items-center gap-2 px-8 py-4 bg-orange-500 hover:bg-orange-600 text-white rounded-full font-medium transition-colors">
                <Smartphone className="w-5 h-5" />
                下载 Android 版
              </button>
              <button className="flex items-center gap-2 px-8 py-4 bg-white/10 hover:bg-white/20 text-white rounded-full font-medium transition-colors">
                <Info className="w-5 h-5" />
                了解更多
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-8 px-4 border-t border-white/5">
        <div className="max-w-6xl mx-auto text-center">
          <p className="text-white/40 text-sm">
            © 2025 OMaster. 专业影像参数管理工具
          </p>
        </div>
      </footer>
    </div>
  );
};

export default AndroidAppShowcase;
