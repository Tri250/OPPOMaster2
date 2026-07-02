#!/bin/bash
# 下载所有必要的Gradle插件依赖

LOCAL_REPO="/workspace/local-maven-repo"

download_artifact() {
    local group=$1
    local artifact=$2
    local version=$3
    local repo_url=$4

    # 转换group路径
    local group_path=$(echo $group | tr '.' '/')
    local artifact_dir="$LOCAL_REPO/$group_path/$artifact/$version"

    mkdir -p $artifact_dir

    echo "下载: $group:$artifact:$version"

    # 下载JAR
    curl -L -s -o "$artifact_dir/$artifact-$version.jar" "$repo_url/$group_path/$artifact/$version/$artifact-$version.jar"
    # 下载POM
    curl -L -s -o "$artifact_dir/$artifact-$version.pom" "$repo_url/$group_path/$artifact/$version/$artifact-$version.pom"

    # 清理无效文件
    if [ -f "$artifact_dir/$artifact-$version.jar" ] && [ $(stat -c%s "$artifact_dir/$artifact-$version.jar" 2>/dev/null) -lt 1024 ]; then
        rm -f "$artifact_dir/$artifact-$version.jar"
    fi
    if [ -f "$artifact_dir/$artifact-$version.pom" ] && [ $(stat -c%s "$artifact_dir/$artifact-$version.pom" 2>/dev/null) -lt 200 ]; then
        rm -f "$artifact_dir/$artifact-$version.pom"
    fi

    ls -la $artifact_dir
}

echo "===== 下载Kotlin插件依赖 ====="

# Kotlin插件标记符
download_artifact "org.jetbrains.kotlin.plugin.compose" "org.jetbrains.kotlin.plugin.compose.gradle.plugin" "2.1.20" "https://plugins.gradle.org/m2"
download_artifact "org.jetbrains.kotlin.plugin.serialization" "org.jetbrains.kotlin.plugin.serialization.gradle.plugin" "2.1.20" "https://plugins.gradle.org/m2"
download_artifact "org.jetbrains.kotlin.plugin.parcelize" "org.jetbrains.kotlin.plugin.parcelize.gradle.plugin" "2.1.20" "https://plugins.gradle.org/m2"

# Kotlin核心库
download_artifact "org.jetbrains.kotlin" "kotlin-stdlib" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-stdlib-jdk8" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-reflect" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-compiler-embeddable" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-compose-compiler-plugin" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-scripting-compiler-embeddable" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-sam-with-receiver-compiler-plugin" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-serialization-compiler-plugin" "2.1.20" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlin" "kotlin-parcelize-compiler-plugin" "2.1.20" "https://maven.aliyun.com/repository/public"

# kotlinx-serialization
download_artifact "org.jetbrains.kotlinx" "kotlinx-serialization-core" "1.8.0" "https://maven.aliyun.com/repository/public"
download_artifact "org.jetbrains.kotlinx" "kotlinx-serialization-json" "1.8.0" "https://maven.aliyun.com/repository/public"

echo "===== 下载完成 ====="