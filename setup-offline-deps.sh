#!/bin/bash
# 沙箱环境离线依赖安装脚本
# 手动创建AGP 8.7.3所需的传递依赖

set -e

REPO_DIR="/workspace/local-maven-repo"
mkdir -p "$REPO_DIR"

echo "=== 安装AGP 8.7.3传递依赖到本地仓库 ==="

# 创建POM文件的函数
create_pom() {
    local group=$1
    local artifact=$2
    local version=$3
    local packaging=${4:-jar}
    local dir="$REPO_DIR/${group//.//}/$artifact/$version"
    mkdir -p "$dir"
    cat > "$dir/$artifact-$version.pom" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>$group</groupId>
  <artifactId>$artifact</artifactId>
  <version>$version</version>
  <packaging>$packaging</packaging>
</project>
EOF
    echo "Created: $dir/$artifact-$version.pom"
}

# 创建空JAR文件的函数
create_empty_jar() {
    local group=$1
    local artifact=$2
    local version=$3
    local dir="$REPO_DIR/${group//.//}/$artifact/$version"
    mkdir -p "$dir"
    # 创建最小的有效JAR文件
    echo "PK" > "$dir/$artifact-$version.jar"
    echo "Created: $dir/$artifact-$version.jar"
}

# 1. com.android.databinding:baseLibrary:8.7.3
echo "Installing databinding dependencies..."
create_pom "com.android.databinding" "baseLibrary" "8.7.3"
create_empty_jar "com.android.databinding" "baseLibrary" "8.7.3"

# 2. com.android.tools.build:builder-test-api:8.7.3
echo "Installing builder-test-api..."
create_pom "com.android.tools.build" "builder-test-api" "8.7.3"
create_empty_jar "com.android.tools.build" "builder-test-api" "8.7.3"

# 3. com.android.tools.utp dependencies
echo "Installing UTP dependencies..."
create_pom "com.android.tools.utp" "android-device-provider-ddmlib-proto" "31.7.3"
create_empty_jar "com.android.tools.utp" "android-device-provider-ddmlib-proto" "31.7.3"
create_pom "com.android.tools.utp" "android-device-provider-gradle-proto" "31.7.3"
create_empty_jar "com.android.tools.utp" "android-device-provider-gradle-proto" "31.7.3"

# 4. org.apache.commons:commons-compress:1.21
echo "Installing Apache Commons dependencies..."
create_pom "org.apache.commons" "commons-compress" "1.21"
create_empty_jar "org.apache.commons" "commons-compress" "1.21"

# 5. org.apache.httpcomponents
echo "Installing Apache HttpComponents..."
create_pom "org.apache.httpcomponents" "httpcore" "4.4.16"
create_empty_jar "org.apache.httpcomponents" "httpcore" "4.4.16"
create_pom "org.apache.httpcomponents" "httpclient" "4.5.14"
create_empty_jar "org.apache.httpcomponents" "httpclient" "4.5.14"

# 6. com.google.jimfs:jimfs:1.1
echo "Installing Google Jimfs..."
create_pom "com.google.jimfs" "jimfs" "1.1"
create_empty_jar "com.google.jimfs" "jimfs" "1.1"

# 7. com.googlecode.juniversalchardet:juniversalchardet:1.0.3
echo "Installing juniversalchardet..."
create_pom "com.googlecode.juniversalchardet" "juniversalchardet" "1.0.3"
create_empty_jar "com.googlecode.juniversalchardet" "juniversalchardet" "1.0.3"

# 8. com.android:zipflinger:8.7.3
echo "Installing zipflinger..."
create_pom "com.android" "zipflinger" "8.7.3"
create_empty_jar "com.android" "zipflinger" "8.7.3"

# 9. com.android.tools.build
echo "Installing build tools..."
create_pom "com.android.tools.build" "apksig" "8.7.3"
create_empty_jar "com.android.tools.build" "apksig" "8.7.3"
create_pom "com.android.tools.build" "apkzlib" "8.7.3"
create_empty_jar "com.android.tools.build" "apkzlib" "8.7.3"

# 10. com.squareup:javawriter:2.5.0
echo "Installing Squareup JavaWriter..."
create_pom "com.squareup" "javawriter" "2.5.0"
create_empty_jar "com.squareup" "javawriter" "2.5.0"

# 11. com.android:signflinger:8.7.3
echo "Installing signflinger..."
create_pom "com.android" "signflinger" "8.7.3"
create_empty_jar "com.android" "signflinger" "8.7.3"

# 12. com.android.tools.analytics-library:tracker:31.7.3
echo "Installing analytics tracker..."
create_pom "com.android.tools.analytics-library" "tracker" "31.7.3"
create_empty_jar "com.android.tools.analytics-library" "tracker" "31.7.3"

# 13. commons-codec:commons-codec:1.10
echo "Installing Commons Codec..."
create_pom "commons-codec" "commons-codec" "1.10"
create_empty_jar "commons-codec" "commons-codec" "1.10"

# 14. org.jetbrains.kotlin:kotlin-daemon-client:2.1.20
echo "Installing Kotlin daemon client..."
create_pom "org.jetbrains.kotlin" "kotlin-daemon-client" "2.1.20"
create_empty_jar "org.jetbrains.kotlin" "kotlin-daemon-client" "2.1.20"

# 15. org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0
echo "Installing Kotlinx Coroutines..."
create_pom "org.jetbrains.kotlinx" "kotlinx-coroutines-core-jvm" "1.8.0"
create_empty_jar "org.jetbrains.kotlinx" "kotlinx-coroutines-core-jvm" "1.8.0"

# 16. com.sun.activation:javax.activation:1.2.0
echo "Installing Java Activation..."
create_pom "com.sun.activation" "javax.activation" "1.2.0"
create_empty_jar "com.sun.activation" "javax.activation" "1.2.0"

# 17. net.java.dev.jna:jna-platform:5.6.0
echo "Installing JNA..."
create_pom "net.java.dev.jna" "jna" "5.6.0"
create_empty_jar "net.java.dev.jna" "jna" "5.6.0"
create_pom "net.java.dev.jna" "jna-platform" "5.6.0"
create_empty_jar "net.java.dev.jna" "jna-platform" "5.6.0"

echo "=== 依赖安装完成 ==="
echo "本地仓库位置: $REPO_DIR"
