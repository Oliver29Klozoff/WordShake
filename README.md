# WordShake

A Boggle-style word game for Android. Shake a tray of lettered dice, then trace
words through touching letters before the clock runs out.

**"WordShake" is a placeholder name.** The rules of Boggle are not protected,
but *Boggle* is a live Hasbro trademark, so the app needs a real name of its own
before it goes anywhere public. (Same situation as Tilerummy.)

## The game

- **Board** — 4x4 classic or 5x5 big, filled by rolling the real Boggle dice.
  The letter distributions are the actual 1983 16-die set and the 25-die Big
  Boggle set, not a weighted random fill; they are tuned so a shaken tray is
  nearly always playable.
- **Words** — trace through dice that touch, including diagonally. No die twice
  in one word. The `Qu` die is one die but counts as two letters.
- **Minimum length** — 3 letters on 4x4, 4 on 5x5, matching the printed rules.
- **Scoring** — the standard table, by letter count:

  | Letters | 3 | 4 | 5 | 6 | 7 | 8+ |
  |---------|---|---|---|---|---|----|
  | Points  | 1 | 1 | 2 | 3 | 5 | 11 |

- **Round** — 2, 3, or 5 minutes. The board stays covered until the clock
  starts, so nobody gets a free scan of it.
- **After the round** — your words, the score a perfect player would have
  posted, and every word you missed, grouped by length.

Best scores are kept per board size and round length.

## Input

Drag across letters to trace a word; lift to submit. Sliding back onto the
previous die retracts a step, so an overshoot is fixable without lifting.

Tapping works too, for anyone who would rather not drag: first tap opens a word,
each further tap extends it, and tapping the last die again submits.

## Dictionary

`app/src/main/assets/words.txt` is the **ENABLE** word list (public domain, and
the usual reference list for word games), filtered to the 170,398 entries that
are 3–16 letters of plain `a`–`z`, and sorted by byte value.

That sort order is load-bearing: `WordDictionary` binary-searches a flat
character buffer rather than building a `HashSet` of 170k strings, which keeps
the cost to about 3 MB of heap and two array allocations. `DictionaryAssetTest`
re-checks the ordering on every build, because a mis-sorted list would fail
silently and at random rather than loudly.

If you regenerate the file, sort it with `LC_ALL=C sort -u` — a locale-aware
sort will not necessarily match.

## Settings

Reachable from the start screen; changes apply immediately and persist.

- **Board** — 4x4 or 5x5. Changing it shakes a new board.
- **Round length** — 1, 2, 3 or 5 minutes.
- **Shortest word** — Auto (3 on 4x4, 4 on 5x5) or a fixed 3, 4 or 5. This
  re-grades the board, since it changes which words were ever there.
- **Show words remaining** — the score bar counts against the board total.
- **Die reach** — how much of a die counts as touching it, from 0.28 to 0.46 of
  a cell. See below; this is the dial for how diagonals feel.
- **Vibrate / Sound** — feedback on accepted and rejected words. Tones are
  generated rather than shipped, so no audio ships in the APK.
- **Theme** — system, light or dark.

Best scores are kept separately per board size, round length and minimum word
length, since all three change how many points are on the table.

## Why the hit region is a square

A drag claims a die once it reaches within *reach* of that die's centre, and
that region is a centred square rather than a circle. The shape matters more
than it sounds.

Diagonal neighbours sit 1.41 cells apart while orthogonal ones sit 1.0, but a
circle reaches the same distance whichever way you go. At the default reach a
circle therefore leaves a dead stretch across about 40% of a diagonal drag
against 16% of a straight one — so diagonals stop responding halfway across,
and approaching a die corner-on (which is what a diagonal does) lands in the
part of the die a circle does not cover. A square leaves the same 16% either
way.

The 0.46 ceiling is load-bearing. At 0.5 the squares of two orthogonal
neighbours reach the line of a diagonal drag, and a stray die starts being
spliced into words again — the v0.2 bug. `DeadZoneTest` re-checks every reach
the slider allows.

The trade is tolerance for a wobbly finger: a drag may stray about
`0.5 - reach` of a cell off the line before the die it passes gets claimed, so
a lower reach is steadier and a higher one is more responsive.

## Layout

```
game/                 no Android dependencies, all unit-tested
  Model.kt            board geometry, the dice, scoring
  Dictionary.kt       the flat-buffer word list
  Solver.kt           exhaustive board search, used to grade the round
  Play.kt             trace rules and word judging
ui/
  GameViewModel.kt    phases, clock, submissions
  BoardView.kt        the tray, the dice, drag tracing
  GameScreen.kt       screens and layout
  Theme.kt            palette
MainActivity.kt
```

The board is square in both orientations, so the layout only decides whether the
panel sits beside it or beneath it — it works in portrait and landscape.

## Build

```sh
./gradlew :app:testDebugUnitTest     # 87 tests
./gradlew :app:assembleDebug
```

`gradle.properties` pins `org.gradle.java.home` to Android Studio's bundled JDK,
because the JDK on PATH is Java 8 and Gradle 9 rejects it.
