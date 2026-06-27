#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# OMaster Android 稳定性/压力测试脚本（基于 adb monkey）
# 用法：./scripts/stability_monkey_test.sh [事件数] [seed]
# 默认：10000 事件，随机 seed
# ============================================================

EVENT_COUNT="${1:-10000}"
SEED="${2:-$(date +%s)}"
PACKAGE_NAME="com.silas.omaster"
APK_PATH="${APK_PATH:-app/build/outputs/apk/release/app-universal-release.apk}"

if ! command -v adb >/dev/null 2>&1; then
    echo "错误：未找到 adb"
    exit 1
fi

echo "============================================"
echo "稳定性测试（Monkey）"
echo "包名: $PACKAGE_NAME"
echo "事件数: $EVENT_COUNT"
echo "Seed: $SEED"
echo "APK: $APK_PATH"
echo "============================================"

# 如果设备未安装则自动安装
if ! adb shell pm list packages | grep -q "package:$PACKAGE_NAME"; then
    if [[ ! -f "$APK_PATH" ]]; then
        echo "错误：APK 不存在且设备未安装应用"
        exit 1
    fi
    echo "[准备] 安装测试 APK..."
    adb install -r -d "$APK_PATH"
fi

# 清理旧日志
echo "[准备] 清理旧日志..."
adb logcat -c || true

# 启动应用到前台，确保 monkey 作用于目标应用
adb shell am start -n "$PACKAGE_NAME/.MainActivity" || true
sleep 2

# 运行 monkey
LOG_FILE="monkey_report_${SEED}.txt"
echo "[运行] 开始 monkey 测试，日志保存至 $LOG_FILE"
adb shell monkey -p "$PACKAGE_NAME" \
    --throttle 100 \
    --pct-touch 45 \
    --pct-motion 25 \
    --pct-nav 10 \
    --pct-majornav 10 \
    --pct-syskeys 5 \
    --pct-appswitch 5 \
    --ignore-crashes \
    --ignore-timeouts \
    --ignore-security-exceptions \
    --monitor-native-crashes \
    -s "$SEED" \
    "$EVENT_COUNT" > "$LOG_FILE" 2>&1 || true

# 检查结果
if grep -q "// CRASH" "$LOG_FILE" || grep -q "// NOT RESPONDING" "$LOG_FILE"; then
    echo "============================================"
    echo "稳定性测试发现异常："
    grep -E "// CRASH|// NOT RESPONDING|// Short MD5|Events injected" "$LOG_FILE"
    echo "============================================"
    exit 1
else
    echo "============================================"
    echo "稳定性测试通过，未检测到 Crash/ANR"
    grep "Events injected" "$LOG_FILE" || true
    echo "日志文件: $LOG_FILE"
    echo "============================================"
fi

# 收集可能的崩溃日志
echo "[收尾] 收集 logcat 中 ERROR 级别日志..."
adb logcat -d *:E | grep -i "$PACKAGE_NAME" > "monkey_logcat_${SEED}.txt" 2>&1 || true
echo "Logcat 错误日志: monkey_logcat_${SEED}.txt"
