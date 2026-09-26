# Clock Adventure 3D

A **native Android** learning game that teaches children aged 5–10 how to read analog and
digital clocks. Kotlin + Jetpack Compose + Material 3, MVVM + Clean Architecture, Room +
DataStore, Hilt, Navigation Compose. Portrait first, phones and tablets, English and Arabic (RTL),
and it works **completely offline**.

There is no WebView, no HTML/CSS/JS, no React/Flutter anywhere in the project — every pixel is
drawn by Compose (`Canvas`, `Modifier`, animation APIs) and every screen is a native destination.

---

## Open it in Android Studio

1. **File → Open… → select the `clock-adventure-3d` folder** (this folder is the Gradle root of
   the new project — the rest of the repository is an unrelated older project and is *not* part of
   this build).
2. Use Android Studio **Koala (2024.1.1) or newer** with the Android Gradle Plugin it suggests.
3. Let Gradle sync. The toolchain is pinned in `dependencies.gradle`:

   | | |
   |---|---|
   | compileSdk / targetSdk | 35 |
   | minSdk | 26 (Android 8.0, ~99 % of devices) |
   | Java | 17 |
   | Kotlin | 2.0.21 |
   | Compose BOM | 2024.12.01 |
   | Hilt | 2.52 |
   | Room | 2.6.1 |
   | DataStore | 1.1.1 |
   | Navigation | 2.8.5 |

4. Run → **app** on a device or emulator.

### Build the installable artefacts

```bash
cd clock-adventure-3d
./gradlew :app:assembleDebug          # APK  -> app/build/outputs/apk/debug/
./gradlew :app:assembleRelease        # APK  -> app/build/outputs/apk/release/ (minified)
./gradlew :app:bundleRelease          # AAB  -> app/build/outputs/bundle/release/
./gradlew test                        # unit tests of the domain (clock maths, rewards, …)
```

---

## Module map

```
clock-adventure-3d/
├── domain/        pure Kotlin: models, catalogs, engines (clock maths, questions, grading,
│                  difficulty, rewards, formatting), repository interfaces, use cases
├── data/          Room database + DAOs + DataStore settings + repository implementations + Hilt DI
├── presentation/  Compose UI: theme, components, the interactive clock, all screens, navigation
└── app/           the Android application: Hilt app class, MainActivity, audio, notifications
```

Dependencies point inwards only: `app → presentation → domain ← data`. `domain` has no Android
dependency at all, which is why its rules are unit-testable on the JVM.

---

## What the game does

### Ten progressive levels
1. Clock numbers & the two hands · 2. Full hours · 3. Half hours · 4. Quarter past / to ·
5. Five-minute steps · 6. Random analog times · 7. Analog → digital · 8. Set the clock from
digital · 9. Real-life challenges (“school starts at 7:30”) · 10. Advanced timed challenges.

Each level opens with a short mascot explanation and a free-play clock, then asks its questions,
and finishes with a 3-star result screen.

### A genuinely interactive clock
`presentation/…/clock/AnalogClock.kt` is a `Canvas`-drawn clock with a bevelled bezel, gradient
face, ticks, glass highlight and hand shadows (the “3D” look, no image assets).

* drag the long hand → minutes, snapped to the level granularity (60/30/15/5/1 minutes);
* drag the short hand → the hour;
* the hour hand follows the minute hand automatically, exactly like a real clock;
* wrong answer → the hand that needs attention is highlighted, “show me” draws the correct hands
  as translucent ghosts;
* correct answer → the clock pops and confetti flies;
* the digital time is shown as a helper and hidden during tests.

### A first-run introduction
The very first start opens a four page onboarding: the mascot says hello, the child picks the
character they like, drags both hands of a free-play clock, sees what stars and coins are for, and
hears that the game needs no account and no internet. It is shown once - `hasSeenIntro` is stored
in DataStore (and `intro_seen` in the profile), so it never comes back.

### Six mini games
What Time Is It? · Set The Clock · Match The Time · Time Race (60 s) · True or False ·
My Daily Routine — plus four optional challenges (Daily, Time Attack, Perfect Run, Boss Clock).

### Adaptive difficulty
Easy / Medium / Hard, or automatic: two wrong answers in a row step down and switch hints on,
three correct answers in a row step up. The step size of the minute hand is tied to the level.

### Rewards
Stars, coins, XP, player levels, achievements, badges, a daily gift with a day streak, and a shop
where coins and stars unlock new clock faces, themes and mascot characters. Everything is stored
in Room, so it survives a restart.

