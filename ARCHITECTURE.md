# ARCHITECTURE — JusNotes

> Single-module Android MVVM, pill-centric Compose, Room as source of truth.

## 1. At a Glance

```
: app  — com.jusdots.jusnotes
         minSdk 24 / compileSdk 36 / targetSdk 36
         versionCode 11 / versionName 0.3.0
         JDK 17 / Kotlin 2.0.21 / AGP 8.13.2
         Compose BOM 2024.09.00 / Navigation 2.8.5 / Room 2.6.1 (KSP)
         Single NavHost in MainActivity.kt
         No DI, no multi-module, no lint/detekt
```

Build files: `settings.gradle.kts` (`include(":app")`), `build.gradle.kts` (top-level plugins), `app/build.gradle.kts` (all deps), `gradle/libs.versions.toml` (catalog), `gradle.properties` (note Windows `java.home` gotcha).

## 2. Modules & Packages

```
app/src/main/java/com/jusdots/jusnotes/
├── MainActivity.kt               NavHost, spring enter/exit, edge-to-edge
├── data/
│   ├── Note.kt                   @Entity(notes) + ChecklistItem + MapItem, indices on timestamp/lastModified/isSecret/isPinned/isDeleted
│   ├── Converters.kt             List<String> ↔ JSON, ChecklistItem/MapItem lists
│   ├── NoteDao.kt                Flow<List<Note>> + suspend getById/title/ids, softDelete/restore, purge, insert
│   ├── AppDatabase.kt            RoomDatabase v10, fallbackToDestructiveMigration, singleton getDatabase()
│   ├── NoteRepository.kt         thin wrapper over Dao (getAllNotes, getPinnedNotes, getDeletedNotes, getNoteById…)
│   └── SettingsManager.kt        DataStore<Preferences> — API_KEY, MODEL_ID, THEME, ANIMATIONS_ENABLED, STORAGE_URI, HAS_SHOWN_GESTURE_HINTS, VIEW_MODE (Flow + save*)
├── ui/
│   ├── NoteViewModel.kt          AndroidViewModel, owns every StateFlow, debounced search, combine filtering/sorting
│   ├── NoteListScreen.kt         LazyColumn (list) vs LazyVerticalStaggeredGrid(2) (grid), swipe actions, segmented cards, NoteItem + GridNoteItem
│   ├── NoteEditorScreen.kt       ~40 remember states, checklist↔text toggle, color/tags/lock/pin/duplicate/AI/image, pill menu
│   ├── NoteMapScreen.kt          map canvas, mapItems (x,y), drag, link
│   ├── SettingsScreen.kt         segmented groups (Vault/Appearance/Intelligence/Storage/Help), inline editors
│   ├── TrashScreen.kt            soft-deleted list, restore/permanent/empty
│   ├── SecretNotesScreen.kt      biometric-gated list
│   ├── TutorialScreen.kt         wrapper over VisualTutorialOverlay
│   ├── ImageViewerScreen.kt      full-screen image
│   ├── components/               TutorialOverlay.kt, BlurText.kt (alpha+slide only), PinnedNotesBar.kt, MarkdownBlockView.kt, PillControlBar.kt, etc.
│   └── theme/                    Theme.kt (dynamic + window bars), Color.kt, Type.kt
├── network/
│   ├── NetworkModule.kt          singleton Retrofit (OkHttp + Gson, OpenRouter base)
│   └── OpenRouterApi.kt          ChatRequest/Message, getCompletion
└── utils/
    ├── BackupUtils.kt            exportNotesToDirectory / importNotesFromUris (DocumentFile, JSON)
    └── MarkdownRenderer.kt       block parsing + rendering
```

Resources: `res/values/themes.xml` (transparent bars, windowBackground black to avoid flash).

## 3. Data Flow

```
Room (Note)  ── NoteDao (Flow) ── NoteRepository ── NoteViewModel ── Compose (collectAsState)
                DataStore (Prefs) ──────────────────┘                     │
                                                                            NavHost (MainActivity)
OpenRouterApi ← NetworkModule ← ViewModel.processAiAction → onResult
BackupUtils ←→ DocumentFile (StorageUri) ← SettingsManager
```

**Single source of truth is Room.** `getAllNotes(): Flow` is mapped/filtered/sorted in `ViewModel`; no SQL sorting beyond `timestamp DESC`.

### NoteViewModel flows (all `stateIn(WhileSubscribed(5000), …)`)

- `debouncedSearchQuery = _searchQuery.debounce(300).distinctUntilChanged`
- `filterCombiner = combine(debouncedQuery, selectedMapId, selectedTab) -> Triple`
- `sortCombiner   = combine(sortOrder, selectedTag) -> Pair`
- `notes = combine(allNonDeletedNotes, filterCombiner, sortCombiner) { filter by tab (pinned stays visible), by query (title/content/tags), by mapId (linkedNoteIds), by tag, exclude isSecret, then baseSorted (DATE_DESC/ASC/TITLE_ASC/DESC/MODIFIED) then sortedWith(isPinned desc) }` — `flowOn(Default)` to keep main free
- `secretNotes / pinnedNotes / deletedNotes / mapNotes / allTags` derived similarly
- `viewMode / theme / apiKey / …` from `SettingsManager` Flows

