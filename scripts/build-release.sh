#!/bin/bash
# Android Release APK 构建脚本
# 使用系统已安装的Gradle，避免网络下载问题

set -e

echo "======================================"
echo "OMaster Android Release 构建"
echo "======================================"

# 检查环境
echo "检查Gradle环境..."
gradle --version

# 清理构建缓存
echo "清理构建缓存..."
gradle clean

# 构建Release APK
echo "开始构建Release APK..."
gradle assembleRelease \
    --no-daemon \
    --parallel \
    --build-cache \
    --configure-on-demand \
    -x test \
    -x lint \
    -x lintVitalRelease

echo "======================================"
echo "构建完成！"
echo "APK位置: app/build/outputs/apk/release/"
echo "======================================"

# 列出生成的APK
ls -lh app/build/outputs/apk/release/*.apk 2>/dev/null || echo "未找到APK文件"
