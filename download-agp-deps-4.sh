#!/bin/bash
# 下载父 POM 和额外缺失依赖
set -e

REPO_DIR="/workspace/local-maven-repo"
MIRROR_PUBLIC="https://maven.aliyun.com/repository/public"
MIRROR_GOOGLE="https://maven.aliyun.com/repository/google"

download() {
    local mirror=$1
    local group=$2
    local artifact=$3
    local version=$4
    local ext=${5:-pom}
    
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

# Parent POMs
download "$MIRROR_PUBLIC" "com.google.guava" "guava-parent" "32.0.1-jre"
download "$MIRROR_PUBLIC" "org.apache.commons" "commons-parent" "52"
download "$MIRROR_PUBLIC" "org.apache.httpcomponents" "httpcomponents-core" "4.4.16"
download "$MIRROR_PUBLIC" "com.google.jimfs" "jimfs-parent" "1.1"
download "$MIRROR_PUBLIC" "com.sun.activation" "all" "1.2.0"
download "$MIRROR_PUBLIC" "org.apache.httpcomponents" "httpcomponents-client" "4.5.14"
download "$MIRROR_PUBLIC" "org.sonatype.oss" "oss-parent" "7"
download "$MIRROR_PUBLIC" "org.apache.commons" "commons-parent" "35"
download "$MIRROR_PUBLIC" "org.jetbrains.kotlinx" "kotlinx-coroutines-bom" "1.8.0"
download "$MIRROR_PUBLIC" "net.java.dev.jna" "jna" "5.6.0" "jar"
download "$MIRROR_PUBLIC" "com.google.code.findbugs" "jsr305" "3.0.2" "jar"
download "$MIRROR_PUBLIC" "org.jetbrains.kotlin" "fus-statistics-gradle-plugin" "2.1.20" "jar"

# Also download the parent of guava-parent (guava-bom)
download "$MIRROR_PUBLIC" "com.google.guava" "guava-bom" "32.0.1-jre" "pom"
# And the parent of guava-bom
download "$MIRROR_PUBLIC" "com.google.guava" "guava-parent-jre" "32.0.1-jre" "pom"

# jna:5.6.0 POM
download "$MIRROR_PUBLIC" "net.java.dev.jna" "jna" "5.6.0" "pom"
# jna parent
download "$MIRROR_PUBLIC" "net.java.dev.jna" "jna-parent" "5.6.0" "pom"

# jsr305 POM
download "$MIRROR_PUBLIC" "com.google.code.findbugs" "jsr305" "3.0.2" "pom"

# fus-statistics-gradle-plugin POM
download "$MIRROR_PUBLIC" "org.jetbrains.kotlin" "fus-statistics-gradle-plugin" "2.1.20" "pom"

echo ""
echo "=== Done ==="