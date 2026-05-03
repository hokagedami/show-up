package com.codekage.showup.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.codekage.showup.OfficeAttendanceApp
import com.codekage.showup.data.local.AppDatabase
import com.codekage.showup.util.EncryptionUtils
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val database: AppDatabase =
        (appContext.applicationContext as OfficeAttendanceApp).appContainer.database

    override suspend fun doWork(): Result = runCatching {
        val backupsDir = File(applicationContext.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        database.close()
        val dbFile = applicationContext.getDatabasePath("office_attendance_db")
        if (!dbFile.exists()) return@runCatching Result.success()

        val tempDb = File(backupsDir, "backup_temp.db")
        dbFile.copyTo(tempDb, overwrite = true)
        File(dbFile.path + "-wal").takeIf { it.exists() }
            ?.copyTo(File(backupsDir, "backup_temp.db-wal"), overwrite = true)
        File(dbFile.path + "-shm").takeIf { it.exists() }
            ?.copyTo(File(backupsDir, "backup_temp.db-shm"), overwrite = true)

        val encryptedFile = File(backupsDir, "backup_${timestamp}.enc")
        EncryptionUtils.encryptFile(tempDb, encryptedFile)
        tempDb.delete()
        File(backupsDir, "backup_temp.db-wal").delete()
        File(backupsDir, "backup_temp.db-shm").delete()

        backupsDir.listFiles { f -> f.extension == "enc" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(7)
            ?.forEach { it.delete() }

        Log.i(TAG, "Encrypted backup completed: ${encryptedFile.name}")
        Result.success()
    }.getOrElse {
        Log.e(TAG, "Backup failed", it)
        Result.retry()
    }

    companion object {
        private const val TAG = "BackupWorker"
        private const val WORK_NAME = "periodic_backup"

        fun schedule(context: Context, intervalHours: Long = 24) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalHours, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun getBackupFiles(context: Context): List<File> =
            File(context.filesDir, "backups")
                .listFiles { f -> f.extension == "enc" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()

        fun restoreFromBackup(context: Context, backupFile: File, database: AppDatabase): Boolean = runCatching {
            val tempFile = File(context.cacheDir, "restore_temp.db")
            EncryptionUtils.decryptFile(backupFile, tempFile)
            if (!tempFile.exists() || tempFile.length() < 100) return@runCatching false
            val header = FileInputStream(tempFile).use { it.readNBytes(16) }
            if (!String(header, Charsets.UTF_8).startsWith("SQLite format 3".take(header.size))) {
                tempFile.delete()
                return@runCatching false
            }
            database.close()
            val target = context.getDatabasePath("office_attendance_db")
            tempFile.copyTo(target, overwrite = true)
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            tempFile.delete()
            true
        }.getOrDefault(false)
    }
}
