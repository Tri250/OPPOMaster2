import React, { useState, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Download, Eye, Heart, Star } from 'lucide-react';
import {
  LUT_RESOURCES,
  LUT_CATEGORIES,
  formatFileSize,
  formatDownloads,
} from '../../services/lutResourceService';
import type { LUTResource } from '../../services/lutResourceService';

const LUTSharePage: React.FC = () => {
  const { goBack } = useAppStore();
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [downloadedIds, setDownloadedIds] = useState<Set<string>>(new Set());
  const [likedIds, setLikedIds] = useState<Set<string>>(new Set());
  const [previewLUT, setPreviewLUT] = useState<LUTResource | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const filteredLUTs =
    selectedCategory === 'all'
      ? LUT_RESOURCES
      : LUT_RESOURCES.filter((l) => l.category === selectedCategory);

  const handleDownload = useCallback((lut: LUTResource) => {
    setDownloadingId(lut.id);
    setTimeout(() => {
      setDownloadedIds((prev) => new Set([...prev, lut.id]));
      setDownloadingId(null);
    }, 1000);
  }, []);

  const toggleLike = useCallback((id: string) => {
    setLikedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

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
            <h1 className="text-lg font-bold">LUT 资源分享</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>{LUT_RESOURCES.length}+ 专业 LUT 滤镜</p>
          </div>
        </div>
        {/* 分类 */}
        <div className="px-4 pb-3 flex gap-2 overflow-x-auto scrollbar-hide">
          {LUT_CATEGORIES.map((cat) => (
            <button
              key={cat.key}
              onClick={() => setSelectedCategory(cat.key)}
              aria-label={`筛选${cat.label}`}
              className="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-liquid"
              style={{
                background: selectedCategory === cat.key ? 'var(--color-accent-primary)' : 'var(--color-bg-secondary)',
                color: selectedCategory === cat.key ? '#fff' : 'var(--color-text-secondary)',
                border: `1px solid ${selectedCategory === cat.key ? 'var(--color-accent-primary)' : 'var(--color-border-light)'}`,
              }}
            >
              {cat.icon} {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        <div className="grid grid-cols-2 gap-3">
          {filteredLUTs.map((lut, index) => {
            const isDownloaded = downloadedIds.has(lut.id);
            const isLiked = likedIds.has(lut.id);
            return (
              <div
                key={lut.id}
                className="rounded-2xl overflow-hidden animate-liquid-slide-up"
                style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: `${(index % 6) * 60}ms` }}
              >
                {/* 预览图 */}
                <div className="aspect-video relative" style={{ background: 'var(--color-bg-tertiary)' }}>
                  <img src={lut.previewImage} alt={lut.name} className="w-full h-full object-cover" loading="lazy" />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/70 to-transparent" />
                  {lut.isNew && (
                    <span className="absolute top-2 left-2 px-1.5 py-0.5 rounded text-[9px] font-bold" style={{ background: 'var(--color-success)', color: '#fff' }}>NEW</span>
                  )}
                  {lut.isHot && !lut.isNew && (
                    <span className="absolute top-2 left-2 px-1.5 py-0.5 rounded text-[9px] font-bold" style={{ background: 'var(--color-accent-primary)', color: '#fff' }}>HOT</span>
                  )}
                  <button
                    onClick={(e) => { e.stopPropagation(); toggleLike(lut.id); }}
                    aria-label={`${isLiked ? '取消收藏' : '收藏'}${lut.name}`}
                    className="absolute top-2 right-2 p-1.5 rounded-full"
                    style={{ background: 'rgba(0,0,0,0.5)' }}
                  >
                    <Heart size={14} fill={isLiked ? 'var(--color-accent-primary)' : 'none'} style={{ color: isLiked ? 'var(--color-accent-primary)' : '#fff' }} />
                  </button>
                </div>
                {/* 信息 */}
                <div className="p-3">
                  <h3 className="text-sm font-medium truncate">{lut.name}</h3>
                  <div className="flex items-center gap-2 mt-1">
                    <Star size={10} style={{ color: '#FFD700' }} />
                    <span className="text-[10px]" style={{ color: 'var(--color-text-tertiary)' }}>{lut.rating.toFixed(1)}</span>
                    <span className="text-[10px]" style={{ color: 'var(--color-text-tertiary)' }}>{formatDownloads(lut.downloads)}下载</span>
                  </div>
                  <div className="flex gap-2 mt-2">
                    <button
                      onClick={() => setPreviewLUT(lut)}
                      aria-label={`预览${lut.name}`}
                      className="flex-1 py-1.5 rounded-lg text-xs font-medium flex items-center justify-center gap-1 transition-liquid"
                      style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-secondary)' }}
                    >
                      <Eye size={12} /> 预览
                    </button>
                    <button
                      onClick={() => handleDownload(lut)}
                      disabled={downloadingId === lut.id}
                      aria-label={`下载${lut.name}`}
                      className="flex-1 py-1.5 rounded-lg text-xs font-medium flex items-center justify-center gap-1 transition-liquid"
                      style={{
                        background: isDownloaded ? 'var(--color-success)' : 'var(--color-accent-primary)',
                        color: '#fff',
                        opacity: downloadingId === lut.id ? 0.6 : 1,
                      }}
                    >
                      {downloadingId === lut.id ? (
                        <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      ) : isDownloaded ? (
                        <span>已下载</span>
                      ) : (
                        <>
                          <Download size={12} /> {formatFileSize(lut.fileSize)}
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 预览弹窗 */}
      {previewLUT && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ background: 'rgba(0,0,0,0.8)' }}
          onClick={() => setPreviewLUT(null)}
        >
          <div
            className="w-full max-w-sm rounded-2xl overflow-hidden animate-liquid-fade"
            style={{ background: 'var(--color-bg-secondary)' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="aspect-video">
              <img src={previewLUT.previewImage} alt={previewLUT.name} className="w-full h-full object-cover" />
            </div>
            <div className="p-4">
              <h2 className="text-lg font-bold">{previewLUT.name}</h2>
              <p className="text-xs mt-1" style={{ color: 'var(--color-text-tertiary)' }}>{previewLUT.description}</p>
              <div className="flex gap-2 mt-3">
                <span className="text-xs px-2 py-1 rounded" style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-tertiary)' }}>
                  .{previewLUT.format}
                </span>
                <span className="text-xs px-2 py-1 rounded" style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-tertiary)' }}>
                  {previewLUT.size}x{previewLUT.size}
                </span>
                <span className="text-xs px-2 py-1 rounded" style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-tertiary)' }}>
                  {formatFileSize(previewLUT.fileSize)}
                </span>
              </div>
              <button
                onClick={() => { handleDownload(previewLUT); setPreviewLUT(null); }}
                aria-label={`下载${previewLUT.name}`}
                className="w-full mt-4 py-3 rounded-xl font-medium flex items-center justify-center gap-2"
                style={{ background: 'var(--color-accent-primary)', color: '#fff' }}
              >
                <Download size={16} /> 下载 LUT
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default React.memo(LUTSharePage);
