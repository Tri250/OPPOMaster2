#!/bin/bash
# 下载 AGP 8.7.3 Android 专属依赖（从 Google Maven 镜像）
set -e

REPO_DIR="/workspace/local-maven-repo"
MIRROR="https://maven.aliyun.com/repository/google"

download_dep() {
    local group=$1
    local artifact=$2
    local version=$3
    local ext=${4:-jar}
    
    local group_path="${group//.//}"
    local dir="$REPO_DIR/$group_path/$artifact/$version"
    mkdir -p "$dir"
    
    local base_name="$artifact-$version"
    local pom_file="$dir/$base_name.pom"
    local artifact_file="$dir/$base_name.$ext"
    
    if [ ! -f "$pom_file" ]; then
        curl -f -L --connect-timeout 15 --max-time 60 -s -o "$pom_file" \
            "$MIRROR/$group_path/$artifact/$version/$base_name.pom" 2>/dev/null && \
            echo "OK: $group:$artifact:$version.pom" || \
            echo "FAIL: $group:$artifact:$version.pom"
    fi
    
    if [ "$ext" != "pom" ] && [ ! -f "$artifact_file" ]; then
        curl -f -L --connect-timeout 15 --max-time 120 -s -o "$artifact_file" \
            "$MIRROR/$group_path/$artifact/$version/$base_name.$ext" 2>/dev/null && \
            echo "OK: $group:$artifact:$version.$ext" || \
            echo "FAIL: $group:$artifact:$version.$ext"
    fi
}

echo "=== Downloading Android-specific AGP dependencies ==="

download_dep "com.android.tools" "dvlib" "31.7.3"
download_dep "com.android" "zipflinger" "8.7.3"
download_dep "com.android.tools.build" "apksig" "8.7.3"
download_dep "com.android.tools.build" "apkzlib" "8.7.3"
download_dep "com.android" "signflinger" "8.7.3"
download_dep "com.android.tools.analytics-library" "tracker" "31.7.3"

echo "=== Done ==="