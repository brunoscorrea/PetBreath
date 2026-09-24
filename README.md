# PetBreath

PetBreath is a simple, private Android app that helps dog and cat owners measure
and track their pet's **resting (sleeping) respiratory rate (RRR)**. It is
especially useful for pets with heart conditions, where a rising resting
breathing rate can be an early warning sign your veterinarian may ask you to
watch for.

> **PetBreath does not diagnose medical conditions.** It is a record-keeping
> tool. Always follow your veterinarian's advice, and seek emergency care if your
> pet is struggling to breathe.

## Features

- **Tap-to-count breath counter.** Tap once per breath in 30- or 60-second mode.
  The timer starts on the first tap. Includes undo, restart, haptic feedback and
  a keep-screen-on display.
- **Automatic breaths/minute** with a clear *Within range / Above range / Below
  range* result (icon + text + color, never color alone).
- **Configurable normal range per pet.** The default upper limit is 30
  breaths/min, which you or your vet can change. An optional lower limit is
  available too.
- **Multiple pets**, each with name, species, breed, age, weight and medical notes.
- **History** with date and time. Every reading can be edited or deleted (with
  undo), and readings can also be added manually.
- **Trend chart** (7 / 30 / 90 days / all) with the threshold drawn in, plus
  average, low, high and out-of-range counts.
- **Reminders to measure** at chosen times and days of the week.
- **Medications:** dose and instructions, reminder times, a "Mark as given"
  button (also available as a notification action) and a recent dose log.
- **Vet reports:** export a printable **PDF** (summary, chart, medications,
  notes, full table) or a **CSV** spreadsheet and share it with any app you choose.
- **Education:** what RRR is, how to measure it, what's normal, when to contact
  your vet and when to seek emergency care.
- Light/dark theme, kg/lb units, TalkBack support, large touch targets.

## Privacy: 100% on-device

- No account, no sign-in, no backend, no analytics, no ads.
- The app **does not request the `INTERNET` permission**, so it cannot send data
  anywhere.
- Data lives in a local Room/SQLite database and a DataStore preferences file in
  the app's private storage.
- `allowBackup="false"` plus data-extraction rules keep health data out of cloud
  backups and device-to-device transfers.
- Reports are written to the app's private cache and shared only through the
  Android share sheet, when you ask.
- Uninstalling the app deletes all data.

## Building

Requirements: **JDK 17+** and the **Android SDK** (Android Studio Ladybug or
newer is the easiest way to get both). The project targets **compileSdk/targetSdk
35** and **minSdk 26** (Android 8.0).

```bash
# Debug build: installs alongside a release build (package com.petbreath.app.debug)
./gradlew assembleDebug
./gradlew installDebug            # with a phone connected (USB debugging on)

# Release build: minified with R8
./gradlew assembleRelease
```

The APKs are written to `app/build/outputs/apk/<buildType>/`.

If Gradle can't find the SDK, create `local.properties` in the project root:

```properties
sdk.dir=/path/to/Android/sdk
```

### Release signing

Without extra setup, release builds are signed with the debug key so they can be
installed for testing. To sign with your own key, create `keystore.properties` in
the project root (it is git-ignored):

```properties
storeFile=release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

## Tests

```bash
./gradlew testDebugUnitTest        # JVM + Robolectric unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (device/emulator)
./gradlew lintDebug
```

- `app/src/test/…/domain`: pure-Kotlin tests for the rate calculation, the
  tap-counting state machine, range classification, statistics, reminder
  scheduling (including DST), age/weight conversion and CSV escaping.
- `app/src/test/…/data`: Robolectric + in-memory Room tests for the DAOs,
  cascading deletes, medication reminders and reminder scheduling.
- `app/src/test/…/ui`: `MeasureViewModel` with a fake clock, and pet form validation.
- `app/src/androidTest`: Room on a real device, plus Compose UI tests
  (status chip, chart accessibility summary, onboarding).

A GitHub Actions workflow (`.github/workflows/android.yml`) runs the unit tests
and lint, and builds debug and release APKs on every push.

## Architecture

Single-activity app with Jetpack Compose (Material 3), MVVM and unidirectional
data flow.

```
app/src/main/java/com/petbreath/app/
├── PetBreathApp.kt          Application + small manual DI container
├── MainActivity.kt          Compose host, notification deep links
├── domain/                  Pure Kotlin: rate math, session state machine,
│                            ranges, stats, reminder schedule, CSV builder
├── data/db/                 Room entities, DAOs, database
├── data/repository/         Repositories (pets, measurements, medications, reminders)
├── data/settings/           DataStore-backed app settings
├── reminders/               AlarmManager scheduling, notifications, receivers
├── export/                  PDF (android.graphics.pdf) and CSV report export
└── ui/                      Compose screens, ViewModels, theme, navigation
```

- **Storage:** Room (SQLite) with foreign keys and cascading deletes. The schema
  is exported to `app/schemas/` for migration tests.
- **Reminders:** inexact `AlarmManager.setAndAllowWhileIdle` alarms (no special
  exact-alarm permission), rescheduled after each alarm fires, on reboot, on app
  update, and on time or time-zone changes. They may arrive a few minutes late
  when the phone is in battery-saving mode.
- **Notifications:** Android 13+ asks for the notification permission in
  context, on the Reminders and Medications screens.
- **Accessibility:** semantic headings, content descriptions, a TalkBack-friendly
  tap counter (double-tap counts a breath), live-region results, a chart with a
  spoken summary, 48dp+ touch targets, and support for font scaling.

## About the reference values

PetBreath uses **30 breaths per minute** as its default reference threshold,
because many veterinary cardiology resources describe most healthy sleeping dogs
and cats as breathing below 30 times per minute. Every pet is different: always
use the range your veterinarian recommends.

## License

This project does not include a license yet. Add one before publishing it.
