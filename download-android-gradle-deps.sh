#!/bin/bash
# Android Gradle插件依赖下载脚本
# 手动下载所有必要的依赖到本地Maven仓库

set -e

LOCAL_REPO="/workspace/local-maven-repo"
# 镜像源回退列表: 阿里云 > 腾讯云 > 官方
BASE_URLS=(
    "https://maven.aliyun.com/repository/google"
    "https://mirrors.cloud.tencent.com/nexus/repository/maven-public"
    "https://dl.google.com/android/maven2"
)

echo "下载Android Gradle插件依赖..."

# 下载函数（带多镜像回退 + 严格大小校验）
download_artifact() {
    local group=$1
    local artifact=$2
    local version=$3

    # 转换group路径
    local group_path=$(echo $group | tr '.' '/')
    local artifact_dir="$LOCAL_REPO/$group_path/$artifact/$version"

    mkdir -p $artifact_dir

    # 下载JAR（多镜像回退 + 最小大小校验 1KB）
    local jar_success=false
    for base in "${BASE_URLS[@]}"; do
        local jar_url="$base/$group_path/$artifact/$version/$artifact-$version.jar"
        curl -L --max-time 30 -s -o "$artifact_dir/$artifact-$version.jar" "$jar_url"
        if [ -s "$artifact_dir/$artifact-$version.jar" ] && [ $(stat -c%s "$artifact_dir/$artifact-$version.jar" 2>/dev/null || stat -f%z "$artifact_dir/$artifact-$version.jar" 2>/dev/null) -gt 1024 ]; then
            jar_success=true
            echo "  ✅ JAR: $base"
            break
        fi
    done
    if [ "$jar_success" = false ]; then
        rm -f "$artifact_dir/$artifact-$version.jar"
        echo "  ❌ JAR: $group:$artifact:$version"
        return 1
    fi

    # 下载POM
    local pom_success=false
    for base in "${BASE_URLS[@]}"; do
        local pom_url="$base/$group_path/$artifact/$version/$artifact-$version.pom"
        curl -L --max-time 30 -s -o "$artifact_dir/$artifact-$version.pom" "$pom_url"
        if [ -s "$artifact_dir/$artifact-$version.pom" ] && [ $(stat -c%s "$artifact_dir/$artifact-$version.pom" 2>/dev/null || stat -f%z "$artifact_dir/$artifact-$version.pom" 2>/dev/null) -gt 200 ]; then
            pom_success=true
            echo "  ✅ POM: $base"
            break
        fi
    done
    if [ "$pom_success" = false ]; then
        rm -f "$artifact_dir/$artifact-$version.pom"
        echo "  ❌ POM: $group:$artifact:$version"
        return 1
    fi

    return 0
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