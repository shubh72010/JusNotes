# DESIGN LANGUAGE — JusNotes

> Pill-centric, Material 3, spring motion. Every screen speaks the same language.

## 1. Pill — the System

The pill is not a button; it is the navigation. Same component on `list`, `editor`, `map`, `settings`, `trash`, `secret`.

- **Surface:** `MaterialTheme.colorScheme.surfaceContainerHighest`, `pillBg` on dark it reads as charcoal.
- **Shape:** `RoundedCornerShape(corner)` where `corner` animates `28dp ↔ 32dp` via `animateDpAsState(spring(damping 0.85, stiffness 300))`. Collapsed `56dp` high, expanded `200–420dp` (list `320dp`, editor `420dp`, trash/secret `200dp`).
- **States:** collapsed (search field + `+` action) → expanded (full menu). `AnimatedContent` / `AnimatedVisibility` for content switch.
- **Gestures:** `detectDragGestures` — drag up → expand, down >80px → collapse; horizontal >100px → switch tab (`NoteTab` ordinal). `combinedClickable` — tap toggles, long-press `+` adds (`createMapNote` on MAPS else `onAddNoteClick`).
- **Handles:** 28×3dp drag handle (`onSurfaceVariant 0.25`), `HorizontalDivider( outlineVariant 0.18)`.

Expanded menu (list example):
- Header: `Menu` (`titleMedium semiBold -0.3sp`) + `notes.size notes • tab • #tag` subtitle.
- Segmented groups: `surfaceContainerLow` cards with `28dp` outer corners, `6dp` inner (`GroupLargeCorner=28`, `GroupSmallCorner=6`). Row: 32dp icon box (`secondaryContainer 0.85`, 10dp radius, 18dp icon) + label + `KeyboardArrowRight 0.35`.
- Tag `FlowRow` + `FilterChip`.

## 2. Color & Elevation

- **Outer:** `background` (window also `background.toArgb()` via `Theme.kt` + `themes.xml` `windowBackground @android:color/black` to kill white flash).
- **Card:** `surfaceContainer` (grid `surfaceContainer`), note tint `color.copy(alpha 0.22–0.24).compositeOver(base)` keeps hue without killing white text. Border `color.copy(0.35–0.38)` + 4dp side indicator (`Color(value or 0xFF000000)`).
- **Elevation:** `CardDefaults 0dp` almost everywhere; only draggable reorder gets `8dp` shadow.
- **Emoji/Accent:** `primary`/`secondaryContainer` for pins/maps, `errorContainer`/`tertiaryContainer` for swipe actions.

## 3. Typography

`MaterialTheme.typography` as-is, tightened where it matters:

- `JusNotes` headline: `headlineLarge ExtraBold -1.2sp`
- Titles: `titleLarge` (note), `titleMedium SemiBold` (grid), `titleMedium ExtraBold` (settings header -0.8sp)
- Meta: `labelSmall 0.6–0.7 alpha`, `bodySmall onSurfaceVariant`, grid lineHeight `16sp`

## 4. Shapes & Spacing

- **Segmented:** `28 / 6` system (outer large, inner small) — identical in `NoteList` (via `segmentedShape(index,count)`), `Settings`, `Grid` (`16dp` for grid cards).
- **Page:** header `8×4dp`, content `horizontal 16, vertical 12`, pills `horizontal 16, vertical 12 + imePadding`, cards `16dp padding` (14dp in grid), `2dp` between notes.
- **Radii:** list card `MaterialTheme.shapes.medium` (segmented), grid `16dp`, tutorial bubble `28dp`, icon boxes `10dp`.

## 5. Motion

- **Pill expand/collapse:** `spring(dampingRatio 0.85, stiffness 300)` on height + corner — feels heavy, not bouncy.
- **Nav transitions:** `slideIntoContainer(Left spring stiffness 300) + fadeIn` / `slideOutOfContainer(Left) + fadeOut` (all routes share the same).
- **Stagger:** `AnimatedWord` (now `BlurText`) `alpha 0→1 350ms LinearOutSlowIn + translationY 12→0 400ms BackOut` (c1=1.70158) — blur removed for performance; used only in editor.
- **List reveal:** swipe threshold `midway 120dp → revealed (0.4→1 alpha)`, `dismiss 250dp`, haptic `LongPress` on snap, background `animateColorAsState` (`errorContainer` vs `tertiaryContainer`, dismissing → `error`/`tertiary`).
- **Skeleton:** 10 × `120dp` (`grid`) / staggered column spacing `12dp`.

No custom lerp beyond `BackOutEasing`; `LinearOutSlowInEasing` for fades.

## 6. Lists & Cards

- **List** (`LazyColumn`, 12dp horizontal, 2dp vertical, 2dp content spacing): `NoteItem` row with 4dp color edge, title `maxLines 2 ellip`, tags `labelSmall primary`, date `bodySmall onSurfaceVariant`, truncated content `3 lines` (was `BlurText`, now `Text`), checklist preview 3 items + `…`.
- **Grid** (`LazyVerticalStaggeredGrid Fixed(2)`, 8dp spacing): same tint but `surfaceContainer`, border `1dp`, `16dp` radius, checklist `8 items + "+ N ticked"`, content `180 chars`.
- **Empty/Loading:** centered `No notes here` / `No results` or 10 skeletons (`surfaceVariant 0.3`).
- **Pinned:** pin icon `16dp primary` (14dp in grid), pinned partition always on top.

## 7. Tutorial

`VisualTutorialOverlay` — 5 steps (`Hub`, `PushPin`, `Swipe`, `TouchApp`, `PanTool`) with spotlight cutout (`BlendMode.Clear` offscreen) on `pill` (56dp) or `notes` (160dp). Dim `Black 0.64`, stroke highlight `White 0.25–0.35`. Bubble `surfaceContainer 28dp`, fixed `92dp` text slot + `72dp` visual slot to prevent jump, dots `6→18dp` spring.

## 8. Component Rules

- **Cards** never use hardcoded `Color.White/Black` — use `MaterialTheme.colorScheme.*` + `alpha` + `compositeOver`.
- **Icons** prefer `Icons.Default.*` but migrate deprecated `Icons.Filled.Sort/KeyboardArrowRight/List` → `AutoMirrored` where Android Lint warns (already flagged as `w:`).
- **Dividers** are `outlineVariant 0.14–0.18` α, not `DividerDefaults`.
- **No extra elevation** unless dragging; no shadows on pills.

## 9. Do / Don't

- Do keep pill behavior identical across screens (same `pillBg`, same spring, same drag thresholds).
- Do use segmented `28/6` for any grouped rows.
- Don't reintroduce per-word `RenderEffect` blur on list screens (was the jank root cause).
- Don't add new navigation that bypasses the pill.
- Don't invent folder UI — the system is Maps (`isMap`).

## 10. References

- Impl: `ui/NoteListScreen.kt:220` (pill), `ui/components/TutorialOverlay.kt:50`, `ui/theme/Theme.kt:60`, `ui/components/BlurText.kt`, `utils/MarkdownRenderer.kt`

