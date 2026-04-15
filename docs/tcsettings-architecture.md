# Compound Settings 自动化架构规范

这份文档定义了 **Compound** 项目中设置项模块的终极架构。它通过 **KSP** 实现“类型即协议”，利用 **Koin** 进行解耦，并兼容 **MVI** 与 **TDLib** 的异步联动逻辑。

---

## 1. 设计核心 (Core Principles)

- **Domain 纯净性**：Domain 层仅定义状态与 Token，不感知存储介质（DataStore / TDLib）。
- **类型即协议**：通过数据类型（如 `SelectableValue<T>`）自动推导 UI 需求与数据供给契约。
- **插槽式 UI**：UI 组件必须是仅包含 `value` 与 `onValueChange` 的纯受控组件。
- **拦截器副作用**：所有跨模块联动（如改设置后通知 TDLib）都由 `Interceptor` 异步处理。

---

## 2. 开发者编写指南 (Manual Coding)

### A. Domain 层：定义真理之源

使用接口声明设置项，KSP 根据属性类型自动推导生成逻辑。

```kotlin
@SettingsModule(name = "Chat")
interface ChatSettings {
    // 基础类型：自动生成本地持久化逻辑
    @SettingItem
    val showMessageStatus: Boolean

    // 复合类型：KSP 自动推导需要 OptionsProvider 供给数据
    @SettingItem
    val language: SelectableValue<String>
}
```

### B. UI 层：纯受控组件

严格遵守双参数约定，KSP 在编译期校验签名。

```kotlin
@SettingsToken(ChatSettingsToken.Language::class)
@Composable
fun LanguagePicker(
    value: SelectableValue<String>, // 包含 current 与 options
    onValueChange: (String) -> Unit
) {
    // 只负责渲染，不持有 ViewModel / Repository
}
```

### C. Data 层：拦截器与数据供给

通过 Koin 注入，处理存储与联动。

```kotlin
// 拦截器：处理“写”逻辑（例如同步到 TDLib）
@SettingsToken(ChatSettingsToken.Language::class)
class LanguageInterceptor(private val client: TdApi.Client) : SettingsInterceptor<String> {
    override suspend fun intercept(newValue: String): InterceptorResult {
        client.send(TdApi.SetOption("language", TdApi.OptionValueString(newValue)))
        return InterceptorResult.Success
    }
}

// Provider：处理“读”逻辑（供给动态列表）
class TdLibLanguageProvider(private val client: TdApi.Client) : ChatLanguageOptionsProvider {
    override val options: Flow<List<String>> = client.getAvailableLanguagesFlow()
}
```

---

## 3. KSP 自动生成样板代码 (Generated Glue)

### 1) 状态容器 (State)

生成的 State 基于 Token 映射，而非臃肿 Data Class。

```kotlin
class ChatSettingsState(private val values: Map<SettingsToken<*>, Any>) {
    operator fun <T> get(token: SettingsToken<T>): T = values[token] as T
    fun patch(token: SettingsToken<*>, newValue: Any): ChatSettingsState = ...
}
```

### 2) 依赖注入 (Koin Module)

自动生成 Interceptor 与 Repository 的绑定。

```kotlin
val generatedChatSettingsModule = module {
    single { ChatSettingsRepository(get(), get()) }
    single {
        SettingsInterceptorDispatcher(
            mapOf(ChatSettingsToken.Language::class to get<LanguageInterceptor>())
        )
    }
}
```

### 3) UI 路由映射 (Mapper)

```kotlin
@Composable
fun RenderChatSetting(token: SettingsToken<*>, state: ChatSettingsState, onIntent: (Intent) -> Unit) {
    when (token) {
        is ChatSettingsToken.Language -> LanguagePicker(
            value = state[token],
            onValueChange = { onIntent(UpdateIntent(token, it)) }
        )
    }
}
```

---

## 4. 整体架构流转

1. **用户操作**：UI 触发 `onValueChange`。
2. **Intent 处理**：ViewModel 接收更新 Intent。
3. **拦截逻辑**：Dispatcher 定位 `LanguageInterceptor`，执行 TDLib 请求。
4. **持久化**：拦截器成功后更新本地缓存（DataStore）。
5. **状态分发**：Repository `combine` 持久化值与 Provider 动态列表，推动 UI 局部重绘。

---

## 5. 样板代码对比 (Boilerplate Reduction)

| 传统模式 (Manual) | 自动化架构 (KSP) |
| :--- | :--- |
| 手动修改 Data Class 字段 | **Domain 接口增删一行代码** |
| 手动编写 UseCase / ViewModel | **KSP 自动生成流式处理** |
| UI 手动处理 TDLib 异步回调 | **Interceptor 自动拦截副作用** |
| 改设置导致全屏重组成本高 | **Token Patch 驱动局部更新** |

---

> 结语：这套方案实现了“配置即代码”。对 Compound 这类 TDLib 联动密集应用，它不仅提升效率，也增强一致性与可验证性。
