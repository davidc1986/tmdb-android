# android-app

See `../CLAUDE.md` for the overall exercise context, working mode, and
cross-cutting architecture decisions (modularization, testing philosophy,
interface principle, MVI shape). This file is specific to this sub-project.

## Status

End-to-end movie list and movie detail features are wired and build
successfully — TMDB data loading through the full chain:
`MovieListState`/`Intent`/`Result`/`Effect` + pure reducer (tested), data
layer (`MovieApi`/`TmdbRemoteDataSource`/`MovieRepository`, errors as a
sealed result type not exceptions) via Koin's `dataModule`,
`MovieListViewModel`/`MovieDetailViewModel` via `presentationModule`,
rendering through `MovieListScreen`/`MovieDetailScreen` in a `NavHost`
inside `MainActivity`.

`Search` intent deliberately left out until its state interaction is
designed.

## Detail screen

Reached by tapping a row in the movie list. Follows the same MVI shape as
the list screen: `MovieDetailState`/`Intent`/`Result`/`Effect` +
`reduceMovieDetailState` (pure, tested), `MovieDetailViewModel`,
`MovieDetailScreen`.

- **Navigation is modeled as an effect**, same pattern as the list
  screen's Snackbar: `MovieListIntent.SelectMovie` → `MovieListViewModel`
  emits `MovieListEffect.NavigateToDetail(movieId)` →
  `MovieListScreen` collects it and calls an `onMovieClick: (Int) -> Unit`
  lambda, decoupling the screen from `NavController` directly. The actual
  `NavHost` (`MoviesNavHost.kt`) lives in `:app`, with two routes
  (`movieList`, `movieDetail/{movieId}`), per the module dependency graph
  (`:app` owns nav host wiring).
- **`MovieDetail` is a separate domain model from `Movie`** (`id, title,
  overview, posterPath, releaseDate, voteAverage, isFavorite, genres,
  runtimeMinutes, tagline`) fetched from TMDB's `/movie/{movie_id}`
  endpoint, which returns richer data than the popular-list endpoint. Kept
  separate rather than adding nullable genre/runtime/tagline fields to
  `Movie`, since the list endpoint can never populate them.
- `MovieDetailViewModel` takes the movie ID via Koin's `parametersOf` at
  construction (`viewModel { (movieId: Int) -> ... }`) and loads in
  `init`, same `loadJob` cancel/restart pattern as the list screen's
  `load()`.
- No effect channel on the detail screen (unlike the list) — there's no
  refresh-over-stale-content case here, so a Snackbar-style effect would
  be unused.

## Favorites

Implemented — the first real `MovieRepository` orchestration.

- **`Movie.isFavorite: Boolean`** added to the domain model. Decided
  this belongs on `Movie` itself rather than as separately-combined
  state (unlike the earlier hypothetical auth-session discussion) —
  "is this movie favorited" is a per-entity fact meaningful only in the
  context of movie data, not genuinely cross-cutting the way session
  state is.
- **Room stores only favorited movie IDs** (`FavoriteMovieEntity`,
  `FavoriteDao`, `MoviesDatabase`, all in `data/local/`) — no dedicated
  "browse your favorites" screen exists yet, so there's nothing that
  needs a full local snapshot of favorited movies' data. Reconsider if
  that screen gets built.
- **`MovieRepository.observePopularMovies()` is reactive** — a `Flow`,
  not a one-shot `suspend fun` — fetches remote data once, then
  `emitAll`s a `Flow` that combines it with `favoriteDao.observeFavoriteIds()`,
  re-merging on every local favorite change with **no new network
  call**. This was the actual trigger for restructuring
  `MovieListViewModel` from "one suspend call per Intent" to "collect a
  long-lived Flow" — toggling a favorite needed to update the rendered
  list immediately, which a one-shot repository call couldn't do.
