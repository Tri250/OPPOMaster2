#!/bin/bash
# 沙箱环境依赖下载脚本（深度优化版 v3）
# 关键改进：通过 HTTP 代理下载（沙箱直连被阻断）
# 验证：使用 127.0.0.1:18080 代理后，Aliyun/Tencent/Google 均可达（10-400ms）

set -e
LOCAL_REPO="/workspace/local-maven-repo"
LOG_FILE="/tmp/deps-download-v3.log"
PROXY="http://127.0.0.1:18080"

# 镜像优先级排序：Aliyun（最快10-20ms）> Tencent（200-400ms）> Google > Maven Central
MIRRORS=(
  "https://maven.aliyun.com/repository/google"
  "https://maven.aliyun.com/repository/public"
  "https://maven.aliyun.com/repository/central"
  "https://maven.aliyun.com/repository/gradle-plugin"
  "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"
  "https://dl.google.com/dl/android/maven2"
  "https://repo1.maven.org/maven2"
  "https://plugins.gradle.org/m2"
)

mkdir -p "$LOCAL_REPO"
echo "$(date): Start downloading via proxy $PROXY" > "$LOG_FILE"

download() {
  local group_path="$1"   # e.g. org/jetbrains/kotlin/kotlin-stdlib
  local file="$2"          # e.g. kotlin-stdlib-2.1.20.pom
  local dest_dir="$LOCAL_REPO/$group_path"
  local dest_file="$dest_dir/$file"

  # 已存在且非 0 字节文件，跳过
  if [[ -f "$dest_file" ]] && [[ -s "$dest_file" ]]; then
    echo "  ✓ $group_path/$file (cached)"
    return 0
  fi

  mkdir -p "$dest_dir"
  local success=0
  for mirror in "${MIRRORS[@]}"; do
    local url="$mirror/$group_path/$file"
    if curl -sS --max-time 30 --proxy "$PROXY" -fL -o "$dest_file.tmp" "$url" 2>>"$LOG_FILE"; then
      # 校验下载文件（不能是 HTML 错误页）
      if file "$dest_file.tmp" 2>/dev/null | grep -qE "HTML|JSON|XML|gzip|Zip" || \
         head -c 4 "$dest_file.tmp" 2>/dev/null | grep -qE "(<htm|<HTM|<xml|<x|<\\$|PK|\\037)"; then
        # 是有效内容（HTML 错误页会被 next mirror 拒绝）
        size=$(stat -c%s "$dest_file.tmp")
        if [[ $size -gt 50 ]]; then
          mv "$dest_file.tmp" "$dest_file"
          echo "  ✓ $group_path/$file ($(($size/1024))KB) via $mirror"
          success=1
          break
        fi
      fi
      rm -f "$dest_file.tmp"
    fi
  done

  if [[ $success -eq 0 ]]; then
    echo "  ✗ FAILED: $group_path/$file"
    rm -f "$dest_file.tmp"
    return 1
  fi
  return 0
}

echo "=========================================="
echo "沙箱依赖下载 (使用代理 $PROXY)"
echo "=========================================="

# ===== 1. AGP 8.7.3 核心依赖 =====
echo ""
echo "[1/8] AGP 8.7.3 核心依赖..."
for f in \
  "com/android/tools/build/gradle/8.7.3/gradle-8.7.3.pom" \
  "com/android/tools/build/gradle/8.7.3/gradle-8.7.3.jar" \
  "com/android/tools/build/gradle/8.7.3/gradle-8.7.3.module" \
  "com/android/tools/build/gradle-api/8.7.3/gradle-api-8.7.3.pom" \
  "com/android/tools/build/gradle-api/8.7.3/gradle-api-8.7.3.jar" \
  ; do
  group=$(dirname "$f")
  file=$(basename "$f")
  download "$group" "$file" || true
done

# ===== 2. Kotlin 2.1.20 核心 =====
echo ""
echo "[2/8] Kotlin 2.1.20 核心..."
for f in \
  "org/jetbrains/kotlin/kotlin-stdlib/2.1.20/kotlin-stdlib-2.1.20.pom" \
  "org/jetbrains/kotlin/kotlin-stdlib/2.1.20/kotlin-stdlib-2.1.20.jar" \
  "org/jetbrains/kotlin/kotlin-stdlib/2.1.20/kotlin-stdlib-2.1.20.module" \
  "org/jetbrains/kotlin/kotlin-gradle-plugin/2.1.20/kotlin-gradle-plugin-2.1.20.pom" \
  "org/jetbrains/kotlin/kotlin-gradle-plugin/2.1.20/kotlin-gradle-plugin-2.1.20.jar" \
  "org/jetbrains/kotlin/kotlin-gradle-plugin/2.1.20/kotlin-gradle-plugin-2.1.20.module" \
  "org/jetbrains/kotlin/kotlin-compiler-embeddable/2.1.20/kotlin-compiler-embeddable-2.1.20.pom" \
  "org/jetbrains/kotlin/kotlin-compiler-embeddable/2.1.20/kotlin-compiler-embeddable-2.1.20.jar" \
  "org/jetbrains/kotlin/kotlin-build-tools-impl/2.1.20/kotlin-build-tools-impl-2.1.20.pom" \
  "org/jetbrains/kotlin/kotlin-build-tools-impl/2.1.20/kotlin-build-tools-impl-2.1.20.jar" \
  ; do
  group=$(dirname "$f")
  file=$(basename "$f")
  download "$group" "$file" || true
