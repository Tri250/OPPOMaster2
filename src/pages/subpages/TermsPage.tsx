import React from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, FileText, Shield, Users, Globe,
  Clock, Lock, CheckCircle
} from 'lucide-react';

const TermsPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();

  return (
    <div className="h-full w-full bg-[#0a0a0a] flex flex-col overflow-hidden">
      {/* Header */}
      <div className="bg-[#0a0a0a] border-b border-white/5 px-4 py-3 flex items-center gap-3">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-white/10 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <div className="flex items-center gap-2">
          <FileText size={20} className="text-[#FF6B35]" />
          <h1 className="text-lg font-semibold text-white">用户协议</h1>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Last Updated */}
        <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
          <div className="flex items-center gap-2 text-sm text-white/60">
            <Clock size={16} className="text-white/40" />
            <span>最后更新：2026年6月1日</span>
          </div>
        </div>

        {/* Agreement Summary */}
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

        {/* Key Terms */}
        <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">重要条款</h3>
          <div className="space-y-4">
            <div className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
              <div className="p-2 bg-[#FF6B35]/20 rounded-lg">
                <Users size={18} className="text-[#FF6B35]" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-white">用户责任</h4>
                <p className="text-xs text-white/50 mt-1">您需要对自己的账户安全和使用行为负责</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
              <div className="p-2 bg-[#4CAF50]/20 rounded-lg">
                <Shield size={18} className="text-[#4CAF50]" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-white">知识产权</h4>
                <p className="text-xs text-white/50 mt-1">应用内所有内容均受知识产权法保护</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
              <div className="p-2 bg-[#2196F3]/20 rounded-lg">
                <Globe size={18} className="text-[#2196F3]" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-white">服务范围</h4>
                <p className="text-xs text-white/50 mt-1">我们致力于提供稳定、高质量的摄影工具服务</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
              <div className="p-2 bg-[#9C27B0]/20 rounded-lg">
                <Lock size={18} className="text-[#9C27B0]" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-white">隐私保护</h4>
                <p className="text-xs text-white/50 mt-1">我们严格保护您的个人信息和隐私安全</p>
              </div>
            </div>
          </div>
        </div>

        {/* Full Terms */}
        <div className="bg-[#1a1a1a] rounded-2xl p-4 border border-white/5">
          <h3 className="text-sm font-semibold text-white mb-3">完整协议</h3>
          <div className="space-y-4 text-sm text-white/60">
            <div>
              <h4 className="font-medium text-white mb-2">1. 服务条款</h4>
              <p className="text-xs leading-relaxed">
                本应用提供专业的摄影后期处理工具，包括但不限于 哈苏之眼、色彩调整、滤镜效果、水印添加等功能。我们将持续更新和优化服务，为用户提供更好的使用体验。
              </p>
            </div>
            <div>
              <h4 className="font-medium text-white mb-2">2. 用户账户</h4>
              <p className="text-xs leading-relaxed">
                用户需要注册账户才能使用部分功能。请妥善保管账户信息，对账户下的所有行为负责。如发现账户异常，请立即联系我们。
              </p>
            </div>
            <div>
              <h4 className="font-medium text-white mb-2">3. 用户内容</h4>
              <p className="text-xs leading-relaxed">
                用户上传和处理的图片内容归用户所有。我们不会在未经许可的情况下使用或分享您的图片内容。
              </p>
            </div>
            <div>
              <h4 className="font-medium text-white mb-2">4. 禁止行为</h4>
              <p className="text-xs leading-relaxed">
                禁止利用本应用从事任何违法活动，禁止传播恶意代码，禁止攻击或干扰服务正常运行。
              </p>
            </div>
            <div>
              <h4 className="font-medium text-white mb-2">5. 免责声明</h4>
              <p className="text-xs leading-relaxed">
                我们尽力保证服务稳定运行，但不对因不可抗力或技术原因导致的服务中断承担责任。
              </p>
            </div>
            <div>
              <h4 className="font-medium text-white mb-2">6. 协议修改</h4>
              <p className="text-xs leading-relaxed">
                我们保留随时修改本协议的权利，修改后的协议将在应用内公布，继续使用即表示您同意修改后的协议。
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TermsPage;
