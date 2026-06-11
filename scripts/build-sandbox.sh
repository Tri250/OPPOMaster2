#!/bin/bash
# 沙箱环境构建脚本
# 使用系统 Gradle 进行构建，绕过 wrapper 网络限制

set -e

# 检查系统 Gradle 是否可用
if command -v gradle &> /dev/null; then
    echo "✓ 使用系统 Gradle: $(gradle --version | head -5)"
else
    echo "✗ 系统未安装 Gradle，请先安装"
    exit 1
fi

# 构建参数
BUILD_ARGS="--stacktrace"

# 检查是否有本地缓存
if [ -d "$HOME/.gradle/caches" ]; then
    echo "✓ 发现本地 Gradle 缓存"
    BUILD_ARGS="$BUILD_ARGS --offline"
fi

# 执行构建
echo ""
echo "开始构建..."
echo "命令: gradle $BUILD_ARGS $@"
echo ""

gradle $BUILD_ARGS "$@"