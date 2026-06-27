#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# OMaster Android 构建环境一键配置脚本
# 功能：安装 Android SDK、配置国内镜像加速、设置环境变量、验证构建
# 用法：./scripts/setup-android-build-env.sh
# 支持：Linux / macOS（WSL 需单独处理 USB 调试）
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# 默认安装路径（可改为 $HOME/Android/Sdk）
ANDROID_SDK_DIR="${ANDROID_SDK_DIR:-$HOME/Android/Sdk}"
PACKAGE_NAME="com.silas.omaster"

# 与项目 build.gradle.kts 对齐：compileSdk=36, buildTools=36.0.0
BUILD_TOOLS_VERSION="36.0.0"
COMPILE_SDK_VERSION="36"
MIN_PLATFORM_VERSION="24"
CMDLINE_TOOLS_VERSION="11076708"

# 国内镜像源（按顺序尝试）
MIRROR_URLS=(
    "https://mirrors.cloud.tencent.com/AndroidSDK/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    "https://mirrors.aliyun.com/android-sdk/android-sdk-commandline-tools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
)
OFFICIAL_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

log() {
    echo "============================================"
    echo "$1"
    echo "============================================"
}

detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "linux";;
        Darwin*)    echo "mac";;
        CYGWIN*|MINGW*|MSYS*) echo "windows";;
        *)          echo "unknown";;
    esac
}

install_dependencies() {
    log "Step 1/6: 安装系统依赖"
    local os
    os=$(detect_os)
    if [[ "$os" == "linux" ]]; then
        if command -v apt-get >/dev/null 2>&1; then
            sudo apt-get update
            sudo apt-get install -y unzip wget openjdk-17-jdk
        elif command -v yum >/dev/null 2>&1; then
            sudo yum install -y unzip wget java-17-openjdk-devel
        fi
    elif [[ "$os" == "mac" ]]; then
        if ! command -v brew >/dev/null 2>&1; then
            echo "请手动安装 Homebrew: https://brew.sh"
            exit 1
        fi
        brew install unzip wget openjdk@17
    fi
    echo "系统依赖安装完成"
}

download_with_mirrors() {
    local output="$1"
    local url
    for url in "${MIRROR_URLS[@]}"; do
        echo "尝试下载: $url"
        if wget -q --timeout=60 --tries=2 -O "$output" "$url"; then
            echo "镜像下载成功: $url"
            return 0
        fi
        echo "镜像失败，尝试下一个..."
    done

    echo "国内镜像均失败，尝试官方源..."
    if wget -q --timeout=120 --tries=2 -O "$output" "$OFFICIAL_URL"; then
        echo "官方源下载成功"
        return 0
    fi

    return 1
}

install_android_sdk() {
    log "Step 2/6: 安装 Android SDK 到 $ANDROID_SDK_DIR"

    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"

    if [[ -d "$ANDROID_SDK_DIR/cmdline-tools/latest/bin" ]]; then
        echo "命令行工具已存在，跳过下载"
    else
        local tmp_zip="/tmp/cmdline-tools-${CMDLINE_TOOLS_VERSION}.zip"
        rm -f "$tmp_zip"
        if ! download_with_mirrors "$tmp_zip"; then
            echo "错误：命令行工具下载失败"
            exit 1
        fi

        echo "解压命令行工具..."
        rm -rf "$ANDROID_SDK_DIR/cmdline-tools/latest"
        unzip -q "$tmp_zip" -d "$ANDROID_SDK_DIR/cmdline-tools/"
        mv "$ANDROID_SDK_DIR/cmdline-tools/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest" || true
        rm -f "$tmp_zip"
    fi

    export ANDROID_HOME="$ANDROID_SDK_DIR"
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION:$PATH"

    echo "接受 SDK 许可..."
    yes | sdkmanager --licenses >/dev/null 2>&1 || true

    echo "安装必要 SDK 组件（与项目对齐：API $COMPILE_SDK_VERSION, Build Tools $BUILD_TOOLS_VERSION）..."
    sdkmanager --install \
        "platform-tools" \
        "platforms;android-$COMPILE_SDK_VERSION" \
        "platforms;android-$MIN_PLATFORM_VERSION" \
        "build-tools;$BUILD_TOOLS_VERSION" \
        "extras;android;m2repository" \
        "extras;google;m2repository"

    echo "Android SDK 安装完成"
}

