# JusNotes 📝

> Modern, privacy-focused note-taking for Android — spatial maps instead of folders, AI inside the editor, and a pill-centric UI that feels the same everywhere.

[![Kotlin 2.0](https://img.shields.io/badge/Kotlin-2.0.21-purple?logo=kotlin)](https://kotlinlang.org)
[![Compose BOM 2024.09](https://img.shields.io/badge/Compose_BOM-2024.09.00-blue)](https://developer.android.com/jetpack/compose)
[![Min SDK 24](https://img.shields.io/badge/minSdk-24-green)](https://developer.android.com)
[![License GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/shubh72010/JusNotes/total?label=Downloads&color=success)](https://github.com/shubh72010/JusNotes/releases)

## ✨ Features

- **🗺️ Visual Note Maps** — every Map is a `Note(isMap=true)` with `mapItems`; link notes via `linkedNoteIds` or wiki `[[Note Name]]`. Spatial organization, not folders.
- **🤖 AI Assistant** — OpenRouter (`google/gemini-flash-1.5` default) fixes grammar, retones, bullets↔prose, Q&A, summarize. Key/model stored in `DataStore` (`SettingsManager`), never committed.
- **✍️ Markdown Engine** — custom block renderer: bold/italic/strike, code, tables, quotes, checklists.
- **✅ Checklists** — `isChecklist + checklist: List<ChecklistItem>` with `reorderable 2.4.1` drag-and-drop. `[] ` at start of content auto-converts.
- **🖼️ Images** — `images: List<String>` (uris), Coil `2.5.0`, in-app camera + picker.
- **🔒 Secret Notes** — `isSecret` + Biometric prompt, filtered from main feed, authenticated via `isSecretAuthenticated`.
- **📌 Pinned on Top** — `isPinned` sorted via `sortedWith(compareByDescending isPinned)` after every sort; visible in every tab, also exposed as `pinnedNotes`.
- **🎨 Theming** — System / Material You (dynamic on S+) / Light / Dark, plus global animation toggle (`isAnimationsEnabled`). Edge-to-edge transparent bars (`Theme.kt`, `themes.xml`).
- **🔍 Search & Tags** — debounced `300ms` search across title/content/tags, `selectedTag` chips (`allTags` derived), `SortOrder` 5 ways, `selectedMapId` filtering.
- **🗑️ Trash** — soft-delete `isDeleted + deletedTimestamp`, 30-day auto-purge on `ViewModel.init`, restore/empty/permanent delete.
- **📐 Grid / List** — `viewMode` (`list`/`grid`) persisted in `DataStore`; `LazyVerticalStaggeredGrid(2)` on grid, `LazyColumn` with swipe-to-delete/lock on list.
- **💊 Pill Navigation** — every primary screen (`list`, `editor`, `map`, `settings`, `trash`, `secret`) has the same floating pill (`surfaceContainerHighest`, `28dp↔32dp` spring `damping 0.85, stiffness 300`); tap to expand, drag/swipe to switch tabs, long-press `+` to add.
- **🎓 Visual Tutorial** — 5-step spotlight overlay (`TutorialOverlay`) on first launch (`hasShownGestureHints`), re-openable from Settings → Help.

## 🛠️ Tech Stack

| Layer | Choice |
|-------|--------|
| UI | Jetpack Compose (BOM 2024.09.00) + Material3, Navigation Compose 2.8.5 |
| State | `NoteViewModel` (monolithic, `AndroidViewModel`) owns all `StateFlow`s, no DI |
| DB | Room 2.6.1 + KSP (`AppDatabase` v10, `fallbackToDestructiveMigration`, `exportSchema=false`) |
| Async | Kotlin Coroutines + Flow (`debounce`, `combine`, `flowOn(Default)`) |
| Prefs | DataStore Preferences (`SettingsManager`: `API_KEY`, `MODEL_ID`, `THEME`, `ANIMATIONS_ENABLED`, `STORAGE_URI`, `HAS_SHOWN_GESTURE_HINTS`, `VIEW_MODE`) |
| Network | Retrofit + OkHttp + Gson (`NetworkModule` singleton, `OpenRouterApi`) |
| Media | Coil 2.5.0, Biometric, DocumentFile, `reorderable` |
| Build | Gradle 8.13.2, AGP 8.13.2, Kotlin 2.0.21, JDK 17, Version Catalog `gradle/libs.versions.toml` |

## 📁 Project Layout

```
:app (com.jusdots.jusnotes, minSdk 24, targetSdk 36, compileSdk 36, v0.3.0/11)
├── MainActivity.kt — sole NavHost (routes: list, settings, trash, secret_notes,
│                      editor/{noteId}?initialTab=, image_viewer/{imageUri},
│                      map/{noteId}, tutorial) with spring transitions
├── data/ — Note.kt, NoteDao.kt, AppDatabase.kt, Converters.kt,
│           NoteRepository.kt, SettingsManager.kt
├── ui/ — NoteViewModel.kt, NoteListScreen.kt (+ GridNoteItem), NoteEditorScreen.kt,
│         NoteMapScreen.kt, SettingsScreen.kt, TrashScreen.kt, SecretNotesScreen.kt,
│         TutorialScreen.kt, ImageViewerScreen.kt
│   ├── components/ — TutorialOverlay.kt, BlurText.kt, PinnedNotesBar.kt,
│   │                 MarkdownBlockView.kt, PillControlBar.kt, etc.
│   └── theme/ — Theme.kt, Color.kt, Type.kt
├── network/ — NetworkModule.kt, OpenRouterApi.kt
├── utils/ — BackupUtils.kt, MarkdownRenderer.kt
└── res/values/themes.xml — transparent status/nav, windowBackground
```

## 🚀 Build & Run

```bash
# prerequisites: JDK 17, Android SDK 24+, Android Studio
git clone https://github.com/shubh72010/JusNotes.git
cd JusNotes

# Linux gotcha: gradle.properties hardcodes Windows jbr — override
./gradlew assembleDebug -Porg.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
# or: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 && sed -i '/org.gradle.java.home/d' gradle.properties

./gradlew assembleDebug        # debug APK → app/build/outputs/apk/debug/app-debug.apk (22M)
./gradlew assembleRelease      # release APK (R8 + shrinkResources, signed with debug.keystore)
                               # → app/build/outputs/apk/release/app-release.apk (2.7M)
./gradlew test                 # unit tests
./gradlew connectedAndroidTest # on device/emulator
./gradlew clean
```

APKs are attached to GitHub Releases (`v0.3.0 "Pill & Polish"` etc.). CI (`release.yml`) builds the same on `push to main`.

## 🧭 Usage

1. **Pill** — tap to expand menu (Sort, Grid/List, Settings, tags), drag up/down to expand/collapse, swipe left/right to switch tabs (`NOTES`/`CHECKLISTS`/`MAPS`/`SEARCH`), long-press `+` to add.
2. **Notes** — swipe right → trash, left → lock (secret). Long-press → add to Map.
3. **Editor** — title/content, color, tags (`#tag`), lock/pin, duplicate, AI assist, image, checklist toggle (`[] `), `[[Wiki Link]]`.
4. **Maps** — create Map note, drag items on canvas.
5. **Settings** — Appearance (theme + animation switch), Intelligence (API key/model), Storage (backup folder `/ import .txt/.md/.json`), Vault (trash/secret), Help (re-show tutorial).
6. **Backup** — pick a DocumentTree folder, then Backup now (exports JSON); Import via picker.

## 🤝 Contributing

Branch from `main`, keep Kotlin `official` style (`kotlin.code.style=official`, `nonTransitiveRClass=true`), no formatter/linter enforced. See [CONTRIBUTING.md](CONTRIBUTING.md).

## 🔒 Privacy

All notes stored locally in Room (`note_database`). AI calls are the only network egress, only when you trigger an AI action, using your own OpenRouter key.

## 📄 License

GPL-3.0 — see [LICENSE](LICENSE).

---
Made with ❤️ by [flakesofsmth](https://github.com/shubh72010) · Architecture → [ARCHITECTURE.md](ARCHITECTURE.md) · Design → [DESIGN_LANGUAGE.md](DESIGN_LANGUAGE.md) · Agents → [AGENTS.md](AGENTS.md)
