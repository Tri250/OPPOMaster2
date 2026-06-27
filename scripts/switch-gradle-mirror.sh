#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Gradle Wrapper 镜像切换脚本
# 用法：./scripts/switch-gradle-mirror.sh [aliyun|tencent|official]
# 默认：自动探测可用镜像
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WRAPPER_PROPS="$SCRIPT_DIR/../gradle/wrapper/gradle-wrapper.properties"

MIRRORS=(
    "aliyun:https://mirrors.aliyun.com/gradle/gradle-8.14.4-bin.zip"
    "tencent:https://mirrors.cloud.tencent.com/gradle/gradle-8.14.4-bin.zip"
    "official:https://services.gradle.org/distributions/gradle-8.14.4-bin.zip"
)

usage() {
    echo "用法: $0 [aliyun|tencent|official|auto]"
    echo "  aliyun   阿里云镜像（默认推荐，国内）"
    echo "  tencent  腾讯云镜像（备选）"
    echo "  official Gradle 官方源"
    echo "  auto     自动探测 fastest mirror"
    exit 1
}

set_mirror() {
    local name="$1"
    local url="$2"
    sed -i.bak "s|^distributionUrl=.*|distributionUrl=$url|" "$WRAPPER_PROPS"
    rm -f "$WRAPPER_PROPS.bak"
    echo "已切换 Gradle wrapper 镜像: $name"
    echo "  $url"
}

test_mirror_speed() {
    local url="$1"
    # 测量下载前 512KB 耗时
    local elapsed
    elapsed=$(curl -o /dev/null -s -w '%{time_total}' --max-time 8 -r 0-524288 "$url" || echo "999")
    echo "$elapsed"
}

auto_select() {
    echo "正在探测 fastest Gradle 镜像..."
    local best_name="official"
    local best_time=999
    local entry
    for entry in "${MIRRORS[@]}"; do
        local name="${entry%%:*}"
        local url="${entry#*:}"
        local time
        time=$(test_mirror_speed "$url")
        printf "  %-10s %.3fs\n" "$name" "$time"
        if (( $(echo "$time < $best_time" | bc -l) )); then
            best_time="$time"
            best_name="$name"
        fi
    done
    echo "最快镜像: $best_name (${best_time}s)"

    for entry in "${MIRRORS[@]}"; do
        if [[ "${entry%%:*}" == "$best_name" ]]; then
            set_mirror "$best_name" "${entry#*:}"
            return
        fi
    done
}

main() {
    local choice="${1:-auto}"

    if [[ ! -f "$WRAPPER_PROPS" ]]; then
        echo "错误：未找到 $WRAPPER_PROPS"
        exit 1
    fi

    case "$choice" in
        aliyun)  set_mirror "aliyun" "https://mirrors.aliyun.com/gradle/gradle-8.14.4-bin.zip" ;;
        tencent) set_mirror "tencent" "https://mirrors.cloud.tencent.com/gradle/gradle-8.14.4-bin.zip" ;;
        official)set_mirror "official" "https://services.gradle.org/distributions/gradle-8.14.4-bin.zip" ;;
        auto)    auto_select ;;
        *)       usage ;;
    esac
}

main "$@"
