# AGENTS.md — DotNotes

## Stack
- Single-module Android app (`:app`), package `com.folius.dotnotes`, minSdk 24 / compileSdk 36 / targetSdk 36.
- Jetpack Compose (BOM 2024.09.00) + Material3, Room 2.6.1 + KSP, Navigation Compose 2.8.5, Retrofit+OkHttp, DataStore, Biometric.
- Version catalog at `gradle/libs.versions.toml` — add/update deps there.

## Build & Run
```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # minified (R8 + shrinkResources)
./gradlew test                   # single trivial unit test (app/src/test)
./gradlew connectedAndroidTest   # requires device/emulator
./gradlew clean
```
- No lint/detekt/CI checks configured — just build + test.

## Gotcha: `gradle.properties` breaks on Linux
`org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr` is hardcoded to a Windows path. On Linux override it:
```bash
./gradlew assembleDebug -Porg.gradle.java.home=/path/to/jdk17
# or unset/export JAVA_HOME and delete the line for local builds
```
Requires JDK 17 (`JavaVersion.VERSION_17`, `jvmTarget 17`).

## Project Layout
- `app/src/main/java/com/folius/dotnotes/`
  - `MainActivity.kt` — sole `NavHost` (routes: `list`, `settings`, `trash`, `secret_notes`, `editor/{noteId}?initialTab=`, `image_viewer/{imageUri}`, `map/{noteId}`) with spring transitions
  - `data/` — `Note.kt` (entity), `NoteDao.kt`, `AppDatabase.kt` (v10, `fallbackToDestructiveMigration`, `exportSchema=false`), `NoteRepository.kt`, `SettingsManager.kt` (DataStore)
  - `ui/` — `NoteViewModel.kt` (monolithic — owns all `StateFlow`s), `NoteListScreen.kt`, `NoteEditorScreen.kt`, `NoteMapScreen.kt`, `SettingsScreen.kt`, `TrashScreen.kt`, `SecretNotesScreen.kt`
  - `network/` — `NetworkModule.kt` (singleton Retrofit), `OpenRouterApi.kt`
  - `utils/` — `BackupUtils.kt`, `MarkdownRenderer.kt`
- Trash is soft-delete (`isDeleted` + `deletedTimestamp`); 30-day auto-purge in `NoteViewModel.init`.

## Architecture Notes
- No DI framework — `NoteViewModel` manually constructs `AppDatabase.getDatabase()` + `NoteRepository` + `SettingsManager`.
- Maps replace former folders: a Map is a `Note` with `isMap=true` + `mapItems`; linked notes via `linkedNoteIds` / wiki `[[Note Name]]` links. Search `isMap`/`linkedNoteIds` before assuming folder code exists.
- `release` build enables `isMinifyEnabled`/`isShrinkResources`; `proguard-rules.pro` is empty — expect R8 to strip unkept reflection if added.
- `patch_note_editor.py` / `fix_note_editor.ps1` / `check_braces.py` are stale one-off patch scripts from the folder→map migration — ignore unless touching `NoteEditorScreen.kt`.

## Style
- `kotlin.code.style=official`, `android.nonTransitiveRClass=true`.
- No formatter/linter config — follow existing Kotlin style.

## References
- `CURRENT_STATE.md` — current architecture decisions and next priorities (editor state → ViewModel, shared transitions, advanced search).
- `README.md` — feature overview; `CONTRIBUTING.md` — branching from `main`, GPL-3.0.
