#!/bin/bash
# AGP 8.7.3 完整依赖补全脚本
# 解决本地仓库缺失的 AGP 传递依赖问题
# 镜像源：腾讯云（沙箱环境首选，AGP 完整）

set -e

LOCAL_REPO="/workspace/local-maven-repo"

# 镜像源（按优先级）
TENCENT_PUBLIC="https://mirrors.cloud.tencent.com/nexus/repository/maven-public"
TENCENT_GRADLE="https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins"
ALIYUN_PUBLIC="https://maven.aliyun.com/repository/public"
ALIYUN_GOOGLE="https://maven.aliyun.com/repository/google"
HUAWEI="https://repo.huaweicloud.com/repository/maven"

# 通用下载函数（带回退）
# 用法: download <group_path> <artifact> <version> <extension> [classifier]
# 例如: download com/android/tools/build apksig 8.7.3 jar
download_artifact() {
    local group_path=$1
    local artifact=$2
    local version=$3
    local ext=$4
    local classifier=${5:-}
    local group_id=$(echo "$group_path" | tr '/' '.')

    local dir="$LOCAL_REPO/$group_path/$artifact/$version"
    mkdir -p "$dir"

    # 文件名（含 classifier）
    local file_name
    if [ -n "$classifier" ]; then
        file_name="$artifact-$version-$classifier.$ext"
    else
        file_name="$artifact-$version.$ext"
    fi

    # 如果已存在且大小合理，跳过
    if [ -f "$dir/$file_name" ]; then
        local sz=$(stat -c%s "$dir/$file_name" 2>/dev/null || echo 0)
        if [ "$sz" -gt 500 ]; then
            echo "  ✓ 已存在: $file_name ($sz bytes)"
            return 0
        fi
    fi

    # 镜像优先级：腾讯云 → 阿里云 → 华为云
    local repos=("$TENCENT_PUBLIC" "$ALIYUN_PUBLIC" "$ALIYUN_GOOGLE" "$HUAWEI")

    for repo in "${repos[@]}"; do
        local url="$repo/$group_path/$artifact/$version/$file_name"
        if curl -L --max-time 30 -s -o "$dir/$file_name" "$url" 2>/dev/null; then
            local sz=$(stat -c%s "$dir/$file_name" 2>/dev/null || echo 0)
            if [ "$sz" -gt 500 ]; then
                echo "  ✓ 下载: $file_name from $repo ($sz bytes)"
                return 0
            fi
        fi
    done

    echo "  ✗ 失败: $file_name (所有镜像均不可达)"
    rm -f "$dir/$file_name"
    return 1
}

echo "=========================================="
echo "AGP 8.7.3 完整依赖补全"
echo "时间: $(date)"
echo "=========================================="

# ===== AGP 8.7.3 核心（缺失的关键依赖） =====
echo ""
echo "===== AGP 8.7.3 内部模块（com.android.tools.build）====="
for art in "apksig" "apkzlib" "gradle" "gradle-api" "builder" "builder-model" \
           "aapt2" "aapt2-proto" "aaptcompiler" "manifest-merger"; do
    download_artifact "com/android/tools/build" "$art" "8.7.3" "jar"
    download_artifact "com/android/tools/build" "$art" "8.7.3" "pom"
done

# ===== AGP 31.7.3 工具库 =====
echo ""
echo "===== AGP 31.7.3 工具库 ====="
for art in "common" "sdk-common" "sdklib" "ddms" "layoutlib-api" \
           "layoutlib-api-jdk11" "annotations"; do
    download_artifact "com/android/tools" "$art" "31.7.3" "jar"
    download_artifact "com/android/tools" "$art" "31.7.3" "pom"
done

# ===== AGP analytics-library =====
echo ""
echo "===== AGP analytics-library ====="
for art in "analytics" "crash" "protos" "shared" "tracker"; do
    download_artifact "com/android/tools/analytics-library" "$art" "31.7.3" "jar"
    download_artifact "com/android/tools/analytics-library" "$art" "31.7.3" "pom"
