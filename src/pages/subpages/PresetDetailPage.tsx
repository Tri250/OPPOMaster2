import React, { useState, useEffect, useCallback, useRef } from 'react';
import { ArrowLeft, Expand, Edit2, Heart, Crown, Zap, Star, Download, MessageCircle, Camera, Sun, Mountain, User, Building, Settings, Share2, ChevronRight, RefreshCw } from 'lucide-react';
import { useFadeInUp, animationKeyframes } from '../../hooks/useAnimations';

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
  shootingTips: {
    environment: string;
    scenes: string[];
    tips: string;
  };
  proParams: {
    iso: number;
    shutter: string;
    exposure: number;
    colorTemp: number;
    softLight: string;
  };
  colorParams: {
    filter: string;
    saturation: number;
    contrast: number;
    sharpness: number;
    vignette: boolean;
  };
  relatedPresets: RelatedPreset[];
  reviewList: UserReview[];
}

interface RelatedPreset {
  id: string;
  name: string;
  cover: string;
}

interface UserReview {
  id: string;
  user: string;
  avatar: string;
  rating: number;
  content: string;
}

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
  shootingTips: {
    environment: '日间户外或充足自然光',
    scenes: ['街拍', '人像', '风景', '建筑'],
    tips: '适合追求经典胶片质感，建议配合哈苏大师模式使用，可获得更自然的肤色表现和层次丰富的影调。',
  },
  proParams: {
    iso: 100,
    shutter: '1/125',
    exposure: 0.3,
    colorTemp: 5500,
    softLight: '梦幻柔光',
  },
  colorParams: {
    filter: '复古100%',
    saturation: 19,
    contrast: 10,
    sharpness: 25,
    vignette: true,
  },
  relatedPresets: [
    { id: 'r1', name: '胶片人像', cover: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200&h=250&fit=crop' },
    { id: 'r2', name: '日系清新', cover: 'https://images.unsplash.com/photo-1488426862026-c5e5a0a0a8e1?w=200&h=250&fit=crop' },
    { id: 'r3', name: '复古胶片', cover: 'https://images.unsplash.com/photo-1507003211169-0a70dd7d80ad?w=200&h=250&fit=crop' },
    { id: 'r4', name: '电影色调', cover: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=250&fit=crop' },
  ],
  reviewList: [
    { id: 'rev1', user: '摄影爱好者', avatar: 'https://images.unsplash.com/photo-1507003211169-0a70dd7d80ad?w=100&h=100&fit=crop', rating: 5, content: '非常好用的预设！色彩还原很准确，配合哈苏大师模式效果更佳。' },
    { id: 'rev2', user: '专业摄影师', avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop', rating: 5, content: '配合哈苏大师模式使用效果绝佳，肤色表现自然，影调层次丰富。' },
  ],
};

// 图片轮播组件
const ImageCarousel: React.FC<{ images: string[] }> = ({ images }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isAutoPlaying, setIsAutoPlaying] = useState(true);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (isAutoPlaying) {
      timerRef.current = setInterval(() => {
        setCurrentIndex((prev) => (prev + 1) % images.length);
      }, 3000);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isAutoPlaying, images.length]);

  return (
    <div className="relative w-full aspect-[4/3] rounded-xl overflow-hidden bg-[#1a1a1a]">
      {/* 图片 */}
      {images.map((img, idx) => (
        <div
          key={idx}
          className={`absolute inset-0 transition-opacity duration-500 ${
            idx === currentIndex ? 'opacity-100' : 'opacity-0'
          }`}
        >
          <img src={img} alt={`样张${idx + 1}`} className="w-full h-full object-cover" />
        </div>
      ))}
      
      {/* 指示器 */}
      <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-2">
        {images.map((_, idx) => (
          <button
            key={idx}
            onClick={() => {
              setCurrentIndex(idx);
              setIsAutoPlaying(false);
            }}
            className={`w-2 h-2 rounded-full transition-all ${
              idx === currentIndex ? 'bg-[#FF6B35] w-6' : 'bg-white/40'
            }`}
          />
        ))}
      </div>
      
      {/* 图片标签 */}
      <div className="absolute top-3 left-3 px-2 py-1 bg-black/50 backdrop-blur-sm rounded-lg text-xs text-white/80">
        {currentIndex === 0 ? '封面' : `样张${currentIndex}`}
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

// 拍摄建议卡片
const ShootingTipsCard: React.FC<{ tips: PresetDetail['shootingTips'] }> = ({ tips }) => {
  const { style } = useFadeInUp(200, 400);
  
  return (
    <div style={style} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
      <div className="flex items-center gap-2 mb-3">
        <Camera size={18} className="text-[#FF6B35]" />
        <h3 className="text-sm font-semibold text-white">拍摄建议</h3>
      </div>
      
      {/* 环境建议 */}
      <div className="mb-3">
        <div className="flex items-center gap-1.5 mb-1">
          <Sun size={14} className="text-yellow-400" />
          <span className="text-xs text-white/60">环境建议</span>
        </div>
        <p className="text-sm text-white/80 pl-5">{tips.environment}</p>
      </div>
      
      {/* 场景推荐 */}
      <div className="mb-3">
        <div className="flex items-center gap-1.5 mb-1">
          <Mountain size={14} className="text-green-400" />
          <span className="text-xs text-white/60">场景推荐</span>
        </div>
        <div className="flex gap-2 pl-5">
          {tips.scenes.map((scene) => (
            <span key={scene} className="px-2 py-1 bg-[#FF6B35]/10 rounded text-xs text-[#FF6B35]">
              {scene}
            </span>
          ))}
        </div>
      </div>
      
      {/* 拍摄要点 */}
      <div>
        <div className="flex items-center gap-1.5 mb-1">
          <User size={14} className="text-blue-400" />
          <span className="text-xs text-white/60">拍摄要点</span>
        </div>
        <p className="text-sm text-white/80 pl-5">{tips.tips}</p>
      </div>
    </div>
  );
};

// 专业参数卡片
const ProParamsCard: React.FC<{ params: PresetDetail['proParams'] }> = ({ params }) => {
  const { style } = useFadeInUp(300, 400);
  
  const paramItems = [
    { label: 'ISO', value: params.iso.toString() },
    { label: '快门', value: params.shutter },
    { label: '曝光补偿', value: `+${params.exposure}` },
    { label: '色温', value: `${params.colorTemp}K` },
  ];
  
  return (
    <div style={style} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
      <div className="flex items-center gap-2 mb-3">
        <Settings size={18} className="text-[#FF6B35]" />
        <h3 className="text-sm font-semibold text-white">专业参数 (Pro模式)</h3>
      </div>
      
      {/* 参数网格 */}
      <div className="grid grid-cols-2 gap-2 mb-3">
        {paramItems.map((item) => (
          <div key={item.label} className="bg-[#0a0a0a] rounded-lg p-3">
            <div className="text-xs text-white/40 mb-1">{item.label}</div>
            <div className="text-sm font-semibold text-white">{item.value}</div>
          </div>
        ))}
      </div>
      
      {/* 柔光模式 */}
      <div className="bg-[#0a0a0a] rounded-lg p-3">
        <div className="text-xs text-white/40 mb-1">柔光</div>
        <div className="text-sm font-semibold text-[#FF6B35]">{params.softLight}</div>
      </div>
    </div>
  );
};

// 调色参数卡片
const ColorParamsCard: React.FC<{ params: PresetDetail['colorParams'] }> = ({ params }) => {
  const { style } = useFadeInUp(400, 400);
  
  const paramItems = [
    { label: '滤镜', value: params.filter },
    { label: '饱和度', value: `+${params.saturation}` },
    { label: '对比度', value: `+${params.contrast}` },
    { label: '锐度', value: `+${params.sharpness}` },
  ];
  
  return (
    <div style={style} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
      <div className="flex items-center gap-2 mb-3">
        <div className="w-4 h-4 rounded-full bg-gradient-to-r from-[#FF6B35] to-purple-500" />
        <h3 className="text-sm font-semibold text-white">调色参数</h3>
      </div>
      
      {/* 参数网格 */}
      <div className="grid grid-cols-2 gap-2 mb-3">
        {paramItems.map((item) => (
          <div key={item.label} className="bg-[#0a0a0a] rounded-lg p-3">
            <div className="text-xs text-white/40 mb-1">{item.label}</div>
            <div className="text-sm font-semibold text-white">{item.value}</div>
          </div>
        ))}
      </div>
      
      {/* 暗角 */}
      <div className="bg-[#0a0a0a] rounded-lg p-3">
        <div className="text-xs text-white/40 mb-1">暗角</div>
        <div className="text-sm font-semibold text-[#FF6B35]">{params.vignette ? '开' : '关'}</div>
      </div>
    </div>
  );
};

// 关联推荐卡片
const RelatedPresetsCard: React.FC<{ presets: RelatedPreset[] }> = ({ presets }) => {
  const { style } = useFadeInUp(500, 400);
  
  return (
    <div style={style} className="bg-[#1a1a1a] rounded-xl p-4 border border-white/5">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className="text-sm">🎞️</div>
          <h3 className="text-sm font-semibold text-white">关联推荐</h3>
        </div>
        <span className="text-xs text-white/40">看了这个的人也看了：</span>
      </div>
      
      {/* 预设列表 */}
      <div className="grid grid-cols-4 gap-2">
        {presets.map((preset) => (
          <div key={preset.id} className="relative aspect-[3/4] rounded-lg overflow-hidden bg-[#0a0a0a] cursor-pointer hover:scale-105 transition-transform">
            <img src={preset.cover} alt={preset.name} className="w-full h-full object-cover" />
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
            <div className="absolute bottom-1 left-1 right-1 text-xs text-white truncate">{preset.name}</div>
          </div>
        ))}
      </div>
    </div>
  );
};

// 用户评价卡片
const ReviewsCard: React.FC<{ reviews: UserReview[] }> = ({ reviews }) => {
  const { style } = useFadeInUp(600, 400);
  
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
        className={`flex items-center gap-2 px-4 py-2 rounded-full transition-all ${
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
        className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-white font-semibold"
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
  const [showFullReviews, setShowFullReviews] = useState(false);
  
  const handleToggleFavorite = useCallback(() => {
    setIsFavorite((prev) => !prev);
  }, []);
  
  const handleApply = useCallback(() => {
    // 应用预设逻辑
    console.log('Apply preset:', preset.id);
  }, [preset.id]);
  
  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* 顶栏 */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/5">
        <button className="p-2 rounded-full hover:bg-white/10 transition-colors">
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
        {/* 图片轮播 */}
        <div className="mt-4 mb-4">
          <ImageCarousel images={preset.coverImages} />
        </div>
        
        {/* 预设信息 */}
        <div className="mb-4">
          <PresetInfoCard preset={preset} />
        </div>
        
        {/* 拍摄建议 */}
        <div className="mb-4">
          <ShootingTipsCard tips={preset.shootingTips} />
        </div>
        
        {/* 专业参数 */}
        <div className="mb-4">
          <ProParamsCard params={preset.proParams} />
        </div>
        
        {/* 调色参数 */}
        <div className="mb-4">
          <ColorParamsCard params={preset.colorParams} />
        </div>
        
        {/* 关联推荐 */}
        <div className="mb-4">
          <RelatedPresetsCard presets={preset.relatedPresets} />
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