- **`MovieListViewModel`** cancels/restarts the collecting job on
  Load/Refresh/Retry (`loadJob`) so intents don't stack collectors.
  `ToggleFavorite` just calls `repository.toggleFavorite(movieId, isFavorite)` — it
  doesn't touch `_state` itself; the already-running collector picks up
  Room's emission automatically.
- **Considered and rejected: Realm** as a way to sidestep Room's
  `Context` requirement in JVM tests (Realm's own native storage engine
  doesn't need one, unlike Room which sits on Android's SQLite). Ruled
  out: MongoDB deprecated Atlas Device Sync + Realm SDKs in Sept 2024
  (migration deadline Sept 2025) — the core DB lives on as
  community-maintained open source, but recommending it for a project
  about *current* practice would be bad advice regardless of the
  testability upside.
- **Robolectric introduced instead**, solely to get a real `Context` for
  Room's in-memory database builder in `MovieListViewModelTest` — no
  other Robolectric capability is used. Worked cleanly on the first
  attempt (worth noting since it has a dated reputation for being slow/
  flaky — that was much more true a decade-plus ago than now).
- New test: `toggling favorite reactively updates the list with no new
  network call` — asserts `server.requestCount == 1` after a toggle,
  which is the actual proof the reactive design works as intended.

`MovieListEffect` (Snackbar on refresh failure) is now actually
consumed — `MovieListScreen` collects it via `LaunchedEffect` into a
`SnackbarHost`, previously it was only emitted, never collected.

The sociable ViewModel/Repository test promised in the testing
philosophy is written: `MovieListViewModelTest` (`presentation/src/test/`)
fakes only MockWebServer (via the `mockwebserver3` artifact — OkHttp 5.x
renamed/restructured this from the old `okhttp3.mockwebserver` package);
everything else (OkHttp, Retrofit, the serialization converter,
`TmdbRemoteDataSource`, `MovieRepository`, `MovieListViewModel`) is real,
using Turbine to assert on `StateFlow` emissions. Needed
`unitTests.isReturnDefaultValues = true` in both `:data` and
`:presentation` — `Log.w` inside `TmdbRemoteDataSource` isn't mocked in a
plain JVM unit test and throws by default, which was silently crashing
the coroutine before it reached the `Error` state (surfaced as a Turbine
timeout, not an obvious stack trace pointing at `Log`).

## Architecture: repository/data-source interfaces, extension-function mappers, UI models

- **`MovieRepository` is an interface in `:domain`**, implemented by
  `MovieRepositoryImpl` in `:data`. Its methods
  (`observePopularMovies`, `observeMovieDetail`, `toggleFavorite`) are
  business-meaningful operations with no mention of "remote" or "local"
  anywhere — that split is `:data`'s own implementation strategy for
  satisfying the contract, not something `:domain` needs to know exists.
- **`MovieRemoteDataSource`/`MovieLocalDataSource` are interfaces inside
  `:data`** (`data/remote/`, `data/local/`), implemented by
  `TmdbRemoteDataSource` and `RoomMovieLocalDataSource` respectively.
  `MovieRepositoryImpl` composes the two. `RoomMovieLocalDataSource` is
  the only class that touches `FavoriteDao`/`FavoriteMovieEntity`
  directly — `MovieRepositoryImpl` only ever sees the interface.
- **`Outcome<T>`** (`domain/Outcome.kt`) is the sealed result type
  (`Success` / `Error.NetworkUnavailable` / `Error.RemoteFailure` /
  `Error.Unknown`), living in `:domain` since it's part of the repository
  interface's signature.
- **Mapping is done via extension functions**, each in its own file:
  `MovieDto.toMovie()` / `MovieDetailDto.toMovieDetail()`
  (`data/remote/MovieMapper.kt` / `MovieDetailMapper.kt`),
  `Movie.toMovieListItemUiModel()` / `MovieDetail.toMovieDetailUiModel()`
  (`presentation/movielist/MovieUiMapper.kt` /
  `presentation/moviedetail/MovieDetailUiMapper.kt`). Each has its own
  pure unit test (`MovieMapperTest`, `MovieDetailMapperTest`,
  `MovieUiMapperTest`, `MovieDetailUiMapperTest`).
