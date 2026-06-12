import React, { useState, useCallback, useMemo } from 'react';
import { useAppStore, homePresets } from '../../store/appStore';
import { 
  ArrowLeft, Plus, Search, Grid, List, Heart, Share2, Trash2, Check, 
  Download, Filter, SortAsc, Folder, Clock, Star, 
  Edit, Move, MoreVertical, Crown, Sparkles, X,
  Upload, Cloud, RefreshCw, Tag,
  TrendingUp, Award, Archive, Zap
} from 'lucide-react';
import PresetImageGallery from '../../components/PresetImageGallery';
import PresetParameters, { PresetStats, ShootingTipsCard, UserComments, ApplyPresetButton, FavoriteButton, SimpleRelatedPresets } from '../../components/PresetParameters';

// 排序选项
const SORT_OPTIONS = [
  { id: 'newest', name: '最新添加', icon: Clock },
  { id: 'popular', name: '最多下载', icon: Download },
  { id: 'rating', name: '最高评分', icon: Star },
  { id: 'name', name: '名称排序', icon: SortAsc },
  { id: 'trending', name: '热门趋势', icon: TrendingUp },
];

// 筛选选项
const FILTER_OPTIONS = [
  { id: 'all', name: '全部', icon: Grid },
  { id: 'favorites', name: '收藏', icon: Heart },
  { id: 'hncs', name: '哈苏HNCS', icon: Crown },
  { id: 'new', name: '最新', icon: Sparkles },
  { id: 'custom', name: '自定义', icon: Edit },
  { id: 'downloaded', name: '已下载', icon: Download },
  { id: 'cloud', name: '云同步', icon: Cloud },
  { id: 'pro', name: '专业', icon: Award },
];

// 分组选项
const GROUP_OPTIONS = [
  { id: 'none', name: '不分组' },
  { id: 'brand', name: '按品牌' },
  { id: 'scene', name: '按场景' },
  { id: 'date', name: '按日期' },
  { id: 'rating', name: '按评分' },
];

// 预设标签
const PRESET_TAGS = [
  '人像', '风景', '夜景', '美食', '街拍', '胶片', 
  '黑白', '复古', '电影', '自然', '鲜艳', '柔和',
  '哈苏', '徕卡', '富士', '索尼', '佳能', '尼康',
  '专业', '入门', '创意', '商业', '婚礼', '旅行',
];

// 预设评论
interface PresetComment {
  id: string;
  user: string;
  avatar: string;
  content: string;
  rating: number;
  timestamp: Date;
  likes: number;
}

// 预设详情扩展
interface PresetDetail {
  id: string;
  downloads: number;
  rating: number;
  ratingCount: number;
  comments: PresetComment[];
  tags: string[];
  category: string;
  isPro: boolean;
  isDownloaded: boolean;
  isCloudSync: boolean;
}

const PresetManagerPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('newest');
  const [filterBy, setFilterBy] = useState('all');
  const [groupBy, setGroupBy] = useState('none');
  const [favorites, setFavorites] = useState<string[]>(['home_1', 'home_3']);
  const [downloadedPresets, setDownloadedPresets] = useState<string[]>(['home_1', 'home_2']);
  const [selectedPresets, setSelectedPresets] = useState<string[]>([]);
  const [actionMode, setActionMode] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [showPresetDetail, setShowPresetDetail] = useState<string | null>(null);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [showTagPanel, setShowTagPanel] = useState(false);
  const [showImportExport, setShowImportExport] = useState(false);

  // 模拟预设详情数据
  const presetDetails: Record<string, PresetDetail> = useMemo(() => ({
    'home_1': {
      id: 'home_1',
      downloads: 12580,
      rating: 4.9,
      ratingCount: 856,
      comments: [
        { id: '1', user: '摄影爱好者', avatar: '', content: '非常好用的预设！', rating: 5, timestamp: new Date(), likes: 24 },
        { id: '2', user: '专业摄影师', avatar: '', content: '色彩还原很准确', rating: 5, timestamp: new Date(), likes: 18 },
      ],
      tags: ['人像', '专业', '哈苏'],
      category: 'portrait',
      isPro: true,
      isDownloaded: true,
      isCloudSync: true,
    },
    'home_2': {
      id: 'home_2',
      downloads: 8920,
      rating: 4.7,
      ratingCount: 423,
      comments: [],
      tags: ['风景', '自然'],
      category: 'landscape',
      isPro: false,
      isDownloaded: true,
      isCloudSync: false,
    },
  }), []);

  // 切换收藏
  const toggleFavorite = useCallback((id: string) => {
    setFavorites(prev => 
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    );
  }, []);

  // 切换选择
  const toggleSelect = useCallback((id: string) => {
    setSelectedPresets(prev => 
      prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id]
    );
  }, []);

  // 全选/取消全选
  const toggleSelectAll = useCallback(() => {
    if (selectedPresets.length === homePresets.length) {
      setSelectedPresets([]);
    } else {
      setSelectedPresets(homePresets.map(p => p.id));
    }
  }, [selectedPresets.length]);

  // 批量删除
  const handleBatchDelete = useCallback(() => {
    setSelectedPresets([]);
    setActionMode(false);
  }, []);

  // 批量收藏
  const handleBatchFavorite = useCallback(() => {
    setFavorites(prev => [...new Set([...prev, ...selectedPresets])]);
    setSelectedPresets([]);
    setActionMode(false);
  }, [selectedPresets]);

  // 批量下载
  const handleBatchDownload = useCallback(() => {
    setDownloadedPresets(prev => [...new Set([...prev, ...selectedPresets])]);
    setSelectedPresets([]);
    setActionMode(false);
  }, [selectedPresets]);

  // 切换标签
  const toggleTag = useCallback((tag: string) => {
    setSelectedTags(prev => 
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    );
  }, []);

  // 过滤和排序
  const filteredPresets = useMemo(() => {
    let result = [...homePresets];

    // 筛选
    switch (filterBy) {
      case 'favorites':
        result = result.filter(p => favorites.includes(p.id));
        break;
      case 'hncs':
        result = result.filter(p => p.isHncs);
        break;
      case 'new':
        result = result.filter(p => p.isNew);
        break;
      case 'downloaded':
        result = result.filter(p => downloadedPresets.includes(p.id));
        break;
      case 'pro':
        result = result.filter(p => presetDetails[p.id]?.isPro);
        break;
    }

    // 标签筛选
    if (selectedTags.length > 0) {
      result = result.filter(p => 
        p.tags.some(t => selectedTags.includes(t))
      );
    }

    // 搜索
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.author.toLowerCase().includes(q) ||
        p.tags.some(t => t.toLowerCase().includes(q))
      );
    }

    // 排序
    result.sort((a, b) => {
      switch (sortBy) {
        case 'newest':
          return (b.isNew ? 1 : 0) - (a.isNew ? 1 : 0);
        case 'popular':
          return (presetDetails[b.id]?.downloads || 0) - (presetDetails[a.id]?.downloads || 0);
        case 'rating':
          return (presetDetails[b.id]?.rating || 0) - (presetDetails[a.id]?.rating || 0);
        case 'trending':
          return (presetDetails[b.id]?.ratingCount || 0) - (presetDetails[a.id]?.ratingCount || 0);
        case 'name':
          return a.name.localeCompare(b.name);
        default:
          return 0;
      }
    });

    return result;
  }, [filterBy, searchQuery, sortBy, favorites, downloadedPresets, selectedTags, presetDetails]);

  // 分组
  const groupedPresets = useMemo(() => {
    if (groupBy === 'none') {
      return { '全部': filteredPresets };
    }

    const groups: Record<string, typeof homePresets> = {};
    filteredPresets.forEach(preset => {
      let key: string;
      switch (groupBy) {
        case 'brand':
          key = preset.brand || '其他';
          break;
        case 'scene':
          key = preset.tags[0] || '其他';
          break;
        case 'date':
          key = preset.isNew ? '最新' : '较早';
          break;
        case 'rating': {
          const rating = presetDetails[preset.id]?.rating || 0;
          key = rating >= 4.5 ? '高评分' : rating >= 4 ? '中评分' : '普通';
          break;
        }
        default:
          key = '全部';
      }
      if (!groups[key]) groups[key] = [];
      groups[key].push(preset);
    });
    return groups;
  }, [filteredPresets, groupBy, presetDetails]);

  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white">
      {/* Header */}
      <div className="sticky top-0 z-50 bg-[#0a0a0a]/95 backdrop-blur-sm border-b border-white/5">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <button 
              onClick={() => actionMode ? (setActionMode(false), setSelectedPresets([])) : goBack()}
              className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
            >
              <ArrowLeft size={20} className="text-white" />
            </button>
            <div>
              <h1 className="text-lg font-bold">
                {actionMode ? `已选 ${selectedPresets.length} 项` : '预设管理'}
              </h1>
              <p className="text-xs text-white/50">
                {actionMode ? '选择要操作的预设' : `${filteredPresets.length} 个预设 · 企业级`}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {!actionMode && (
              <>
                <button
                  onClick={() => setShowTagPanel(!showTagPanel)}
                  className={`p-2 rounded-full ${showTagPanel ? 'bg-[#FF6B35]/20' : 'hover:bg-white/10'}`}
                >
                  <Tag size={18} className={showTagPanel ? 'text-[#FF6B35]' : 'text-white/50'} />
                </button>
                <button
                  onClick={() => setShowFilters(!showFilters)}
                  className={`p-2 rounded-full ${showFilters ? 'bg-[#FF6B35]/20' : 'hover:bg-white/10'}`}
                >
                  <Filter size={18} className={showFilters ? 'text-[#FF6B35]' : 'text-white/50'} />
                </button>
                <button
                  onClick={() => setShowImportExport(true)}
                  className="p-2 rounded-full hover:bg-white/10"
                >
                  <Archive size={18} className="text-white/50" />
                </button>
                <button
                  onClick={() => setActionMode(true)}
                  className="p-2 rounded-full hover:bg-white/10"
                >
                  <Check size={18} className="text-white/50" />
                </button>
              </>
            )}
            {actionMode && (
              <button
                onClick={toggleSelectAll}
                className="px-3 py-1.5 rounded-lg bg-white/10 text-white text-xs font-medium"
              >
                {selectedPresets.length === homePresets.length ? '取消全选' : '全选'}
              </button>
            )}
          </div>
        </div>

        {/* Search Bar */}
        <div className="px-4 pb-3">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="搜索预设名称、作者、标签..."
              className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2"
              >
                <X size={14} className="text-white/40" />
              </button>
            )}
          </div>
        </div>

        {/* Tag Panel */}
        {showTagPanel && (
          <div className="px-4 pb-3 border-t border-white/5 pt-3">
            <h3 className="text-white/50 text-xs mb-2">标签筛选</h3>
            <div className="flex flex-wrap gap-2">
              {PRESET_TAGS.map((tag) => (
                <button
                  key={tag}
                  onClick={() => toggleTag(tag)}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                    selectedTags.includes(tag)
                      ? 'bg-[#FF6B35] text-white'
                      : 'bg-white/5 text-white/60 hover:bg-white/10'
                  }`}
                >
                  {tag}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Filter Bar */}
        {showFilters && (
          <div className="px-4 pb-3 space-y-3 border-t border-white/5 pt-3">
            {/* Filter Options */}
            <div className="flex gap-2 overflow-x-auto scrollbar-hide">
              {FILTER_OPTIONS.map((filter) => {
                const Icon = filter.icon;
                return (
                  <button
                    key={filter.id}
                    onClick={() => setFilterBy(filter.id)}
                    className={`flex-shrink-0 px-3 py-2 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all ${
                      filterBy === filter.id
                        ? 'bg-[#FF6B35] text-white'
                        : 'bg-white/5 text-white/60 hover:bg-white/10'
                    }`}
                  >
                    <Icon size={14} />
                    {filter.name}
                  </button>
                );
              })}
            </div>

            {/* Sort & Group */}
            <div className="flex gap-2">
              <div className="flex-1">
                <span className="text-white/40 text-[10px] mb-1 block">排序</span>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-white/5 text-white text-xs outline-none"
                >
                  {SORT_OPTIONS.map(opt => (
                    <option key={opt.id} value={opt.id} className="bg-[#1a1a1a]">{opt.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex-1">
                <span className="text-white/40 text-[10px] mb-1 block">分组</span>
                <select
                  value={groupBy}
                  onChange={(e) => setGroupBy(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-white/5 text-white text-xs outline-none"
                >
                  {GROUP_OPTIONS.map(opt => (
                    <option key={opt.id} value={opt.id} className="bg-[#1a1a1a]">{opt.name}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        )}

        {/* Toolbar */}
        {!showFilters && !showTagPanel && (
          <div className="px-4 pb-3 flex items-center justify-between">
            <div className="flex gap-2 overflow-x-auto scrollbar-hide">
              {FILTER_OPTIONS.slice(0, 5).map((filter) => (
                <button
                  key={filter.id}
                  onClick={() => setFilterBy(filter.id)}
                  className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
                    filterBy === filter.id
                      ? 'bg-[#FF6B35] text-white'
                      : 'bg-white/5 text-white/60'
                  }`}
                >
                  {filter.name}
                </button>
              ))}
            </div>
            <div className="flex gap-1">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 rounded-lg transition-colors ${viewMode === 'grid' ? 'bg-white/10 text-white' : 'text-white/40'}`}
              >
                <Grid size={16} />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 rounded-lg transition-colors ${viewMode === 'list' ? 'bg-white/10 text-white' : 'text-white/40'}`}
              >
                <List size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Preset List */}
      <div className="flex-1 overflow-y-auto px-4 pb-24">
        {Object.entries(groupedPresets).map(([groupName, presets]) => (
          <div key={groupName}>
            {groupBy !== 'none' && (
              <h3 className="text-white/50 text-xs font-medium py-3 sticky top-0 bg-[#0a0a0a]">
                {groupName} ({presets.length})
              </h3>
            )}
            
            {viewMode === 'grid' ? (
              <div className="grid grid-cols-2 gap-3">
                {presets.map((preset) => {
                  const isFavorite = favorites.includes(preset.id);
                  const isSelected = selectedPresets.includes(preset.id);
                  const detail = presetDetails[preset.id];
                  
                  return (
                    <div
                      key={preset.id}
                      onClick={() => actionMode ? toggleSelect(preset.id) : setShowPresetDetail(preset.id)}
                      className={`relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer transition-all duration-300 ${
                        isSelected ? 'ring-2 ring-[#FF6B35]' : ''
                      }`}
                    >
                      {/* Selection Overlay */}
                      {actionMode && (
                        <div className={`absolute top-2 left-2 z-20 w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                          isSelected ? 'bg-[#FF6B35] border-[#FF6B35]' : 'border-white/50 bg-black/30'
                        }`}>
                          {isSelected && <Check size={14} className="text-white" />}
                        </div>
                      )}
                      
                      <div className="aspect-square relative">
                        <img
                          src={preset.coverPath}
                          alt={preset.name}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-gradient-to-t from-black/80 to-transparent" />
                        
                        {/* Badges */}
                        <div className="absolute top-2 right-2 flex gap-1">
                          {preset.isHncs && (
                            <span className="px-1.5 py-0.5 rounded bg-[#FF6B35] text-[9px] font-bold flex items-center gap-0.5">
                              <Crown size={10} />
                              HNCS
                            </span>
                          )}
                          {preset.isNew && !preset.isHncs && (
                            <span className="px-1.5 py-0.5 rounded bg-[#4CAF50] text-[9px] font-bold">NEW</span>
                          )}
                          {detail?.isPro && (
                            <span className="px-1.5 py-0.5 rounded bg-yellow-500/80 text-[9px] font-bold">PRO</span>
                          )}
                        </div>
                        
                        {/* Rating */}
                        {detail && (
                          <div className="absolute bottom-2 left-2 flex items-center gap-1 px-2 py-1 rounded-full bg-black/50 backdrop-blur-sm">
                            <Star size={10} className="text-yellow-400 fill-yellow-400" />
                            <span className="text-white text-[10px] font-medium">{detail.rating.toFixed(1)}</span>
                            <span className="text-white/40 text-[10px]">({detail.ratingCount})</span>
                          </div>
                        )}
                      </div>
                      
                      <div className="p-3">
                        <h3 className="text-white text-sm font-medium truncate">{preset.name}</h3>
                        <p className="text-white/50 text-xs truncate">{preset.author}</p>
                        
                        {/* Stats */}
                        <div className="flex items-center gap-3 mt-2">
                          <div className="flex items-center gap-1">
                            <Download size={10} className="text-white/30" />
                            <span className="text-white/40 text-[10px]">
                              {detail ? `${(detail.downloads / 1000).toFixed(1)}k` : '0'}
                            </span>
                          </div>
                          {detail?.isCloudSync && (
                            <Cloud size={10} className="text-[#00BCD4]" />
                          )}
                        </div>
                      </div>
                      
                      <button
                        onClick={(e) => { e.stopPropagation(); toggleFavorite(preset.id); }}
                        className="absolute top-2 right-2 p-1.5 rounded-full bg-black/40 backdrop-blur-sm z-20"
                        style={{ right: actionMode ? '40px' : '8px' }}
                      >
                        <Heart size={14} className={isFavorite ? 'text-red-500 fill-red-500' : 'text-white/70'} />
                      </button>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="space-y-2">
                {presets.map((preset) => {
                  const isFavorite = favorites.includes(preset.id);
                  const isSelected = selectedPresets.includes(preset.id);
                  const detail = presetDetails[preset.id];
                  
                  return (
                    <div
                      key={preset.id}
                      onClick={() => actionMode ? toggleSelect(preset.id) : setShowPresetDetail(preset.id)}
                      className={`flex items-center gap-3 p-3 rounded-2xl bg-white/5 transition-all ${
                        isSelected ? 'ring-2 ring-[#FF6B35]' : ''
                      }`}
                    >
                      {/* Selection */}
                      {actionMode && (
                        <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                          isSelected ? 'bg-[#FF6B35] border-[#FF6B35]' : 'border-white/30'
                        }`}>
                          {isSelected && <Check size={14} className="text-white" />}
                        </div>
                      )}
                      
                      <img
                        src={preset.coverPath}
                        alt={preset.name}
                        className="w-16 h-16 rounded-xl object-cover"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <h3 className="text-white text-sm font-medium truncate">{preset.name}</h3>
                          {preset.isHncs && <Crown size={12} className="text-[#FF6B35]" />}
                          {preset.isNew && <Sparkles size={12} className="text-[#4CAF50]" />}
                          {detail?.isPro && <Award size={12} className="text-yellow-400" />}
                        </div>
                        <p className="text-white/50 text-xs">{preset.author}</p>
                        <div className="flex items-center gap-3 mt-1">
                          {detail && (
                            <>
                              <div className="flex items-center gap-1">
                                <Star size={10} className="text-yellow-400 fill-yellow-400" />
                                <span className="text-white/40 text-[10px]">{detail.rating.toFixed(1)}</span>
                              </div>
                              <div className="flex items-center gap-1">
                                <Download size={10} className="text-white/30" />
                                <span className="text-white/40 text-[10px]">{(detail.downloads / 1000).toFixed(1)}k</span>
                              </div>
                            </>
                          )}
                          <span className="text-white/30 text-[10px]">{preset.brand}</span>
                        </div>
                      </div>
                      <div className="flex gap-1">
                        <button
                          onClick={(e) => { e.stopPropagation(); toggleFavorite(preset.id); }}
                          className="p-2"
                        >
                          <Heart size={18} className={isFavorite ? 'text-red-500 fill-red-500' : 'text-white/40'} />
                        </button>
                        <button className="p-2">
                          <Share2 size={18} className="text-white/40" />
                        </button>
                        <button className="p-2">
                          <MoreVertical size={18} className="text-white/40" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        ))}

        {/* Empty State */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20">
            <Folder size={48} className="text-white/20 mb-4" />
            <p className="text-white/50 text-sm">未找到匹配的预设</p>
            <p className="text-white/30 text-xs mt-1">请调整筛选条件</p>
          </div>
        )}
      </div>

      {/* Batch Actions */}
      {actionMode && selectedPresets.length > 0 && (
        <div className="fixed bottom-20 left-4 right-4 p-4 rounded-2xl bg-[#1a1a1a]/95 backdrop-blur-xl border border-white/10">
          <div className="flex items-center justify-between">
            <span className="text-white text-sm">{selectedPresets.length} 项已选中</span>
            <div className="flex gap-2">
              <button 
                onClick={handleBatchFavorite}
                className="p-2.5 rounded-xl bg-red-500/20 hover:bg-red-500/30"
              >
                <Heart size={18} className="text-red-500" />
              </button>
              <button 
                onClick={handleBatchDownload}
                className="p-2.5 rounded-xl bg-white/10 hover:bg-white/20"
              >
                <Download size={18} className="text-white" />
              </button>
              <button className="p-2.5 rounded-xl bg-white/10 hover:bg-white/20">
                <Share2 size={18} className="text-white" />
              </button>
              <button className="p-2.5 rounded-xl bg-white/10 hover:bg-white/20">
                <Move size={18} className="text-white" />
              </button>
              <button className="p-2.5 rounded-xl bg-white/10 hover:bg-white/20">
                <Cloud size={18} className="text-white" />
              </button>
              <button 
                onClick={handleBatchDelete}
                className="p-2.5 rounded-xl bg-red-500/20 hover:bg-red-500/30"
              >
                <Trash2 size={18} className="text-red-500" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* FAB */}
      {!actionMode && (
        <button className="fixed bottom-20 right-4 w-14 h-14 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center shadow-lg shadow-[#FF6B35]/30 z-50">
          <Plus size={24} className="text-white" />
        </button>
      )}

      {/* Preset Detail Modal - 哈苏大师配方卡 */}
      {showPresetDetail && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
          onClick={() => setShowPresetDetail(null)}
        >
          <div 
            className="w-full max-w-md bg-[#0a0a0a] rounded-2xl overflow-hidden max-h-[90vh] overflow-y-auto scrollbar-hide"
            onClick={(e) => e.stopPropagation()}
          >
            {(() => {
              const preset = homePresets.find(p => p.id === showPresetDetail);
              const detail = presetDetails[showPresetDetail];
              if (!preset) return null;
              return (
                <>
                  {/* 顶栏 */}
                  <div className="sticky top-0 z-10 bg-[#0a0a0a]/95 backdrop-blur-sm px-4 py-3 flex items-center justify-between border-b border-white/5">
                    <span className="text-white/50 text-sm">← 预设详情</span>
                    <div className="flex items-center gap-2">
                      <button className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors">
                        <Grid size={16} className="text-white/50" />
                      </button>
                      <button className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors">
                        <Edit size={16} className="text-white/50" />
                      </button>
                      <button
                        onClick={() => toggleFavorite(preset.id)}
                        className={`p-1.5 rounded-lg transition-colors ${
                          favorites.includes(preset.id)
                            ? 'bg-red-500/20'
                            : 'bg-white/5 hover:bg-white/10'
                        }`}
                      >
                        <Heart size={16} className={favorites.includes(preset.id) ? 'text-red-400 fill-red-400' : 'text-white/50'} />
                      </button>
                      <button
                        onClick={() => setShowPresetDetail(null)}
                        className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors"
                      >
                        <X size={16} className="text-white/50" />
                      </button>
                    </div>
                  </div>

                  {/* 图片轮播画廊 */}
                  <div className="px-4 pt-4">
                    <PresetImageGallery
                      images={[
                        preset.coverPath,
                        preset.coverPath + '?v=2',
                        preset.coverPath + '?v=3'
                      ]}
                      isPro={preset.isHncs || detail?.isPro}
                    />
                  </div>

                  {/* 预设信息 */}
                  <div className="px-4 py-4">
                    <div className="flex items-center gap-2 mb-1">
                      <h2 className="text-lg font-bold text-white">{preset.name}</h2>
                      {preset.isHncs && (
                        <span className="px-1.5 py-0.5 rounded bg-[#FF6B35] text-[9px] font-bold text-white flex items-center gap-0.5">
                          <Crown size={10} />
                          HNCS
                        </span>
                      )}
                      {detail?.isPro && (
                        <span className="px-1.5 py-0.5 rounded bg-yellow-500/80 text-[9px] font-bold text-white flex items-center gap-0.5">
                          <Zap size={10} />
                          PRO
                        </span>
                      )}
                    </div>
                    <p className="text-white/50 text-sm mb-2">@{preset.author}</p>
                    
                    {/* 标签 */}
                    <div className="flex flex-wrap gap-1.5 mb-4">
                      {preset.tags.map((tag, idx) => (
                        <span key={idx} className="px-2 py-0.5 bg-white/5 rounded-full text-[10px] text-white/60">
                          #{tag}
                        </span>
                      ))}
                    </div>

                    {/* 统计数据 */}
                    {detail && (
                      <PresetStats
                        downloads={detail.downloads}
                        rating={detail.rating}
                        ratingCount={detail.ratingCount}
                      />
                    )}
                  </div>

                  {/* 拍摄建议 */}
                  <div className="px-4 pb-4">
                    <ShootingTipsCard
                      description={{
                        title: '拍摄建议',
                        content: '【环境建议】日间户外或充足自然光【场景推荐】街拍、人像、风景、建筑【拍摄要点】适合追求经典胶片质感，建议使用黄金时刻拍摄'
                      }}
                    />
                  </div>

                  {/* 调色参数 */}
                  <div className="px-4 pb-4">
                    <PresetParameters
                      sections={[
                        {
                          title: '🎨 调色参数',
                          items: [
                            { label: '滤镜', value: '复古100%', span: 1 },
                            { label: '饱和度', value: `+${preset.saturation}`, span: 1 },
                            { label: '对比度', value: `+${preset.contrast}`, span: 1 },
                            { label: '锐度', value: `+${preset.sharpness}`, span: 1 },
                            { label: '暗角', value: '开', span: 2 },
                          ]
                        }
                      ]}
                    />
                  </div>

                  {/* 关联推荐 */}
                  <div className="px-4 pb-4">
                    <SimpleRelatedPresets
                      presets={[
                        { id: 'r1', name: '电影胶片', coverPath: preset.coverPath + '?r=1' },
                        { id: 'r2', name: '复古人像', coverPath: preset.coverPath + '?r=2' },
                        { id: 'r3', name: '清新风景', coverPath: preset.coverPath + '?r=3' },
                        { id: 'r4', name: '黑白经典', coverPath: preset.coverPath + '?r=4' },
                      ]}
                      onSelect={(id) => console.log('Selected:', id)}
                    />
                  </div>

                  {/* 用户评价 */}
                  {detail && detail.comments.length > 0 && (
                    <div className="px-4 pb-4">
                      <UserComments
                        comments={detail.comments.map(c => ({
                          id: c.id,
                          user: c.user,
                          content: c.content,
                          rating: c.rating
                        }))}
                        onViewAll={() => console.log('View all comments')}
                      />
                    </div>
                  )}

                  {/* 底部操作栏 */}
                  <div className="sticky bottom-0 bg-[#0a0a0a]/95 backdrop-blur-sm px-4 py-3 border-t border-white/5 flex gap-3">
                    <FavoriteButton
                      isFavorite={favorites.includes(preset.id)}
                      onToggle={() => toggleFavorite(preset.id)}
                    />
                    <ApplyPresetButton
                      onApply={() => console.log('Apply preset:', preset.id)}
                    />
                  </div>
                </>
              );
            })()}
          </div>
        </div>
      )}

      {/* Import/Export Modal */}
      {showImportExport && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
          onClick={() => setShowImportExport(false)}
        >
          <div 
            className="w-full max-w-sm bg-[#1a1a1a] rounded-2xl p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="text-lg font-bold mb-4">导入/导出预设</h2>
            
            <div className="space-y-3">
              <button className="w-full p-4 rounded-xl bg-white/5 hover:bg-white/10 flex items-center gap-3">
                <Upload size={20} className="text-[#FF6B35]" />
                <div className="text-left">
                  <p className="text-white text-sm font-medium">导入预设</p>
                  <p className="text-white/40 text-xs">从文件导入预设</p>
                </div>
              </button>
              
              <button className="w-full p-4 rounded-xl bg-white/5 hover:bg-white/10 flex items-center gap-3">
                <Download size={20} className="text-[#00BCD4]" />
                <div className="text-left">
                  <p className="text-white text-sm font-medium">导出预设</p>
                  <p className="text-white/40 text-xs">导出所有预设</p>
                </div>
              </button>
              
              <button className="w-full p-4 rounded-xl bg-white/5 hover:bg-white/10 flex items-center gap-3">
                <Cloud size={20} className="text-[#4CAF50]" />
                <div className="text-left">
                  <p className="text-white text-sm font-medium">云同步</p>
                  <p className="text-white/40 text-xs">同步到云端</p>
                </div>
              </button>
              
              <button className="w-full p-4 rounded-xl bg-white/5 hover:bg-white/10 flex items-center gap-3">
                <RefreshCw size={20} className="text-yellow-400" />
                <div className="text-left">
                  <p className="text-white text-sm font-medium">恢复默认</p>
                  <p className="text-white/40 text-xs">重置所有预设</p>
                </div>
              </button>
            </div>
            
            <button
              onClick={() => setShowImportExport(false)}
              className="w-full mt-4 py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium"
            >
              关闭
            </button>
          </div>
        </div>
      )}

      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default PresetManagerPage;
