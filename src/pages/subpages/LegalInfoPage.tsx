import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft,
  Shield,
  Lock,
  Eye,
  FileText,
  Users,
  Globe,
  Clock,
  CheckCircle,
  ExternalLink
} from 'lucide-react';

type TabType = 'privacy' | 'terms';

const privacySections = [
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

const termsSections = [
  {
    icon: Users,
    title: '用户责任',
    content: '您需要对自己的账户安全和使用行为负责',
  },
  {
    icon: Shield,
    title: '知识产权',
    content: '应用内所有内容均受知识产权法保护',
  },
  {
    icon: Globe,
    title: '服务范围',
    content: '我们致力于提供稳定、高质量的摄影工具服务',
  },
  {
    icon: Lock,
    title: '隐私保护',
    content: '我们严格保护您的个人信息和隐私安全',
  },
];

const termsContent = [
  {
    title: '1. 服务条款',
    content: '本应用提供专业的摄影后期处理工具，包括但不限于 哈苏之眼、色彩调整、滤镜效果、水印添加等功能。我们将持续更新和优化服务，为用户提供更好的使用体验。',
  },
  {
    title: '2. 用户账户',
    content: '用户需要注册账户才能使用部分功能。请妥善保管账户信息，对账户下的所有行为负责。如发现账户异常，请立即联系我们。',
  },
  {
    title: '3. 用户内容',
    content: '用户上传和处理的图片内容归用户所有。我们不会在未经许可的情况下使用或分享您的图片内容。',
  },
  {
    title: '4. 禁止行为',
    content: '禁止利用本应用从事任何违法活动，禁止传播恶意代码，禁止攻击或干扰服务正常运行。',
  },
  {
    title: '5. 免责声明',
    content: '我们尽力保证服务稳定运行，但不对因不可抗力或技术原因导致的服务中断承担责任。',
  },
  {
    title: '6. 协议修改',
    content: '我们保留随时修改本协议的权利，修改后的协议将在应用内公布，继续使用即表示您同意修改后的协议。',
  },
];

const LegalInfoPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();
  const [activeTab, setActiveTab] = useState<TabType>('privacy');

  const renderPrivacy = () => (
    <div className="space-y-4 animate-in fade-in duration-200">
      <div className="p-4 rounded-2xl bg-gradient-to-br from-[#FF6B35]/10 to-transparent border border-[#FF6B35]/20">
        <div className="flex items-center gap-3 mb-2">
          <Shield size={24} className="text-[#FF6B35]" />
          <span className="text-white font-medium">您的隐私对我们很重要</span>
        </div>
        <p className="text-white/60 text-sm">
          我们致力于保护您的隐私和数据安全。请阅读以下内容了解我们如何处理您的信息。
        </p>
      </div>

      {privacySections.map((section, index) => {
        const Icon = section.icon;
        return (
          <div key={index} className="p-4 rounded-2xl bg-white/5">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center">
                <Icon size={20} className="text-white/70" />
              </div>
              <h3 className="text-white font-medium">{section.title}</h3>
            </div>
            <p className="text-white/50 text-sm leading-relaxed">{section.content}</p>
          </div>
        );
      })}

      <div className="mt-6 space-y-2">
        <button className="w-full p-4 rounded-2xl bg-white/5 flex items-center justify-between active:scale-[0.98] transition-transform">
          <div className="flex items-center gap-3">
            <FileText size={20} className="text-white/60" />
            <span className="text-white text-sm">完整隐私政策</span>
          </div>
          <ExternalLink size={16} className="text-white/40" />
        </button>
        <button className="w-full p-4 rounded-2xl bg-white/5 flex items-center justify-between active:scale-[0.98] transition-transform">
          <div className="flex items-center gap-3">
            <FileText size={20} className="text-white/60" />
            <span className="text-white text-sm">用户协议</span>
          </div>
          <ExternalLink size={16} className="text-white/40" />
        </button>
      </div>

      <div className="mt-8 text-center">
        <p className="text-white/30 text-xs">最后更新：2026年6月</p>
        <p className="text-white/20 text-xs mt-1">版本 1.3.1</p>
      </div>
    </div>
  );

  const renderTerms = () => (
    <div className="space-y-4 animate-in fade-in duration-200">
      <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
        <div className="flex items-center gap-2 text-sm text-white/60">
          <Clock size={16} className="text-white/40" />
          <span>最后更新：2026年6月1日</span>
        </div>
      </div>

      <div className="bg-gradient-to-br from-[#FF6B35] to-[#FF8C42] rounded-2xl p-5 text-white">
        <div className="flex items-start gap-4">
          <div className="p-3 bg-white/20 rounded-xl">
            <CheckCircle size={32} className="text-white" />
          </div>
          <div className="flex-1">
            <h2 className="text-xl font-bold mb-2">欢迎使用我们的服务</h2>
            <p className="text-white/80 text-sm">
              使用我们的应用即表示您同意本用户协议和隐私政策。请仔细阅读以下内容。
            </p>
          </div>
        </div>
      </div>

      <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
        <h3 className="text-sm font-semibold text-white mb-3">重要条款</h3>
        <div className="space-y-3">
          {termsSections.map((section, index) => {
            const Icon = section.icon;
            const colors = ['#FF6B35', '#4CAF50', '#2196F3', '#9C27B0'];
            return (
              <div key={index} className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
                <div className="p-2 rounded-lg flex-shrink-0" style={{ backgroundColor: `${colors[index]}20` }}>
                  <Icon size={18} style={{ color: colors[index] }} />
                </div>
                <div>
                  <h4 className="text-sm font-medium text-white">{section.title}</h4>
                  <p className="text-xs text-white/50 mt-1">{section.content}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
        <h3 className="text-sm font-semibold text-white mb-3">完整协议</h3>
        <div className="space-y-4 text-sm text-white/60">
          {termsContent.map((item, index) => (
            <div key={index}>
              <h4 className="font-medium text-white mb-2 text-sm">{item.title}</h4>
              <p className="text-xs leading-relaxed">{item.content}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  return (
    <div className="h-full w-full bg-[#0a0a0a] flex flex-col overflow-hidden">
      {/* Header */}
      <div className="bg-[#0a0a0a] border-b border-white/5 px-4 py-3 flex items-center gap-3 shrink-0">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-white/10 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-semibold text-white">法律信息</h1>
      </div>

      {/* Tabs */}
      <div className="px-4 pt-4 pb-2 shrink-0">
        <div className="flex p-1 rounded-xl bg-white/5">
          {[
            { id: 'privacy', label: '隐私政策' },
            { id: 'terms', label: '用户协议' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as TabType)}
              className={`flex-1 py-2 rounded-lg text-sm font-medium transition-all ${
                activeTab === tab.id
                  ? 'bg-[#FF6B35] text-white'
                  : 'text-white/50 hover:text-white/70'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 pb-6">
        {activeTab === 'privacy' && renderPrivacy()}
        {activeTab === 'terms' && renderTerms()}
      </div>
    </div>
  );
};

export default LegalInfoPage;
