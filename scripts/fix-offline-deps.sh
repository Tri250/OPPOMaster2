#!/bin/bash
# =============================================================================
# 沙盒网络受限 - 离线依赖补全脚本
# =============================================================================
# 从 Gradle 分发包中提取可用的 JAR，补全 local-maven-repo 中缺失的传递依赖
#
# 策略：
#   1. 从 Gradle 分发包复制可用 JAR（版本替换）
#   2. 为 POM-only 工件创建 POM 文件
#   3. 为 Kotlin 子模块创建桩 JAR（主 JAR 已包含部分类）
#   4. 为完全缺失的工件创建空桩 JAR
# =============================================================================

set -eo pipefail

# 算术运算不因结果为 0 而失败
count_ok=0
count_stub=0
count_pom=0

GRADLE_HOME="/root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4"
GRADLE_LIB="$GRADLE_HOME/lib"
GRADLE_PLUGINS="$GRADLE_LIB/plugins"
REPO="/workspace/local-maven-repo"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 创建 Maven 目录结构
maven_dir() {
    local group=$1 artifact=$2 version=$3
    local group_path=$(echo "$group" | tr '.' '/')
    echo "$REPO/$group_path/$artifact/$version"
}

# 从 Gradle 分发包安装 JAR（版本替换）
install_from_gradle() {
    local src_jar=$1 group=$2 artifact=$3 version=$4
    local dir=$(maven_dir "$group" "$artifact" "$version")
    mkdir -p "$dir"
    cp "$src_jar" "$dir/$artifact-$version.jar"
    cat > "$dir/$artifact-$version.pom" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>$group</groupId>
    <artifactId>$artifact</artifactId>
    <version>$version</version>
</project>
EOF
    echo -e "${GREEN}  [GRADLE] $group:$artifact:$version${NC} (from $(basename $src_jar))"
    count_ok=$((count_ok + 1))
}

# 创建 POM-only 工件（无 JAR）
install_pom_only() {
    local group=$1 artifact=$2 version=$3
    local deps_xml=${4:-}
    local dir=$(maven_dir "$group" "$artifact" "$version")
    mkdir -p "$dir"
    cat > "$dir/$artifact-$version.pom" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>$group</groupId>
    <artifactId>$artifact</artifactId>
    <version>$version</version>
    <packaging>pom</packaging>
$deps_xml
</project>
EOF
    echo -e "${YELLOW}  [POM]    $group:$artifact:$version${NC}"
    count_pom=$((count_pom + 1))
}

# 创建桩 JAR（空 JAR + POM）
install_stub_jar() {
    local group=$1 artifact=$2 version=$3
    local dir=$(maven_dir "$group" "$artifact" "$version")
    mkdir -p "$dir"

    # 创建空 JAR
    local tmpdir=$(mktemp -d)
    (cd "$tmpdir" && jar cf "$dir/$artifact-$version.jar" .)
    rm -rf "$tmpdir"

    cat > "$dir/$artifact-$version.pom" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>$group</groupId>
    <artifactId>$artifact</artifactId>
    <version>$version</version>
</project>
EOF
    echo -e "${RED}  [STUB]   $group:$artifact:$version${NC}"
    count_stub=$((count_stub + 1))
}

echo "=========================================="
echo " 离线依赖补全脚本"
echo "=========================================="
echo ""

# =============================================================================
# 1. 从 Gradle 分发包复制可用 JAR（版本替换）
# =============================================================================
echo "--- 1. 从 Gradle 分发包复制可用 JAR ---"

# ASM 字节码操作库 (Gradle 9.8 → AGP 需要 9.6)
install_from_gradle "$GRADLE_LIB/asm-9.8.jar" "org.ow2.asm" "asm" "9.6"
install_from_gradle "$GRADLE_LIB/asm-commons-9.8.jar" "org.ow2.asm" "asm-commons" "9.6"
install_from_gradle "$GRADLE_LIB/asm-tree-9.8.jar" "org.ow2.asm" "asm-tree" "9.6"

# Commons IO (Gradle 2.15.1 → AGP 需要 2.13.0)
install_from_gradle "$GRADLE_LIB/commons-io-2.15.1.jar" "commons-io" "commons-io" "2.13.0"

# Commons Codec (Gradle 1.16.1 → AGP 需要 1.10)
install_from_gradle "$GRADLE_LIB/commons-codec-1.16.1.jar" "commons-codec" "commons-codec" "1.10"

