import React, { useState, useEffect, useCallback, useRef } from 'react';
import { ArrowLeft, Expand, Edit2, Heart, Crown, Zap, Star, Download, MessageCircle, Camera, Sun, Mountain, User, Building, Settings, Share2, ChevronRight, ChevronLeft, RefreshCw, Aperture } from 'lucide-react';
import { useFadeInUp, animationKeyframes } from '../../hooks/useAnimations';

// 参数Section数据模型
interface PresetSection {
  title: string;
  items: PresetParamItem[];
}

interface PresetParamItem {
  label: string;
  value: string;
  span: 1 | 2; // 1=半宽, 2=全宽
}

// 预设详情数据模型
interface PresetDetail {
  id: string;
  name: string;
  author: string;
  brand: string;
  tags: string[];
  isHncs: boolean;
  isPro: boolean;
  downloads: number;
  rating: number;
  reviewCount: number;
  coverImages: string[];
  description: {
    title: string;
    content: string; // 结构化文本：【环境建议】...【场景推荐】...【拍摄要点】...
  };
  sections: PresetSection[];
  relatedPresets: RelatedPreset[];
  reviewList: UserReview[];
}

interface RelatedPreset {
  id: string;
  name: string;
  cover: string;
  similarity?: number; // 标签相似度
}

interface UserReview {
  id: string;
  user: string;
  avatar: string;
  rating: number;
  content: string;
}

// 解析结构化文本
const parseStructuredContent = (content: string): { label: string; content: string }[] => {
  const tips: { label: string; content: string }[] = [];
  
  // 匹配【xxx】模式
  const regex = /【([^】]+)】([^【]*)/g;
  let match;
  
  while ((match = regex.exec(content)) !== null) {
    tips.push({
      label: match[1],
      content: match[2].trim(),
    });
  }
  
  // 如果没有匹配到结构化格式，返回原始内容
  if (tips.length === 0) {
    tips.push({ label: '说明', content: content });
  }
  
  return tips;
};

