# Android SDK 安装指南

## 当前环境状态

系统已预装基础 Android SDK：
- **位置**: `/usr/lib/android-sdk`
- **Build Tools**: 29.0.3
- **Platform Tools**: adb, fastboot 等
- **缺少**: Android API 35 平台

## 完整安装步骤（真实环境）

### 方法一：使用 Android Studio（推荐）

```bash
# 1. 下载 Android Studio
wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2024.1.1.11/android-studio-2024.1.1.11-linux.tar.gz

# 2. 解压
tar -xzf android-studio-2024.1.1.11-linux.tar.gz -C /opt/

# 3. 运行
/opt/android-studio/bin/studio.sh

# 4. 通过 SDK Manager 安装 API 35
# Tools -> SDK Manager -> SDK Platforms -> Android 15.0 (API 35)
```

### 方法二：命令行安装

```bash
# 1. 设置环境变量
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 2. 下载命令行工具
mkdir -p $ANDROID_HOME/cmdline-tools
cd $ANDROID_HOME/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11066523_latest.zip
unzip commandlinetools-linux-11066523_latest.zip
mv cmdline-tools latest

# 3. 安装 SDK 组件
sdkmanager --install "platforms;android-35"
sdkmanager --install "build-tools;35.0.0"
sdkmanager --install "platform-tools"
sdkmanager --install "extras;google;m2repository"
sdkmanager --install "extras;android;m2repository"

# 4. 接受许可证
yes | sdkmanager --licenses
```

### 方法三：使用包管理器（Linux）

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install android-sdk

# 或使用 snap
sudo snap install android-studio --classic
```

## 环境变量配置

添加到 `~/.bashrc` 或 `~/.zshrc`：

```bash
# Android SDK
export ANDROID_HOME=$HOME/Android/Sdk
# 或使用系统预装路径
# export ANDROID_HOME=/usr/lib/android-sdk

export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/build-tools/35.0.0
```

## 验证安装

```bash
# 检查环境变量
echo $ANDROID_HOME

# 检查工具
adb version
sdkmanager --list

# 检查已安装平台
ls $ANDROID_HOME/platforms/

# 构建项目
cd /workspace
./gradlew assembleDebug
```

## 必要组件清单

| 组件 | 用途 | 命令 |
|------|------|------|
| platforms;android-35 | API 35 平台 | `sdkmanager --install "platforms;android-35"` |
| build-tools;35.0.0 | 构建工具 | `sdkmanager --install "build-tools;35.0.0"` |
| platform-tools | adb, fastboot | `sdkmanager --install "platform-tools"` |
| emulator | 模拟器 | `sdkmanager --install "emulator"` |
| sources;android-35 | 源码 | `sdkmanager --install "sources;android-35"` |

## 常见问题

### Q: sdkmanager 命令找不到

```bash
# 确保路径正确
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

### Q: 许可证未接受

```bash
yes | sdkmanager --licenses
```

### Q: 网络问题无法下载

```bash
# 配置代理
export HTTP_PROXY=http://proxy.example.com:8080
export HTTPS_PROXY=http://proxy.example.com:8080

# 或使用国内镜像（见项目 settings.gradle.kts）
```

## 构建 Release APK

SDK 安装完成后：

```bash
cd /workspace

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 输出位置
ls app/build/outputs/apk/
```

---

**注意**: 当前沙箱环境缺少 API 35 平台，请在真实环境中完成 SDK 安装后构建 APK。
