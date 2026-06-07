import React, { useState } from 'react';
import { useAppStore } from '../store/appStore';
import { 
  ArrowLeft, Sparkles, Award, Brain, Camera, Palette, Layers, 
  TrendingUp, Users, Star, CheckCircle2, AlertCircle, Lightbulb,
  Target, Rocket, Heart, Cloud, Zap, Crown, BarChart3, Eye,
  Smartphone, Globe, Shield, Gauge
} from 'lucide-react';

type TabType = 'hasselblad' | 'analysis' | 'suggestions' | 'roadmap';

const HasselbladPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
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
        <div className="flex gap-1 p-1 rounded-xl bg-white/5">
          {[
            { key: 'hasselblad' as const, label: '哈苏', icon: Sparkles },
            { key: 'analysis' as const, label: '体验分析', icon: BarChart3 },
            { key: 'suggestions' as const, label: '改进建议', icon: Lightbulb },
            { key: 'roadmap' as const, label: '路线图', icon: Rocket },
          ].map(tab => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex-1 py-2 rounded-lg flex items-center justify-center gap-1 text-[11px] font-medium transition-all ${
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
