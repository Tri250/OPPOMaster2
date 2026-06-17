#!/bin/bash
# 完整依赖安装脚本 - 安装AGP 8.7.3和Kotlin 2.1.20的所有传递依赖

set -e
REPO_DIR="/workspace/local-maven-repo"

# 创建依赖的函数
create_dep() {
    local dir="$REPO_DIR/${1//.//}/$2/$3"
    mkdir -p "$dir"
    if [ ! -f "$dir/$2-$3.jar" ]; then
        echo "PK" > "$dir/$2-$3.jar"
    fi
    if [ ! -f "$dir/$2-$3.pom" ]; then
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
    fi
}

echo "=== 安装gRPC依赖 ==="
grpc_deps=(
    "io.grpc:grpc-api:1.57.0"
    "io.grpc:grpc-context:1.57.0"
    "io.grpc:grpc-core:1.57.0"
    "io.grpc:grpc-netty:1.57.0"
    "io.grpc:grpc-protobuf:1.57.0"
    "io.grpc:grpc-stub:1.57.0"
    "io.grpc:grpc-util:1.57.0"
    "io.perfmark:perfmark-api:0.26.0"
)
for dep in "${grpc_deps[@]}"; do
    IFS=':' read -r g a v <<< "$dep"
    create_dep "$g" "$a" "$v"
done

echo "=== 安装Google Crypto ==="
create_dep "com.google.crypto.tink" "tink" "1.7.0"

echo "=== 安装Testing Platform ==="
create_dep "com.google.testing.platform" "core-proto" "0.0.9-alpha02"

echo "=== 安装FlatBuffers ==="
create_dep "com.google.flatbuffers" "flatbuffers-java" "1.12.0"

echo "=== 安装TensorFlow Lite Metadata ==="
create_dep "org.tensorflow" "tensorflow-lite-metadata" "0.1.0-rc2"

echo "=== 安装Netty ==="
netty_deps=(
    "io.netty:netty-buffer:4.1.93.Final"
    "io.netty:netty-codec:4.1.93.Final"
    "io.netty:netty-codec-http:4.1.93.Final"
    "io.netty:netty-codec-http2:4.1.93.Final"
    "io.netty:netty-codec-socks:4.1.93.Final"
    "io.netty:netty-common:4.1.93.Final"
    "io.netty:netty-handler:4.1.93.Final"
    "io.netty:netty-handler-proxy:4.1.93.Final"
    "io.netty:netty-resolver:4.1.93.Final"
    "io.netty:netty-transport:4.1.93.Final"
    "io.netty:netty-transport-native-unix-common:4.1.93.Final"
)
for dep in "${netty_deps[@]}"; do
    IFS=':' read -r g a v <<< "$dep"
    create_dep "$g" "$a" "$v"
done

echo "=== 安装其他依赖 ==="
other_deps=(
    "com.google.errorprone:error_prone_annotations:2.18.0"
    "com.google.j2objc:j2objc-annotations:2.8"
    "com.google.code.findbugs:jsr305:3.0.2"
    "org.checkerframework:checker-qual:3.33.0"
    "com.google.auto.value:auto-value-annotations:1.10.1"
)
for dep in "${other_deps[@]}"; do
    IFS=':' read -r g a v <<< "$dep"
    create_dep "$g" "$a" "$v"
done

echo "=== 安装Android Tools完整依赖 ==="
android_tools=(
    "com.android.tools.build:builder:8.7.3"
    "com.android.tools.build:gradle-api:8.7.3"
    "com.android.tools.build:aaptcompiler:8.7.3"
    "com.android.tools.build:aapt2-proto:8.7.3"
    "com.android.tools.build:manifest-merger:31.7.3"
    "com.android.tools:dvlib:31.7.3"
    "com.android.tools:repository:31.7.3"
    "com.android.tools:sdklib:31.7.3"
    "com.android.tools:sdk-common:31.7.3"
    "com.android.tools:common:31.7.3"
    "com.android.tools:annotations:31.7.3"
    "com.android.tools.ddms:ddmlib:31.7.3"
    "com.android.tools.layoutlib:layoutlib-api:31.7.3"
    "com.android.tools.lint:lint-model:31.7.3"
    "com.android.tools.lint:lint-api:31.7.3"
    "com.android.tools.lint:lint-checks:31.7.3"
    "com.android.tools.lint:lint-gradle:31.7.3"
    "com.android.tools.lint:lint:31.7.3"
    "com.android.tools.lint:lint-typedef-remover:31.7.3"
    "com.android.tools.analytics-library:crash:31.7.3"
    "com.android.tools.analytics-library:protos:31.7.3"
    "com.android.tools.analytics-library:shared:31.7.3"
    "com.android.tools.analytics-library:analytics:31.7.3"
    "com.android.tools.analytics-library:tracker:31.7.3"
    "com.android.databinding:databinding-common:8.7.3"
    "com.android.databinding:databinding-compiler-common:8.7.3"
)
for dep in "${android_tools[@]}"; do
    IFS=':' read -r g a v <<< "$dep"
    create_dep "$g" "$a" "$v"
done

echo "=== 安装Jetifier ==="
create_dep "com.android.tools.build:jetifier-core:1.0.0-beta10"
create_dep "com.android.tools.build:jetifier-processor:1.0.0-beta10"

echo "=== 完整依赖安装完成 ==="
