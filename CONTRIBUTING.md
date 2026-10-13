# 贡献指南（CONTRIBUTING）

感谢关注 OMaster！请先阅读 [README](README.md) 与 [行为准则](SECURITY.md)。

## 分工边界

- **主仓库（本仓库）**：应用代码、CI、发布；
- **[OMaster-Community](https://github.com/iCurrer/OMaster-Community)**：预设 JSON、样片图片、订阅源。**预设投稿请到社区仓库**，不要把样片图片提交到本仓库（ED-04 体积治理约束：单图 ≤ 500KB，APK 目标 ≤ 15MB）。

## 开发流程

1. 从 `master` 拉出功能分支（`feat/xxx` / `fix/xxx`）；
2. 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)（`feat:` / `fix:` / `docs:` / `chore:`），发布说明由 git-cliff 自动生成；
3. PR 合并前 CI 必须全绿：`testDebugUnitTest` + `lint` + 版本校验（门禁不可绕过）。

## 质量约定（2.0 架构红线摘要）

- 一切外部内容（导入 / 订阅）必须经过统一校验管道入库（`CustomPresetManager`）；
- 悬浮窗 / 详情页只读消费预设展示模型，不直接读写存储；
- 一切统计与上报受 `isAnalyticsEnabled` 总开关约束；
- 品牌 / 机型差异一律数据化（预设 JSON 元数据），业务代码不出现机型特判。

## 发布流程（维护者）

1. 确认 `app/build.gradle.kts` 中 `versionName` 为目标版本；
2. 在 GitHub **Settings → Secrets → Actions** 配置签名密钥：
   - `OMASTER_KEYSTORE_BASE64`：keystore 文件的 base64（`base64 -w0 omaster.keystore`）
   - `OMASTER_KEYSTORE_PASSWORD`：keystore 密码
   - `OMASTER_KEYSTORE_ALIAS`：别名（缺省 `omaster`）
   - `OMASTER_KEY_PASSWORD`：key 密码（缺省同 keystore 密码）
3. 推送 tag `vX.Y.Z`（必须与 `versionName` 一致，否则 CI 拒绝），或手动触发 workflow 勾选创建 Release；
4. CI 流程：质量门禁 → 版本校验 → 签名构建 → 产物签名复核 → 草稿 Release（git-cliff 自动生成说明）；
5. **真机验收草稿后手动发布**，并同步蒲公英 / 蓝奏云镜像。

> 注意：`versionCode` 由 CI 按提交数自动注入（提交数 + 100），不要手改 `versionCode`。

## Issue

- 报 bug 请用「问题反馈」模板，**导入类问题**请附：预设来源、JSON 片段（脱敏后）、失败提示原文；
- 功能建议请用「功能建议」模板。
