# Compound Settings 自动化架构规范

这份文档定义了 **Compound** 项目中设置项模块的终极架构。它通过 **KSP** 实现"类型即协议"，利用 **Koin** 进行解耦，并兼容 **MVI** 与 **TDLib** 的异步联动逻辑。

---

## 1. 设计核心 (Core Principles)

- **Domain 纯净性**：Domain 层仅定义状态与 Token，不感知存储介质（DataStore / TDLib）。
- **类型即协议**：通过数据类型（如 `SelectableValue<T>`）自动推导 UI 需求与数据供给契约。
- **Token 自渲染**：每个 Token 对象携带 `Render()` 方法，自动处理状态获取和更新。
- **拦截器副作用**：所有跨模块联动（如改设置后通知 TDLib）都由 `Interceptor` 异步处理。
- **更新拦截**：`onValueChange` 返回 `Boolean`，允许或阻止更新。

---

## 2. 开发者编写指南 (Manual Coding)

### A. Domain 层：定义真理之源

使用接口声明设置项，KSP 根据属性类型自动推导生成逻辑。

```kotlin
@SettingsModule
interface ChatSettingsModule {
    @SettingItem
    @DefaultValue("false")
    val listSwipeGesture: Boolean

    @SettingItem
    @DefaultValue("16")
    val emojiFont: Int
}
```

### B. UI 层：Token 自渲染

每个 Token 对象都携带 `Render()` 方法，自动处理状态获取和更新。

```kotlin
@Composable
fun ChatSettingsFragment() {
    // 方式1：完全自动
    ChatSettingsModuleToken.ListSwipeGesture.Render()
    
    // 方式2：自定义处理（拦截更新）
    ChatSettingsModuleToken.EmojiFont.Render { newValue ->
        if (newValue > 32) {
            showError("值太大")
            false  // 阻止更新
        } else {
            true   // 允许更新
        }
    }
}
```

### C. Data 层：拦截器与数据供给

通过 Koin 注入，处理存储与联动。

```kotlin
// 拦截器：处理"写"逻辑（例如同步到 TDLib）
@SettingsToken(ChatSettingsModuleToken.Language::class)
class LanguageInterceptor(private val client: TdApi.Client) : SettingsInterceptor<String> {
    override suspend fun intercept(newValue: String): InterceptorResult {
        client.send(TdApi.SetOption("language", TdApi.OptionValueString(newValue)))
        return InterceptorResult.Success
    }
}
```

---

## 3. KSP 自动生成样板代码 (Generated Glue)

### 1) Token 类型 (Token)

生成的 Token 是 sealed interface，每个实现是 object，携带 `Render()` 方法。

```kotlin
sealed interface ChatSettingsModuleToken<T> : SettingToken<T> {
    @Composable
    fun Render(onValueChange: ((T) -> Boolean)? = null)

    object ListSwipeGesture : ChatSettingsModuleToken<Boolean> {
        @Composable
        override fun Render(onValueChange: ((Boolean) -> Boolean)?) {
            val store = remember { koinInject<SettingsStore>() }
            val controller = remember { koinInject<SettingsController>() }
            
            val value = remember(this) { store.flow(this, false) }
                .collectAsState(initial = false).value
            
            val handleChange: (Boolean) -> Unit = { newValue ->
                val allowed = onValueChange?.invoke(newValue) ?: true
                if (allowed) {
                    controller.update(this, newValue)
                }
            }
            
            RenderBooleanSettingItem(value, handleChange)
        }
    }
}
```

### 2) 状态容器 (State)

生成的 State 是 data class，包含所有设置项的当前值。

```kotlin
data class ChatSettingsModuleState(
    val listSwipeGesture: Boolean = false,
    val emojiFont: Int = 16
)

@Composable
fun rememberChatSettingsModuleState(): ChatSettingsModuleState {
    val store = remember { koinInject<SettingsStore>() }
    
    val listSwipeGesture by remember { store.flow(ChatSettingsModuleToken.ListSwipeGesture, false) }
        .collectAsState(initial = false)
    val emojiFont by remember { store.flow(ChatSettingsModuleToken.EmojiFont, 16) }
        .collectAsState(initial = 16)
    
    return remember(listSwipeGesture, emojiFont) {
        ChatSettingsModuleState(listSwipeGesture, emojiFont)
    }
}
```

