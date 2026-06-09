import React from 'react';
import { Film } from 'lucide-react';
import { FilmPreset, FilmSeries } from '../store/sceneProfile';

/**
 * Layer 3: 大师呈现层 - 胶片推荐卡片组件
 * 
 * 设计规范：
 * - 横向滚动，仿胶片齿孔边框
 * - 选中状态：哈苏橙边框 + 背景
 * - 显示匹配度百分比
 * - 胶片系列名称
 */

interface FilmRecommendationStripProps {
  films: FilmPreset[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}

const FilmSeriesNames: Record<FilmSeries, string> = {
  [FilmSeries.CLASSIC]: '原生经典',
  [FilmSeries.EMOTION]: '情绪与表达',
  [FilmSeries.STRUCTURE]: '结构与时间',
  [FilmSeries.DIGITAL]: '数字记忆',
};

export const FilmRecommendationStrip: React.FC<FilmRecommendationStripProps> = ({
  films,
  selectedId,
  onSelect,
}) => {
  return (
    <div>
      {/* 标题 */}
      <div className="flex items-center gap-2 mb-3">
        <Film size={14} className="text-[#FF6B35]" />
        <span className="text-white/60 text-xs font-medium">推荐胶片风格</span>
      </div>

      {/* 胶片卡片列表 */}
      <div className="flex gap-2.5 overflow-x-auto pb-2 scrollbar-hide">
        {films.map((film) => (
          <button
            key={film.id}
            onClick={() => onSelect(film.id)}
            className={`flex-shrink-0 w-[88px] rounded-xl p-2.5 text-center
                        transition-all duration-200
                        ${selectedId === film.id
                  ? 'bg-[#FF6B35]/20 border border-[#FF6B35]/60 shadow-[0_0_12px_rgba(255,107,53,0.2)]'
                  : 'bg-white/5 border border-white/5 hover:border-white/20 hover:bg-white/8'
                }`}
          >
            {/* 胶片齿孔装饰 */}
            <div className="flex justify-between mb-2 px-0.5">
              {[...Array(5)].map((_, i) => (
                <div
                  key={i}
                  className={`w-1.5 h-2 rounded-full ${selectedId === film.id ? 'bg-[#FF6B35]/40' : 'bg-white/20'
                    }`}
                />
              ))}
            </div>

            {/* 胶片名称 */}
            <div className="text-white text-xs font-medium truncate leading-tight">
              {film.name}
            </div>

            {/* 胶片系列 */}
            <div className="text-white/40 text-[10px] mt-1 truncate">
              {FilmSeriesNames[film.series]}
            </div>

            {/* 匹配度 */}
            <div className="mt-2">
              <span className={`text-[10px] font-semibold ${selectedId === film.id ? 'text-[#FF6B35]' : 'text-white/60'
                }`}>
                {Math.round(film.matchScore * 100)}% 匹配
              </span>
            </div>

            {/* 选中指示器 */}
            {selectedId === film.id && (
              <div className="mt-1.5 flex justify-center">
                <div className="w-1.5 h-1.5 rounded-full bg-[#FF6B35]" />
              </div>
            )}
          </button>
        ))}
      </div>
    </div>
  );
};

export default FilmRecommendationStrip;
