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

# 备用镜像
if [ ! -f "/tmp/cmdline-tools.zip" ]; then
    echo "尝试下载: $CMDLINE_TOOLS_URL"
    wget -q --timeout=30 --tries=3 -O /tmp/cmdline-tools.zip "$CMDLINE_TOOLS_URL" || {
        echo "腾讯云镜像下载失败，尝试阿里云镜像..."
        CMDLINE_TOOLS_URL="https://mirrors.aliyun.com/android-sdk/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
        wget -q --timeout=30 --tries=3 -O /tmp/cmdline-tools.zip "$CMDLINE_TOOLS_URL" || {
            echo "阿里云镜像下载失败，尝试官方源..."
            CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
            wget -q --timeout=60 --tries=2 -O /tmp/cmdline-tools.zip "$CMDLINE_TOOLS_URL" || {
                echo "下载失败，请检查网络连接"
                exit 1
            }
        }
    }
fi

# 解压命令行工具
echo "解压命令行工具..."
unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_DIR/cmdline-tools/
mv $ANDROID_SDK_DIR/cmdline-tools/cmdline-tools $ANDROID_SDK_DIR/cmdline-tools/latest || true

# 配置环境变量
echo "配置环境变量..."
export ANDROID_HOME=$ANDROID_SDK_DIR
export PATH=$PATH:$ANDROID_SDK_DIR/cmdline-tools/latest/bin:$ANDROID_SDK_DIR/platform-tools

# 写入环境变量到profile
echo "export ANDROID_HOME=$ANDROID_SDK_DIR" >> ~/.bashrc
echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools" >> ~/.bashrc

# 接受SDK许可
echo "接受SDK许可..."
yes | sdkmanager --licenses 2>/dev/null || true

# 安装必要组件
echo "安装必要SDK组件..."
sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0" 2>/dev/null || {
    echo "SDK组件安装失败，请手动安装"
}

echo "======================================"
echo "Android SDK 安装完成！"
echo "ANDROID_HOME: $ANDROID_SDK_DIR"
echo "======================================"

# 验证安装
sdkmanager --list_installed 2>/dev/null || echo "sdkmanager验证失败"