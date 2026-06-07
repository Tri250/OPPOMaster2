import { useState, useEffect, useCallback } from 'react';
import { cloudPresetService } from '../services/cloudPresetService';
import { CloudPreset, SyncState } from '../types/cloudPreset';

/**
 * 云端预设Hook
 * 提供预设数据访问与同步能力
 */
export function useCloudPresets() {
  const [presets, setPresets] = useState<CloudPreset[]>([]);
  const [state, setState] = useState<SyncState>(cloudPresetService.getState());
  const [loading, setLoading] = useState(false);

  // 加载并同步
  const load = useCallback(async (force = false) => {
    setLoading(true);
    cloudPresetService.initialize();
    
    if (force || cloudPresetService.shouldSync()) {
      const newState = await cloudPresetService.sync(force);
      setState(newState);
    }
    
    setPresets(cloudPresetService.getAll());
    setLoading(false);
  }, []);

  // 初始化加载
  useEffect(() => {
    load(false);
  }, [load]);

  // 强制刷新
  const refresh = useCallback(async () => {
    await load(true);
  }, [load]);

  // 切换收藏
  const toggleFavorite = useCallback((id: string) => {
    cloudPresetService.toggleFavorite(id);
    setPresets([...cloudPresetService.getAll()]);
  }, []);

  // 切换置顶
  const togglePin = useCallback((id: string) => {
    cloudPresetService.togglePin(id);
    setPresets([...cloudPresetService.getAll()]);
  }, []);

  // 应用预设参数
  const applyPreset = useCallback((preset: CloudPreset, onApply: (params: CloudPreset['params']) => void) => {
    onApply(preset.params);
  }, []);

  return {
    presets,
    state,
    loading,
    refresh,
    toggleFavorite,
    togglePin,
    applyPreset,
    getFavorites: () => presets.filter(p => p.isFavorite),
    getPinned: () => presets.filter(p => p.isPinned),
    getByBrand: (brand: string) => presets.filter(p => p.brand === brand),
    getByTag: (tag: string) => presets.filter(p => p.tags.includes(tag)),
    search: (query: string) => {
      const q = query.toLowerCase();
      return presets.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.author.toLowerCase().includes(q) ||
        p.tags.some(t => t.toLowerCase().includes(q))
      );
    },
  };
}
