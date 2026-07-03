#!/bin/bash
# ============================================================================
# OMaster Play Store 图形资源准备脚本
# ============================================================================
# 用途：检查、生成和验证 Play Store 所需的图形资源
# 用法：bash scripts/prepare_playstore_assets.sh [--check|--generate]
#
# 选项：
#   --check    仅检查现有资源状态（默认）
#   --generate 使用 ImageMagick 生成宣传图和图标
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ASSETS_DIR="$PROJECT_ROOT/fastlane/metadata/android"
MODE="${1:---check}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

pass() { echo -e "${GREEN}[PASS]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
info() { echo -e "${BLUE}[INFO]${NC} $*"; }

# 检查 ImageMagick
check_imagemagick() {
    if command -v convert &> /dev/null; then
        return 0
    else
        return 1
    fi
}

# 检查图标资源
check_icon() {
    local icon_file="$PROJECT_ROOT/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
    local playstore_icon="$PROJECT_ROOT/fastlane/metadata/android/images/icon.png"

    if [ -f "$playstore_icon" ]; then
        local size
        size=$(identify -format "%wx%h" "$playstore_icon" 2>/dev/null || echo "unknown")
        local channels
        channels=$(identify -format "%[channels]" "$playstore_icon" 2>/dev/null || echo "unknown")
        if [ "$size" = "512x512" ]; then
            pass "Play Store 图标: 存在 ($size, $channels channels)"
        else
            warn "Play Store 图标: 尺寸为 $size，需要 512x512"
        fi
    else
        fail "Play Store 图标: 缺失 (需要 512x512 PNG)"
        if [ -f "$icon_file" ]; then
            info "  → 可以从 mipmap-xxxhdpi 生成: $icon_file"
        fi
    fi
}

# 检查宣传图
check_feature_graphic() {
    local fg_file="$ASSETS_DIR/images/feature_graphic.png"

    if [ -f "$fg_file" ]; then
        local size
        size=$(identify -format "%wx%h" "$fg_file" 2>/dev/null || echo "unknown")
        if [ "$size" = "1024x500" ]; then
            pass "宣传图 (Feature Graphic): 存在 ($size)"
        else
            warn "宣传图 (Feature Graphic): 尺寸为 $size，需要 1024x500"
        fi
    else
        fail "宣传图 (Feature Graphic): 缺失 (需要 1024x500 PNG)"
    fi
}

# 检查手机截图
check_screenshots() {
    local screenshot_dir="$ASSETS_DIR/images/phoneScreenshots"
    local count=0

    if [ -d "$screenshot_dir" ]; then
        count=$(find "$screenshot_dir" -name "*.png" 2>/dev/null | wc -l)
        if [ "$count" -ge 2 ]; then
            pass "手机截图: $count 张 (最少需要 2 张)"
        elif [ "$count" -eq 1 ]; then
            warn "手机截图: 仅 1 张 (最少需要 2 张)"
        else
            fail "手机截图: 缺失 (最少需要 2 张)"
        fi
    else
        fail "手机截图: 目录不存在 ($screenshot_dir)"
    fi
}

# 生成宣传图 (Feature Graphic)
generate_feature_graphic() {
    local output="$ASSETS_DIR/images/feature_graphic.png"
    mkdir -p "$(dirname "$output")"

    info "生成宣传图 (1024x500)..."

    # 使用 ImageMagick 创建宣传图
    convert -size 1024x500 \
        gradient:'#1a1a2e'-'#e67e22' \
        -font Helvetica -pointsize 48 -fill white \
        -gravity center \
        -annotate 0 "OMaster\n哈苏色调大师" \
        -pointsize 24 -fill '#f0a04b' \
        -gravity south -annotate +0+60 "AI 场景识别 · 专业调色预设 · 一键出片" \
        "$output"

    pass "宣传图已生成: $output"
}

# 生成图标 (RGBA→RGB 转换)
generate_icon() {
    local input="$PROJECT_ROOT/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
    local output="$ASSETS_DIR/images/icon.png"

    mkdir -p "$(dirname "$output")"

    if [ ! -f "$input" ]; then
        fail "源图标未找到: $input"
        info "请先构建项目生成图标资源"
        return 1
    fi

    info "转换图标 (RGBA → RGB, 512x512)..."

    # 步骤1: 缩放到 512x512
    # 步骤2: 去除 Alpha 通道，用白色背景填充
    # 步骤3: 保存为 RGB PNG
    convert "$input" \
        -resize 512x512 \
        -background white \
        -alpha remove \
        -alpha off \
        "$output"

    local actual_size
    actual_size=$(identify -format "%wx%h" "$output")
    local channels
    channels=$(identify -format "%[channels]" "$output")

    if [ "$actual_size" = "512x512" ] && [ "$channels" = "rgb" ]; then
        pass "图标已转换: $output ($actual_size, $channels)"
    else
        warn "图标已生成但规格可能不符: $actual_size, $channels channels"
    fi
}

# 创建截图目录和模板
generate_screenshot_placeholders() {
    local screenshot_dir="$ASSETS_DIR/images/phoneScreenshots"
    mkdir -p "$screenshot_dir"

    info "创建截图目录结构..."

    local screens=(
        "01_home.png"
        "02_preset_detail.png"
        "03_ai_finetune.png"
        "04_hasselblad_viewfinder.png"
        "05_lut_library.png"
        "06_smart_optimize.png"
        "07_cloud_sync.png"
        "08_floating_window.png"
    )

    for screen in "${screens[@]}"; do
        local target="$screenshot_dir/$screen"
        if [ ! -f "$target" ]; then
            # 创建占位截图（纯色背景 + 文字标注）
            convert -size 1080x1920 \
                gradient:'#1a1a2e'-'#16213e' \
                -font Helvetica -pointsize 36 -fill '#e67e22' \
                -gravity center -annotate 0 "${screen%.png}" \
                -pointsize 20 -fill '#9ca3af' \
                -gravity south -annotate +0+120 "OMaster v2.2.1" \
                "$target"
            info "  创建占位截图: $screen"
        else
            info "  已存在: $screen"
        fi
    done

    pass "截图目录已准备: $screenshot_dir ($(ls "$screenshot_dir"/*.png 2>/dev/null | wc -l) 张)"
}

# ===== 主流程 =====

main() {
    echo -e "${BLUE}╔══════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   OMaster Play Store 图形资源准备工具    ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════╝${NC}"
    echo ""

    if [ "$MODE" = "--generate" ]; then
        if ! check_imagemagick; then
            fail "需要安装 ImageMagick: brew install imagemagick 或 apt-get install imagemagick"
            exit 1
        fi
        info "模式: 生成资源"
        echo ""
        generate_icon
        generate_feature_graphic
        generate_screenshot_placeholders

        echo ""
        info "图形资源生成完成！"
        info "请替换占位截图为真实应用截图。"
        echo ""
        info "资源位置:"
        info "  图标:       $ASSETS_DIR/images/icon.png"
        info "  宣传图:     $ASSETS_DIR/images/feature_graphic.png"
        info "  截图:       $ASSETS_DIR/images/phoneScreenshots/"
    else
        info "模式: 检查现有资源"
        echo ""
        check_icon
        check_feature_graphic
        check_screenshots

        echo ""
        info "如需生成资源，请运行: bash scripts/prepare_playstore_assets.sh --generate"
    fi
}

main "$@"