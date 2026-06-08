import React, { useState, useCallback } from 'react';
import { useAppStore, homePresets } from '../../store/appStore';
import { ArrowLeft, Heart, Download, Upload, Star } from 'lucide-react';

const PresetManagerPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [favorites, setFavorites] = useState<string[]>(['home_1', 'home_3']);
  const [installed, setInstalled] = useState<string[]>(['home_1', 'home_2', 'home_3']);

  const toggleFavorite = useCallback((id: string) => {
    setFavorites((prev) => (prev.includes(id) ? prev.filter((f) => f !== id) : [...prev, id]));
  }, []);

  const toggleInstalled = useCallback((id: string) => {
    setInstalled((prev) => (prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]));
  }, []);

  const favoritePresets = homePresets.filter((p) => favorites.includes(p.id));
  const installedPresets = homePresets.filter((p) => installed.includes(p.id));

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: 'var(--color-bg-primary)', color: 'var(--color-text-primary)' }}
    >
      {/* 顶部标题栏 */}
      <div
        className="sticky top-0 z-50 backdrop-blur-md"
        style={{ background: 'rgba(10,10,10,0.92)', borderBottom: '1px solid var(--color-border-light)' }}
      >
        <div className="flex items-center gap-3 px-4 py-3">
          <button onClick={goBack} aria-label="返回上一页" className="p-2 -ml-2 rounded-full transition-colors" style={{ color: 'var(--color-text-primary)' }}>
            <ArrowLeft size={20} />
          </button>
          <div className="flex-1">
            <h1 className="text-lg font-bold">预设管理</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>管理已安装与收藏的预设</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 已安装预设 */}
        <div className="mb-6 animate-liquid-slide-up">
          <div className="flex items-center gap-2 mb-3">
            <Download size={14} style={{ color: 'var(--color-accent-primary)' }} />
            <span className="text-xs font-medium" style={{ color: 'var(--color-text-tertiary)' }}>
              已安装预设 ({installedPresets.length})
            </span>
          </div>
          {installedPresets.length > 0 ? (
            <div className="space-y-2">
              {installedPresets.map((preset) => (
                <div
                  key={preset.id}
                  className="rounded-2xl p-3 flex items-center gap-3"
                  style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)' }}
                >
                  <div
                    className="w-12 h-12 rounded-xl flex items-center justify-center text-lg font-bold flex-shrink-0"
                    style={{ background: 'var(--color-accent-primary-muted)', color: 'var(--color-accent-primary)' }}
                  >
                    {preset.name[0]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{preset.name}</p>
                    <p className="text-xs truncate" style={{ color: 'var(--color-text-tertiary)' }}>{preset.author}</p>
                  </div>
                  <button
                    onClick={() => toggleInstalled(preset.id)}
                    aria-label={`卸载预设${preset.name}`}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium transition-liquid"
                    style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-secondary)' }}
                  >
                    卸载
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs py-4 text-center" style={{ color: 'var(--color-text-tertiary)' }}>暂无已安装预设</p>
          )}
        </div>

        {/* 收藏预设 */}
        <div className="mb-6 animate-liquid-slide-up" style={{ animationDelay: '60ms' }}>
          <div className="flex items-center gap-2 mb-3">
            <Heart size={14} style={{ color: 'var(--color-accent-primary)' }} />
            <span className="text-xs font-medium" style={{ color: 'var(--color-text-tertiary)' }}>
              收藏预设 ({favoritePresets.length})
            </span>
          </div>
          {favoritePresets.length > 0 ? (
            <div className="space-y-2">
              {favoritePresets.map((preset) => (
                <div
                  key={preset.id}
                  className="rounded-2xl p-3 flex items-center gap-3"
                  style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)' }}
                >
                  <div
                    className="w-12 h-12 rounded-xl flex items-center justify-center text-lg font-bold flex-shrink-0"
                    style={{ background: 'var(--color-accent-primary-muted)', color: 'var(--color-accent-primary)' }}
                  >
                    {preset.name[0]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{preset.name}</p>
                    <p className="text-xs truncate" style={{ color: 'var(--color-text-tertiary)' }}>{preset.author}</p>
                  </div>
                  <button
                    onClick={() => toggleFavorite(preset.id)}
                    aria-label={`取消收藏${preset.name}`}
                    className="p-2"
                  >
                    <Heart size={16} fill="var(--color-accent-primary)" style={{ color: 'var(--color-accent-primary)' }} />
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs py-4 text-center" style={{ color: 'var(--color-text-tertiary)' }}>暂无收藏预设</p>
          )}
        </div>

        {/* 全部预设 */}
        <div className="mb-4 animate-liquid-slide-up" style={{ animationDelay: '120ms' }}>
          <div className="flex items-center gap-2 mb-3">
            <Star size={14} style={{ color: 'var(--color-accent-primary)' }} />
            <span className="text-xs font-medium" style={{ color: 'var(--color-text-tertiary)' }}>
              全部预设 ({homePresets.length})
            </span>
          </div>
          <div className="space-y-2">
            {homePresets.map((preset) => {
              const isFav = favorites.includes(preset.id);
              const isInst = installed.includes(preset.id);
              return (
                <div
                  key={preset.id}
                  className="rounded-2xl p-3 flex items-center gap-3"
                  style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)' }}
                >
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center text-sm font-bold flex-shrink-0"
                    style={{ background: 'var(--color-accent-primary-muted)', color: 'var(--color-accent-primary)' }}
                  >
                    {preset.name[0]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{preset.name}</p>
                    <p className="text-xs truncate" style={{ color: 'var(--color-text-tertiary)' }}>{preset.author} · {preset.brand}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <button onClick={() => toggleFavorite(preset.id)} aria-label={`${isFav ? '取消收藏' : '收藏'}${preset.name}`}>
                      <Heart size={16} fill={isFav ? 'var(--color-accent-primary)' : 'none'} style={{ color: isFav ? 'var(--color-accent-primary)' : 'var(--color-text-tertiary)' }} />
                    </button>
                    <button onClick={() => toggleInstalled(preset.id)} aria-label={`${isInst ? '卸载' : '安装'}${preset.name}`}>
                      <Download size={16} style={{ color: isInst ? 'var(--color-accent-primary)' : 'var(--color-text-tertiary)' }} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 导入/导出 */}
        <div className="flex gap-3 pb-6 animate-liquid-slide-up" style={{ animationDelay: '180ms' }}>
          <button
            aria-label="导入预设"
            className="flex-1 py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid"
            style={{ border: '1px solid var(--color-border-medium)', color: 'var(--color-text-secondary)' }}
          >
            <Upload size={16} />
            导入
          </button>
          <button
            aria-label="导出预设"
            className="flex-1 py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid"
            style={{ border: '1px solid var(--color-border-medium)', color: 'var(--color-text-secondary)' }}
          >
            <Download size={16} />
            导出
          </button>
        </div>
      </div>
    </div>
  );
};

export default React.memo(PresetManagerPage);
