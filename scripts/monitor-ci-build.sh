#!/bin/bash
# CI 构建监控脚本
# 持续跟踪构建状态直到完成

REPO="Tri250/OPPOMaster2"
RUN_ID="${1:-27666009328}"
TOKEN="${GITHUB_TOKEN:-ghu_zJjv9UsD4bikGgoqfCoO1bQPnOV1ZA05XOqr}"

echo "======================================"
echo "CI 构建监控"
echo "仓库: $REPO"
echo "运行 ID: $RUN_ID"
echo "======================================"
echo ""

# 构建状态跟踪
while true; do
    # 获取构建状态
    RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
        "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID")
    
    STATUS=$(echo "$RESPONSE" | grep -o '"status": "[^"]*"' | head -1 | cut -d'"' -f4)
    CONCLUSION=$(echo "$RESPONSE" | grep -o '"conclusion": "[^"]*"' | head -1 | cut -d'"' -f4)
    RUN_NUMBER=$(echo "$RESPONSE" | grep -o '"run_number": [0-9]*' | head -1 | grep -o '[0-9]*')
    WORKFLOW_NAME=$(echo "$RESPONSE" | grep -o '"name": "[^"]*"' | head -1 | cut -d'"' -f4)
    HTML_URL=$(echo "$RESPONSE" | grep -o '"html_url": "[^"]*"' | head -1 | cut -d'"' -f4)
    
    # 获取 jobs 信息
    JOBS_RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
        "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/jobs")
    
    echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "工作流: $WORKFLOW_NAME #$RUN_NUMBER"
    echo "状态: $STATUS"
    echo "结论: ${CONCLUSION:-N/A}"
    echo "链接: $HTML_URL"
    echo ""
    
    # 显示 jobs 状态
    echo "Jobs 状态:"
    echo "$JOBS_RESPONSE" | grep -o '"name": "[^"]*"' | cut -d'"' -f4 | while read job_name; do
        echo "  - $job_name"
    done
    echo ""
    
    # 检查是否完成
    if [ "$STATUS" == "completed" ]; then
        echo "======================================"
        echo "构建完成!"
        echo "最终状态: ${CONCLUSION:-unknown}"
        echo "======================================"
        
        # 获取产物信息
        echo ""
        echo "构建产物:"
        ARTIFACTS_RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
            "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/artifacts")
        
        echo "$ARTIFACTS_RESPONSE" | grep -o '"name": "[^"]*"' | cut -d'"' -f4 | while read artifact; do
            echo "  - $artifact"
        done
        
        break
    fi
    
    echo "构建进行中，30秒后刷新..."
    echo "--------------------------------------"
    sleep 30
done
