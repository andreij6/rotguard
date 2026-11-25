# Android Agent-First Coding Constitution

## 1. Architecture & Locality (Locality Over DRY)
- Organize by **vertical feature slices** (`feature/<feature_name>/`), never horizontal layers (`ui/`, `domain/`, `data/`).
- Keep feature-specific mappers, UI models, validation logic, and helper functions **colocated** inside the feature slice. Duplicate small helpers (<30 lines) across features rather than creating shared `Base*` classes or `util` modules.
- Enforce **Unidirectional Data Flow (UDF)** with a pure, deterministic `reduce(state, event)` function separated from side-effect execution.
- Keep every file self-contained and under 500 lines.

## 2. Type Contracts & State Modeling
- **No primitive obsession:** Wrap IDs, timestamps, and currency in `@JvmInline value class` types (e.g., `UserId`, `TimestampEpochMillis`, `PriceInCents`).
- Model every screen's UI state and user actions as exhaustive `sealed interface` hierarchies. Never use multiple independent `Boolean` flags (`isLoading`, `isError`, `isEmpty`) to represent mutually exclusive states.
- Expose a single `val uiState: StateFlow<FeatureUiState>` per `ViewModel`.

## 3. Control Flow & Concurrency (Flat & Boring)
- Write **linear, top-to-bottom `suspend` control flow** inside `viewModelScope.launch`. Avoid multi-flow reactive operator chains (`flatMapLatest`, nested `combine`) when sequential `await` or `async` calls express the same flow.
- Always rethrow `CancellationException` when catching `Exception` inside coroutines.
- Pass all dependencies explicitly via constructor injection (Hilt). Never read global singletons, `LocalContext`, or static state inside business logic or reducers.

## 4. Jetpack Compose Rules
- Composables must be stateless functions accepting `(state: FeatureUiState, onAction: (FeatureAction) -> Unit, modifier: Modifier = Modifier)`.
- Never launch coroutines, access repositories, or mutate state directly inside `@Composable` functions.
- Prefer standard Compose layout primitives (`Row`, `Column`, `Box`) driven by `StateFlow` emissions over custom `Canvas` drawing unless pixel-level custom rendering is strictly required.

## 5. Semantic Naming & Boundary KDoc
- Name functions and properties as unambiguous search anchors including domain subject and units: `fetchPendingOrdersFromRoomCache()`, `timeoutDurationMillis`.
- Every `ViewModel`, `Repository`, and `Reducer` MUST include a top-level KDoc block stating:
  - **Responsibility & Boundaries**
  - **Side Effects** (network, Room DB, disk, telemetry)
  - **Invariants** (preconditions/postconditions)
  - **Anti-Patterns** (what callers must never do)

## 6. Verification Loop & Consistency
- **Golden Pattern Rule:** Before creating a new feature slice, read an existing reference slice (e.g., `feature/orders/`) and mirror its exact file structure, naming, and KDoc format. Do not invent new abstractions.
- Every new state transition or repository method requires a colocated unit test covering success, failure, and edge cases.
- Run `./gradlew testDebugUnitTest detekt` before marking any task complete.
