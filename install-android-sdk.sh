#!/bin/bash
# Android SDK 安装脚本
# 使用国内镜像下载，避免网络问题

set -e

ANDROID_SDK_DIR="/root/android-sdk"
CMDLINE_TOOLS_VERSION="11076708"  # 最新版本号

echo "======================================"
echo "Android SDK 安装脚本"
echo "======================================"

# 创建SDK目录
mkdir -p $ANDROID_SDK_DIR
mkdir -p $ANDROID_SDK_DIR/cmdline-tools

# 下载命令行工具（使用国内镜像）
echo "下载Android SDK命令行工具..."
CMDLINE_TOOLS_URL="https://mirrors.cloud.tencent.com/AndroidSDK/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

# 强制使用国内镜像，不依赖 /tmp 旧文件
if [ -f "/tmp/cmdline-tools.zip" ]; then
    echo "检测到旧缓存，删除重下..."
    rm -f /tmp/cmdline-tools.zip
fi

mirror=1
CMDLINE_TOOLS_URL="https://mirrors.cloud.tencent.com/AndroidSDK/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
while [ $mirror -le 4 ]; do
    echo "尝试下载: $CMDLINE_TOOLS_URL"
    if wget -q --timeout=30 --tries=3 -O /tmp/cmdline-tools.zip "$CMDLINE_TOOLS_URL"; then
        break
    fi
    echo "镜像 $mirror 下载失败"
    rm -f /tmp/cmdline-tools.zip
    case $mirror in
        1) CMDLINE_TOOLS_URL="https://mirrors.aliyun.com/android-sdk/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" ;;
        2) CMDLINE_TOOLS_URL="https://mirrors.bfsu.edu.cn/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" ;;
        3) CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" ;;
    esac
    mirror=$((mirror + 1))
done

if [ ! -f "/tmp/cmdline-tools.zip" ]; then
    echo "下载失败，请检查网络连接"
    exit 1
fi

# 解压命令行工具
echo "解压命令行工具..."
unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_DIR/cmdline-tools/
mv $ANDROID_SDK_DIR/cmdline-tools/cmdline-tools $ANDROID_SDK_DIR/cmdline-tools/latest || true

# 配置环境变量
echo "配置环境变量..."
export ANDROID_HOME=$ANDROID_SDK_DIR
export PATH=$PATH:$ANDROID_SDK_DIR/cmdline-tools/latest/bin:$ANDROID_SDK_DIR/platform-tools

# 写入环境变量到profile（仅当前用户）
echo "export ANDROID_HOME=$ANDROID_SDK_DIR" >> ~/.bashrc
echo "export ANDROID_SDK_ROOT=$ANDROID_SDK_DIR" >> ~/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> ~/.bashrc

# 接受SDK许可
echo "接受SDK许可..."
# 部分许可证默认通过，使用 --licenses 自动同意
yes | sdkmanager --sdk_root="$ANDROID_SDK_DIR" --licenses

# 安装必要组件（与项目 build.gradle.kts 匹配：compileSdk=36, minSdk=24, targetSdk=36, AGP=8.7.3）
echo "安装必要SDK组件..."
sdkmanager --sdk_root="$ANDROID_SDK_DIR" \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "extras;android;m2repository" \
    "extras;google;m2repository" || {
    echo "SDK组件安装失败，请手动安装"
    exit 1
}

echo "======================================"
echo "Android SDK 安装完成！"
echo "ANDROID_HOME: $ANDROID_SDK_DIR"
echo "======================================"

# 验证安装
sdkmanager --list_installed 2>/dev/null || echo "sdkmanager验证失败"