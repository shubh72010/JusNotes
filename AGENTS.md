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
- CI: `.github/workflows/release.yml` (push→main, JDK 17, `assembleRelease`, changelog, `softprops/action-gh-release`, `attest-build-provenance`); Dependabot `.github/dependabot.yml` (gradle + actions grouped).
- Release APK at `app/build/outputs/apk/release/app-release.apk` (2.7M, R8, signed via `~/.android/debug.keystore`), debug at `app/build/outputs/apk/debug/app-debug.apk`.
- No lint/detekt locally — just build + test.

## Gotcha: `gradle.properties` breaks on Linux
`org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr` is hardcoded to a Windows path. On Linux override it:
```bash
./gradlew assembleDebug -Porg.gradle.java.home=/path/to/jdk17
# or unset/export JAVA_HOME and delete the line for local builds
```
Requires JDK 17 (`JavaVersion.VERSION_17`, `jvmTarget 17`).

## Project Layout
- `app/src/main/java/com/folius/dotnotes/`
  - `MainActivity.kt` — sole `NavHost` (routes: `list`, `settings`, `trash`, `secret_notes`, `editor/{noteId}?initialTab=`, `image_viewer/{imageUri}`, `map/{noteId}`, `tutorial`) with spring transitions; `tutorial` reuses `VisualTutorialOverlay`
  - `data/` — `Note.kt` (entity, indices on timestamp/lastModified/isSecret/isPinned/isDeleted), `NoteDao.kt`, `AppDatabase.kt` (v10, `fallbackToDestructiveMigration`, `exportSchema=false`), `NoteRepository.kt`, `SettingsManager.kt` (DataStore: VIEW_MODE + THEME + API_KEY + HAS_SHOWN_GESTURE_HINTS)
  - `ui/` — `NoteViewModel.kt` (monolithic — owns all `StateFlow`s, `viewMode`, `debounce 300ms`, pinned-on-top sort), `NoteListScreen.kt` (`GridNoteItem` + `LazyVerticalStaggeredGrid`), `NoteEditorScreen.kt`, `NoteMapScreen.kt`, `SettingsScreen.kt`, `TrashScreen.kt`, `SecretNotesScreen.kt`, `TutorialScreen.kt`
  - `ui/components/` — `TutorialOverlay.kt` (spotlight pill/notes), `BlurText.kt` (alpha+slide only, no RenderEffect on list), `PinnedNotesBar.kt`, `MarkdownBlockView.kt`, etc.
  - `network/` — `NetworkModule.kt` (singleton Retrofit), `OpenRouterApi.kt`
  - `utils/` — `BackupUtils.kt`, `MarkdownRenderer.kt`
- Trash is soft-delete (`isDeleted` + `deletedTimestamp`); 30-day auto-purge in `NoteViewModel.init`. Grid/List persisted via `SettingsManager.viewMode`.

## Architecture Notes
- No DI framework — `NoteViewModel` manually constructs `AppDatabase.getDatabase()` + `NoteRepository` + `SettingsManager`.
- Maps replace former folders: a Map is a `Note` with `isMap=true` + `mapItems`; linked notes via `linkedNoteIds` / wiki `[[Note Name]]` links. Search `isMap`/`linkedNoteIds` before assuming folder code exists.
 - `release` build enables `isMinifyEnabled`/`isShrinkResources` + `signingConfig release` (debug keystore); `proguard-rules.pro` is empty — expect R8 to strip unkept reflection if added.
 - `kotlinOptions.freeCompilerArgs` opts-in `ExperimentalFoundationApi/Material3/ExperimentalLayoutApi` so release compiles with `w:` not `e:`.
- `patch_note_editor.py` / `fix_note_editor.ps1` / `check_braces.py` are stale one-off patch scripts from the folder→map migration — ignore unless touching `NoteEditorScreen.kt`.

## Style
- `kotlin.code.style=official`, `android.nonTransitiveRClass=true`.
- No formatter/linter config — follow existing Kotlin style.

## References
- `CURRENT_STATE.md` — current architecture decisions and next priorities (editor state → ViewModel, shared transitions, advanced search).
- `README.md` — feature overview; `ARCHITECTURE.md` — data flow; `DESIGN_LANGUAGE.md` — pill system; `CONTRIBUTING.md` — branching from `main`, GPL-3.0.
