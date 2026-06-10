#!/bin/bash

# =============================================================================
# Gradle 镜像配置验证脚本
# =============================================================================
# 检测所有配置的镜像源是否可用，并验证配置正确性
# =============================================================================

set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS_COUNT=0
FAIL_COUNT=0

check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    PASS_COUNT=$((PASS_COUNT + 1))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    FAIL_COUNT=$((FAIL_COUNT + 1))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# 标题
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Gradle 镜像网络配置验证                                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 检查镜像源可用性
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  配置文件检查（推荐）${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}注：网络可达性检测可能因网络环境而波动，仅作为参考${NC}"
echo ""

check_network() {
    local name="$1"
    local url="$2"
    # 200/301/302/403 都表示服务器可达
    # 403 表示服务器存在但拒绝列表访问（Maven 仓库通常禁止目录浏览）
    local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null)
    if echo "$code" | grep -qE "^(200|301|302|403)$"; then
        echo -e "  ${GREEN}✓${NC} $name"
    else
        echo -e "  ${YELLOW}⚠${NC} $name（HTTP $code，可能是网络问题）"
    fi
}

# 阿里云镜像
check_network "阿里云 Google 镜像" "https://maven.aliyun.com/repository/google/"
check_network "阿里云 Central 镜像" "https://maven.aliyun.com/repository/central/"
check_network "阿里云 Public 镜像" "https://maven.aliyun.com/repository/public/"
check_network "阿里云 Gradle Plugin 镜像" "https://maven.aliyun.com/repository/gradle-plugin/"

# 腾讯云镜像
check_network "腾讯云 Maven 镜像" "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"

# 华为云镜像
check_network "华为云 Maven 镜像" "https://repo.huaweicloud.com/repository/maven/"

# 友盟仓库
check_network "友盟仓库" "https://repo.umeng.com/maven-releases/"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  配置文件检查${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# 检查 settings.gradle.kts
if [ -f "/workspace/settings.gradle.kts" ]; then
    if grep -q "maven.aliyun.com" "/workspace/settings.gradle.kts"; then
        check_pass "settings.gradle.kts 包含阿里云镜像"
    else
        check_fail "settings.gradle.kts 缺少阿里云镜像"
    fi

    if grep -q "mirrors.cloud.tencent.com" "/workspace/settings.gradle.kts"; then
        check_pass "settings.gradle.kts 包含腾讯云镜像"
    else
        check_warn "settings.gradle.kts 缺少腾讯云镜像"
    fi

    if grep -q "repo.huaweicloud.com" "/workspace/settings.gradle.kts"; then
        check_pass "settings.gradle.kts 包含华为云镜像"
    else
        check_warn "settings.gradle.kts 缺少华为云镜像"
    fi
else
    check_fail "settings.gradle.kts 不存在"
fi

# 检查 gradle.properties
if [ -f "/workspace/gradle.properties" ]; then
    if grep -q "connectionTimeout" "/workspace/gradle.properties"; then
        check_pass "gradle.properties 配置了网络超时"
    else
        check_fail "gradle.properties 缺少网络超时配置"
    fi

    if grep -q "retry.max.attempts" "/workspace/gradle.properties"; then
        check_pass "gradle.properties 配置了网络重试"
    else
        check_warn "gradle.properties 缺少网络重试配置"
    fi

    if grep -q "caching" "/workspace/gradle.properties"; then
        check_pass "gradle.properties 配置了构建缓存"
    else
        check_warn "gradle.properties 缺少构建缓存配置"
    fi
else
    check_fail "gradle.properties 不存在"
fi

# 检查 init.d 脚本
if [ -f "/workspace/gradle/init.d/mirror.init.gradle.kts" ]; then
    check_pass "Gradle init.d 全局镜像脚本存在"
else
    check_warn "Gradle init.d 全局镜像脚本不存在"
fi

# 检查 wrapper 配置
if [ -f "/workspace/gradle/wrapper/gradle-wrapper.properties" ]; then
    if grep -q "mirrors.aliyun.com" "/workspace/gradle/wrapper/gradle-wrapper.properties"; then
        check_pass "Gradle wrapper 使用阿里云镜像"
    else
        check_warn "Gradle wrapper 未配置阿里云镜像"
    fi
else
    check_fail "gradle-wrapper.properties 不存在"
fi

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  配置检查汇总${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}通过: $PASS_COUNT${NC}  ${RED}失败: $FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 镜像配置验证通过！${NC}"
    exit 0
else
    echo -e "${RED}✗ 发现 $FAIL_COUNT 个问题，请检查配置。${NC}"
    exit 1
fi