# Apache Commons Compress (Gradle 1.26.1 → AGP 需要 1.21)
install_from_gradle "$GRADLE_LIB/commons-compress-1.26.1.jar" "org.apache.commons" "commons-compress" "1.21"

# Guava (Gradle 33.4.6 → AGP 需要 32.0.1)
install_from_gradle "$GRADLE_LIB/guava-33.4.6-jre.jar" "com.google.guava" "guava" "32.0.1-jre"

# Gson (Gradle 2.10 → AGP 需要 2.10.1)
install_from_gradle "$GRADLE_LIB/gson-2.10.jar" "com.google.code.gson" "gson" "2.10.1"

# javax.inject (精确匹配)
install_from_gradle "$GRADLE_LIB/javax.inject-1.jar" "javax.inject" "javax.inject" "1"

# Apache HttpComponents (Gradle 4.5.14 → AGP 需要 4.5.14, 精确匹配)
install_from_gradle "$GRADLE_LIB/httpclient-4.5.14.jar" "org.apache.httpcomponents" "httpclient" "4.5.14"
# httpcore (Gradle 4.4.14 → AGP 需要 4.4.16)
install_from_gradle "$GRADLE_LIB/httpcore-4.4.14.jar" "org.apache.httpcomponents" "httpcore" "4.4.16"

# trove4j (精确匹配)
install_from_gradle "$GRADLE_LIB/trove4j-1.0.20200330.jar" "org.jetbrains.intellij.deps" "trove4j" "1.0.20200330"

# BouncyCastle (Gradle plugins 1.78.1 → AGP 需要 1.77)
install_from_gradle "$GRADLE_PLUGINS/bcprov-jdk18on-1.78.1.jar" "org.bouncycastle" "bcprov-jdk18on" "1.77"
# bcpkix - 注意: Gradle 有 bcpg (PGP) 不是 bcpkix (PKIX)，使用 bcutil 作为替代
install_from_gradle "$GRADLE_PLUGINS/bcutil-jdk18on-1.78.1.jar" "org.bouncycastle" "bcpkix-jdk18on" "1.77"

# jsr305 (从 Gradle lib)
install_from_gradle "$GRADLE_LIB/jsr305-3.0.2.jar" "com.google.code.findbugs" "jsr305" "3.0.2"

# =============================================================================
# 2. 创建 POM-only 工件
# =============================================================================
echo ""
echo "--- 2. 创建 POM-only 工件 ---"

# kotlin-stdlib-jdk8:1.9.20 (POM-only, 重定向到 kotlin-stdlib)
install_pom_only "org.jetbrains.kotlin" "kotlin-stdlib-jdk8" "1.9.20" \
'    <dependencies>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>2.1.20</version>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib-jdk7</artifactId>
            <version>2.1.20</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>'

# kotlin-reflect:1.9.20 (POM-only, 重定向到 kotlin-reflect:2.1.20)
install_pom_only "org.jetbrains.kotlin" "kotlin-reflect" "1.9.20" \
'    <dependencies>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-reflect</artifactId>
            <version>2.1.20</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>'

# org.jetbrains:annotations:23.0.0 (POM-only, 重定向到 24.1.0)
install_pom_only "org.jetbrains" "annotations" "23.0.0" \
'    <dependencies>
        <dependency>
            <groupId>org.jetbrains</groupId>
            <artifactId>annotations</artifactId>
            <version>24.1.0</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>'

# transform-api (已废弃的空工件)
install_pom_only "com.android.tools.build" "transform-api" "2.0.0-deprecated-use-gradle-api"

# =============================================================================
# 3. 为 Kotlin 子模块创建桩 JAR
# =============================================================================
echo ""
echo "--- 3. 创建 Kotlin 子模块桩 JAR ---"

# Kotlin Gradle Plugin 子模块 - 主 JAR 可能包含部分类
install_stub_jar "org.jetbrains.kotlin" "kotlin-gradle-plugin-api" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "kotlin-gradle-plugin-model" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "fus-statistics-gradle-plugin" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "kotlin-gradle-plugin-idea" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "kotlin-gradle-plugin-idea-proto" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "kotlin-klib-commonizer-api" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "kotlin-build-statistics" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "kotlin-util-klib-metadata" "2.1.20"
install_stub_jar "org.jetbrains.kotlin" "compose-compiler-gradle-plugin" "2.1.20"

