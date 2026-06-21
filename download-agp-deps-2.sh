#!/bin/bash
# 下载 AGP 8.7.3 + Kotlin 2.1.20 构建依赖到本地 Maven 仓库
set -e

REPO_DIR="/workspace/local-maven-repo"
# 使用阿里云镜像（已验证可访问）
MIRROR="https://maven.aliyun.com/repository/public"

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
    
    # Download POM
    if [ ! -f "$pom_file" ]; then
        curl -f -L --connect-timeout 15 --max-time 60 -s -o "$pom_file" \
            "$MIRROR/$group_path/$artifact/$version/$base_name.pom" 2>/dev/null && \
            echo "OK: $group:$artifact:$version.pom" || \
            echo "FAIL: $group:$artifact:$version.pom"
    fi
    
    # Download artifact
    if [ "$ext" != "pom" ] && [ ! -f "$artifact_file" ]; then
        curl -f -L --connect-timeout 15 --max-time 120 -s -o "$artifact_file" \
            "$MIRROR/$group_path/$artifact/$version/$base_name.$ext" 2>/dev/null && \
            echo "OK: $group:$artifact:$version.$ext" || \
            echo "FAIL: $group:$artifact:$version.$ext"
    fi
}

echo "=== Downloading AGP 8.7.3 build dependencies ==="

# Guava
download_dep "com.google.guava" "guava" "32.0.1-jre"

# javax.inject
download_dep "javax.inject" "javax.inject" "1"

# kxml2
download_dep "net.sf.kxml" "kxml2" "2.3.0"

# BouncyCastle
download_dep "org.bouncycastle" "bcprov-jdk18on" "1.77"

# trove4j
download_dep "org.jetbrains.intellij.deps" "trove4j" "1.0.20200330"

# dvlib
download_dep "com.android.tools" "dvlib" "31.7.3"

# commons-compress
download_dep "org.apache.commons" "commons-compress" "1.21"

# httpcore
download_dep "org.apache.httpcomponents" "httpcore" "4.4.16"

# jimfs
download_dep "com.google.jimfs" "jimfs" "1.1"

# javax.activation
download_dep "com.sun.activation" "javax.activation" "1.2.0"

# httpclient
download_dep "org.apache.httpcomponents" "httpclient" "4.5.14"

# jna-platform
download_dep "net.java.dev.jna" "jna-platform" "5.6.0"

# juniversalchardet
download_dep "com.googlecode.juniversalchardet" "juniversalchardet" "1.0.3"

# zipflinger
download_dep "com.android" "zipflinger" "8.7.3"

# apksig
download_dep "com.android.tools.build" "apksig" "8.7.3"

# apkzlib
download_dep "com.android.tools.build" "apkzlib" "8.7.3"

# javawriter
download_dep "com.squareup" "javawriter" "2.5.0"

# signflinger
download_dep "com.android" "signflinger" "8.7.3"

# tracker
download_dep "com.android.tools.analytics-library" "tracker" "31.7.3"

# commons-codec
download_dep "commons-codec" "commons-codec" "1.10"

# commons-logging (needed by httpclient)
download_dep "commons-logging" "commons-logging" "1.2"

echo ""
echo "=== Downloading Kotlin 2.1.20 build dependencies ==="

# kotlin-daemon-client
download_dep "org.jetbrains.kotlin" "kotlin-daemon-client" "2.1.20"

# kotlinx-coroutines-core-jvm
download_dep "org.jetbrains.kotlinx" "kotlinx-coroutines-core-jvm" "1.8.0"

echo ""
echo "=== Done ==="
echo "Verifying downloads..."
find "$REPO_DIR" -name "*.jar" -newer /tmp -size 0 2>/dev/null | while read f; do
    echo "WARNING: Empty file: $f"
    rm -f "$f"
done
echo "All dependencies downloaded."