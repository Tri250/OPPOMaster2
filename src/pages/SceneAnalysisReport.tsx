import React, { useState, useEffect, useCallback } from 'react';
import { ArrowLeft, Share2, Download, Eye, Camera, Image as ImageIcon, Film, TrendingUp, Calendar, MapPin } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { SceneProfile } from '../store/sceneProfile';

/**
 * Layer 4: 大师洞察层 - 场景分析报告
 *
 * 完整设计规范：
 * - 主色调：#FF6B35（哈苏橙）
 * - 背景：#0A0A0A（纯黑）
 * - 数据可视化卡片
 * - 场景分布图表
 * - 拍摄习惯分析
 * - 胶片使用排行
 * - 大师建议
 * 
 * 已修复：
 * - 使用真实数据统计替代硬编码模拟数据
 * - 实现导出、分享、查看功能
 */

interface SceneStats {
  sceneId: string;
  sceneName: string;
  count: number;
  percentage: number;
  trend: 'up' | 'down' | 'stable';
}

interface FilmUsage {
  filmId: string;
  filmName: string;
  count: number;
  percentage: number;
}

interface ShootingHabit {
  totalPhotos: number;
  totalRecipes: number;
  favoriteScene: string;
  favoriteFilm: string;
  avgConfidence: number;
  streakDays: number;
  lastShootDate: string;
}

interface LocationData {
  location: string;
  count: number;
  coordinates?: { lat: number; lng: number };
}

