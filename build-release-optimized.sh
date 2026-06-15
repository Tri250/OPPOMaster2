#!/bin/bash
# ============================================================================
# OMaster Android Release APK 构建脚本 - 网络优化版
# 解决问题：沙箱环境网络限制，无法下载 Gradle distribution 和依赖
# 策略：
#   1. 使用系统已安装的 Gradle（避免 wrapper 下载）
#   2. 优先使用 local-maven-repo 本地仓库
#   3. 阿里云/腾讯云镜像作为备用
#   4. 启用并行下载和构建缓存
# ============================================================================

set -e

# ===== 颜色输出 =====
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_err() { echo -e "${RED}[ERR]${NC} $1"; }

# ===== 步骤 0：环境准备 =====
log_info "===== 步骤 0：环境准备 ====="

# 切换到工作目录
cd /workspace

# 验证 Java
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
fi
log_ok "JAVA_HOME=$JAVA_HOME"
java -version 2>&1 | head -1

# 验证 Gradle
GRADLE_BIN=$(which gradle)
if [ -z "$GRADLE_BIN" ]; then
    log_err "未找到系统 Gradle"
    exit 1
fi
log_ok "Gradle: $GRADLE_BIN"
$GRADLE_BIN --version | head -3

# ===== 步骤 1：清理 wrapper 缓存（避免使用损坏的 wrapper 缓存）=====
log_info "===== 步骤 1：清理 wrapper 缓存 ====="
# 清理可能损坏的部分下载文件
find /root/.gradle/wrapper/dists -name "*.lck" -delete 2>/dev/null || true
find /root/.gradle/wrapper/dists -name "*.part" -delete 2>/dev/null || true
# 清理空的 wrapper dist 目录
find /root/.gradle/wrapper/dists -type d -empty -delete 2>/dev/null || true
log_ok "wrapper 缓存已清理"

# ===== 步骤 2：检查本地依赖 =====
log_info "===== 步骤 2：检查本地依赖 ====="
LOCAL_REPO="/workspace/local-maven-repo"
if [ -d "$LOCAL_REPO" ]; then
    LOCAL_COUNT=$(find "$LOCAL_REPO" -type f \( -name "*.aar" -o -name "*.jar" -o -name "*.pom" \) 2>/dev/null | wc -l)
    log_ok "本地仓库: $LOCAL_REPO ($LOCAL_COUNT 个文件)"
else
    log_warn "本地仓库不存在: $LOCAL_REPO"
fi

# ===== 步骤 3：检查 Android SDK =====
log_info "===== 步骤 3：检查 Android SDK ====="
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
    log_warn "未配置 ANDROID_HOME，尝试自动检测..."
    
    # 尝试常见路径
    for SDK_PATH in /root/android-sdk /opt/android-sdk /usr/local/android-sdk ~/Android/Sdk /workspace/.android-sdk; do
        if [ -d "$SDK_PATH" ] && [ -d "$SDK_PATH/platforms" ]; then
            export ANDROID_HOME=$SDK_PATH
            log_ok "自动检测到 Android SDK: $ANDROID_HOME"
            break
        fi
    done
    
    if [ -z "$ANDROID_HOME" ]; then
        log_err "未找到 Android SDK，请先运行 install-android-sdk.sh"
        log_info "可执行: bash install-android-sdk.sh"
        exit 1
    fi
else
    log_ok "ANDROID_HOME=$ANDROID_HOME"
fi

# 创建 local.properties（如果不存在）
if [ ! -f "local.properties" ]; then
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    log_ok "已创建 local.properties"
fi

# ===== 步骤 4：网络环境探测 =====
log_info "===== 步骤 4：网络环境探测 ====="
TEST_HOSTS=(
    "https://maven.aliyun.com"
    "https://mirrors.cloud.tencent.com"
    "https://dl.google.com"
    "https://services.gradle.org"
)
for host in "${TEST_HOSTS[@]}"; do
    if curl -sI --connect-timeout 5 --max-time 10 "$host" >/dev/null 2>&1; then
        log_ok "$host 可达"
    else
        log_warn "$host 不可达"
    fi
done

# ===== 步骤 5：清理 Gradle 缓存（避免使用过期/损坏缓存）=====
log_info "===== 步骤 5：清理过期 Gradle 缓存 ====="
if [ -d /root/.gradle/caches/modules-2/metadata-2.* ]; then
    rm -rf /root/.gradle/caches/modules-2/metadata-2.* 2>/dev/null || true
    log_ok "已清理 metadata 缓存"
fi

# ===== 步骤 6：开始构建 =====
log_info "===== 步骤 6：开始构建 Release APK ====="
log_info "使用本地 Gradle: $GRADLE_BIN"
log_info "配置: --no-daemon --parallel --build-cache --configure-on-demand"

# 构建参数说明：
# --no-daemon: 沙箱环境 daemon 经常被清理，禁用以提升稳定性
# --parallel: 并行编译模块
# --build-cache: 启用构建缓存（需要本地缓存目录）
# --configure-on-demand: 只配置相关项目，加速配置阶段
# -x test: 跳过测试加速构建
# -x lint: 跳过 lint 检查
# -x lintVitalRelease: 跳过 release lint 检查
# --stacktrace: 出错时显示完整堆栈

$GRADLE_BIN clean assembleRelease \
    --no-daemon \
    --parallel \
    --build-cache \
    --configure-on-demand \
    -x test \
    -x lint \
    -x lintVitalRelease \
    --stacktrace

BUILD_EXIT=$?

# ===== 步骤 7：验证输出 =====
if [ $BUILD_EXIT -eq 0 ]; then
    log_ok "===== 构建成功 ====="
    APK_DIR="/workspace/app/build/outputs/apk/release"
    if [ -d "$APK_DIR" ]; then
        log_ok "APK 位置: $APK_DIR"
        ls -lh "$APK_DIR"/*.apk 2>/dev/null
    fi
else
    log_err "===== 构建失败，退出码: $BUILD_EXIT ====="
    log_info "提示：可尝试执行 ./download-all-deps.sh 补全缺失依赖"
    exit $BUILD_EXIT
fi
