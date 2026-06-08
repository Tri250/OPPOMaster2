import React, { useState, useEffect } from 'react';
import { useAppStore, homePresets, Preset } from '../../store/appStore';
import { cloudSyncService, cdnSources, BrandSyncState } from '../../services/cloudSyncService';
import { ArrowLeft, Plus, Search, Grid, List, Heart, Share2, Trash2, Check, Download, Upload, RefreshCw, Cloud, ToggleLeft, ToggleRight } from 'lucide-react';

const PresetManagerPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [searchQuery, setSearchQuery] = useState('');
  const [favorites, setFavorites] = useState<string[]>(['home_1', 'home_3']);
  const [selectedPresets, setSelectedPresets] = useState<string[]>([]);
  const [actionMode, setActionMode] = useState(false);
  const [presets, setPresets] = useState<Preset[]>(homePresets);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncProgress, setSyncProgress] = useState(0);
  const [brands, setBrands] = useState<BrandSyncState[]>([]);
  const [selectedBrand, setSelectedBrand] = useState<string | null>(null);

  // 初始化品牌状态
  useEffect(() => {
    const state = cloudSyncService.getState();
    setBrands(state.brands);
  }, []);

  // 自动从云同步加载预设
  useEffect(() => {
    const loadPresetsFromCloud = async () => {
      setIsSyncing(true);
      try {
        // 从所有已连接品牌获取预设
        const cloudPresets = await cloudSyncService.fetchPresets();
        setPresets([...cloudPresets, ...homePresets]);
      } catch (e) {
        setPresets(homePresets);
      }
      setIsSyncing(false);
    };

    loadPresetsFromCloud();
  }, [brands]);

  // 切换品牌连接状态
  const handleToggleBrand = (brandId: string) => {
    const brand = brands.find(b => b.id === brandId);
    if (brand) {
      if (brand.isConnected) {
        cloudSyncService.disconnectBrand(brandId);
        brand.isConnected = false;
      } else {
        cloudSyncService.connectBrand(brandId);
        brand.isConnected = true;
      }
      setBrands([...brands]);
    }
  };

  // 同步预设模块
  const handleSyncPresets = async () => {
    setIsSyncing(true);
    setSyncProgress(0);

    await cloudSyncService.syncModule('presets', (progress) => {
      setSyncProgress(progress);
    });

    // 刷新预设列表
    const cloudPresets = await cloudSyncService.fetchPresets();
    setPresets([...cloudPresets, ...homePresets]);
    setIsSyncing(false);
  };

  // 同步指定品牌
  const handleSyncBrand = async (brandId: string) => {
    setSelectedBrand(brandId);
    setIsSyncing(true);
    setSyncProgress(0);

    await cloudSyncService.syncBrandModules(brandId, (moduleId, progress) => {
      if (moduleId === 'presets') {
        setSyncProgress(progress);
      }
    });

    // 刷新预设列表
    const cloudPresets = await cloudSyncService.fetchPresets();
    setPresets([...cloudPresets, ...homePresets]);
    setIsSyncing(false);
    setSelectedBrand(null);
  };

  const toggleFavorite = (id: string) => {
    setFavorites(prev => 
      prev.includes(id) ? prev.filter(f => f !== id) : [...prev, id]
    );
  };

  const toggleSelect = (id: string) => {
    setSelectedPresets(prev => 
      prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id]
    );
  };

  const handleBatchDelete = () => {
    setSelectedPresets([]);
    setActionMode(false);
  };

  const filteredPresets = presets.filter(preset => 
    preset.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    preset.author.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // 计算已连接品牌数量
  const connectedCount = brands.filter(b => b.isConnected).length;

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button 
          onClick={() => actionMode ? setActionMode(false) : goBack()}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white flex-1">预设管理</h1>
        <div className="flex items-center gap-2">
          <div className="px-2 py-1 rounded-full bg-[#4CAF50]/20 border border-[#4CAF50]/30">
            <span className="text-[#4CAF50] text-xs font-medium">{connectedCount}/{brands.length} 已连接</span>
          </div>
        </div>
        <button
          onClick={() => setActionMode(!actionMode)}
          className="p-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <Check size={20} className={actionMode ? 'text-[#FF6B35]' : 'text-white'} />
        </button>
      </div>

      {/* Brand Selection */}
      <div className="px-4 py-3">
        <p className="text-white/50 text-xs mb-2">选择品牌数据源</p>
        <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-2">
          {brands.map((brand) => (
            <button
              key={brand.id}
              onClick={() => handleToggleBrand(brand.id)}
              className={`flex items-center gap-2 px-3 py-2 rounded-xl transition-all whitespace-nowrap ${
                brand.isConnected
                  ? 'bg-gradient-to-r from-white/10 to-white/5 border border-white/10'
                  : 'bg-white/5 opacity-60'
              }`}
            >
              <div 
                className="w-6 h-6 rounded-lg flex items-center justify-center font-bold text-white text-xs"
                style={{ backgroundColor: `${brand.color}20`, color: brand.color }}
              >
                {brand.name.charAt(0)}
              </div>
              <span className="text-white text-xs font-medium">{brand.name}</span>
              {brand.isConnected ? (
                <ToggleRight size={16} className="text-[#4CAF50]" />
              ) : (
                <ToggleLeft size={16} className="text-white/30" />
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Sync Progress */}
      {isSyncing && (
        <div className="px-4 py-2">
          <div className="p-3 rounded-xl bg-[#FF6B35]/20 border border-[#FF6B35]/30">
            <div className="flex items-center gap-3 mb-2">
              <RefreshCw size={16} className="text-[#FF6B35] animate-spin" />
              <span className="text-white text-sm">
                {selectedBrand ? `正在同步 ${cdnSources[selectedBrand]?.name} 预设...` : '正在同步预设...'}
              </span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div 
                className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] transition-all duration-200"
                style={{ width: `${syncProgress}%` }}
              />
            </div>
            <p className="text-white/50 text-xs mt-2">{syncProgress}% 完成</p>
          </div>
        </div>
      )}

      {/* Search Bar */}
      <div className="px-4 py-2">
        <div className="relative">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索预设..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
          />
        </div>
      </div>

      {/* Toolbar */}
      <div className="px-4 pb-2 flex items-center justify-between">
        <span className="text-white/50 text-xs">{filteredPresets.length} 个预设</span>
        <div className="flex gap-2">
          <button
            onClick={handleSyncPresets}
            disabled={isSyncing}
            className={`p-2 rounded-lg transition-colors ${isSyncing ? 'bg-white/5 text-white/30' : 'bg-[#FF6B35]/10 text-[#FF6B35] hover:bg-[#FF6B35]/20'}`}
          >
            <RefreshCw size={18} className={isSyncing ? 'animate-spin' : ''} />
          </button>
          <button
            onClick={() => setViewMode('grid')}
            className={`p-2 rounded-lg transition-colors ${viewMode === 'grid' ? 'bg-white/10 text-white' : 'text-white/40'}`}
          >
            <Grid size={18} />
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={`p-2 rounded-lg transition-colors ${viewMode === 'list' ? 'bg-white/10 text-white' : 'text-white/40'}`}
          >
            <List size={18} />
          </button>
        </div>
      </div>

      {/* Preset List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        {viewMode === 'grid' ? (
          <div className="grid grid-cols-2 gap-3">
            {filteredPresets.map((preset) => {
              const isFavorite = favorites.includes(preset.id);
              const isSelected = selectedPresets.includes(preset.id);
              
              return (
                <div
                  key={preset.id}
                  onClick={() => actionMode ? toggleSelect(preset.id) : null}
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
                  
                  {/* Cloud Badge */}
                  {preset.isNew && (
                    <div className="absolute top-2 left-2 ml-8 px-1.5 py-0.5 bg-[#4CAF50]/80 backdrop-blur-sm rounded text-[8px] font-bold text-white z-20">
                      云端
                    </div>
                  )}
                  
                  {/* Brand Badge */}
                  {preset.brand && (
                    <div className="absolute top-2 right-2 mt-6 px-1.5 py-0.5 bg-white/20 backdrop-blur-sm rounded text-[8px] text-white/70 z-20">
                      {preset.brand}
                    </div>
                  )}
                  
                  <div className="aspect-square">
                    <img
                      src={preset.coverPath}
                      alt={preset.name}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="absolute bottom-0 left-0 right-0 p-3 bg-gradient-to-t from-black/80 to-transparent">
                    <h3 className="text-white text-sm font-medium truncate">{preset.name}</h3>
                    <p className="text-white/50 text-xs truncate">{preset.author}</p>
                  </div>
                  <button
                    onClick={(e) => { e.stopPropagation(); toggleFavorite(preset.id); }}
                    className="absolute top-2 right-2 p-1.5 rounded-full bg-black/40 backdrop-blur-sm z-20"
                  >
                    <Heart size={14} className={isFavorite ? 'text-red-500 fill-red-500' : 'text-white/70'} />
                  </button>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="space-y-2">
            {filteredPresets.map((preset) => {
              const isFavorite = favorites.includes(preset.id);
              const isSelected = selectedPresets.includes(preset.id);
              
              return (
                <div
                  key={preset.id}
                  onClick={() => actionMode ? toggleSelect(preset.id) : null}
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
                    className="w-14 h-14 rounded-xl object-cover"
                  />
                  <div className="flex-1">
                    <h3 className="text-white text-sm font-medium">{preset.name}</h3>
                    <p className="text-white/50 text-xs">{preset.author}</p>
                    {preset.isNew && (
                      <span className="text-[#4CAF50] text-xs">云端同步</span>
                    )}
                  </div>
                  <button
                    onClick={(e) => { e.stopPropagation(); toggleFavorite(preset.id); }}
                    className="p-2"
                  >
                    <Heart size={18} className={isFavorite ? 'text-red-500 fill-red-500' : 'text-white/40'} />
                  </button>
                  <button className="p-2">
                    <Share2 size={18} className="text-white/40" />
                  </button>
                </div>
              );
            })}
          </div>
        )}

        {/* Batch Actions */}
        {actionMode && selectedPresets.length > 0 && (
          <div className="fixed bottom-24 left-4 right-4 p-4 rounded-2xl bg-white/10 backdrop-blur-xl border border-white/10">
            <div className="flex items-center justify-between">
              <span className="text-white text-sm">{selectedPresets.length} 项已选中</span>
              <div className="flex gap-2">
                <button className="p-2 rounded-lg bg-white/10">
                  <Download size={18} className="text-white" />
                </button>
                <button className="p-2 rounded-lg bg-white/10">
                  <Upload size={18} className="text-white" />
                </button>
                <button 
                  onClick={handleBatchDelete}
                  className="p-2 rounded-lg bg-red-500/20"
                >
                  <Trash2 size={18} className="text-red-500" />
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* FAB */}
      <button className="fixed bottom-24 right-4 w-14 h-14 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center shadow-lg shadow-[#FF6B35]/30 z-50">
        <Plus size={24} className="text-white" />
      </button>
    </div>
  );
};

export default PresetManagerPage;