done

# ===== AGP 顶层包（com.android） =====
echo ""
echo "===== AGP 顶层包 ====="
download_artifact "com/android" "signflinger" "8.7.3" "jar"
download_artifact "com/android" "signflinger" "8.7.3" "pom"
download_artifact "com/android" "tools" "31.7.3" "jar" 2>/dev/null || true
download_artifact "com/android" "tools" "31.7.3" "pom" 2>/dev/null || true

# ===== Jetifier =====
echo ""
echo "===== Jetifier ====="
for art in "jetifier-core" "jetifier-processor"; do
    download_artifact "com/android/tools/build/jetifier" "$art" "1.0.0" "jar"
    download_artifact "com/android/tools/build/jetifier" "$art" "1.0.0" "pom"
done

# ===== Gradle 8.7.3 插件 POM =====
echo ""
echo "===== Gradle Plugin POM ====="
download_artifact "com/android/application" "com.android.application.gradle.plugin" "8.7.3" "pom"

# ===== 通用第三方依赖（AGP 间接依赖） =====
echo ""
echo "===== 通用第三方依赖 ====="
# commons-codec
download_artifact "commons-codec" "commons-codec" "1.10" "jar"
download_artifact "commons-codec" "commons-codec" "1.10" "pom"
# commons-compress
download_artifact "org/apache/commons" "commons-compress" "1.21" "jar"
download_artifact "org/apache/commons" "commons-compress" "1.21" "pom"
# httpclient
download_artifact "org/apache/httpcomponents" "httpclient" "4.5.14" "jar"
download_artifact "org/apache/httpcomponents" "httpclient" "4.5.14" "pom"
# httpcore
download_artifact "org/apache/httpcomponents" "httpcore" "4.4.16" "jar"
download_artifact "org/apache/httpcomponents" "httpcore" "4.4.16" "pom"
# javawriter
download_artifact "com/squareup" "javawriter" "2.5.0" "jar"
download_artifact "com/squareup" "javawriter" "2.5.0" "pom"
# javax.inject
download_artifact "javax/inject" "javax.inject" "1" "jar"
download_artifact "javax/inject" "javax.inject" "1" "pom"
# guava
download_artifact "com/google/guava" "guava" "32.0.1-jre" "jar"
download_artifact "com/google/guava" "guava" "32.0.1-jre" "pom"
# kxml2
download_artifact "net/sf/kxml" "kxml2" "2.3.0" "jar"
download_artifact "net/sf/kxml" "kxml2" "2.3.0" "pom"
# trove4j
download_artifact "org/jetbrains/intellij/deps" "trove4j" "1.0.20200330" "jar"
download_artifact "org/jetbrains/intellij/deps" "trove4j" "1.0.20200330" "pom"
# bouncycastle
download_artifact "org/bouncycastle" "bcprov-jdk18on" "1.77" "jar"
download_artifact "org/bouncycastle" "bcprov-jdk18on" "1.77" "pom"
# jna-platform
download_artifact "net/java/dev/jna" "jna-platform" "5.6.0" "jar"
download_artifact "net/java/dev/jna" "jna-platform" "5.6.0" "pom"

# ===== 清理 =====
echo ""
echo "===== 清理无效文件 ====="
find "$LOCAL_REPO" -type f \( -name "*.jar" -o -name "*.aar" -o -name "*.pom" \) -size -1k -delete 2>/dev/null || true
find "$LOCAL_REPO" -type d -empty -delete 2>/dev/null || true

echo ""
echo "=========================================="
echo "✓ 补全完成"
echo "总文件数: $(find "$LOCAL_REPO" -type f | wc -l)"
echo "总大小: $(du -sh "$LOCAL_REPO" | cut -f1)"
echo "=========================================="
