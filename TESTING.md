# Testing Documentation

This document outlines the testing strategy and execution for the ShowUp V2 Android application.

## End-to-End (E2E) Testing

A comprehensive E2E test suite has been implemented to verify core user journeys. Due to compatibility issues with Espresso and Compose UI Testing on Android 15 (API 35) regarding `InputManager` accessibility, the E2E tests are built using **UI Automator**.

### Test Suite: `ShowUpE2ETest`
**Path:** `app/src/androidTest/kotlin/com/codekage/showup/v2/ShowUpE2ETest.kt`

#### Coverage:
1.  **App Launch:** Verifies the app starts and reaches the Dashboard.
2.  **Add Job Flow:**
    *   Clicks "Add Job" from the empty state.
    *   Enters Job Name and Office Address using shell-level input for maximum reliability in Compose.
    *   Saves the job and verifies navigation back to the Dashboard.
3.  **Dashboard Verification:** Ensures the newly created job card is visible with the correct name.
4.  **Attendance Interaction:** Marks today's status as "Office" using the quick-action chips.
5.  **Bottom Navigation:** Verifies that the user can navigate between the Dashboard, Calendar, and Reports screens.

### How to Run E2E Tests
Ensure a virtual or physical device is connected (Target SDK 35 recommended).

```powershell
./gradlew connectedDebugAndroidTest
```

## Technical Implementation Details

### UI Automator & Compose Integration
To make Compose components visible to UI Automator and other accessibility-based tools, `testTagsAsResourceId` is enabled in `MainActivity.kt`:

```kotlin
Box(Modifier.semantics { testTagsAsResourceId = true }) {
    AppNavigation(appContainer)
}
```

This allows finding nodes by their `testTag` using the `By.res(resourceId)` selector in UI Automator.

### Test Tags Used
The following tags are available for automated testing:
*   `job_name_field`: Input field for the job name.
*   `job_address_field`: Input field for the office address.
*   `save_job_button`: The button to submit the add/edit job form.
*   `add_job_fab`: The Floating Action Button on the dashboard.
*   `add_job_empty_button`: The "Add Job" button shown in the empty dashboard state.

### Build Stability
The `app/build.gradle.kts` file includes packaging exclusions to prevent build failures caused by duplicate license files in JUnit and other dependencies:

```kotlin
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/LICENSE.md"
        excludes += "/META-INF/LICENSE-notice.md"
    }
}
```

## Unit & Integration Testing

Unit tests are located in `app/src/test` and cover:
*   **Data Mappers:** Conversion between Local Entities and Domain Models.
*   **Database Converters:** JSON serialization of complex types for Room.
*   **Domain Logic:** Business rules for working days calculation and goal tracking.

To run unit tests:
```powershell
./gradlew test
```

## Known Issues & Troubleshooting
*   **Keyboard Interference:** E2E tests automatically attempt to hide the keyboard before clicking "Save". If a test fails, ensure the software keyboard is not blocking the UI components.
*   **Permission Dialogs:** The `ShowUpE2ETest` automatically grants `ACCESS_FINE_LOCATION` and `POST_NOTIFICATIONS` via `adb shell pm grant` during the `@Before` phase to prevent UI blocking.