### Sound and voice
Button clicks, correct/incorrect, celebration and coin sounds are generated at run time
(`app/…/audio/ToneGenerator.kt`) — no audio files are shipped. The background loop is generated
too. The time is spoken by the on-device TextToSpeech engine in English or Arabic. Music, sound
effects and voice can each be switched off in Settings.

### Parent area
Behind a little multiplication gate so a child cannot reach it by accident. It shows today’s and
total learning time, accuracy, finished lessons, strong topics, topics that need practise, a
7-day bar chart, a daily time limit (off / 15 / 20 / 30 / 45 / 60 minutes) and a reset button.
Strong and weak topics are named in the language the app is set to, and every weak topic has a
**Practise** button that drops straight into that lesson.

### When the daily limit is reached
The limit is never enforced by throwing the child out. A session that runs into the limit is
**paused** behind a friendly break dialog ("stretch, drink some water") with two choices: stop for
today, or ask a grown-up. The grown-up route asks one multiplication and, when it is solved, grants
15 extra minutes and rewinds the timers — the pause itself is not counted as play time. The maths
lives in the domain (`CreateParentGateQuestionUseCase`), the same use case the parent gate uses, so
both gates can always be solved.

### Big screens and accessibility
Every screen sits in a `ContentColumn` that stops at 560 dp and stays centred, so on a tablet or a
foldable the buttons keep their phone size instead of stretching into ribbons. The clock, the
mascot and the hud are laid out from fractions of the available width, and the clock itself is
capped at 300 dp. The analog clock publishes a TalkBack description of the time it is showing, so
it is never a silent picture.

### Child safety & privacy
* no account, no sign-in, no personal data collected, nothing uploaded;
* **no internet permission and no network code at all** — the whole app is offline;
* no advertising SDK, no analytics, no behavioural tracking;
* no external links anywhere in the children’s area;
* the only notification is an optional, device-local daily reminder that can be turned off.

---

## Persistence

| Store | Contains |
|---|---|
| Room `clock_adventure.db` | user profile (xp, coins, stars, level, counters, streak, last daily gift), per-lesson progress, achievements, unlocks, daily statistics (seconds, answers, sessions), best mini-game scores |
| DataStore `clock_adventure_settings` | language, difficulty mode, music / sound / voice, 12 h / 24 h, hints, reduce motion, notifications, clock style, theme, character, daily limit |

Both are included in Android backup rules, and every write goes through the repositories, so the
UI never touches SQL or DataStore directly.

---

## Tests

| Where | What | How to run |
|---|---|---|
| `domain/src/test` | 11 test classes, 74 tests: clock geometry, question generation and its invariants, grading, adaptive difficulty, reward maths, time formatting, achievement evaluation, the parent-gate maths, the parent topic stats, and the level / game / challenge catalogues | `./gradlew :domain:test` |
| `presentation/src/androidTest` | instrumented tests of the interactive clock: the clock is on screen, a drag reports a new time, the minutes obey the snap granularity of the level, a read-only clock ignores touches | `./gradlew :presentation:connectedAndroidTest` (needs a device) |

## Verification in this repository

The sandbox that produced this project has no Android SDK and no access to Google’s Maven
repository, so `./gradlew assembleDebug` could not be executed here. Instead every source set is
compiled with a real Kotlin compiler against a generated stub library that declares the third-party
APIs the app uses, which catches everything a compiler can catch without the real artifacts:
syntax, unresolved references, argument counts, nullability, generics and exhaustiveness.

```bash
cd tools/clock-adventure-typecheck
python3 build_stubs.py          # regenerates stubs/ and the R classes from the real res/ folders
python3 check.py                # domain · data · presentation · app · tests · androidTest
python3 check.py domain         # or a single module

python3 run_tests.py            # actually RUNS the domain unit tests (65 passed, 0 failed)
python3 audit.py                # build readiness: dependencies, resources, Room SQL
    audit_deps.py  every Kotlin import has a matching Gradle dependency; AGP config is valid
    audit_res.py   every resource XML parses; every @string/@drawable/@mipmap/... reference resolves
    audit_room.py  every table and column used in a Room @Query exists on its @Entity
```

Maven cannot be reached from this sandbox, so `run_tests.py` compiles the domain against a tiny
JUnit replacement (`testrt/junit-runtime.kt`) whose assertions really check, and runs every
`@Test` method reflectively. `check.py` stays a pure type check.

Current state:

* **all six source sets type-check clean** (domain, data, presentation, app, domain tests,
  instrumented tests) - 528 files compiled per module pass;
* **65 domain unit tests pass, 0 fail**;
* **all three audits pass**.

Open the project in Android Studio for the Gradle build and the APK/AAB.
