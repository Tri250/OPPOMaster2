#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Android SDK 安装脚本（已整合到 scripts/setup-android-build-env.sh）
# 保留此文件用于兼容性，新环境请优先使用统一配置脚本：
#   ./scripts/setup-android-build-env.sh
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
UNIFIED_SETUP="$SCRIPT_DIR/scripts/setup-android-build-env.sh"

if [[ -f "$UNIFIED_SETUP" ]]; then
    echo "正在调用统一构建环境配置脚本..."
    exec "$UNIFIED_SETUP" "$@"
else
    echo "错误：未找到统一配置脚本 $UNIFIED_SETUP"
    exit 1
fi
