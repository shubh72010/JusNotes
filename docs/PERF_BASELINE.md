# JusNotes — Performance Baseline & Audit (Phases 0–1)

Date: 2026-09-06 | Branch: main | Commit: pre-optimization | Build: release (R8, shrinkResources)

## 0. Measurement methodology (as required)
- **Build assumption:** release/R8 (`isMinifyEnabled`, `isShrinkResources`), per `app/build.gradle.kts:58`.
  Measure on physical device only; debug build has interpreter/JIT/compose metrics skew.
- **Traces:** Compose/Layout + system Perfetto where useful. Macrobenchmark module recommended (not yet present — see §13).
- **What to capture on device:**
  `note-list cold display`, `100/500/1000 scroll`, `fling`, `open large Markdown`, `open multi-image note`, `editor→list return`.

### How to capture (repeatable)
```bash
./gradlew assembleRelease -Porg.gradle.java.home=/path/to/jdk17
adb install -r app/build/outputs/apk/release/app-release.apk
# Perfetto: Record → System Trace → 10s window while exercising flows
# Macrobenchmark (once module exists): ./gradlew :benchmark:connectedCheck
```

## 1. Pipeline audit

```
Room (Note) → Converters (Gson×4 cols) → NoteDao Flows (×3-4 parallel) → NoteRepository
  → ViewModel (allNonDeletedNotes + mapNotes + pinnedNotes + allTags + deletedNotes)
  → notes StateFlow (filter/search/sort, flowOn Default, sortedWith pinned)
  → NoteListScreen (collectAsState ×8) → LazyColumn / LazyVerticalStaggeredGrid
  → NoteItem / GridNoteItem → Text truncation (no Markdown parse, no image load)
  → Editor Markdown preview: MarkdownRenderer.parseBlocks inside remember(content.text)
  → MarkdownBlockView Image: coil AsyncImage fixed 220dp
```

### A. Per-note-in-DB work (unnecessary today)
| Op | Happens? | Cost at 1000 notes |
|---|---|---|
| Markdown parsing (`MarkdownRenderer.parseBlocks`) | **No** on list — list preview is `content.take(100/180)` plain truncation (`NoteListScreen.kt:514,569`). Only editor preview parses. | — |
| Preview generation | `take(100)` alloc per visible card only; still receives full `Note` with full `content` String but trivial CPU. | low per visible, but full `content` Strings held for all 1000 notes in memory |
| Image extraction / decoding | **No** on list — `NoteItem`/`GridNoteItem` never renders `images`. Editor + `MarkdownBlockView.Image` decodes only when note opened/previewed. | — |
| `[[wikilink]]` resolution | Only on `saveNote`/autosave via `resolveWikiLinks` (regex + N title lookups). Not per-list-card. | medium on save |
| `Gson` deserialization | **Yes, worst offender** — `Converters` deserializes `images`, `checklist`, `linkedNoteIds`, `mapItems`, `tags` for **every** note on **every** DB emission, and does `object : TypeToken<T>() {}.type` allocation per call (`Converters.kt:15,27,35,47`). With 4 parallel `Flow<List<Note>>` collectors (`getAllNotes` ×3 + `getPinnedNotes` + `getDeletedNotes`) the conversion runs 3–4×. | O(N) Gson per emission × fan-out |
| DB queries fan-out | `allNonDeletedNotes`, `mapNotes` (derived but via independent `getAllNotes()`), `pinnedNotes` (its own `SELECT … WHERE isPinned=1`), `allTags`, `notes` — 4 collectors of `getAllNotes()` = 4 identical `SELECT * …` cursors + 4× deserialization. N+1 is not present for map linking but `backlinks` is `getAllNotesSnapshot().filter` (full scan). | 4× IO/deser |
| Object construction | `notes` `StateFlow` re-filters/sorts/copies whole list on each emission (`sortedBy…` + `sortedWith pinned` — allocates 2 lists). | O(N log N) per keystroke or DB change |

