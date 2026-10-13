# SECURITY.md

## 安全政策

### 报告漏洞

如发现安全问题（权限滥用、数据泄露、可被利用的崩溃、更新链路劫持等），请**不要**在公开 Issue 中披露：

1. 使用 [Private vulnerability reporting](https://github.com/iCurrer/OMaster/security/advisories/new) 私密上报；或
2. 通过仓库主页联系方式联系维护者。

我们会在 72 小时内确认收到，修复后统一致谢。

### 安全设计要点（ED-01 / ED-05）

- **签名密钥**：Release 签名密钥只通过 GitHub Secrets（`OMASTER_KEYSTORE_BASE64` / `OMASTER_KEYSTORE_PASSWORD` / `OMASTER_KEYSTORE_ALIAS` / `OMASTER_KEY_PASSWORD`）注入 CI，任何情况下不提交到仓库（`.signing/` 已被 .gitignore 排除）；
- **统计与上报**：友盟统计受应用内总开关约束，拒绝即完全静默；应用不采集、不上报用户预设内容；
- **订阅内容**：订阅与导入内容入库前经过统一校验（非法 JSON / 缺字段 / 超范围参数会被拒绝），内容来源为社区公开仓库；
- **应用内自更新**：更新包下载后交由系统安装器安装，更新源以官方 Release 页为准。

### 支持版本

仅对最新正式版本提供安全修复。
