#!/bin/bash
# 批量修复 Kotlin 编译错误

echo "开始修复编译错误..."

# 1. 修复 AppNavigation.kt - 添加缺失的导入
echo "修复 AppNavigation.kt..."
sed -i '1s/^/import com.silas.omaster.ui.theme.saturation\nimport com.silas.omaster.ui.theme.contrast\nimport com.silas.omaster.ui.theme.warmth\nimport com.silas.omaster.ui.theme.sharpness\nimport com.silas.omaster.ui.theme.clarity\nimport com.silas.omaster.ui.theme.brightness\n/' app/src/main/java/com/silas/omaster/AppNavigation.kt

# 2. 修复 OMasterApplication.kt
echo "修复 OMasterApplication.kt..."
# 修复 backing field 问题
sed -i 's/private lateinit var prefs: SharedPreferences/private var prefs: SharedPreferences? = null/' app/src/main/java/com/silas/omaster/OMasterApplication.kt
sed -i 's/prefs = getSharedPreferences/prefs = applicationContext.getSharedPreferences/' app/src/main/java/com/silas/omaster/OMasterApplication.kt

# 3. 修复 MasterInsightEngine.kt - 移除中文变量名
echo "修复 MasterInsightEngine.kt..."
sed -i "s/season时节/season/g" app/src/main/java/com/silas/omaster/ai/MasterInsightEngine.kt

# 4. 修复 PresetRemoteManager.kt - CIO 引擎配置
echo "修复 PresetRemoteManager.kt..."
sed -i "s/endpoint =/\/\/ endpoint =/g" app/src/main/java/com/silas/omaster/network/PresetRemoteManager.kt
sed -i "s/maxConnectionsPerRoute =/\/\/ maxConnectionsPerRoute =/g" app/src/main/java/com/silas/omaster/network/PresetRemoteManager.kt
sed -i "s/pipelineMaxSize =/\/\/ pipelineMaxSize =/g" app/src/main/java/com/silas/omaster/network/PresetRemoteManager.kt
sed -i "s/keepAliveTime =/\/\/ keepAliveTime =/g" app/src/main/java/com/silas/omaster/network/PresetRemoteManager.kt
sed -i "s/connectTimeout =/\/\/ connectTimeout =/g" app/src/main/java/com/silas/omaster/network/PresetRemoteManager.kt
sed -i "s/connectRetryAttempts =/\/\/ connectRetryAttempts =/g" app/src/main/java/com/silas/omaster/network/PresetRemoteManager.kt

echo "修复完成！"
