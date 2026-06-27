#!/bin/bash
# Android SDK 安装脚本
# 使用国内镜像下载，避免网络问题
# 推荐镜像顺序：清华 TUNA > 中科大 USTC > 阿里云 > 腾讯云 > 官方

set -e

ANDROID_SDK_DIR="/root/android-sdk"
CMDLINE_TOOLS_VERSION="11076708"  # 最新版本号

# 国内镜像列表（命令行工具 zip）
MIRRORS=(
    "https://mirrors.tuna.tsinghua.edu.cn/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    "https://mirrors.ustc.edu.cn/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    "https://mirrors.aliyun.com/android-sdk/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    "https://mirrors.cloud.tencent.com/AndroidSDK/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
)

echo "======================================"
echo "Android SDK 安装脚本（国内镜像加速版）"
echo "======================================"

# 创建SDK目录
mkdir -p "$ANDROID_SDK_DIR"
mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
mkdir -p "$HOME/.android"

# 下载命令行工具（自动切换镜像）
echo "下载 Android SDK 命令行工具..."
CMDLINE_TOOLS_URL=""
if [ ! -f "/tmp/cmdline-tools.zip" ]; then
    for url in "${MIRRORS[@]}"; do
        echo "尝试下载: $url"
        if wget -q --timeout=30 --tries=2 -O /tmp/cmdline-tools.zip "$url"; then
            CMDLINE_TOOLS_URL="$url"
            echo "✓ 下载成功: $url"
            break
        else
            echo "✗ 下载失败，切换下一个镜像..."
            rm -f /tmp/cmdline-tools.zip
        fi
    done

    if [ -z "$CMDLINE_TOOLS_URL" ]; then
        echo "所有镜像下载失败，请检查网络连接"
        exit 1
    fi
else
    echo "使用已缓存的 /tmp/cmdline-tools.zip"
fi

# 解压命令行工具
echo "解压命令行工具..."
rm -rf "$ANDROID_SDK_DIR/cmdline-tools/latest"
unzip -q -o /tmp/cmdline-tools.zip -d "$ANDROID_SDK_DIR/cmdline-tools/"
mv "$ANDROID_SDK_DIR/cmdline-tools/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest" || true

# 配置环境变量
echo "配置环境变量..."
export ANDROID_HOME="$ANDROID_SDK_DIR"
export ANDROID_SDK_ROOT="$ANDROID_SDK_DIR"
export PATH="$PATH:$ANDROID_SDK_DIR/cmdline-tools/latest/bin:$ANDROID_SDK_DIR/platform-tools:$ANDROID_SDK_DIR/build-tools/36.0.0"

# 写入环境变量到 profile（幂等，避免重复追加）
# 使用 ~/.profile 确保 login shell 与非交互 shell 都能加载
ANDROID_ENV_BLOCK="# Android SDK
export ANDROID_HOME=$ANDROID_SDK_DIR
export ANDROID_SDK_ROOT=$ANDROID_SDK_DIR
export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/build-tools/36.0.0"

if ! grep -q "ANDROID_HOME=$ANDROID_SDK_DIR" ~/.profile 2>/dev/null; then
    echo "" >> ~/.profile
    echo "$ANDROID_ENV_BLOCK" >> ~/.profile
fi

# 配置 sdkmanager 国内镜像（repository 源）
echo "配置 sdkmanager 国内镜像源..."
cat > "$HOME/.android/repositories.cfg" <<'EOF'
count=4
src0=https\://mirrors.tuna.tsinghua.edu.cn/android/repository/addons_list-5.xml
src1=https\://mirrors.ustc.edu.cn/android/repository/addons_list-5.xml
src2=https\://mirrors.aliyun.com/android-sdk/addons_list-5.xml
src3=https\://dl.google.com/android/repository/addons_list-5.xml
EOF

# 接受SDK许可
echo "接受 SDK 许可..."
yes | sdkmanager --sdk_root="$ANDROID_SDK_DIR" --licenses || true

# 安装必要组件（与项目 build.gradle.kts 匹配：compileSdk=36, minSdk=24, targetSdk=36, AGP=8.7.3）
echo "安装必要 SDK 组件..."
sdkmanager --sdk_root="$ANDROID_SDK_DIR" --install \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "extras;android;m2repository" \
    "extras;google;m2repository" || {
    echo "SDK 组件安装失败，请检查网络或手动重试"
    exit 1
}

echo "======================================"
echo "Android SDK 安装完成！"
echo "ANDROID_HOME: $ANDROID_SDK_DIR"
echo "======================================"

# 验证安装
echo ""
echo "已安装组件："
sdkmanager --sdk_root="$ANDROID_SDK_DIR" --list_installed || echo "sdkmanager 验证失败"

echo ""
echo "验证工具版本："
"$ANDROID_SDK_DIR/platform-tools/adb" version || true
"$ANDROID_SDK_DIR/build-tools/36.0.0/aapt" version || true