### 3) 依赖注入 (Koin Module)

自动生成 Interceptor 与 Dispatcher 的绑定。

```kotlin
val generatedChatSettingsModuleModule = module {
    single { ChatSettingsModuleInterceptor1(get()) }
    single { ChatSettingsModuleInterceptor2(get()) }
    single {
        SettingsInterceptorDispatcher(
            mapOf(
                ChatSettingsModuleToken.ListSwipeGesture to get<ChatSettingsModuleInterceptor1>(),
                ChatSettingsModuleToken.EmojiFont to get<ChatSettingsModuleInterceptor2>()
            )
        )
    }
}
```

---

## 4. 整体架构流转

1. **用户操作**：UI 调用 `Token.Render()` 或 `Token.Render { ... }`。
2. **状态获取**：Token 从 `SettingsStore` 自动获取当前值。
3. **更新拦截**：`onValueChange` 返回 `Boolean`，允许或阻止更新。
4. **拦截逻辑**：`SettingsController` 调用 `SettingsInterceptorDispatcher`，执行 TDLib 请求。
5. **持久化**：拦截器成功后更新本地缓存（DataStore）。
6. **状态分发**：`SettingsStore.flow()` 自动推送新值，推动 UI 局部重绘。

---

## 5. 样板代码对比 (Boilerplate Reduction)

| 传统模式 (Manual) | 自动化架构 (KSP) |
| :--- | :--- |
| 手动修改 Data Class 字段 | **Domain 接口增删一行代码** |
| 手动编写 UseCase / ViewModel | **KSP 自动生成流式处理** |
| UI 手动处理 TDLib 异步回调 | **Interceptor 自动拦截副作用** |
| 改设置导致全屏重组成本高 | **Token 自渲染驱动局部更新** |
| 手动管理状态和回调 | **Token 自动获取状态和处理更新** |

---

## 6. API 速览

```kotlin
// 1. 声明设置模块
@SettingsModule
interface ChatSettingsModule {
    @SettingItem
    @DefaultValue("false")
    val listSwipeGesture: Boolean
}

// 2. 使用 Token 渲染
ChatSettingsModuleToken.ListSwipeGesture.Render()

// 3. 自定义处理（拦截更新）
ChatSettingsModuleToken.ListSwipeGesture.Render { newValue ->
    if (needsConfirm) {
        showConfirmDialog()
        false  // 阻止
    } else {
        true   // 允许
    }
}

// 4. 批量读取状态
val state = rememberChatSettingsModuleState()
println(state.listSwipeGesture)

// 5. 监听错误
val controller = rememberSettingsController()
LaunchedEffect(controller) {
    controller.errors.collect { error ->
        showError(error.message)
    }
}
```

---

## 7. 模块结构

```
tcsettings/
├── core/                    # 纯 Kotlin 模块：注解 + 接口定义
│   ├── SettingsAnnotations.kt   # @SettingsModule, @SettingItem, @DefaultValue, @SettingsToken, @SettingsFallback
│   └── SettingsContracts.kt     # SettingToken, SettingsStore, SettingsInterceptor, SettingsInterceptorDispatcher, SettingsError, SelectableValue
├── compose/                 # Android Compose 模块：运行时支持
│   └── SettingsController.kt    # SettingsController, rememberSettingsController
└── processor/               # KSP 处理器：代码生成
    └── TcSettingsSymbolProcessor.kt  # 生成 Token, State, Koin Module
```

---

> 结语：这套方案实现了"配置即代码"。对 Compound 这类 TDLib 联动密集应用，它不仅提升效率，也增强一致性与可验证性。Token 自渲染设计让 UI 层代码更加简洁，类型安全性得到保障。
