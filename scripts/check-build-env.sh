#!/bin/bash
# 构建环境自检脚本
# 检查 JDK、Android SDK、Gradle、签名等 Release 构建前置条件

set -e

ERRORS=0

echo "======================================"
echo "OMaster Release 构建环境自检"
echo "======================================"

# 1. JDK
echo ""
echo "[1/6] 检查 JDK..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "  ✓ Java 已安装: $JAVA_VERSION"
else
    echo "  ✗ Java 未安装"
    ERRORS=$((ERRORS + 1))
fi

# 2. Android SDK
echo ""
echo "[2/6] 检查 Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    echo "  ⚠ ANDROID_HOME 未设置，尝试 /root/android-sdk"
    export ANDROID_HOME="/root/android-sdk"
fi

if [ -d "$ANDROID_HOME" ]; then
    echo "  ✓ ANDROID_HOME: $ANDROID_HOME"
    if [ -d "$ANDROID_HOME/platforms/android-36" ]; then
        echo "  ✓ Android API 36 平台已安装"
    else
        echo "  ✗ Android API 36 平台缺失"
        ERRORS=$((ERRORS + 1))
    fi
    if [ -d "$ANDROID_HOME/build-tools/36.0.0" ]; then
        echo "  ✓ Build Tools 36.0.0 已安装"
    else
        echo "  ✗ Build Tools 36.0.0 缺失"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo "  ✗ Android SDK 未找到: $ANDROID_HOME"
    ERRORS=$((ERRORS + 1))
fi

# 3. Gradle
echo ""
echo "[3/6] 检查 Gradle..."
if command -v gradle &> /dev/null; then
    GRADLE_VERSION=$(gradle --version 2>&1 | grep "Gradle" | head -n 1)
    echo "  ✓ Gradle 已安装: $GRADLE_VERSION"
else
    echo "  ✗ Gradle 未安装（项目推荐使用系统 Gradle 8.14.4）"
    ERRORS=$((ERRORS + 1))
fi

# 4. 签名配置
echo ""
echo "[4/6] 检查 Release 签名配置..."
if [ -f "/workspace/app/keystore-release.properties" ]; then
    echo "  ✓ app/keystore-release.properties 存在"
else
    echo "  ✗ app/keystore-release.properties 缺失（Release 构建必需）"
    ERRORS=$((ERRORS + 1))
fi

if [ -f "/workspace/app/release.keystore" ]; then
    echo "  ✓ app/release.keystore 存在"
else
    echo "  ✗ app/release.keystore 缺失（Release 构建必需）"
    ERRORS=$((ERRORS + 1))
fi

# 5. local.properties
echo ""
echo "[5/6] 检查 local.properties..."
if [ -f "/workspace/local.properties" ]; then
    echo "  ✓ local.properties 存在"
    cat /workspace/local.properties | sed 's/^/      /'
else
    echo "  ✗ local.properties 缺失"
    ERRORS=$((ERRORS + 1))
fi

# 6. 版本号一致性
echo ""
echo "[6/6] 检查版本号..."
APP_VERSION=$(grep -o 'versionName = "[^"]*"' /workspace/app/build.gradle.kts | grep -o '"[^"]*"' | tr -d '"')
APP_CODE=$(grep -o 'versionCode = [0-9]*' /workspace/app/build.gradle.kts | grep -o '[0-9]*' | head -n 1)
echo "  app/build.gradle.kts: versionName=$APP_VERSION, versionCode=$APP_CODE"

CHANGELOG_VERSION=$(grep -o '### v[0-9]\+\.[0-9]\+\.[0-9]\+' /workspace/CHANGELOG.md | head -n 1 | sed 's/### v//')
echo "  CHANGELOG.md 最新版本: $CHANGELOG_VERSION"

if [ "$APP_VERSION" = "$CHANGELOG_VERSION" ]; then
    echo "  ✓ 版本号一致"
else
    echo "  ✗ 版本号不一致，请同步 app/build.gradle.kts 与 CHANGELOG.md"
    ERRORS=$((ERRORS + 1))
fi

echo ""
echo "======================================"
if [ "$ERRORS" -eq 0 ]; then
    echo "✅ 所有检查通过，可执行 Release 构建"
    echo "   构建命令: bash build-release.sh"
else
    echo "❌ 发现 $ERRORS 个问题，请修复后再构建"
fi
echo "======================================"

exit "$ERRORS"