- **`:presentation` has separate UI models** (`MovieListItemUiModel`,
  `MovieDetailUiModel`) distinct from the `:domain` models, holding
  precomputed display fields (`displayYear`, `subtitle` combining year +
  runtime, `genresText`) rather than the Compose screen calling
  formatting logic on the domain model directly.
- **`:presentation`'s production code depends only on `:domain`** — no
  compile-time dependency on `:data` (`presentation/build.gradle.kts` has
  `testImplementation(project(":data"))`, not `implementation`). Tests
  still build the full real stack (`TmdbRemoteDataSource`,
  `MovieRepositoryImpl`, `RoomMovieLocalDataSource`) against
  MockWebServer/in-memory Room, since that's the one place
  `:presentation` is allowed to know `:data`'s concrete types exist.

## Error handling: sealed `Outcome`, not exceptions

`MovieRepository`/`TmdbRemoteDataSource` return `Outcome<T>` (`domain/Outcome.kt`)
rather than throwing. `TmdbRemoteDataSource` is the only place that
catches `IOException`/`HttpException` and translates them into
`Outcome.Error` — the same boundary that already owns DTO mapping, for
the same reason (it's the one place that's supposed to know Retrofit/
TMDB specifics exist).

**Why, not just what:** `MovieListViewModel` originally caught
`IOException`/`HttpException` directly, which forced `:presentation` to
depend on Retrofit (a real dependency leak) and meant swapping the
backend later could silently break ViewModel error handling if the new
implementation fails differently — exactly the coupling the
`RemoteDataSource` seam was meant to prevent.

Kotlin has no checked exceptions, so nothing forces a caller to know
what a function can throw — you're relying on docs/convention, which is
easy to get wrong. A sealed return type moves failure handling into
`when` exhaustiveness, which *is* compiler-enforced: adding a new
`Outcome.Error` case breaks every `when` that doesn't handle it,
project-wide. Preferred generally, going forward — not just for this repository.

**Follow-up refinement:** `Error.ServerError(code: Int)` originally
carried the raw HTTP status code, which is itself a leak one level
subtler than the exception types — a protocol detail riding inside a
type that's supposed to be transport-agnostic. Collapsed to a plain
`Error.RemoteFailure` (no fields); the code is now only `Log.w`'d at the
point of translation in `TmdbRemoteDataSource`, for debugging, never
exposed upward. `NetworkUnavailable` stayed as-is — connectivity is
closer to a device-level fact than a wire-protocol detail, and it's a
real UX distinction (retry vs. "check your connection") that can't
fully be eliminated, only kept at the category level rather than as raw
protocol values.


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

## Known future refactor: `:presentation` bundles two concerns

`:presentation` currently holds both **presentation logic** (State/Intent/
Result/Reducer, eventually the ViewModel — plain Kotlin, zero Android
dependencies) and **presentation rendering** (the Compose `@Composable`
screens — genuinely Android/Compose-specific). They're co-located for now
because we're not doing KMP yet, which is the right call for this phase.

When the roadmap reaches "extract shared ViewModel/business logic into
KMP," presentation *logic* (reducer, state, intent, ViewModel) is the
expected candidate to move into `kmp-shared/commonMain` largely
unchanged, while presentation *rendering* stays behind and gets rewritten
per platform. Not an action item now — just a heads-up for when that
phase starts, so the split isn't rediscovered from scratch.

## Next step

`Search` intent design — state interaction not yet worked out.

Possible follow-up, not scheduled: a dedicated `FavoriteDao` test (real
in-memory Room DB via Robolectric) to verify actual SQL correctness —
`MovieListViewModelTest`/`MovieDetailViewModelTest` verify the
ViewModel/repository's reaction to Room, not the DAO's query correctness
in isolation.
