import React from 'react';
import { useAppStore } from '../store/appStore';
import {
  Palette,
  Moon,
  Globe,
  Bell,
  Shield,
  FileText,
  ChevronRight,
  Camera,
  Crown,
  Cloud,
  Heart,
  Settings,
  Award,
  Zap,
  Star,
  Download,
  Trophy,
  Target,
  Flame,
  Users,
  MessageCircle,
  Share2,
  Eye,
  TrendingUp,
  Gift,
  Medal,
} from 'lucide-react';

// 2026年成就徽章
const achievements2026 = [
  { id: 1, icon: Crown, name: '哈苏大师', desc: '使用HNCS预设100次', progress: 100, unlocked: true, color: '#FF6B35' },
  { id: 2, icon: Flame, name: '热门创作者', desc: '作品获赞1000+', progress: 78, unlocked: false, color: '#FFD700' },
  { id: 3, icon: Medal, name: '胶片达人', desc: '使用胶片预设50次', progress: 100, unlocked: true, color: '#9C27B0' },
  { id: 4, icon: Trophy, name: '摄影大师', desc: '完成全部场景拍摄', progress: 45, unlocked: false, color: '#4CAF50' },
];

// 2026年每日任务
const dailyTasks2026 = [
  { id: 1, name: '分享作品到社区', reward: '+10积分', done: true },
  { id: 2, name: '使用哈苏预设拍摄', reward: '+20积分', done: false },
  { id: 3, name: '评论他人作品', reward: '+5积分', done: true },
];

