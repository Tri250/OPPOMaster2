import React from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, FileText } from 'lucide-react';

const TERMS_TEXT = `哈苏相机助手用户协议

生效日期：2025年1月1日
最后更新：2025年6月1日

欢迎使用哈苏相机助手（以下简称"本应用"）。在使用本应用之前，请仔细阅读以下用户协议。

一、服务条款

1.1 本应用由哈苏相机助手团队开发运营，为用户提供专业相机参数优化、AI 场景识别、LUT 滤镜管理等功能。

1.2 您在使用本应用时，必须遵守本协议的所有条款和条件。

1.3 我们保留随时修改或终止服务的权利，恕不另行通知。

二、用户账户

2.1 您可能需要创建账户才能使用某些功能。

2.2 您有责任保管好您的账户信息，对账户下的所有活动负责。

2.3 如发现未经授权使用您账户的情况，请立即通知我们。

三、使用规范

3.1 您同意不会：
- 以任何方式滥用或破坏服务
- 尝试未经授权访问我们的系统
- 使用自动化工具批量操作
- 上传恶意代码或有害内容
- 侵犯他人的知识产权

3.2 您对本应用创建的内容（如自定义预设、水印等）享有所有权。

四、知识产权

4.1 本应用的所有内容，包括但不限于软件、文本、图像、标识和界面设计，均受知识产权法保护。

4.2 哈苏（Hasselblad）及 HNCS 为 Hasselblad Group 的注册商标，经授权使用。

4.3 未经许可，您不得复制、修改、分发或以其他方式使用本应用的任何部分。

五、免责声明

5.1 本应用按"现状"提供，不作任何明示或暗示的保证。

5.2 我们不保证服务将不间断、及时、安全或无错误。

5.3 对于因使用本应用而产生的任何直接、间接、附带或后果性损害，我们不承担责任。

5.4 AI 优化建议仅供参考，实际拍摄效果可能因设备、环境等因素而异。

六、付费服务

6.1 部分高级功能可能需要付费订阅。

6.2 付费订阅按周期自动续费，您可随时取消。

6.3 退款政策遵循相关法律法规。

七、终止

7.1 您可以随时停止使用本应用并删除账户。

7.2 如您违反本协议，我们有权终止您的访问权限。

八、争议解决

8.1 本协议适用中华人民共和国法律。

8.2 因本协议产生的争议，双方应友好协商解决；协商不成的，提交有管辖权的人民法院诉讼解决。

九、其他条款

9.1 本协议构成您与我们之间的完整协议。

9.2 本协议的任何条款如被认定为无效，不影响其他条款的效力。

9.3 我们未行使任何权利不构成对该权利的放弃。

十、联系方式

如有任何问题，请联系：
- 电子邮件：support@hasselblad-assistant.com
- 客服热线：400-888-8888
- 工作时间：周一至周五 9:00 - 18:00`;

const TermsPage: React.FC = () => {
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
            <h1 className="text-lg font-bold">用户协议</h1>
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
            <FileText size={20} style={{ color: '#fff' }} />
          </div>
          <div>
            <p className="text-sm font-bold" style={{ color: 'var(--color-accent-primary)' }}>用户协议</p>
            <p className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>使用本应用即表示您同意本协议</p>
          </div>
        </div>

        {/* 最后更新日期 */}
        <p className="text-xs mb-4" style={{ color: 'var(--color-text-tertiary)' }}>
          最后更新日期：2025年6月1日
        </p>

        {/* 协议文本 */}
        <div
          className="rounded-2xl p-4 animate-liquid-slide-up"
          style={{ background: 'var(--color-bg-secondary)', border: '1px solid var(--color-border-light)', animationDelay: '60ms' }}
        >
          <div className="text-sm leading-relaxed whitespace-pre-wrap" style={{ color: 'var(--color-text-secondary)' }}>
            {TERMS_TEXT}
          </div>
        </div>
      </div>
    </div>
  );
};

export default React.memo(TermsPage);
