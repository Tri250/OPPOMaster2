import React, { useState, useEffect, useCallback } from 'react';
import { useAppStore, PresetSource } from '../../store/appStore';
import { tokens } from '../../styles/designTokens';
import {
  fetchPresetsFromSources as loadPresetsFromService,
  getLoadMessage,
} from '../../services/presetService';
import {
  ArrowLeft,
  Plus,
  Trash2,
  Edit2,
  X,
  RefreshCw,
  Cloud,
  Database,
  AlertCircle,
  CheckCircle2,
} from 'lucide-react';

const PresetSourceManager: React.FC = () => {
  const {
    reduceMotion, setCurrentSubPage,
    presetSources,
    addPresetSource,
    updatePresetSource,
    removePresetSource,
    togglePresetSource,
    fetchedPresets,
    setFetchedPresets,
  } = useAppStore();

  const [showAddModal, setShowAddModal] = useState(false);
  const [editingSource, setEditingSource] = useState<string | null>(null);
  const [newSourceName, setNewSourceName] = useState('');
  const [newSourceUrl, setNewSourceUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [lastLoadTime, setLastLoadTime] = useState<Date | null>(null);

  const fetchPresetsFromSources = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const result = await loadPresetsFromService(presetSources);

      // 更新成功加载源的时间戳
      if (result.successCount > 0) {
        presetSources.forEach((source) => {
          if (source.enabled) {
            updatePresetSource(source.id, { lastUpdated: new Date() });
          }
        });
      }

      setFetchedPresets(result.presets);
      setLastLoadTime(new Date());
      setLoadError(getLoadMessage(result));
    } catch (err) {
      console.error('Failed to fetch presets:', err);
      setLoadError('预设加载异常，请检查网络或预设源配置');
      setLastLoadTime(new Date());
    } finally {
      setIsLoading(false);
    }
  }, [presetSources, setFetchedPresets, updatePresetSource]);

  useEffect(() => {
    fetchPresetsFromSources();
  }, [fetchPresetsFromSources]);

  const handleAddSource = () => {
    if (!newSourceName.trim() || !newSourceUrl.trim()) return;
    addPresetSource({ name: newSourceName.trim(), url: newSourceUrl.trim(), enabled: true });
    setNewSourceName('');
    setNewSourceUrl('');
    setShowAddModal(false);
  };

  const handleUpdateSource = () => {
    if (!editingSource) return;
    updatePresetSource(editingSource, { name: newSourceName, url: newSourceUrl });
    setEditingSource(null);
    setNewSourceName('');
    setNewSourceUrl('');
  };

  const startEdit = (source: PresetSource) => {
    setEditingSource(source.id);
    setNewSourceName(source.name);
    setNewSourceUrl(source.url);
  };

  return (
    <div className="h-full w-full bg-master-bg flex flex-col" style={{ fontFamily: tokens.typography.fontFamily }}>
      {/* Header */}
      <div className="bg-master-bg border-b border-master-glass-border px-4 py-3 flex items-center gap-3">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-master-glass-strong rounded-full transition-all duration-normal"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h2 className="text-lg font-semibold text-white">预设源管理</h2>
        <div className="flex-1" />
        <button
          onClick={fetchPresetsFromSources}
          disabled={isLoading}
          className="p-2 hover:bg-master-glass-strong rounded-full transition-all duration-normal disabled:opacity-50 active:scale-95"
        >
          <RefreshCw size={20} className={`text-master-text-secondary ${isLoading ? 'animate-spin' : ''}`} />
        </button>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-[#FF6B35] hover:bg-[#FF8C42] text-white rounded-lg transition-all duration-normal"
        >
          <Plus size={18} />
          <span>添加</span>
        </button>
      </div>

      {/* Stats */}
      <div className="p-4 bg-master-glass border-b border-master-glass-border space-y-3">
        <div className="flex items-center gap-4 text-sm">
          <div className="flex items-center gap-2">
            <Database size={16} className="text-[#4CAF50]" />
            <span className="text-master-text-secondary">
              已启用: <span className="text-white font-medium">{presetSources.filter(s => s.enabled).length}</span>
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Cloud size={16} className="text-[#2196F3]" />
            <span className="text-master-text-secondary">
              已加载预设: <span className="text-white font-medium">{fetchedPresets.length}</span>
            </span>
          </div>
          {lastLoadTime && (
            <div className="flex items-center gap-2 ml-auto">
              <CheckCircle2 size={14} className="text-master-text-muted" />
              <span className="text-master-text-muted text-xs">
                {lastLoadTime.toLocaleTimeString()}
              </span>
            </div>
          )}
        </div>

        {loadError && (
          <div className="flex items-start gap-2 p-3 rounded-xl bg-yellow-500/10 border border-yellow-500/20">
            <AlertCircle size={16} className="text-yellow-500 mt-0.5 flex-shrink-0" />
            <p className="text-yellow-500/90 text-xs leading-relaxed">{loadError}</p>
          </div>
        )}
      </div>

      {/* Sources List */}
      <div className={`flex-1 overflow-y-auto p-4 space-y-3 ${!reduceMotion ? 'animate-fade-in-up' : ''}`}>
        {presetSources.map((source) => (
          <div key={source.id} className="bg-master-surface rounded-lg p-4 border border-master-glass-border">
            <div className="flex items-start justify-between gap-3">
              <div className="flex-1 min-w-0">
                {editingSource === source.id ? (
                  <div className="space-y-2">
                    <input
                      value={newSourceName}
                      onChange={(e) => setNewSourceName(e.target.value)}
                      placeholder="名称"
                      className="w-full px-3 py-2 bg-master-glass border border-master-glass-border rounded-lg text-white placeholder-white/30 focus:outline-none focus:border-[#FF6B35]"
                    />
                    <input
                      value={newSourceUrl}
                      onChange={(e) => setNewSourceUrl(e.target.value)}
                      placeholder="URL"
                      className="w-full px-3 py-2 bg-master-glass border border-master-glass-border rounded-lg text-white placeholder-white/30 focus:outline-none focus:border-[#FF6B35]"
                    />
                    <div className="flex gap-2">
                      <button
                        onClick={handleUpdateSource}
                        className="px-3 py-1.5 bg-[#4CAF50] hover:bg-[#4CAF50]/80 text-white rounded-lg text-sm active:scale-95"
                      >
                        保存
                      </button>
                      <button
                        onClick={() => {
                          setEditingSource(null);
                          setNewSourceName('');
                          setNewSourceUrl('');
                        }}
                        className="px-3 py-1.5 bg-master-glass-strong hover:bg-white/15 text-white rounded-lg text-sm"
                      >
                        取消
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <h3 className="font-medium text-white">{source.name}</h3>
                    <p className="text-xs text-master-text-muted mt-1 break-all">{source.url}</p>
                    {source.lastUpdated && (
                      <p className="text-xs text-master-text-muted mt-1">
                        上次更新: {source.lastUpdated.toLocaleDateString()}
                      </p>
                    )}
                  </>
                )}
              </div>
              
              {editingSource !== source.id && (
                <div className="flex items-center gap-2">
                  {/* Toggle */}
                  <button
                    onClick={() => togglePresetSource(source.id)}
                    className={`w-12 h-6 rounded-full transition-all duration-normal relative ${source.enabled ? 'bg-[#4CAF50]' : 'bg-master-glass-strong'}`}
                  >
                    <div
                      className={`absolute top-1 left-1 w-4 h-4 bg-white rounded-full transition-transform ${source.enabled ? 'translate-x-6' : 'translate-x-0'}`}
                    />
                  </button>
                  
                  {/* Edit */}
                  <button
                    onClick={() => startEdit(source)}
                    className="p-2 hover:bg-master-glass-strong rounded-lg transition-all duration-normal"
                  >
                    <Edit2 size={16} className="text-master-text-tertiary" />
                  </button>
                  
                  {/* Delete */}
                  <button
                    onClick={() => removePresetSource(source.id)}
                    className="p-2 hover:bg-red-500/20 rounded-lg transition-all duration-normal"
                  >
                    <Trash2 size={16} className="text-red-400" />
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Add Source Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
          <div className="bg-master-surface rounded-xl p-6 w-full max-w-md border border-master-glass-border">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-white">添加预设源</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1 hover:bg-master-glass-strong rounded-full"
              >
                <X size={20} className="text-master-text-tertiary" />
              </button>
            </div>
            
            <div className="space-y-3">
              <div>
                <label className="block text-sm text-master-text-secondary mb-1">名称</label>
                <input
                  value={newSourceName}
                  onChange={(e) => setNewSourceName(e.target.value)}
                  placeholder="例如：我的预设库"
                  className="w-full px-3 py-2 bg-master-glass border border-master-glass-border rounded-lg text-white placeholder-white/30 focus:outline-none focus:border-[#FF6B35]"
                />
              </div>
              
              <div>
                <label className="block text-sm text-master-text-secondary mb-1">URL</label>
                <input
                  value={newSourceUrl}
                  onChange={(e) => setNewSourceUrl(e.target.value)}
                  placeholder="https://example.com/presets.json"
                  className="w-full px-3 py-2 bg-master-glass border border-master-glass-border rounded-lg text-white placeholder-white/30 focus:outline-none focus:border-[#FF6B35]"
                />
              </div>
              
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 px-4 py-2 bg-master-glass-strong hover:bg-white/15 text-white rounded-lg transition-all duration-normal"
                >
                  取消
                </button>
                <button
                  onClick={handleAddSource}
                  disabled={!newSourceName.trim() || !newSourceUrl.trim()}
                  className="flex-1 px-4 py-2 bg-[#FF6B35] hover:bg-[#FF8C42] text-white rounded-lg transition-all duration-normal disabled:opacity-50 active:scale-95"
                >
                  添加
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PresetSourceManager;
