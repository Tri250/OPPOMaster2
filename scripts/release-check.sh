#!/bin/bash

# =============================================================================
# Android Release 构建检查脚本
# =============================================================================
# 此脚本用于在构建 Release APK 前进行全面的自检
# 行业最严标准检查清单
# =============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
APP_DIR="$PROJECT_ROOT/app"

# 计数器
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

# 检查函数
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
    WARN_COUNT=$((WARN_COUNT + 1))
}

section() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# 标题
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Android Release APK 发布自检                              ║${NC}"
echo -e "${BLUE}║   行业最严标准检查清单                                       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"

# 1. 检查签名配置
section "1. 签名配置检查"

if [ -f "$APP_DIR/keystore-release.properties" ]; then
    check_pass "keystore-release.properties 存在"
    
    # 检查密码是否已配置
    if grep -q "YOUR_STORE_PASSWORD" "$APP_DIR/keystore-release.properties"; then
        check_fail "keystore-release.properties 中密码未配置（仍为占位符）"
    else
        check_pass "keystore-release.properties 密码已配置"
    fi
else
    check_warn "keystore-release.properties 不存在，将使用 debug 签名（仅用于开发测试）"
fi

# 2. 检查 build.gradle.kts
section "2. build.gradle.kts 配置检查"

if [ -f "$APP_DIR/build.gradle.kts" ]; then
    check_pass "build.gradle.kts 文件存在"
    
    # 检查 release 块配置
    if grep -q "isMinifyEnabled = true" "$APP_DIR/build.gradle.kts"; then
        check_pass "isMinifyEnabled 已启用（R8/ProGuard 混淆）"
    else
        check_fail "isMinifyEnabled 未启用"
    fi
    
    if grep -q "isShrinkResources = true" "$APP_DIR/build.gradle.kts"; then
        check_pass "isShrinkResources 已启用（资源压缩）"
    else
        check_fail "isShrinkResources 未启用"
    fi
    
    if grep -q "proguardFiles" "$APP_DIR/build.gradle.kts"; then
        check_pass "proguardFiles 已配置"
    else
        check_fail "proguardFiles 未配置"
    fi
    
    if grep -q "splits {" "$APP_DIR/build.gradle.kts"; then
        check_pass "ABI splits 已配置"
    else
        check_warn "ABI splits 未配置（将生成单一 APK，体积较大）"
    fi
else
    check_fail "build.gradle.kts 不存在"
fi

# 3. 检查 ProGuard 规则
section "3. ProGuard 规则检查"

if [ -f "$APP_DIR/proguard-rules.pro" ]; then
    check_pass "proguard-rules.pro 文件存在"
    
    if grep -q "optimizationpasses" "$APP_DIR/proguard-rules.pro"; then
        check_pass "优化次数已配置"
    fi
    
    if grep -q "androidx.compose" "$APP_DIR/proguard-rules.pro"; then
        check_pass "Compose 规则已配置"
    else
        check_warn "Compose 规则可能不完整"
    fi
else
    check_fail "proguard-rules.pro 不存在"
fi

# 4. 检查 AndroidManifest
section "4. AndroidManifest.xml 检查"

MANIFEST="$APP_DIR/src/main/AndroidManifest.xml"
if [ -f "$MANIFEST" ]; then
    check_pass "AndroidManifest.xml 存在"
    
    if grep -q "usesCleartextTraffic=\"false\"" "$MANIFEST"; then
        check_pass "usesCleartextTraffic=\"false\"（禁用明文流量）"
    else
        check_fail "usesCleartextTraffic 未禁用明文流量"
    fi
    
    if grep -q "networkSecurityConfig" "$MANIFEST"; then
        check_pass "networkSecurityConfig 已配置"
    else
        check_fail "networkSecurityConfig 未配置"
    fi
    
    if grep -q "allowBackup=\"true\"" "$MANIFEST"; then
        check_pass "allowBackup 已配置（配合 backup_rules 排除敏感数据）"
    fi
    
    # 检查硬编码的 API Key
    if grep -E "(app[_-]?key|api[_-]?key|secret)" "$MANIFEST" -i > /dev/null; then
        check_fail "AndroidManifest.xml 中可能存在硬编码密钥"
    else
        check_pass "未发现硬编码 API Key"
    fi
