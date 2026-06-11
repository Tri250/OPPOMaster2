#!/bin/bash
# 批量下载 Gradle 依赖到本地 Maven 仓库

BASE_URL="https://dl.google.com/android/maven2"
MavenCentral_URL="https://repo1.maven.org/maven2"
M2_REPO="/root/.m2/repository"

# AGP 8.7.3 核心依赖
download_google() {
    local group=$1
    local artifact=$2
    local version=$3
    local path="${group//.//}/${artifact}/${version}"
    local dir="${M2_REPO}/${path}"
    mkdir -p "$dir"
    
    echo "Downloading $group:$artifact:$version from Google Maven..."
    curl -sSL -o "$dir/${artifact}-${version}.pom" "${BASE_URL}/${path}/${artifact}-${version}.pom" || true
    curl -sSL -o "$dir/${artifact}-${version}.jar" "${BASE_URL}/${path}/${artifact}-${version}.jar" || true
}

download_maven_central() {
    local group=$1
    local artifact=$2
    local version=$3
    local path="${group//.//}/${artifact}/${version}"
    local dir="${M2_REPO}/${path}"
    mkdir -p "$dir"
    
    echo "Downloading $group:$artifact:$version from Maven Central..."
    curl -sSL -o "$dir/${artifact}-${version}.pom" "${MavenCentral_URL}/${path}/${artifact}-${version}.pom" || true
    curl -sSL -o "$dir/${artifact}-${version}.jar" "${MavenCentral_URL}/${path}/${artifact}-${version}.jar" || true
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