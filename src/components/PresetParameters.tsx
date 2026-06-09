import React from 'react';

/**
 * 预设参数区块接口
 */
interface PresetParamItem {
  label: string;
  value: string;
  span: 1 | 2; // 1=半宽, 2=全宽
}

interface PresetSection {
  title: string;
  icon?: string;
  items: PresetParamItem[];
}

interface PresetParametersProps {
  sections: PresetSection[];
}

/**
 * 预设参数展示组件
 * 支持动态sections展示，对齐Android端的sections体系
 */
const PresetParameters: React.FC<PresetParametersProps> = ({ sections }) => {
  return (
    <div className="space-y-4">
      {sections.map((section, si) => (
        <div key={si}>
          {/* 区块标题 */}
          <h3 className="text-white/60 text-xs font-medium mb-2 flex items-center gap-2">
            <div className="w-1 h-3 rounded-full bg-[#FF6B35]" />
            {section.icon && <span>{section.icon}</span>}
            {section.title}
          </h3>

          {/* 参数网格 */}
          <div className="grid grid-cols-2 gap-2">
            {section.items.map((item, ii) => {
              // 处理span布局
              const colSpanClass = item.span === 2 ? 'col-span-2' : '';

              return (
                <div
                  key={ii}
                  className={`${colSpanClass} bg-white/5 rounded-xl p-3 flex justify-between items-center hover:bg-white/10 transition-colors`}
                >
                  <span className="text-white/50 text-xs">{item.label}</span>
                  <span className="text-[#FF6B35] text-sm font-bold">{item.value}</span>
                </div>
              );
            })}
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
 * 预设拍摄建议卡片组件
 */
interface ShootingTipsCardProps {
  tips: {
    environment?: string;
    scenes?: string;
    points?: string;
  };
}

export const ShootingTipsCard: React.FC<ShootingTipsCardProps> = ({ tips }) => {
  return (
    <div className="bg-white/5 rounded-xl p-4">
      <h3 className="text-white/70 text-sm font-medium mb-3 flex items-center gap-2">
        <span className="text-lg">📷</span>
        拍摄建议
      </h3>
      <div className="space-y-2">
        {tips.environment && (
          <div className="flex items-start gap-2">
            <span className="text-[#FF6B35] text-xs font-medium shrink-0">【环境建议】</span>
            <span className="text-white/60 text-xs">{tips.environment}</span>
          </div>
        )}
        {tips.scenes && (
          <div className="flex items-start gap-2">
            <span className="text-[#FF6B35] text-xs font-medium shrink-0">【场景推荐】</span>
            <span className="text-white/60 text-xs">{tips.scenes}</span>
          </div>
        )}
        {tips.points && (
          <div className="flex items-start gap-2">
            <span className="text-[#FF6B35] text-xs font-medium shrink-0">【拍摄要点】</span>
            <span className="text-white/60 text-xs">{tips.points}</span>
          </div>
        )}
      </div>
    </div>
  );
};

/**
 * 关联推荐组件
 */
interface RelatedPresetsProps {
  presets: Array<{
    id: string;
    name: string;
    coverPath: string;
  }>;
  onSelect: (id: string) => void;
}

export const RelatedPresets: React.FC<RelatedPresetsProps> = ({
  presets,
  onSelect,
}) => {
  if (presets.length === 0) return null;

  return (
    <div className="bg-white/5 rounded-xl p-4">
      <h3 className="text-white/70 text-sm font-medium mb-3 flex items-center gap-2">
        <span className="text-lg">🎞️</span>
        关联推荐
      </h3>
      <p className="text-white/40 text-xs mb-3">看了这个的人也看了：</p>
      <div className="flex gap-2 overflow-x-auto scrollbar-hide">
        {presets.map((preset) => (
          <div
            key={preset.id}
            onClick={() => onSelect(preset.id)}
            className="shrink-0 w-20 cursor-pointer hover:opacity-80 transition-opacity"
          >
            <div className="aspect-square rounded-lg overflow-hidden bg-[#1a1a1a]">
              <img
                src={preset.coverPath}
                alt={preset.name}
                className="w-full h-full object-cover"
                loading="lazy"
              />
            </div>
            <p className="text-white/60 text-[10px] mt-1 truncate">{preset.name}</p>
          </div>
        ))}
      </div>
    </div>
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