#!/bin/bash
# Android SDK 环境变量设置脚本
# 用法: source setup-android-env.sh

export ANDROID_HOME=/root/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

echo "Android SDK 环境已设置: ANDROID_HOME=$ANDROID_HOME"
