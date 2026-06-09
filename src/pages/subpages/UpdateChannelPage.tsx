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
  const { setCurrentSubPage } = useAppStore();
  const [selectedChannel, setSelectedChannel] = useState('stable');

  return (
    <div className="h-full w-full bg-gray-50 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center gap-3 shadow-sm">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-gray-700" />
        </button>
        <div className="flex items-center gap-2">
          <Download size={20} className="text-green-500" />
          <h1 className="text-lg font-semibold text-gray-900">更新设置</h1>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Current Version */}
        <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-2xl p-5 text-white">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-white/20 rounded-xl">
              <Check size={32} className="text-white" />
            </div>
            <div className="flex-1">
              <h2 className="text-xl font-bold mb-1">已是最新版本</h2>
              <p className="text-green-100 text-sm">v3.2.0 (20260608)</p>
              <div className="flex items-center gap-2 mt-3">
                <Clock size={14} className="text-green-200" />
                <span className="text-green-200 text-xs">最后检查：刚刚</span>
              </div>
            </div>
          </div>
          <button className="w-full mt-4 py-3 bg-white/20 hover:bg-white/30 rounded-xl font-medium transition-colors">
            检查更新
          </button>
        </div>

        {/* Update Channels */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">更新渠道</h3>
          <div className="space-y-2">
            {UPDATE_CHANNELS.map(channel => {
              const Icon = channel.icon;
              const isSelected = selectedChannel === channel.id;
              return (
                <button
                  key={channel.id}
                  onClick={() => setSelectedChannel(channel.id)}
                  className={`w-full flex items-center justify-between p-3 rounded-xl border-2 transition-all text-left ${
                    isSelected
                      ? 'border-green-500 bg-green-50'
                      : 'border-gray-100 hover:border-gray-200 hover:bg-gray-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg" style={{ backgroundColor: `${channel.color}20` }}>
                      <Icon size={20} style={{ color: channel.color }} />
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-900">{channel.name}</span>
                      <p className="text-xs text-gray-500">{channel.desc}</p>
                    </div>
                  </div>
                  {isSelected && <Check size={20} className="text-green-500" />}
                </button>
              );
            })}
          </div>
        </div>

        {/* Update Settings */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">更新选项</h3>
          <div className="space-y-2">
            {UPDATE_SETTINGS.map(setting => (
              <div
                key={setting.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-xl"
              >
                <span className="text-sm text-gray-700">{setting.name}</span>
                <div
                  className={`w-12 h-6 rounded-full transition-colors ${
                    setting.enabled ? 'bg-green-500' : 'bg-gray-300'
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
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">v3.2.0 更新内容</h3>
          <div className="space-y-2 text-sm">
            <div className="flex items-start gap-2">
              <span className="text-green-500 font-bold mt-0.5">•</span>
              <span className="text-gray-600">全新 LUT 资源下载功能，20+ 专业滤镜</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-green-500 font-bold mt-0.5">•</span>
              <span className="text-gray-600">哈苏色彩科学升级，HNCS 3.0 自然色彩</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-green-500 font-bold mt-0.5">•</span>
              <span className="text-gray-600">哈苏之眼增强，支持 50+ 精细场景</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-green-500 font-bold mt-0.5">•</span>
              <span className="text-gray-600">优化性能，启动速度提升 30%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default UpdateChannelPage;
