import React, { useState, useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Shield, Zap, FlaskConical, RefreshCw, Check } from 'lucide-react';

const CHANNELS = [
  { id: 'stable', name: '稳定版', desc: '经过充分测试，推荐所有用户使用', icon: Shield, color: 'var(--color-success)' },
  { id: 'beta', name: 'Beta 版', desc: '提前体验新功能，可能存在不稳定', icon: Zap, color: 'var(--color-accent-primary)' },
  { id: 'dev', name: '开发版', desc: '最新功能尝鲜，仅供开发者测试', icon: FlaskConical, color: '#7C6EF6' },
];

const CURRENT_VERSION = 'v2.4.1';

const UpdateChannelPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [selectedChannel, setSelectedChannel] = useState('stable');
  const [isChecking, setIsChecking] = useState(false);
  const [checkResult, setCheckResult] = useState<string | null>(null);

  const handleCheckUpdate = useCallback(() => {
    setIsChecking(true);
    setCheckResult(null);
    setTimeout(() => {
      setCheckResult('已是最新版本');
      setIsChecking(false);
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
            <h1 className="text-lg font-bold">更新渠道</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>选择更新频率与渠道</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 当前版本 */}
        <div
          className="rounded-2xl p-4 mb-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)' }}
        >
          <p className="text-xs font-medium" style={{ color: 'var(--color-text-tertiary)' }}>当前版本</p>
          <p className="text-lg font-bold mt-1">{CURRENT_VERSION}</p>
          <p className="text-xs mt-0.5" style={{ color: 'var(--color-text-tertiary)' }}>
            渠道：{CHANNELS.find((c) => c.id === selectedChannel)?.name}
          </p>
        </div>

        {/* 渠道选择 */}
        <div className="space-y-3">
          {CHANNELS.map((channel, index) => {
            const Icon = channel.icon;
            const isActive = selectedChannel === channel.id;
            return (
              <button
                key={channel.id}
                onClick={() => setSelectedChannel(channel.id)}
                aria-label={`选择${channel.name}渠道`}
                className="w-full rounded-2xl p-4 text-left transition-liquid animate-liquid-slide-up"
                style={{
                  background: isActive ? 'var(--color-accent-primary-muted)' : 'var(--color-bg-secondary)',
                  border: `1px solid ${isActive ? 'var(--color-accent-primary)' : 'var(--color-border-light)'}`,
                  animationDelay: `${index * 60}ms`,
                }}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center"
                    style={{ background: isActive ? 'var(--color-accent-primary)' : 'var(--color-bg-tertiary)' }}
                  >
                    <Icon size={20} style={{ color: isActive ? '#fff' : channel.color }} />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold">{channel.name}</p>
                      {isActive && <Check size={14} style={{ color: 'var(--color-accent-primary)' }} />}
                    </div>
                    <p className="text-xs mt-0.5" style={{ color: 'var(--color-text-tertiary)' }}>
                      {channel.desc}
                    </p>
                  </div>
                </div>
              </button>
            );
          })}
        </div>

        {/* 检查更新 */}
        <button
          onClick={handleCheckUpdate}
          disabled={isChecking}
          aria-label="检查更新"
          className="w-full mt-6 py-3.5 rounded-xl font-medium flex items-center justify-center gap-2 transition-liquid mb-6"
          style={{ background: 'var(--color-accent-primary)', color: '#fff', opacity: isChecking ? 0.6 : 1 }}
        >
          {isChecking ? (
            <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : (
            <RefreshCw size={18} />
          )}
          {isChecking ? '检查中...' : '检查更新'}
        </button>

        {/* 检查结果 */}
        {checkResult && (
          <div
            className="p-4 rounded-2xl text-center animate-liquid-fade"
            style={{ background: 'var(--color-success-muted)', border: '1px solid var(--color-success)', color: 'var(--color-success)' }}
          >
            <Check size={20} className="mx-auto mb-1" />
            <p className="text-sm font-medium">{checkResult}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default React.memo(UpdateChannelPage);
