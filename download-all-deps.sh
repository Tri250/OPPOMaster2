#!/bin/bash
# 批量下载 libs.versions.toml 中所有依赖
# 使用多镜像回退：阿里云 > 腾讯云 > 官方

LOCAL_REPO="/workspace/local-maven-repo"
# 镜像源
ALIYUN_PUBLIC="https://maven.aliyun.com/repository/public"
ALIYUN_GOOGLE="https://maven.aliyun.com/repository/google"
TENCENT="https://mirrors.cloud.tencent.com/nexus/repository/maven-public"
TENCENT_GRADLE="https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins"
MVN_CENTRAL="https://repo1.maven.org/maven2"
GOOGLE_OFFICIAL="https://dl.google.com/android/maven2"

# 根据 group 选择仓库
pick_repo() {
    local group=$1
    case "$group" in
        androidx.*|com.android.*|com.google.android.*|androidx.*)
            echo "$ALIYUN_GOOGLE $TENCENT $GOOGLE_OFFICIAL"
            ;;
        org.jetbrains.*|org.jetbrains.kotlinx*)
            echo "$ALIYUN_PUBLIC $TENCENT $MVN_CENTRAL"
            ;;
        com.google.*|io.ktor*|io.coil-kt*|io.mockk*)
            echo "$ALIYUN_PUBLIC $TENCENT $MVN_CENTRAL"
            ;;
        junit)
            echo "$ALIYUN_PUBLIC $TENCENT $MVN_CENTRAL"
            ;;
        *)
            echo "$ALIYUN_PUBLIC $TENCENT $MVN_CENTRAL"
            ;;
    esac
}

# 严格下载（带大小校验 - 阈值500字节足以区分 KMP 占位 [200-300B] 和真实 aar [1KB+]）
download() {
    local group=$1
    local artifact=$2
    local version=$3
    local group_path=$(echo $group | tr '.' '/')
    local dir="$LOCAL_REPO/$group_path/$artifact/$version"
    mkdir -p "$dir"

    local repos=($(pick_repo "$group"))

    local jar_ok=false
    for repo in "${repos[@]}"; do
        curl -L --max-time 30 -s -o "$dir/$artifact-$version.jar" "$repo/$group_path/$artifact/$version/$artifact-$version.jar"
        local sz=$(stat -c%s "$dir/$artifact-$version.jar" 2>/dev/null)
        if [ "$sz" -gt 500 ]; then
            jar_ok=true
            break
        fi
    done
    [ "$jar_ok" = false ] && rm -f "$dir/$artifact-$version.jar" && return 1

    local pom_ok=false
    for repo in "${repos[@]}"; do
        curl -L --max-time 30 -s -o "$dir/$artifact-$version.pom" "$repo/$group_path/$artifact/$version/$artifact-$version.pom"
        local sz=$(stat -c%s "$dir/$artifact-$version.pom" 2>/dev/null)
        if [ "$sz" -gt 200 ]; then
            pom_ok=true
            break
        fi
    done
    [ "$pom_ok" = false ] && rm -f "$dir/$artifact-$version.pom" && return 1

    return 0
}

# 处理 aar 文件
download_aar() {
    local group=$1
    local artifact=$2
    local version=$3
    local group_path=$(echo $group | tr '.' '/')
    local dir="$LOCAL_REPO/$group_path/$artifact/$version"
    mkdir -p "$dir"

    local repos=($(pick_repo "$group"))

    local ok=false
    for repo in "${repos[@]}"; do
        curl -L --max-time 30 -s -o "$dir/$artifact-$version.aar" "$repo/$group_path/$artifact/$version/$artifact-$version.aar"
        local sz=$(stat -c%s "$dir/$artifact-$version.aar" 2>/dev/null)
        if [ "$sz" -gt 500 ]; then
            ok=true
            break
        fi
    done
    [ "$ok" = false ] && rm -f "$dir/$artifact-$version.aar" && return 1

    for repo in "${repos[@]}"; do
        curl -L --max-time 30 -s -o "$dir/$artifact-$version.pom" "$repo/$group_path/$artifact/$version/$artifact-$version.pom"
        local sz=$(stat -c%s "$dir/$artifact-$version.pom" 2>/dev/null)
        if [ "$sz" -gt 200 ]; then
            break
        fi
    done
    return 0
}

