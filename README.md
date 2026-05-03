# ShowUpV2

Android attendance tracker that helps users hit a configurable monthly office-day goal across one or more jobs. Hybrid-work focused: tracks office vs. remote days, factors in sick / leave / annual leave / bank holidays, suggests a plan to hit the goal, and surfaces a weekly / monthly report.

Originally built as **ShowUp** (`com.myofficeattendance`) — recovered from an APK after the source was lost, ported to Kotlin + Compose, and renamed to **ShowUpV2** (`com.codekage.showup.v2`) to allow side-by-side install.

---

## Status

| Check | State |
|---|---|
| `./gradlew assembleDebug` | green |
| `./gradlew testDebugUnitTest` | 30 / 30 passing |
| FLAG_SECURE | removed (screenshots allowed) |
| Target device | minSdk 26, targetSdk 35 |

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Architecture

Clean architecture, single-module, no DI framework — manual DI via `AppContainer`.

```
┌──────────────────────────────────────────────┐
│  presentation/  (Compose UI + ViewModels)    │
│   ↳ Material3, Navigation-Compose             │
│   ↳ StateFlow for screen state                │
└──────────────────────┬───────────────────────┘
                       │ uses
┌──────────────────────▼───────────────────────┐
│  domain/        (use cases, models, repos)   │
│   ↳ pure Kotlin, no Android dependencies      │
└──────────────────────┬───────────────────────┘
                       │ implemented by
┌──────────────────────▼───────────────────────┐
│  data/          (Room, Retrofit, DataStore)  │
│   ↳ entities ↔ domain models via mappers      │
└──────────────────────────────────────────────┘
                       ▲
                       │ scheduled by
┌──────────────────────┴───────────────────────┐
│  service/       (WorkManager + Receivers)    │
│   ↳ geofence triggers, daily/weekly workers   │
└──────────────────────────────────────────────┘
```

**Key principles:**
- Domain layer has zero Android imports — it's pure Kotlin and runs in JVM unit tests.
- Repositories are interfaces in `domain.repository`, implemented in `data.repository`.
- Use cases are single-method classes (`operator fun invoke`) so they can be invoked like functions.
- ViewModels expose a single `StateFlow<XxxUiState>` and don't leak `Job` / scope to callers.

---

## Module layout

