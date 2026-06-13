#!/bin/bash
# AGP 8.7.3 二次依赖补全 - 处理 parent POM 和传递依赖
# 解决 commons-parent, httpcomponents-parent, jna 等父级 POM 缺失

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
            echo "  ✓ 已存在: $file_name ($sz bytes)"
            return 0
        fi
    fi

    local repos=("$TENCENT_PUBLIC" "$ALIYUN_PUBLIC" "$ALIYUN_GOOGLE" "$HUAWEI")
    for repo in "${repos[@]}"; do
        local url="$repo/$group_path/$artifact/$version/$file_name"
        if curl -L --max-time 30 -s -o "$dir/$file_name" "$url" 2>/dev/null; then
            local sz=$(stat -c%s "$dir/$file_name" 2>/dev/null || echo 0)
            if [ "$sz" -gt 500 ]; then
                echo "  ✓ 下载: $file_name from $repo ($sz bytes)"
                return 0
            fi
        fi
    done
    echo "  ✗ 失败: $file_name"
    rm -f "$dir/$file_name"
    return 1
}

echo "===== 二次补全：parent POM 和传递依赖 ====="

# ===== Apache Commons Parent POMs =====
echo "--- Apache Commons Parents ---"
for ver in "52" "53" "54"; do
    download_artifact "org/apache/commons" "commons-parent" "$ver" "pom"
done

# ===== Apache HttpComponents Parent POMs =====
echo "--- HttpComponents Parents ---"
download_artifact "org/apache/httpcomponents" "httpcomponents-client" "4.5.14" "pom"
download_artifact "org/apache/httpcomponents" "httpcomponents-core" "4.4.16" "pom"
download_artifact "org/apache/httpcomponents" "httpcomponents-parent" "11" "pom" 2>/dev/null || true
download_artifact "org/apache/httpcomponents" "httpcomponents-parent" "12" "pom" 2>/dev/null || true

# ===== JNA 完整包 =====
echo "--- JNA ---"
download_artifact "net/java/dev/jna" "jna" "5.6.0" "jar"
download_artifact "net/java/dev/jna" "jna" "5.6.0" "pom"

# ===== FindBugs JSR305 =====
echo "--- JSR305 ---"
download_artifact "com/google/code/findbugs" "jsr305" "3.0.2" "jar"
download_artifact "com/google/code/findbugs" "jsr305" "3.0.2" "pom"

# ===== Guava 父 POM =====
echo "--- Guava Parent ---"
download_artifact "com/google/guava" "guava-parent" "32.0.1-jre" "pom" 2>/dev/null || true
download_artifact "com/google/guava" "guava-parent" "26.0-android" "pom" 2>/dev/null || true

# ===== Gradle Internal Parent POMs (for AGP) =====
echo "--- Gradle API Internal ---"
for v in "8.7.3" "8.7"; do
    for art in "gradle-core" "gradle-base-services"; do
        download_artifact "org/gradle" "$art" "$v" "pom" 2>/dev/null || true
    done
done

# ===== 工具类依赖 =====
echo "--- 工具类 ---"
# error_prone_annotations
download_artifact "com/google/errorprone" "error_prone_annotations" "2.36.0" "jar" 2>/dev/null || true
download_artifact "com/google/errorprone" "error_prone_annotations" "2.36.0" "pom" 2>/dev/null || true
download_artifact "com/google/errorprone" "error_prone_annotations" "2.10.0" "jar" 2>/dev/null || true
download_artifact "com/google/errorprone" "error_prone_annotations" "2.10.0" "pom" 2>/dev/null || true

# Animal Sniffer
download_artifact "org/codehaus/mojo" "animal-sniffer-annotations" "1.23" "jar" 2>/dev/null || true
download_artifact "org/codehaus/mojo" "animal-sniffer-annotations" "1.23" "pom" 2>/dev/null || true

# J2ObjC
download_artifact "com/google/j2objc" "j2objc-annotations" "2.8" "jar" 2>/dev/null || true
download_artifact "com/google/j2objc" "j2objc-annotations" "2.8" "pom" 2>/dev/null || true

# Checker Framework
download_artifact "org/checkerframework" "checker-qual" "3.37.0" "jar" 2>/dev/null || true
download_artifact "org/checkerframework" "checker-qual" "3.37.0" "pom" 2>/dev/null || true

# ===== 清理 =====
echo ""
echo "===== 清理 ====="
find "$LOCAL_REPO" -type f \( -name "*.jar" -o -name "*.aar" -o -name "*.pom" \) -size -1k -delete 2>/dev/null || true
find "$LOCAL_REPO" -type d -empty -delete 2>/dev/null || true

echo ""
echo "=========================================="
echo "✓ 二次补全完成"
echo "总文件数: $(find "$LOCAL_REPO" -type f | wc -l)"
echo "总大小: $(du -sh "$LOCAL_REPO" | cut -f1)"
echo "=========================================="