# AndroidX Core（KMP split: -android 后缀才是真包）
echo "===== AndroidX Core ====="
download_aar androidx.core core-ktx 1.15.0
download_aar androidx.core core-ktx-android 1.15.0
download_aar androidx.core core 1.15.0
download_aar androidx.lifecycle lifecycle-runtime-ktx 2.8.7
download_aar androidx.lifecycle lifecycle-runtime-ktx-android 2.8.7
download_aar androidx.lifecycle lifecycle-runtime-compose 2.8.7
download_aar androidx.lifecycle lifecycle-runtime-compose-android 2.8.7
download_aar androidx.lifecycle lifecycle-runtime 2.8.7
download_aar androidx.lifecycle lifecycle-runtime-android 2.8.7
download_aar androidx.lifecycle lifecycle-viewmodel-compose 2.8.7
download_aar androidx.lifecycle lifecycle-viewmodel-compose-android 2.8.7
download_aar androidx.lifecycle lifecycle-viewmodel-ktx 2.8.7
download_aar androidx.lifecycle lifecycle-viewmodel-ktx-android 2.8.7
download_aar androidx.lifecycle lifecycle-viewmodel 2.8.7
download_aar androidx.lifecycle lifecycle-viewmodel-android 2.8.7
download_aar androidx.lifecycle lifecycle-common 2.8.7
download_aar androidx.activity activity-compose 1.9.3
download_aar androidx.activity activity-compose-android 1.9.3
download_aar androidx.activity activity-ktx 1.9.3
download_aar androidx.activity activity-ktx-android 1.9.3
download_aar androidx.activity activity 1.9.3
download_aar androidx.core core-splashscreen 1.0.1

# Compose BOM 2025.01.01（实际版本 1.7.7，使用 KMP split：-android 后缀才是真正的 Android 库）
echo "===== Compose ====="
download_aar androidx.compose compose-bom 2025.01.01
# 注意:Compose 1.7+ 引入 KMP 拆分，android 专属 artifact 是 -android 后缀
download_aar androidx.compose.ui ui-android 1.7.7
download_aar androidx.compose.ui ui 1.7.7
download_aar androidx.compose.ui ui-graphics-android 1.7.7
download_aar androidx.compose.ui ui-graphics 1.7.7
download_aar androidx.compose.ui ui-tooling-android 1.7.7
download_aar androidx.compose.ui ui-tooling 1.7.7
download_aar androidx.compose.ui ui-tooling-preview-android 1.7.7
download_aar androidx.compose.ui ui-tooling-preview 1.7.7
download_aar androidx.compose.ui ui-test-manifest-android 1.7.7
download_aar androidx.compose.ui ui-test-manifest 1.7.7
download_aar androidx.compose.ui ui-test-junit4-android 1.7.7
download_aar androidx.compose.ui ui-test-junit4 1.7.7
download_aar androidx.compose.ui ui-text-android 1.7.7
download_aar androidx.compose.ui ui-util-android 1.7.7
download_aar androidx.compose.foundation foundation-android 1.7.7
download_aar androidx.compose.foundation foundation-layout-android 1.7.7
download_aar androidx.compose.material3 material3 1.3.1
download_aar androidx.compose.material3 material3-android 1.3.1
download_aar androidx.compose.material material-android 1.7.7
download_aar androidx.compose.material material 1.7.7
download_aar androidx.compose.material material-icons-core-android 1.7.7
download_aar androidx.compose.material material-icons-core 1.7.7
download_aar androidx.compose.material material-icons-extended-android 1.7.7
download_aar androidx.compose.material material-icons-extended 1.7.7
download_aar androidx.compose.runtime runtime-android 1.7.7
download_aar androidx.compose.runtime runtime 1.7.7
download_aar androidx.compose.animation animation-android 1.7.7
download_aar androidx.compose.animation animation 1.7.7