30-day trash purge in `init { repository.permanentlyDeleteOldTrash(30d ago) }`.

### Note schema (`Note.kt`)

`id PK autoInc, title, content, timestamp idx, lastModified idx def 0, images List<String>, isChecklist, checklist List<ChecklistItem>(id,text,isChecked), isSecret idx, isPinned idx, isDeleted idx def 0, deletedTimestamp?, linkedNoteIds List<Int>, tags List<String>, color Int?, isMap, mapItems List<MapItem>(id,noteId,x,y,color?)`

Indices exist because `ViewModel` filters/sorts in-memory; Dao queries are simple (`isDeleted=0`, `isPinned=1`, etc.).

## 4. Navigation

`MainActivity : FragmentActivity` (needed for Biometric) hosts:

```
NavHost(start = "list") with slideIntoContainer(Left spring) / slideOutOfContainer(Left)
├── "list"                          NoteListScreen (viewModel, onNoteClick, onMapNoteClick, onAddNoteClick, onSettingsClick)
├── "settings"                      SettingsScreen (onBack, onSecretNotesClick, onTrashClick, onTutorialClick)
├── "trash"                         TrashScreen
├── "secret_notes"                  SecretNotesScreen
├── "editor/{noteId}?initialTab="   NoteEditorScreen
├── "image_viewer/{imageUri}"       ImageViewerScreen
├── "map/{noteId}"                  NoteMapScreen
└── "tutorial"                      TutorialScreen
```

All screens share pill layout; editor/map pass `currentNoteId` / `Note`.

## 5. Key Decisions & Constraints

- **No DI** — `ViewModel` manually `AppDatabase.getDatabase(app) + NoteRepository + SettingsManager`. Trivial to test via `FakeDao` but not done.
- **Maps replace folders** — prior folder code removed; grep `isMap`/`linkedNoteIds`/`mapItems` before assuming folder exists. `Note.isMap` + `MapItem` is the canvas.
- **Trash is soft-delete** — `isDeleted` + `deletedTimestamp`; queries exclude deleted; purge is time-based, not count-based.
- **Compose only** — no Views, no XML layouts besides `themes.xml`. `kotlin.code.style=official`, `nonTransitiveRClass=true`.
- **Release signing** — `signingConfigs.release` uses `~/.android/debug.keystore` (so CI can `assembleRelease` without secrets); `isMinifyEnabled + isShrinkResources` with near-empty `proguard-rules.pro` — R8 will strip reflection if you add it, keep rules needed.
- **Experimental APIs** — `ExperimentalFoundationApi / Material3 / LayoutApi` are opt-in via `kotlinOptions.freeCompilerArgs -opt-in=…` so both debug and release compile with `w:` not `e:`.
- **Linux build gotcha** — `gradle.properties` contains `org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr`; on Linux override with `-Porg.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64` or delete the line.
- **One-off scripts** — `patch_note_editor.py`, `fix_note_editor.ps1`, `check_braces.py` are stale folder→map migration helpers; ignore unless touching `NoteEditorScreen.kt`.

## 6. Build & Artifacts

- Debug: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (~22M, not minified)
- Release: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (~2.7M, signed, R8)
- Tests: `./gradlew test` (single trivial unit test), `connectedAndroidTest` needs device
- CI (`release.yml`): JDK 17 (Temurin), `android-actions/setup-android`, Gradle cache via `setup-java`/`setup-gradle`, `assembleRelease` (or `Debug` if no keystore), tests, changelog from `git log`, `softprops/action-gh-release`, `actions/attest-build-provenance`

## 7. External Contracts

- **OpenRouter** — `POST /chat/completions` via `OpenRouterApi.getCompletion` with `Bearer $apiKey`, model `google/gemini-flash-1.5`; prompts are hard-coded in `ViewModel.processAiAction` (Grammar/Formal/Casual/Bullets↔Prose/Q&A).
- **Storage** — `SettingsManager.storageUri` is a `DocumentTree` URI persisted with `takePersistableUriPermission`; `BackupUtils` traverses `DocumentFile` to write/read JSON.
- **Biometric** — `androidx.biometric` for secret notes; `isSecretAuthenticated` gates `secretNotes` access.

## 8. Risks & Next Steps (from CURRENT_STATE.md)

- Editor owns ~40 `remember` states → move to `ViewModel` + `SavedStateHandle` for config-change survival.
- No shared element transitions between list→editor (planned).
- Search lacks highlighting/fuzzy — only `contains(ignoreCase)`.

See also: `README.md` (user-facing), `DESIGN_LANGUAGE.md` (pill system), `AGENTS.md` (agent instructions).
