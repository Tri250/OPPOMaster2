import React, { useState } from 'react';
import { useAppStore } from '../store/appStore';
import {
  ArrowLeft, Smartphone, Camera, Palette, Cpu, Droplets,
  SlidersHorizontal, Images, Aperture, Cloud, Settings,
  Bell, Shield, FileText, CheckCircle, Download, Star,
  Search, Heart, Clock, Zap, Layers, Wifi, RefreshCw,
  ChevronRight, Sun, Moon, Eye, Sparkles, Lock, Globe
} from 'lucide-react';

// Android 风格主题色
const ANDROID_COLORS = {
  primary: '#FF6B35',
  background: '#121212',
  surface: '#1E1E1E',
  surfaceVariant: '#2D2D2D',
  onSurface: '#FFFFFF',
  onSurfaceVariant: 'rgba(255, 255, 255, 0.7)',
  accent: '#4CAF50',
};

// Android 手机模拟器组件
const PhoneSimulator: React.FC<{ children: React.ReactNode; title: string }> = ({ children, title }) => (
  <div className="flex flex-col items-center">
    <div className="text-white/60 text-sm mb-2">{title}</div>
    <div className="w-[280px] h-[580px] bg-black rounded-[40px] p-2 shadow-2xl border border-gray-800">
      <div className="w-full h-full bg-[#121212] rounded-[32px] overflow-hidden relative">
        {/* 刘海区域 */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[120px] h-[28px] bg-black rounded-b-2xl z-50" />
        {/* 状态栏 */}
        <div className="h-7 bg-[#121212] px-6 flex items-center justify-between text-xs text-white/60 relative z-40">
          <span>9:41</span>
          <div className="flex items-center gap-1">
            <Wifi size={12} />
            <span>100%</span>
          </div>
        </div>
        {/* 内容区域 */}
        <div className="absolute top-7 left-0 right-0 bottom-0 overflow-hidden">
          {children}
        </div>
        {/* 导航条 */}
        <div className="absolute bottom-1 left-1/2 -translate-x-1/2 w-24 h-1 bg-white/30 rounded-full" />
      </div>
    </div>
  </div>
);

// Android 顶部导航栏
const AndroidTopBar: React.FC<{ title: string; onBack?: () => void; showBack?: boolean }> = ({ title, onBack, showBack = true }) => (
  <div className="h-12 px-4 flex items-center bg-[#121212] border-b border-white/10">
    {showBack && onBack && (
      <button onClick={onBack} className="p-2 -ml-2 hover:bg-white/10 rounded-full">
        <ArrowLeft size={20} className="text-white" />
      </button>
    )}
    <span className="text-white font-medium ml-2">{title}</span>
  </div>
);

// Android 设置项
const AndroidSettingItem: React.FC<{ icon: React.ReactNode; title: string; subtitle?: string; trailing?: React.ReactNode }> = ({ icon, title, subtitle, trailing }) => (
  <div className="flex items-center justify-between py-3 px-4 hover:bg-white/5">
    <div className="flex items-center gap-3">
      <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/10 flex items-center justify-center text-[#FF6B35]">
        {icon}
      </div>
      <div>
        <div className="text-white text-sm">{title}</div>
        {subtitle && <div className="text-white/50 text-xs">{subtitle}</div>}
      </div>
    </div>
    {trailing || <ChevronRight size={18} className="text-white/30" />}
  </div>
);

// Android 开关
const AndroidSwitch: React.FC<{ checked: boolean; onChange: (checked: boolean) => void }> = ({ checked, onChange }) => (
  <div
    className={`w-12 h-6 rounded-full transition-colors ${checked ? 'bg-[#4CAF50]' : 'bg-gray-600'}`}
    onClick={() => onChange(!checked)}
  >
    <div className={`w-5 h-5 bg-white rounded-full shadow transition-transform mt-0.5 ${checked ? 'translate-x-6 ml-0.5' : 'translate-x-0.5'}`} />
  </div>
);

// Android 卡片
const AndroidCard: React.FC<{ children: React.ReactNode; className?: string }> = ({ children, className = '' }) => (
  <div className={`bg-[#1E1E1E] rounded-2xl p-4 ${className}`}>
    {children}
  </div>
);

// 主页面组件
const AndroidShowcasePage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();
  const [activeTab, setActiveTab] = useState('home');
  const [selectedScreen, setSelectedScreen] = useState<string | null>(null);

  // 底部导航项
  const navItems = [
    { id: 'home', label: '首页', icon: <Camera size={22} /> },
    { id: 'subscription', label: '订阅', icon: <Star size={22} /> },
    { id: 'features', label: '功能', icon: <Sparkles size={22} /> },
    { id: 'about', label: '关于', icon: <Settings size={22} /> },
  ];

  // 功能列表
  const featureList = [
    { id: 'ai-scene', title: 'AI 场景识别', subtitle: '识别50+拍摄场景', icon: <Camera size={24} />, color: '#4CAF50' },
    { id: 'ai-fine-tune', title: 'AI 微调', subtitle: '智能微调参数', icon: <Palette size={24} />, color: '#9C27B0' },
    { id: 'smart-optimize', title: '智能优化', subtitle: '一键优化画质', icon: <Cpu size={24} />, color: '#2196F3' },
    { id: 'lut-share', title: 'LUT 资源', subtitle: '20+专业滤镜', icon: <Palette size={24} />, color: '#9C27B0' },
    { id: 'watermark', title: '水印编辑', subtitle: '14+水印模板', icon: <Droplets size={24} />, color: '#00BCD4' },
    { id: 'hasselblad', title: '哈苏色彩', subtitle: 'HNCS 3.0 自然色彩', icon: <Aperture size={24} />, color: '#FF6B35' },
  ];

  // 设置列表
  const settingsList = [
    { icon: <Settings size={20} />, title: '设置', subtitle: '应用设置' },
    { icon: <Bell size={20} />, title: '通知设置', subtitle: '推送和提醒' },
    { icon: <Shield size={20} />, title: '隐私政策', subtitle: '隐私协议' },
    { icon: <FileText size={20} />, title: '用户协议', subtitle: '服务条款' },
    { icon: <Cloud size={20} />, title: '云同步', subtitle: '数据同步' },
    { icon: <RefreshCw size={20} />, title: '检查更新', subtitle: 'v3.2.0' },
  ];

  // 渲染各个屏幕
  const renderHomeScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="OMaster" showBack={false} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* Hero Card */}
        <div className="bg-gradient-to-br from-[#FF6B35]/20 via-[#FF6B35]/10 to-transparent rounded-2xl p-5 mb-4">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] flex items-center justify-center">
              <Camera size={28} className="text-white" />
            </div>
            <div>
              <h2 className="text-white font-bold text-lg">OMaster</h2>
              <p className="text-white/50 text-sm">专业影像参数管理</p>
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mb-4">
          <h3 className="text-white font-semibold mb-3">快捷功能</h3>
          <div className="grid grid-cols-2 gap-3">
            {featureList.slice(0, 4).map((feature) => (
              <button
                key={feature.id}
                onClick={() => setSelectedScreen(feature.id)}
                className="bg-[#1E1E1E] rounded-xl p-3 text-left hover:bg-[#2D2D2D] transition-colors"
              >
                <div className="w-10 h-10 rounded-lg flex items-center justify-center mb-2" style={{ backgroundColor: `${feature.color}20` }}>
                  <div style={{ color: feature.color }}>{feature.icon}</div>
                </div>
                <div className="text-white text-sm font-medium">{feature.title}</div>
                <div className="text-white/50 text-xs">{feature.subtitle}</div>
              </button>
            ))}
          </div>
        </div>

        {/* Featured Presets */}
        <div>
          <h3 className="text-white font-semibold mb-3">精选预设</h3>
          <div className="space-y-3">
            {[
              { name: '清新通透', style: '日系', color: '#4ECDC4' },
              { name: '电影色调', style: 'Cinematic', color: '#FF6B35' },
              { name: '复古胶片', style: 'Vintage', color: '#9C27B0' },
            ].map((preset, i) => (
              <div key={i} className="bg-[#1E1E1E] rounded-xl p-3 flex items-center gap-3">
                <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-gray-800 to-gray-900 flex items-center justify-center" style={{ borderColor: preset.color }}>
                  <Camera size={24} className="text-white/50" />
                </div>
                <div className="flex-1">
                  <div className="text-white font-medium">{preset.name}</div>
                  <div className="text-white/50 text-xs">{preset.style}</div>
                </div>
                <div className="flex items-center gap-1 text-yellow-400">
                  <Star size={14} />
                  <span className="text-xs">4.9</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Navigation */}
      <div className="h-16 bg-[#1E1E1E] border-t border-white/10 flex items-center justify-around px-4">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => setActiveTab(item.id)}
            className={`flex flex-col items-center gap-1 ${activeTab === item.id ? 'text-[#FF6B35]' : 'text-white/50'}`}
          >
            {item.icon}
            <span className="text-xs">{item.label}</span>
          </button>
        ))}
      </div>
    </div>
  );

  const renderFeaturesScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="核心功能" showBack={false} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* AI 功能区 */}
        <div className="mb-4">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/20 flex items-center justify-center">
              <Sparkles size={16} className="text-[#FF6B35]" />
            </div>
            <span className="text-white font-semibold">AI 智能功能</span>
          </div>
          <div className="space-y-3">
            {featureList.slice(0, 2).map((feature) => (
              <button
                key={feature.id}
                onClick={() => setSelectedScreen(feature.id)}
                className="w-full bg-gradient-to-r from-[#1B5E20] to-[#2E7D32] rounded-2xl p-4 text-left"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-white/15 flex items-center justify-center">
                      <div className="text-white">{feature.icon}</div>
                    </div>
                    <div>
                      <div className="text-white font-semibold">{feature.title}</div>
                      <div className="text-white/70 text-sm">{feature.subtitle}</div>
                    </div>
                  </div>
                  <ChevronRight size={20} className="text-white/50" />
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* 专业工具区 */}
        <div className="mb-4">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/20 flex items-center justify-center">
              <Settings size={16} className="text-[#FF6B35]" />
            </div>
            <span className="text-white font-semibold">专业工具</span>
          </div>
          <div className="space-y-3">
            {featureList.slice(3, 5).map((feature) => (
              <button
                key={feature.id}
                onClick={() => setSelectedScreen(feature.id)}
                className="w-full bg-gradient-to-r from-[#006064] to-[#00838F] rounded-2xl p-4 text-left"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-white/15 flex items-center justify-center">
                      <div className="text-white">{feature.icon}</div>
                    </div>
                    <div>
                      <div className="text-white font-semibold">{feature.title}</div>
                      <div className="text-white/70 text-sm">{feature.subtitle}</div>
                    </div>
                  </div>
                  <ChevronRight size={20} className="text-white/50" />
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* 品牌特色区 */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/20 flex items-center justify-center">
              <Aperture size={16} className="text-[#FF6B35]" />
            </div>
            <span className="text-white font-semibold">品牌特色</span>
          </div>
          <button
            onClick={() => setSelectedScreen('hasselblad')}
            className="w-full bg-gradient-to-r from-[#CC5500] to-[#E86A17] rounded-2xl p-4 text-left"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-white/15 flex items-center justify-center">
                  <Aperture size={24} className="text-white" />
                </div>
                <div>
                  <div className="text-white font-semibold">哈苏色彩科学</div>
                  <div className="text-white/70 text-sm">HNCS 3.0 自然色彩</div>
                </div>
              </div>
              <ChevronRight size={20} className="text-white/50" />
            </div>
          </button>
        </div>
      </div>

      {/* Bottom Navigation */}
      <div className="h-16 bg-[#1E1E1E] border-t border-white/10 flex items-center justify-around px-4">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => setActiveTab(item.id)}
            className={`flex flex-col items-center gap-1 ${activeTab === item.id ? 'text-[#FF6B35]' : 'text-white/50'}`}
          >
            {item.icon}
            <span className="text-xs">{item.label}</span>
          </button>
        ))}
      </div>
    </div>
  );

  const renderAboutScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="关于" showBack={false} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* App Info Card */}
        <div className="bg-gradient-to-br from-[#FF6B35]/20 via-[#FF6B35]/10 to-transparent rounded-2xl p-6 mb-4 text-center">
          <div className="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] flex items-center justify-center shadow-lg shadow-[#FF6B35]/30">
            <Camera size={36} className="text-white" />
          </div>
          <h2 className="text-2xl font-bold text-white mb-1">OMaster</h2>
          <p className="text-white/50 text-sm mb-4">专业影像参数管理工具</p>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10">
            <span className="text-white/70 text-xs">版本 3.2.0</span>
            <span className="w-1 h-1 rounded-full bg-[#FF6B35]" />
            <span className="text-[#FF6B35] text-xs">最新</span>
          </div>
        </div>

        {/* Settings List */}
        <AndroidCard>
          {settingsList.map((item, index) => (
            <React.Fragment key={item.title}>
              {index > 0 && <div className="border-t border-white/10" />}
              <AndroidSettingItem
                icon={item.icon}
                title={item.title}
                subtitle={item.subtitle}
              />
            </React.Fragment>
          ))}
        </AndroidCard>

        {/* Features Preview */}
        <div className="mt-4">
          <h3 className="text-white font-semibold mb-3 px-1">功能亮点</h3>
          <AndroidCard>
            <div className="space-y-3">
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-[#4CAF50]/20 flex items-center justify-center flex-shrink-0">
                  <Zap size={16} className="text-[#4CAF50]" />
                </div>
                <div>
                  <div className="text-white font-medium text-sm">AI 智能识别</div>
                  <div className="text-white/50 text-xs">50+ 场景自动识别</div>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/20 flex items-center justify-center flex-shrink-0">
                  <Layers size={16} className="text-[#FF6B35]" />
                </div>
                <div>
                  <div className="text-white font-medium text-sm">哈苏色彩科学</div>
                  <div className="text-white/50 text-xs">HNCS 3.0 自然色彩</div>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-[#9C27B0]/20 flex items-center justify-center flex-shrink-0">
                  <Download size={16} className="text-[#9C27B0]" />
                </div>
                <div>
                  <div className="text-white font-medium text-sm">LUT 资源库</div>
                  <div className="text-white/50 text-xs">20+ 专业滤镜下载</div>
                </div>
              </div>
            </div>
          </AndroidCard>
        </div>

        <div className="text-center mt-6 text-white/30 text-xs">
          <p>Developed by Silas</p>
          <p className="mt-1">© 2026 OMaster. All rights reserved.</p>
        </div>
      </div>

      {/* Bottom Navigation */}
      <div className="h-16 bg-[#1E1E1E] border-t border-white/10 flex items-center justify-around px-4">
        {navItems.map((item) => (
          <button
            key={item.id}
            onClick={() => setActiveTab(item.id)}
            className={`flex flex-col items-center gap-1 ${activeTab === item.id ? 'text-[#FF6B35]' : 'text-white/50'}`}
          >
            {item.icon}
            <span className="text-xs">{item.label}</span>
          </button>
        ))}
      </div>
    </div>
  );

  const renderAISceneScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="AI 场景识别" onBack={() => setSelectedScreen(null)} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* Hero Card */}
        <AndroidCard className="bg-gradient-to-br from-[#4CAF50]/20 to-[#4CAF50]/5 mb-4">
          <div className="text-center">
            <Camera size={32} className="text-[#4CAF50] mx-auto mb-2" />
            <h3 className="text-white font-bold">智能场景识别</h3>
            <p className="text-white/60 text-sm mb-4">识别50+拍摄场景，自动推荐最佳参数</p>
            <button className="px-6 py-2 bg-[#4CAF50] text-white rounded-full text-sm font-medium">
              开始识别
            </button>
          </div>
        </AndroidCard>

        {/* Scene Categories */}
        <div className="mb-4">
          <h3 className="text-white font-semibold mb-3">场景分类</h3>
          <div className="grid grid-cols-2 gap-3">
            {[
              { name: '人像系列', count: '8种', color: '#FF6B9D' },
              { name: '风景系列', count: '10种', color: '#4ECDC4' },
              { name: '美食系列', count: '6种', color: '#FF9800' },
              { name: '夜景系列', count: '5种', color: '#9C27B0' },
            ].map((cat) => (
              <button
                key={cat.name}
                className="bg-[#1E1E1E] rounded-xl p-3 text-left hover:bg-[#2D2D2D]"
              >
                <div className="w-8 h-8 rounded-lg mb-2" style={{ backgroundColor: `${cat.color}20` }}>
                  <Camera size={16} style={{ color: cat.color }} className="m-1.5" />
                </div>
                <div className="text-white text-sm font-medium">{cat.name}</div>
                <div className="text-white/50 text-xs">{cat.count}</div>
              </button>
            ))}
          </div>
        </div>

        {/* Scene Types */}
        <div>
          <h3 className="text-white font-semibold mb-3">精细场景</h3>
          <div className="space-y-2">
            {[
              { name: '人像', desc: '优化肤色，自然美化', icon: <Camera size={18} /> },
              { name: '逆光人像', desc: '保留逆光氛围，提亮面部', icon: <Sun size={18} /> },
              { name: '风景', desc: '增强自然色彩', icon: <Layers size={18} /> },
              { name: '日落', desc: '强化日落暖调', icon: <Sparkles size={18} /> },
            ].map((scene) => (
              <button
                key={scene.name}
                className="w-full bg-[#1E1E1E] rounded-xl p-3 flex items-center gap-3 hover:bg-[#2D2D2D]"
              >
                <div className="w-10 h-10 rounded-lg bg-[#4CAF50]/20 flex items-center justify-center text-[#4CAF50]">
                  {scene.icon}
                </div>
                <div className="flex-1 text-left">
                  <div className="text-white text-sm font-medium">{scene.name}</div>
                  <div className="text-white/50 text-xs">{scene.desc}</div>
                </div>
                <ChevronRight size={18} className="text-white/30" />
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );

  const renderLUTScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="LUT 资源分享" onBack={() => setSelectedScreen(null)} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* Search Bar */}
        <div className="bg-[#2D2D2D] rounded-xl px-4 py-3 flex items-center gap-2 mb-4">
          <Search size={18} className="text-white/50" />
          <input
            type="text"
            placeholder="搜索 LUT 滤镜"
            className="bg-transparent text-white text-sm flex-1 outline-none placeholder:text-white/30"
          />
        </div>

        {/* Categories */}
        <div className="flex gap-2 mb-4 overflow-x-auto pb-2">
          {['全部', '电影色调', '胶片风格', '日系清新', '欧美复古'].map((cat) => (
            <button
              key={cat}
              className={`px-4 py-2 rounded-full text-sm whitespace-nowrap ${
                cat === '全部' ? 'bg-[#FF6B35] text-white' : 'bg-[#2D2D2D] text-white/70'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* LUT List */}
        <div className="space-y-3">
          {[
            { name: '氧气感2026', desc: '清新通透，适合人像', category: '日系清新', rating: 4.9, downloads: 5000 },
            { name: '莫兰迪2026', desc: '温柔低饱和', category: '电影色调', rating: 4.8, downloads: 4200 },
            { name: '哈苏自然色彩', desc: 'HNCS自然色彩还原', category: '自然风景', rating: 4.9, downloads: 8500 },
            { name: '电影冷调', desc: '好莱坞大片风格', category: '电影色调', rating: 4.7, downloads: 6800 },
            { name: '经典胶片', desc: '复古胶片质感', category: '胶片风格', rating: 4.8, downloads: 5600 },
          ].map((lut) => (
            <button
              key={lut.name}
              className="w-full bg-[#1E1E1E] rounded-xl p-3 flex items-center gap-3 hover:bg-[#2D2D2D]"
            >
              <div className="w-16 h-16 rounded-lg bg-gradient-to-br from-[#FF6B35]/20 to-[#9C27B0]/20 flex items-center justify-center">
                <Palette size={24} className="text-[#FF6B35]" />
              </div>
              <div className="flex-1 text-left">
                <div className="text-white font-medium">{lut.name}</div>
                <div className="text-white/50 text-xs">{lut.desc}</div>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs px-2 py-0.5 rounded bg-[#FF6B35]/20 text-[#FF6B35]">{lut.category}</span>
                  <span className="text-white/30 text-xs">⭐ {lut.rating}</span>
                  <span className="text-white/30 text-xs">{lut.downloads} 下载</span>
                </div>
              </div>
              <Download size={18} className="text-[#FF6B35]" />
            </button>
          ))}
        </div>
      </div>
    </div>
  );

  const renderHasselbladScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="哈苏色彩科学" onBack={() => setSelectedScreen(null)} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* Hero Card */}
        <AndroidCard className="bg-gradient-to-br from-[#FF6B35]/20 to-[#FF6B35]/5 mb-4">
          <div className="text-center">
            <Aperture size={32} className="text-[#FF6B35] mx-auto mb-2" />
            <h3 className="text-white font-bold">HNCS 3.0</h3>
            <p className="text-white/60 text-sm">哈苏自然色彩解决方案</p>
            <p className="text-white/50 text-xs mt-2">还原真实色彩，呈现自然之美</p>
          </div>
        </AndroidCard>

        {/* Color Modes */}
        <div className="mb-4">
          <h3 className="text-white font-semibold mb-3">色彩模式</h3>
          <div className="space-y-2">
            {[
              { name: '哈苏自然色彩', desc: 'HNCS 3.0 自然色彩', color: '#4CAF50', selected: true },
              { name: '人像肤色优化', desc: '自然美化肤色，保留细节', color: '#FF6B9D', selected: false },
              { name: '风景色彩增强', desc: '增强风景色彩层次', color: '#4ECDC4', selected: false },
              { name: '哈苏经典胶片', desc: '复古胶片色彩质感', color: '#9C27B0', selected: false },
              { name: '哈苏黑白', desc: '经典黑白摄影风格', color: '#808080', selected: false },
              { name: '鲜艳色彩', desc: '鲜艳饱满的色彩表现', color: '#FF9800', selected: false },
            ].map((mode) => (
              <button
                key={mode.name}
                className={`w-full rounded-xl p-3 flex items-center gap-3 ${
                  mode.selected ? 'bg-[#FF6B35]/20 border border-[#FF6B35]/50' : 'bg-[#1E1E1E]'
                }`}
              >
                <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ backgroundColor: `${mode.color}20` }}>
                  <Eye size={18} style={{ color: mode.color }} />
                </div>
                <div className="flex-1 text-left">
                  <div className="text-white text-sm font-medium">{mode.name}</div>
                  <div className="text-white/50 text-xs">{mode.desc}</div>
                </div>
                {mode.selected && <CheckCircle size={18} className="text-[#FF6B35]" />}
              </button>
            ))}
          </div>
        </div>

        {/* Features */}
        <div>
          <h3 className="text-white font-semibold mb-3">核心特性</h3>
          <div className="space-y-3">
            <AndroidCard>
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-lg bg-[#FF6B35]/20 flex items-center justify-center">
                  <Zap size={18} className="text-[#FF6B35]" />
                </div>
                <div>
                  <div className="text-white font-medium">自然肤色还原</div>
                  <div className="text-white/50 text-xs">智能识别肤色区域，自然美化不偏色</div>
                </div>
              </div>
            </AndroidCard>
            <AndroidCard>
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-lg bg-blue-500/20 flex items-center justify-center">
                  <Layers size={18} className="text-blue-500" />
                </div>
                <div>
                  <div className="text-white font-medium">色彩层次增强</div>
                  <div className="text-white/50 text-xs">智能增强色彩过渡，层次更丰富</div>
                </div>
              </div>
            </AndroidCard>
            <AndroidCard>
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-lg bg-purple-500/20 flex items-center justify-center">
                  <Sparkles size={18} className="text-purple-500" />
                </div>
                <div>
                  <div className="text-white font-medium">16-bit 色彩深度</div>
                  <div className="text-white/50 text-xs">超高色彩精度，细节分毫毕现</div>
                </div>
              </div>
            </AndroidCard>
          </div>
        </div>
      </div>
    </div>
  );

  const renderCloudSyncScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="云同步" onBack={() => setSelectedScreen(null)} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* Sync Status Card */}
        <AndroidCard className="bg-gradient-to-br from-blue-500/20 to-blue-500/5 mb-4">
          <div className="text-center">
            <Cloud size={32} className="text-blue-500 mx-auto mb-2" />
            <h3 className="text-white font-bold">同步状态</h3>
            <p className="text-white/60 text-sm">自动同步已开启</p>
            <p className="text-white/50 text-xs mt-2">最后同步：2分钟前</p>
            <button className="px-6 py-2 bg-blue-500 text-white rounded-full text-sm font-medium mt-4">
              立即同步
            </button>
          </div>
        </AndroidCard>

        {/* Cloud Providers */}
        <div className="mb-4">
          <h3 className="text-white font-semibold mb-3">云服务提供商</h3>
          <AndroidCard>
            {[
              { name: 'OPPO Cloud', color: '#1E90FF', connected: true },
              { name: 'realme Cloud', color: '#FFD700', connected: false },
              { name: 'vivo Cloud', color: '#4169E1', connected: false },
              { name: '荣耀 Cloud', color: '#32CD32', connected: false },
            ].map((provider, i) => (
              <React.Fragment key={provider.name}>
                {i > 0 && <div className="border-t border-white/10" />}
                <div className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ backgroundColor: `${provider.color}20` }}>
                      <Smartphone size={18} style={{ color: provider.color }} />
                    </div>
                    <span className="text-white text-sm">{provider.name}</span>
                  </div>
                  {provider.connected ? (
                    <CheckCircle size={18} className="text-green-500" />
                  ) : (
                    <span className="text-blue-500 text-sm">连接</span>
                  )}
                </div>
              </React.Fragment>
            ))}
          </AndroidCard>
        </div>

        {/* Sync Items */}
        <div>
          <h3 className="text-white font-semibold mb-3">同步特性</h3>
          <div className="space-y-3">
            <AndroidCard>
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-lg bg-green-500/20 flex items-center justify-center">
                  <Shield size={18} className="text-green-500" />
                </div>
                <div>
                  <div className="text-white font-medium">端到端加密</div>
                  <div className="text-white/50 text-xs">您的数据完全加密，安全可靠</div>
                </div>
              </div>
            </AndroidCard>
            <AndroidCard>
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-lg bg-purple-500/20 flex items-center justify-center">
                  <Wifi size={18} className="text-purple-500" />
                </div>
                <div>
                  <div className="text-white font-medium">Wi-Fi 自动同步</div>
                  <div className="text-white/50 text-xs">仅在 Wi-Fi 下自动同步，节省流量</div>
                </div>
              </div>
            </AndroidCard>
          </div>
        </div>
      </div>
    </div>
  );

  const renderNotificationScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="通知设置" onBack={() => setSelectedScreen(null)} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* Main Switch */}
        <AndroidCard className="bg-gradient-to-br from-[#FF6B35]/20 to-[#FF6B35]/5 mb-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Bell size={24} className="text-[#FF6B35]" />
              <div>
                <div className="text-white font-medium">接收推送通知</div>
                <div className="text-white/50 text-xs">开启后接收重要通知</div>
              </div>
            </div>
            <AndroidSwitch checked={true} onChange={() => {}} />
          </div>
        </AndroidCard>

        {/* Notification Types */}
        <div className="mb-4">
          <h3 className="text-white font-semibold mb-3">通知类型</h3>
          <AndroidCard>
            {[
              { title: '功能更新通知', desc: '接收新功能和更新提醒', enabled: true },
              { title: '预设推荐', desc: '接收个性化预设推荐', enabled: true },
              { title: '云同步提醒', desc: '同步状态变更通知', enabled: true },
              { title: '系统公告', desc: '重要系统公告通知', enabled: false },
              { title: '每日提示', desc: '摄影技巧每日提示', enabled: false },
            ].map((item, i) => (
              <React.Fragment key={item.title}>
                {i > 0 && <div className="border-t border-white/10" />}
                <div className="flex items-center justify-between py-3">
                  <div>
                    <div className="text-white text-sm">{item.title}</div>
                    <div className="text-white/50 text-xs">{item.desc}</div>
                  </div>
                  <AndroidSwitch checked={item.enabled} onChange={() => {}} />
                </div>
              </React.Fragment>
            ))}
          </AndroidCard>
        </div>

        {/* Do Not Disturb */}
        <div>
          <h3 className="text-white font-semibold mb-3">免打扰设置</h3>
          <AndroidCard>
            <div className="flex items-center justify-between py-2">
              <div className="flex items-center gap-3">
                <Moon size={20} className="text-purple-500" />
                <span className="text-white text-sm">夜间免打扰</span>
              </div>
              <AndroidSwitch checked={false} onChange={() => {}} />
            </div>
            <div className="border-t border-white/10 pt-3 mt-3">
              <div className="flex items-center justify-between">
                <span className="text-white/70 text-sm">免打扰时段</span>
                <span className="text-white text-sm">22:00 - 08:00</span>
              </div>
            </div>
          </AndroidCard>
        </div>
      </div>
    </div>
  );

  const renderTermsScreen = () => (
    <div className="h-full flex flex-col bg-[#121212]">
      <AndroidTopBar title="用户协议" onBack={() => setSelectedScreen(null)} />
      <div className="flex-1 overflow-y-auto p-4">
        {/* Update Info */}
        <AndroidCard className="mb-4">
          <div className="flex items-center gap-2 text-white/50 text-sm">
            <Clock size={14} />
            <span>最后更新：2026年6月1日</span>
          </div>
        </AndroidCard>

        {/* Welcome Card */}
        <AndroidCard className="bg-gradient-to-br from-purple-500/20 to-purple-500/5 mb-4">
          <div className="text-center">
            <CheckCircle size={40} className="text-purple-500 mx-auto mb-3" />
            <h3 className="text-white font-bold mb-2">欢迎使用我们的服务</h3>
            <p className="text-white/70 text-sm">使用我们的应用即表示您同意本用户协议和隐私政策</p>
          </div>
        </AndroidCard>

        {/* Key Terms */}
        <div className="mb-4">
          <h3 className="text-white font-semibold mb-3">重要条款</h3>
          <div className="space-y-3">
            {[
              { icon: <Settings size={18} />, title: '用户责任', desc: '您需要对自己的账户安全和使用行为负责', color: '#4CAF50' },
              { icon: <Shield size={18} />, title: '知识产权', desc: '应用内所有内容均受知识产权法保护', color: '#2196F3' },
              { icon: <Globe size={18} />, title: '服务范围', desc: '我们致力于提供稳定、高质量的摄影工具服务', color: '#FF9800' },
              { icon: <Lock size={18} />, title: '隐私保护', desc: '我们严格保护您的个人信息和隐私安全', color: '#9C27B0' },
            ].map((item) => (
              <AndroidCard key={item.title}>
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ backgroundColor: `${item.color}20` }}>
                    <div style={{ color: item.color }}>{item.icon}</div>
                  </div>
                  <div>
                    <div className="text-white font-medium">{item.title}</div>
                    <div className="text-white/50 text-xs">{item.desc}</div>
                  </div>
                </div>
              </AndroidCard>
            ))}
          </div>
        </div>

        {/* Full Terms */}
        <div>
          <h3 className="text-white font-semibold mb-3">完整协议</h3>
          <AndroidCard>
            <div className="space-y-4 text-sm">
              {[
                { num: '1', title: '服务条款', content: '本应用提供专业的摄影后期处理工具，包括但不限于 AI 场景识别、色彩调整、滤镜效果、水印添加等功能。' },
                { num: '2', title: '用户账户', content: '用户需要注册账户才能使用部分功能。请妥善保管账户信息，对账户下的所有行为负责。' },
                { num: '3', title: '用户内容', content: '用户上传和处理的图片内容归用户所有。我们不会在未经许可的情况下使用或分享您的图片内容。' },
                { num: '4', title: '禁止行为', content: '禁止利用本应用从事任何违法活动，禁止传播恶意代码，禁止攻击或干扰服务正常运行。' },
              ].map((term) => (
                <div key={term.num}>
                  <div className="flex items-center gap-2 mb-1">
                    <div className="w-6 h-6 rounded-full bg-[#FF6B35] flex items-center justify-center">
                      <span className="text-white text-xs font-bold">{term.num}</span>
                    </div>
                    <span className="text-white font-medium">{term.title}</span>
                  </div>
                  <div className="text-white/60 text-xs ml-8">{term.content}</div>
                </div>
              ))}
            </div>
          </AndroidCard>
        </div>
      </div>
    </div>
  );

  // 渲染选中的屏幕
  const renderSelectedScreen = () => {
    switch (selectedScreen) {
      case 'ai-scene':
        return renderAISceneScreen();
      case 'lut-share':
        return renderLUTScreen();
      case 'hasselblad':
        return renderHasselbladScreen();
      case 'cloud-sync':
        return renderCloudSyncScreen();
      case 'notification':
        return renderNotificationScreen();
      case 'terms':
        return renderTermsScreen();
      default:
        return null;
    }
  };

  // 主渲染
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 py-8 px-4">
      {/* Header */}
      <div className="max-w-6xl mx-auto mb-8 text-center">
        <div className="flex items-center justify-center gap-3 mb-4">
          <Smartphone size={32} className="text-[#FF6B35]" />
          <h1 className="text-3xl font-bold text-white">Android APP 功能展示</h1>
        </div>
        <p className="text-white/60">以 Web 方式呈现 Android APP 的所有功能界面 UI</p>
      </div>

      {/* Phone Simulators */}
      <div className="max-w-6xl mx-auto">
        {selectedScreen ? (
          // 单屏展示模式
          <div className="flex justify-center">
            <PhoneSimulator title={getScreenTitle(selectedScreen)}>
              {renderSelectedScreen()}
            </PhoneSimulator>
          </div>
        ) : (
          // 多屏展示模式
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 justify-items-center">
            <PhoneSimulator title="首页">
              {renderHomeScreen()}
            </PhoneSimulator>
            <PhoneSimulator title="核心功能">
              {renderFeaturesScreen()}
            </PhoneSimulator>
            <PhoneSimulator title="关于">
              {renderAboutScreen()}
            </PhoneSimulator>
          </div>
        )}
      </div>

      {/* Feature List */}
      <div className="max-w-6xl mx-auto mt-12">
        <h2 className="text-2xl font-bold text-white text-center mb-6">功能列表</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {featureList.map((feature) => (
            <button
              key={feature.id}
              onClick={() => setSelectedScreen(feature.id)}
              className="bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl p-4 text-center transition-all hover:scale-105"
            >
              <div className="w-12 h-12 rounded-xl mx-auto mb-3 flex items-center justify-center" style={{ backgroundColor: `${feature.color}20` }}>
                <div style={{ color: feature.color }}>{feature.icon}</div>
              </div>
              <div className="text-white font-medium text-sm">{feature.title}</div>
              <div className="text-white/50 text-xs mt-1">{feature.subtitle}</div>
            </button>
          ))}
          <button
            onClick={() => setSelectedScreen('cloud-sync')}
            className="bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl p-4 text-center transition-all hover:scale-105"
          >
            <div className="w-12 h-12 rounded-xl mx-auto mb-3 flex items-center justify-center bg-blue-500/20">
              <Cloud size={24} className="text-blue-500" />
            </div>
            <div className="text-white font-medium text-sm">云同步</div>
            <div className="text-white/50 text-xs mt-1">多平台数据同步</div>
          </button>
          <button
            onClick={() => setSelectedScreen('notification')}
            className="bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl p-4 text-center transition-all hover:scale-105"
          >
            <div className="w-12 h-12 rounded-xl mx-auto mb-3 flex items-center justify-center bg-green-500/20">
              <Bell size={24} className="text-green-500" />
            </div>
            <div className="text-white font-medium text-sm">通知设置</div>
            <div className="text-white/50 text-xs mt-1">推送和提醒</div>
          </button>
          <button
            onClick={() => setSelectedScreen('terms')}
            className="bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl p-4 text-center transition-all hover:scale-105"
          >
            <div className="w-12 h-12 rounded-xl mx-auto mb-3 flex items-center justify-center bg-purple-500/20">
              <FileText size={24} className="text-purple-500" />
            </div>
            <div className="text-white font-medium text-sm">用户协议</div>
            <div className="text-white/50 text-xs mt-1">服务条款</div>
          </button>
        </div>
      </div>

      {/* Back Button */}
      {selectedScreen && (
        <div className="fixed bottom-8 left-1/2 -translate-x-1/2">
          <button
            onClick={() => setSelectedScreen(null)}
            className="px-6 py-3 bg-[#FF6B35] text-white rounded-full font-medium shadow-lg shadow-[#FF6B35]/30 hover:bg-[#FF8855] transition-colors"
          >
            返回总览
          </button>
        </div>
      )}
    </div>
  );
};

// Helper function to get screen title
const getScreenTitle = (screenId: string): string => {
  const titles: Record<string, string> = {
    'ai-scene': 'AI 场景识别',
    'ai-fine-tune': 'AI 微调',
    'smart-optimize': '智能优化',
    'lut-share': 'LUT 资源分享',
    'watermark': '水印编辑',
    'hasselblad': '哈苏色彩科学',
    'cloud-sync': '云同步',
    'notification': '通知设置',
    'terms': '用户协议',
  };
  return titles[screenId] || '功能详情';
};

export default AndroidShowcasePage;
