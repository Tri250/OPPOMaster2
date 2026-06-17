#!/bin/bash
# 安装所有Kotlin 2.1.20相关依赖

set -e
REPO_DIR="/workspace/local-maven-repo"

# 创建依赖的函数
create_dep() {
    local dir="$REPO_DIR/${1//.//}/$2/$3"
    mkdir -p "$dir"
    echo "PK" > "$dir/$2-$3.jar"
    cat > "$dir/$2-$3.pom" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>$1</groupId>
  <artifactId>$2</artifactId>
  <version>$3</version>
  <packaging>jar</packaging>
</project>
EOF
    echo "Created: $1:$2:$3"
}

echo "=== 安装Kotlin Gradle Plugin完整依赖 ==="

# Kotlin Gradle Plugin 核心依赖
kotlin_deps=(
    "org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.20"
    "org.jetbrains.kotlin:kotlin-gradle-plugin-model:2.1.20"
    "org.jetbrains.kotlin:kotlin-gradle-plugin-idea:2.1.20"
    "org.jetbrains.kotlin:kotlin-gradle-plugin-idea-proto:2.1.20"
    "org.jetbrains.kotlin:kotlin-klib-commonizer-api:2.1.20"
    "org.jetbrains.kotlin:kotlin-build-statistics:2.1.20"
    "org.jetbrains.kotlin:kotlin-util-klib-metadata:2.1.20"
    "org.jetbrains.kotlin:kotlin-util-klib:2.1.20"
    "org.jetbrains.kotlin:kotlin-native-utils:2.1.20"
    "org.jetbrains.kotlin:kotlin-project-model:2.1.20"
    "org.jetbrains.kotlin:kotlin-tooling-core:2.1.20"
    "org.jetbrains.kotlin:kotlin-compiler-runner:2.1.20"
    "org.jetbrains.kotlin:kotlin-daemon-client:2.1.20"
    "org.jetbrains.kotlin:kotlin-build-tools-api:2.1.20"
    "org.jetbrains.kotlin:kotlin-compiler-daemon-embeddable:2.1.20"
    "org.jetbrains.kotlin:kotlin-annotation-processing-gradle:2.1.20"
)

for dep in "${kotlin_deps[@]}"; do
    IFS=':' read -r group artifact version <<< "$dep"
    create_dep "$group" "$artifact" "$version"
done

echo "=== Kotlin依赖安装完成 ==="
