#!/bin/bash
# Gradle 启动包装脚本 - 沙箱环境专用
# 解决问题: 沙箱直连外网被阻断，必须通过 127.0.0.1:18080 HTTP 代理
# 配置已在 gradle.properties 的 org.gradle.jvmargs 中设置代理

set -e

# 确保 HTTP_PROXY 环境变量被传递到 JVM
# org.gradle.jvmargs 中的 -Dhttp.proxyHost=127.0.0.1 会覆盖 Java 网络栈
export http_proxy=http://127.0.0.1:18080
export https_proxy=http://127.0.0.1:18080
export HTTP_PROXY=http://127.0.0.1:18080
export HTTPS_PROXY=http://127.0.0.1:18080

# 定位项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Android SDK 环境
export ANDROID_HOME="${ANDROID_HOME:-/root/android-sdk}"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

# 调用原始 gradlew
exec ./gradlew "$@"
