import React, { useState } from 'react';
import { ArrowLeft, Upload, Download, Heart, MessageCircle, Share2, Users, FileCode, Image, TrendingUp, Plus, Trash2 } from 'lucide-react';

const LUTSharePage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'lut' | 'share' | 'community'>('lut');

  const lutResources = [
    { id: 1, name: '电影感 LUT', type: 'CUBE', size: '12KB', downloads: 2800, rating: 4.8 },
    { id: 2, name: '胶片模拟', type: '3DL', size: '8KB', downloads: 1900, rating: 4.6 },
    { id: 3, name: '哈苏色彩', type: 'CUBE', size: '15KB', downloads: 3200, rating: 4.9 },
    { id: 4, name: '人像优化', type: 'CUBE', size: '10KB', downloads: 2100, rating: 4.5 },
  ];

  const sharedShots = [
    { id: 1, author: '@摄影师小王', title: '日落海边', likes: 128, comments: 23, time: '2小时前' },
    { id: 2, author: '@风光达人', title: '雪山日出', likes: 256, comments: 45, time: '5小时前' },
    { id: 3, author: '@街拍大师', title: '城市夜景', likes: 89, comments: 12, time: '1天前' },
  ];

  const creators = [
    { id: 1, name: '哈苏色彩研究所', followers: 12800, posts: 156, avatar: '📷' },
    { id: 2, name: '富士胶片俱乐部', followers: 8900, posts: 98, avatar: '🎬' },
    { id: 3, name: '徕卡影像工坊', followers: 6700, posts: 72, avatar: '📸' },
  ];

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">LUT资源与分享</h1>
          <p className="text-xs text-white/50">LUT库 · 拍摄分享 · 创作者社区</p>
        </div>
        <button className="text-[#FF6B35] bg-white/5 p-2 rounded-xl">
          <Plus size={20} />
        </button>
      </div>

      {/* Tab Bar */}
      <div className="flex border-b border-white/10">
        {[
          { key: 'lut', label: 'LUT资源', icon: FileCode },
          { key: 'share', label: '拍摄分享', icon: Image },
          { key: 'community', label: '社区', icon: Users },
        ].map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              className={`flex-1 py-3 flex items-center justify-center gap-2 text-sm font-medium transition-all ${
                activeTab === tab.key 
                  ? 'text-[#FF6B35] border-b-2 border-[#FF6B35]' 
                  : 'text-white/50'
              }`}
            >
              <Icon size={16} />
              {tab.label}
            </button>
          );
        })}
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {/* LUT Resources Tab */}
        {activeTab === 'lut' && (
          <>
            {/* Upload Section */}
            <button className="w-full border-2 border-dashed border-white/20 rounded-2xl p-6 flex flex-col items-center gap-2 mb-4 hover:border-[#FF6B35] transition-all">
              <Upload size={24} className="text-white/40" />
              <span className="text-sm text-white/60">上传 LUT 文件</span>
              <span className="text-xs text-white/40">支持 .cube / .3dl / .look 格式</span>
            </button>

            {/* LUT List */}
            <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
              <FileCode size={16} className="text-[#FF6B35]" />
              热门 LUT 资源
            </h3>
            <div className="space-y-3">
              {lutResources.map((lut) => (
                <div key={lut.id} className="bg-white/5 rounded-2xl p-4">
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h4 className="font-semibold">{lut.name}</h4>
                      <p className="text-xs text-white/50">{lut.type} · {lut.size}</p>
                    </div>
                    <span className="px-2 py-1 rounded-lg bg-[#FF6B35]/20 text-[#FF6B35] text-xs font-bold">
                      {lut.rating} ★
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-xs text-white/50">
                      <Download size={14} />
                      <span>{lut.downloads} 次下载</span>
                    </div>
                    <button className="px-4 py-2 rounded-xl bg-[#FF6B35] text-xs font-semibold">
                      下载
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

        {/* Share Tab */}
        {activeTab === 'share' && (
          <>
            {/* Share Upload */}
            <button className="w-full bg-gradient-to-r from-[#FF6B35]/20 to-[#3B82F6]/20 rounded-2xl p-6 flex flex-col items-center gap-2 mb-4 hover:from-[#FF6B35]/30 transition-all">
              <Share2 size={24} className="text-[#FF6B35]" />
              <span className="text-sm font-semibold">分享你的作品</span>
              <span className="text-xs text-white/50">上传照片，展示你的拍摄技巧</span>
            </button>

            {/* Shared Shots */}
            <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
              <TrendingUp size={16} className="text-[#FF6B35]" />
              最新分享
            </h3>
            <div className="space-y-4">
              {sharedShots.map((shot) => (
                <div key={shot.id} className="bg-white/5 rounded-2xl overflow-hidden">
                  <div className="aspect-[4/3] bg-gradient-to-br from-[#FF6B35]/10 to-[#3B82F6]/10" />
                  <div className="p-4">
                    <div className="flex items-center gap-2 mb-2">
                      <div className="w-8 h-8 rounded-full bg-white/10" />
                      <span className="text-sm font-medium">{shot.author}</span>
                      <span className="text-xs text-white/40 ml-auto">{shot.time}</span>
                    </div>
                    <h4 className="font-semibold mb-3">{shot.title}</h4>
                    <div className="flex items-center gap-4 text-white/60">
                      <button className="flex items-center gap-1 hover:text-red-400 transition-colors">
                        <Heart size={16} />
                        <span className="text-xs">{shot.likes}</span>
                      </button>
                      <button className="flex items-center gap-1 hover:text-blue-400 transition-colors">
                        <MessageCircle size={16} />
                        <span className="text-xs">{shot.comments}</span>
                      </button>
                      <button className="flex items-center gap-1 hover:text-green-400 transition-colors ml-auto">
                        <Share2 size={16} />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

        {/* Community Tab */}
        {activeTab === 'community' && (
          <>
            {/* Stats */}
            <div className="grid grid-cols-3 gap-3 mb-6">
              <div className="bg-white/5 rounded-2xl p-4 text-center">
                <p className="text-2xl font-bold text-[#FF6B35]">12.8K</p>
                <p className="text-xs text-white/50">创作者</p>
              </div>
              <div className="bg-white/5 rounded-2xl p-4 text-center">
                <p className="text-2xl font-bold text-[#3B82F6]">48.5K</p>
                <p className="text-xs text-white/50">作品</p>
              </div>
              <div className="bg-white/5 rounded-2xl p-4 text-center">
                <p className="text-2xl font-bold text-green-400">156K</p>
                <p className="text-xs text-white/50">互动</p>
              </div>
            </div>

            {/* Featured Creators */}
            <h3 className="text-sm font-semibold mb-3 flex items-center gap-2">
              <Users size={16} className="text-[#FF6B35]" />
              推荐创作者
            </h3>
            <div className="space-y-3">
              {creators.map((creator) => (
                <div key={creator.id} className="bg-white/5 rounded-2xl p-4 flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-[#FF6B35] to-[#3B82F6] flex items-center justify-center text-2xl">
                    {creator.avatar}
                  </div>
                  <div className="flex-1">
                    <h4 className="font-semibold">{creator.name}</h4>
                    <p className="text-xs text-white/50">
                      {(creator.followers / 1000).toFixed(1)}K 粉丝 · {creator.posts} 作品
                    </p>
                  </div>
                  <button className="px-4 py-2 rounded-xl bg-[#FF6B35] text-xs font-semibold">
                    关注
                  </button>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default LUTSharePage;
