# Android KMP 插件设置指南

本文档总结了官方文档中关于为 Kotlin Multiplatform (KMP) 设置 `com.android.kotlin.multiplatform.library` Gradle 插件的内容。

## 概述

`com.android.kotlin.multiplatform.library` 插件是官方支持的工具，用于向 KMP 库模块添加 Android 目标。它可以简化项目配置、提升构建性能，并提供与 Android Studio 的更好集成。

**重要提示：** 传统的 `com.android.library` 插件依赖于已废弃的 API，这些 API 将在 AGP 9.0+ 中需要选择启用，并在 AGP 10.0 中移除。

---

## 主要特性与区别

Android-KMP 插件与标准的 `com.android.library` 插件有以下关键不同：

| 特性 | Android-KMP 插件 |
| :--- | :--- |
| **架构** | 单变体架构（移除产品变种和构建类型支持） |
| **优化** | 专为 KMP 库设计，专注共享代码和互操作性 |
| **测试** | 默认停用单元测试和设备测试（可选择性启用） |
| **Android 扩展** | 无顶级 `android` 扩展，配置通过 Gradle KMP DSL 中的 `android` 代码块处理 |
| **Java 编译** | 默认停用，使用 `withJava()` 启用 |

---

## 优势

- **提升构建性能和稳定性**
- **增强 IDE 集成**（代码补全、导航、调试）
- **简化项目配置**（移除构建变体等复杂性，避免令人困惑的源代码集命名）

---

## 不受支持功能的解决方法

### 1. 构建变体
- **替代方案**：创建一个独立的 `com.android.library` 模块配置变体，然后作为依赖项从 KMP 库的 `androidMain` 源代码集使用。

### 2. 数据绑定和视图绑定
- **建议**：使用 Compose Multiplatform 等多平台框架处理 UI。

### 3. 原生构建支持
- **替代方案**：使用 KMP 的原生目标（如 `androidNativeArm64`）及其 C 互操作功能，或创建独立的 `com.android.library` 模块集成原生代码。

### 4. BuildConfig 类
- **替代方案**：使用 BuildKonfig 插件或类似社区方案。

---

## 前提条件

- **Android Gradle 插件 (AGP)**：8.10.0 或更高版本
- **Kotlin Gradle 插件 (KGP)**：2.0.0 或更高版本

---

## 应用插件步骤

### 1. 在版本目录中声明插件 (`gradle/libs.versions.toml`)
```toml
[versions]
androidGradlePlugin = "9.2.0"
kotlin = "KOTLIN_VERSION"

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-kotlin-multiplatform-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "androidGradlePlugin" }
```

### 2. 在根构建文件中应用插件 (`build.gradle.kts`)
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}
```

### 3. 在模块构建文件中应用插件
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}
```

### 4. 配置 Android KMP 目标
```kotlin
kotlin {
    android {
        namespace = "com.example.kmpfirstlib"
        compileSdk = 33
        minSdk = 24
        
        withJava() // 可选：启用 Java 编译
        
        // 可选：启用测试
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
        
        compilerOptions.configure {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            // Android 专用依赖
        }
    }
}
```

---

## 从旧版插件迁移

### 1. 移动源代码目录
| 旧目录 | 新目录 |
| :--- | :--- |
| `src/main` | `src/androidMain` |
| `src/test` | `src/androidHostTest` |
| `src/androidTest` | `src/androidDeviceTest` |

### 2. 声明依赖项
- **新插件**：依赖项在 `sourceSets` 代码块内声明
- **旧插件**：依赖项在顶级 `dependencies` 代码块内声明

### 3. 启用 Android 资源
```kotlin
kotlin {
    android {
        androidResources {
            enable = true // 显式启用资源处理
        }
    }
}
```

### 4. 配置测试
```kotlin
kotlin {
    android {
        withHostTest { isIncludeAndroidResources = true }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
    }
}
```

### 5. 启用 Java 编译并配置 JVM 目标
```kotlin
kotlin {
    android {
        withJava()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}
```

### 6. 配置 JVM 目标（优先级从低到高）
- **工具链级别**（最低优先级）：`kotlin.jvmToolchain(21)`
- **Android 目标级别**（中等优先级）：`android.compilerOptions.jvmTarget.set(...)`
- **编译级别**（最高优先级）：`compilations.all { compilerOptions.jvmTarget.set(...) }`

### 7. 发布消费者 keep 规则
```kotlin
kotlin {
    android {
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard-rules.pro")
            }
        }
    }
}
```

### 8. 发布到 Maven
- 新插件与标准 KMP 发布机制集成，无需额外 Android 专用步骤。

---

## 其他资源

- [设置环境](https://developer.android.google.cn/kotlin/multiplatform/setup?hl=zh-cn)
- [向项目添加 KMP 模块](https://developer.android.google.cn/kotlin/multiplatform/add-module?hl=zh-cn)
- [API 参考文档](https://developer.android.google.cn/reference/tools/gradle-api/com/android/build/api/dsl/AndroidKotlinMultiplatform?hl=zh-cn)