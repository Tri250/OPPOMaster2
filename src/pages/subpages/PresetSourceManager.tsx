import React, { useState, useEffect, useCallback } from 'react';
import { useAppStore, Preset, PresetSource } from '../../store/appStore';
import {
  ArrowLeft,
  Plus,
  Trash2,
  Edit2,
  X,
  RefreshCw,
  Cloud,
  Database,
} from 'lucide-react';

const PresetSourceManager: React.FC = () => {
  const { 
    setCurrentSubPage,
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

  const fetchPresetsFromSources = useCallback(async () => {
    setIsLoading(true);
    try {
      const allPresets: Preset[] = [];
      
      for (const source of presetSources) {
        if (!source.enabled) continue;
        
        try {
          const response = await fetch(source.url);
          if (response.ok) {
            const data = await response.json();
            allPresets.push(...(data.presets || data || []));
          }
        } catch (err) {
          console.error(`Failed to fetch from ${source.name}:`, err);
        }
      }
      
      setFetchedPresets(allPresets);
    } catch (err) {
      console.error('Failed to fetch presets:', err);
    } finally {
      setIsLoading(false);
    }
  }, [presetSources, setFetchedPresets]);

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
    <div className="h-full w-full bg-gray-900 flex flex-col">
      {/* Header */}
      <div className="bg-gray-800 border-b border-gray-700 px-4 py-3 flex items-center gap-3">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-gray-700 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-gray-300" />
        </button>
        <h2 className="text-lg font-semibold text-white">订阅资源管理</h2>
        <div className="flex-1" />
        <button
          onClick={fetchPresetsFromSources}
          disabled={isLoading}
          className="p-2 hover:bg-gray-700 rounded-full transition-colors disabled:opacity-50"
        >
          <RefreshCw size={20} className={`text-gray-300 ${isLoading ? 'animate-spin' : ''}`} />
        </button>
        <button
          onClick={() => setShowAddModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-lg transition-colors"
        >
          <Plus size={18} />
          <span>添加</span>
        </button>
      </div>

      {/* Stats */}
      <div className="p-4 bg-gray-800/50 border-b border-gray-700">
        <div className="flex items-center gap-4 text-sm">
          <div className="flex items-center gap-2">
            <Database size={16} className="text-green-400" />
            <span className="text-gray-300">
              已启用: <span className="text-white font-medium">{presetSources.filter(s => s.enabled).length}</span>
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Cloud size={16} className="text-blue-400" />
            <span className="text-gray-300">
              已加载预设: <span className="text-white font-medium">{fetchedPresets.length}</span>
            </span>
          </div>
        </div>
      </div>

      {/* Sources List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {presetSources.map((source) => (
          <div key={source.id} className="bg-gray-800 rounded-lg p-4 border border-gray-700">
            <div className="flex items-start justify-between gap-3">
              <div className="flex-1 min-w-0">
                {editingSource === source.id ? (
                  <div className="space-y-2">
                    <input
                      value={newSourceName}
                      onChange={(e) => setNewSourceName(e.target.value)}
                      placeholder="名称"
                      className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-orange-500"
                    />
                    <input
                      value={newSourceUrl}
                      onChange={(e) => setNewSourceUrl(e.target.value)}
                      placeholder="URL"
                      className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-orange-500"
                    />
                    <div className="flex gap-2">
                      <button
                        onClick={handleUpdateSource}
                        className="px-3 py-1.5 bg-green-500 hover:bg-green-600 text-white rounded-lg text-sm"
                      >
                        保存
                      </button>
                      <button
                        onClick={() => {
                          setEditingSource(null);
                          setNewSourceName('');
                          setNewSourceUrl('');
                        }}
                        className="px-3 py-1.5 bg-gray-600 hover:bg-gray-500 text-white rounded-lg text-sm"
                      >
                        取消
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <h3 className="font-medium text-white">{source.name}</h3>
                    <p className="text-xs text-gray-400 mt-1 break-all">{source.url}</p>
                    {source.lastUpdated && (
                      <p className="text-xs text-gray-500 mt-1">
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
                    className={`w-12 h-6 rounded-full transition-colors relative ${source.enabled ? 'bg-green-500' : 'bg-gray-600'}`}
                  >
                    <div
                      className={`absolute top-1 left-1 w-4 h-4 bg-white rounded-full transition-transform ${source.enabled ? 'translate-x-6' : 'translate-x-0'}`}
                    />
                  </button>
                  
                  {/* Edit */}
                  <button
                    onClick={() => startEdit(source)}
                    className="p-2 hover:bg-gray-700 rounded-lg transition-colors"
                  >
                    <Edit2 size={16} className="text-gray-400" />
                  </button>
                  
                  {/* Delete */}
                  <button
                    onClick={() => removePresetSource(source.id)}
                    className="p-2 hover:bg-red-500/20 rounded-lg transition-colors"
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
          <div className="bg-gray-800 rounded-xl p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-white">添加预设源</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1 hover:bg-gray-700 rounded-full"
              >
                <X size={20} className="text-gray-400" />
              </button>
            </div>
            
            <div className="space-y-3">
              <div>
                <label className="block text-sm text-gray-300 mb-1">名称</label>
                <input
                  value={newSourceName}
                  onChange={(e) => setNewSourceName(e.target.value)}
                  placeholder="例如：我的预设库"
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-orange-500"
                />
              </div>
              
              <div>
                <label className="block text-sm text-gray-300 mb-1">URL</label>
                <input
                  value={newSourceUrl}
                  onChange={(e) => setNewSourceUrl(e.target.value)}
                  placeholder="https://example.com/presets.json"
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-orange-500"
                />
              </div>
              
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 px-4 py-2 bg-gray-600 hover:bg-gray-500 text-white rounded-lg transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={handleAddSource}
                  disabled={!newSourceName.trim() || !newSourceUrl.trim()}
                  className="flex-1 px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-lg transition-colors disabled:opacity-50"
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
