# 🤖 Agent Guide for Mocha Compound Project

Welcome to the Mocha Compound project! This is an unofficial chat application client based on Kotlin deeply integrated with Telegram's TDLib. This guide is specifically prepared for agents and automated development assistants to quickly familiarize themselves with the underlying architecture, naming conventions, and standard workflows for feature expansion.

## 🎯 Key Commands

*   **Build Android Debug APK**: `./gradlew app:assembleDebug`
*   **Clean & Re-sync**: When dealing with changes in the underlying layer or system dependencies (like JNI or Gradle environment variables), you can optionally clean the project: `./gradlew clean`
*   **Full-text Search**: `grep_search`
*   **List Directory**: `list_dir` (Combined with `find_by_name`)

## 📁 Core Project Structure

The project follows the classic **Clean Architecture** patterns combined with Unidirectional Data Flow (MVI):

```text
compound
├── app/
│   ├── build.gradle.kts           # App module top-level configuration
│   └── src/main/
│       ├── java/com/mocharealm/compound/
│       │   ├── data/              # [Data Layer] Data fetching and persistence implementation
│       │   │   ├── dto/           # Data Transfer Objects
│       │   │   └── source/        # Data source implementations (e.g., `TelegramRepositoryImpl`)
│       │   ├── domain/            # [Domain Layer] Core business abstractions
│       │   │   ├── model/         # Internal standard business models (e.g., `Chat`, `Message`)
│       │   │   ├── repository/    # Repository abstract interface definitions (for decoupling implementations)
│       │   │   └── usecase/       # Atomic business logic use cases (e.g., `SendMessageUseCase`)
│       │   ├── di/                # [DI Layer] Koin Dependency Injection configurations
│       │   │   ├── DataModule.kt  # Repository and external data instances registration
│       │   │   ├── DomainModule.kt# UseCase instances registration
│       │   │   └── UIModule.kt    # ViewModel instances registration
│       │   ├── ui/                # [Presentation Layer] UI interaction and views
│       │   │   ├── composable/    # Reusable, component-based Compose foundation widgets
│       │   │   └── screen/        # Page-level UI and controllers (e.g., `ChatScreen`, `ChatViewModel`)
│       │   └── MainActivity.kt
│       └── cpp/                   # C/C++ JNI native implementations (e.g., customized underlying dependencies)
├── tci18n/                        # Abstracted multi-language internationalization module
└── gradle/libs.versions.toml      # Unified version management configuration for dependency libraries
```

## 🛠️ Standard Workflow for Adding Features

For most system feature additions, you must strictly follow this serialized workflow. **It is STRICTLY PROHIBITED to pierce through the UI layer directly to the underlying API/Repository skipping the Domain layer, unless specifically justified**:

### 1. Update Data Models (Domain Model & DTO)
*   **Modify Business Rules**: If a new feature requires expanding concepts with new properties or fields, navigate to `domain/model` to append or modify them first (e.g., `Chat.kt`).
*   **Append Network Mapping Methods**: Update the corresponding new/old field structures inside `data/dto`, and supplement the conversion workflow sequence to map the raw TDLib object instances returned to internal Domain object instances.

### 2. Define Contracts and Behaviors (Domain Repository)
*   Navigate to `domain/repository/TelegramRepository.kt` to declare new abstractions. All remote communication/asynchronous functions are recommended to be wrapped within `suspend fun doSomething(id: Long): Result<[Domain Model]>` to safely expose the expected operations.

### 3. Implement Data Source Agent (Data Source Implementation)
*   Navigate to `data/source/TelegramRepositoryImpl.kt` and implement the new methods defined in step 2. Execute concrete call logic against lower layers (such as TDLib Object APIs), utilizing `runCatching` to wrap execution return values: utilize patterns like `send(TdApi...())` to invoke native request network protocols.

### 4. Encapsulate Composable System Use Cases (Domain UseCase)
*   Create a single-responsibility new business action class in the `domain/usecase/` directory, for example: `CloseChatUseCase`.
*   **Inject Dependencies**: The exact and only constructor dependency of this class must be the appropriate data-handling `TelegramRepository` (or other relevant Repositories).
*   **Implement Functionality**: Override the method signature behavior as `suspend operator fun invoke(...)`. This method often proxies calls to the underlying Repo, adding upfront filtering like validation, caching strategies, and business rule legality checks.

### 5. Assemble Dependency Injection Factory (DI Modules)
*   Add the Koin configuration snippet in `di/DomainModule.kt`: `factory { YourNewUseCase(get()) }`
*   Ensure the UseCase is properly injected and managed throughout the dependency graph's visible lifecycle.

### 6. Orchestrate the Presentation Controller (UI ViewModel)
*   Inject the previously created `UseCase` into the relevant UI controller's ViewModel parameters. Replace and scrub any direct `Repository` instance references that might have existed there.
*   **Critically Important**: Immediately synchronize with `di/UIModule.kt` to add a `get()` parameter constructor requirement for the modified ViewModel. If omitted, crash errors might not appear immediately during compilation but will cause fatal application crashes when lazy-loading the ViewModel during page navigation.
*   Modify/Expand the `uiState: DefaultData(val param = X)` held by this ViewModel, and process StateFlow event emissions based on newly fetched response contexts to orchestrate the functionality.

### 7. Connect to the User View Render Layer (UI Composable)
*   Open the most upstream `ui/screen/.../*Screen.kt` application file.
*   Reflect the newly acquired State data emitted from the ViewModel back into the visual rendering tier during UI binding. Delegate interactive Compose callback events (e.g., button clicks) back to the ViewModel, ultimately completing the business loop.
