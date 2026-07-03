#!/bin/bash

# =============================================================================
# Android Release 签名密钥生成脚本
# =============================================================================
# 此脚本用于生成 Android Release 签名密钥库 (release.keystore)
# 生成的密钥库文件将保存在 app/ 目录下
#
# 使用方法:
#   chmod +x scripts/generate-keystore.sh
#   ./scripts/generate-keystore.sh
#
# 注意: 请妥善保管密钥库文件和密码，丢失后无法恢复！
# =============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
APP_DIR="$PROJECT_ROOT/app"

# 密钥库配置
KEYSTORE_FILE="$APP_DIR/release.keystore"
KEYSTORE_ALIAS="omaster"
VALIDITY_DAYS=10000
KEY_ALG="RSA"
KEY_SIZE=2048

# 证书信息
CERT_CN="OPPOMaster2"
CERT_OU="Development"
CERT_O="Silas"
CERT_L="Beijing"
CERT_ST="Beijing"
CERT_C="CN"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}Android Release 签名密钥生成脚本${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# 检查 keytool 是否可用
if ! command -v keytool &> /dev/null; then
    echo -e "${RED}错误: 未找到 keytool 命令${NC}"
    echo -e "${YELLOW}请确保已安装 Java JDK 并配置了环境变量${NC}"
    exit 1
fi

# 检查是否已存在密钥库
if [ -f "$KEYSTORE_FILE" ]; then
    echo -e "${YELLOW}警告: 密钥库文件已存在: $KEYSTORE_FILE${NC}"
    echo -e "${YELLOW}继续将覆盖现有密钥库${NC}"
    read -p "是否继续? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "已取消操作"
        exit 0
    fi
    rm -f "$KEYSTORE_FILE"
fi

echo ""
echo -e "${GREEN}密钥库配置信息:${NC}"
echo "  文件路径: $KEYSTORE_FILE"
echo "  密钥别名: $KEYSTORE_ALIAS"
echo "  有效期: $VALIDITY_DAYS 天"
echo "  密钥算法: $KEY_ALG $KEY_SIZE 位"
echo ""
echo -e "${GREEN}证书信息:${NC}"
echo "  CN (Common Name): $CERT_CN"
echo "  OU (Organization Unit): $CERT_OU"
echo "  O (Organization): $CERT_O"
echo "  L (Locality): $CERT_L"
echo "  ST (State): $CERT_ST"
echo "  C (Country): $CERT_C"
echo ""

# 提示输入密码
echo -e "${YELLOW}请输入密钥库密码 (storePassword):${NC}"
read -s STORE_PASSWORD
echo ""

echo -e "${YELLOW}请确认密钥库密码:${NC}"
read -s STORE_PASSWORD_CONFIRM
echo ""

if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
    echo -e "${RED}错误: 两次输入的密码不一致${NC}"
    exit 1
fi

echo -e "${YELLOW}请输入密钥密码 (keyPassword):${NC}"
read -s KEY_PASSWORD
echo ""

echo -e "${YELLOW}请确认密钥密码:${NC}"
read -s KEY_PASSWORD_CONFIRM
echo ""

if [ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]; then
    echo -e "${RED}错误: 两次输入的密码不一致${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}正在生成密钥库...${NC}"

# 构建 DNAME
DNAME="CN=$CERT_CN, OU=$CERT_OU, O=$CERT_O, L=$CERT_L, ST=$CERT_ST, C=$CERT_C"

# 使用 keytool 生成密钥库
keytool -genkeypair \
    -alias "$KEYSTORE_ALIAS" \
    -keyalg "$KEY_ALG" \
    -keysize "$KEY_SIZE" \
    -validity "$VALIDITY_DAYS" \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "$DNAME"

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}密钥库生成成功!${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo -e "${GREEN}文件位置: $KEYSTORE_FILE${NC}"
    echo ""
    echo -e "${YELLOW}下一步操作:${NC}"
    echo "1. 在 app/ 目录下创建 keystore-release.properties 文件"
    echo "2. 填入以下配置:"
    echo ""
    echo "   storeFile=release.keystore"
    echo "   storePassword=<你设置的密钥库密码>"
    echo "   keyAlias=$KEYSTORE_ALIAS"
    echo "   keyPassword=<你设置的密钥密码>"
    echo ""
    echo -e "${RED}重要提醒:${NC}"
    echo "- 请妥善保管密钥库文件和密码"
    echo "- 请勿将 keystore-release.properties 提交到版本控制"
    echo "- 丢失密钥库将无法更新应用"
    echo ""
else
    echo -e "${RED}密钥库生成失败${NC}"
    exit 1
fi