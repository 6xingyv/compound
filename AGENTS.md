# 🤖 Agent Guide for Mocha Compound Project

Welcome to the Mocha Compound project! This is an unofficial chat application client based on Kotlin deeply integrated with Telegram's TDLib. This guide is specifically prepared for agents and automated development assistants to quickly familiarize themselves with the underlying architecture, naming conventions, and standard workflows for feature expansion.

## 🎯 Key Tech Stack

* **Language**: Kotlin (with C++ JNI for TDLib)
* **UI Framework**: Jetpack Compose
* **Visual Effects**: [Gaze Glassy](https://github.com/6xingyv/gaze-glassy)
* **Architecture**: Clean Architecture + Unidirectional Data Flow (MVI)
* **Core Engine**: TDLib
* **Dependency Injection**: Koin
* **Media**: Media3 & Coil & `VpxPlayer`(for webm stickers)
* **i18n**: `tci18n` (KSP-powered localized string system)

## 📁 Core Project Structure

The project follows the classic **Clean Architecture** patterns combined with Unidirectional Data Flow (MVI):

```text
compound
├── app/
│   ├── build.gradle.kts           # App module top-level configuration
│   └── src/main/
│       ├── java/com/mocharealm/compound/
│       │   ├── data/              # [Data Layer] TDLib interactions and persistence
│       │   │   ├── dto/           # Data Transfer Objects
│       │   │   ├── mapper/        # Converters between TDLib objects and Domain models
│       │   │   └── source/        # Concrete implementation of Repositories (e.g., `TelegramRepositoryImpl`)
│       │   ├── domain/            # [Domain Layer] Business logic (Pure Kotlin)
│       │   │   ├── model/         # Internal business models (e.g., `Chat`, `Message`)
│       │   │   ├── repository/    # Repository interfaces
│       │   │   └── usecase/       # Single-responsibility business actions
│       │   ├── di/                # [DI Layer] Koin modules
│       │   ├── ui/                # [Presentation Layer] MVI + Compose
│       │   │   ├── composable/    # Reusable UI widgets
│       │   │   ├── screen/        # Screen-level UI (View + ViewModel)
│       │   │   └── theme/         # App styling and colors
│       │   └── App.kt             # Application class (DI initialization)
│       ├── cpp/                   # C++ JNI code for TDLib and native enhancements
│       └── java/org/drinkless/tdlib/ # TDLib Java binding (DO NOT MODIFY)
├── tci18n/                        # Custom Internationalization Module
│   ├── core/                      # Runtime logic for localized strings
│   └── processor/                 # KSP Processor for static analysis and preloading
└── gradle/libs.versions.toml      # Centralized dependency management
```

## 🛠️ Standard Workflow for Adding Features

Strictly follow this workflow to maintain architectural integrity. **Do NOT skip layers**.

### 1. Update Domain Model & DTOs

* Define or update business models in `domain/model/`.
* If data comes from TDLib, check if `data/dto/` needs updates.
* Update/Add mappers in `data/mapper/` to handle the conversion.

### 2. Define Repository Contract

* Add the new method to `domain/repository/TelegramRepository.kt`.
* Use `Result<T>` or `Flow<T>` for asynchronous data streams.

### 3. Implement in Data Source

* Implement the method in `data/source/TelegramRepositoryImpl.kt`.
* Use `send(TdApi.Request())` to interact with TDLib.

### 4. Create/Update UseCase

* Create a new class in `domain/usecase/` (e.g., `GetChatDetailsUseCase.kt`).
* Inject the `TelegramRepository` into the constructor.
* Implement `suspend operator fun invoke(...)`.

### 5. Register in DI

* Register the UseCase in `di/DomainModule.kt`.
* Ensure the Repository is registered in `di/DataModule.kt`.

### 6. Update ViewModel & UI State

* Inject the UseCase into the relevant ViewModel in `ui/screen/`.
* Update the `UiState` data class to reflect new data.
* Expose a `StateFlow<UiState>` for the UI to consume.
* **Remember**: Register new ViewModels in `di/UIModule.kt`.

### 7. Implementation in Compose

* Bind the `UiState` in the `*Screen.kt` file.
* Use `tdString("Key")` for any user-visible text.

## 🌍 Internationalization with `tci18n`

Mocha Compound uses a custom localization system that bridges TDLib's dynamic string loading with Compose.

* **Usage**: `tdString("Your_Key_Here")` inside a `@Composable` function.
* **Dynamic Args**: `tdString("WelcomeUser", "name" to userName)`.
* **How it works**: The `tci18n` processor scans for these calls and informs the `TdStringProvider` to preload these keys when navigating to a screen.
* **Provider**: Registered in `DataModule.kt`. It fetches strings from TDLib's `getLocalizationTargetInfo` and `getLanguagePackString` equivalents.

## 🎯 Key Commands

* **Build Android Debug APK**: `./gradlew app:assembleDebug`
* **Clean & Re-sync**: `./gradlew clean`
* **Check Lint**: `./gradlew app:lint`

## ⚠️ Important Rules

1. **Never** use `TdApi` directly in the UI layer. All TDLib interactions must be abstracted via Repositories and UseCases.
2. **MVI Pattern**: ViewModels should emit States and receive Intents/Events. Avoid exposing mutable state directly.
3. **Thread Safety**: TDLib operations are generally handled on a dedicated thread by the Repository implementation. Use `Dispatchers.IO` for repository logic.
4. **UI Consistency**: Use components from `ui/composable` or `miuix` to maintain the app's aesthetic.
