#!/bin/bash
# Android Release APK 构建脚本
# 使用系统已安装的 Gradle，避免 wrapper 网络下载问题
#
# 使用方式：
#   ./build-release.sh           # 完整构建（包含测试和 Lint）
#   ./build-release.sh --quick   # 快速构建（跳过测试和 Lint，仅用于紧急修复）

set -e

QUICK_BUILD=false
if [ "$1" == "--quick" ]; then
    QUICK_BUILD=true
    echo "⚠️ 快速构建模式：跳过测试和Lint检查"
fi

echo "======================================"
echo "OMaster Android Release 构建"
echo "======================================"

# 检查 Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️ ANDROID_HOME 未设置，尝试使用默认路径 /root/android-sdk"
    export ANDROID_HOME="/root/android-sdk"
    export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
fi

if [ ! -d "$ANDROID_HOME" ]; then
    echo "❌ Android SDK 未找到: $ANDROID_HOME"
    echo "请先运行 install-android-sdk.sh 安装 SDK"
    exit 1
fi

echo "✅ ANDROID_HOME: $ANDROID_HOME"

# 选择 Gradle 命令：优先系统 gradle，否则尝试 wrapper
if command -v gradle &>/dev/null; then
    GRADLE_CMD="gradle"
    echo "✅ 使用系统 Gradle"
else
    GRADLE_CMD="./gradlew"
    echo "⚠️ 未找到系统 Gradle，尝试使用 Gradle Wrapper"
fi

# 检查签名配置（release 构建必需）
if [ ! -f "app/keystore-release.properties" ] && [ ! -f "app/keystore.properties" ]; then
    echo "⚠️ 未找到 app/keystore-release.properties 或 app/keystore.properties"
    echo "   Release 构建可能失败，请在 CI 环境设置 RELEASE_* 变量或使用 debug 签名回退"
fi

# 检查 local.properties（友盟 AppKey 等）
if [ ! -f "local.properties" ]; then
    echo "⚠️ 未找到 local.properties，友盟统计将不可用"
fi

# 清理构建缓存
echo "清理构建缓存..."
$GRADLE_CMD clean

# 构建Release APK
echo "开始构建Release APK..."

BUILD_ARGS=(
    assembleRelease
    --no-daemon
    --parallel
    --build-cache
)

if [ "$QUICK_BUILD" = true ]; then
    # 快速构建：跳过测试和Lint（仅用于紧急修复）
    BUILD_ARGS+=(-x test -x lint -x lintVitalRelease)
fi

$GRADLE_CMD "${BUILD_ARGS[@]}"

echo "======================================"
echo "构建完成！"
echo "APK位置: app/build/outputs/apk/release/"

# 列出生成的APK
APK_DIR="app/build/outputs/apk/release"
if [ -d "$APK_DIR" ]; then
    ls -lh "$APK_DIR"/*.apk 2>/dev/null || echo "未找到APK文件"
else
    echo "未找到APK输出目录"
fi

# 验证 mapping 文件备份
MAPPING_FILE="app/build/outputs/mapping/release/mapping.txt"
if [ -f "$MAPPING_FILE" ]; then
    echo "✅ Mapping 文件已生成: $MAPPING_FILE"
    BACKUP_DIR="app/mapping"
    if [ -d "$BACKUP_DIR" ]; then
        echo "✅ Mapping 备份目录: $BACKUP_DIR"
        ls -lh "$BACKUP_DIR"/mapping-*.txt 2>/dev/null | tail -n 5
    fi
else
    echo "⚠️ 未找到 Mapping 文件，请检查 R8 配置"
fi

echo "======================================"
