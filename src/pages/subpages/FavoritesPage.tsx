import React, { useState } from 'react';
import { ArrowLeft, Plus, FolderOpen, Star, Heart } from 'lucide-react';

const FavoritesPage: React.FC = () => {
  const [selectedFolder, setSelectedFolder] = useState('all');
  const [showNewFolder, setShowNewFolder] = useState(false);

  const folders = [
    { id: 'all', name: '全部', count: 24 },
    { id: 'portrait', name: '人像', count: 8 },
    { id: 'landscape', name: '风景', count: 10 },
    { id: 'food', name: '美食', count: 6 },
  ];

  const presets = [
    { id: 1, name: '哈苏浓郁', author: 'Aurora', rating: 4.8 },
    { id: 2, name: '蓝调时刻', author: 'OPPO 影像', rating: 4.5 },
    { id: 3, name: '富士 NC', author: 'Aurora', rating: 4.9 },
    { id: 4, name: '手机徕卡', author: 'OPPO 影像', rating: 4.8 },
  ];

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">收藏夹</h1>
          <p className="text-xs text-white/50">管理你的珍藏预设</p>
        </div>
        <button 
          className="text-[#FF6B35] bg-white/5 p-2 rounded-xl"
          onClick={() => setShowNewFolder(true)}
        >
          <Plus size={20} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {/* Folder Tabs */}
        <div className="p-4 border-b border-white/5">
          <div className="flex gap-2 overflow-x-auto pb-2">
            {folders.map((folder) => (
              <button
                key={folder.id}
                onClick={() => setSelectedFolder(folder.id)}
                className={`flex-shrink-0 flex items-center gap-2 px-4 py-3 rounded-xl transition-all ${
                  selectedFolder === folder.id ? 'bg-[#FF6B35]' : 'bg-white/5'
                }`}
              >
                <FolderOpen size={16} />
                <span className="text-sm font-medium">{folder.name}</span>
                <span className="text-xs bg-white/20 px-2 py-0.5 rounded-full">
                  {folder.count}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* Preset Grid */}
        <div className="p-4">
          <div className="grid grid-cols-2 gap-3">
            {presets.map((preset) => (
              <div key={preset.id} className="bg-white/5 rounded-xl overflow-hidden">
                <div className="aspect-[3/4] bg-gradient-to-br from-[#FF6B35]/20 to-[#4DABF7]/20" />
                <div className="p-3">
                  <h4 className="text-sm font-semibold mb-1 truncate">{preset.name}</h4>
                  <p className="text-xs text-white/50 mb-2">{preset.author}</p>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1">
                      <Star size={12} className="text-yellow-400 fill-yellow-400" />
                      <span className="text-xs text-yellow-400">{preset.rating}</span>
                    </div>
                    <button className="text-[#FF6B35]">
                      <Heart size={16} fill="#FF6B35" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* New Folder Dialog */}
      {showNewFolder && (
        <div className="absolute inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-[#1A1A1A] rounded-2xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-bold mb-4">新建文件夹</h3>
            <input 
              className="w-full bg-white/5 rounded-xl p-4 mb-4 text-white border border-white/10 focus:border-[#FF6B35] outline-none"
              placeholder="输入文件夹名称"
            />
            <div className="flex gap-3">
              <button 
                className="flex-1 py-3 rounded-xl border border-white/20 text-sm"
                onClick={() => setShowNewFolder(false)}
              >
                取消
              </button>
              <button 
                className="flex-1 py-3 rounded-xl bg-[#FF6B35] text-sm font-semibold"
                onClick={() => setShowNewFolder(false)}
              >
                创建
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FavoritesPage;
