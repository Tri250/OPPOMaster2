#!/bin/bash
# OMaster Android 离线构建脚本
# 用于在网络受限环境中构建项目

set -e

echo "=========================================="
echo "OMaster Android 离线构建"
echo "=========================================="

# 设置环境变量
export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
export ANDROID_HOME=/root/android-sdk
export GRADLE_USER_HOME=/root/.gradle

# 检查本地 Gradle 缓存
if [ -d "$GRADLE_USER_HOME/caches" ]; then
    echo "✅ Gradle 缓存目录存在: $GRADLE_USER_HOME/caches"
    CACHE_SIZE=$(du -sh $GRADLE_USER_HOME/caches 2>/dev/null | cut -f1)
    echo "   缓存大小: $CACHE_SIZE"
else
    echo "⚠️  Gradle 缓存目录不存在，首次构建需要下载依赖"
fi

# 检查本地 Gradle 发行版
GRADLE_VERSION="8.14.4"
GRADLE_DIST="$GRADLE_USER_HOME/wrapper/dists/gradle-$GRADLE_VERSION-bin"
if [ -d "$GRADLE_DIST" ]; then
    echo "✅ Gradle $GRADLE_VERSION 已缓存"
else
    echo "⚠️  Gradle $GRADLE_VERSION 未缓存"
fi

# 构建选项
BUILD_TYPE=${1:-debug}

echo ""
echo "构建类型: $BUILD_TYPE"
echo ""

# 根据构建类型执行
if [ "$BUILD_TYPE" = "offline" ] || [ "$BUILD_TYPE" = "--offline" ]; then
    echo "🔄 使用离线模式构建..."
    ./gradlew assembleDebug --offline --no-daemon --build-cache \
        -Dorg.gradle.internal.network.retry.max.attempts=1 \
        -Dorg.gradle.internal.http.connectionTimeout=5000 \
        -Dorg.gradle.internal.http.socketTimeout=5000 \
        -I init.gradle.kts
elif [ "$BUILD_TYPE" = "clean" ]; then
    echo "🧹 清理构建..."
    ./gradlew clean --no-daemon -I init.gradle.kts
elif [ "$BUILD_TYPE" = "test" ]; then
    echo "🧪 运行测试..."
    ./gradlew testDebugUnitTest --no-daemon -I init.gradle.kts
elif [ "$BUILD_TYPE" = "release" ]; then
    echo "📦 构建 Release APK..."
    ./gradlew assembleRelease --no-daemon -I init.gradle.kts
else
    echo "🔄 使用在线模式构建..."
    ./gradlew assembleDebug --no-daemon \
        -Dorg.gradle.internal.network.retry.max.attempts=10 \
        -Dorg.gradle.internal.http.connectionTimeout=180000 \
        -Dorg.gradle.internal.http.socketTimeout=180000 \
        -I init.gradle.kts
fi

echo ""
echo "=========================================="
echo "构建完成"
echo "=========================================="
