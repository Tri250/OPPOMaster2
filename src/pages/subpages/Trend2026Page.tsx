import React, { useState } from 'react';
import { ArrowLeft, TrendingUp, Sparkles, Palette, Bell } from 'lucide-react';

const Trend2026Page: React.FC = () => {
  const [subscribed, setSubscribed] = useState(false);

  const colors = [
    { name: '氧气蓝', hex: '#87CEEB' },
    { name: '莫兰迪灰', hex: '#988B7E' },
    { name: '哈苏橙', hex: '#FF6B35' },
    { name: '柯达金', hex: '#FFD700' },
    { name: '富士绿', hex: '#69DB7C' },
    { name: '徕卡红', hex: '#FF6B6B' },
    { name: '理光蓝', hex: '#4DABF7' },
    { name: '复古棕', hex: '#A0522D' },
  ];

  const trends = [
    { 
      id: 1, 
      name: '氧气感', 
      desc: '清新通透的日系风格', 
      color: '#87CEEB',
      count: 12
    },
    { 
      id: 2, 
      name: '莫兰迪', 
      desc: '高级灰调的优雅质感', 
      color: '#988B7E',
      count: 8
    },
    { 
      id: 3, 
      name: '哈苏浓郁', 
      desc: '饱满丰富的色彩表现', 
      color: '#FF6B35',
      count: 15
    },
    { 
      id: 4, 
      name: '柯达金', 
      desc: '温暖怀旧的胶片感', 
      color: '#FFD700',
      count: 10
    },
  ];

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">2026 趋势</h1>
          <p className="text-xs text-white/50">年度流行风格</p>
        </div>
        <button 
          className={`p-2 rounded-xl transition-all ${
            subscribed ? 'bg-[#FF6B35]' : 'bg-white/5'
          }`}
          onClick={() => setSubscribed(!subscribed)}
        >
          <Bell size={20} className={subscribed ? 'text-white' : 'text-white/60'} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {/* Hero Section */}
        <div className="bg-gradient-to-br from-[#FF6B35]/20 to-[#4DABF7]/20 rounded-2xl p-6 mb-6">
          <div className="flex items-center gap-3 mb-3">
            <Sparkles size={24} className="text-[#FF6B35]" />
            <h2 className="text-xl font-bold">2026 年度色彩</h2>
          </div>
          <p className="text-sm text-white/70 mb-4">
            探索今年最流行的色调和滤镜风格
          </p>
          <div className="flex flex-wrap gap-2">
            {colors.map((color) => (
              <div key={color.hex} className="flex flex-col items-center gap-1">
                <div 
                  className="w-12 h-12 rounded-xl"
                  style={{ backgroundColor: color.hex }}
                />
                <span className="text-[10px] text-white/60">{color.name}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Trend Styles */}
        <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
          <TrendingUp size={16} className="text-[#FF6B35]" />
          热门风格
        </h3>
        <div className="space-y-4 mb-6">
          {trends.map((trend) => (
            <div 
              key={trend.id}
              className="bg-white/5 rounded-2xl p-4 flex items-center gap-4"
            >
              <div 
                className="w-16 h-16 rounded-xl"
                style={{ backgroundColor: trend.color }}
              />
              <div className="flex-1">
                <h4 className="font-semibold mb-1">{trend.name}</h4>
                <p className="text-sm text-white/50">{trend.desc}</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-[#FF6B35] font-bold">{trend.count}</p>
                <p className="text-xs text-white/40">预设</p>
              </div>
            </div>
          ))}
        </div>

        {/* Subscribe Card */}
        <div className="bg-white/5 rounded-2xl p-6 mb-6">
          <h3 className="font-semibold mb-2">订阅更新</h3>
          <p className="text-sm text-white/60 mb-4">
            第一时间获取新的流行趋势和预设更新
          </p>
          <button 
            className={`w-full py-4 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 transition-all ${
              subscribed ? 'bg-green-500/20 text-green-400 border border-green-500/30' : 'bg-[#FF6B35]'
            }`}
            onClick={() => setSubscribed(!subscribed)}
          >
            {subscribed ? '已订阅 ✓' : '立即订阅'}
          </button>
        </div>

        {/* Tags */}
        <div className="flex flex-wrap gap-2">
          {['小红书热门', '抖音爆款', '人像推荐', '风景必备', '美食滤镜'].map((tag, idx) => (
            <span 
              key={idx}
              className="px-4 py-2 rounded-full bg-white/5 text-sm text-white/60"
            >
              #{tag}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Trend2026Page;