done

# ===== 3. AndroidX 核心 =====
echo ""
echo "[3/8] AndroidX 核心..."
for f in \
  "androidx/core/core-ktx/1.13.1/core-ktx-1.13.1.pom" \
  "androidx/core/core-ktx/1.13.1/core-ktx-1.13.1.aar" \
  "androidx/core/core-ktx/1.13.1/core-ktx-1.13.1.module" \
  "androidx/appcompat/appcompat/1.7.0/appcompat-1.7.0.pom" \
  "androidx/appcompat/appcompat/1.7.0/appcompat-1.7.0.aar" \
  "androidx/activity/activity-compose/1.9.3/activity-compose-1.9.3.pom" \
  "androidx/activity/activity-compose/1.9.3/activity-compose-1.9.3.aar" \
  "androidx/lifecycle/lifecycle-runtime-ktx/2.8.7/lifecycle-runtime-ktx-2.8.7.pom" \
  "androidx/lifecycle/lifecycle-runtime-ktx/2.8.7/lifecycle-runtime-ktx-2.8.7.aar" \
  "androidx/lifecycle/lifecycle-viewmodel-compose/2.8.7/lifecycle-viewmodel-compose-2.8.7.pom" \
  "androidx/lifecycle/lifecycle-viewmodel-compose/2.8.7/lifecycle-viewmodel-compose-2.8.7.aar" \
  "androidx/compose/compose-bom/2025.01.01/compose-bom-2025.01.01.pom" \
  "androidx/compose/compose-bom/2025.01.01/compose-bom-2025.01.01.module" \
  "androidx/compose/ui/ui/1.7.6/ui-1.7.6.pom" \
  "androidx/compose/ui/ui/1.7.6/ui-1.7.6.aar" \
  "androidx/compose/ui/ui-tooling-preview/1.7.6/ui-tooling-preview-1.7.6.pom" \
  "androidx/compose/ui/ui-tooling-preview/1.7.6/ui-tooling-preview-1.7.6.aar" \
  "androidx/compose/material3/material3/1.3.1/material3-1.3.1.pom" \
  "androidx/compose/material3/material3/1.3.1/material3-1.3.1.aar" \
  "androidx/navigation/navigation-compose/2.8.5/navigation-compose-2.8.5.pom" \
  "androidx/navigation/navigation-compose/2.8.5/navigation-compose-2.8.5.aar" \
  ; do
  group=$(dirname "$f")
  file=$(basename "$f")
  download "$group" "$file" || true
done

# ===== 4. Kotlinx =====
echo ""
echo "[4/8] Kotlinx 核心..."
for f in \
  "org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.1/kotlinx-coroutines-core-1.8.1.pom" \
  "org/jetbrains/kotlinx/kotlinx-coroutines-core/1.8.1/kotlinx-coroutines-core-1.8.1.jar" \
  "org/jetbrains/kotlinx/kotlinx-coroutines-android/1.8.1/kotlinx-coroutines-android-1.8.1.pom" \
  "org/jetbrains/kotlinx/kotlinx-coroutines-android/1.8.1/kotlinx-coroutines-android-1.8.1.jar" \
  "org/jetbrains/kotlinx/kotlinx-serialization-core/1.7.3/kotlinx-serialization-core-1.7.3.pom" \
  "org/jetbrains/kotlinx/kotlinx-serialization-core/1.7.3/kotlinx-serialization-core-1.7.3.jar" \
  "org/jetbrains/kotlinx/kotlinx-serialization-json/1.7.3/kotlinx-serialization-json-1.7.3.pom" \
  "org/jetbrains/kotlinx/kotlinx-serialization-json/1.7.3/kotlinx-serialization-json-1.7.3.jar" \
  ; do
  group=$(dirname "$f")
  file=$(basename "$f")
  download "$group" "$file" || true
done

