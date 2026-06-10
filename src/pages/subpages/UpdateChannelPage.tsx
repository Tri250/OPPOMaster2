import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Download, Check, Zap,
  Clock, Shield, RotateCcw
} from 'lucide-react';

const UPDATE_CHANNELS = [
  { id: 'stable', name: '稳定版', desc: '最稳定的版本，推荐日常使用', icon: Shield, color: '#10B981' },
  { id: 'beta', name: '测试版', desc: '提前体验新功能，可能存在小问题', icon: Zap, color: '#F59E0B' },
  { id: 'dev', name: '开发版', desc: '最新功能，适合尝鲜用户', icon: RotateCcw, color: '#3B82F6' },
];

const UPDATE_SETTINGS = [
  { id: 'auto-check', name: '自动检查更新', enabled: true },
  { id: 'wifi-only', name: '仅 Wi-Fi 下下载', enabled: true },
  { id: 'auto-install', name: '夜间自动安装', enabled: false },
];

const UpdateChannelPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [selectedChannel, setSelectedChannel] = useState('stable');

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
        <h1 className="text-lg font-bold text-white">更新设置</h1>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
        {/* Current Version */}
        <div className="p-5 rounded-2xl bg-gradient-to-br from-[#FF6B35]/20 to-[#FF6B35]/5 border border-[#FF6B35]/20">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-[#FF6B35]/20 rounded-xl">
              <Check size={24} className="text-[#FF6B35]" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-bold text-white mb-1">已是最新版本</h2>
              <p className="text-white/60 text-sm">v2.0 (20260610)</p>
              <div className="flex items-center gap-2 mt-3">
                <Clock size={14} className="text-white/40" />
                <span className="text-white/40 text-xs">最后检查：刚刚</span>
              </div>
            </div>
          </div>
          <button className="w-full mt-4 py-3 bg-[#FF6B35] hover:bg-[#FF6B35]/80 rounded-xl font-medium text-white transition-colors">
            检查更新
          </button>
        </div>

        {/* Update Channels */}
        <div className="p-4 rounded-2xl bg-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">更新渠道</h3>
          <div className="space-y-2">
            {UPDATE_CHANNELS.map(channel => {
              const Icon = channel.icon;
              const isSelected = selectedChannel === channel.id;
              return (
                <button
                  key={channel.id}
                  onClick={() => setSelectedChannel(channel.id)}
                  className={`w-full flex items-center justify-between p-3 rounded-xl border transition-all text-left ${
                    isSelected
                      ? 'border-[#FF6B35]/50 bg-[#FF6B35]/10'
                      : 'border-white/5 hover:border-white/10 hover:bg-white/5'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-white/10">
                      <Icon size={18} className="text-white/70" />
                    </div>
                    <div>
                      <span className="text-sm font-medium text-white">{channel.name}</span>
                      <p className="text-xs text-white/50">{channel.desc}</p>
                    </div>
                  </div>
                  {isSelected && <Check size={18} className="text-[#FF6B35]" />}
                </button>
              );
            })}
          </div>
        </div>

        {/* Update Settings */}
        <div className="p-4 rounded-2xl bg-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">更新选项</h3>
          <div className="space-y-2">
            {UPDATE_SETTINGS.map(setting => (
              <div
                key={setting.id}
                className="flex items-center justify-between p-3 bg-white/5 rounded-xl"
              >
                <span className="text-sm text-white/80">{setting.name}</span>
                <div
                  className={`w-12 h-6 rounded-full transition-colors ${
                    setting.enabled ? 'bg-[#FF6B35]' : 'bg-white/20'
                  }`}
                >
                  <div
                    className={`w-5 h-5 bg-white rounded-full shadow transition-transform ${
                      setting.enabled ? 'translate-x-6' : 'translate-x-1'
                    }`}
                    style={{ marginTop: 2 }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Release Notes */}
        <div className="p-4 rounded-2xl bg-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">v2.0 更新内容</h3>
          <div className="space-y-2 text-sm">
            <div className="flex items-start gap-2">
              <span className="text-[#FF6B35] font-bold mt-0.5">•</span>
              <span className="text-white/60">全新 LUT 资源下载功能，20+ 专业滤镜</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-[#FF6B35] font-bold mt-0.5">•</span>
              <span className="text-white/60">哈苏色彩科学升级，HNCS 3.0 自然色彩</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-[#FF6B35] font-bold mt-0.5">•</span>
              <span className="text-white/60">哈苏之眼增强，支持 50+ 精细场景</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-[#FF6B35] font-bold mt-0.5">•</span>
              <span className="text-white/60">优化性能，启动速度提升 30%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UpdateChannelPage;
