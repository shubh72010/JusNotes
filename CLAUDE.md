# CLAUDE.md — JusNotes

This file is for Claude Code / Muse. Keep it tight and grounded.

## What This Repo Is

Single-module Android app (`:app`, `com.jusdots.jusnotes`, minSdk 24 / compileSdk 36 / targetSdk 36, JDK 17, Kotlin 2.0.21, Compose BOM 2024.09.00). NavHost lives in `MainActivity.kt` with routes `list`, `settings`, `trash`, `secret_notes`, `editor/{noteId}`, `image_viewer/{imageUri}`, `map/{noteId}`, `tutorial`. Data is Room v10 (`Note` entity) + DataStore (`SettingsManager`), no DI, `NoteViewModel` owns all `StateFlow`s.

## How to Build

```bash
./gradlew assembleDebug -Porg.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64  # Linux override needed (gradle.properties hardcodes Windows jbr)
./gradlew assembleRelease   # R8 + shrinkResources, signed with debug.keystore (2.7M)
./gradlew test
```

Catalog is `gradle/libs.versions.toml` — add deps there. `app/build.gradle.kts` has signing with debug keystore and `kotlinOptions -opt-in` for `ExperimentalFoundationApi/Material3`.

## Where to Look

- `data/Note.kt:10` — entity (title/content/timestamp/lastModified/images/isChecklist/checklist/isSecret/isPinned/isDeleted/deletedTimestamp/linkedNoteIds/tags/color/isMap/mapItems)
- `data/NoteDao.kt` / `AppDatabase.kt:8` / `NoteRepository.kt` / `SettingsManager.kt`
- `ui/NoteViewModel.kt:31` — all flows, debounced search (300ms), tab/tag/map filtering, pinned-on-top sort, 30-day trash purge
- `ui/NoteListScreen.kt` — list vs `LazyVerticalStaggeredGrid(2)`, swipe actions, pill menu
- `ui/NoteEditorScreen.kt` — ~40 `remember` states (future move to VM)
- `ui/SettingsScreen.kt`, `TrashScreen.kt`, `SecretNotesScreen.kt`, `NoteMapScreen.kt`, `TutorialScreen.kt`
- `ui/components/TutorialOverlay.kt`, `BlurText.kt` (now alpha+slide only), `MarkdownBlockView.kt`
- `network/NetworkModule.kt` / `OpenRouterApi.kt` — single Retrofit for OpenRouter
- `res/values/themes.xml` — transparent bars, windowBackground

## Pill System

Every screen has the same floating pill (`surfaceContainerHighest`, height `56↔320/420dp`, corner `28↔32dp`, spring `damping 0.85 stiffness 300`). `detectDragGestures` + `combinedClickable`. Don't diverge per-screen.

## Maps ≠ Folders

`Note(isMap=true, mapItems, linkedNoteIds)` replaces folders. Search `isMap` before assuming folder code exists. `fix_note_editor.ps1` etc. are stale migration scripts.

## Perf Trap Fixed

`BlurText` previously ran per-word `RenderEffect.createBlurEffect` on every list item → hundreds of blurs. Now `NoteListScreen` uses plain `Text`; `BlurText` elsewhere is just `alpha + 12dp` translation. Don't reintroduce blur on list screens.

## Conventions

- `kotlin.code.style=official`, `nonTransitiveRClass=true`
- No formatter/linter. Match existing Kotlin style.
- Themes: System / Material You / Light / Dark + animation toggle, all via `SettingsManager` Flows.
- Trash is soft-delete; secret notes are biometric-gated (`isSecretAuthenticated`).

## Tests & CI

- No required CI before; now `.github/workflows/release.yml` exists (checkout@v*, setup-java@v*, setup-android, assembleRelease, attest, release via `softprops/action-gh-release`).
- Validate with `python -c 'import yaml; yaml.safe_load(...)'` and `./gradlew assembleDebug`.
- Git: `origin` is SSH (`git@github.com:shubh72010/JusNotes.git`), `main` → `nightly` sync, tags `v0.3.0` etc.

## What to Do

- Ground claims in files above; don't invent arch.
- Keep diffs minimal (fewest files, shortest diff) but read the whole flow first — see AGENTS.md ladder.
- For UI work, follow `DESIGN_LANGUAGE.md` (28/6 segmented, no hardcoded colors, spring motion).
- See `ARCHITECTURE.md` for data-flow diagram and module map.

