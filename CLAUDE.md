# android-app

See `../CLAUDE.md` for the overall exercise context, working mode, and
cross-cutting architecture decisions (modularization, testing philosophy,
interface principle, MVI shape). This file is specific to this sub-project.

## Status

Scaffold builds and runs (empty shell — `MainActivity` shows a placeholder
screen). Movie list screen (State/Intent/Reducer/Processor) not yet
implemented.

## Tech stack

- Kotlin 2.4.10, AGP 9.2.0, Gradle 9.4.1 (see gotchas below for why these
  specific versions matter)
- Jetpack Compose (BOM 2026.03.00), Material 3
- **Koin** for DI (chosen over Hilt specifically because it's
  multiplatform-native — see `../CLAUDE.md` for the reasoning tied to the
  KMP goal)
- Retrofit 3.0.0 + OkHttp + kotlinx.serialization for networking (no Gson)
- Room 2.8.4 (KSP, not kapt) for local persistence
- Navigation Compose 2.9.7

## Toolchain gotchas hit while scaffolding (worth knowing — current as of Aug 2026)

- **AGP 9.0+ has built-in Kotlin support.** The `org.jetbrains.kotlin.android`
  plugin is no longer applied (and errors if you try) — Kotlin compilation
  for Android modules is now part of AGP itself. The old
  `android.kotlinOptions { jvmTarget = ... }` DSL is gone too; jvmTarget now
  just follows `compileOptions.targetCompatibility`. `kotlin.jvm` (for the
  pure-Kotlin `:domain` module), `kotlin-compose`, and `kotlin-serialization`
  are unaffected — this only replaced the Android-specific facade plugin.
- **AGP 9.2.0 requires Gradle 9.4.1+.** (Not 8.x — an assumption from an
  earlier web search turned out wrong; confirmed via the actual Gradle
  error message, which names the exact minimum.)
- **KSP versioning changed.** Older KSP releases were versioned as
  `<kotlin-version>-<ksp-patch>` (e.g. `2.2.10-2.0.2`), tightly coupled to
  one exact Kotlin version. As of KSP 2.3.0 it switched to plain
  independent version numbers (e.g. `2.3.11`) — don't try to construct a
  KSP version string from the Kotlin version anymore; check
  Maven Central directly for the current release.
- **Gradle toolchain auto-provisioning needed explicit setup.** The
  `:domain` module's `jvmToolchain(17)` failed until the
  `org.gradle.toolchains.foojay-resolver-convention` plugin was added to
  `settings.gradle.kts` — without it, Gradle won't download a JDK it
  doesn't find locally. Good practice regardless of this project, since it
  makes the build reproducible on any machine rather than relying on
  whatever JDKs happen to be installed.
- Local machine's default `java` is JDK 8; builds were run with
  `JAVA_HOME` pointed at Android Studio's bundled JBR (JDK 21) instead —
  Android Studio itself will use its own bundled JDK automatically when
  you open the project there.

## Next step

Implement the movie list screen: `MovieListState` / `MovieListIntent` /
`MovieListResult` / reducer (as sketched in conversation), then the
Processor/ViewModel wiring it to a real `MovieRepository` hitting TMDB.
