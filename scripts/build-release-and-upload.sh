#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# OMaster Android Release 构建 + GitHub Release 上传脚本
# 用法：
#   ./scripts/build-release-and-upload.sh [version]
# 默认版本：读取 app/build.gradle.kts 中的 versionName
# 环境变量：GITHUB_TOKEN（如未设置，会尝试从 git remote URL 提取）
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_DIR="$PROJECT_DIR/app"

# 获取版本号（优先参数，其次从 build.gradle.kts 解析）
VERSION="${1:-}"
if [[ -z "$VERSION" ]]; then
    VERSION=$(grep 'versionName = ' "$APP_DIR/build.gradle.kts" | head -1 | sed 's/.*"\(.*\)".*/\1/')
fi

VERSION_CODE=$(grep 'versionCode = ' "$APP_DIR/build.gradle.kts" | head -1 | grep -oE '[0-9]+')
TAG="v$VERSION"
REPO="Tri250/OPPOMaster2"

APK_DIR="$APP_DIR/build/outputs/apk/release"

log() {
    echo "============================================"
    echo "$1"
    echo "============================================"
}

# 获取 GitHub Token：优先环境变量，其次从 remote URL 提取
get_github_token() {
    if [[ -n "${GITHUB_TOKEN:-}" ]]; then
        echo "$GITHUB_TOKEN"
        return 0
    fi

    local remote_url
    remote_url=$(git -C "$PROJECT_DIR" remote get-url origin 2>/dev/null || true)
    if [[ "$remote_url" =~ x-access-token:([^@]+)@github.com ]]; then
        echo "${BASH_REMATCH[1]}"
        return 0
    fi

    echo ""
}

# 校验 GitHub Token
GH_TOKEN=$(get_github_token)
if [[ -z "$GH_TOKEN" ]]; then
    echo "错误：无法获取 GITHUB_TOKEN"
    echo "请设置环境变量 GITHUB_TOKEN，或使用 https://token@github.com/... 格式的 remote"
    exit 1
fi

log "OMaster Release 构建与发布"
echo "版本: $VERSION (versionCode: $VERSION_CODE)"
echo "Tag: $TAG"
echo "仓库: $REPO"

# 1. 校验工作区干净
cd "$PROJECT_DIR"
if [[ -n "$(git status --short)" ]]; then
    echo "错误：工作区存在未提交修改，请先提交或暂存"
    git status --short
    exit 1
fi

# 2. 校验签名配置
if [[ ! -f "$APP_DIR/release.keystore" ]]; then
    log "生成 Release 签名密钥..."
    "$SCRIPT_DIR/generate-keystore-ci.sh"
fi

if [[ ! -f "$APP_DIR/keystore-release.properties" ]]; then
    echo "错误：未找到 $APP_DIR/keystore-release.properties"
    exit 1
fi

# 3. 构建 Release APK
log "Step 1/5: 清理并构建 Release APK"
./gradlew clean assembleRelease --no-daemon

# 4. 校验 APK 输出
log "Step 2/5: 校验 APK 输出"
if [[ ! -d "$APK_DIR" ]]; then
    echo "错误：未找到 APK 输出目录 $APK_DIR"
    exit 1
fi

APK_FILES=("$APK_DIR"/*.apk)
if [[ ! -f "${APK_FILES[0]}" ]]; then
    echo "错误：未找到生成的 APK 文件"
    exit 1
fi

echo "生成的 APK 文件："
ls -lh "$APK_DIR"/*.apk

# 5. 备份 mapping 文件
MAPPING_FILE="$APP_DIR/build/outputs/mapping/release/mapping.txt"
if [[ -f "$MAPPING_FILE" ]]; then
    mkdir -p "$APP_DIR/mapping"
    cp "$MAPPING_FILE" "$APP_DIR/mapping/mapping-$VERSION-$VERSION_CODE.txt"
    echo "已备份 mapping 文件"
fi

# 6. 创建并推送 Git Tag
log "Step 3/5: 创建并推送 Git Tag"
if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "Tag $TAG 已存在，跳过创建"
else
    git tag -a "$TAG" -m "Release $TAG"
    echo "已创建 Tag: $TAG"
fi

git push origin "$TAG"
echo "已推送 Tag: $TAG"

# 7. 创建 GitHub Release
log "Step 4/5: 创建 GitHub Release"
RELEASE_JSON=$(cat <<EOF
{
  "tag_name": "$TAG",
  "target_commitish": "$(git rev-parse --abbrev-ref HEAD)",
  "name": "OMaster $TAG",
  "body": "## OMaster $TAG\n\n- 版本号: $VERSION\n- versionCode: $VERSION_CODE\n- 构建类型: Release\n- ABI: armeabi-v7a, arm64-v8a, x86, x86_64（含 universal APK）\n\n### APK 说明\n\n- \`app-arm64-v8a-release.apk\`：ARM 64 位设备（推荐）\n- \`app-armeabi-v7a-release.apk\`：ARM 32 位设备\n- \`app-x86_64-release.apk\` / \`app-x86-release.apk\`：模拟器/Chrome OS\n- \`app-universal-release.apk\`：通用包（包含所有 ABI）\n",
  "draft": false,
  "prerelease": false
}
EOF
)

RESPONSE=$(curl -s -X POST \
    -H "Authorization: token $GH_TOKEN" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/json" \
    "https://api.github.com/repos/$REPO/releases" \
    -d "$RELEASE_JSON")

RELEASE_ID=$(echo "$RESPONSE" | grep -o '"id": [0-9]*' | head -1 | awk '{print $2}')
UPLOAD_URL=$(echo "$RESPONSE" | grep -o '"upload_url": "[^"]*' | sed 's/"upload_url": "//')

if [[ -z "$RELEASE_ID" || "$RELEASE_ID" == "null" ]]; then
    echo "错误：GitHub Release 创建失败"
    echo "$RESPONSE"
    exit 1
fi

echo "GitHub Release 创建成功: ID=$RELEASE_ID"
echo "上传 URL: $UPLOAD_URL"

# 8. 上传 APK 文件
log "Step 5/5: 上传 APK 到 GitHub Release"
for apk in "$APK_DIR"/*.apk; do
    filename=$(basename "$apk")
    echo "上传: $filename"
    curl -s -X POST \
        -H "Authorization: token $GH_TOKEN" \
        -H "Accept: application/vnd.github+json" \
        -H "Content-Type: application/vnd.android.package-archive" \
        --data-binary "@$apk" \
        "${UPLOAD_URL%\{*}?name=$filename" >/dev/null
    echo "  ✓ 完成"
done

log "发布完成"
echo "版本: $TAG"
echo "Release 页面: https://github.com/$REPO/releases/tag/$TAG"
echo ""
echo "注意："
echo "- 请妥善保管 app/release.keystore 和 app/keystore-release.properties"
echo "- 如需上传 mapping 文件，请手动上传："
echo "  $APP_DIR/mapping/mapping-$VERSION-$VERSION_CODE.txt"