# =============================================================================
# 4. 为完全缺失的工件创建桩 JAR
# =============================================================================
echo ""
echo "--- 4. 创建缺失工件桩 JAR ---"

# Android Build Tools 缺失工件
install_stub_jar "com.android.databinding" "baseLibrary" "8.7.3"
install_stub_jar "com.android.tools.analytics-library" "tracker" "31.7.3"
install_stub_jar "com.android.tools.build" "apksig" "8.7.3"
install_stub_jar "com.android.tools.build" "apkzlib" "8.7.3"
install_stub_jar "com.android.tools.build" "builder-test-api" "8.7.3"
install_stub_jar "com.android.tools.build" "bundletool" "1.17.1"
install_stub_jar "com.android.tools" "dvlib" "31.7.3"
install_stub_jar "com.android" "signflinger" "8.7.3"
install_stub_jar "com.android" "zipflinger" "8.7.3"

# Jetifier
install_stub_jar "com.android.tools.build.jetifier" "jetifier-core" "1.0.0-beta10"
install_stub_jar "com.android.tools.build.jetifier" "jetifier-processor" "1.0.0-beta10"

# Android UTP (Unified Test Platform) proto 模块
install_stub_jar "com.android.tools.utp" "android-device-provider-ddmlib-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-device-provider-gradle-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-device-provider-profile-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-test-plugin-host-additional-test-output-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-test-plugin-host-apk-installer-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-test-plugin-host-coverage-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-test-plugin-host-emulator-control-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-test-plugin-host-logcat-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-test-plugin-host-retention-proto" "31.7.3"
install_stub_jar "com.android.tools.utp" "android-test-plugin-result-listener-gradle-proto" "31.7.3"

# Google 库
install_stub_jar "com.google.crypto.tink" "tink" "1.7.0"
install_stub_jar "com.google.flatbuffers" "flatbuffers-java" "1.12.0"
install_stub_jar "com.google.jimfs" "jimfs" "1.1"
install_stub_jar "com.google.protobuf" "protobuf-java" "3.22.3"
install_stub_jar "com.google.protobuf" "protobuf-java-util" "3.22.3"
install_stub_jar "com.google.testing.platform" "core-proto" "0.0.9-alpha02"

# 其他缺失库
install_stub_jar "com.googlecode.juniversalchardet" "juniversalchardet" "1.0.3"
install_stub_jar "com.squareup" "javapoet" "1.10.0"
install_stub_jar "com.squareup" "javawriter" "2.5.0"
install_stub_jar "com.sun.activation" "javax.activation" "1.2.0"

# gRPC
install_stub_jar "io.grpc" "grpc-core" "1.57.0"
install_stub_jar "io.grpc" "grpc-netty" "1.57.0"
install_stub_jar "io.grpc" "grpc-protobuf" "1.57.0"
install_stub_jar "io.grpc" "grpc-stub" "1.57.0"

# 其他
install_stub_jar "net.java.dev.jna" "jna-platform" "5.6.0"
install_stub_jar "net.sf.jopt-simple" "jopt-simple" "4.9"
install_stub_jar "net.sf.kxml" "kxml2" "2.3.0"
install_stub_jar "org.glassfish.jaxb" "jaxb-runtime" "2.3.2"
install_stub_jar "org.tensorflow" "tensorflow-lite-metadata" "0.1.0-rc2"

# httpmime (Apache HttpComponents MIME)
install_stub_jar "org.apache.httpcomponents" "httpmime" "4.5.6"

# ASM 缺失模块
install_stub_jar "org.ow2.asm" "asm-analysis" "9.6"
install_stub_jar "org.ow2.asm" "asm-util" "9.6"

# =============================================================================
# 总结
# =============================================================================
echo ""
echo "=========================================="
echo " 补全完成"
echo "=========================================="
echo -e "  ${GREEN}从 Gradle 分发包复制: $count_ok${NC}"
echo -e "  ${YELLOW}POM-only 工件: $count_pom${NC}"
echo -e "  ${RED}桩 JAR 工件: $count_stub${NC}"
echo ""
echo "注意: 桩 JAR 是空文件，仅用于通过依赖解析。"
echo "      如果构建时出现 ClassNotFoundException，"
echo "      说明对应的库需要在联网环境中预下载。"
echo ""