# ===== 5. ML Kit + TF Lite =====
echo ""
echo "[5/8] ML Kit + TF Lite..."
for f in \
  "com/google/mlkit/face-detection/16.1.7/face-detection-16.1.7.pom" \
  "com/google/mlkit/face-detection/16.1.7/face-detection-16.1.7.aar" \
  "com/google/mlkit/face-detection/16.1.7/face-detection-16.1.7.module" \
  "com/google/mlkit/common/common-ktx/17.0.0/common-ktx-17.0.0.pom" \
  "org/tensorflow/tensorflow-lite/2.16.1/tensorflow-lite-2.16.1.pom" \
  "org/tensorflow/tensorflow-lite/2.16.1/tensorflow-lite-2.16.1.aar" \
  "org/tensorflow/tensorflow-lite-api/2.16.1/tensorflow-lite-api-2.16.1.pom" \
  "org/tensorflow/tensorflow-lite-support/0.4.4/tensorflow-lite-support-0.4.4.pom" \
  "org/tensorflow/tensorflow-lite-support/0.4.4/tensorflow-lite-support-0.4.4.aar" \
  ; do
  group=$(dirname "$f")
  file=$(basename "$f")
  download "$group" "$file" || true
done

# ===== 6. Coil + Networking =====
echo ""
echo "[6/8] Coil + Networking..."
for f in \
  "io/coil-kt/coil-compose/2.7.0/coil-compose-2.7.0.pom" \
  "io/coil-kt/coil-compose/2.7.0/coil-compose-2.7.0.aar" \
  "io/coil-kt/coil/2.7.0/coil-2.7.0.pom" \
  "io/coil-kt/coil-base/2.7.0/coil-base-2.7.0.pom" \
  "io/coil-kt/coil-base/2.7.0/coil-base-2.7.0.aar" \
  "io/ktor/ktor-client-core/3.0.3/ktor-client-core-3.0.3.pom" \
  "io/ktor/ktor-client-core/3.0.3/ktor-client-core-3.0.3.jar" \
  "io/ktor/ktor-client-okhttp/3.0.3/ktor-client-okhttp-3.0.3.pom" \
  "io/ktor/ktor-client-okhttp/3.0.3/ktor-client-okhttp-3.0.3.jar" \
  "io/ktor/ktor-client-content-negotiation/3.0.3/ktor-client-content-negotiation-3.0.3.pom" \
  "io/ktor/ktor-client-content-negotiation/3.0.3/ktor-client-content-negotiation-3.0.3.jar" \
  "io/ktor/ktor-serialization-kotlinx-json/3.0.3/ktor-serialization-kotlinx-json-3.0.3.pom" \
  "io/ktor/ktor-serialization-kotlinx-json/3.0.3/ktor-serialization-kotlinx-json-3.0.3.jar" \
  "com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.pom" \
  "com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar" \
  "com/squareup/okio/okio/3.9.0/okio-3.9.0.pom" \
  "com/squareup/okio/okio/3.9.0/okio-3.9.0.jar" \
  "com/squareup/okio/okio-jvm/3.9.0/okio-jvm-3.9.0.jar" \
  ; do
  group=$(dirname "$f")
  file=$(basename "$f")
  download "$group" "$file" || true
done

# ===== 7. Umeng（已移除） =====
# 友盟 SDK 已从项目中移除，跳过下载

# ===== 8. Gradle Plugin 核心 =====
echo ""
echo "[8/8] Gradle Plugin 核心..."
for f in \
  "com/android/application/com.android.application.gradle.plugin/8.7.3/com.android.application.gradle.plugin-8.7.3.pom" \
  "com/android/library/com.android.library.gradle.plugin/8.7.3/com.android.library.gradle.plugin-8.7.3.pom" \
  "org/jetbrains/kotlin/android/org.jetbrains.kotlin.android.gradle.plugin/2.1.20/org.jetbrains.kotlin.android.gradle.plugin-2.1.20.pom" \
  "org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/2.1.20/org.jetbrains.kotlin.plugin.compose.gradle.plugin-2.1.20.pom" \
  "org/jetbrains/kotlin/plugin/serialization/org.jetbrains.kotlin.plugin.serialization.gradle.plugin/2.1.20/org.jetbrains.kotlin.plugin.serialization.gradle.plugin-2.1.20.pom" \
  "org/jetbrains/kotlin/plugin/parcelize/org.jetbrains.kotlin.plugin.parcelize.gradle.plugin/2.1.20/org.jetbrains.kotlin.plugin.parcelize.gradle.plugin-2.1.20.pom" \
  ; do
  group=$(dirname "$f")
  file=$(basename "$f")
  download "$group" "$file" || true
done

echo ""
echo "=========================================="
echo "下载完成！统计："
echo "  POM: $(find "$LOCAL_REPO" -name "*.pom" 2>/dev/null | wc -l)"
echo "  JAR: $(find "$LOCAL_REPO" -name "*.jar" 2>/dev/null | wc -l)"
echo "  AAR: $(find "$LOCAL_REPO" -name "*.aar" 2>/dev/null | wc -l)"
echo "  MODULE: $(find "$LOCAL_REPO" -name "*.module" 2>/dev/null | wc -l)"
echo "  总计: $(find "$LOCAL_REPO" -type f 2>/dev/null | wc -l)"
echo "=========================================="
echo "日志: $LOG_FILE"
