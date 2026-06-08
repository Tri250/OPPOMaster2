import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Shield } from 'lucide-react';

const PRIVACY_TEXT = `哈苏相机助手隐私政策

生效日期：2025年1月1日
最后更新：2025年6月1日

一、信息收集

我们收集以下类型的信息以提供和改善服务：

1. 账户信息：当您注册账户时，我们会收集您的用户名、电子邮件地址和密码。
2. 设备信息：我们会收集设备型号、操作系统版本和唯一设备标识符，以优化应用性能。
3. 使用数据：我们收集关于您如何使用应用的信息，包括功能使用频率、崩溃报告和性能指标。
4. 照片元数据：当您使用相机功能时，我们会处理照片的 EXIF 数据（如 ISO、快门速度、光圈等），以提供参数优化建议。

二、信息使用

我们使用收集的信息用于：
- 提供和维护我们的服务
- 改善用户体验和应用性能
- 发送技术通知和更新信息
- 响应客户服务请求
- 监控和分析使用趋势

三、信息共享

我们不会出售您的个人信息。我们仅在以下情况下共享信息：
- 获得您的明确同意
- 遵守法律义务
- 保护我们的权利和安全

四、数据安全

我们采用行业标准的安全措施保护您的信息：
- SSL/TLS 加密传输
- AES-256 加密存储
- 定期安全审计
- 访问权限控制

五、您的权利

您有权：
- 访问您的个人数据
- 更正不准确的信息
- 删除您的账户和数据
- 撤回数据使用同意
- 导出您的数据

六、Cookie 政策

我们使用 Cookie 和类似技术来：
- 记住您的偏好设置
- 分析应用使用情况
- 提供个性化内容

七、第三方服务

本应用可能包含第三方服务链接，这些服务有各自的隐私政策。我们建议您阅读这些政策。

八、儿童隐私

我们的服务不面向 13 岁以下儿童。我们不会故意收集儿童的个人信息。

九、政策更新

我们可能会不时更新本隐私政策。重大变更将通过应用内通知或电子邮件告知您。

十、联系我们

如果您对本隐私政策有任何疑问，请通过以下方式联系我们：
- 电子邮件：privacy@hasselblad-assistant.com
- 客服热线：400-888-8888`;

const PrivacyPage: React.FC = () => {
  const { goBack } = useAppStore();

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
            <h1 className="text-lg font-bold">隐私政策</h1>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        {/* 标识 */}
        <div
          className="rounded-2xl p-4 flex items-center gap-3 mb-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-accent-primary-muted)', border: '1px solid var(--color-border-accent)' }}
        >
          <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: 'var(--color-accent-primary)' }}>
            <Shield size={20} style={{ color: '#fff' }} />
          </div>
          <div>
            <p className="text-sm font-bold" style={{ color: 'var(--color-accent-primary)' }}>隐私保护承诺</p>
            <p className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>我们重视并保护您的个人隐私</p>
          </div>
        </div>

        {/* 最后更新日期 */}
        <p className="text-xs mb-4" style={{ color: 'var(--color-text-tertiary)' }}>
          最后更新日期：2025年6月1日
        </p>

        {/* 隐私政策文本 */}
        <div
          className="rounded-2xl p-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '60ms' }}
        >
          <div className="text-sm leading-relaxed whitespace-pre-wrap" style={{ color: 'var(--color-text-secondary)' }}>
            {PRIVACY_TEXT}
          </div>
        </div>
      </div>
    </div>
  );
};

export default React.memo(PrivacyPage);