# Navigation
echo "===== Navigation ====="
download_aar androidx.navigation navigation-compose 2.8.5
download_aar androidx.navigation navigation-compose-android 2.8.5
download_aar androidx.navigation navigation-common 2.8.5
download_aar androidx.navigation navigation-runtime 2.8.5
download_aar androidx.navigation navigation-runtime-android 2.8.5

# DataStore
echo "===== DataStore ====="
download_aar androidx.datastore datastore-preferences 1.1.1
download_aar androidx.datastore datastore-preferences-android 1.1.1
download_aar androidx.datastore datastore-core 1.1.1
download_aar androidx.datastore datastore-core-android 1.1.1

# 其他 AndroidX
echo "===== 其他AndroidX ====="
download_aar androidx.test.ext junit 1.3.0
download_aar androidx.test.espresso espresso-core 3.7.0
download_aar androidx.test.espresso espresso-core 3.7.0
download_aar androidx.savedstate savedstate 1.2.1
download_aar androidx.savedstate savedstate-ktx 1.2.1

# Kotlin 扩展库
echo "===== Kotlinx ====="
download org.jetbrains.kotlinx kotlinx-coroutines-core 1.10.1
download org.jetbrains.kotlinx kotlinx-coroutines-android 1.10.1
download org.jetbrains.kotlinx kotlinx-coroutines-play-services 1.10.1
download org.jetbrains.kotlinx kotlinx-coroutines-test 1.10.1
download org.jetbrains.kotlinx kotlinx-serialization-json 1.8.0
download org.jetbrains.kotlinx kotlinx-serialization-core 1.8.0
download org.jetbrains.kotlinx kotlinx-serialization-bom 1.8.0

# Networking（coil 是 aar）
echo "===== Networking ====="
download_aar io.coil-kt coil-compose 2.7.0
download_aar io.coil-kt coil 2.7.0
download_aar io.coil-kt coil-base 2.7.0
download io.ktor ktor-client-core 3.0.3
download io.ktor ktor-client-cio 3.0.3
download io.ktor ktor-client-content-negotiation 3.0.3
download io.ktor ktor-serialization-kotlinx-json 3.0.3
download io.ktor ktor-client-core-jvm 3.0.3
download io.ktor ktor-client-cio-jvm 3.0.3
download io.ktor ktor-client-content-negotiation-jvm 3.0.3
download io.ktor ktor-serialization-kotlinx-json-jvm 3.0.3
download io.ktor ktor-io 3.0.3
download io.ktor ktor-io-jvm 3.0.3
download io.ktor ktor-utils 3.0.3
download io.ktor ktor-utils-jvm 3.0.3
download io.ktor ktor-events 3.0.3
download io.ktor ktor-events-jvm 3.0.3
download com.google.code.gson gson 2.11.0

# ML Kit
echo "===== ML Kit ====="
download_aar com.google.mlkit face-detection 16.1.7
download_aar com.google.mlkit common 18.10.0
download_aar com.google.mlkit vision-common 17.3.0
download_aar com.google.mlkit vision-interfaces 16.0.0

# TensorFlow Lite
echo "===== TensorFlow Lite ====="
download_aar org.tensorflow tensorflow-lite 2.16.1
download_aar org.tensorflow tensorflow-lite-gpu 2.16.1
download_aar org.tensorflow tensorflow-lite-support 0.4.4
download_aar org.tensorflow tensorflow-lite-support-library 0.4.4
download_aar org.tensorflow tensorflow-lite-api 2.16.1

# Testing
echo "===== Testing ====="
download junit junit 4.13.2
download io.mockk mockk 1.13.12
download io.mockk mockk-jvm 1.13.12
download io.mockk mockk-android 1.13.12

# 清理
echo "===== 清理无效文件 ====="
find "$LOCAL_REPO" -type f \( -name "*.jar" -o -name "*.aar" -o -name "*.pom" \) -size -1k -delete
find "$LOCAL_REPO" -type d -empty -delete

echo "===== 完成 ====="
echo "总文件数: $(find "$LOCAL_REPO" -type f | wc -l)"
echo "总大小: $(du -sh "$LOCAL_REPO" | cut -f1)"
