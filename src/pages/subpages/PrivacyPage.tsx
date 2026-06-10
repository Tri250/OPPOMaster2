import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Shield, Lock, Eye, FileText, ExternalLink } from 'lucide-react';

const PrivacyPage: React.FC = () => {
  const { goBack } = useAppStore();

  const sections = [
    {
      icon: Shield,
      title: '数据保护',
      content: '我们采用行业标准的加密技术保护您的数据安全。所有预设参数和设置都存储在本地，不会上传到服务器。',
    },
    {
      icon: Lock,
      title: '权限说明',
      content: '本应用需要相机权限以提供实时预览功能，存储权限用于保存预设参数。我们不会访问您的个人文件。',
    },
    {
      icon: Eye,
      title: '隐私政策',
      content: '我们尊重您的隐私，不会收集、存储或分享您的个人信息。应用分析数据均为匿名化处理。',
    },
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
        <h1 className="text-lg font-bold text-white">隐私政策</h1>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/10 to-transparent border border-[#FF6B35]/20 mb-6">
          <div className="flex items-center gap-3 mb-2">
            <Shield size={24} className="text-[#FF6B35]" />
            <span className="text-white font-medium">您的隐私对我们很重要</span>
          </div>
          <p className="text-white/60 text-sm">
            我们致力于保护您的隐私和数据安全。请阅读以下内容了解我们如何处理您的信息。
          </p>
        </div>

        <div className="space-y-4">
          {sections.map((section, index) => {
            const Icon = section.icon;
            return (
              <div key={index} className="p-4 rounded-2xl bg-white/5">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center">
                    <Icon size={20} className="text-white/70" />
                  </div>
                  <h3 className="text-white font-medium">{section.title}</h3>
                </div>
                <p className="text-white/50 text-sm leading-relaxed">
                  {section.content}
                </p>
              </div>
            );
          })}
        </div>

        {/* Links */}
        <div className="mt-6 space-y-2">
          <button className="w-full p-4 rounded-2xl bg-white/5 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <FileText size={20} className="text-white/60" />
              <span className="text-white text-sm">完整隐私政策</span>
            </div>
            <ExternalLink size={16} className="text-white/40" />
          </button>
          <button className="w-full p-4 rounded-2xl bg-white/5 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <FileText size={20} className="text-white/60" />
              <span className="text-white text-sm">用户协议</span>
            </div>
            <ExternalLink size={16} className="text-white/40" />
          </button>
        </div>

        {/* Version */}
        <div className="mt-8 text-center">
          <p className="text-white/30 text-xs">最后更新：2026年6月</p>
          <p className="text-white/20 text-xs mt-1">版本 1.3.1</p>
        </div>
      </div>
    </div>
  );
};

export default PrivacyPage;
