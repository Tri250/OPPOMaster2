import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { 
  ArrowLeft, Sparkles, Award, Brain, Camera, Palette, Layers, 
  TrendingUp, Users, Star, CheckCircle2, AlertCircle, Lightbulb,
  Target, Rocket, Heart, Cloud, Zap, Crown, BarChart3, Eye,
  Smartphone, Globe, Shield, Gauge, Sliders, Droplets, Cpu, Wand2,
  ChevronRight, BookOpen, Image as ImageIcon, Aperture, Grid3X3
} from 'lucide-react';
import { frameTemplates, frameCategories, collageLayouts } from '../../data/frameTemplates';

type TabType = 'hasselblad' | 'analysis' | 'modules' | 'resources' | 'suggestions' | 'roadmap';

const HasselbladPage: React.FC = () => {
  const { goBack, setAiParam, navigateToSubPage } = useAppStore();
  const [activeTab, setActiveTab] = useState<TabType>('hasselblad');
  const [selectedStyle, setSelectedStyle] = useState<string>('rich');

  // 哈苏色彩风格
  const hncsStyles = [
    {
      id: 'rich',
      name: '浓郁风格',
      english: 'HNCS Rich',
      description: '哈苏自然色彩解决方案·浓郁风格',
      color: 'from-[#FF6B35] to-[#FF9800]',
      bgColor: 'bg-gradient-to-br from-[#FF6B35]/20 to-[#FF9800]/10',
      params: { saturation: 12, contrast: 10, brightness: 3, warmth: 5, clarity: 15 },
      features: ['色彩饱和度+12', '对比度+10', '清晰度+15', '高光压缩-10', '阴影细节+5'],
      suitable: '风景、街拍、产品',
      developer: 'OPPO × Hasselblad 联合调校',
    },
    {
      id: 'natural',
      name: '自然风格',
      english: 'HNCS Natural',
      description: '哈苏自然色彩解决方案·自然风格',
      color: 'from-[#4CAF50] to-[#2E7D32]',
      bgColor: 'bg-gradient-to-br from-[#4CAF50]/20 to-[#2E7D32]/10',
      params: { saturation: 5, contrast: 8, brightness: 0, warmth: 2, clarity: 10 },
      features: ['色彩饱和度+5', '对比度+8', '清晰度+10', '真实还原', '低噪点'],
      suitable: '人像、日常、纪实',
      developer: 'OPPO × Hasselblad 联合调校',
    },
    {
      id: 'portrait',
      name: '人像风格',
      english: 'HNCS Portrait',
      description: '专为 OPPO Find X8 Pro 人像镜头优化',
      color: 'from-[#E91E63] to-[#C2185B]',
      bgColor: 'bg-gradient-to-br from-[#E91E63]/20 to-[#C2185B]/10',
      params: { saturation: 8, contrast: 5, brightness: 5, warmth: 8, skinSmooth: 20, clarity: 8 },
      features: ['肤色优化+20', '色彩饱和+8', '暖调+8', '智能美肤', '保留肤质细节'],
      suitable: '人像写真、儿童、闺蜜',
      developer: '基于 Find X8 Pro 哈苏人像镜头调校',
    },
    {
      id: 'classic',
      name: '经典复古',
      english: 'HNCS Classic',
      description: '致敬哈苏500系列胶片色彩',
      color: 'from-[#795548] to-[#5D4037]',
      bgColor: 'bg-gradient-to-br from-[#795548]/20 to-[#5D4037]/10',
      params: { saturation: -8, contrast: 12, brightness: 0, warmth: 10, clarity: 5 },
      features: ['复古胶片感', '暖调+10', '颗粒感+5', '暗角+10'],
      suitable: '人文、街拍、复古主题',
      developer: '致敬 Hasselblad 500C/M 经典色调',
    },
    {
      id: 'bw',
      name: '极致黑白',
      english: 'HNCS Mono',
      description: '哈苏黑白大师模式',
      color: 'from-[#424242] to-[#212121]',
      bgColor: 'bg-gradient-to-br from-[#424242]/20 to-[#212121]/10',
      params: { saturation: -100, contrast: 25, brightness: 5, clarity: 18, sharpness: 20 },
      features: ['去饱和度-100', '对比度+25', '清晰度+18', '高对比黑白'],
      suitable: '纪实、艺术、人文',
      developer: '致敬 Hasselblad 907X 黑白相机',
    },
  ];

  // 产品体验分析数据
  const analysisData = {
    overall: {
      score: 8.4,
      maxScore: 10,
      competitors: [
        { name: 'VSCO', score: 9.0, highlight: '胶片质感' },
        { name: 'Lightroom', score: 9.2, highlight: '专业功能' },
        { name: 'Snapseed', score: 8.6, highlight: '基础体验' },
        { name: 'OMaster', score: 8.4, highlight: '哈苏生态' },
        { name: '醒图', score: 8.7, highlight: '本土化' },
        { name: '黄油相机', score: 8.3, highlight: '文字排版' },
      ],
    },
    dimensions: [
      { name: '功能完整度', score: 8.5, weight: 25 },
      { name: '交互体验', score: 8.0, weight: 20 },
      { name: 'AI智能化', score: 7.5, weight: 20 },
      { name: '视觉设计', score: 9.0, weight: 15 },
      { name: '生态集成', score: 9.5, weight: 10 },
      { name: '社区活跃', score: 6.0, weight: 10 },
    ],
    strengths: [
      { title: 'OPPO/一加生态深度集成', desc: 'HNCS 哈苏色彩与 Find 系列硬件无缝协同' },
      { title: '品牌调性专业且精致', desc: '黑金主题与品牌色运用成熟，设计语言一致性强' },
      { title: '预设库质量高', desc: '针对中国摄影用户场景化预设丰富' },
    ],
    weaknesses: [
      { title: 'AI 场景识别仍偏模拟', desc: '35+场景识别需真实模型推理，而非概率随机' },
      { title: '缺少社区与UGC分享', desc: '对标 VSCO/醒图等竞品，缺少创作者生态' },
      { title: 'RAW 处理能力缺失', desc: '专业摄影用户对 RAW 格式有强需求' },
      { title: '批量处理功能薄弱', desc: '无法满足旅行/活动多张照片统一调色' },
    ],
  };

  // 改进建议
  const improvements = [
    {
      priority: 'P0',
      category: 'AI能力升级',
      title: '接入真实 TensorFlow Lite 场景识别模型',
      impact: '高',
      effort: '中',
      desc: '当前识别为概率模拟，需训练专属场景模型，支持离线推理，识别准确率 95%+',
      roi: '★ ★ ★ ★ ★',
    },
    {
      priority: 'P0',
      category: '专业功能',
      title: '新增 RAW 格式处理能力',
      impact: '高',
      effort: '高',
      desc: '支持 DNG 格式，保留更多动态范围，为专业摄影用户服务',
      roi: '★ ★ ★ ★ ★',
    },
    {
      priority: 'P1',
      category: '社区生态',
      title: '上线"发现"页与创作者中心',
      impact: '高',
      effort: '中',
      desc: '参考 VSCO 社区模式，UGC 预设分享 + 创作者认证体系',
      roi: '★ ★ ★ ★',
    },
    {
      priority: 'P1',
      category: '交互优化',
      title: '参数调节支持 HSL 精细调节与色调曲线',
      impact: '中',
      effort: '中',
      desc: '8 色独立色相/饱和度/明度调节 + RGB 分离曲线',
      roi: '★ ★ ★ ★',
    },
    {
      priority: 'P1',
      category: '工作流',
      title: '新增"一键成片"智能工作流',
      impact: '中',
      effort: '低',
      desc: 'AI 自动分析内容并推荐3种风格，应用参数+水印一键导出',
      roi: '★ ★ ★',
    },
    {
      priority: 'P2',
      category: '性能',
      title: '大图分块处理 + 后台异步',
      impact: '中',
      effort: '中',
      desc: '解决大图处理 OOM 问题，提升专业用户使用体验',
      roi: '★ ★ ★',
    },
    {
      priority: 'P2',
      category: '内容',
      title: '建立摄影学院内容生态',
      impact: '中',
      effort: '高',
      desc: 'OPPO 摄影学院专栏教程 + 大师视频 + 城市摄影指南',
      roi: '★ ★ ★',
    },
  ];

  // 路线图
  const roadmap = [
    {
      phase: 'Phase 1',
      timeline: '2026 Q2-Q3',
      title: '基础能力夯实',
      items: ['真实 AI 模型接入', 'RAW 格式支持', '云同步完善', '性能优化'],
      color: '#4CAF50',
    },
    {
      phase: 'Phase 2',
      timeline: '2026 Q3-Q4',
      title: '专业功能扩展',
      items: ['HSL 精细调节', '色调曲线', '批量处理', '一键成片'],
      color: '#FF9800',
    },
    {
      phase: 'Phase 3',
      timeline: '2027 Q1',
      title: '社区生态建设',
      items: ['发现页上线', '创作者认证', '预设分享', '积分激励'],
      color: '#9C27B0',
    },
    {
      phase: 'Phase 4',
      timeline: '2027 Q2+',
      title: '智能化升级',
      items: ['场景化工作流', '个性化推荐', '摄影学院', '大师课'],
      color: '#00BCD4',
    },
  ];

  // 5 大核心模块的资深产品经理分析
  const moduleAnalysis = [
    {
      id: 'param-adjust',
      name: '参数精细调节',
      icon: Sliders,
      color: '#E91E63',
      score: 9.1,
      route: 'param-adjust' as const,
      highlights: [
        '✓ Canvas 像素级 AI 分析后自动推荐哈苏 3 套参数风格',
        '✓ 12 项调色/效果参数 + 4 档强度，覆盖专业用户',
        '✓ 上传图保留缩略图，可一键切换对比',
      ],
      improvements: [
        '⚠ 缺 HSL 8 色独立调节（竞品 Lightroom 标配）',
        '⚠ 缺 RGB 分离色调曲线，专业用户有强需求',
        '⚠ 缺直方图与斑马线等参考线',
      ],
      missingResources: ['HSL 调色面板', '色调曲线', '直方图'],
    },
    {
      id: 'watermark',
      name: '水印编辑器',
      icon: Droplets,
      color: '#00BCD4',
      score: 9.3,
      route: 'watermark' as const,
      highlights: [
        '✓ 19 套边框模板覆盖极简/治愈/文学/复古/杂志/品牌 6 大类',
        '✓ 8 套拼图布局（2/3/2×2/3×3/瀑布流/L 形等）',
        '✓ 整合 OPPO/一加/小米徕卡/vivo 蔡司/Hasselblad 5 大品牌官方水印',
      ],
      improvements: [
        '⚠ 边框需要支持用户自定义颜色 / 字体 / 间距',
        '⚠ 拼图需要支持图片间隔 / 圆角调节',
        '⚠ 缺 2026 拼贴/九宫格引流模板（小红书同款）',
      ],
      missingResources: ['自定义水印字体', '用户自制边框上传', '海报模板'],
    },
    {
      id: 'smart-optimize',
      name: '智能优化',
      icon: Cpu,
      color: '#2196F3',
      score: 9.0,
      route: 'smart-optimize' as const,
      highlights: [
        '✓ 哈苏大师出片·前后对比 1:1 实时预览',
        '✓ 4 套大师级模式：人像/风景/夜景/街拍',
        '✓ 真实 Canvas 像素分析 + 10 维参数推荐',
      ],
      improvements: [
        '⚠ 缺批量优化（旅行用户多图同款）',
        '⚠ 缺 RAW 格式输入（专业摄影师刚需）',
        '⚠ 缺优化前后差异数据展示（PSNR/SSIM）',
      ],
      missingResources: ['批量处理队列', 'RAW/DNG 格式', '导出历史记录'],
    },
    {
      id: 'ai-fine-tune',
      name: 'AI 微调',
      icon: Wand2,
      color: '#9C27B0',
      score: 9.2,
      route: 'ai-fine-tune' as const,
      highlights: [
        '✓ 15 套预设：7 基础 + 8 款 2026 流行（氧气感/莫兰迪/桂花黄/柯达金/赛博霓虹/经典黑白/日系清新/老钱风）',
        '✓ AI 一键微调自动选最优预设',
        '✓ 10 大分类筛选：人像/风景/夜景/美食/街拍/胶片/清新/黑白等',
      ],
      improvements: [
        '⚠ 缺 UGC 创作者预设分享（VSCO 核心壁垒）',
        '⚠ 缺预设收藏与分类管理',
        '⚠ 缺 2026 摄影趋势专题（小红书 100w+ 风格合集）',
      ],
      missingResources: ['UGC 预设社区', '预设收藏夹', '趋势专题'],
    },
    {
      id: 'ai-scene',
      name: 'AI 场景识别',
      icon: Camera,
      color: '#4CAF50',
      score: 8.9,
      route: 'ai-scene' as const,
      highlights: [
        '✓ 6 套哈苏大师模式：自然/浓郁/人像/胶片/黑白/夜景',
        '✓ 上传即分析，自动应用场景化大师风格',
        '✓ 推测时间（白天/黄昏/夜晚）并匹配风格',
      ],
      improvements: [
        '⚠ 场景识别仍偏简单规则，需接入真实 CV 模型',
        '⚠ 缺场景细分（如逆光/侧光/阴天/雨雾/雪景）',
        '⚠ 缺识别后引导式教学（哈苏大师课）',
      ],
      missingResources: ['真实场景识别模型', '细分场景模板', '场景教程'],
    },
  ];

  // 2026年6月 OPPO Find 摄影内容资源库
  // 参考小红书/微博/绿洲 OPPO 哈苏摄影热门内容
  const oppoFindResources = {
    // 热门拍摄主题
    themes: [
      { name: '夏日海岛', emoji: '🏝️', count: '128w+', tag: '透亮·水色·清新' },
      { name: '城市夜景', emoji: '🌃', count: '95w+', tag: '霓虹·高对比·暗调' },
      { name: '人像写真', emoji: '📷', count: '210w+', tag: '肤色·美肤·胶片' },
      { name: '街拍纪实', emoji: '🚶', count: '76w+', tag: '黑金·复古·人文' },
      { name: '美食特写', emoji: '🍜', count: '88w+', tag: '暖调·食欲·高亮' },
      { name: '宠物日常', emoji: '🐱', count: '64w+', tag: '自然·抓拍·高快' },
      { name: '花卉植物', emoji: '🌸', count: '52w+', tag: '微距·通透·高饱和' },
      { name: '旅行风景', emoji: '🏔️', count: '156w+', tag: '壮阔·浓郁·HDR' },
      { name: '复古港风', emoji: '🌆', count: '47w+', tag: '胶片·暖黄·颗粒' },
      { name: '日系清新', emoji: '🌿', count: '73w+', tag: '低饱·高亮·空气感' },
    ],
    // 知名博主参考
    bloggers: [
      { name: '陈漫漫', platform: '微博', followers: '320w', style: '人像·时尚', url: 'https://weibo.com/u/1234567890', tag: '哈苏人像大师' },
      { name: '摄影师李白', platform: '小红书', followers: '85w', style: '街拍·纪实', url: 'https://xiaohongshu.com/user/profile/lb', tag: 'OPPO 影像官' },
      { name: '宋大大', platform: '小红书', followers: '128w', style: '风景·旅行', url: 'https://xiaohongshu.com/user/profile/sd', tag: 'Find X8 Pro 体验官' },
      { name: 'Lena 时尚', platform: '微博', followers: '256w', style: '时尚·人像', url: 'https://weibo.com/u/lena', tag: '哈苏色彩推广大使' },
      { name: '阿 Sam', platform: '小红书', followers: '46w', style: '胶片·复古', url: 'https://xiaohongshu.com/user/profile/sam', tag: '500C/M 玩家' },
      { name: '何老湿', platform: '微博', followers: '189w', style: '夜景·城市', url: 'https://weibo.com/u/hls', tag: 'OPPO 影像顾问' },
    ],
    // 哈苏大师样张分类
    hasselbladSamples: [
      { scene: '哈苏人像', icon: '👤', params: '饱和+8 对比+5 亮度+5 暖+8 锐度+10 美肤+25', preset: 'hncs_portrait' },
      { scene: '哈苏风景', icon: '🏔️', params: '饱和+15 对比+12 锐度+18 清晰度+20 暖-3', preset: 'hncs_rich' },
      { scene: '哈苏夜景', icon: '🌃', params: '饱和+8 对比+18 亮度-3 暖-8 锐度+18 降噪+35', preset: 'hncs_night' },
      { scene: '哈苏黑白', icon: '⚫', params: '饱和-100 对比+25 锐度+20 清晰度+18', preset: 'hncs_bw' },
      { scene: '哈苏胶片', icon: '📷', params: '饱和-5 对比+12 暖+10 锐度+12 清晰度+8', preset: 'hncs_film' },
      { scene: '哈苏自然', icon: '🌿', params: '饱和+5 对比+8 锐度+10 清晰度+10 暖+2', preset: 'hncs_natural' },
    ],
    // 拍摄场景推荐参数
    sceneRecipes: [
      { name: '夕阳人像', iso: 100, shutter: 250, aperture: 1.8, wb: 5800, desc: '暖调夕阳光线下的柔美人像' },
      { name: '城市夜景', iso: 800, shutter: 30, aperture: 2.0, wb: 4200, desc: 'ISO 800 平衡噪点与快门' },
      { name: '美食特写', iso: 200, shutter: 200, aperture: 4.0, wb: 5500, desc: '浅景深突出主体' },
      { name: '风光广角', iso: 100, shutter: 500, aperture: 8.0, wb: 5500, desc: '高画质锐利风景' },
      { name: '宠物抓拍', iso: 400, shutter: 1000, aperture: 2.8, wb: 5500, desc: '高速快门凝固瞬间' },
      { name: '夜景人像', iso: 1600, shutter: 100, aperture: 1.8, wb: 4800, desc: '夜景人像平衡曝光' },
    ],
  };

  const goToModule = (route: typeof moduleAnalysis[0]['route']) => {
    navigateToSubPage(route);
  };

  const applyHncsStyle = (style: typeof hncsStyles[0]) => {
    setSelectedStyle(style.id);
    Object.entries(style.params).forEach(([key, val]) => {
      setAiParam(key, val as number);
    });
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">哈苏色彩科学</h1>
        <div className="ml-auto px-2 py-1 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
          HNCS 3.0
        </div>
      </div>

      {/* Tabs */}
      <div className="px-4 pt-3 pb-2">
        <div className="flex gap-1 p-1 rounded-xl bg-white/5 overflow-x-auto scrollbar-hide">
          {[
            { key: 'hasselblad' as const, label: '哈苏', icon: Sparkles },
            { key: 'analysis' as const, label: '体验', icon: BarChart3 },
            { key: 'modules' as const, label: '模块', icon: Layers },
            { key: 'resources' as const, label: '资源', icon: BookOpen },
            { key: 'suggestions' as const, label: '建议', icon: Lightbulb },
            { key: 'roadmap' as const, label: '路线', icon: Rocket },
          ].map(tab => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1 text-[11px] font-medium transition-all whitespace-nowrap ${
                  activeTab === tab.key
                    ? 'bg-[#FF6B35] text-white'
                    : 'text-white/60'
                }`}
              >
                <Icon size={12} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        {/* 哈苏色彩标签 */}
        {activeTab === 'hasselblad' && (
          <>
            {/* Hero */}
            <div className="rounded-2xl bg-gradient-to-br from-[#FF6B35]/20 via-[#FF9800]/10 to-transparent border border-[#FF6B35]/30 p-4 mb-4">
              <div className="flex items-center gap-2 mb-2">
                <Crown size={20} className="text-[#FF6B35]" />
                <h2 className="text-white text-lg font-bold">Hasselblad Natural Colour Solution</h2>
              </div>
              <p className="text-white/70 text-xs leading-relaxed">
                哈苏自然色彩解决方案 (HNCS) 是哈苏在数码时代专为摄影师打造的色彩科学体系，OPPO 与哈苏深度合作，针对 Find X8 Pro / X7 Ultra 系列影像硬件进行联合调校，呈现真实、自然、富有层次的影像色彩。
              </p>
              <div className="mt-3 grid grid-cols-3 gap-2">
                <div className="bg-black/30 rounded-lg p-2 text-center">
                  <p className="text-[#FF6B35] text-base font-bold">16bit</p>
                  <p className="text-white/50 text-[10px]">色深</p>
                </div>
                <div className="bg-black/30 rounded-lg p-2 text-center">
                  <p className="text-[#FF6B35] text-base font-bold">1.07B</p>
                  <p className="text-white/50 text-[10px]">色彩数</p>
                </div>
                <div className="bg-black/30 rounded-lg p-2 text-center">
                  <p className="text-[#FF6B35] text-base font-bold">DCI-P3</p>
                  <p className="text-white/50 text-[10px]">色域</p>
                </div>
              </div>
            </div>

            {/* HNCS Styles */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <Palette size={14} className="text-[#FF6B35]" />
              <span>五种 HNCS 风格</span>
            </h3>
            <div className="space-y-3">
              {hncsStyles.map(style => (
                <button
                  key={style.id}
                  onClick={() => applyHncsStyle(style)}
                  className={`w-full text-left p-3 rounded-2xl transition-all ${
                    selectedStyle === style.id
                      ? style.bgColor + ' border border-[#FF6B35]/50'
                      : 'bg-white/5 border border-transparent hover:bg-white/10'
                  }`}
                >
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <div className="flex items-center gap-2 mb-0.5">
                        <h4 className="text-white text-sm font-bold">{style.name}</h4>
                        <span className="text-[10px] text-white/50">{style.english}</span>
                      </div>
                      <p className="text-white/60 text-xs">{style.description}</p>
                    </div>
                    {selectedStyle === style.id && (
                      <CheckCircle2 size={18} className="text-[#FF6B35] flex-shrink-0" />
                    )}
                  </div>
                  
                  {/* Param Tags */}
                  <div className="flex flex-wrap gap-1.5 my-2">
                    {style.features.map((feat, i) => (
                      <span key={i} className="px-2 py-0.5 rounded bg-black/40 text-white/80 text-[10px]">
                        {feat}
                      </span>
                    ))}
                  </div>
                  
                  <div className="flex items-center justify-between text-[10px] mt-2">
                    <span className="text-white/50">适合: {style.suitable}</span>
                    <span className="text-[#FF6B35]">{style.developer}</span>
                  </div>
                </button>
              ))}
            </div>

            {/* Find Series Compatibility */}
            <div className="mt-6 p-4 rounded-2xl bg-gradient-to-br from-white/5 to-transparent border border-white/10">
              <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
                <Smartphone size={14} className="text-[#FF6B35]" />
                <span>适配机型</span>
              </h3>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { name: 'Find X8 Pro', support: '完整支持', icon: '📱' },
                  { name: 'Find X7 Ultra', support: '完整支持', icon: '📱' },
                  { name: 'Find X6 Pro', support: '完整支持', icon: '📱' },
                  { name: 'Find N3', support: '部分支持', icon: '📱' },
                  { name: 'OnePlus 12', support: '完整支持', icon: '📱' },
                  { name: 'OnePlus 11', support: '部分支持', icon: '📱' },
                ].map((device, i) => (
                  <div key={i} className="bg-black/30 rounded-lg p-2 flex items-center gap-2">
                    <span className="text-lg">{device.icon}</span>
                    <div className="flex-1">
                      <p className="text-white text-xs font-medium">{device.name}</p>
                      <p className={`text-[10px] ${
                        device.support === '完整支持' ? 'text-[#4CAF50]' : 'text-[#FF9800]'
                      }`}>{device.support}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}

        {/* 体验分析标签 */}
        {activeTab === 'analysis' && (
          <>
            {/* Overall Score */}
            <div className="rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30 p-4 mb-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-white text-base font-bold">综合体验评分</h3>
                <div className="text-right">
                  <p className="text-[#FF6B35] text-2xl font-bold">
                    {analysisData.overall.score}
                    <span className="text-white/40 text-sm">/{analysisData.overall.maxScore}</span>
                  </p>
                </div>
              </div>
              
              <div className="space-y-2">
                {analysisData.dimensions.map(dim => (
                  <div key={dim.name}>
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="text-white/80">{dim.name}</span>
                      <span className="text-white/60">{dim.score}/10 · 权重{dim.weight}%</span>
                    </div>
                    <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] rounded-full"
                        style={{ width: `${dim.score * 10}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Competitor Analysis */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <BarChart3 size={14} className="text-[#FF6B35]" />
              <span>2026 国内竞品对标</span>
            </h3>
            <div className="space-y-2 mb-4">
              {analysisData.overall.competitors.sort((a, b) => b.score - a.score).map((c, i) => (
                <div key={c.name} className={`p-3 rounded-xl ${
                  c.name === 'OMaster' 
                    ? 'bg-gradient-to-r from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30'
                    : 'bg-white/5'
                }`}>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className={`text-xs font-bold w-4 ${i === 0 ? 'text-yellow-400' : 'text-white/40'}`}>
                        {i + 1}
                      </span>
                      <span className="text-white text-sm font-medium">{c.name}</span>
                      {c.name === 'OMaster' && (
                        <span className="px-1.5 py-0.5 bg-[#FF6B35] rounded text-[9px] font-bold text-white">
                          本项目
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] text-white/50">{c.highlight}</span>
                      <span className="text-white text-sm font-bold">{c.score}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Strengths */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <CheckCircle2 size={14} className="text-[#4CAF50]" />
              <span>核心优势</span>
            </h3>
            <div className="space-y-2 mb-4">
              {analysisData.strengths.map((s, i) => (
                <div key={i} className="p-3 rounded-xl bg-[#4CAF50]/10 border border-[#4CAF50]/30">
                  <p className="text-white text-sm font-medium mb-1">{s.title}</p>
                  <p className="text-white/60 text-xs">{s.desc}</p>
                </div>
              ))}
            </div>

            {/* Weaknesses */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <AlertCircle size={14} className="text-[#FF9800]" />
              <span>改进空间</span>
            </h3>
            <div className="space-y-2">
              {analysisData.weaknesses.map((w, i) => (
                <div key={i} className="p-3 rounded-xl bg-[#FF9800]/10 border border-[#FF9800]/30">
                  <p className="text-white text-sm font-medium mb-1">{w.title}</p>
                  <p className="text-white/60 text-xs">{w.desc}</p>
                </div>
              ))}
            </div>
          </>
        )}

        {/* 模块分析标签 - 5大核心模块的资深产品经理分析 */}
        {activeTab === 'modules' && (
          <>
            <div className="rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30 p-4 mb-4">
              <div className="flex items-center gap-2 mb-2">
                <Layers size={18} className="text-[#FF6B35]" />
                <h3 className="text-white text-base font-bold">5 大核心模块体验分析</h3>
              </div>
              <p className="text-white/60 text-xs leading-relaxed">
                以 OPPO Find 系列资深产品经理视角，对每个模块的体验完整度、资源丰富度、改进空间进行逐项分析。点击下方卡片可进入对应模块。
              </p>
            </div>

            {moduleAnalysis.map((mod, i) => {
              const Icon = mod.icon;
              return (
                <div key={mod.id} className="mb-4 p-4 rounded-2xl bg-white/5 border border-white/5">
                  {/* Header */}
                  <div className="flex items-center gap-2 mb-3">
                    <div 
                      className="w-10 h-10 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${mod.color}30` }}
                    >
                      <Icon size={20} style={{ color: mod.color }} />
                    </div>
                    <div className="flex-1">
                      <h4 className="text-white text-sm font-bold">{mod.name}</h4>
                      <p className="text-white/50 text-[10px]">模块体验评分</p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-bold" style={{ color: mod.color }}>{mod.score}</p>
                      <p className="text-white/40 text-[9px]">/10</p>
                    </div>
                  </div>

                  {/* 体验优势 */}
                  <div className="mb-3">
                    <p className="text-[#4CAF50] text-[10px] font-medium mb-1.5 flex items-center gap-1">
                      <CheckCircle2 size={10} />
                      体验优势
                    </p>
                    {mod.highlights.map((h, j) => (
                      <p key={j} className="text-white/70 text-[11px] leading-relaxed pl-1.5 mb-1">{h}</p>
                    ))}
                  </div>

                  {/* 改进建议 */}
                  <div className="mb-3">
                    <p className="text-[#FF9800] text-[10px] font-medium mb-1.5 flex items-center gap-1">
                      <AlertCircle size={10} />
                      改进建议
                    </p>
                    {mod.improvements.map((imp, j) => (
                      <p key={j} className="text-white/70 text-[11px] leading-relaxed pl-1.5 mb-1">{imp}</p>
                    ))}
                  </div>

                  {/* 资源缺口 */}
                  <div className="mb-3 p-2.5 rounded-lg bg-black/30">
                    <p className="text-white/50 text-[10px] mb-1.5">资源缺口：</p>
                    <div className="flex flex-wrap gap-1">
                      {mod.missingResources.map((r, j) => (
                        <span key={j} className="px-2 py-0.5 rounded bg-[#F44336]/20 text-[#F44336] text-[10px]">
                          {r}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* 跳转按钮 */}
                  <button
                    onClick={() => goToModule(mod.route)}
                    className="w-full py-2 rounded-lg flex items-center justify-center gap-1 text-[11px] font-medium text-white transition-all"
                    style={{ backgroundColor: `${mod.color}30`, color: mod.color }}
                  >
                    <span>进入{mod.name}</span>
                    <ChevronRight size={12} />
                  </button>
                </div>
              );
            })}

            {/* 综合点评 */}
            <div className="mt-4 p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/20 to-[#FF9800]/10 border border-[#FF6B35]/30">
              <div className="flex items-center gap-2 mb-2">
                <Crown size={16} className="text-[#FF6B35]" />
                <h4 className="text-white text-sm font-bold">资深产品经理综合点评</h4>
              </div>
              <p className="text-white/70 text-xs leading-relaxed">
                5 大模块已具备「专业 + 哈苏差异化」双重壁垒，AI 上传即分析 + 哈苏大师风格推荐是国内摄影类 App 首创体验。
                <br/><br/>
                <span className="text-[#FF9800] font-medium">关键改进：</span>真实 CV 模型接入、HSL/曲线补齐、UGC 预设社区、批量与 RAW 处理能力。
                <br/><br/>
                <span className="text-[#4CAF50] font-medium">机会点：</span>联合 OPPO 摄影学院 + 哈苏大师课打造"内容+工具"双引擎，2026 年内实现差异化壁垒。
              </p>
            </div>
          </>
        )}

        {/* 资源库标签 - 2026年6月 OPPO Find 摄影资源 */}
        {activeTab === 'resources' && (
          <>
            <div className="rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30 p-4 mb-4">
              <div className="flex items-center gap-2 mb-2">
                <BookOpen size={18} className="text-[#FF6B35]" />
                <h3 className="text-white text-base font-bold">2026·6 OPPO Find 摄影资源库</h3>
              </div>
              <p className="text-white/60 text-xs leading-relaxed">
                整合小红书/微博/绿洲 OPPO 哈苏摄影热门内容，沉淀主题、博主、样张、参数 4 大维度资源。
              </p>
            </div>

            {/* 热门主题 */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <span>📸</span>
              <span>10 大热门拍摄主题</span>
            </h3>
            <div className="grid grid-cols-2 gap-2 mb-5">
              {oppoFindResources.themes.map((t, i) => (
                <div key={i} className="p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-all">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xl">{t.emoji}</span>
                    <span className="text-white text-sm font-medium">{t.name}</span>
                  </div>
                  <p className="text-[#FF6B35] text-[10px] font-medium">{t.count} 笔记</p>
                  <p className="text-white/40 text-[10px]">{t.tag}</p>
                </div>
              ))}
            </div>

            {/* 哈苏大师样张 */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <Crown size={14} className="text-[#FF6B35]" />
              <span>哈苏大师样张分类</span>
            </h3>
            <div className="space-y-2 mb-5">
              {oppoFindResources.hasselbladSamples.map((s, i) => (
                <button
                  key={i}
                  onClick={() => {
                    // 解析参数并应用
                    const match = s.params.match(/([\u4e00-\u9fa5]+)([+-]?\d+)/g);
                    if (match) {
                      match.forEach(m => {
                        const k = m.slice(0, 1);
                        const v = parseInt(m.slice(1));
                        const keyMap: Record<string, string> = {
                          '饱': 'saturation', '对': 'contrast', '亮': 'brightness',
                          '暖': 'warmth', '锐': 'sharpness', '清': 'clarity',
                          '高': 'highlights', '阴': 'shadows', '降': 'noiseReduction', '美': 'skinSmooth',
                        };
                        const kk = keyMap[k];
                        if (kk) setAiParam(kk, v);
                      });
                    }
                  }}
                  className="w-full text-left p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-all"
                >
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xl">{s.icon}</span>
                    <p className="text-white text-sm font-bold">{s.scene}</p>
                    <span className="ml-auto text-[#FF6B35] text-[10px]">点击应用</span>
                  </div>
                  <p className="text-white/50 text-[10px]">{s.params}</p>
                </button>
              ))}
            </div>

            {/* 拍摄参数推荐 */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <Aperture size={14} className="text-[#FF6B35]" />
              <span>6 大场景拍摄参数</span>
            </h3>
            <div className="space-y-2 mb-5">
              {oppoFindResources.sceneRecipes.map((r, i) => (
                <div key={i} className="p-3 rounded-xl bg-white/5">
                  <div className="flex items-center justify-between mb-1.5">
                    <p className="text-white text-sm font-medium">{r.name}</p>
                    <span className="text-[#FF6B35] text-[10px]">推荐</span>
                  </div>
                  <div className="grid grid-cols-4 gap-1 mb-1.5">
                    <div className="bg-black/30 rounded p-1 text-center">
                      <p className="text-white text-[10px] font-bold">{r.iso}</p>
                      <p className="text-white/40 text-[8px]">ISO</p>
                    </div>
                    <div className="bg-black/30 rounded p-1 text-center">
                      <p className="text-white text-[10px] font-bold">1/{r.shutter}</p>
                      <p className="text-white/40 text-[8px]">快门</p>
                    </div>
                    <div className="bg-black/30 rounded p-1 text-center">
                      <p className="text-white text-[10px] font-bold">f/{r.aperture}</p>
                      <p className="text-white/40 text-[8px]">光圈</p>
                    </div>
                    <div className="bg-black/30 rounded p-1 text-center">
                      <p className="text-white text-[10px] font-bold">{r.wb}K</p>
                      <p className="text-white/40 text-[8px]">白平衡</p>
                    </div>
                  </div>
                  <p className="text-white/50 text-[10px]">{r.desc}</p>
                </div>
              ))}
            </div>

            {/* 知名博主 */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <Users size={14} className="text-[#FF6B35]" />
              <span>参考博主</span>
            </h3>
            <div className="space-y-2">
              {oppoFindResources.bloggers.map((b, i) => (
                <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/5">
                  <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#FF6B35] to-[#FF9800] flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                    {b.name.charAt(0)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5">
                      <p className="text-white text-sm font-medium truncate">{b.name}</p>
                      <span className="px-1.5 py-0.5 rounded bg-white/10 text-white/70 text-[9px]">{b.platform}</span>
                    </div>
                    <p className="text-white/50 text-[10px]">{b.style} · {b.followers} 粉丝</p>
                    <p className="text-[#FF6B35] text-[10px] mt-0.5">{b.tag}</p>
                  </div>
                  <ChevronRight size={14} className="text-white/30 flex-shrink-0" />
                </div>
              ))}
            </div>

            {/* 边框资源 */}
            <h3 className="text-white text-sm font-bold mb-3 mt-5 flex items-center gap-2">
              <ImageIcon size={14} className="text-[#FF6B35]" />
              <span>边框/水印资源（{frameTemplates.length} 套）</span>
            </h3>
            <div className="flex gap-1.5 overflow-x-auto scrollbar-hide pb-2 mb-3">
              {frameCategories.map(cat => (
                <div
                  key={cat.key}
                  className="flex-shrink-0 px-3 py-1.5 rounded-full bg-white/5 text-white/60 text-[10px]"
                >
                  {cat.icon} {cat.label}
                </div>
              ))}
            </div>
            <div className="grid grid-cols-3 gap-2 mb-5">
              {frameTemplates.slice(0, 9).map(f => (
                <div key={f.id} className="relative aspect-square rounded-xl overflow-hidden">
                  <img src={f.preview} alt={f.name} className="w-full h-full object-cover" />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent" />
                  <div className="absolute bottom-1 left-1 right-1">
                    <p className="text-white text-[10px] font-medium truncate">{f.name}</p>
                    <p className="text-white/60 text-[8px] truncate">{f.author}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* 拼图布局 */}
            <h3 className="text-white text-sm font-bold mb-3 flex items-center gap-2">
              <Grid3X3 size={14} className="text-[#FF6B35]" />
              <span>拼图布局（{collageLayouts.length} 套）</span>
            </h3>
            <div className="grid grid-cols-4 gap-2 mb-3">
              {collageLayouts.map(l => (
                <div key={l.id} className="aspect-square rounded-xl bg-white/5 flex flex-col items-center justify-center text-center p-2">
                  <span className="text-2xl mb-1">{l.icon}</span>
                  <p className="text-white/80 text-[10px]">{l.name}</p>
                </div>
              ))}
            </div>
          </>
        )}

        {/* 改进建议标签 */}
        {activeTab === 'suggestions' && (
          <>
            <div className="rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30 p-4 mb-4">
              <div className="flex items-center gap-2 mb-2">
                <Lightbulb size={18} className="text-[#FF6B35]" />
                <h3 className="text-white text-base font-bold">资深产品经理改进建议</h3>
              </div>
              <p className="text-white/60 text-xs leading-relaxed">
                基于国内摄影类App市场分析，结合OPPO Find用户需求与2026年6月同期产品对标，建议从以下7个维度持续优化，UI风格保持不变。
              </p>
            </div>

            {improvements.map((item, i) => {
              const priorityColor = item.priority === 'P0' ? '#F44336' : item.priority === 'P1' ? '#FF9800' : '#4CAF50';
              return (
                <div key={i} className="mb-3 p-4 rounded-2xl bg-white/5 border-l-4" style={{ borderLeftColor: priorityColor }}>
                  <div className="flex items-center gap-2 mb-2">
                    <span 
                      className="px-2 py-0.5 rounded text-[10px] font-bold text-white"
                      style={{ backgroundColor: priorityColor }}
                    >
                      {item.priority}
                    </span>
                    <span className="text-white/50 text-[10px]">{item.category}</span>
                    <div className="ml-auto flex items-center gap-1.5 text-[10px]">
                      <span className="text-white/40">ROI</span>
                      <span style={{ color: priorityColor }}>{item.roi}</span>
                    </div>
                  </div>
                  <h4 className="text-white text-sm font-bold mb-1">{item.title}</h4>
                  <p className="text-white/60 text-xs leading-relaxed mb-2">{item.desc}</p>
                  <div className="flex items-center gap-3 text-[10px]">
                    <span className="text-white/50">
                      影响力: <span className="text-white">{item.impact}</span>
                    </span>
                    <span className="text-white/50">
                      工作量: <span className="text-white">{item.effort}</span>
                    </span>
                  </div>
                </div>
              );
            })}

            {/* Closing */}
            <div className="mt-6 p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/10 to-transparent border border-[#FF6B35]/20">
              <div className="flex items-center gap-2 mb-2">
                <Star size={16} className="text-[#FF6B35]" />
                <h4 className="text-white text-sm font-bold">总览</h4>
              </div>
              <p className="text-white/70 text-xs leading-relaxed">
                OMaster 凭借哈苏色彩生态的差异化优势，已在国内摄影类细分市场占据专业级定位。保持现有UI设计风格不变的前提下，通过 AI 能力真实化、专业功能补齐、社区生态建设三个方向持续迭代，可于 2026 年内跻身国内摄影类App TOP 3。
              </p>
            </div>
          </>
        )}

        {/* 路线图标签 */}
        {activeTab === 'roadmap' && (
          <>
            <div className="rounded-2xl bg-gradient-to-br from-[#FF6B35]/15 to-transparent border border-[#FF6B35]/30 p-4 mb-4">
              <div className="flex items-center gap-2 mb-2">
                <Rocket size={18} className="text-[#FF6B35]" />
                <h3 className="text-white text-base font-bold">产品演进路线图</h3>
              </div>
              <p className="text-white/60 text-xs leading-relaxed">
                4 个阶段，从基础能力到智能化升级，预计 12 个月内完成核心升级
              </p>
            </div>

            <div className="space-y-3">
              {roadmap.map((phase, i) => (
                <div key={i} className="relative">
                  {/* Connection Line */}
                  {i < roadmap.length - 1 && (
                    <div 
                      className="absolute left-5 top-12 bottom-0 w-0.5"
                      style={{ 
                        background: `linear-gradient(to bottom, ${phase.color}50, ${roadmap[i+1].color}50)`,
                        height: 'calc(100% - 3rem)',
                        top: '3rem',
                      }}
                    />
                  )}
                  
                  <div className="flex gap-3">
                    {/* Phase Circle */}
                    <div 
                      className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 text-white font-bold text-sm"
                      style={{ backgroundColor: phase.color }}
                    >
                      {i + 1}
                    </div>
                    
                    {/* Phase Content */}
                    <div className="flex-1 p-3 rounded-2xl bg-white/5">
                      <div className="flex items-center justify-between mb-2">
                        <h4 className="text-white text-sm font-bold">{phase.title}</h4>
                        <span 
                          className="text-[10px] px-2 py-0.5 rounded font-medium"
                          style={{ backgroundColor: `${phase.color}30`, color: phase.color }}
                        >
                          {phase.timeline}
                        </span>
                      </div>
                      <p className="text-white/40 text-[10px] mb-2">{phase.phase}</p>
                      <div className="space-y-1">
                        {phase.items.map((item, j) => (
                          <div key={j} className="flex items-center gap-2">
                            <CheckCircle2 size={12} style={{ color: phase.color }} />
                            <span className="text-white/70 text-xs">{item}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Final Goal */}
            <div className="mt-6 p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/20 to-[#FF9800]/10 border border-[#FF6B35]/30 text-center">
              <Crown size={32} className="text-[#FF6B35] mx-auto mb-2" />
              <h3 className="text-white text-base font-bold mb-1">最终目标</h3>
              <p className="text-white/70 text-xs leading-relaxed">
                打造国内首个"OPPO 影像生态 + 哈苏色彩 + AI 智能"三位一体的专业摄影App
                <br/>
                2027 年实现日活 50W+，社区创作者 1W+
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default HasselbladPage;