```
app/src/main/kotlin/com/codekage/showup/v2/
├── OfficeAttendanceApp.kt          Application class (DI bootstrap, channels, worker scheduling)
├── AppContainer.kt                 Manual DI graph — lazy singletons for repos, use cases, services
│
├── domain/
│   ├── model/                      Job, AttendanceRecord, NonWorkDay, status enums
│   ├── repository/                 JobRepository, AttendanceRepository, NonWorkDayRepository
│   └── usecase/
│       ├── SaveJobUseCase           insert/update + register geofence
│       ├── DeleteJobUseCase         cascade delete records → non-work-days → geofence → job
│       ├── MarkAttendanceUseCase    insert/update record + goal-pace check
│       ├── GetDashboardDataUseCase  Flow<DashboardData> joining records + non-work-days
│       ├── GetReportDataUseCase     weekly/monthly aggregation, weekly breakdown
│       ├── SyncHolidaysUseCase      pull bank holidays from Nager.Date API
│       ├── GeneratePlanUseCase      suggests office-day distribution to hit goal
│       └── WorkingDays.kt           shared helper: countWorkingDays() with leave exclusion
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           Room DB, version 1, schema export enabled
│   │   ├── Converters.kt            UUID, LocalDate, LocalTime, LocalDateTime, DayOfWeek list, enums
│   │   ├── dao/                     JobDao, AttendanceRecordDao, NonWorkDayDao
│   │   └── entity/                  *Entity classes + EntityMappers.kt (toDomain/toEntity)
│   ├── remote/
│   │   ├── HolidayApiService.kt     Retrofit interface, base https://date.nager.at/
│   │   ├── PublicHolidayDto.kt      kotlinx.serialization @Serializable
│   │   └── CountryDto.kt
│   └── repository/
│       ├── JobRepositoryImpl, AttendanceRepositoryImpl, NonWorkDayRepositoryImpl
│       ├── HolidayRepository.kt     fetchAndStoreHolidays, getAvailableCountries
│       ├── SettingsRepository.kt    DataStore Preferences-backed AppSettings (15 fields)
│       ├── AppSettings, ThemeMode, AccentColor, BackupFrequency
│
├── service/
│   ├── GeofenceManager + GeofenceBroadcastReceiver  Play Services geofence registration
│   ├── NotificationScheduler + NotificationReceiver   AlarmManager-driven local notifications
│   ├── BootReceiver                ACTION_BOOT_COMPLETED → re-register geofences + notifications
│   ├── BackupWorker                 periodic AES-GCM-encrypted DB backup (Android Keystore)
│   ├── DailyNotificationWorker      morning office-day reminders
│   ├── WeeklySummaryWorker          Monday weekly summary
│   └── HolidaySyncWorker            7-day periodic holiday API sync
│
├── presentation/
│   ├── MainActivity.kt              FragmentActivity (for biometric), splash, theme wiring
│   ├── MainViewModel + MainUiState  observes settings → drives theme/accent + app-lock state
│   ├── theme/Theme.kt               Light + Dark color schemes, accent color override
│   ├── common/AttendanceColors.kt   status palette + goal-pace color heuristic
│   ├── common/ScreenHeader.kt       reusable greeting / settings-gear top bar
│   ├── navigation/
│   │   ├── Screen.kt                sealed class, kotlinx.serialization + type-safe routes
│   │   ├── BottomNavItem.kt         Dashboard / Calendar / Reports
│   │   └── AppNavigation.kt         NavHost wiring + bottom bar visibility logic
│   ├── auth/BiometricAuthManager.kt androidx.biometric prompt
│   ├── dashboard/                   DashboardScreen, ViewModel, UiState, JobDashboardItem, PlanDialog
│   ├── calendar/                    CalendarScreen with grid + status-picker sheet + range dialog
│   ├── reports/                     ReportsScreen with weekly/monthly toggle + bar chart + PDF export
│   ├── settings/                    SettingsScreen — 7 sections, SAF import, holiday manager
│   ├── addjob/                      AddEditJobScreen + ViewModel — geocoding + current-location
│   ├── jobdetail/                   JobDetailScreen — month stats, edit/delete actions
│   └── onboarding/OnboardingScreen.kt  first-run welcome (currently bypassed by default route)
│
└── util/
    ├── EncryptionUtils.kt           AES/GCM/NoPadding via AndroidKeyStore (alias office_attendance_backup_key)
    ├── PdfExporter.kt               PdfDocument-based report generator
    └── PdfReportData.kt + WeeklyPdfStats.kt
```

---

## Domain model

### Tables (Room v1)

```sql
-- jobs
id TEXT PK, name, officeAddress, officeLat REAL, officeLng REAL,
geofenceRadiusMeters INT, monthlyGoalPercent INT,
workDays TEXT (CSV of DayOfWeek), workStartTime TEXT, workEndTime TEXT,
isActive INT, createdAt TEXT

-- attendance_records
id TEXT PK, jobId TEXT, date TEXT,
status TEXT [OFFICE|REMOTE|LEAVE|SICK|BANK_HOLIDAY|ANNUAL_LEAVE|ABSENT],
entryMethod TEXT [AUTO_GPS|MANUAL], gpsConfirmed INT, notes TEXT?,
createdAt TEXT, updatedAt TEXT

-- non_work_days
id TEXT PK, jobId TEXT? (NULL = global), date TEXT,
type TEXT [BANK_HOLIDAY|ANNUAL_LEAVE|SICK|OTHER], label TEXT
```

### Settings (DataStore Preferences)

`AppSettings` — 15 fields covering theme mode, accent colour, all 4 notification toggles, app-lock + timeout, auto-backup + frequency, reminder time, holiday country code + last sync year.

---

## Tech stack

- **Kotlin** 2.1.20 / **JVM target** 17 / **Compose Compiler** plugin
- **Android Gradle Plugin** 8.13.2 / **Gradle** 8.13
- **KSP** 2.1.20-1.0.32 (KSP1 mode — `ksp.useKSP2=false` to dodge a Room compile bug)
- **Compose BOM** 2024.12.01 — Material3, Navigation Compose, Activity Compose
- **Room** 2.6.1
- **Retrofit** 2.11.0 + Square's `converter-kotlinx-serialization`
- **OkHttp** 4.12.0
- **kotlinx.serialization** 1.7.3
- **kotlinx.coroutines** 1.9.0
- **DataStore Preferences** 1.1.1
- **WorkManager** 2.10.0
- **Play Services Location** 21.3.0 (geofencing + fused location)
- **Biometric** 1.2.0-alpha05
- **Splash Screen** 1.0.1

