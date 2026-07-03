#!/bin/bash
# ============================================================================
# OMaster TFLite 模型准备脚本
# ============================================================================
# 用途：在正式发布前下载并验证 TFLite 模型文件
# 用法：bash scripts/prepare_models.sh [--force]
#
# 注意：
# - 此脚本仅在模型文件正式就绪后使用
# - 当前三个模型文件为占位符（placeholder），不包含真实模型数据
# - 在模型训练完成并上传到 releases.omaster.app 后，
#   更新 MODEL_SPEC.json 中相应的 checksum 值，然后运行此脚本
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODELS_DIR="$PROJECT_ROOT/app/src/main/assets/models"
MODEL_SPEC="$MODELS_DIR/MODEL_SPEC.json"
FORCE="${1:-}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# 检查依赖
check_deps() {
    if ! command -v jq &> /dev/null; then
        log_error "需要安装 jq（JSON 处理工具）"
        log_info "安装: brew install jq 或 apt-get install jq"
        exit 1
    fi
    if ! command -v sha256sum &> /dev/null && ! command -v shasum &> /dev/null; then
        log_error "需要 sha256sum 或 shasum 工具"
        exit 1
    fi
}

# 获取 SHA-256 校验和
get_sha256() {
    local file="$1"
    if command -v sha256sum &> /dev/null; then
        sha256sum "$file" | awk '{print $1}'
    else
        shasum -a 256 "$file" | awk '{print $1}'
    fi
}

# 下载并验证模型文件
download_model() {
    local model_name="$1"
    local url="$2"
    local expected_checksum="$3"
    local output_file="$MODELS_DIR/$model_name"

    # 如果文件已存在且不是强制模式，跳过
    if [ -f "$output_file" ] && [ "$FORCE" != "--force" ]; then
        log_info "$model_name 已存在，跳过下载（使用 --force 强制重新下载）"
        return 0
    fi

    log_info "正在下载 $model_name ..."
    if command -v curl &> /dev/null; then
        curl -fSL --connect-timeout 30 --max-time 120 -o "$output_file" "$url" || {
            log_error "下载失败: $model_name"
            return 1
        }
    elif command -v wget &> /dev/null; then
        wget -q --timeout=30 -O "$output_file" "$url" || {
            log_error "下载失败: $model_name"
            return 1
        }
    else
        log_error "需要 curl 或 wget"
        return 1
    fi

    # 如果提供了校验和，验证
    if [ -n "$expected_checksum" ]; then
        local actual_checksum
        actual_checksum=$(get_sha256 "$output_file")
        if [ "$actual_checksum" != "$expected_checksum" ]; then
            log_error "校验和不匹配: $model_name"
            log_error "  期望: $expected_checksum"
            log_error "  实际: $actual_checksum"
            rm -f "$output_file"
            return 1
        fi
        log_info "校验和验证通过: $model_name"
    else
        log_warn "未提供校验和，跳过验证: $model_name"
    fi

    log_info "$model_name 下载完成"

    # 输出文件大小
    local file_size
    file_size=$(du -h "$output_file" | cut -f1)
    log_info "  文件大小: $file_size"
}

# 主流程
main() {
    log_info "=== OMaster TFLite 模型准备 ==="
    log_info "模型目录: $MODELS_DIR"
    echo ""

    check_deps

    # 确保模型目录存在
    mkdir -p "$MODELS_DIR"

    # 读取 MODEL_SPEC.json 获取下载信息
    local version
    version=$(jq -r '.version' "$MODEL_SPEC")
    local status
    status=$(jq -r '.download.versions["'"$version"'"].status' "$MODEL_SPEC")

    if [ "$status" = "not_ready" ]; then
        log_warn "模型版本 $version 当前状态为 'not_ready'"
        log_warn "模型文件尚未正式发布，将下载占位文件（仅用于测试）"
        echo ""
    fi

    # 下载三个模型文件
    local models=("scene_classifier" "quality_analyzer" "param_predictor")
    local failed=0

    for model in "${models[@]}"; do
        local url checksum
        url=$(jq -r '.download.versions["'"$version"'"].'"$model"'.url // empty' "$MODEL_SPEC")
        checksum=$(jq -r '.download.versions["'"$version"'"].'"$model"'.checksum // empty' "$MODEL_SPEC")

        if [ -z "$url" ]; then
            log_warn "未找到 $model 的下载 URL，跳过"
            continue
        fi

        if ! download_model "${model}.tflite" "$url" "$checksum"; then
            failed=$((failed + 1))
        fi
        echo ""
    done

    # 汇总
    echo ""
    if [ $failed -eq 0 ]; then
        log_info "所有模型文件准备完成！"
    else
        log_error "有 $failed 个模型文件下载失败，请检查网络连接和 URL 配置"
        exit 1
    fi

    log_info "模型文件列表:"
    ls -lh "$MODELS_DIR"/*.tflite 2>/dev/null || log_warn "未找到 .tflite 文件"
}

main "$@"