# Release 签名配置说明

## 概述

本应用使用 Gradle 签名配置来生成正式版 APK。为了确保安全性，签名密钥不应提交到版本控制系统。

## 配置步骤

### 1. 创建签名密钥库 (Keystore)

在项目根目录下创建签名密钥库：

```bash
keytool -genkey -v -keystore release.keystore -alias omaster -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 创建 keystore.properties 文件

在项目根目录下创建 `keystore-release.properties` 文件（不要提交到 Git）：

```properties
STORE_FILE=release.keystore
STORE_PASSWORD=your_store_password
KEY_ALIAS=omaster
KEY_PASSWORD=your_key_password
```

### 3. 配置 .gitignore

确保以下文件不会被提交到 Git：

```
*.keystore
*.jks
keystore-release.properties
keystore.properties
```

### 4. Gradle 配置

签名配置已在 `app/build.gradle.kts` 中完成：

```kotlin
android {
    signingConfigs {
        create("release") {
            // 优先使用 keystore-release.properties
            val keystorePropertiesFile = rootProject.file("keystore-release.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(keystorePropertiesFile.inputStream())
                storeFile = file(keystoreProperties["STORE_FILE"] as String)
                storePassword = keystoreProperties["STORE_PASSWORD"] as String
                keyAlias = keystoreProperties["KEY_ALIAS"] as String
                keyPassword = keystoreProperties["KEY_PASSWORD"] as String)
            } else {
                // 回退到 debug 签名（仅用于 CI/CD）
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## CI/CD 配置

在 GitHub Actions 中，使用环境变量配置签名：

```yaml
- name: Build Release APK
  env:
    STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: |
    echo "STORE_FILE=release.keystore" > keystore-release.properties
    echo "STORE_PASSWORD=$STORE_PASSWORD" >> keystore-release.properties
    echo "KEY_ALIAS=omaster" >> keystore-release.properties
    echo "KEY_PASSWORD=$KEY_PASSWORD" >> keystore-release.properties
    ./gradlew assembleRelease
```

## 安全注意事项

1. **永远不要**将签名密钥库提交到版本控制系统
2. 使用强密码保护密钥库
3. 定期备份密钥库文件
4. 密钥库丢失后无法恢复，将导致无法更新应用
5. 建议使用硬件安全模块 (HSM) 或密钥管理服务 (KMS) 存储密钥

## 验证签名

构建完成后，可以使用以下命令验证 APK 签名：

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## 常见问题

### Q: 为什么 CI/CD 构建使用 debug 签名？
A: 为了确保 CI/CD 流程可以正常运行，当 `keystore-release.properties` 不存在时，会回退到 debug 签名。正式发布时必须配置正确的签名密钥。

### Q: 如何更新签名密钥？
A: 如果需要更新签名密钥，需要使用相同的包名和签名密钥，否则无法更新应用。建议在发布第一个版本前就确定好签名密钥。

### Q: 密钥库密码忘记了怎么办？
A: 密钥库密码无法找回。如果忘记密码，需要重新创建密钥库，但这样会导致无法更新已发布的应用。建议使用密码管理器保存密码。