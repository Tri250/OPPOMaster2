# OMaster CI/CD 完整指南

> **版本**: v1.0  
> **更新日期**: 2026-06-17  
> **状态**: ✅ 已配置完成

---

## 目录

1. [CI/CD 架构概览](#架构概览)
2. [工作流说明](#工作流说明)
3. [触发方式](#触发方式)
4. [构建跟踪与闭环](#构建跟踪)
5. [故障排查](#故障排查)
6. [最佳实践](#最佳实践)

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                    CI/CD 架构图                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │   Push Tag  │    │   PR/Merge  │    │   Manual    │     │
│  │   (v1.x.x)  │    │  (main/dev) │    │  Trigger    │     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘     │
│         │                  │                  │            │
│         ▼                  ▼                  ▼            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              GitHub Actions Runners                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │   │
│  │  │ Main Release│  │  CI Build   │  │Beta Release │  │   │
│  │  │   Workflow  │  │   & Test    │  │  Workflow   │  │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │   │
│  │         │                │                │         │   │
│  │         └────────────────┼────────────────┘         │   │
│  │                          ▼                          │   │
│  │              ┌─────────────────────┐                │   │
│  │              │   Build Tracker     │                │   │
│  │              │   (Monitor & Track) │                │   │
│  │              └──────────┬──────────┘                │   │
│  │                         │                           │   │
│  │         ┌───────────────┼───────────────┐           │   │
│  │         ▼               ▼               ▼           │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐   │   │
│  │  │   Report    │ │   Notify    │ │   Archive   │   │   │
│  │  │  Generate   │ │   Failure   │ │  Artifacts  │   │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘   │   │
│  └─────────────────────────────────────────────────────┘   │
│                            │                               │
│                            ▼                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    输出产物                          │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐            │   │
│  │  │  GitHub  │ │  Build   │ │  Issue   │            │   │
│  │  │ Release  │ │  Report  │ │  (Fail)  │            │   │
│  │  └──────────┘ └──────────┘ └──────────┘            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 工作流说明

### 1. Main Release (`main-release.yml`)

**用途**: 正式版本发布

**触发条件**:
- 推送标签 `v*.*.*` 或 `v*.*.*-rc*`
- 手动触发 (workflow_dispatch)

**功能**:
- 多架构 APK 构建 (armeabi-v7a, arm64-v8a, x86, x86_64, universal)
- 自动版本号同步 (从 Git Tag)
- 自动生成 Release Notes (git-cliff)
- 自动创建 GitHub Release
- 构建报告生成

**输出**:
- `release-apk-universal`
- `release-apk-arm64-v8a` (推荐)
- `release-apk-armeabi-v7a`
- `release-apk-x86`
- `release-apk-x86_64`
- `build-reports` (包含 mapping 文件)

---

### 2. CI Build & Test (`ci-build.yml`)

**用途**: 持续集成验证

**触发条件**:
- Push 到 main/master/develop 分支
- Pull Request 到 main/master

**功能**:
- 代码质量检查 (ktlint, Android Lint)
- 单元测试
- 测试覆盖率报告
- Debug APK 构建
- Release APK 构建 (main 分支)
- 依赖安全检查

**Jobs**:
| Job | 说明 | 依赖 |
|-----|------|------|
| `code-quality` | Kotlin 代码风格、Android Lint | - |
| `unit-tests` | 运行单元测试 | code-quality |
| `coverage` | 生成测试覆盖率报告 | unit-tests |
| `build-debug` | 构建 Debug APK | code-quality, unit-tests |
| `build-release` | 构建 Release APK | code-quality, unit-tests |
| `dependency-check` | 依赖版本检查 | - |

---

### 3. Build Beta & Release (`beta-release.yml`)

**用途**: Beta 版本快速发布

**触发条件**:
- Push 到 main/master
- Pull Request
- 手动触发

**功能**:
- 灵活的构建类型选择 (Debug/Release)
- 可选自动创建 Release
- 快速迭代测试

---

### 4. Build Tracker & Monitor (`build-tracker.yml`)

**用途**: 构建跟踪与闭环监控

**触发条件**:
- 其他工作流完成时自动触发

**功能**:
- 跟踪所有构建状态
- 生成构建报告
- 构建失败时自动创建 Issue
- 构建产物归档清单
- 构建状态徽章更新

---

## 触发方式

### 方式 1: 推送标签 (推荐用于正式版)

```bash
# 1. 确保代码已提交
git add .
git commit -m "release: 准备发布 v1.3.1"

# 2. 创建标签
git tag v1.3.1

# 3. 推送标签 (自动触发构建)
git push origin v1.3.1
```

**CI 行为**:
- 自动提取版本号 `1.3.1`
- 构建所有架构 APK
- 自动生成 Release Notes
- 创建 GitHub Release

---

### 方式 2: 手动触发 (推荐用于测试)

1. 访问仓库页面
2. 点击 **Actions** 标签
3. 选择 **Main Release** 工作流
4. 点击 **Run workflow**
5. 填写参数:
   - `release_tag`: `v1.3.1-beta1`
   - `release_name`: `v1.3.1 Beta 1` (可选)
   - `prerelease`: 勾选表示预发布

---

### 方式 3: PR 自动验证

创建 Pull Request 到 `main` 分支时自动触发:
- 代码质量检查
- 单元测试
- Debug APK 构建

---

## 构建跟踪

### 构建状态跟踪

Build Tracker 会自动监控所有工作流的执行:

```
┌────────────────────────────────────────────────────┐
│                 构建跟踪流程                        │
├────────────────────────────────────────────────────┤
│                                                    │
│  1. 工作流完成 ────────────────────────────────┐  │
│     (Main Release / CI Build / Beta Release)   │  │
│                                                │  │
│  2. Build Tracker 触发                         │  │
│     - 获取构建信息                             │  │
│     - 计算构建时长                             │  │
│     - 收集产物列表                             │  │
│                                                │  │
│  3. 分支判断                                   │  │
│     ├─ 成功 ──► 生成报告 ──► 归档产物          │  │
│     └─ 失败 ──► 创建 Issue ──► 通知相关人员    │  │
│                                                │  │
│  4. 闭环完成                                   │  │
│     - 报告上传                                 │  │
│     - 状态更新                                 │  │
│                                                │  │
└────────────────────────────────────────────────────┘
```

### 构建报告内容

每个构建完成后会生成报告:

```markdown
# 构建跟踪报告

**生成时间**: 2026-06-17 10:30:00 UTC

## 构建信息

- **工作流**: Main Release
- **运行编号**: #42
- **状态**: success
- **分支**: main
- **提交**: a1b2c3d
- **提交信息**: release: 准备发布 v1.3.1
- **作者**: Developer
- **触发者**: Developer
- **构建时长**: 180秒
- **构建链接**: [查看详情](https://github.com/...)

## 构建产物

- release-apk-universal (25MB)
- release-apk-arm64-v8a (18MB)
- release-apk-armeabi-v7a (15MB)
- build-reports (2MB)
```

### 失败闭环处理

构建失败时自动执行:

1. **创建 Issue**:
   - 标题: `❌ 构建失败: Main Release #42`
   - 标签: `build-failure`, `automated`
   - 内容: 包含构建链接、失败分析、修复建议

2. **避免重复**:
   - 检查是否已存在相同的 open issue
   - 存在则跳过创建

3. **手动关闭**:
   - 修复代码后重新推送
   - 构建成功后手动关闭 Issue

---

## 故障排查

### 常见问题

#### 1. 构建超时

**症状**: 构建在 `Build Release APK` 步骤超时

**解决方案**:
```yaml
# 在 job 级别添加超时配置
build:
  runs-on: ubuntu-latest
  timeout-minutes: 30  # 增加超时时间
```

#### 2. 签名失败

**症状**: `Release 签名未配置`

**解决方案**:
- CI 环境自动使用 debug 签名回退
- 生产发布需要在 Secrets 中配置签名:
  - `RELEASE_STORE_FILE`
  - `RELEASE_STORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`

#### 3. 依赖下载失败

**症状**: `Connect to maven.aliyun.com:443 failed`

**解决方案**:
- CI 环境网络正常，通常不会出现此问题
- 如发生，重试构建即可

#### 4. 版本号未同步

**症状**: Release 中版本号与 Tag 不一致

**解决方案**:
- 确保 Tag 格式为 `vX.Y.Z`
- 检查 `sed` 命令是否成功执行

---

### 调试技巧

#### 查看详细日志

1. 进入 Actions 页面
2. 点击失败的 workflow run
3. 点击失败的 job
4. 展开失败的步骤查看日志

#### 启用调试模式

```yaml
# 在 workflow 中添加
env:
  ACTIONS_STEP_DEBUG: true
  ACTIONS_RUNNER_DEBUG: true
```

#### 本地模拟 CI 构建

```bash
# 使用相同的 Gradle 参数
./gradlew assembleRelease --no-daemon --stacktrace
```

---

## 最佳实践

### 1. 版本号管理

```
版本号格式: v{主版本}.{次版本}.{修订版本}

示例:
- v1.0.0    # 正式版
- v1.0.1    # 补丁修复
- v1.1.0    # 功能更新
- v2.0.0    # 重大更新
- v1.0.0-rc1 # 候选版本
- v1.0.0-beta1 # 测试版本
```

### 2. 分支策略

```
main/master     ──────── 稳定版本，可发布
    │
    ▼
develop/feature ──────── 开发分支，PR 验证
    │
    ▼
release/v1.x    ──────── 发布分支，版本准备
```

### 3. 发布流程

```
1. 在 develop 分支完成功能开发
        │
        ▼
2. 创建 PR 合并到 main
        │
        ▼
3. CI 自动验证 (代码检查、测试、构建)
        │
        ▼
4. PR 合并后，创建 Tag
   git tag v1.3.1
   git push origin v1.3.1
        │
        ▼
5. CI 自动构建并发布 Release
        │
        ▼
6. Build Tracker 生成报告
        │
        ▼
7. ✅ 发布完成，闭环结束
```

### 4. 产物管理

| 产物类型 | 保留时间 | 用途 |
|---------|---------|------|
| APK 文件 | 30 天 | 测试分发 |
| Build Report | 90 天 | 构建追溯 |
| Mapping 文件 | 永久 | Crash 反混淆 |
| Test Report | 7 天 | 测试分析 |

### 5. 安全建议

- ✅ 签名密钥存储在 GitHub Secrets
- ✅ AppKey 使用 XOR+Base64 混淆
- ✅ ProGuard 启用代码混淆
- ✅ Release 构建移除日志
- ❌ 不要将密钥提交到代码仓库

---

## 相关链接

- [GitHub Actions 文档](https://docs.github.com/cn/actions)
- [工作流文件](../.github/workflows/)
- [构建状态](../../actions)
- [Release 页面](../../releases)

---

## 更新日志

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-06-17 | v1.0 | 初始版本，完整 CI/CD 配置 |