export const SceneAnalysisReport: React.FC = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(true);
  const [timeRange, setTimeRange] = useState<'week' | 'month' | 'year' | 'all'>('month');
  
  // 真实统计数据
  const [sceneStats, setSceneStats] = useState<SceneStats[]>([]);
  const [filmUsage, setFilmUsage] = useState<FilmUsage[]>([]);
  const [habits, setHabits] = useState<ShootingHabit | null>(null);
  const [locations, setLocations] = useState<LocationData[]>([]);
  const [masterTips, setMasterTips] = useState<string[]>([]);

  // 加载真实数据
  useEffect(() => {
    loadRealData();
  }, [timeRange]);

  const loadRealData = () => {
    setIsLoading(true);
    
    try {
      // 从本地存储获取保存的配方
      const savedRecipes = JSON.parse(localStorage.getItem('hasselblad_recipes') || '[]');
      const sceneHistory = JSON.parse(localStorage.getItem('hasselblad_scene_history') || '[]');
      
      // 计算场景统计
      const sceneCounts: Record<string, { name: string; count: number }> = {};
      savedRecipes.forEach((recipe: any) => {
        if (!sceneCounts[recipe.sceneId]) {
          sceneCounts[recipe.sceneId] = { name: recipe.sceneName, count: 0 };
        }
        sceneCounts[recipe.sceneId].count++;
      });
      
      const totalScenes = Object.values(sceneCounts).reduce((sum, s) => sum + s.count, 0);
      const sortedScenes = Object.entries(sceneCounts)
        .map(([id, data]) => ({
          sceneId: id,
          sceneName: data.name,
          count: data.count,
          percentage: totalScenes > 0 ? (data.count / totalScenes) * 100 : 0,
          trend: 'stable' as const,
        }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 5);
      
      setSceneStats(sortedScenes);
      
      // 计算胶片使用统计
      const filmCounts: Record<string, { name: string; count: number }> = {};
      savedRecipes.forEach((recipe: any) => {
        const filmId = recipe.filmId || 'unknown';
        const filmName = getFilmNameById(filmId);
        if (!filmCounts[filmId]) {
          filmCounts[filmId] = { name: filmName, count: 0 };
        }
        filmCounts[filmId].count++;
      });
      
      const totalFilms = Object.values(filmCounts).reduce((sum, f) => sum + f.count, 0);
      const sortedFilms = Object.entries(filmCounts)
        .map(([id, data]) => ({
          filmId: id,
          filmName: data.name,
          count: data.count,
          percentage: totalFilms > 0 ? (data.count / totalFilms) * 100 : 0,
        }))
        .sort((a, b) => b.count - a.count)
        .slice(0, 5);
      
      setFilmUsage(sortedFilms);
      
      // 计算拍摄习惯
      const totalPhotos = savedRecipes.length;
      const uniqueScenes = new Set(savedRecipes.map((r: any) => r.sceneId)).size;
      const avgConf = savedRecipes.length > 0
        ? savedRecipes.reduce((sum: number, r: any) => sum + (r.confidence || 0.85), 0) / savedRecipes.length
        : 0;
      
      // 计算连续拍摄天数
      const dates = savedRecipes.map((r: any) => new Date(r.timestamp).toDateString());
      const uniqueDates = [...new Set(dates)];
      const sortedDates = uniqueDates.map(d => new Date(d)).sort((a, b) => b.getTime() - a.getTime());
      
      let streakDays = 0;
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      for (let i = 0; i < sortedDates.length; i++) {
        const date = sortedDates[i];
        date.setHours(0, 0, 0, 0);
        const diffDays = Math.floor((today.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
        if (diffDays === i) {
          streakDays++;
        } else {
          break;
        }
      }
      
      setHabits({
        totalPhotos,
        totalRecipes: uniqueScenes,
        favoriteScene: sortedScenes[0]?.sceneName || '暂无数据',
        favoriteFilm: sortedFilms[0]?.filmName || '暂无数据',
        avgConfidence: avgConf,
        streakDays,
        lastShootDate: sortedDates[0]?.toLocaleDateString('zh-CN') || '从未',
      });
      
      // 生成大师建议
      generateMasterTips(sortedScenes, sortedFilms, totalPhotos);
      
    } catch (error) {
      console.error('加载数据失败:', error);
    }
    
    setIsLoading(false);
  };

  const getFilmNameById = (filmId: string): string => {
    const filmNames: Record<string, string> = {
      'cc': 'CC 经典负片',
      'nc': 'NC 经典负片',
      'nh': 'NH 经典负片',
      'portra': 'Portra 400',
      'rdp3': 'RDP3 反转片',
      '800t': 'CineStill 800T',
      'tx400': 'TX 400 黑白',
      'ccd-cool': '冷调 CCD',
      'ccd-warm': '暖调 CCD',
    };
    return filmNames[filmId] || filmId;
  };

  const generateMasterTips = (scenes: SceneStats[], films: FilmUsage[], total: number) => {
    const tips: string[] = [];
    
    if (total === 0) {
      tips.push('开始拍摄你的第一张照片，哈苏大师将为你提供个性化建议。');
    } else {
      if (scenes.length > 0 && scenes[0].percentage > 40) {
        tips.push(`你在${scenes[0].sceneName}上投入了大量精力，建议尝试其他场景类型以拓展创作视野。`);
      }
      
      if (films.length > 0) {
        tips.push(`${films[0].filmName}是你的最爱，它的色彩特性非常适合你的拍摄风格。`);
      }
      
      if (total < 10) {
        tips.push('你的拍摄量还有提升空间，建议每周至少拍摄3-5张照片来培养摄影眼。');
      } else if (total > 50) {
        tips.push('你已经积累了相当丰富的拍摄经验，可以考虑尝试更高级的创作技巧。');
      }
      
      tips.push('黄金时刻（日出后/日落前1小时）是拍摄风景和人像的最佳时机。');
      tips.push('使用哈苏的HNCS自然色彩解决方案，可以获得更真实的色彩还原。');
    }
    
    setMasterTips(tips);
  };

  const handleBack = () => navigate(-1);

  /**
   * 导出报告功能
   */
  const handleExport = useCallback(() => {
    if (!habits) return;

    const reportData = {
      generatedAt: new Date().toISOString(),
      timeRange,
      summary: habits,
      sceneStats,
      filmUsage,
      masterTips,
    };

    const blob = new Blob([JSON.stringify(reportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `hasselblad_report_${Date.now()}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }, [habits, timeRange, sceneStats, filmUsage, masterTips]);

  /**
   * 分享报告功能
   */
  const handleShare = useCallback(async () => {
    if (!habits) return;

    const shareText = `📊 我的哈苏之眼拍摄报告\n\n` +
      `📷 总照片数：${habits.totalPhotos}\n` +
      `🎞️ 最爱胶片：${habits.favoriteFilm}\n` +
      `🏞️ 最爱场景：${habits.favoriteScene}\n` +
      `🔥 连续拍摄：${habits.streakDays}天\n\n` +
      `用哈苏之眼，记录每一刻的光影。`;

    try {
      if (navigator.share) {
        await navigator.share({
          title: '我的哈苏之眼拍摄报告',
          text: shareText,
        });
      } else {
        await navigator.clipboard.writeText(shareText);
        alert('报告已复制到剪贴板！');
      }
    } catch (error) {
      console.error('分享失败:', error);
    }
  }, [habits]);

  /**
   * 查看详细数据
   */
  const handleViewDetails = useCallback(() => {
    navigate('/preset-manager');
  }, [navigate]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#0A0A0A] flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 rounded-full border-2 border-[#FF6B35]/20 border-t-[#FF6B35] animate-spin mx-auto" />
          <p className="mt-4 text-white/60 text-sm">正在分析你的拍摄数据...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0A0A0A] pb-20">
      {/* 顶部导航栏 */}
      <header className="sticky top-0 z-50 bg-[#0A0A0A]/90 backdrop-blur-md border-b border-white/5">
        <div className="flex items-center justify-between px-4 h-14">
          <button
            onClick={handleBack}
            className="flex items-center gap-1 text-white/80 hover:text-white transition-colors"
          >
            <ArrowLeft size={20} />
            <span className="text-sm">返回</span>
          </button>
          <h1 className="text-white font-semibold">拍摄分析报告</h1>
          <div className="flex items-center gap-2">
            <button
              onClick={handleExport}
              className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
              title="导出报告"
            >
              <Download size={18} className="text-white/60" />
            </button>
            <button
              onClick={handleShare}
              className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
              title="分享报告"
            >
              <Share2 size={18} className="text-[#FF6B35]" />
            </button>
          </div>
        </div>
      </header>

      <div className="px-4 py-4 space-y-4">
        {/* 时间范围选择 */}
        <div className="flex gap-2">
          {(['week', 'month', 'year', 'all'] as const).map((range) => (
            <button
              key={range}
              onClick={() => setTimeRange(range)}
              className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                timeRange === range
                  ? 'bg-[#FF6B35] text-white'
                  : 'bg-white/5 text-white/60 hover:bg-white/10'
              }`}
            >
              {range === 'week' && '本周'}
              {range === 'month' && '本月'}
              {range === 'year' && '本年'}
              {range === 'all' && '全部'}
            </button>
          ))}
        </div>

        {/* 概览卡片 */}
        {habits && (
          <section className="grid grid-cols-2 gap-3">
            <div className="bg-white/5 rounded-2xl p-4 border border-white/5">
              <div className="flex items-center gap-2 mb-2">
                <Camera size={16} className="text-[#FF6B35]" />
                <span className="text-white/40 text-xs">总照片</span>
              </div>
              <p className="text-2xl font-bold text-white">{habits.totalPhotos}</p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/5">
              <div className="flex items-center gap-2 mb-2">
                <ImageIcon size={16} className="text-[#FF6B35]" />
                <span className="text-white/40 text-xs">配方数</span>
              </div>
              <p className="text-2xl font-bold text-white">{habits.totalRecipes}</p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/5">
              <div className="flex items-center gap-2 mb-2">
                <TrendingUp size={16} className="text-[#FF6B35]" />
                <span className="text-white/40 text-xs">连续拍摄</span>
              </div>
              <p className="text-2xl font-bold text-white">{habits.streakDays}<span className="text-sm font-normal text-white/40">天</span></p>
            </div>
            <div className="bg-white/5 rounded-2xl p-4 border border-white/5">
              <div className="flex items-center gap-2 mb-2">
                <Eye size={16} className="text-[#FF6B35]" />
                <span className="text-white/40 text-xs">平均置信度</span>
              </div>
              <p className="text-2xl font-bold text-white">{Math.round(habits.avgConfidence * 100)}<span className="text-sm font-normal text-white/40">%</span></p>
            </div>
          </section>
        )}

        {/* 最爱场景和胶片 */}
        {habits && (
          <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
            <h3 className="text-white/60 text-sm font-medium mb-3">拍摄偏好</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-white/40 text-xs">最爱场景</span>
                <span className="text-white text-sm">{habits.favoriteScene}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-white/40 text-xs">最爱胶片</span>
                <span className="text-white text-sm">{habits.favoriteFilm}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-white/40 text-xs">最后拍摄</span>
                <span className="text-white text-sm">{habits.lastShootDate}</span>
              </div>
            </div>
          </section>
        )}

        {/* 场景分布 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <h3 className="text-white/60 text-sm font-medium mb-4">场景分布</h3>
          {sceneStats.length > 0 ? (
            <div className="space-y-3">
              {sceneStats.map((scene) => (
                <div key={scene.sceneId} className="flex items-center gap-3">
                  <span className="text-white/60 text-xs w-20 truncate">{scene.sceneName}</span>
                  <div className="flex-1 h-2 bg-white/5 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF8A50] rounded-full transition-all duration-500"
                      style={{ width: `${scene.percentage}%` }}
                    />
                  </div>
                  <span className="text-white/40 text-xs w-12 text-right">{scene.count}张</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-white/40 text-sm text-center py-4">暂无场景数据</p>
          )}
        </section>

        {/* 胶片使用排行 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-4">
            <Film size={16} className="text-[#FF6B35]" />
            <h3 className="text-white/60 text-sm font-medium">胶片使用排行</h3>
          </div>
          {filmUsage.length > 0 ? (
            <div className="space-y-3">
              {filmUsage.map((film, index) => (
                <div key={film.filmId} className="flex items-center gap-3">
                  <span className="text-white/30 text-xs w-6">{index + 1}</span>
                  <span className="text-white/60 text-xs flex-1">{film.filmName}</span>
                  <div className="w-24 h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-white/30 rounded-full"
                      style={{ width: `${film.percentage}%` }}
                    />
                  </div>
                  <span className="text-white/40 text-xs w-10 text-right">{film.count}次</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-white/40 text-sm text-center py-4">暂无胶片使用数据</p>
          )}
        </section>

        {/* 大师建议 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <h3 className="text-white/60 text-sm font-medium mb-3">大师建议</h3>
          <div className="space-y-2.5">
            {masterTips.map((tip, index) => (
              <div key={index} className="flex items-start gap-2">
                <span className="text-[#FF6B35] text-xs mt-0.5">•</span>
                <span className="text-white/70 text-sm leading-relaxed">{tip}</span>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* 底部操作栏 */}
      <footer className="fixed bottom-0 left-0 right-0 bg-[#0A0A0A]/95 backdrop-blur-md border-t border-white/5">
        <div className="flex items-center justify-center px-4 py-3">
          <button
            onClick={handleViewDetails}
            className="flex items-center gap-2 px-6 py-2.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF8A50] shadow-lg shadow-[#FF6B35]/20"
          >
            <Eye size={18} className="text-white" />
            <span className="text-white font-medium text-sm">查看详细数据</span>
          </button>
        </div>
      </footer>
    </div>
  );
};

export default SceneAnalysisReport;
