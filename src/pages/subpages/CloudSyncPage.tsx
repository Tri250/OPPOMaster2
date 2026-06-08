import React, { useState, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Cloud, RefreshCw, Smartphone, Check } from 'lucide-react';

const BRANDS = [
  { id: 'oppo', name: 'OPPO', color: '#1BA784' },
  { id: 'realme', name: 'realme', color: '#F5C542' },
  { id: 'vivo', name: 'vivo', color: '#4A90D9' },
  { id: 'honor', name: '荣耀', color: '#2D6BE6' },
];

const CloudSyncPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [syncEnabled, setSyncEnabled] = useState(false);
  const [selectedBrand, setSelectedBrand] = useState('oppo');
  const [lastSyncTime, setLastSyncTime] = useState<string | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);

  const handleSync = useCallback(() => {
    setIsSyncing(true);
    setTimeout(() => {
      const now = new Date();
      setLastSyncTime(
        `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`,
      );
      setIsSyncing(false);
    }, 2000);
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
            <h1 className="text-lg font-bold">云同步</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>跨设备同步预设与设置</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 同步开关 */}
        <div
          className="rounded-2xl p-4 flex items-center justify-between animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)' }}
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary-muted)' }}>
              <Cloud size={20} style={{ color: 'var(--color-accent-primary)' }} />
            </div>
            <div>
              <p className="text-sm font-medium">启用云同步</p>
              <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
                {syncEnabled ? '同步已开启' : '同步已关闭'}
              </p>
            </div>
          </div>
          <button
            onClick={() => setSyncEnabled((v) => !v)}
            aria-label={`${syncEnabled ? '关闭' : '开启'}云同步`}
            className="w-12 h-7 rounded-full relative transition-colors flex-shrink-0"
            style={{ background: syncEnabled ? 'var(--color-accent-primary)' : 'var(--color-border-medium)' }}
          >
            <div
              className="absolute top-0.5 w-6 h-6 rounded-full bg-white transition-transform"
              style={{ left: syncEnabled ? '22px' : '2px' }}
            />
          </button>
        </div>

        {/* 上次同步时间 */}
        <div
          className="mt-3 rounded-2xl p-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '60ms' }}
        >
          <p className="text-xs font-medium mb-1" style={{ color: 'var(--color-text-tertiary)' }}>上次同步时间</p>
          <p className="text-sm font-medium">
            {lastSyncTime ?? '从未同步'}
          </p>
        </div>

        {/* 同步品牌选择 */}
        <div
          className="mt-3 rounded-2xl p-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '120ms' }}
        >
          <div className="flex items-center gap-2 mb-3">
            <Smartphone size={14} style={{ color: 'var(--color-accent-primary)' }} />
            <p className="text-xs font-medium" style={{ color: 'var(--color-text-tertiary)' }}>同步品牌</p>
          </div>
          <div className="grid grid-cols-2 gap-2">
            {BRANDS.map((brand) => {
              const isSelected = selectedBrand === brand.id;
              return (
                <button
                  key={brand.id}
                  onClick={() => setSelectedBrand(brand.id)}
                  aria-label={`选择${brand.name}品牌同步`}
                  className="py-3 rounded-xl flex items-center justify-center gap-2 transition-liquid"
                  style={{
                    background: isSelected ? `${brand.color}20` : 'var(--color-bg-tertiary)',
                    border: `1px solid ${isSelected ? brand.color : 'var(--color-border-light)'}`,
                  }}
                >
                  <div className="w-3 h-3 rounded-full" style={{ background: brand.color }} />
                  <span
                    className="text-sm font-medium"
                    style={{ color: isSelected ? brand.color : 'var(--color-text-secondary)' }}
                  >
                    {brand.name}
                  </span>
                  {isSelected && <Check size={14} style={{ color: brand.color }} />}
                </button>
              );
            })}
          </div>
        </div>

        {/* 立即同步按钮 */}
        <button
          onClick={handleSync}
          disabled={!syncEnabled || isSyncing}
          aria-label="立即同步"
          className="w-full mt-6 py-3.5 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid mb-6"
          style={{
            background: 'var(--color-accent-primary)',
            color: '#fff',
            opacity: !syncEnabled || isSyncing ? 0.5 : 1,
          }}
        >
          {isSyncing ? (
            <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : (
            <RefreshCw size={18} />
          )}
          {isSyncing ? '同步中...' : '立即同步'}
        </button>
      </div>
    </div>
  );
};

export default React.memo(CloudSyncPage);
