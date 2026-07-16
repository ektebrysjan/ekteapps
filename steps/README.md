# Ekte Steps

App #1 in the [Ekte Apps suite](../README.md) (application id `com.ektebrysjan.steps`). A **free,
simple, ad-free** step counter for Android with **no data collection** — the app has no `INTERNET`
permission, so it physically cannot send your data anywhere. Everything is stored locally on your
phone, in a single clean dark-mode UI.

## Features

- **Today** screen — a large count of steps taken today, plus a bar chart of the previous 6 days.
- **Statistics** screen — total steps, daily average, best day, days tracked, full day-by-day
  history, and a **Clear history** button (with confirmation).
- **Background counting** — a lightweight foreground service keeps counting while the app is closed,
  and it resumes automatically after a reboot.
- Local-only storage via a Room (SQLite) database. No accounts, no analytics, no ads.

## How it works

The app reads the phone's hardware **step-counter sensor** (`TYPE_STEP_COUNTER`), which reports a
running total since the last reboot. A background service converts that running total into per-day
deltas and stores a daily total in a local database. The UI observes the database, so counts update
live. Reboots (which reset the hardware counter) are detected and handled so no phantom steps are
added.

## Requirements

- **Android Studio** (Ladybug / 2024.2 or newer recommended).
- A **physical Android device** running Android 8.0 (API 26) or newer, with a step-counter sensor
  (virtually all modern phones have one). **Most emulators do not have a real step sensor**, so
  counts won't move on an emulator unless you inject sensor events via ADB.

## Build & run

1. Open this `steps/` folder in **Android Studio** (File → Open) — open the app directory itself, not the suite root.
2. Let Gradle **sync**. On first sync Android Studio downloads the Gradle distribution and
   generates the Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`), which is intentionally
   not included here. It will also download the Android SDK components and dependencies.
3. Connect a physical device (USB debugging on) and click **Run ▶**.
4. On first launch, grant **Physical activity** (Activity Recognition) and **Notifications**
   permissions when prompted. A quiet, ongoing notification shows the service is running.
5. Walk around — the "Today" count rises. Try the Statistics tab and the Clear history button.

If you prefer the command line, the toolchain is already set up on this machine (see the
[suite README](../README.md#building-any-app)): `./gradlew assembleDebug` builds the APK and
`./gradlew installDebug` installs it to a connected device.

## Before publishing

- The application id is `com.ektebrysjan.steps` and the launcher label is **“Ekte Steps”** (the
  in-app header reads “Steps” with “by EkteBrysjan” beneath it). `applicationId` cannot be changed
  after the first Play Store release.
- Replace the launcher icon if you want branding beyond the generated green walking figure
  (`app/src/main/res/drawable/ic_launcher_foreground.xml` and `ic_launcher_background.xml`).

## Project layout

```
app/src/main/java/com/ektebrysjan/steps/
  StepsApp.kt          Application; creates the notification channel
  MainActivity.kt          Compose host; requests permissions; starts the service
  data/                    Room entity, DAO, database, repository (daily-delta logic)
  service/                 Foreground StepCounterService + BootReceiver
  ui/                      Compose theme (single dark scheme), ViewModel, Home & Stats, bottom nav
  util/DateUtils.kt        Calendar-day helpers
```

## Privacy

No network access, no third-party SDKs, no tracking. Step data lives only in the app's private
storage. Uninstalling the app, or tapping **Clear history**, removes it.
