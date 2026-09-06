# Benchmark & profile instructions (Phases 12-13)

## Release measurement (required before claiming wins)
```bash
# Build R8 release (signs with debug keystore locally):
./gradlew :app:assembleRelease -Porg.gradle.java.home=/usr/lib/jvm/temurin-21-jdk
adb install -r app/build/outputs/apk/release/app-release.apk

# Stress dataset generation — run on device once to seed DB with 10/100/500/1000 cases
# (Phase 11): insert via adb + helper test or use app's import of backup_manifest.json fixtures.
# Fixture generator example: app/src/test/ perf helpers — see PerfRegressionTest.kt for content shapes.

# Capture traces (Perfetto):
# Android Studio → Profiler → System Trace 10s while: cold list, scroll, fling, open large md, multi-image, editor→list.
```

## Macrobenchmark (recommended, not yet a module)
Add module once AndroidX benchmark stabilises for AGP 8.13:
```
com.android.test (isDefault=false) + androidx.benchmark:benchmark-macro-junit4
```
Critical scenarios to measure (StartupMode.COLD + FrameTimingMetric):
- `coldLaunch → noteList`
- `scrollNotes` (100/500 items, small/big bodies)
- `openNote` (large Markdown, multi-image)
- `typeEdit → returnToList`

Until module exists, use Perfetto + `dumpsys gfxinfo` as lightweight before/after.

## Baseline Profile
- Stub at `app/src/main/baseline-prof.txt` covers startup→list→scroll→open→markdown→editor→back.
- Proper profile: create `:benchmark` with `BaselineProfileRule` that drives those journeys on release, then
  `./gradlew :benchmark:connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.jusdots.jusnotes.benchmark.BaselineProfileGenerator`
  copies result to `app/src/main/baseline-prof.txt`.
- Re-measure; keep profile only if `startupMs` / `scroll jank` improve (Phase 12 rule: revert non-measurable changes).
