package com.codekage.showup.v2.presentation.settings

import android.net.Uri
import com.codekage.showup.v2.data.repository.AppSettings
import com.codekage.showup.v2.domain.model.NonWorkDay
import java.io.File

enum class PortableBackupAction { EXPORT, IMPORT }

data class PortableBackupPrompt(
    val action: PortableBackupAction,
    val uri: Uri,
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val holidaySyncStatus: String? = null,
    val backupStatus: String? = null,
    val restoreStatus: String? = null,
    val backupFiles: List<File> = emptyList(),
    val showRestoreDialog: Boolean = false,
    val pendingRestoreFile: File? = null,
    val bankHolidays: List<NonWorkDay> = emptyList(),
    val showHolidayManager: Boolean = false,
    val shareBackupFile: File? = null,
    val portablePrompt: PortableBackupPrompt? = null,
    val portableStatus: String? = null,
)