// 模拟预设详情数据
const mockPresetDetail: PresetDetail = {
  id: 'preset-001',
  name: '清新人像',
  author: 'OPPO影像',
  brand: 'OPPO',
  tags: ['人像', '清新', '哈苏', '专业'],
  isHncs: true,
  isPro: true,
  downloads: 12500,
  rating: 4.9,
  reviewCount: 856,
  coverImages: [
    'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800&h=600&fit=crop',
    'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&h=600&fit=crop',
  ],
  description: {
    title: '拍摄建议',
    content: '【环境建议】日间户外或充足自然光，避免强逆光场景。【场景推荐】街拍、人像、风景、建筑等日常拍摄场景。【拍摄要点】适合追求经典胶片质感，建议配合哈苏大师模式使用，可获得更自然的肤色表现和层次丰富的影调。',
  },
  sections: [
    {
      title: '🎨 调色参数',
      items: [
        { label: '滤镜', value: '复古100%', span: 1 },
        { label: '饱和度', value: '+19', span: 1 },
        { label: '对比度', value: '+10', span: 1 },
        { label: '锐度', value: '+25', span: 1 },
        { label: '暗角', value: '开', span: 2 },
      ],
    },
    {
      title: '🎞️ 胶片特性',
      items: [
        { label: '胶片类型', value: 'Portra 400', span: 1 },
        { label: '颗粒感', value: '轻微', span: 1 },
        { label: '色调倾向', value: '暖调偏移', span: 2 },
      ],
    },
  ],
  relatedPresets: [
    { id: 'r1', name: '胶片人像', cover: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200&h=250&fit=crop', similarity: 85 },
    { id: 'r2', name: '日系清新', cover: 'https://images.unsplash.com/photo-1488426862026-c5e5a0a0a8e1?w=200&h=250&fit=crop', similarity: 72 },
    { id: 'r3', name: '复古胶片', cover: 'https://images.unsplash.com/photo-1507003211169-0a70dd7d80ad?w=200&h=250&fit=crop', similarity: 68 },
    { id: 'r4', name: '电影色调', cover: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=250&fit=crop', similarity: 55 },
  ],
  reviewList: [
    { id: 'rev1', user: '摄影爱好者', avatar: 'https://images.unsplash.com/photo-1507003211169-0a70dd7d80ad?w=100&h=100&fit=crop', rating: 5, content: '非常好用的预设！色彩还原很准确，配合哈苏大师模式效果更佳。' },
    { id: 'rev2', user: '专业摄影师', avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop', rating: 5, content: '配合哈苏大师模式使用效果绝佳，肤色表现自然，影调层次丰富。' },
  ],
};

// A. 图片画廊组件 - 对齐Android端
const PresetImageGallery: React.FC<{ images: string[]; isPro: boolean }> = ({ images, isPro }) => {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isAutoPlaying, setIsAutoPlaying] = useState(true);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  
  const allImages = images.length > 0 ? images : [images[0] || 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=800&h=600&fit=crop'];
  
  useEffect(() => {
    if (allImages.length <= 1 || !isAutoPlaying) return;
    timerRef.current = setInterval(() => {
      setActiveIndex(prev => (prev + 1) % allImages.length);
    }, 3000);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [allImages.length, isAutoPlaying]);
  
  const prev = useCallback(() => {
    setIsAutoPlaying(false);
    setActiveIndex(prev => (prev - 1 + allImages.length) % allImages.length);
  }, [allImages.length]);
  
  const next = useCallback(() => {
    setIsAutoPlaying(false);
    setActiveIndex(prev => (prev + 1) % allImages.length);
  }, [allImages.length]);
  
  return (
    <div className="relative aspect-[4/3] rounded-2xl overflow-hidden bg-[#1a1a1a]">
      {/* 当前图片 */}
      {allImages.map((img, idx) => (
        <div
          key={idx}
          className={`absolute inset-0 transition-opacity duration-500 ${
            idx === activeIndex ? 'opacity-100 z-10' : 'opacity-0 z-0'
          }`}
        >
          <img src={img} alt={`样张${idx + 1}`} className="w-full h-full object-cover" />
        </div>
      ))}
      
      {/* 左右切换箭头 */}
      {allImages.length > 1 && (
        <>
          <button
            onClick={prev}
            className="absolute left-2 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center hover:bg-black/70 transition-colors"
          >
            <ChevronLeft size={16} className="text-white" />
          </button>
          <button
            onClick={next}
            className="absolute right-2 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full bg-black/50 backdrop-blur-sm flex items-center justify-center hover:bg-black/70 transition-colors"
          >
            <ChevronRight size={16} className="text-white" />
          </button>
        </>
      )}
      
      {/* 指示器 */}
      <div className="absolute bottom-3 left-1/2 -translate-x-1/2 z-20 flex gap-1.5">
        {allImages.map((_, i) => (
          <button
            key={i}
            onClick={() => {
              setActiveIndex(i);
              setIsAutoPlaying(false);
            }}
            className={`h-1.5 rounded-full transition-all ${
              i === activeIndex ? 'bg-[#FF6B35] w-4' : 'bg-white/40 w-1.5'
            }`}
          />
        ))}
      </div>
      
      {/* 模式标签 */}
      <div className="absolute top-3 left-3 z-20">
        <span className="px-2 py-1 rounded-md bg-black/50 backdrop-blur-sm text-white text-[10px] font-medium flex items-center gap-1">
          {isPro ? (
            <>
              <Zap size={12} className="text-[#FF6B35]" />
              <span>PRO</span>
            </>
          ) : (
            <>
              <Aperture size={12} className="text-white/60" />
              <span>AUTO</span>
            </>
          )}
        </span>
      </div>
      
      {/* 图片计数 */}
      <div className="absolute top-3 right-3 z-20 px-2 py-1 rounded-md bg-black/50 backdrop-blur-sm text-white text-[10px]">
        {activeIndex + 1}/{allImages.length}
      </div>
    </div>
  );
};

// 预设信息卡片
const PresetInfoCard: React.FC<{ preset: PresetDetail }> = ({ preset }) => {
  const { style } = useFadeInUp(100, 400);
  
  return (
    <div style={style} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
      {/* 标题和徽章 */}
      <div className="flex items-center gap-2 mb-2">
        <h2 className="text-lg font-bold text-white">{preset.name}</h2>
        {preset.isHncs && (
          <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[10px] font-bold text-white">
            <Crown size={12} />
            <span>HNCS</span>
          </div>
        )}
        {preset.isPro && (
          <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-[#FF6B35]/20 text-[10px] font-bold text-[#FF6B35]">
            <Zap size={12} />
            <span>PRO</span>
          </div>
        )}
      </div>
      
      {/* 作者 */}
      <p className="text-white/60 text-sm mb-2">@{preset.author}</p>
      
      {/* 标签 */}
      <div className="flex gap-2 mb-4">
        {preset.tags.map((tag) => (
          <span key={tag} className="px-2 py-1 bg-white/5 rounded-full text-xs text-white/50">
            #{tag}
          </span>
        ))}
      </div>
      
      {/* 统计数据 */}
      <div className="grid grid-cols-3 gap-3">
        <div className="bg-[#0a0a0a] rounded-lg p-3 text-center">
          <div className="text-lg font-bold text-white">{(preset.downloads / 1000).toFixed(1)}k</div>
          <div className="text-xs text-white/40">下载</div>
        </div>
        <div className="bg-[#0a0a0a] rounded-lg p-3 text-center">
          <div className="text-lg font-bold text-[#FF6B35]">{preset.rating}</div>
          <div className="text-xs text-white/40">评分</div>
        </div>
        <div className="bg-[#0a0a0a] rounded-lg p-3 text-center">
          <div className="text-lg font-bold text-white">{preset.reviewCount}</div>
          <div className="text-xs text-white/40">评价</div>
        </div>
      </div>
    </div>
  );
};

// C. 拍摄建议卡片 - 解析结构化文本
const ShootingTipsCard: React.FC<{ description: PresetDetail['description'] }> = ({ description }) => {
  const { style } = useFadeInUp(200, 400);
  const tips = parseStructuredContent(description.content);
  
  return (
    <div style={style} className="bg-gradient-to-br from-[#1a1a2e] to-[#0f0f1a] rounded-2xl p-4 border border-white/5">
      <div className="flex items-center gap-2 mb-3">
        <Camera size={16} className="text-[#FF6B35]" />
        <h3 className="text-white font-medium text-sm">{description.title}</h3>
      </div>
      
      <div className="space-y-3">
        {tips.map((tip, i) => (
          <div key={i} className="flex gap-2">
            <div className="w-1.5 h-1.5 rounded-full bg-[#FF6B35] mt-1.5 flex-shrink-0" />
            <div>
              <span className="text-[#FF6B35] text-xs font-medium">{tip.label}</span>
              <p className="text-white/60 text-xs mt-0.5">{tip.content}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// B. 完整参数展示组件 - sections体系
const PresetParameters: React.FC<{ sections: PresetSection[] }> = ({ sections }) => {
  const { style } = useFadeInUp(300, 400);
  
  return (
    <div style={style} className="space-y-4">
      {sections.map((section, si) => (
        <div key={si} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
          <h3 className="text-white/60 text-xs font-medium mb-3 flex items-center gap-2">
            <div className="w-1 h-3 rounded-full bg-[#FF6B35]" />
            {section.title}
          </h3>
          <div className="grid grid-cols-2 gap-2">
            {section.items.map((item, ii) => (
              <div
                key={ii}
                className={`${item.span === 2 ? 'col-span-2' : ''} bg-white/5 rounded-xl p-3 flex justify-between items-center`}
              >
                <span className="text-white/50 text-xs">{item.label}</span>
                <span className="text-[#FF6B35] text-sm font-bold">{item.value}</span>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

// D. 关联推荐卡片 - 基于标签相似度
const RelatedPresetsCard: React.FC<{ presets: RelatedPreset[]; currentTags: string[] }> = ({ presets, currentTags }) => {
  const { style } = useFadeInUp(400, 400);
  
  // 计算标签相似度（如果预设没有similarity则计算）
  const presetsWithSimilarity = presets.map(p => ({
    ...p,
    similarity: p.similarity || Math.floor(Math.random() * 30 + 50), // 模拟相似度
  })).sort((a, b) => b.similarity - a.similarity);
  
  return (
    <div style={style} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className="text-sm">🎞️</div>
          <h3 className="text-sm font-semibold text-white">关联推荐</h3>
        </div>
        <span className="text-xs text-white/40">基于标签相似度</span>
      </div>
      
      {/* 预设列表 */}
      <div className="grid grid-cols-4 gap-2">
        {presetsWithSimilarity.map((preset) => (
          <div
            key={preset.id}
            className="relative aspect-[3/4] rounded-lg overflow-hidden bg-[#0a0a0a] cursor-pointer hover:scale-105 transition-transform group"
          >
            <img src={preset.cover} alt={preset.name} className="w-full h-full object-cover" />
            <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
            
            {/* 相似度标签 */}
            <div className="absolute top-1 right-1 px-1.5 py-0.5 bg-[#FF6B35]/80 rounded text-[8px] text-white font-medium">
              {preset.similarity}%
            </div>
            
            <div className="absolute bottom-1 left-1 right-1 text-xs text-white truncate">{preset.name}</div>
          </div>
        ))}
      </div>
      
      {/* 当前标签提示 */}
      <div className="mt-3 flex items-center gap-2">
        <span className="text-xs text-white/40">当前标签：</span>
        <div className="flex gap-1">
          {currentTags.slice(0, 3).map((tag) => (
            <span key={tag} className="px-1.5 py-0.5 bg-[#FF6B35]/10 rounded text-[10px] text-[#FF6B35]">
              #{tag}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
};

// 用户评价卡片
const ReviewsCard: React.FC<{ reviews: UserReview[] }> = ({ reviews }) => {
  const { style } = useFadeInUp(500, 400);
  
  return (
    <div style={style} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <MessageCircle size={18} className="text-[#FF6B35]" />
          <h3 className="text-sm font-semibold text-white">用户评价</h3>
        </div>
        <button className="flex items-center gap-1 text-xs text-[#FF6B35]">
          <span>查看全部</span>
          <ChevronRight size={14} />
        </button>
      </div>
      
      {/* 评价列表 */}
      <div className="space-y-3">
        {reviews.map((review) => (
          <div key={review.id} className="bg-[#0a0a0a] rounded-lg p-3">
            {/* 用户信息 */}
            <div className="flex items-center gap-2 mb-2">
              <img src={review.avatar} alt={review.user} className="w-8 h-8 rounded-full object-cover" />
              <div className="flex-1">
                <div className="text-sm text-white">{review.user}</div>
                <div className="flex items-center gap-0.5">
                  {[1, 2, 3, 4, 5].map((i) => (
                    <Star key={i} size={10} className={i <= review.rating ? 'text-yellow-400 fill-yellow-400' : 'text-white/20'} />
                  ))}
                </div>
              </div>
            </div>
            {/* 评价内容 */}
            <p className="text-sm text-white/70">{review.content}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

// 底部操作栏
const BottomActionBar: React.FC<{
  isFavorite: boolean;
  onToggleFavorite: () => void;
  onApply: () => void;
}> = ({ isFavorite, onToggleFavorite, onApply }) => {
  const [isHeartAnimating, setIsHeartAnimating] = useState(false);
  const [showFlash, setShowFlash] = useState(false);
  
  const handleFavorite = () => {
    if (!isFavorite) {
      setIsHeartAnimating(true);
      setShowFlash(true);
      setTimeout(() => setIsHeartAnimating(false), 300);
      setTimeout(() => setShowFlash(false), 200);
    }
    onToggleFavorite();
  };
  
  return (
    <div className="absolute bottom-0 left-0 right-0 h-16 bg-[#0a0a0a] border-t border-white/5 flex items-center justify-between px-4 z-30">
      {/* 收藏按钮 */}
      <button
        onClick={handleFavorite}
        className={`relative flex items-center gap-2 px-4 py-2 rounded-full transition-all ${
          isFavorite ? 'bg-red-500/20 text-red-400' : 'bg-white/5 text-white/60'
        }`}
        style={{
          transform: isHeartAnimating ? 'scale(1.1)' : 'scale(1)',
          transition: 'transform 150ms ease-out',
        }}
      >
        <Heart size={18} className={isFavorite ? 'fill-red-400' : ''} />
        <span className="text-sm">收藏</span>
        {showFlash && (
          <div className="absolute inset-0 rounded-full bg-[#FF6B35]/20 animate-ping" />
        )}
      </button>
      
      {/* 一键应用按钮 */}
      <button
        onClick={onApply}
        className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-white font-semibold shadow-lg shadow-[#FF6B35]/20"
      >
        <Zap size={18} />
        <span className="text-sm">一键应用哈苏配方</span>
      </button>
    </div>
  );
};

// 预设详情页主组件
const PresetDetailPage: React.FC<{ presetId?: string }> = ({ presetId }) => {
  const preset = mockPresetDetail; // 实际应根据presetId获取
  const [isFavorite, setIsFavorite] = useState(false);
  
  const handleToggleFavorite = useCallback(() => {
    setIsFavorite((prev) => !prev);
  }, []);
  
  const handleApply = useCallback(() => {
    // 应用预设逻辑
    console.log('Apply preset:', preset.id);
  }, [preset.id]);
  
  const handleBack = useCallback(() => {
    // 返回逻辑
    console.log('Go back');
  }, []);
  
  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* 顶栏 */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
        <button onClick={handleBack} className="p-2 rounded-full hover:bg-white/10 transition-colors">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-base font-semibold text-white">预设详情</h1>
        <div className="flex items-center gap-2">
          <button className="p-2 rounded-full hover:bg-white/10 transition-colors">
            <Expand size={18} className="text-white/60" />
          </button>
          <button className="p-2 rounded-full hover:bg-white/10 transition-colors">
            <Edit2 size={18} className="text-white/60" />
          </button>
          <button className="p-2 rounded-full hover:bg-white/10 transition-colors">
            <Share2 size={18} className="text-white/60" />
          </button>
        </div>
      </div>
      
      {/* 内容区域 */}
      <div className="flex-1 overflow-y-auto px-4 pb-20 scrollbar-hide">
        {/* A. 图片画廊 */}
        <div className="mt-4 mb-4">
          <PresetImageGallery images={preset.coverImages} isPro={preset.isPro} />
        </div>
        
        {/* 预设信息 */}
        <div className="mb-4">
          <PresetInfoCard preset={preset} />
        </div>
        
        {/* C. 拍摄建议 */}
        <div className="mb-4">
          <ShootingTipsCard description={preset.description} />
        </div>
        
        {/* B. 完整参数展示 */}
        <div className="mb-4">
          <PresetParameters sections={preset.sections} />
        </div>
        
        {/* D. 关联推荐 */}
        <div className="mb-4">
          <RelatedPresetsCard presets={preset.relatedPresets} currentTags={preset.tags} />
        </div>
        
        {/* 用户评价 */}
        <div className="mb-4">
          <ReviewsCard reviews={preset.reviewList} />
        </div>
      </div>
      
      {/* 底部操作栏 */}
      <BottomActionBar
        isFavorite={isFavorite}
        onToggleFavorite={handleToggleFavorite}
        onApply={handleApply}
      />
      
      {/* 动画样式 */}
      <style>{`
        ${animationKeyframes}
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default PresetDetailPage;