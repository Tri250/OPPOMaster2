#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# OMaster Android 安装/卸载/覆盖安装测试脚本
# 用法：./scripts/install_uninstall_test.sh [apk_path]
# 默认 APK：app/build/outputs/apk/release/app-universal-release.apk
# ============================================================

APK_PATH="${1:-app/build/outputs/apk/release/app-universal-release.apk}"
PACKAGE_NAME="com.silas.omaster"

if ! command -v adb >/dev/null 2>&1; then
    echo "错误：未找到 adb，请确保 Android SDK platform-tools 已加入 PATH"
    exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
    echo "错误：APK 不存在：$APK_PATH"
    echo "请先用 ./gradlew assembleRelease 构建 release 包"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo "============================================"
echo "APK: $APK_PATH"
echo "包大小: $APK_SIZE"
echo "包名: $PACKAGE_NAME"
echo "============================================"

# 1. 清理旧版本（如果存在）
echo "[1/5] 检查并卸载旧版本..."
if adb shell pm list packages | grep -q "package:$PACKAGE_NAME"; then
    adb uninstall "$PACKAGE_NAME" || true
else
    echo "      未检测到旧版本"
fi

# 2. 首次安装
echo "[2/5] 首次安装 APK..."
adb install -r -d "$APK_PATH"
echo "      安装成功"

# 3. 验证启动主 Activity
echo "[3/5] 启动应用验证..."
adb shell am start -n "$PACKAGE_NAME/.MainActivity"
sleep 3

# 检查进程是否存活
if adb shell ps | grep -q "$PACKAGE_NAME"; then
    echo "      应用进程存活"
else
    echo "      警告：未检测到应用进程"
fi

# 4. 覆盖安装（模拟升级）
echo "[4/5] 覆盖安装（模拟升级）..."
adb install -r "$APK_PATH"
echo "      覆盖安装成功"

# 5. 卸载并检查残留
echo "[5/5] 卸载应用并检查残留..."
adb uninstall "$PACKAGE_NAME"
if adb shell pm list packages | grep -q "package:$PACKAGE_NAME"; then
    echo "      错误：卸载后仍检测到包名"
    exit 1
else
    echo "      卸载成功，无包名残留"
fi

echo "============================================"
echo "安装/卸载/覆盖安装测试全部通过"
echo "============================================"
