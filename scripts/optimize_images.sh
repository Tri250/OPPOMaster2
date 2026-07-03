#!/bin/bash
# 图片资源优化脚本
# 用于压缩和优化 app/src/main/assets/images/ 中的 WebP 图片

echo "=== OMaster 图片资源优化 ==="
echo ""

IMAGES_DIR="app/src/main/assets/images"

# 检查目录是否存在
if [ ! -d "$IMAGES_DIR" ]; then
    echo "错误: 图片目录不存在: $IMAGES_DIR"
    exit 1
fi

echo "当前图片统计:"
echo "文件数量: $(find $IMAGES_DIR -name '*.webp' | wc -l)"
echo "总大小: $(du -sh $IMAGES_DIR | cut -f1)"
echo ""

# 列出最大的文件
echo "最大的 10 个文件:"
ls -lhS $IMAGES_DIR/*.webp | head -10 | awk '{print $5, $9}'
echo ""

echo "优化建议:"
echo "1. 使用 cwebp 工具压缩图片:"
echo "   cwebp -q 80 input.webp -o output.webp"
echo ""
echo "2. 批量压缩所有图片:"
echo "   for f in $IMAGES_DIR/*.webp; do cwebp -q 80 \"\$f\" -o \"\$f\"; done"
echo ""
echo "3. 考虑使用动态加载或 CDN 分发大图片"
echo ""
echo "4. 对于 placeholder.webp 等占位图，可以使用更小的尺寸"
echo ""

# 计算优化潜力
echo "分析完成。建议将图片质量从默认降低到 75-85%，可减少 30-50% 体积。"
