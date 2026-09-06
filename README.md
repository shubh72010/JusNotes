<p align="center">
  <img src="app/src/main/assets/JusNotes.svg" width="96" height="96" alt="JusNotes" />
</p>

<h1 align="center">JusNotes</h1>
<p align="center"><em>Modern, privacy-focused notes for Android — spatial maps, markdown, checklists & AI, wrapped in a pill.</em></p>

<p align="center">
  <a href="https://github.com/shubh72010/DotNotes/releases"><img src="https://img.shields.io/github/v/release/shubh72010/DotNotes?label=release&color=111111&style=flat-square" alt="release" /></a>
  <a href="https://github.com/shubh72010/DotNotes/releases"><img src="https://img.shields.io/github/downloads/shubh72010/DotNotes/total?label=downloads&color=0ea5e9&style=flat-square" alt="downloads" /></a>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7f52ff?style=flat-square&logo=kotlin&logoColor=white" alt="kotlin" />
  <img src="https://img.shields.io/badge/Compose_BOM-2024.09.00-4285f4?style=flat-square" alt="compose" />
  <img src="https://img.shields.io/badge/minSdk-24-00c853?style=flat-square" alt="minsdk" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-111111?style=flat-square" alt="license" /></a>
</p>

<p align="center">
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-features">Features</a> •
  <a href="#-pill-philosophy">Pill</a> •
  <a href="#-build--run">Build</a> •
  <a href="ARCHITECTURE.md">Architecture</a> •
  <a href="DESIGN_LANGUAGE.md">Design</a>
</p>

---

### ✨ Screenshots

<table>
  <tr>
    <td align="center"><img src="app/src/main/assets/Screenshot_20260906-194319.png" width="200" alt="Editor" /><br/><sub><b>Editor</b> — markdown + pill toolbar</sub></td>
    <td align="center"><img src="app/src/main/assets/Screenshot_20260906-194331.png" width="200" alt="Note actions" /><br/><sub><b>Pill menu</b> — Color / Tags / Preview / Lock / Pin</sub></td>
    <td align="center"><img src="app/src/main/assets/Screenshot_20260906-194348.png" width="200" alt="Preview" /><br/><sub><b>Preview</b> — long form, smooth scroll</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="app/src/main/assets/Screenshot_20260906-194738.png" width="200" alt="AI Assist" /><br/><sub><b>AI Assist</b> — Summary / Grammar / Rewrite</sub></td>
    <td align="center"><img src="app/src/main/assets/Screenshot_20260906-194400.png" width="200" alt="Settings" /><br/><sub><b>Settings</b> — Material You + Vault + Backup</sub></td>
    <td align="center"><img src="app/src/main/assets/Normal.png" width="120" alt="Icon" /><br/><sub><b>Icon</b> — light & monochrome variants</sub></td>
  </tr>
</table>

> Tip: all screens share the same floating **Pill** — tap to expand, drag to switch tabs, long-press **+** to create.

---

### 🌟 Features

| Area | What you get |
|---|---|
| 🗺️ **Maps, not folders** | `Note(isMap=true)` + `mapItems` + `linkedNoteIds` / `[[Wiki]]`. Spatial canvas with drag. |
| ✅ **Checklists** | `isChecklist + List<ChecklistItem>` with `reorderable 2.4.1`. Type `[] ` to convert. Now visible on **homescreen (NOTES)** and filtered in **CHECKLISTS** tab. |
| ✍️ **Markdown** | Block renderer — headings, bold/italic/strike, code, tables, quotes, checklists, `![image](uri)`. Off-main-thread parsing for large docs. |
| 🖼️ **Images** | `images: List<String>` uris, Coil 2.5.0 with `size(800)` — viewport-aware decode. Camera + picker → `![alt](content://…)` embed. |
| 🤖 **AI** | OpenRouter (`minimax/m3:free` etc, your key only). Polish / Rewrite / Structure — never stored, `DataStore` only. |
| 🔒 **Secret** | `isSecret` + Biometric, hidden from main feed, `isSecretAuthenticated` gate. |
| 📌 **Pinned** | `isPinned` always on top (`sortedWith` stable partition) — every tab. |
| 🔍 **Search & Tags** | Debounced 300ms on title/content/tags, tag chips (`allTags`), 5 sort orders, `selectedMapId` filter. |
| 🗑️ **Trash** | Soft delete `isDeleted + deletedTimestamp`, 30-day auto-purge on `ViewModel.init`. |
| 📐 **Grid / List** | `viewMode` persisted in `DataStore`; `LazyVerticalStaggeredGrid(2)` + `LazyColumn` with swipe-to-delete/lock. |
| 🎓 **Tutorial** | 5-step spotlight (`hasShownGestureHints`), reopen from Settings. |

