import React, { useState, useCallback } from 'react';
import { ArrowLeft, Download, Star, Heart, Search, Filter, Check, ExternalLink, FileText, Sparkles, Crown, Layers, Sliders, Circle, Users, Sun, X } from 'lucide-react';
import { useAppStore } from '../../store/appStore';
import { 
  LUT_RESOURCES, 
  LUT_CATEGORIES, 
  LUTResource, 
  formatFileSize, 
  formatDownloads,
  getLUTResources,
  searchLUTResources
} from '../../services/lutResourceService';

// LUT 混合配置
interface LUTBlendConfig {
  lut: LUTResource;
  weight: number;
  enabled: boolean;
}

const LUTSharePage: React.FC = () => {
  const { navigateToSubPage } = useAppStore();
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'downloads' | 'rating' | 'newest'>('downloads');
  const [downloadingId, setDownloadingId] = useState<string | null>(null);
  const [downloadedIds, setDownloadedIds] = useState<Set<string>>(new Set());
  const [likedIds, setLikedIds] = useState<Set<string>>(new Set());
  const [selectedLUT, setSelectedLUT] = useState<LUTResource | null>(null);
  
  // 新增状态：LUT 精细调节
  const [showAdjustPanel, setShowAdjustPanel] = useState(false);
  const [adjustingLUT, setAdjustingLUT] = useState<LUTResource | null>(null);
  const [lutIntensity, setLutIntensity] = useState(100);
  const [blendConfigs, setBlendConfigs] = useState<LUTBlendConfig[]>([]);
  const [showBlendPanel, setShowBlendPanel] = useState(false);
  const [maskType, setMaskType] = useState<'full' | 'sky' | 'person' | 'custom'>('full');

  // 过滤和排序
  const getFilteredLUTs = useCallback(() => {
    const baseResult = searchQuery 
      ? searchLUTResources(searchQuery)
      : getLUTResources(selectedCategory);
    const result = [...baseResult];

    // 排序
    result.sort((a, b) => {
      switch (sortBy) {
        case 'downloads':
          return b.downloads - a.downloads;
        case 'rating':
          return b.rating - a.rating;
        case 'newest':
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        default:
          return 0;
      }
    });

    return result;
  }, [selectedCategory, searchQuery, sortBy]);

  // 下载LUT
  const handleDownload = useCallback(async (lut: LUTResource) => {
    setDownloadingId(lut.id);
    try {
      // 创建下载链接
      const link = document.createElement('a');
      link.href = lut.downloadUrl;
      link.download = `${lut.nameEn.replace(/\s+/g, '_')}_${lut.size}x${lut.size}.cube`;
      link.target = '_blank';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      // 标记已下载
      setDownloadedIds(prev => new Set([...prev, lut.id]));
    } catch (error) {
      console.error('Download failed:', error);
    } finally {
      setDownloadingId(null);
    }
  }, []);

  // 切换喜欢
  const toggleLike = useCallback((id: string) => {
    setLikedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  // 打开 LUT 精细调节
  const openAdjustPanel = useCallback((lut: LUTResource) => {
    setAdjustingLUT(lut);
    setLutIntensity(100);
    setShowAdjustPanel(true);
  }, []);

  // 添加到混合列表
  const addToBlend = useCallback((lut: LUTResource) => {
    if (blendConfigs.length >= 3) return;
    setBlendConfigs(prev => [...prev, { lut, weight: 1 / (prev.length + 1), enabled: true }]);
    setShowBlendPanel(true);
  }, [blendConfigs.length]);

  // 更新混合权重
  const updateBlendWeight = useCallback((index: number, weight: number) => {
    setBlendConfigs(prev => prev.map((c, i) => i === index ? { ...c, weight } : c));
  }, []);

  // 移除混合项
  const removeBlendItem = useCallback((index: number) => {
    setBlendConfigs(prev => prev.filter((_, i) => i !== index));
  }, []);

  // 导出 LUT（模拟）
  const handleExport = useCallback(() => {
    alert('LUT 导出功能：将当前参数导出为 .cube 格式文件');
  }, []);

  const filteredLUTs = getFilteredLUTs();

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white">
      {/* Header */}
      <div className="sticky top-0 z-50 bg-[#0a0a0a]/95 backdrop-blur-sm border-b border-white/5">
        <div className="flex items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigateToSubPage(null)}
              className="p-2 rounded-full hover:bg-white/10 transition-colors"
            >
              <ArrowLeft size={20} className="text-white/70" />
            </button>
            <div>
              <h1 className="text-lg font-bold">LUT资源库</h1>
              <p className="text-xs text-white/50">视频调色LUT下载</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-white/40">{LUT_RESOURCES.length} 个LUT</span>
          </div>
        </div>

        {/* Search */}
        <div className="px-4 pb-3">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="搜索LUT名称、风格..."
              className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
            />
          </div>
        </div>

        {/* Categories */}
        <div className="px-4 pb-3 flex gap-2 overflow-x-auto scrollbar-hide">
          {LUT_CATEGORIES.map((cat) => (
            <button
              key={cat.key}
              onClick={() => setSelectedCategory(cat.key)}
              className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                selectedCategory === cat.key
                  ? 'bg-[#FF6B35] text-white'
                  : 'bg-white/5 text-white/60 hover:bg-white/10'
              }`}
            >
              <span className="mr-1">{cat.icon}</span>
              {cat.label}
            </button>
          ))}
        </div>

        {/* Sort */}
        <div className="px-4 pb-3 flex items-center gap-2">
          <Filter size={14} className="text-white/40" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'downloads' | 'rating' | 'newest')}
            className="bg-transparent text-white/60 text-xs outline-none cursor-pointer"
          >
            <option value="downloads" className="bg-[#1a1a1a]">最多下载</option>
            <option value="rating" className="bg-[#1a1a1a]">最高评分</option>
            <option value="newest" className="bg-[#1a1a1a]">最新发布</option>
          </select>
        </div>
      </div>

      {/* Content */}
      <div className="p-4">
        {/* Hot & New Section */}
        {selectedCategory === 'all' && !searchQuery && (
          <div className="mb-6">
            {/* 2026新品 */}
            <div className="mb-4">
              <h2 className="text-sm font-semibold text-white mb-3 flex items-center gap-2">
                <Sparkles size={16} className="text-[#FF6B35]" />
                2026新品
              </h2>
              <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2">
                {LUT_RESOURCES.filter(l => l.isNew).map((lut) => (
                  <div
                    key={lut.id}
                    onClick={() => setSelectedLUT(lut)}
                    className="flex-shrink-0 w-40 rounded-xl overflow-hidden bg-[#1a1a1a] cursor-pointer hover:scale-[1.02] transition-transform"
                  >
                    <div className="aspect-square relative">
                      <img src={lut.previewImage} alt={lut.name} className="w-full h-full object-cover" />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent" />
                      <div className="absolute top-2 left-2 px-2 py-0.5 bg-[#4CAF50] rounded text-[10px] font-bold">
                        NEW
                      </div>
                    </div>
                    <div className="p-2">
                      <h3 className="text-xs font-medium truncate">{lut.name}</h3>
                      <p className="text-[10px] text-white/50">{formatDownloads(lut.downloads)}下载</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 热门推荐 */}
            <div>
              <h2 className="text-sm font-semibold text-white mb-3 flex items-center gap-2">
                <Crown size={16} className="text-yellow-500" />
                热门推荐
              </h2>
              <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2">
                {LUT_RESOURCES.filter(l => l.isHot && !l.isNew).slice(0, 6).map((lut) => (
                  <div
                    key={lut.id}
                    onClick={() => setSelectedLUT(lut)}
                    className="flex-shrink-0 w-40 rounded-xl overflow-hidden bg-[#1a1a1a] cursor-pointer hover:scale-[1.02] transition-transform"
                  >
                    <div className="aspect-square relative">
                      <img src={lut.previewImage} alt={lut.name} className="w-full h-full object-cover" />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent" />
                      <div className="absolute top-2 left-2 px-2 py-0.5 bg-[#FF6B35] rounded text-[10px] font-bold">
                        HOT
                      </div>
                    </div>
                    <div className="p-2">
                      <h3 className="text-xs font-medium truncate">{lut.name}</h3>
                      <p className="text-[10px] text-white/50">{formatDownloads(lut.downloads)}下载</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* LUT Grid */}
        <div className="grid grid-cols-2 gap-3">
          {filteredLUTs.map((lut) => (
            <div
              key={lut.id}
              className="rounded-xl overflow-hidden bg-[#1a1a1a] border border-white/5"
            >
              {/* Preview */}
              <div 
                className="aspect-video relative cursor-pointer"
                onClick={() => setSelectedLUT(lut)}
              >
                <img src={lut.previewImage} alt={lut.name} className="w-full h-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
                
                {/* Badges */}
                <div className="absolute top-2 left-2 flex gap-1">
                  {lut.isNew && (
                    <span className="px-1.5 py-0.5 bg-[#4CAF50] rounded text-[9px] font-bold">NEW</span>
                  )}
                  {lut.isHot && !lut.isNew && (
                    <span className="px-1.5 py-0.5 bg-[#FF6B35] rounded text-[9px] font-bold">HOT</span>
                  )}
                </div>

                {/* Format Badge */}
                <div className="absolute top-2 right-2 px-1.5 py-0.5 bg-black/50 backdrop-blur-sm rounded text-[9px]">
                  .{lut.format}
                </div>

                {/* Like Button */}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleLike(lut.id);
                  }}
                  className="absolute bottom-2 right-2 p-1.5 rounded-full bg-black/50 backdrop-blur-sm"
                >
                  <Heart
                    size={14}
                    className={likedIds.has(lut.id) ? 'text-red-500 fill-red-500' : 'text-white/70'}
                  />
                </button>
              </div>

              {/* Info */}
              <div className="p-3">
                <h3 
                  className="text-sm font-medium truncate cursor-pointer hover:text-[#FF6B35]"
                  onClick={() => setSelectedLUT(lut)}
                >
                  {lut.name}
                </h3>
                <p className="text-xs text-white/50 truncate mt-0.5">{lut.description}</p>

                {/* Tags */}
                <div className="flex gap-1 mt-2 overflow-hidden">
                  {lut.tags.slice(0, 2).map((tag, idx) => (
                    <span key={idx} className="text-[10px] text-white/40">#{tag}</span>
                  ))}
                </div>

                {/* Stats */}
                <div className="flex items-center gap-3 mt-2">
                  <div className="flex items-center gap-1">
                    <Star size={10} className="text-yellow-400 fill-yellow-400" />
                    <span className="text-[10px] text-white/50">{lut.rating.toFixed(1)}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Download size={10} className="text-white/40" />
                    <span className="text-[10px] text-white/50">{formatDownloads(lut.downloads)}</span>
                  </div>
                  <div className="text-[10px] text-white/40 ml-auto">
                    {lut.size}x{lut.size}
                  </div>
                </div>

                {/* Download Button */}
                <button
                  onClick={() => handleDownload(lut)}
                  disabled={downloadingId === lut.id}
                  className={`w-full mt-3 py-2 rounded-lg text-xs font-medium flex items-center justify-center gap-1.5 transition-colors ${
                    downloadedIds.has(lut.id)
                      ? 'bg-green-500/20 text-green-400'
                      : 'bg-[#FF6B35]/20 text-[#FF6B35] hover:bg-[#FF6B35]/30'
                  }`}
                >
                  {downloadedIds.has(lut.id) ? (
                    <>
                      <Check size={14} />
                      <span>已下载</span>
                    </>
                  ) : downloadingId === lut.id ? (
                    <>
                      <div className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin" />
                      <span>下载中...</span>
                    </>
                  ) : (
                    <>
                      <Download size={14} />
                      <span>下载 ({formatFileSize(lut.fileSize)})</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Empty State */}
        {filteredLUTs.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20">
            <FileText size={48} className="text-white/20 mb-4" />
            <p className="text-white/50 text-sm">未找到匹配的LUT</p>
            <p className="text-white/30 text-xs mt-1">请调整搜索条件</p>
          </div>
        )}
      </div>

      {/* LUT Detail Modal */}
      {selectedLUT && !showAdjustPanel && !showBlendPanel && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-[#1a1a1a] rounded-2xl overflow-hidden">
            {/* Preview */}
            <div className="aspect-video relative">
              <img src={selectedLUT.previewImage} alt={selectedLUT.name} className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-[#1a1a1a] via-transparent to-transparent" />
              <button
                onClick={() => setSelectedLUT(null)}
                className="absolute top-3 right-3 p-2 rounded-full bg-black/50 backdrop-blur-sm"
              >
                <ArrowLeft size={18} className="text-white" />
              </button>
            </div>

            {/* Content */}
            <div className="p-4">
              <div className="flex items-start justify-between">
                <div>
                  <h2 className="text-lg font-bold">{selectedLUT.name}</h2>
                  <p className="text-xs text-white/50">{selectedLUT.nameEn}</p>
                </div>
                <div className="flex items-center gap-1 px-2 py-1 bg-[#FF6B35]/20 rounded-lg">
                  <Star size={12} className="text-yellow-400 fill-yellow-400" />
                  <span className="text-xs text-white/70">{selectedLUT.rating.toFixed(1)}</span>
                </div>
              </div>

              <p className="text-sm text-white/70 mt-3">{selectedLUT.description}</p>

              {/* Tags */}
              <div className="flex flex-wrap gap-2 mt-3">
                {selectedLUT.tags.map((tag, idx) => (
                  <span key={idx} className="px-2 py-1 bg-white/5 rounded-full text-xs text-white/60">
                    #{tag}
                  </span>
                ))}
              </div>

              {/* Info Grid */}
              <div className="grid grid-cols-2 gap-3 mt-4 p-3 bg-white/5 rounded-xl">
                <div>
                  <p className="text-[10px] text-white/40">格式</p>
                  <p className="text-sm font-medium">.{selectedLUT.format.toUpperCase()}</p>
                </div>
                <div>
                  <p className="text-[10px] text-white/40">尺寸</p>
                  <p className="text-sm font-medium">{selectedLUT.size}x{selectedLUT.size}</p>
                </div>
                <div>
                  <p className="text-[10px] text-white/40">文件大小</p>
                  <p className="text-sm font-medium">{formatFileSize(selectedLUT.fileSize)}</p>
                </div>
                <div>
                  <p className="text-[10px] text-white/40">下载次数</p>
                  <p className="text-sm font-medium">{formatDownloads(selectedLUT.downloads)}</p>
                </div>
              </div>

              {/* Suitable For */}
              <div className="mt-4">
                <p className="text-xs text-white/40 mb-2">适用场景</p>
                <div className="flex flex-wrap gap-2">
                  {selectedLUT.suitableFor.map((scene, idx) => (
                    <span key={idx} className="px-2 py-1 bg-[#FF6B35]/10 rounded-lg text-xs text-[#FF6B35]">
                      {scene}
                    </span>
                  ))}
                </div>
              </div>

              {/* Actions */}
              <div className="flex gap-3 mt-5">
                <button
                  onClick={() => {
                    toggleLike(selectedLUT.id);
                  }}
                  className={`flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-colors ${
                    likedIds.has(selectedLUT.id)
                      ? 'bg-red-500/20 text-red-400'
                      : 'bg-white/5 text-white/70 hover:bg-white/10'
                  }`}
                >
                  <Heart size={16} className={likedIds.has(selectedLUT.id) ? 'fill-current' : ''} />
                  {likedIds.has(selectedLUT.id) ? '已收藏' : '收藏'}
                </button>
                <button
                  onClick={() => openAdjustPanel(selectedLUT)}
                  className="flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 bg-purple-500/20 text-purple-400 hover:bg-purple-500/30 transition-colors"
                >
                  <Sliders size={16} />
                  精细调节
                </button>
              </div>
              
              <div className="flex gap-3 mt-3">
                <button
                  onClick={() => addToBlend(selectedLUT)}
                  disabled={blendConfigs.length >= 3}
                  className={`flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-colors ${
                    blendConfigs.length >= 3
                      ? 'bg-white/5 text-white/30'
                      : 'bg-blue-500/20 text-blue-400 hover:bg-blue-500/30'
                  }`}
                >
                  <Layers size={16} />
                  {blendConfigs.length >= 3 ? '已达上限' : '添加混合'}
                </button>
                <button
                  onClick={() => handleDownload(selectedLUT)}
                  disabled={downloadingId === selectedLUT.id}
                  className={`flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-colors ${
                    downloadedIds.has(selectedLUT.id)
                      ? 'bg-green-500/20 text-green-400'
                      : 'bg-[#FF6B35] text-white hover:bg-[#FF6B35]/90'
                  }`}
                >
                  {downloadedIds.has(selectedLUT.id) ? (
                    <>
                      <Check size={16} />
                      已下载
                    </>
                  ) : downloadingId === selectedLUT.id ? (
                    <>
                      <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
                      下载中...
                    </>
                  ) : (
                    <>
                      <Download size={16} />
                      下载LUT
                    </>
                  )}
                </button>
              </div>

              {/* Author */}
              <div className="mt-4 pt-4 border-t border-white/5 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] flex items-center justify-center text-xs font-bold">
                    O
                  </div>
                  <div>
                    <p className="text-xs font-medium">{selectedLUT.author}</p>
                    <p className="text-[10px] text-white/40">{selectedLUT.createdAt}</p>
                  </div>
                </div>
                {selectedLUT.authorUrl && (
                  <a
                    href={selectedLUT.authorUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="p-2 rounded-full hover:bg-white/5 transition-colors"
                  >
                    <ExternalLink size={14} className="text-white/40" />
                  </a>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* LUT 精细调节面板 */}
      {showAdjustPanel && adjustingLUT && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-[#1a1a1a] rounded-2xl overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between p-4 border-b border-white/10">
              <button
                onClick={() => setShowAdjustPanel(false)}
                className="p-2 rounded-full hover:bg-white/10 transition-colors"
              >
                <ArrowLeft size={18} className="text-white/70" />
              </button>
              <h2 className="text-lg font-bold">LUT 精细调节</h2>
              <button
                onClick={handleExport}
                className="p-2 rounded-full hover:bg-white/10 transition-colors"
              >
                <Download size={18} className="text-white/70" />
              </button>
            </div>

            <div className="p-4 space-y-4">
              {/* 当前 LUT */}
              <div className="flex items-center gap-3 p-3 bg-white/5 rounded-xl">
                <img src={adjustingLUT.previewImage} alt="" className="w-12 h-12 rounded-lg object-cover" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{adjustingLUT.name}</p>
                  <p className="text-xs text-white/50">{adjustingLUT.category}</p>
                </div>
              </div>

              {/* 强度滑块 */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm text-white/70">LUT 强度</span>
                  <span className="text-sm font-bold text-[#FF6B35]">{lutIntensity}%</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={lutIntensity}
                  onChange={(e) => setLutIntensity(Number(e.target.value))}
                  className="w-full h-2 rounded-full appearance-none bg-white/10 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#FF6B35]"
                />
                <div className="flex gap-2 mt-2">
                  {[0, 25, 50, 75, 100].map((v) => (
                    <button
                      key={v}
                      onClick={() => setLutIntensity(v)}
                      className={`flex-1 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                        lutIntensity === v ? 'bg-[#FF6B35] text-white' : 'bg-white/5 text-white/50'
                      }`}
                    >
                      {v}%
                    </button>
                  ))}
                </div>
              </div>

              {/* 局部应用 */}
              <div>
                <p className="text-sm text-white/70 mb-2">局部应用</p>
                <div className="grid grid-cols-4 gap-2">
                  {[
                    { id: 'full', name: '全图', icon: Circle },
                    { id: 'sky', name: '天空', icon: Sun },
                    { id: 'person', name: '人物', icon: Users },
                    { id: 'custom', name: '自定义', icon: Sliders },
                  ].map((item) => (
                    <button
                      key={item.id}
                      onClick={() => setMaskType(item.id as any)}
                      className={`py-2 rounded-lg flex flex-col items-center gap-1 transition-colors ${
                        maskType === item.id
                          ? 'bg-[#FF6B35] text-white'
                          : 'bg-white/5 text-white/50 hover:bg-white/10'
                      }`}
                    >
                      <item.icon size={16} />
                      <span className="text-[10px]">{item.name}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* 应用按钮 */}
              <button
                onClick={() => {
                  setShowAdjustPanel(false);
                  alert(`已应用 ${adjustingLUT.name}，强度 ${lutIntensity}%，区域 ${maskType}`);
                }}
                className="w-full py-3 rounded-xl bg-[#FF6B35] text-white font-medium flex items-center justify-center gap-2"
              >
                <Check size={18} />
                应用调节
              </button>
            </div>
          </div>
        </div>
      )}

      {/* LUT 混合面板 */}
      {showBlendPanel && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-[#1a1a1a] rounded-2xl overflow-hidden">
            {/* Header */}
            <div className="flex items-center justify-between p-4 border-b border-white/10">
              <button
                onClick={() => setShowBlendPanel(false)}
                className="p-2 rounded-full hover:bg-white/10 transition-colors"
              >
                <ArrowLeft size={18} className="text-white/70" />
              </button>
              <h2 className="text-lg font-bold">LUT 混合叠加</h2>
              <button
                onClick={handleExport}
                className="p-2 rounded-full hover:bg-white/10 transition-colors"
              >
                <Download size={18} className="text-white/70" />
              </button>
            </div>

            <div className="p-4 space-y-4">
              {blendConfigs.length === 0 ? (
                <div className="text-center py-8">
                  <Layers size={40} className="mx-auto text-white/20 mb-3" />
                  <p className="text-white/50 text-sm">暂无混合 LUT</p>
                  <p className="text-white/30 text-xs mt-1">从 LUT 详情添加到混合</p>
                </div>
              ) : (
                <>
                  {/* 混合列表 */}
                  <div className="space-y-3">
                    {blendConfigs.map((config, index) => (
                      <div key={index} className="p-3 bg-white/5 rounded-xl">
                        <div className="flex items-center gap-3 mb-2">
                          <img src={config.lut.previewImage} alt="" className="w-10 h-10 rounded-lg object-cover" />
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium truncate">{config.lut.name}</p>
                            <p className="text-xs text-white/50">权重: {(config.weight * 100).toFixed(0)}%</p>
                          </div>
                          <button
                            onClick={() => removeBlendItem(index)}
                            className="p-1.5 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
                          >
                            <X size={14} className="text-white/50" />
                          </button>
                        </div>
                        <input
                          type="range"
                          min="0"
                          max="1"
                          step="0.1"
                          value={config.weight}
                          onChange={(e) => updateBlendWeight(index, Number(e.target.value))}
                          className="w-full h-1.5 rounded-full appearance-none bg-white/10 [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-[#FF6B35]"
                        />
                      </div>
                    ))}
                  </div>

                  {/* 应用按钮 */}
                  <button
                    onClick={() => {
                      setShowBlendPanel(false);
                      alert(`已应用 ${blendConfigs.length} 个 LUT 混合`);
                    }}
                    className="w-full py-3 rounded-xl bg-[#FF6B35] text-white font-medium flex items-center justify-center gap-2"
                  >
                    <Check size={18} />
                    应用混合
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Styles */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default LUTSharePage;
