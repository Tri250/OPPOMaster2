import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Bell, RefreshCw, Megaphone, Moon } from 'lucide-react';

const NotificationPage: React.FC = () => {
  const { goBack, notifications, setNotification } = useAppStore();
  const [nightDND, setNightDND] = React.useState(false);

  const toggleItems = [
    {
      id: 'enabled' as const,
      label: '通知总开关',
      desc: '启用或关闭所有通知推送',
      icon: Bell,
      enabled: notifications.enabled,
      onToggle: () => setNotification('enabled', !notifications.enabled),
    },
    {
      id: 'updates' as const,
      label: '更新通知',
      desc: '接收新版本和功能更新提醒',
      icon: RefreshCw,
      enabled: notifications.updates,
      onToggle: () => setNotification('updates', !notifications.updates),
    },
    {
      id: 'promotions' as const,
      label: '推广通知',
      desc: '接收活动和优惠信息推送',
      icon: Megaphone,
      enabled: notifications.promotions,
      onToggle: () => setNotification('promotions', !notifications.promotions),
    },
  ];

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
            <h1 className="text-lg font-bold">通知设置</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>管理通知推送偏好</p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        <div className="space-y-3">
          {toggleItems.map((item, index) => {
            const Icon = item.icon;
            return (
              <div
                key={item.id}
                className="rounded-2xl p-4 flex items-center gap-4 animate-liquid-slide-up"
                style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: `${index * 60}ms` }}
              >
                <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary-muted)' }}>
                  <Icon size={20} style={{ color: 'var(--color-accent-primary)' }} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium">{item.label}</p>
                  <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>{item.desc}</p>
                </div>
                <button
                  onClick={item.onToggle}
                  aria-label={`${item.enabled ? '关闭' : '开启'}${item.label}`}
                  className="w-12 h-7 rounded-full relative transition-colors flex-shrink-0"
                  style={{ background: item.enabled ? 'var(--color-accent-primary)' : 'var(--color-border-medium)' }}
                >
                  <div
                    className="absolute top-0.5 w-6 h-6 rounded-full bg-white transition-transform"
                    style={{ left: item.enabled ? '22px' : '2px' }}
                  />
                </button>
              </div>
            );
          })}

          {/* 夜间免打扰 */}
          <div
            className="rounded-2xl p-4 flex items-center gap-4 animate-liquid-slide-up"
            style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '180ms' }}
          >
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary-muted)' }}>
              <Moon size={20} style={{ color: 'var(--color-accent-primary)' }} />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium">夜间免打扰</p>
              <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>22:00 - 08:00 静默推送</p>
            </div>
            <button
              onClick={() => setNightDND((v) => !v)}
              aria-label={`${nightDND ? '关闭' : '开启'}夜间免打扰`}
              className="w-12 h-7 rounded-full relative transition-colors flex-shrink-0"
              style={{ background: nightDND ? 'var(--color-accent-primary)' : 'var(--color-border-medium)' }}
            >
              <div
                className="absolute top-0.5 w-6 h-6 rounded-full bg-white transition-transform"
                style={{ left: nightDND ? '22px' : '2px' }}
              />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default React.memo(NotificationPage);
