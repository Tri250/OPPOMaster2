import React, { useState, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Plus, Trash2, Server } from 'lucide-react';

const DEFAULT_SOURCES = [
  { id: 'src_official', name: '官方预设源', url: 'https://presets.hasselblad-assistant.com', enabled: true },
  { id: 'src_community', name: '社区精选源', url: 'https://community.hasselblad-assistant.com/presets', enabled: true },
  { id: 'src_premium', name: '高级会员源', url: 'https://premium.hasselblad-assistant.com/presets', enabled: false },
];

const PresetSourceManager: React.FC = () => {
  const { goBack, presetSources, addPresetSource, removePresetSource, togglePresetSource } = useAppStore();
  const [showAddForm, setShowAddForm] = useState(false);
  const [newName, setNewName] = useState('');
  const [newUrl, setNewUrl] = useState('');

  const handleAdd = useCallback(() => {
    if (!newName.trim() || !newUrl.trim()) return;
    addPresetSource({
      name: newName.trim(),
      url: newUrl.trim(),
      enabled: true,
    });
    setNewName('');
    setNewUrl('');
    setShowAddForm(false);
  }, [newName, newUrl, addPresetSource]);

  const displaySources = presetSources.length > 0 ? presetSources : DEFAULT_SOURCES;

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
            <h1 className="text-lg font-bold">预设源管理</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>管理预设下载源</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 已添加的预设源列表 */}
        <div className="space-y-3">
          {displaySources.map((source, index) => (
            <div
              key={source.id}
              className="rounded-2xl p-4 animate-liquid-slide-up"
              style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: `${index * 60}ms` }}
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary-muted)' }}>
                  <Server size={20} style={{ color: 'var(--color-accent-primary)' }} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium">{source.name}</p>
                  <p className="text-xs truncate" style={{ color: 'var(--color-text-tertiary)' }}>
                    {source.url}
                  </p>
                </div>
                {/* 启用/禁用开关 */}
                <button
                  onClick={() => togglePresetSource(source.id)}
                  aria-label={`${source.enabled ? '禁用' : '启用'}${source.name}`}
                  className="w-12 h-7 rounded-full relative transition-colors flex-shrink-0"
                  style={{ background: source.enabled ? 'var(--color-accent-primary)' : 'var(--color-border-medium)' }}
                >
                  <div
                    className="absolute top-0.5 w-6 h-6 rounded-full bg-white transition-transform"
                    style={{ left: source.enabled ? '22px' : '2px' }}
                  />
                </button>
              </div>
              {/* 删除按钮 */}
              <div className="flex justify-end mt-2">
                <button
                  onClick={() => removePresetSource(source.id)}
                  aria-label={`删除${source.name}`}
                  className="flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs transition-liquid"
                  style={{ color: 'var(--color-error)' }}
                >
                  <Trash2 size={12} />
                  删除
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* 添加新源 */}
        {showAddForm ? (
          <div
            className="mt-4 rounded-2xl p-4 animate-liquid-fade"
            style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-accent)' }}
          >
            <p className="text-sm font-medium mb-3">添加新预设源</p>
            <input
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              aria-label="预设源名称"
              placeholder="源名称"
              className="w-full px-4 py-3 rounded-xl text-sm outline-none mb-2"
              style={{ background: 'var(--color-bg-tertiary)', border: '1px solid var(--color-border-light)', color: 'var(--color-text-primary)' }}
            />
            <input
              type="url"
              value={newUrl}
              onChange={(e) => setNewUrl(e.target.value)}
              aria-label="预设源地址"
              placeholder="https://example.com/presets"
              className="w-full px-4 py-3 rounded-xl text-sm outline-none mb-3"
              style={{ background: 'var(--color-bg-tertiary)', border: '1px solid var(--color-border-light)', color: 'var(--color-text-primary)' }}
            />
            <div className="flex gap-2">
              <button
                onClick={() => setShowAddForm(false)}
                aria-label="取消添加"
                className="flex-1 py-2.5 rounded-xl text-sm font-medium transition-liquid"
                style={{ border: '1px solid var(--color-border-medium)', color: 'var(--color-text-secondary)' }}
              >
                取消
              </button>
              <button
                onClick={handleAdd}
                disabled={!newName.trim() || !newUrl.trim()}
                aria-label="确认添加"
                className="flex-1 py-2.5 rounded-xl text-sm font-medium transition-liquid"
                style={{
                  background: 'var(--color-accent-primary)',
                  color: '#fff',
                  opacity: !newName.trim() || !newUrl.trim() ? 0.5 : 1,
                }}
              >
                添加
              </button>
            </div>
          </div>
        ) : (
          <button
            onClick={() => setShowAddForm(true)}
            aria-label="添加新预设源"
            className="w-full mt-4 py-3 rounded-2xl text-sm font-medium flex items-center justify-center gap-2 transition-liquid animate-liquid-slide-up"
            style={{ background: 'var(--color-bg-secondary)', border: '1px dashed var(--color-border-medium)', color: 'var(--color-text-secondary)', animationDelay: '180ms' }}
          >
            <Plus size={16} />
            添加新源
          </button>
        )}
      </div>
    </div>
  );
};

export default React.memo(PresetSourceManager);