else
    check_fail "AndroidManifest.xml 不存在"
fi

# 5. 检查网络安全配置
section "5. 网络安全配置检查"

NSC="$APP_DIR/src/main/res/xml/network_security_config.xml"
if [ -f "$NSC" ]; then
    check_pass "network_security_config.xml 存在"
    
    if grep -q "cleartextTrafficPermitted=\"false\"" "$NSC"; then
        check_pass "默认 cleartextTrafficPermitted=\"false\""
    else
        check_fail "默认 cleartextTrafficPermitted 未禁用"
    fi
else
    check_fail "network_security_config.xml 不存在"
fi

# 6. 检查备份规则
section "6. 备份规则检查"

BACKUP="$APP_DIR/src/main/res/xml/backup_rules.xml"
DATA_EXTRACTION="$APP_DIR/src/main/res/xml/data_extraction_rules.xml"
if [ -f "$BACKUP" ]; then
    check_pass "backup_rules.xml 存在"
else
    check_warn "backup_rules.xml 不存在"
fi
if [ -f "$DATA_EXTRACTION" ]; then
    check_pass "data_extraction_rules.xml 存在"
else
    check_warn "data_extraction_rules.xml 不存在"
fi

# 7. 检查源代码中的安全风险
section "7. 源代码安全检查"

if grep -rE "printStackTrace" "$APP_DIR/src/main/java" > /dev/null 2>&1; then
    # 排除 LogUtil 中的注释引用
    if grep -rE "printStackTrace" "$APP_DIR/src/main/java" --include="*.kt" | grep -v "LogUtil.kt" | grep -v " \* " > /dev/null 2>&1; then
        check_fail "发现 printStackTrace() 调用（应使用 LogUtil）"
    else
        check_pass "无 printStackTrace() 调用"
    fi
else
    check_pass "无 printStackTrace() 调用"
fi

# 检查硬编码的友盟 AppKey
if grep -rE "698938eb9a7f3764885bbdaa" "$APP_DIR/src/main/java" --include="*.kt" | grep -v "BuildConfig" > /dev/null 2>&1; then
    check_fail "友盟 AppKey 硬编码在源代码中（应通过 BuildConfig 注入）"
else
    check_pass "友盟 AppKey 未硬编码（通过 BuildConfig 注入）"
fi

# 检查 println
if grep -rE "println\(" "$APP_DIR/src/main/java" --include="*.kt" > /dev/null 2>&1; then
    check_fail "发现 println() 调用（应使用 Log）"
else
    check_pass "无 println() 调用"
fi

# 检查 TODO
if grep -rE "TODO|FIXME" "$APP_DIR/src/main/java" --include="*.kt" > /dev/null 2>&1; then
    check_warn "发现 TODO/FIXME 注释（建议清理）"
else
    check_pass "无 TODO/FIXME 注释"
fi

# 8. 检查 .gitignore
section "8. 版本控制安全检查"

GITIGNORE="$PROJECT_ROOT/.gitignore"
if [ -f "$GITIGNORE" ]; then
    if grep -q "\*.keystore" "$GITIGNORE"; then
        check_pass ".gitignore 排除 *.keystore"
    else
        check_fail ".gitignore 未排除 *.keystore"
    fi
    
    if grep -q "keystore-release.properties" "$GITIGNORE"; then
        check_pass ".gitignore 排除 keystore-release.properties"
    else
        check_fail ".gitignore 未排除 keystore-release.properties"
    fi
else
    check_fail ".gitignore 不存在"
fi

# 输出总结
echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  自检结果汇总${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}通过: $PASS_COUNT${NC}  ${YELLOW}警告: $WARN_COUNT${NC}  ${RED}失败: $FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 所有关键检查通过！可以开始构建 Release APK。${NC}"
    if [ $WARN_COUNT -gt 0 ]; then
        echo -e "${YELLOW}⚠ 存在 $WARN_COUNT 个警告项目，建议处理后再发布。${NC}"
    fi
    exit 0
else
    echo -e "${RED}✗ 发现 $FAIL_COUNT 个关键问题，必须先修复后再构建 Release APK。${NC}"
    exit 1
fi
