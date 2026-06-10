#!/bin/bash

# =============================================================================
# Gradle 全局镜像配置安装脚本
# =============================================================================
# 将项目内的 init.d 镜像配置复制到用户的 Gradle 全局配置目录
# 这样所有 Gradle 项目都会自动使用国内镜像，无需在每个项目中配置
# =============================================================================

set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 用户 Gradle 配置目录
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INIT_D_DIR="$GRADLE_USER_HOME/init.d"

# 项目内的 init 脚本
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_INIT_SCRIPT="$SCRIPT_DIR/../gradle/init.d/mirror.init.gradle.kts"

if [ ! -f "$PROJECT_INIT_SCRIPT" ]; then
    echo -e "${RED}✗ 找不到项目内的镜像脚本: $PROJECT_INIT_SCRIPT${NC}"
    exit 1
fi

echo -e "${YELLOW}→ 准备安装 Gradle 全局镜像配置${NC}"
echo "  目标目录: $INIT_D_DIR"

# 创建 init.d 目录
mkdir -p "$INIT_D_DIR"

# 备份已有配置
if [ -f "$INIT_D_DIR/mirror.init.gradle.kts" ]; then
    BACKUP_NAME="mirror.init.gradle.kts.backup.$(date +%Y%m%d_%H%M%S)"
    mv "$INIT_D_DIR/mirror.init.gradle.kts" "$INIT_D_DIR/$BACKUP_NAME"
    echo -e "${YELLOW}⚠ 已备份旧配置到: $BACKUP_NAME${NC}"
fi

# 复制项目内脚本
cp "$PROJECT_INIT_SCRIPT" "$INIT_D_DIR/mirror.init.gradle.kts"

echo -e "${GREEN}✓ 镜像配置已安装到: $INIT_D_DIR/mirror.init.gradle.kts${NC}"
echo ""
echo -e "${YELLOW}提示：${NC}"
echo "  1. 重新执行 Gradle 命令即可生效"
echo "  2. 如需临时禁用，可使用 --init-script /dev/null"
echo "  3. 如需彻底移除，可删除 $INIT_D_DIR/mirror.init.gradle.kts"
echo ""
echo -e "${YELLOW}使用示例：${NC}"
echo "  ./gradlew clean build"
echo "  ./gradlew :app:assembleRelease"
