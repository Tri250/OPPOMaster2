#!/bin/bash
# 批量下载 Gradle 依赖到本地 Maven 仓库
# 使用国内镜像加速（阿里云 + 腾讯云双备份），失败时回退官方源

# Google Maven 镜像（阿里云 → 腾讯云 → 官方）
GOOGLE_BASE_URLS=(
    "https://maven.aliyun.com/repository/google"
    "https://mirrors.cloud.tencent.com/nexus/repository/maven-public"
    "https://dl.google.com/android/maven2"
)

# Maven Central 镜像（阿里云 → 腾讯云 → 清华 → 官方）
MAVEN_CENTRAL_URLS=(
    "https://maven.aliyun.com/repository/public"
    "https://mirrors.cloud.tencent.com/nexus/repository/maven-public"
    "https://mirrors.tuna.tsinghua.edu.cn/maven"
    "https://repo1.maven.org/maven2"
)

M2_REPO="/root/.m2/repository"
DOWNLOAD_TIMEOUT=30  # 单个 URL 超时时间（秒）

# AGP 8.7.3 核心依赖
download_google() {
    local group=$1
    local artifact=$2
    local version=$3
    local path="${group//.//}/${artifact}/${version}"
    local dir="${M2_REPO}/${path}"
    mkdir -p "$dir"

    echo "Downloading $group:$artifact:$version from Google Maven mirrors..."
    for base_url in "${GOOGLE_BASE_URLS[@]}"; do
        echo "  Trying: $base_url"
        if curl -sSL --max-time $DOWNLOAD_TIMEOUT -o "$dir/${artifact}-${version}.pom" "${base_url}/${path}/${artifact}-${version}.pom" && \
           curl -sSL --max-time $DOWNLOAD_TIMEOUT -o "$dir/${artifact}-${version}.jar" "${base_url}/${path}/${artifact}-${version}.jar"; then
            echo "  ✅ Success: $base_url"
            return 0
        fi
    done
    echo "  ❌ All mirrors failed for $group:$artifact:$version"
    return 1
}

download_maven_central() {
    local group=$1
    local artifact=$2
    local version=$3
    local path="${group//.//}/${artifact}/${version}"
    local dir="${M2_REPO}/${path}"
    mkdir -p "$dir"

    echo "Downloading $group:$artifact:$version from Maven Central mirrors..."
    for base_url in "${MAVEN_CENTRAL_URLS[@]}"; do
        echo "  Trying: $base_url"
        if curl -sSL --max-time $DOWNLOAD_TIMEOUT -o "$dir/${artifact}-${version}.pom" "${base_url}/${path}/${artifact}-${version}.pom" && \
           curl -sSL --max-time $DOWNLOAD_TIMEOUT -o "$dir/${artifact}-${version}.jar" "${base_url}/${path}/${artifact}-${version}.jar"; then
            echo "  ✅ Success: $base_url"
            return 0
        fi
    done
    echo "  ❌ All mirrors failed for $group:$artifact:$version"
    return 1
}

# AGP 核心
download_google "com.android.tools.build" "gradle" "8.7.3"
download_google "com.android.tools.build" "builder" "8.7.3"
download_google "com.android.tools.build" "builder-model" "8.7.3"
download_google "com.android.tools.build" "gradle-api" "8.7.3"
download_google "com.android.tools.build" "gradle-settings-api" "8.7.3"

# Android Tools
download_google "com.android.tools" "sdk-common" "31.7.3"
download_google "com.android.tools" "sdklib" "31.7.3"
download_google "com.android.tools" "repository" "31.7.3"
download_google "com.android.tools.ddms" "ddmlib" "31.7.3"

# Plugin markers
download_google "com.android.application" "com.android.application.gradle.plugin" "8.7.3"
download_google "com.android.library" "com.android.library.gradle.plugin" "8.7.3"

# Kotlin 插件
download_maven_central "org.jetbrains.kotlin" "kotlin-gradle-plugin" "2.0.21"
download_maven_central "org.jetbrains.kotlin.android" "org.jetbrains.kotlin.android.gradle.plugin" "2.0.21"
download_maven_central "org.jetbrains.kotlin.compose" "org.jetbrains.kotlin.plugin.compose.gradle.plugin" "2.0.21"
download_maven_central "org.jetbrains.kotlin.plugin.compose" "org.jetbrains.kotlin.plugin.compose.gradle.plugin" "2.0.21"
download_maven_central "org.jetbrains.kotlin.plugin.serialization" "org.jetbrains.kotlin.plugin.serialization.gradle.plugin" "2.0.21"
download_maven_central "org.jetbrains.kotlin.plugin.parcelize" "org.jetbrains.kotlin.plugin.parcelize.gradle.plugin" "2.0.21"

echo "Done!"