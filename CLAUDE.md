# WordShake — agent notes

A Boggle-style word game, Kotlin + Compose, package `com.mj.wordshake`.

- **"WordShake" is still a placeholder name.** Boggle's rules aren't
  protected, but *Boggle* is a live Hasbro trademark, so this needs a
  trademark-safe rename before it goes anywhere public. Check the README
  and repo name for a real name before assuming this is still open.

- **Release signing**: key at `C:\Users\Emjay\.android\wordshake-release.keystore`,
  alias `wordshake`, password in this repo's untracked `local.properties`
  (`RELEASE_STORE_PASSWORD` / `RELEASE_KEY_PASSWORD`). Both the keystore file
  and that password should be backed up — losing either permanently blocks
  updates to any copy already installed from a signed release. Without
  `local.properties`, the release build silently falls back to the debug
  keystore (see `app/build.gradle.kts`), which produces an APK that cannot
  update an existing install.

- **In-app updater** (`update/UpdateChecker.kt`, since v0.5): reads
  `https://api.github.com/repos/Oliver29Klozoff/WordShake/releases/latest`
  directly. The git tag *is* the version (compared via `isNewer`, which
  strips a leading `v`) and the *first* asset whose name ends in `.apk` is
  the download — there's no `version.json`/checksum to keep in sync. Cut
  releases with `gh release create <tag> WordShake.apk` (the filename is
  fixed by convention; `gh`'s `file#label` syntax does not actually rename
  the uploaded asset). **`versionName` in `app/build.gradle.kts` must be
  bumped to match the tag**, or the updater will never think a new release
  is newer.

- **`app/src/main/assets/words.txt`** is the ENABLE word list filtered to
  ~170k words (3–16 letters, `a`-`z`), and MUST stay byte-sorted
  (`LC_ALL=C sort -u` — a locale-aware sort will not match) because
  `WordDictionary` binary-searches the flat buffer directly instead of using
  a `HashSet`. A mis-sorted list breaks lookups silently and at random.
  `DictionaryAssetTest` re-checks strict byte order (and several other
  invariants) every build — keep it passing rather than relaxing it.

- **Dice distributions** in `game/Model.kt` are the real 1983 16-die and
  25-die Big Boggle sets. The `HIMNQuU` die is 6 distinct faces — H, I, M,
  N, `Qu`, and a separate U — not 5; `Dice.die()` parses `Qu` as one token
  via regex, so don't "simplify" that die string down to 5 letters.

- **Not orientation-locked.** The board is square; `GameScreen.kt` only
  decides whether the side panel sits beside it (landscape) or beneath it
  (portrait). No `screenOrientation` lock in the manifest — don't add one.

- **Toolchain**: AGP 8.13.2, Kotlin 2.0.21, Gradle 9.4.1. `gradle.properties`
  pins `org.gradle.java.home` to Android Studio's bundled JDK because the
  JDK on PATH is Java 8 and Gradle 9 rejects it. Build/test with
  `./gradlew :app:testDebugUnitTest`.

- **Test device**: the `Resizable_Experimental` emulator only — never a
  physical device. Its SystemUI throws spurious "System UI isn't
  responding" dialogs (ignore them), and its `screencap` output has a
  warning-line prefix before the PNG bytes that must be stripped before
  the capture is valid.
