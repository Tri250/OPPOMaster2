import React, { useState } from 'react';
import { Camera, Sparkles, CheckCircle, Heart } from 'lucide-react';

/**
 * 预设参数区块接口（对齐Android端sections体系）
 */
interface PresetParamItem {
  label: string;
  value: string;
  span: 1 | 2; // 1=半宽, 2=全宽
}

interface PresetSection {
  title: string;
  items: PresetParamItem[];
}

interface PresetParametersProps {
  sections: PresetSection[];
}

/**
 * 预设参数展示组件（对齐用户规范）
 * 支持动态sections展示，对齐Android端的sections体系
 */
const PresetParameters: React.FC<PresetParametersProps> = ({ sections }) => {
  return (
    <div className="space-y-4">
      {sections.map((section, si) => (
        <div key={si}>
          {/* 区块标题（对齐用户规范样式） */}
          <h3 className="text-white/60 text-xs font-medium mb-2 flex items-center gap-2">
            <div className="w-1 h-3 rounded-full bg-[#FF6B35]" />
            {section.title}
          </h3>

          {/* 参数网格（grid grid-cols-2） */}
          <div className="grid grid-cols-2 gap-2">
            {section.items.map((item, ii) => (
              <div
                key={ii}
                className={`${item.span === 2 ? 'col-span-2' : ''} 
                            bg-white/5 rounded-xl p-3 flex justify-between items-center
                            hover:bg-white/10 transition-colors`}
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

/**
 * 预设统计卡片组件
 */
interface PresetStatsProps {
  downloads: number;
  rating: number;
  ratingCount: number;
}

export const PresetStats: React.FC<PresetStatsProps> = ({
  downloads,
  rating,
  ratingCount,
}) => {
  return (
    <div className="grid grid-cols-3 gap-3 p-3 bg-white/5 rounded-xl">
      <div className="text-center">
        <p className="text-lg font-bold text-white">
          {downloads >= 1000 ? `${(downloads / 1000).toFixed(1)}k` : downloads}
        </p>
        <p className="text-[10px] text-white/40">下载</p>
      </div>
      <div className="text-center">
        <p className="text-lg font-bold text-white">{rating.toFixed(1)}</p>
        <p className="text-[10px] text-white/40">评分</p>
      </div>
      <div className="text-center">
        <p className="text-lg font-bold text-white">{ratingCount}</p>
        <p className="text-[10px] text-white/40">评价</p>
      </div>
    </div>
  );
};

/**
 * 解析结构化内容
 * 【环境建议】...【场景推荐】...【拍摄要点】...
 */
interface ParsedTip {
  label: string;
  content: string;
}

const parseStructuredContent = (content: string): ParsedTip[] => {
  const tips: ParsedTip[] = [];
  const regex = /【([^】]+)】([^【]*)/g;
  let match;
  
  while ((match = regex.exec(content)) !== null) {
    tips.push({
      label: match[1],
      content: match[2].trim()
    });
  }
  
  // 如果没有匹配到结构化内容，返回默认格式
  if (tips.length === 0) {
    tips.push({ label: '拍摄建议', content: content });
  }
  
  return tips;
};

/**
 * 拍摄建议卡片（对齐用户规范：渐变背景+结构化解析）
 */
interface PresetDescription {
  title: string;
  content: string;
}

interface ShootingTipsCardProps {
  description: PresetDescription;
}

export const ShootingTipsCard: React.FC<ShootingTipsCardProps> = ({ description }) => {
  // 解析结构化文本
  const tips = parseStructuredContent(description.content);

  return (
    <div className="bg-gradient-to-br from-[#1a1a2e] to-[#0f0f1a] rounded-2xl p-4 border border-white/5">
      {/* 标题 */}
      <div className="flex items-center gap-2 mb-3">
        <Camera size={16} className="text-[#FF6B35]" />
        <h3 className="text-white font-medium text-sm">{description.title}</h3>
      </div>

      {/* 结构化内容 */}
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

/**
 * 关联推荐组件（基于标签相似度推荐）
 */
interface RelatedPreset {
  id: string;
  name: string;
  coverPath: string;
  author?: string;
  tags?: string[];
}

interface RelatedPresetsProps {
  currentId: string;
  tags: string[];
  allPresets: RelatedPreset[];
  onNavigate: (id: string) => void;
}

export const RelatedPresets: React.FC<RelatedPresetsProps> = ({
  currentId,
  tags,
  allPresets,
  onNavigate,
}) => {
  // 基于标签相似度推荐（对齐用户规范）
  const related = allPresets
    .filter(p => p.id !== currentId)
    .map(p => ({
      ...p,
      score: p.tags?.filter(t => tags.includes(t)).length || 0
    }))
    .filter(p => p.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 4);

  if (related.length === 0) return null;

  return (
    <div>
      <h3 className="text-white/60 text-xs font-medium mb-2">🎞️ 看了这个的人也看了</h3>
      <div className="flex gap-2 overflow-x-auto scrollbar-hide">
        {related.map((preset) => (
          <button
            key={preset.id}
            onClick={() => onNavigate(preset.id)}
            className="flex-shrink-0 w-24 rounded-xl overflow-hidden bg-white/5 
                       hover:bg-white/10 transition-all"
          >
            <img
              src={preset.coverPath}
              alt={preset.name}
              className="w-24 h-24 object-cover"
              loading="lazy"
            />
            <div className="p-2">
              <p className="text-white text-xs truncate">{preset.name}</p>
              {preset.author && (
                <p className="text-white/40 text-[10px]">{preset.author}</p>
              )}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
};

/**
 * 一键应用动画反馈按钮（对齐用户规范）
 */
interface ApplyPresetButtonProps {
  onApply: () => void;
}

export const ApplyPresetButton: React.FC<ApplyPresetButtonProps> = ({ onApply }) => {
  const [applied, setApplied] = useState(false);

  const handleApply = () => {
    setApplied(true);
    onApply();
    setTimeout(() => setApplied(false), 2000);
  };

  return (
    <button
      onClick={handleApply}
      className={`flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-all duration-300 ${
        applied
          ? 'bg-green-500 text-white'
          : 'bg-[#FF6B35] text-white hover:bg-[#FF8C42]'
      }`}
    >
      {applied ? (
        <>
          <CheckCircle size={16} />
          已应用哈苏配方
        </>
      ) : (
        <>
          <Sparkles size={16} />
          一键应用哈苏配方
        </>
      )}
    </button>
  );
};

/**
 * 收藏按钮组件
 */
interface FavoriteButtonProps {
  isFavorite: boolean;
  onToggle: () => void;
}

export const FavoriteButton: React.FC<FavoriteButtonProps> = ({
  isFavorite,
  onToggle,
}) => {
  return (
    <button
      onClick={onToggle}
      className={`flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-all ${
        isFavorite
          ? 'bg-red-500/20 text-red-400 border border-red-500/30'
          : 'bg-white/5 text-white/80 border border-white/10 hover:bg-white/10'
      }`}
    >
      <Heart size={16} className={isFavorite ? 'fill-current' : ''} />
      {isFavorite ? '已收藏' : '收藏'}
    </button>
  );
};

/**
 * 用户评价组件
 */
interface Comment {
  id: string;
  user: string;
  avatar?: string;
  content: string;
  rating: number;
  timestamp?: string;
}

interface UserCommentsProps {
  comments: Comment[];
  onViewAll?: () => void;
}

export const UserComments: React.FC<UserCommentsProps> = ({
  comments,
  onViewAll,
}) => {
  if (comments.length === 0) return null;

  return (
    <div className="bg-white/5 rounded-xl p-4">
      <h3 className="text-white/70 text-sm font-medium mb-3 flex items-center gap-2">
        <span className="text-lg">💬</span>
        用户评价
      </h3>
      <div className="space-y-3">
        {comments.slice(0, 2).map((comment) => (
          <div key={comment.id} className="border-b border-white/5 pb-3 last:border-0 last:pb-0">
            <div className="flex items-center gap-2 mb-1">
              {/* 星级 */}
              <div className="flex">
                {[1, 2, 3, 4, 5].map((star) => (
                  <span
                    key={star}
                    className={`text-xs ${
                      star <= comment.rating ? 'text-yellow-400' : 'text-white/20'
                    }`}
                  >
                    ★
                  </span>
                ))}
              </div>
              <span className="text-white text-xs font-medium">{comment.user}</span>
            </div>
            <p className="text-white/60 text-xs">{comment.content}</p>
          </div>
        ))}
      </div>
      {comments.length > 2 && onViewAll && (
        <button
          onClick={onViewAll}
          className="mt-3 text-[#FF6B35] text-xs font-medium hover:opacity-80"
        >
          查看全部评价
        </button>
      )}
    </div>
  );
};

export default PresetParameters;