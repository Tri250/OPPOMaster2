import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Bell, BellRing, Gift } from 'lucide-react';

const NotificationPage: React.FC = () => {
  const { notifications, setNotification, goBack } = useAppStore();

  const items = [
    { key: 'enabled', label: '接收通知', icon: Bell, desc: '开启或关闭所有通知' },
    { key: 'updates', label: '更新提醒', icon: BellRing, desc: '新版本发布时通知我' },
    { key: 'promotions', label: '活动推送', icon: Gift, desc: '精选推荐和优惠活动' },
  ];

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button 
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">通知设置</h1>
      </div>

      {/* Notification List */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="space-y-3">
          {items.map((item) => {
            const Icon = item.icon;
            const isEnabled = notifications[item.key as keyof typeof notifications];
            return (
              <div
                key={item.key}
                className="p-4 rounded-2xl bg-white/5 flex items-center gap-4"
              >
                <div className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center">
                  <Icon size={24} className="text-white/60" />
                </div>
                <div className="flex-1">
                  <p className="text-white font-medium">{item.label}</p>
                  <p className="text-white/50 text-xs">{item.desc}</p>
                </div>
                <button
                  onClick={() => setNotification(item.key, !isEnabled)}
                  className={`w-14 h-7 rounded-full relative transition-colors ${
                    isEnabled ? 'bg-[#FF6B35]' : 'bg-white/20'
                  }`}
                >
                  <div
                    className={`absolute top-0.5 w-6 h-6 rounded-full bg-white transition-all ${
                      isEnabled ? 'left-7' : 'left-0.5'
                    }`}
                  />
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default NotificationPage;