const AboutScreen: React.FC = () => {
  const { theme, darkMode, navigateToSubPage } = useAppStore();

  const themeNames: Record<string, string> = {
    hasselblad: '哈苏橙',
    oppo: 'OPPO 绿',
    vivo: 'vivo 蓝',
    realme: 'realme 黄',
    honor: '荣耀蓝',
    xiaomi: '小米橙',
  };

  const darkModeNames: Record<string, string> = {
    system: '跟随系统',
    light: '浅色模式',
    dark: '深色模式',
  };

  const settingsItems = [
    { 
      icon: Palette, 
      label: '主题设置', 
      value: themeNames[theme] || '哈苏橙',
      route: 'theme-settings' as const,
      badge: null,
    },
    { 
      icon: Moon, 
      label: '深色模式', 
      value: darkModeNames[darkMode] || '跟随系统',
      route: 'dark-mode' as const,
      badge: null,
    },
    { 
      icon: Globe, 
      label: '更新渠道', 
      value: 'Gitee',
      route: null,
      badge: null,
    },
    { 
      icon: Bell, 
      label: '通知设置', 
      value: '',
      route: 'notification' as const,
      badge: '3',
    },
    { 
      icon: Shield, 
      label: '隐私政策', 
      value: '',
      route: 'privacy' as const,
      badge: null,
    },
    { 
      icon: FileText, 
      label: '用户协议', 
      value: '',
      route: 'privacy' as const,
      badge: null,
    },
  ];

  const statsItems = [
    { icon: Download, label: '下载预设', value: '128', unit: '个' },
    { icon: Heart, label: '收藏', value: '56', unit: '个' },
    { icon: Star, label: '评分', value: '4.8', unit: '分' },
    { icon: Award, label: '等级', value: '大师', unit: '' },
  ];

  const handleItemClick = (route: string | null) => {
    if (route) {
      navigateToSubPage(route as any);
    }
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3">
        <h1 className="text-xl font-bold text-white">我的</h1>
      </div>

      {/* User Profile Card - 哈苏大师模式风格 */}
      <div className="px-4 pb-4">
        <div className="relative rounded-2xl overflow-hidden">
          {/* Glass Background */}
          <div className="absolute inset-0 bg-gradient-to-br from-[#FF6B35]/20 via-[#FF6B35]/10 to-transparent" />
          <div className="absolute inset-0 backdrop-blur-xl bg-white/5" />
          
          <div className="relative p-6">
            <div className="flex items-center gap-4">
              {/* Avatar */}
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] flex items-center justify-center shadow-lg shadow-[#FF6B35]/30">
                <Camera size={32} className="text-white" />
              </div>

              {/* User Info */}
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <h2 className="text-lg font-bold text-white">摄影大师</h2>
                  <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
                    <Crown size={10} />
                    <span>哈苏认证</span>
                  </div>
                </div>
                <p className="text-white/50 text-xs">OPPO Find X7 Ultra 用户</p>
                <div className="flex items-center gap-3 mt-2">
                  <div className="flex items-center gap-1">
                    <Zap size={12} className="text-yellow-400" />
                    <span className="text-white/60 text-xs">Lv.12</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Cloud size={12} className="text-blue-400" />
                    <span className="text-white/60 text-xs">已同步</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-4 gap-3 mt-4 pt-4 border-t border-white/10">
              {statsItems.map((stat) => (
                <div key={stat.label} className="text-center">
                  <div className="flex items-center justify-center gap-1 mb-1">
                    <stat.icon size={14} className="text-[#FF6B35]" />
                    <span className="text-lg font-bold text-white">{stat.value}</span>
                  </div>
                  <span className="text-white/40 text-xs">{stat.label}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="px-4 pb-4">
        <div className="grid grid-cols-4 gap-2">
          {[
            { icon: Heart, label: '我的收藏', color: 'text-red-400' },
            { icon: Download, label: '下载管理', color: 'text-blue-400' },
            { icon: Award, label: '成就', color: 'text-yellow-400' },
            { icon: Settings, label: '设置', color: 'text-white/60' },
          ].map((action) => (
            <button key={action.label} className="flex flex-col items-center gap-1.5 p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors">
              <action.icon size={20} className={action.color} />
              <span className="text-white/60 text-xs">{action.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 2026成就徽章 - 小红书风格 */}
      <div className="px-4 pb-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Trophy size={14} className="text-yellow-400" />
            <span className="text-white/70 text-xs font-medium">成就徽章</span>
          </div>
          <button className="text-[#FF6B35] text-xs">查看全部</button>
        </div>
        <div className="grid grid-cols-4 gap-2">
          {achievements2026.map((achievement) => {
            const AchievementIcon = achievement.icon;
            return (
              <div
                key={achievement.id}
                className={`p-3 rounded-xl text-center transition-all ${
                  achievement.unlocked 
                    ? 'bg-white/5 border border-white/10' 
                    : 'bg-white/2 border border-white/5 opacity-60'
                }`}
              >
                <div 
                  className={`w-10 h-10 mx-auto rounded-full flex items-center justify-center mb-2 ${
                    achievement.unlocked ? '' : 'grayscale'
                  }`}
                  style={{ backgroundColor: `${achievement.color}20` }}
                >
                  <AchievementIcon size={20} style={{ color: achievement.color }} />
                </div>
                <h4 className="text-white text-[10px] font-medium truncate">{achievement.name}</h4>
                <div className="mt-1 h-1 bg-white/10 rounded-full overflow-hidden">
                  <div 
                    className="h-full rounded-full transition-all"
                    style={{ 
                      width: `${achievement.progress}%`,
                      backgroundColor: achievement.color,
                    }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 2026每日任务 - 游戏化设计 */}
      <div className="px-4 pb-4">
        <div className="rounded-2xl bg-gradient-to-r from-[#FF6B35]/10 to-[#FFD700]/10 border border-[#FF6B35]/20 p-4">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Gift size={14} className="text-[#FF6B35]" />
              <span className="text-white text-xs font-medium">每日任务</span>
            </div>
            <span className="text-white/40 text-[10px]">2/3 已完成</span>
          </div>
          <div className="space-y-2">
            {dailyTasks2026.map((task) => (
              <div 
                key={task.id}
                className={`flex items-center justify-between p-2 rounded-lg ${
                  task.done ? 'bg-green-500/10' : 'bg-white/5'
                }`}
              >
                <div className="flex items-center gap-2">
                  {task.done ? (
                    <div className="w-5 h-5 rounded-full bg-green-500 flex items-center justify-center">
                      <Star size={12} className="text-white fill-white" />
                    </div>
                  ) : (
                    <div className="w-5 h-5 rounded-full border border-white/20" />
                  )}
                  <span className={`text-xs ${task.done ? 'text-white/50 line-through' : 'text-white/80'}`}>
                    {task.name}
                  </span>
                </div>
                <span className={`text-[10px] ${task.done ? 'text-green-400' : 'text-[#FF6B35]'}`}>
                  {task.reward}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Settings List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        <div className="rounded-2xl overflow-hidden bg-white/5 backdrop-blur-sm">
          {settingsItems.map((item, index) => (
            <button
              key={item.label}
              onClick={() => handleItemClick(item.route)}
              className={`w-full flex items-center justify-between p-4 transition-all duration-200 hover:bg-white/10 ${
                index !== settingsItems.length - 1 ? 'border-b border-white/5' : ''
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/10 flex items-center justify-center">
                  <item.icon size={18} className="text-[#FF6B35]" />
                </div>
                <span className="text-white text-sm font-medium">{item.label}</span>
              </div>
              <div className="flex items-center gap-2">
                {item.badge && (
                  <span className="px-2 py-0.5 rounded-full bg-[#FF6B35] text-white text-xs font-bold">
                    {item.badge}
                  </span>
                )}
                {item.value && (
                  <span className="text-white/50 text-xs">{item.value}</span>
                )}
                <ChevronRight size={16} className="text-white/30" />
              </div>
            </button>
          ))}
        </div>

        {/* Developer Info */}
        <div className="mt-6 text-center">
          <p className="text-white/30 text-xs">Developed by Silas</p>
          <p className="text-white/20 text-xs mt-1">© 2024 OMaster. All rights reserved.</p>
        </div>

        {/* Bottom Spacing */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default AboutScreen;
