# 沙箱离线构建解决方案

**日期**: 2026-06-17  
**状态**: 部分解决 - 需要网络环境完成首次构建

---

## 问题分析

沙箱环境网络受限，无法从 Maven 仓库下载 AGP 8.7.3 和 Kotlin 2.1.20 的传递依赖。AGP 的传递依赖链非常深，包含 100+ 个依赖。

### 已尝试的解决方案

#### 1. 本地 Maven 仓库预填充 ✅ (部分成功)

创建了以下脚本预填充本地仓库：
- `setup-offline-deps.sh` - 基础 AGP 依赖
- `setup-offline-deps-extended.sh` - 扩展依赖
- `setup-all-kotlin-deps.sh` - Kotlin 插件依赖
- `setup-complete-deps.sh` - 完整依赖集

**已安装的依赖类别**:
- ✅ Android Tools Build (gradle, builder, aapt2 等)
- ✅ Kotlin Gradle Plugin 核心
- ✅ Google Guava, Gson, Protobuf
- ✅ Apache Commons, HttpComponents
- ✅ Bouncy Castle 加密库
- ✅ gRPC, Netty 网络库
- ✅ ASM 字节码操作
- ✅ 以及其他 50+ 个依赖

**本地仓库位置**: `/workspace/local-maven-repo`

#### 2. Gradle 配置优化 ✅

修改了 `settings.gradle.kts`:
- 本地仓库优先级最高
- 保留外部仓库作为 fallback

#### 3. 尝试的离线构建 ⚠️

```bash
gradle clean assembleRelease --offline --no-daemon
```

**结果**: 仍缺少部分深层传递依赖

---

## 推荐的最终解决方案

### 方案 1: GitHub Actions CI (强烈推荐)

项目已配置完整的 CI/CD 流程，网络环境完善。

**触发方式**:
```bash
# 推送标签自动构建
git tag v1.3.1
git push origin v1.3.1
```

**或手动触发**:
访问 https://github.com/Tri250/OPPOMaster2/actions/workflows/main-release.yml

**CI 输出**:
- `app-arm64-v8a-release.apk` - ARM64 推荐版
- `app-universal-release.apk` - 通用版
- 以及其他架构版本

### 方案 2: 预下载依赖包

在有网络的环境中执行以下命令，然后将 Gradle 缓存打包：

```bash
# 1. 在联网机器上执行
./gradlew dependencies --configuration classpath > deps.txt

# 2. 下载所有依赖
./gradlew build --refresh-dependencies

# 3. 打包 Gradle 缓存
tar czf gradle-cache.tar.gz ~/.gradle/caches/modules-2/files-2.1/

# 4. 在沙箱中解压
mkdir -p ~/.gradle/caches/modules-2/files-2.1/
tar xzf gradle-cache.tar.gz -C ~/.gradle/caches/modules-2/files-2.1/
```

### 方案 3: 使用 Docker 构建

创建包含所有依赖的 Docker 镜像：

```dockerfile
FROM gradle:8.14.4-jdk17

# 预下载所有依赖
COPY . /project
RUN cd /project && gradle dependencies --configuration classpath

# 构建
WORKDIR /project
CMD ["gradle", "assembleRelease"]
```

### 方案 4: 降级 AGP 版本 (不推荐)

降级到本地仓库已有完整依赖的 AGP 版本（如 7.x），但会丢失新特性。

---

## 本地仓库已安装的依赖清单

```
local-maven-repo/
├── com/android/
│   ├── application/com.android.application.gradle.plugin/8.7.3
│   ├── databinding/baseLibrary/8.7.3
│   ├── tools/build/
│   │   ├── gradle/8.7.3
│   │   ├── builder/8.7.3
│   │   ├── aaptcompiler/8.7.3
│   │   └── ...
│   └── ...
├── org/jetbrains/kotlin/
│   ├── kotlin-gradle-plugin/2.1.20
│   ├── kotlin-gradle-plugin-api/2.1.20
│   ├── kotlin-compiler-embeddable/2.1.20
│   └── ...
├── com/google/guava/guava/32.0.1-jre
├── io/grpc/grpc-*/1.57.0
├── io/netty/netty-*/4.1.93.Final
└── ... (50+ 其他依赖)
```

---

## 结论

**当前状态**: 本地仓库已预填充大部分依赖，但 AGP 8.7.3 的完整传递依赖链太深（100+），手动维护不现实。

**建议**: 
1. **首选**: 使用 GitHub Actions CI 进行构建
2. **备选**: 在联网环境中预下载 Gradle 缓存，然后导入沙箱

**相关文件**:
- [CI 配置](.github/workflows/main-release.yml)
- [构建文档](docs/BUILD_RELEASE.md)
- [依赖安装脚本](setup-offline-deps.sh)
