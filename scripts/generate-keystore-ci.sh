#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# CI/非交互式 Release 签名密钥生成脚本
# 用法：
#   STORE_PASSWORD=xxx KEY_PASSWORD=xxx ./scripts/generate-keystore-ci.sh
# 默认：自动生成强密码并写入 app/keystore-release.properties
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$SCRIPT_DIR/../app"
KEYSTORE_FILE="$APP_DIR/release.keystore"
KEYSTORE_ALIAS="omaster"
VALIDITY_DAYS=10000
KEY_ALG="RSA"
KEY_SIZE=2048

DNAME="CN=OPPOMaster2, OU=Development, O=Silas, L=Beijing, ST=Beijing, C=CN"

# 如果没有提供密码，则生成随机密码
if [[ -z "${STORE_PASSWORD:-}" ]]; then
    STORE_PASSWORD=$(openssl rand -base64 24 | tr -dc 'a-zA-Z0-9' | head -c 24)
    echo "未提供 STORE_PASSWORD，已自动生成"
fi

if [[ -z "${KEY_PASSWORD:-}" ]]; then
    KEY_PASSWORD="$STORE_PASSWORD"
fi

echo "============================================"
echo "Release 签名密钥生成 (CI 模式)"
echo "============================================"

# 检查 keytool
if ! command -v keytool >/dev/null 2>&1; then
    echo "错误：未找到 keytool，请安装 JDK 17"
    exit 1
fi

# 备份旧密钥
if [[ -f "$KEYSTORE_FILE" ]]; then
    mv "$KEYSTORE_FILE" "$KEYSTORE_FILE.bak.$(date +%s)"
    echo "已备份旧密钥库"
fi

# 生成密钥库
keytool -genkeypair \
    -alias "$KEYSTORE_ALIAS" \
    -keyalg "$KEY_ALG" \
    -keysize "$KEY_SIZE" \
    -validity "$VALIDITY_DAYS" \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "$DNAME"

# 生成签名配置文件
cat > "$APP_DIR/keystore-release.properties" << EOF
storeFile=release.keystore
storePassword=$STORE_PASSWORD
keyAlias=$KEYSTORE_ALIAS
keyPassword=$KEY_PASSWORD
EOF

echo "============================================"
echo "Release 签名密钥生成完成"
echo "密钥库: $KEYSTORE_FILE"
echo "配置文件: $APP_DIR/keystore-release.properties"
echo "============================================"
echo ""
echo "⚠️  重要提醒："
echo "- keystore-release.properties 与 *.keystore 已加入 .gitignore，不会提交"
echo "- 请妥善保存上述密码，丢失后无法更新应用"
echo "- 生产环境建议将密钥库存放到安全位置（如 CI secrets）"
