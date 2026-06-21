#!/bin/bash
# Download remaining AGP 8.7.3 + Kotlin 2.1.20 missing dependencies
set -e

REPO_DIR="/workspace/local-maven-repo"
MIRROR_GOOGLE="https://maven.aliyun.com/repository/google"
MIRROR_PUBLIC="https://maven.aliyun.com/repository/public"

download() {
    local mirror=$1
    local group=$2
    local artifact=$3
    local version=$4
    local ext=${5:-jar}
    
    local group_path="${group//.//}"
    local dir="$REPO_DIR/$group_path/$artifact/$version"
    mkdir -p "$dir"
    
    local base_name="$artifact-$version"
    local file="$dir/$base_name.$ext"
    
    if [ ! -f "$file" ]; then
        curl -f -L --connect-timeout 15 --max-time 60 -s -o "$file" \
            "$mirror/$group_path/$artifact/$version/$base_name.$ext" 2>/dev/null && \
            echo "OK: $group:$artifact:$version.$ext" || \
            echo "FAIL: $group:$artifact:$version.$ext"
    else
        echo "SKIP: $group:$artifact:$version.$ext (exists)"
    fi
}

# Kotlin compose compiler plugin
download "$MIRROR_PUBLIC" "org.jetbrains.kotlin" "compose-compiler-gradle-plugin" "2.1.20" "jar"
download "$MIRROR_PUBLIC" "org.jetbrains.kotlin" "compose-compiler-gradle-plugin" "2.1.20" "pom"

# Databinding
download "$MIRROR_GOOGLE" "com.android.databinding" "baseLibrary" "8.7.3" "jar"
download "$MIRROR_GOOGLE" "com.android.databinding" "baseLibrary" "8.7.3" "pom"

# Builder test API
download "$MIRROR_GOOGLE" "com.android.tools.build" "builder-test-api" "8.7.3" "jar"
download "$MIRROR_GOOGLE" "com.android.tools.build" "builder-test-api" "8.7.3" "pom"

# Device provider proto
download "$MIRROR_GOOGLE" "com.android.tools.utp" "android-device-provider-ddmlib-proto" "31.7.3" "jar"
download "$MIRROR_GOOGLE" "com.android.tools.utp" "android-device-provider-ddmlib-proto" "31.7.3" "pom"

download "$MIRROR_GOOGLE" "com.android.tools.utp" "android-device-provider-gradle-proto" "31.7.3" "jar"
download "$MIRROR_GOOGLE" "com.android.tools.utp" "android-device-provider-gradle-proto" "31.7.3" "pom"

echo "=== Done ==="