### 💊 Pill Philosophy

Every primary destination — **Notes / Editor / Map / Settings / Trash / Secret** — uses the same floating pill (`surfaceContainerHighest`, `28dp↔32dp`, `spring(damping 0.85, stiffness 300)`). It is the only chrome: search on the list, actions on the editor. Swipe the pill to switch tabs, drag up to expand, long-press the `+` to add anywhere. Small API, big consistency.

---

### 🛠️ Stack

| Layer | Choice |
|---|---|
| UI | Compose BOM 2024.09.00 + Material 3, Navigation Compose 2.8.5 |
| State | `NoteViewModel` (`AndroidViewModel`, all `StateFlow`s, no DI) |
| DB | Room 2.6.1 + KSP (`AppDatabase` v10, `fallbackToDestructiveMigration`) — single-source `getAllNotes()` + lightweight `NoteListItem` preview projection |
| Async | Coroutines + Flow (`debounce`, `combine`, `flowOn(Default)`, `produceState` for markdown) |
| Prefs | DataStore (`SettingsManager`) |
| Network | Retrofit + OkHttp + Gson, Coil 2.5.0, Biometric, DocumentFile |
| Build | Gradle 8.13.2, AGP 8.13.2, Kotlin 2.0.21, JDK 21 (CI), version catalog |

---

### 🚀 Build & Run

```bash
git clone https://github.com/shubh72010/DotNotes.git
cd DotNotes

# Linux gotcha — gradle.properties hardcodes Windows jbr, override:
./gradlew assembleDebug -Porg.gradle.java.home=/usr/lib/jvm/temurin-21-jdk
# or: sed -i '/org.gradle.java.home/d' gradle.properties

./gradlew assembleDebug        # → app/build/outputs/apk/debug/app-debug.apk (~22M)
./gradlew assembleRelease      # R8 + shrinkResources → app/build/outputs/apk/release/app-release.apk (~2.7M)
./gradlew test                 # includes PerfRegressionTest
./gradlew connectedAndroidTest # device/emulator
```

Releases are attached to GitHub Releases — CI (`release.yml`) builds `assembleRelease` on `push to main`.

---

### 📁 Layout

```
:app (com.jusdots.jusnotes, minSdk 24, target 36, v0.3.1/12)
├── MainActivity.kt — NavHost (list, editor/{id}, map/{id}, settings, trash, secret, tutorial, image_viewer)
├── data/ — Note.kt, NoteListItem.kt, NoteDao.kt, AppDatabase.kt, Converters.kt, NoteRepository.kt, SettingsManager.kt
├── ui/ — NoteViewModel.kt, NoteListScreen.kt (+ GridNoteItem + SwipeableNoteCard), NoteEditorScreen.kt, ...
│   └── components/ — MarkdownBlockView.kt, TutorialOverlay.kt, BlurText.kt, PinnedNotesBar.kt, ...
├── network/ — NetworkModule.kt, OpenRouterApi.kt
├── utils/ — BackupUtils.kt, MarkdownRenderer.kt
└── res/drawable/jusnotes_logo.xml — pill-cross logo
```

Deep dive: [ARCHITECTURE.md](ARCHITECTURE.md) · [DESIGN_LANGUAGE.md](DESIGN_LANGUAGE.md) · [AGENTS.md](AGENTS.md) · [CURRENT_STATE.md](CURRENT_STATE.md)

---

### 🔒 Privacy

Local-first. Room (`note_database`) is the source of truth. The only network egress is an OpenRouter call when **you** tap an AI action, with **your** key. No telemetry, no Firebase, no crashlytics.

### 📄 License

GPL-3.0 — see [LICENSE](LICENSE).

---

<p align="center"><sub>Made with ❤️ by <a href="https://github.com/shubh72010">flakesofsmth</a> · Star if you like the pill.</sub></p>