configure_environment() {
    log "Step 3/6: 配置环境变量"

    local shell_rc="$HOME/.bashrc"
    if [[ "$SHELL" == *"zsh"* ]]; then
        shell_rc="$HOME/.zshrc"
    fi

    # 去重写入
    grep -v "ANDROID_HOME" "$shell_rc" > "$shell_rc.tmp" 2>/dev/null || true
    mv "$shell_rc.tmp" "$shell_rc"

    {
        echo ""
        echo "# OMaster Android SDK"
        echo "export ANDROID_HOME=$ANDROID_SDK_DIR"
        echo "export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION:\$PATH"
    } >> "$shell_rc"

    echo "环境变量已写入 $shell_rc"
    echo "请执行：source $shell_rc"

    # 同时生成 local.properties，确保 AGP 能直接定位 SDK
    echo "sdk.dir=$ANDROID_SDK_DIR" > "$PROJECT_DIR/local.properties"
    echo "已生成 $PROJECT_DIR/local.properties"
}

configure_gradle_mirror() {
    log "Step 4/6: 配置 Gradle 国内镜像"

    # 已配置在 settings.gradle.kts，此处仅验证 wrapper 可用性
    local wrapper_props="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.properties"
    if [[ -f "$wrapper_props" ]]; then
        echo "Gradle wrapper 配置已存在:"
        grep "distributionUrl" "$wrapper_props"
    fi

    # 为当前 shell 配置 Gradle 国内镜像（可选）
    local gradle_user_dir="$HOME/.gradle"
    mkdir -p "$gradle_user_dir"
    cat > "$gradle_user_dir/init.gradle" << 'EOF'
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://maven.aliyun.com/repository/central' }
        mavenCentral()
        google()
    }
}
EOF
    echo "已生成 $gradle_user_dir/init.gradle 作为全局镜像兜底"
}

verify_installation() {
    log "Step 5/6: 验证安装"

    if [[ -z "${ANDROID_HOME:-}" ]]; then
        export ANDROID_HOME="$ANDROID_SDK_DIR"
    fi

    echo "ANDROID_HOME: $ANDROID_HOME"
    echo "adb 版本:"
    adb version || echo "adb 未找到"
    echo "已安装平台:"
    ls "$ANDROID_HOME/platforms/" 2>/dev/null || echo "无平台"
    echo "已安装构建工具:"
    ls "$ANDROID_HOME/build-tools/" 2>/dev/null || echo "无构建工具"
}

verify_build() {
    log "Step 6/6: 验证项目构建"

    cd "$PROJECT_DIR"
    if [[ -f "./gradlew" ]]; then
        ./gradlew assembleDebug --no-daemon || {
            echo "构建失败，请检查网络或依赖配置"
            exit 1
        }
    else
        echo "未找到 gradlew，跳过构建验证"
    fi

    echo "构建验证完成，APK 输出:"
    find "$PROJECT_DIR/app/build/outputs/apk" -name "*.apk" 2>/dev/null || true
}

main() {
    log "OMaster Android 构建环境配置"
    echo "Android SDK 将安装到: $ANDROID_SDK_DIR"
    echo "项目目录: $PROJECT_DIR"

    install_dependencies
    install_android_sdk
    configure_environment
    configure_gradle_mirror
    verify_installation

    read -rp "是否立即执行 assembleDebug 构建验证？(y/N): " confirm
    if [[ "$confirm" =~ ^[Yy]$ ]]; then
        verify_build
    fi

    log "配置完成"
    echo "请运行: source ~/.bashrc (或 ~/.zshrc)"
    echo "然后执行: ./gradlew assembleDebug"
}

main "$@"
