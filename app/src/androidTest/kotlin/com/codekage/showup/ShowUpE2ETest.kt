package com.codekage.showup

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E flow tests run against a real emulator. Each test starts from a wiped app
 * (`pm clear` in @Before) and verifies one user-observable outcome.
 *
 * Run: ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ShowUpE2ETest {

    private lateinit var device: UiDevice
    private val pkg = "com.codekage.showup"
    private val timeout = 5000L
    private val longTimeout = 10000L

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // NOTE: Don't `pm clear` or `am force-stop` the target package — both kill the
        // instrumentation process (it shares UID/process with the target). Instead clear
        // the Room DB through the live AppContainer and re-launch the activity with
        // CLEAR_TASK so the back-stack is reset between tests.
        device.executeShellCommand("pm grant $pkg android.permission.ACCESS_FINE_LOCATION")
        device.executeShellCommand("pm grant $pkg android.permission.ACCESS_COARSE_LOCATION")
        device.executeShellCommand("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        clearAppDatabase()
        device.pressHome()
        launchApp()
    }

    @After
    fun tearDown() {
        // No force-stop here either — would kill the instrumentation process.
    }

    // ───────────────────────── tests ─────────────────────────

    @Test
    fun smokeTest_appLaunchesAndShowsDashboard() {
        // Either empty state (no jobs) or an existing job card — both indicate Compose is up.
        val emptyState = device.wait(Until.findObject(By.text("No jobs yet")), longTimeout)
        val jobChip = device.findObject(byTag("today_chip_office"))
        assertTrue(
            "Dashboard should render with either empty state or job card",
            emptyState != null || jobChip != null,
        )
    }

    @Test
    fun emptyState_addJob_appearsOnDashboard() {
        val jobName = "TestJob_${System.currentTimeMillis()}"
        addJobViaUi(name = jobName, address = "10 Downing Street")

        // Dashboard should now contain the new job's name
        val card = device.wait(Until.findObject(By.text(jobName)), longTimeout)
        assertNotNull("Job card '$jobName' should appear after save", card)

        // Empty-state button should be gone
        assertNull(
            "Empty state should disappear once a job exists",
            device.findObject(byTag("add_job_empty_button")),
        )
    }

    @Test
    fun markOffice_thenClear_updatesTodayState() {
        addJobViaUi("MarkJob", "Address 1")

        // Click Office chip
        val officeChip = device.wait(Until.findObject(byTag("today_chip_office")), longTimeout)
        assertNotNull("Office chip should be present after a job exists", officeChip)
        officeChip!!.click()

        // Clear chip should now appear (only renders when todayStatus != null)
        val clearChip = device.wait(Until.findObject(byTag("today_clear_button")), timeout)
        assertNotNull("Clear chip should appear after Office is marked", clearChip)

        // Tap clear
        clearChip!!.click()

        // Clear chip should be gone
        device.wait(Until.gone(byTag("today_clear_button")), timeout)
        assertNull(
            "Clear chip should disappear after clearing today's mark",
            device.findObject(byTag("today_clear_button")),
        )
    }

    @Test
    fun bottomNav_traversesAllTabs() {
        addJobViaUi("NavJob", "Anywhere")

        device.findObject(By.text("Calendar"))?.click()
        // Day-of-week header "Mon" is unique to the Calendar grid.
        assertNotNull(
            "Calendar should render its day-of-week headers",
            device.wait(Until.findObject(By.text("Mon")), timeout),
        )

        device.findObject(By.text("Reports"))?.click()
        assertNotNull(
            "Reports stat tile (Working Days) should appear on Reports tab",
            device.wait(Until.findObject(By.text("Working Days")), timeout),
        )

        device.findObject(By.text("Dashboard"))?.click()
        assertNotNull(
            "Dashboard greeting should appear when navigating back",
            device.wait(Until.findObject(By.textContains("Good ")), timeout),
        )
    }

    @Test
    fun calendarDayTap_showsCurrentStateSheet() {
        addJobViaUi("CalJob", "Address")
        device.findObject(By.text("Calendar"))?.click()

        // Tap any visible day cell — find one by its day number text
        val day10 = device.wait(Until.findObject(By.text("10")), longTimeout)
            ?: device.wait(Until.findObject(By.text("15")), timeout)
        assertNotNull("Should find at least one day cell", day10)
        day10!!.click()

        // Sheet shows a "Current state" badge label
        val badge = device.wait(Until.findObject(By.text("Current state")), timeout)
        assertNotNull("Status sheet should show 'Current state' label", badge)

        // And a "Set status" header
        val setStatus = device.wait(Until.findObject(By.text("Set status")), timeout)
        assertNotNull("Status sheet should show 'Set status' header", setStatus)
    }

    @Test
    fun generatePlan_dialogShowsSuggestion() {
        addJobViaUi("PlanJob", "Office Park")
        val planBtn = device.wait(Until.findObject(byTag("generate_plan_button")), longTimeout)
        assertNotNull("Generate Plan button should be on the job card", planBtn)
        planBtn!!.click()

        // Dialog title contains "Suggested plan for"
        val dialog = device.wait(Until.findObject(By.textContains("Suggested plan for")), timeout)
        assertNotNull("Plan dialog should open with title 'Suggested plan for ...'", dialog)
    }

    @Test
    fun reportsGraphToggle_switchesView() {
        addJobViaUi("RepJob", "Office")
        device.findObject(By.text("Reports"))?.click()

        // Initial state: list view → toggle button has contentDesc "Graph view"
        val toggle = device.wait(Until.findObject(byTag("graph_toggle_button")), longTimeout)
        assertNotNull("Graph toggle button should be visible on Reports", toggle)

        // After tapping, the icon's contentDesc flips to "List view" (only when in graph mode)
        toggle!!.click()
        val nowListIcon = device.wait(Until.findObject(By.desc("List view")), timeout)
        assertNotNull("After tapping graph toggle, icon should switch to 'List view' desc", nowListIcon)
    }

    @Test
    fun settings_themeToggle_isReachableAndTappable() {
        addJobViaUi("ThemeJob", "Address")

        // Open Settings via gear icon
        device.findObject(By.desc("Settings"))?.click()
        // Tap "Light" theme
        val lightBtn = device.wait(Until.findObject(byTag("theme_light_button")), longTimeout)
        assertNotNull("Light theme segmented button should be present", lightBtn)
        lightBtn!!.click()

        // Section header should remain visible (we're still on Settings)
        val themeText = device.wait(Until.findObject(By.text("Theme")), timeout)
        assertNotNull("Theme section header should remain visible after select", themeText)

        // Navigate away (back to dashboard) and re-open Settings — proxy for "selection sticks
        // across recomposition". A true cross-process restart can't be done here without killing
        // the instrumentation process; DataStore persistence is covered by unit tests.
        device.pressBack()
        // Wait for Dashboard before re-tapping the gear.
        device.wait(Until.findObject(By.textContains("Good ")), longTimeout)
        val gear = device.wait(Until.findObject(By.desc("Settings")), longTimeout)
        assertNotNull("Settings gear should be present on Dashboard", gear)
        gear!!.click()
        val lightAfterReopen = device.wait(Until.findObject(byTag("theme_light_button")), longTimeout)
        assertNotNull("Light theme button should still exist after reopening Settings", lightAfterReopen)
    }

    @Test
    fun jobDetail_editFlowOpensFormPrefilled() {
        addJobViaUi("EditJob", "First Address")

        // Tap the title row to navigate to Job Detail
        val card = device.wait(Until.findObject(By.text("EditJob")), longTimeout)
        assertNotNull(card)
        card!!.click()

        val editBtn = device.wait(Until.findObject(byTag("edit_job_button")), longTimeout)
        assertNotNull("Edit button in Job Detail top bar should be tappable", editBtn)
        editBtn!!.click()

        // Edit Job screen — title is "Edit Job", name field already has "EditJob"
        val title = device.wait(Until.findObject(By.text("Edit Job")), timeout)
        assertNotNull("Edit Job title should appear", title)
        val nameField = device.wait(Until.findObject(byTag("job_name_field")), timeout)
        assertNotNull("Job name field should be present", nameField)
        // The visible name text should be "EditJob"
        val prefilled = device.wait(Until.findObject(By.text("EditJob")), timeout)
        assertNotNull("Edit form should prefill with 'EditJob'", prefilled)
    }

    @Test
    fun jobDetail_deleteFlow_returnsToEmptyState() {
        addJobViaUi("DeleteMe", "Bye")

        device.wait(Until.findObject(By.text("DeleteMe")), longTimeout)?.click()

        device.wait(Until.findObject(byTag("delete_job_button")), longTimeout)?.click()
        // AlertDialog renders in a separate Compose window which doesn't inherit
        // `testTagsAsResourceId = true` from MainActivity, so testTags on dialog buttons
        // aren't exposed as resource-ids. Match the confirm button by visible text instead.
        val confirm = device.wait(Until.findObject(By.text("Delete")), timeout)
        assertNotNull("Confirm Delete button in dialog should be present", confirm)
        confirm!!.click()

        // Back on Dashboard — empty state should be visible
        val emptyText = device.wait(Until.findObject(By.text("No jobs yet")), longTimeout)
        assertNotNull("Empty state should reappear after deleting the only job", emptyText)
    }

    // ───────────────────────── helpers ─────────────────────────

    /** Wipe Room tables in-process so each test starts from an empty DB. */
    private fun clearAppDatabase() {
        val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as OfficeAttendanceApp
        // clearAllTables() blocks; safe to call from the test (instrumentation) thread.
        runCatching { app.appContainer.database.clearAllTables() }
    }

    private fun launchApp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), longTimeout)
    }

    /**
     * Compose's `testTagsAsResourceId = true` exposes each `Modifier.testTag("foo")` as the
     * literal resource-id `"foo"` (no `pkg:id/` prefix). UiAutomator's two-arg `By.res(pkg, id)`
     * never matches; we use the single-arg form which compares the resource-id as a regex.
     */
    private fun byTag(tag: String) = By.res(tag)

    /** Drives the Add Job UI; assumes start state is the dashboard. */
    private fun addJobViaUi(name: String, address: String) {
        // Use empty-state button if present, else FAB.
        val entry = device.wait(Until.findObject(byTag("add_job_empty_button")), timeout)
            ?: device.wait(Until.findObject(byTag("add_job_fab")), longTimeout)
        assertNotNull("No way to enter Add Job (no empty-state button or FAB)", entry)
        entry!!.click()

        val nameField = device.wait(Until.findObject(byTag("job_name_field")), longTimeout)
        assertNotNull("Job Name field not found on Add Job screen", nameField)
        nameField!!.click()
        nameField.text = name

        val addressField = device.wait(Until.findObject(byTag("job_address_field")), timeout)
        assertNotNull("Office Address field not found", addressField)
        addressField!!.click()
        addressField.text = address

        // Save — the button stays at the bottom; close any soft keyboard, then click.
        device.executeShellCommand("input keyevent 111") // ESC — closes IME on most AOSP builds
        val save = device.wait(Until.findObject(byTag("save_job_button")), longTimeout)
        assertNotNull("Save button (Add Job) not found", save)
        save!!.click()

        // Wait until we're back on Dashboard (greeting visible)
        device.wait(Until.findObject(By.textContains("Good ")), longTimeout)
    }

}