**Unnecessary:** 4× identical Room queries, per-call TypeToken alloc, full `Note` (with `content` + JSON cols) held for all notes even when only 10 visible.

### B. Per-recomposition work
- `NoteListScreen` observes 8 `StateFlow`s via `collectAsState()`. A change to one note re-emits `allNonDeletedNotes` → `notes` recomputes whole list → `LazyColumn` skips non-dirty items (keys help) but parent `Column` still recomposes.
- Per-list-item: `AnchoredDraggableState` created inside `itemsIndexed` with `remember` + `SideEffect { updateAnchors }` + 2 `LaunchedEffect(state.currentValue)` + `animateColorAsState` + `graphicsLayer` scale + reading `LocalDensity` & `LocalHapticFeedback` per item. `density` is re-read inside lambda (`with(density) {120.dp.toPx()}`) on every composition.
- `NoteItem`/`GridNoteItem`: `remember(note.timestamp)` + `remember(note.color, baseBg)` are fine. But `compositeOver` + `BorderStroke` allocate per item per recomposition if color changes.
- Editor: `MarkdownRenderer.parseBlocks` runs inside `remember(content.text)` on composition thread — expensive regex + recursion for large docs; should be off `Default`.

### C. What is *actually* required for a visible item
- `id`, `title`, trimmed preview (`content.take(180)`), `timestamp`→date, `color`→tint, `tags`, `isPinned/isMap` icons.  **Not** required: full content string, `images`, `mapItems`, decoded bitmaps, Markdown AST, wiki resolution.

## 2. Virtualization (Phase 2)
- ✅ Uses `LazyColumn` (`NoteListScreen.kt:670`) and `LazyVerticalStaggeredGrid` (`657`) with `key = { it.id }`.
- Grid `contentType` missing; list `contentType = { _, _ -> "note" }` is degenerate (single type) — could distinguish `checklist/map/note` for better reuse but pool thrash is minimal at current item count.
- ⚠️ No `0.dp` placeholders, no nested eager lists of full dataset.
- Verdict: virtualization is correct; issue is upstream data fan-out and per-item draggable weight, not eager rendering.

## 3. Viewport-aware gaps (Phases 4-6, 10)
- List is already viewport-cheap: no Markdown, no images per card → Phases 4/6 require **no change** on list path (adding Markdown/image work there would regress). Risk is offscreen work in editor preview and image loading, not list.
- Missing: lightweight preview model (Phase 3), draft-cancel on fling, coil `size`/`crossfade` constraints for `MarkdownBlockView.Image` (currently fixed 220dp, no `size`, no explicit cancellation beyond composition disposal).

## 4. Room efficiency (Phase 9)
- Redundant collectors identified above. Fix: single `getAllNotes()` source → derive `pinnedNotes`/`mapNotes`/`allTags` via `map`.
- `Converters` TypeToken allocation fix is trivial win.
- Lightweight projection (Phase 3) worth evaluating if note count realistically reaches 1000+ with large bodies; keep correctness for search (content must stay searchable). Could use `substr(content,1,200) AS preview` query with fallback full fetch when `searchQuery` non-blank.

## 5. Baseline frame/CPU/Mem (to fill on device)
> Fill after device run (release APK):

| Scenario | Frames / jank | CPU | Mem | DB queries | Notes |
|---|---|---|---|---|---|
| Cold list display (100 notes) | — | — | — | — | |
| Scroll large collection | — | — | — | — | |
| Fling | — | — | — | — | |
| Open large Markdown | — | — | — | — | |
| Multi-image note | — | — | — | — | |
| Editor→list return | — | — | — | — | |

Do not claim wins from debug builds. All Populator benchmarks should use Macrobenchmark on release.

---
*Next: incremental optimizations tracked in commits — see `docs/PERF_BASELINE.md` updates after Phase 12 measurements.*