Test deps: JUnit 4, mockk, turbine, Robolectric, kotlinx-coroutines-test, Compose UI Test.

---

## Features

### Dashboard (`presentation/dashboard/`)
- Greeting + today's date + Settings gear
- Per-job card showing:
  - Job name + office address + current %
  - Stats row: This Month % / Office Days (so far) / Days Left
  - Pace banner with one of: `ACHIEVED` / `ON_TRACK` / `AT_RISK` / `IMPOSSIBLE` / `NO_DATA`
  - **All 6 status options** as today-action chips (Office / Remote / Sick / Leave / Annual Leave / Bank Holiday) with current selection highlighted; **Clear** button when something is set
  - **Generate Plan** button → opens `PlanDialog` with suggested office-day distribution
- FAB → Add Job
- Empty state when no jobs

### Calendar (`presentation/calendar/`)
- Month grid (Mon-Sun headers, 6 rows × 7 cols) with status-tinted day cells
- Tap day → `StatusPickerSheet` showing:
  - Date as full title
  - **Current state badge** in colored card (status label, NonWorkDay info, or "Not set")
  - All 6 status buttons with current selection check-marked
  - **Clear** button when a record exists
- Long-press day → enters bulk-selection mode
- Bulk action bar → set status for many days at once
- Top bar: month nav (◀ May 2026 ▶) + "Add Date Range" → modal dialog with start/end day + status

### Reports (`presentation/reports/`)
- **Weekly / Monthly** segmented toggle
- Period nav (◀ ▶) over week-range or month
- 6-tile stat grid: Working Days / Office / Remote / Sick / Leave / Absent + Bank Holiday row
- **Weekly breakdown** with **graph ↔ list toggle**:
  - Graph: stacked bar chart drawn on `Canvas` — Office (green) / Remote (blue) / Leave (orange) / Sick (red), bar height ∝ total working days, horizontal goal line
  - List: card-per-week summary
- **Export PDF** — generates via `PdfDocument`, fires `Intent.ACTION_SEND` chooser via FileProvider so user can save to Drive / send by email

### Settings (`presentation/settings/`)
Seven sections:
1. **Appearance** — Theme (System/Light/Dark segmented) + Accent (5 swatches: Green/Blue/Purple/Orange/Rose)
2. **Notifications** — Office Day Reminders / GPS Failure Alerts / Goal Alerts / Weekly Summary toggles + Reminder Time
3. **Security** — App Lock toggle (biometric/credential)
4. **Backup** — Auto Backup toggle / Backup Now / Restore from Backup / Share Backup / Import Backup (SAF `OpenDocument`)
5. **Holidays** — Country (GB) / Refresh / Manage (inline list of synced bank holidays with Remove)
6. **Permissions** — Location Access / Notifications / Exact Alarms (status-only display)
7. **About** — Version

### Add / Edit Job (`presentation/addjob/`)
- Job Name / Office Address (with helper hint) / Lat / Lng
- **Use Current Location** button — runtime permission flow → `FusedLocationProviderClient.getCurrentLocation` → reverse-geocode → fill all three
- Pasting a Maps URL or `lat,lng` string into Address auto-extracts coordinates (skips slow forward-geocode)
- Geofence Radius slider (50-500 m)
- Monthly Office Goal slider (0-100 %)
- Work Days FlowRow chips
- Start / End Time inputs
- Errors inline; Save button disables during save

### Job Detail (`presentation/jobdetail/`)
- Top bar: Back / job name / Edit / Delete
- Big % donut for current pace
- "This Month" card: Office / Remote / Sick / Leave + Working Days / Remaining / Goal
- "Job Details" card: Address / Geofence Radius / Work Days / Work Hours / Status (Active/Inactive)
- Deactivate / Activate Job toggle
- Delete confirmation dialog

### Manual / Auto Plan (`presentation/dashboard/PlanDialog.kt`, `domain/usecase/GeneratePlanUseCase.kt`)
- Computes `target = ceil(workingDays * goalPercent / 100)` — minus already-marked office days = `needed`
- Distributes `needed` picks evenly across remaining work days from today through month end
- **Apply** → bulk-marks all suggestions as OFFICE
- **Modify** → dismisses dialog (TODO: navigate to Calendar with dates pre-selected)
- **Cancel** → dismiss

