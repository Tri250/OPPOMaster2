import React, { useState, useEffect } from 'react';
import { BarChart3, TrendingUp, Camera, Film, Lightbulb, Download, Share2, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/**
 * Layer 4: 大师洞察层 - 场景分析报告数据看板
 * 
 * 「哈苏大师之眼数据看板」
 * - 场景分布统计
 * - 拍摄习惯洞察
 * - 胶片风格使用排行
 * - 哈苏大师建议
 */

interface SceneDistribution {
  category: string;
  name: string;
  count: number;
  percentage: number;
  color: string;
}

interface ShootingInsight {
  icon: string;
  title: string;
  value: string;
  trend?: string;
  warning?: boolean;
}

interface FilmUsage {
  name: string;
  count: number;
  percentage: number;
}

interface MasterSuggestion {
  icon: string;
  title: string;
  description: string;
  recommendation: string;
}

// 模拟数据
const MOCK_SCENE_DISTRIBUTION: SceneDistribution[] = [
  { category: 'portrait', name: '人像', count: 412, percentage: 33, color: '#FF6B35' },
  { category: 'landscape', name: '风景', count: 298, percentage: 24, color: '#4CAF50' },
  { category: 'food', name: '美食', count: 186, percentage: 15, color: '#FF9800' },
  { category: 'night', name: '夜景', count: 124, percentage: 10, color: '#2196F3' },
  { category: 'urban', name: '城市', count: 89, percentage: 7, color: '#9C27B0' },
  { category: 'macro', name: '微距', count: 58, percentage: 5, color: '#E91E63' },
  { category: 'other', name: '其他', count: 80, percentage: 6, color: '#607D8B' },
];

const MOCK_SHOOTING_INSIGHTS: ShootingInsight[] = [
  { icon: '📸', title: '最常拍摄', value: '人像（周末下午 3-5 点）' },
  { icon: '🌅', title: '黄金时刻利用率', value: '仅 8%', trend: '偏低', warning: true },
  { icon: '🌃', title: '夜景占比增长', value: '+15% vs 上月', trend: '增长' },
  { icon: '🎞️', title: '最常用胶片', value: 'Portra 400（32%）' },
  { icon: '💡', title: '建议', value: '尝试更多逆光人像，搭配和光胶片' },
];

const MOCK_FILM_USAGE: FilmUsage[] = [
  { name: 'Portra 400', count: 398, percentage: 32 },
  { name: 'CC 经典负片', count: 312, percentage: 25 },
  { name: 'NH 浓郁', count: 245, percentage: 20 },
  { name: 'RDP3 正片', count: 198, percentage: 16 },
  { name: 'TX400 黑白', count: 94, percentage: 7 },
];

const MOCK_MASTER_SUGGESTIONS: MasterSuggestion[] = [
  {
    icon: '🎯',
    title: '人像优化',
    description: '你的人像照片 68% 用标准人像模式',
    recommendation: '试试「逆光人像 + 和光胶片」组合，可以拍出更有故事感的照片',
  },
  {
    icon: '🌃',
    title: '夜景提升',
    description: '夜景拍摄中，只有 12% 使用了 800T 胶片',
    recommendation: '800T 专为夜景优化——试试看！',
  },
];

export const SceneAnalysisReport: React.FC = () => {
  const navigate = useNavigate();
  const [totalPhotos] = useState(1247);
  const [month] = useState('2026年6月');

  const handleBack = () => navigate(-1);
  const handleExport = () => console.log('导出报告');
  const handleShare = () => console.log('分享统计');
  const handleViewAll = () => console.log('查看全部照片');

  return (
    <div className="min-h-screen bg-[#0A0A0A] pb-20">
      {/* 顶部导航 */}
      <header className="sticky top-0 z-50 bg-[#0A0A0A]/90 backdrop-blur-md border-b border-white/5">
        <div className="flex items-center justify-between px-4 h-14">
          <button onClick={handleBack} className="flex items-center gap-1 text-white/80">
            <ArrowLeft size={20} />
          </button>
          <h1 className="text-white font-medium">场景分析报告</h1>
          <div className="w-6" />
        </div>
      </header>

      <div className="px-4 py-4 space-y-4">
        {/* 报告标题 */}
        <div className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-2">
            <BarChart3 size={18} className="text-[#FF6B35]" />
            <span className="text-white font-semibold">哈苏大师之眼 · 场景分析报告</span>
          </div>
          <p className="text-white/50 text-sm">{month} · {totalPhotos} 张照片</p>
        </div>

        {/* 场景分布 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-4">
            <Camera size={14} className="text-[#FF6B35]" />
            <span className="text-white/60 text-xs font-medium">场景分布</span>
          </div>

          {/* 柱状图 */}
          <div className="space-y-3">
            {MOCK_SCENE_DISTRIBUTION.map((scene) => (
              <div key={scene.category} className="flex items-center gap-3">
                <div className="w-16 text-white/70 text-xs">{scene.name}</div>
                <div className="flex-1 h-6 bg-white/5 rounded-lg overflow-hidden relative">
                  <div
                    className="absolute inset-y-0 left-0 rounded-lg transition-all duration-500"
                    style={{
                      width: `${scene.percentage}%`,
                      backgroundColor: scene.color,
                    }}
                  />
                  <div className="absolute inset-y-0 left-0 right-0 flex items-center justify-between px-3">
                    <span className="text-white/80 text-xs font-medium">
                      {scene.count} ({scene.percentage}%)
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* 拍摄习惯洞察 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp size={14} className="text-[#FF6B35]" />
            <span className="text-white/60 text-xs font-medium">拍摄习惯洞察</span>
          </div>

          <div className="space-y-3">
            {MOCK_SHOOTING_INSIGHTS.map((insight, index) => (
              <div key={index} className="flex items-start gap-3 py-2">
                <span className="text-lg">{insight.icon}</span>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-white/70 text-xs">{insight.title}</span>
                    {insight.warning && (
                      <span className="text-yellow-500/80 text-xs">⚠️</span>
                    )}
                  </div>
                  <p className="text-white text-sm mt-1">{insight.value}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* 胶片风格使用排行 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-4">
            <Film size={14} className="text-[#FF6B35]" />
            <span className="text-white/60 text-xs font-medium">胶片风格使用排行</span>
          </div>

          <div className="space-y-3">
            {MOCK_FILM_USAGE.map((film, index) => (
              <div key={film.name} className="flex items-center gap-3">
                <div className="w-24 text-white/70 text-xs truncate">{film.name}</div>
                <div className="flex-1 h-4 bg-white/5 rounded overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF8A50] rounded transition-all duration-500"
                    style={{ width: `${film.percentage}%` }}
                  />
                </div>
                <div className="w-16 text-right">
                  <span className="text-white/60 text-xs">{film.count}次</span>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* 哈苏大师建议 */}
        <section className="bg-white/5 rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 mb-4">
            <Lightbulb size={14} className="text-[#FF6B35]" />
            <span className="text-white/60 text-xs font-medium">哈苏大师建议</span>
          </div>

          <div className="space-y-4">
            {MOCK_MASTER_SUGGESTIONS.map((suggestion, index) => (
              <div key={index} className="bg-[#FF6B35]/5 rounded-xl p-3 border border-[#FF6B35]/10">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-lg">{suggestion.icon}</span>
                  <span className="text-white font-medium text-sm">{suggestion.title}</span>
                </div>
                <p className="text-white/60 text-xs mb-2">{suggestion.description}</p>
                <p className="text-[#FF6B35] text-sm">{suggestion.recommendation}</p>
              </div>
            ))}
          </div>
        </section>

        {/* 底部操作按钮 */}
        <div className="flex gap-3 pt-4">
          <button
            onClick={handleExport}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors"
          >
            <Download size={16} className="text-white/60" />
            <span className="text-white/70 text-sm">导出报告</span>
          </button>
          <button
            onClick={handleShare}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors"
          >
            <Share2 size={16} className="text-white/60" />
            <span className="text-white/70 text-sm">分享统计</span>
          </button>
          <button
            onClick={handleViewAll}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-[#FF6B35] hover:bg-[#FF6B35]/90 transition-colors"
          >
            <span className="text-white text-sm font-medium">查看全部照片</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default SceneAnalysisReport;