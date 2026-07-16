# Ekte Apps

A suite of Android apps with a strict **“no-bullshit” philosophy** — honest utility and lifestyle
apps (and later more technical tools), all under the **“Ekte &lt;Name&gt;”** naming scheme.

> This folder is the **suite root**. Each app lives in its own self-contained subdirectory and is a
> complete, independent Gradle project — you can open, build, and release each one on its own.

## The charter — every app must adhere to this

1. **Application id** follows `com.ektebrysjan.<shortappname>` (Ekte Steps → `com.ektebrysjan.steps`).
2. **100% free** — no ads, no tracking, no analytics SDKs.
3. **Local-only data storage** — no cloud sync, no account system.
4. **Privacy-first** — minimal permissions, no unnecessary network access (prefer **no `INTERNET`
   permission** at all, so the app physically cannot exfiltrate data).
5. **Simple dark-mode UI by default** — clean, minimalist, low-friction. One dark theme; no
   Material You / dynamic colour, so the look is consistent on every device.
6. **Open assets only** — freely available icon sets and UI resources (e.g. Material Icons,
   other open-source resources).
7. **Straightforward functionality** — no gamification, no engagement/retention tricks.
8. **No gratuitous notifications** — don't post notifications unless they're genuinely core to the
   app concept (e.g. a calendar's event reminders). When background work doesn't need to alert the
   user (like step counting), use silent mechanisms such as WorkManager instead of a
   notification-backed foreground service.
9. **In-app header convention** — every app's top bar shows the **short app name** as the main
   title (e.g. `Steps`), with **“an Ekte App by EkteBrysjan”** in a smaller font directly beneath.

Naming: user-facing name is **“Ekte &lt;Name&gt;”** (Ekte Steps, Ekte Notes, Ekte Todo, Ekte
Habits, …). Roadmap: utility/lifestyle apps first (step counter, notes, todo, habits, trackers),
later technical tools (WiFi scanner, decibel meter, IT/AV utilities).

## Apps

| Directory | Name | Application id | Description |
|-----------|------|----------------|-------------|
| [`steps/`](steps/) | **Ekte Steps** | `com.ektebrysjan.steps` | Pedometer: today's step count, the previous 6 days, a statistics page with local history, and a clear-history button. Counts in the background. |
| [`notes/`](notes/) | **Ekte Notes** | `com.ektebrysjan.notes` | Note-taking: create/edit/delete notes, comma-separated tags with filtering, search, optional PIN-locked private notes, and manual export/import. Zero permissions, fully offline. |

*(New apps get added as new rows here.)*

## Layout of each app

Each app subdirectory is a standard, standalone Android project:

```
<app>/
├── gradlew, gradlew.bat            # Gradle wrapper (works offline once synced)
├── settings.gradle.kts             # rootProject.name = the app
├── build.gradle.kts                # root build file
├── gradle/
│   ├── libs.versions.toml          # dependency versions (per app)
│   └── wrapper/                    # wrapper jar + properties
├── local.properties               # machine-specific SDK path (gitignored)
└── app/                            # the Android application module
```

Because the apps are independent, each carries its own copy of the Gradle scaffolding and version
catalog. When you bump a shared dependency (Kotlin, Compose, AGP), do it in each app's
`gradle/libs.versions.toml` so they stay consistent.

## Building any app

The Android toolchain for this machine is installed under your home directory (no sudo):
`~/jdk17` (JDK 17), `~/android-sdk` (SDK), `~/gradle-8.9` (Gradle). The env vars are also in
`~/.bashrc`. To build an app:

```sh
cd steps            # or any app directory
./gradlew assembleDebug        # compile + package a debug APK
./gradlew lintDebug            # static analysis
# APK: app/build/outputs/apk/debug/app-debug.apk
```

To develop with a UI, open the **app's own directory** (e.g. `steps/`) in Android Studio — not the
suite root.

## Adding a new app

1. Create a new subdirectory named after the app's short name (e.g. `notes/`).
2. Start it with **Android Studio → New Project** *inside that directory*, or copy an existing app's
   Gradle scaffolding (`gradlew*`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
   `gradle/`, `.gitignore`) as a starting point.
3. Set `applicationId`/`namespace` = `com.ektebrysjan.<shortname>` and use that as the Kotlin package.
4. Apply the **charter** above: no `INTERNET` permission, local-only storage, one dark theme, open
   assets, no gamification. Set the launcher label to **“Ekte &lt;Name&gt;”**.
5. Add a row to the **Apps** table in this file.

> `steps/` is a good *reference* for the shared conventions (Gradle setup, the single dark Material 3
> theme, background-service + Room patterns) — but it contains pedometer-specific code, so copy it as
> a learning reference rather than a blank template. If you'd like a minimal blank **template app**
> (shared dark theme + charter-compliant manifest, no feature code) plus a `new-app.sh` scaffolding
> script, that's easy to add.