### Notifications (`service/`)
4 channels created at app startup:
- `office_reminder` — Office Day Reminders (priority default)
- `gps_failure` — GPS Failure Reminders (priority default)
- `goal_alert` — Goal Alerts (priority high)
- `weekly_summary` — Weekly Summary (priority low)

Scheduled via `AlarmManager` (`setExactAndAllowWhileIdle` if permitted, else `set`).

### Geofencing (`service/GeofenceManager`)
- One geofence per active job — request ID = job UUID
- 100 m default radius (configurable)
- ENTER trigger → `GeofenceBroadcastReceiver` auto-marks today as OFFICE (if no existing record) via `MarkAttendanceUseCase` with `EntryMethod.AUTO_GPS`

### Backup (`service/BackupWorker`, `util/EncryptionUtils`)
- AES-256-GCM via Android Keystore; alias `office_attendance_backup_key`
- Encrypts SQLite file (and -wal / -shm if present) → `<filesDir>/backups/backup_<timestamp>.enc`
- Periodic via WorkManager (default 24 h, configurable to weekly/monthly)
- Keeps 7 most recent, deletes older
- Restore reads encrypted file → decrypt → validate "SQLite format 3" header → copy to live DB path
- **SAF import** (`Settings → Import Backup`): user picks file via `OpenDocument`, content://uri streamed into cache, then restored

---

## Design system

### Colors

Status palette (`presentation/common/AttendanceColors.kt`):

| Status | Hex |
|---|---|
| Office | `#3F8C3D` |
| Remote | `#7AC8FF` |
| Leave | `#FFB343` |
| Sick | `#FF6E6C` |
| Bank Holiday | `#7BB7FF` |
| Absent | `#888A87` |

Goal-pace heuristic: `office` if ≥ goal, `leave` if ≥ 80 % of goal, otherwise `sick`.

### Theme (`presentation/theme/Theme.kt`)

- Material3 color schemes — `LightColors` / `DarkColors`
- 5 accent options override `colorScheme.primary`: GREEN (default) / BLUE / PURPLE / ORANGE / ROSE — each with light + dark variants
- Optional dynamic colours on Android 12+
- System / Light / Dark / Dynamic toggling driven by `MainViewModel` reading `AppSettings.themeMode`

### Iconography

- Adaptive launcher icon: dark navy `#12151D` background, green stroke check-in-circle, **`V2` subscript** in lower-right
- Material Icons for action chips, top app bar, bottom navigation

---

## Build & run

### Prerequisites
- Android Studio
- Java 17 (Adoptium 21 also works — Gradle picks via foojay toolchain plugin)
- Android SDK 35 + build-tools 37+

### CLI
```bash
./gradlew assembleDebug          # build APK
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew installDebug           # install on connected device / emulator
```

### Manual install on emulator
```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.codekage.showup.v2/.presentation.MainActivity
```

### Local config
- `local.properties` contains `sdk.dir` — auto-generated on first AS sync, do not commit
- `gradle.properties` has `ksp.useKSP2=false` — required for Room compile under AGP 8.13 + KSP 1.0.32

---

## Testing

### JVM unit tests (30 — all passing)

| Suite | Count | Coverage |
|---|---|---|
| `ConvertersTest` | 10 | UUID / LocalDate / LocalTime / LocalDateTime / DayOfWeek list / 3 enum types round-trip |
| `EntityMappersTest` | 3 | Job, AttendanceRecord, NonWorkDay (with null jobId) Domain ↔ Entity round-trip |
| `SaveJobUseCaseTest` | 4 | insert vs update dispatch, geofence registered only for active jobs |
| `DeleteJobUseCaseTest` | 1 | cascade delete order: records → non-work-days → geofence → job |
| `GetReportDataUseCaseTest` | 3 | full week aggregation, bank holiday exclusion, ANNUAL_LEAVE counts as leave |
| `GeneratePlanUseCaseTest` | 2 | suggests enough days to hit goal; empty when goal already reached |
| `CoordExtractTest` | 7 | bare lat,lng / `?query=` URL / `@lat,lng` URL / range checks / negative coords |

### Instrumented tests
None yet — `androidTestImplementation` deps are wired (`compose-ui-test-junit4`, `mockk-android`, `androidx.junit`) but no tests written.

