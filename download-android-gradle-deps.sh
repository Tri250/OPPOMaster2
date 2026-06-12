#!/bin/bash
# Android Gradle插件依赖下载脚本
# 手动下载所有必要的依赖到本地Maven仓库

set -e

LOCAL_REPO="/workspace/local-maven-repo"
BASE_URL="https://dl.google.com/android/maven2"

echo "下载Android Gradle插件依赖..."

# 下载函数
download_artifact() {
    local group=$1
    local artifact=$2
    local version=$3
    
    # 转换group路径
    local group_path=$(echo $group | tr '.' '/')
    local artifact_dir="$LOCAL_REPO/$group_path/$artifact/$version"
    
    mkdir -p $artifact_dir
    
    # 下载JAR
    local jar_url="$BASE_URL/$group_path/$artifact/$version/$artifact-$version.jar"
    echo "下载: $jar_url"
    curl -L -s -o "$artifact_dir/$artifact-$version.jar" "$jar_url" || echo "JAR下载失败: $jar_url"
    
    # 下载POM
    local pom_url="$BASE_URL/$group_path/$artifact/$version/$artifact-$version.pom"
    echo "下载: $pom_url"
    curl -L -s -o "$artifact_dir/$artifact-$version.pom" "$pom_url" || echo "POM下载失败: $pom_url"
    
    ls -la $artifact_dir
}

# 下载核心依赖
download_artifact "com.android.tools.build" "gradle" "8.7.3"
download_artifact "com.android.tools.build" "gradle-api" "8.7.3"
download_artifact "com.android.tools.build" "builder" "8.7.3"
download_artifact "com.android.tools.build" "builder-model" "8.7.3"

download_artifact "com.android.tools" "common" "31.7.3"
download_artifact "com.android.tools" "sdklib" "31.7.3"
download_artifact "com.android.tools" "sdk-common" "31.7.3"

download_artifact "com.android.tools.analytics-library" "analytics" "31.7.3"
download_artifact "com.android.tools.analytics-library" "crash" "31.7.3"
download_artifact "com.android.tools.analytics-library" "shared" "31.7.3"
download_artifact "com.android.tools.analytics-library" "protos" "31.7.3"

download_artifact "com.android.tools.ddms" "ddmlib" "31.7.3"
download_artifact "com.android.tools.layoutlib" "layoutlib-api" "31.7.3"
download_artifact "com.android.tools.layoutlib" "layoutlib-api-jdk11" "31.7.3"

download_artifact "com.android.tools.build" "manifest-merger" "31.7.3"
download_artifact "com.android.tools.build" "aaptcompiler" "8.7.3"
download_artifact "com.android.tools.build" "aapt2-proto" "8.7.3"
download_artifact "com.android.tools.build" "aapt2" "8.7.3"

download_artifact "com.android.tools.build.jetifier" "jetifier-core" "1.0.0"
download_artifact "com.android.tools.build.jetifier" "jetifier-processor" "1.0.0"

echo "下载完成！"
ls -la $LOCAL_REPO