package com.samples.appinstaller.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class AppManager(private val context: Context) {
    private val packageManager: PackageManager
        get() = context.packageManager

    fun openApp(packageId: String) {
        packageManager.getLaunchIntentForPackage(packageId)?.let {
            ContextCompat.startActivity(context, it, null)
        }
    }

    private fun isAppInstalled(packageId: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageId, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun checkAppStatus(packageId: String): AppStatus {
        if(!canRequestPackageInstalls()) {
            return AppStatus.UNKNOWN
        }

        return if(isAppInstalled(packageId)) {
            AppStatus.INSTALLED
        } else {
            AppStatus.UNINSTALLED
        }
    }

    fun uninstallApp(packageId: String) {
        val statusIntent = Intent(UNINSTALL_INTENT_NAME).apply {
            `package` = context.packageName
        }

        val statusPendingIntent = PendingIntent.getBroadcast(context, 0, statusIntent, 0)
        packageManager.packageInstaller.uninstall(packageId, statusPendingIntent.intentSender)
    }

    // TODO: How to check this permission before API 26
    private fun canRequestPackageInstalls() = packageManager.canRequestPackageInstalls()

    suspend fun createInstallSession(): Int {
        return withContext(Dispatchers.IO) {
            val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
            params.setInstallReason(PackageManager.INSTALL_REASON_USER)

            return@withContext packageManager.packageInstaller.createSession(params)
        }
    }

    suspend fun installApp(sessionId: Int, apkInputStream: InputStream) {
        withContext(Dispatchers.IO) {
            val session = packageManager.packageInstaller.openSession(sessionId)

            session.openWrite("package", 0, -1).use { destination ->
                apkInputStream.copyTo(destination)
            }

            val statusIntent = Intent(INSTALL_INTENT_NAME)
            statusIntent.setPackage(context.packageName)
            val statusPendingIntent = PendingIntent.getBroadcast(context, 0, statusIntent, 0)
            session.commit(statusPendingIntent.intentSender)
        }
    }
}

enum class AppStatus {
    UNKNOWN, UNINSTALLED, INSTALLED, STAGING, DOWNLOADING, INSTALLING, UPGRADE
}

private val UnexpectedStatusValue = Exception("Status value is unexpected")

fun getStatusFailureName(value: Int): String {
    return when(value) {
        PackageInstaller.STATUS_FAILURE -> "Failure"
        PackageInstaller.STATUS_FAILURE_ABORTED -> "Aborted"
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "Blocked"
        PackageInstaller.STATUS_FAILURE_CONFLICT -> "Conflict"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Incompatible"
        PackageInstaller.STATUS_FAILURE_INVALID -> "Invalid"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "Storage Failure"
        else -> throw UnexpectedStatusValue
    }
}