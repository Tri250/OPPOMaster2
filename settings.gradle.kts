pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        // 沙盒网络受限，仅使用本地缓存的依赖
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 注意：沙盒环境无网络访问，外部仓库已移除
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        // 沙盒网络受限，仅使用本地缓存的依赖
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 注意：沙盒环境无网络访问，外部仓库已移除
    }
}

rootProject.name = "OMaster"
include(":app")
