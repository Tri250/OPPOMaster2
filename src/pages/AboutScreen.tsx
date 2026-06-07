import React from 'react';
import {
  Palette,
  Moon,
  Globe,
  Bell,
  Shield,
  FileText,
  ChevronRight,
  Camera,
} from 'lucide-react';

const AboutScreen: React.FC = () => {
  const settingsItems = [
    { icon: Palette, label: '主题设置', value: '哈苏橙' },
    { icon: Moon, label: '深色模式', value: '跟随系统' },
    { icon: Globe, label: '更新渠道', value: 'Gitee' },
    { icon: Bell, label: '通知设置', value: '' },
    { icon: Shield, label: '隐私政策', value: '' },
    { icon: FileText, label: '用户协议', value: '' },
  ];

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3">
        <h1 className="text-xl font-bold text-white">关于</h1>
      </div>

      {/* App Info Card */}
      <div className="px-4 pb-4">
        <div className="bg-gradient-to-br from-[#1a1a1a] to-[#252525] rounded-2xl p-6 text-center">
          {/* Logo */}
          <div className="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] flex items-center justify-center shadow-lg shadow-[#FF6B35]/20">
            <Camera size={36} className="text-white" />
          </div>

          {/* App Name */}
          <h2 className="text-2xl font-bold text-white mb-1">OMaster</h2>
          <p className="text-white/50 text-sm mb-4">专业影像参数管理工具</p>

          {/* Version */}
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/5">
            <span className="text-white/70 text-xs">版本 1.3.1</span>
            <span className="w-1 h-1 rounded-full bg-[#FF6B35]" />
            <span className="text-[#FF6B35] text-xs">最新</span>
          </div>
        </div>
      </div>

      {/* Settings List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="bg-[#1a1a1a] rounded-2xl overflow-hidden">
          {settingsItems.map((item, index) => (
            <button
              key={item.label}
              className={`w-full flex items-center justify-between p-4 transition-colors duration-200 hover:bg-white/5 ${
                index !== settingsItems.length - 1 ? 'border-b border-white/5' : ''
              }`}
            >
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-white/5 flex items-center justify-center">
                  <item.icon size={18} className="text-[#FF6B35]" />
                </div>
                <span className="text-white text-sm font-medium">{item.label}</span>
              </div>
              <div className="flex items-center gap-2">
                {item.value && (
                  <span className="text-white/50 text-xs">{item.value}</span>
                )}
                <ChevronRight size={16} className="text-white/30" />
              </div>
            </button>
          ))}
        </div>

        {/* Developer Info */}
        <div className="mt-6 text-center">
          <p className="text-white/30 text-xs">Developed by Silas</p>
          <p className="text-white/20 text-xs mt-1">© 2024 OMaster. All rights reserved.</p>
        </div>

        {/* Bottom Spacing */}
        <div className="h-8" />
      </div>
    </div>
  );
};

export default AboutScreen;