### Manual flow checklist (run after each significant change)
- [ ] Launch — dashboard renders empty state
- [ ] Add Job → form validates → returns to dashboard with new card
- [ ] Tap Office chip → mark today, % updates
- [ ] Tap Clear → record removed, % resets
- [ ] Generate Plan → dialog shows distribution → Apply → office days fill, projected % matches goal, currentPercentage stays at 0% until past dates accumulate
- [ ] Calendar tab → grid renders → tap day → status sheet shows current state → change → grid color updates
- [ ] Reports tab → toggle Weekly/Monthly → graph view shows bars → list view shows cards
- [ ] Reports → Export PDF → share chooser fires
- [ ] Settings → toggle dark theme → app rethemes immediately
- [ ] Settings → Import Backup → SAF picker → restore confirmation
- [ ] Job Detail → Edit → values prefilled → save → dashboard updates
- [ ] Job Detail → Delete → confirm → dashboard back to empty

---

## Working-day calculation semantics

A "working day" is a day on which the user was *expected* to work and *could* have been in the office. Calculated in `domain/usecase/WorkingDays.kt`:

```
workingDay = dayOfWeek ∈ job.workDays
           ∧ date ∉ job.nonWorkDates       (bank holidays, manual non-work days)
           ∧ date ∉ excludedRecordDates    (any record with status SICK / LEAVE / ANNUAL_LEAVE / BANK_HOLIDAY)
```

The denominator for office-percentage uses this adjusted count, so taking sick / leave days reduces *both* the numerator (you can't be in office) and the denominator (the day no longer counts) — preserving the percentage rather than penalising leave.

`currentPercentage` uses `officeDaysSoFar` (records with date ≤ today) over `workingDaysSoFar` (working days from monthStart to today). `projectedPercentage` uses `officeDaysScheduled` (all OFFICE records including future planner picks) over `totalWorkingDays`. This split fixes the "applying plan makes percentage shoot to 900%" bug — future planned days no longer inflate the past-only ratio.

---

## Known caveats & future work

### Dropped from original APK during port
- **FLAG_SECURE** — original called `getWindow().setFlags(8192, 8192)` unless Espresso was on classpath. Removed by user request to allow screenshots.
- **Espresso anti-tamper hatch** — gone with FLAG_SECURE.
- **Android Keystore identity** — rebuilt v2 has a different signing cert + applicationId, so it cannot decrypt the original ShowUp's encrypted backup files. Backup format is identical going forward.

### Not yet implemented
- **Onboarding gate** — `OnboardingScreen` exists but `AppNavigation` always starts at Dashboard. Should branch on `AppSettings.onboardingCompleted`.
- **Plan "Modify"** — currently dismisses; should navigate to Calendar with the suggested dates pre-selected as bulk selection. Needs a route argument on `Screen.Calendar`.
- **Reminder Time picker** — Settings shows current time but no picker to change it.
- **Country picker** — Settings shows "GB" hardcoded; needs a chooser pulling from `HolidayRepository.getAvailableCountries()`.
- **App-lock prompt** — `BiometricAuthManager` exists but isn't wired to a route guard. `MainActivity` doesn't gate Compose content on auth.
- **Background-location permission** — required for geofencing on Android 10+, not requested at runtime; needs a tutorial / second-step prompt.
- **Notification permission** — Android 13+ POST_NOTIFICATIONS not requested at runtime.
- **Instrumented UI tests** — none yet.

### Visual fidelity vs. original
Composables were reconstructed from UI dumps + decompiled state classes (the original Compose UI decompiled into unreadable `Composer` plumbing). Layout structure matches; spacing / typography / animations may diverge from the original look-and-feel. Iterate visually against the original screenshots / running APK when refining.

### Recovered reference material
- `recovered/showup.apk` — original 17.1 MB APK pulled from device
- `recovered/jadx-out/sources/com/myofficeattendance/` — 129 decompiled Java classes
- `recovered/jadx-out/resources/` — manifest, full res tree, assets
- `recovered/ui-dumps/` — 11 UI hierarchy dumps captured from the live original app
- `recovered/screenshots/` — 25 KB black PNGs (FLAG_SECURE blocked all capture in original)
- `recovered/livetest/` — flow-test artefacts captured against the rebuilt v2 on emulator
- `backup_20260501_203747.enc` — encrypted user backup; can only be decrypted by the original APK on its origin device

These are kept in-tree as historical reference and are NOT part of the build (no source roots include them).

---

## License / authorship

Personal project. No license file on purpose — all rights reserved unless explicitly stated otherwise.

Reverse-engineered + ported by Mike Akinyemi (codekage), 2026.
