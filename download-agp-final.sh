#!/bin/bash
# AGP 8.7.3 三次依赖补全 - 终极解决方案
# 处理：parent POM、guava 完整依赖链、Gradle 内部

set -e

LOCAL_REPO="/workspace/local-maven-repo"

# 镜像源
TENCENT_PUBLIC="https://mirrors.cloud.tencent.com/nexus/repository/maven-public"
ALIYUN_PUBLIC="https://maven.aliyun.com/repository/public"
ALIYUN_GOOGLE="https://maven.aliyun.com/repository/google"
HUAWEI="https://repo.huaweicloud.com/repository/maven"

# 通用下载
download_artifact() {
    local group_path=$1
    local artifact=$2
    local version=$3
    local ext=$4
    local dir="$LOCAL_REPO/$group_path/$artifact/$version"
    mkdir -p "$dir"
    local file_name="$artifact-$version.$ext"

    if [ -f "$dir/$file_name" ]; then
        local sz=$(stat -c%s "$dir/$file_name" 2>/dev/null || echo 0)
        if [ "$sz" -gt 500 ]; then
            echo "  ✓ 已存在: $file_name"
            return 0
        fi
    fi

    local repos=("$TENCENT_PUBLIC" "$ALIYUN_PUBLIC" "$ALIYUN_GOOGLE" "$HUAWEI")
    for repo in "${repos[@]}"; do
        local url="$repo/$group_path/$artifact/$version/$file_name"
        if curl -L --max-time 30 -s -o "$dir/$file_name" "$url" 2>/dev/null; then
            local sz=$(stat -c%s "$dir/$file_name" 2>/dev/null || echo 0)
            if [ "$sz" -gt 500 ]; then
                echo "  ✓ 下载: $file_name ($sz bytes)"
                return 0
            fi
        fi
    done
    echo "  ✗ 失败: $file_name"
    rm -f "$dir/$file_name"
    return 1
}

echo "===== 三次补全：guava完整链、Gradle内部 ====="

# ===== Checker Framework 多版本 =====
echo "--- Checker Framework ---"
for v in "3.33.0" "3.12.0" "3.5.0"; do
    download_artifact "org/checkerframework" "checker-qual" "$v" "jar"
    download_artifact "org/checkerframework" "checker-qual" "$v" "pom"
done

# ===== Error Prone Annotations 多版本 =====
echo "--- Error Prone ---"
for v in "2.18.0" "2.11.0"; do
    download_artifact "com/google/errorprone" "error_prone_annotations" "$v" "jar"
    download_artifact "com/google/errorprone" "error_prone_annotations" "$v" "pom"
done

# ===== Sonatype OSS Parent =====
echo "--- Sonatype Parents ---"
download_artifact "org/sonatype/oss" "oss-parent" "9" "pom"
download_artifact "org/sonatype" "oss-parent" "9" "pom" 2>/dev/null || true
download_artifact "org/sonatype/oss" "oss-parent" "7" "pom" 2>/dev/null || true

# ===== 已知 missing: dvlib =====
echo "--- Dvlib ---"
download_artifact "com/android/tools" "dvlib" "31.7.3" "jar"
download_artifact "com/android/tools" "dvlib" "31.7.3" "pom"

# ===== Gradle 内部模块 =====
echo "--- Gradle Modules ---"
for v in "8.7.3" "8.7" "8.6" "8.5"; do
    for art in "gradle-core" "gradle-base-services" "gradle-model" "gradle-tooling-api" \
               "gradle-installation-beacon" "gradle-launcher" "gradle-services" \
               "gradle-resources" "gradle-messaging" "gradle-native"; do
        download_artifact "org/gradle" "$art" "$v" "jar" 2>/dev/null || true
        download_artifact "org/gradle" "$art" "$v" "pom" 2>/dev/null || true
    done
done

# ===== Kotlin 完整编译时 =====
echo "--- Kotlin Runtime ---"
for v in "2.1.20" "2.1.10" "2.1.0" "1.9.0"; do
    for art in "kotlin-stdlib" "kotlin-stdlib-jdk7" "kotlin-stdlib-jdk8" \
               "kotlin-reflect" "kotlin-script-runtime" "kotlin-daemon-embeddable" \
               "kotlin-compiler-embeddable" "kotlin-build-tools-impl" \
               "kotlin-gradle-plugin" "kotlin-gradle-plugin-api" "kotlin-gradle-plugin-idea" \
               "kotlin-gradle-plugin-model" "kotlin-gradle-plugin-annotations" \
               "kotlin-gradle-plugin-statistics" "kotlin-tooling-core"; do
        download_artifact "org/jetbrains/kotlin" "$art" "$v" "jar" 2>/dev/null || true
        download_artifact "org/jetbrains/kotlin" "$art" "$v" "pom" 2>/dev/null || true
    done
done

# ===== Kotlin Gradle Plugin =====
echo "--- Kotlin Gradle Plugin POMs ---"
for v in "2.1.20" "2.1.10"; do
    for art in "org.jetbrains.kotlin.android" "org.jetbrains.kotlin.jvm" \
               "org.jetbrains.kotlin.multiplatform" "org.jetbrains.kotlin.plugin.compose" \
               "org.jetbrains.kotlin.plugin.serialization" "org.jetbrains.kotlin.plugin.parcelize"; do
        download_artifact "org/jetbrains/kotlin/android" "$art.gradle.plugin" "$v" "pom" 2>/dev/null || true
        download_artifact "org/jetbrains/kotlin/jvm" "$art.gradle.plugin" "$v" "pom" 2>/dev/null || true
        download_artifact "org/jetbrains/kotlin/plugin/compose" "$art.gradle.plugin" "$v" "pom" 2>/dev/null || true
        download_artifact "org/jetbrains/kotlin/plugin/serialization" "$art.gradle.plugin" "$v" "pom" 2>/dev/null || true
        download_artifact "org/jetbrains/kotlin/plugin/parcelize" "$art.gradle.plugin" "$v" "pom" 2>/dev/null || true
    done
done

# ===== Annotation 库 =====
echo "--- Annotations ---"
download_artifact "javax/annotation" "javax.annotation-api" "1.3.2" "jar" 2>/dev/null || true
download_artifact "javax/annotation" "javax.annotation-api" "1.3.2" "pom" 2>/dev/null || true
download_artifact "com/google/code/findbugs" "jsr305" "3.0.2" "jar"
download_artifact "com/google/code/findbugs" "jsr305" "3.0.2" "pom"

# ===== 清理 =====
echo ""
echo "===== 清理 ====="
find "$LOCAL_REPO" -type f \( -name "*.jar" -o -name "*.aar" -o -name "*.pom" \) -size -1k -delete 2>/dev/null || true
find "$LOCAL_REPO" -type d -empty -delete 2>/dev/null || true

echo ""
echo "=========================================="
echo "✓ 三次补全完成"
echo "总文件数: $(find "$LOCAL_REPO" -type f | wc -l)"
echo "总大小: $(du -sh "$LOCAL_REPO" | cut -f1)"
echo "=========================================="
