#!/bin/bash
# 修复 Gradle 插件 marker artifact
# 当使用本地 Maven 仓库进行离线构建时，Gradle 插件 portal 的 marker artifact 必须存在。
# 本脚本根据 local-maven-repo 中已有的插件依赖，自动生成对应的 plugin marker。

set -e

REPO_DIR="${1:-${REPO_DIR:-$(dirname "$0")/../local-maven-repo}}"
REPO_DIR="$(cd "$REPO_DIR" && pwd)"

echo "修复本地 Maven 仓库的 Gradle 插件 marker: $REPO_DIR"

mkdir -p "$REPO_DIR"

cat > /tmp/marker-template.pom << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>@@GROUP_ID@@</groupId>
  <artifactId>@@ARTIFACT_ID@@</artifactId>
  <version>@@VERSION@@</version>
  <packaging>pom</packaging>
  <dependencies>
    <dependency>
      <groupId>@@IMPL_GROUP_ID@@</groupId>
      <artifactId>@@IMPL_ARTIFACT_ID@@</artifactId>
      <version>@@VERSION@@</version>
    </dependency>
  </dependencies>
</project>
EOF

write_marker() {
  local plugin_group="$1"
  local plugin_name="$2"
  local impl_group="$3"
  local impl_name="$4"
  local version="$5"

  local marker_dir="$REPO_DIR/${plugin_group//./\/}/${plugin_name}/${version}"
  local marker_file="$marker_dir/${plugin_name}-${version}.pom"

  mkdir -p "$marker_dir"
  sed \
    -e "s/@@GROUP_ID@@/${plugin_group}/g" \
    -e "s/@@ARTIFACT_ID@@/${plugin_name}/g" \
    -e "s/@@VERSION@@/${version}/g" \
    -e "s/@@IMPL_GROUP_ID@@/${impl_group}/g" \
    -e "s/@@IMPL_ARTIFACT_ID@@/${impl_name}/g" \
    /tmp/marker-template.pom > "$marker_file"

  echo "  ✓ $plugin_group:$plugin_name:$version -> $impl_group:$impl_name:$version"
}

# 根据项目 build.gradle.kts 中使用的插件生成 marker
# 版本号需与 gradle/libs.versions.toml 保持一致
AGP_VERSION="8.7.3"
KOTLIN_VERSION="2.0.21"

# Android Gradle Plugin
write_marker \
  "com.android.application" \
  "com.android.application.gradle.plugin" \
  "com.android.tools.build" \
  "gradle" \
  "$AGP_VERSION"

# Kotlin 插件
write_marker \
  "org.jetbrains.kotlin.android" \
  "org.jetbrains.kotlin.android.gradle.plugin" \
  "org.jetbrains.kotlin" \
  "kotlin-gradle-plugin" \
  "$KOTLIN_VERSION"

write_marker \
  "org.jetbrains.kotlin.plugin.compose" \
  "org.jetbrains.kotlin.plugin.compose.gradle.plugin" \
  "org.jetbrains.kotlin" \
  "kotlin-compose-compiler-plugin-embeddable" \
  "$KOTLIN_VERSION"

write_marker \
  "org.jetbrains.kotlin.plugin.serialization" \
  "org.jetbrains.kotlin.plugin.serialization.gradle.plugin" \
  "org.jetbrains.kotlin" \
  "kotlin-serialization" \
  "$KOTLIN_VERSION"

write_marker \
  "org.jetbrains.kotlin.plugin.parcelize" \
  "org.jetbrains.kotlin.plugin.parcelize.gradle.plugin" \
  "org.jetbrains.kotlin" \
  "kotlin-parcelize-compiler" \
  "$KOTLIN_VERSION"

echo "完成。若后续新增插件，请补充此脚本并重新运行。"
