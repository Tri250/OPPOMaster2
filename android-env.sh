# Android SDK 环境变量配置
# 生成时间: 2026-06-12

# Android SDK 路径
export ANDROID_HOME=/usr/lib/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME

# 添加到PATH
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/build-tools/35.0.0
export PATH=$PATH:$ANDROID_HOME/build-tools/29.0.3

# Java配置
export JAVA_HOME=/root/.local/share/mise/installs/java/17.0.2
export PATH=$PATH:$JAVA_HOME/bin

# Gradle优化
export GRADLE_OPTS="-Xmx6g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:+UseStringDeduplication"
export GRADLE_USER_HOME=/root/.gradle

# 验证
alias android-sdk-info='echo "ANDROID_HOME: $ANDROID_HOME"; echo "Java: $(java -version 2>&1 | head -1)"; echo "ADB: $(which adb)"'
