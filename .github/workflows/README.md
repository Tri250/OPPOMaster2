# GitHub Actions 工作流说明

## 工作流概览

### 1. CI Build & Test (`ci-build.yml`)

**触发条件:**
- Push 到 `main`, `master`, `develop` 分支
- Pull Request 到 `main`, `master` 分支

**包含任务:**

#### code-quality (代码质量检查)
- Kotlin 代码风格检查 (ktlint)
- Android Lint 检查
- 编译检查

#### unit-tests (单元测试)
- 运行单元测试
- 上传测试报告

#### coverage (测试覆盖率)
- 生成测试覆盖率报告
- 支持 JaCoCo 覆盖率分析
- 可上传至 Codecov

#### build-debug (构建 Debug APK)
- 构建 Debug 版本 APK
- 上传各架构 APK 产物
  - universal (通用版)
  - armeabi-v7a
  - arm64-v8a

#### build-release (构建 Release APK)
- 仅在 push 到 main/master 分支时触发
- 构建 Release 版本 APK
- 使用自动回退的 debug 签名

#### dependency-check (依赖安全检查)
- 检查依赖版本更新
- 生成依赖报告

### 2. Build Beta & Release (`beta-release.yml`)

**触发条件:**
- Push 到 `main`, `master` 分支
- Pull Request 到 `main`, `master` 分支
- 手动触发 (workflow_dispatch)

**手动触发参数:**
- `create_release`: 是否创建 Release
- `release_tag`: Release 标签 (格式: vX.Y.Z)
- `release_name`: Release 名称
- `prerelease`: 是否标记为预发布版本

**包含任务:**

#### build (构建)
- 根据参数构建 Debug 或 Release APK
- 上传各架构 APK 产物

#### release (发布)
- 生成更新日志 (使用 git-cliff)
- 创建并推送 Git 标签
- 创建 GitHub Release (草稿)
- 上传 APK 到 Release

## 使用指南

### 本地构建

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行单元测试
./gradlew testDebugUnitTest

# 生成覆盖率报告
./gradlew createDebugCoverageReport
```

### 触发自动构建

1. **Push 代码** 到 main/master 分支自动触发 CI
2. **创建 Pull Request** 自动触发代码检查和测试
3. **手动发布**:
   - 进入 Actions 页面
   - 选择 "Build Beta & Release"
   - 点击 "Run workflow"
   - 填写参数创建 Release

### 产物下载

构建完成后，APK 文件可在以下位置下载:
- Actions 页面 → 对应工作流运行 → Artifacts
- Release 页面 (手动发布时)

## 配置说明

### 签名配置

Release 构建使用自动回退机制:
1. 优先使用 `keystore-release.properties` 中的真实签名
2. 如果不存在或无效，回退到 debug 签名

**生产环境发布前请配置真实签名:**
1. 复制 `app/keystore.properties` 为 `app/keystore-release.properties`
2. 填入真实的密钥库信息
3. **不要**将 `keystore-release.properties` 提交到版本控制

### 测试覆盖率

项目在 `buildTypes.debug` 中启用了测试覆盖率:
```kotlin
debug {
    enableUnitTestCoverage = true
    enableAndroidTestCoverage = true
}
```

覆盖率报告生成路径:
- `app/build/reports/coverage/debug/`
- `app/build/reports/jacoco/`

### ABI 拆分配置

项目配置了按 ABI 拆分 APK:
- armeabi-v7a
- arm64-v8a
- x86
- x86_64
- universal (包含所有架构)

## 故障排除

### 构建失败

1. 检查 Gradle 配置是否正确
2. 确认所有依赖都在 `libs.versions.toml` 中定义
3. 查看 Actions 日志获取详细错误信息

### 签名问题

如果 Release 构建失败:
1. 检查 `keystore-release.properties` 是否存在
2. 确认密钥库文件路径正确
3. 验证密钥库密码和别名正确

### 测试失败

1. 本地运行 `./gradlew testDebugUnitTest` 复现问题
2. 检查测试代码是否正确
3. 确认测试资源文件存在

## 版本历史

- v1.0.0: 初始工作流配置
- v1.1.0: 添加测试覆盖率支持
- v1.2.0: 添加多架构 APK 构建
