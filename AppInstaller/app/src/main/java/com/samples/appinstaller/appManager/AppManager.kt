package com.samples.appinstaller.appManager

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionInfo
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import com.samples.appinstaller.workers.INSTALL_INTENT_NAME
import com.samples.appinstaller.workers.UNINSTALL_INTENT_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class AppManager(private val context: Context) {
    private val packageManager: PackageManager
        get() = context.packageManager

    private val packageInstaller: PackageInstaller
        get() = context.packageManager.packageInstaller

    fun openApp(packageId: String) {
        packageManager.getLaunchIntentForPackage(packageId)?.let {
            ContextCompat.startActivity(context, it, null)
        }
    }

    suspend fun getPackageInfo(packageId: String): PackageInfo? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                packageManager.getPackageInfo(packageId, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    private suspend fun isAppInstalled(packageId: String): Boolean {
        return getPackageInfo(packageId) != null
    }

    fun uninstallApp(packageId: String) {
        val statusIntent = Intent(UNINSTALL_INTENT_NAME).apply {
            `package` = context.packageName
        }

        val statusPendingIntent = PendingIntent.getBroadcast(context, 0, statusIntent, 0)
        packageInstaller.uninstall(packageId, statusPendingIntent.intentSender)
    }

    @Suppress("DEPRECATION")
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            Settings.Global.getInt(null, Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1
        }
    }


    suspend fun getActiveInstallSessionMap(): Map<Int, String> {
        return withContext(Dispatchers.IO) {
            return@withContext packageInstaller.mySessions
                .filter {
                    // In some cases the originatingUid attached to a sessionInfo is -1 even though
                    // the installerPackageName is the current app. Android loses the originatingUid
                    // when the installer itself is uninstalled during a commit session
                    it.originatingUid == context.applicationInfo.uid
                            && it.isActive
                            && it.appPackageName.isNullOrEmpty()
                }
                .map {
                    it.sessionId to it.appPackageName!!
                }
                .toMap()
        }
    }

    private suspend fun getInstallSessionsByPackage(packageName: String): List<SessionInfo> {
        return withContext(Dispatchers.IO) {
            return@withContext packageInstaller.mySessions.filter {
                // In some cases the originatingUid attached to a sessionInfo is -1 even though
                // the installerPackageName is the current app. Android loses the originatingUid
                // when the installer itself is uninstalled during a commit session
                it.originatingUid == context.applicationInfo.uid &&
                        it.appPackageName == packageName
            }
        }
    }

    suspend fun getCurrentInstallSession(packageName: String): SessionInfo? {
        return getInstallSessionsByPackage(packageName)
            // We filter to only get sessions that haven't been closed
            .filter { it.isActive }
            // We're checking if there's at least one exisiting session
            .maxByOrNull { it.updatedMillis }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun createInstallSession(appName: String) = withContext(Dispatchers.IO) {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setAppLabel(appName)
        }

        if (BuildCompat.isAtLeastS()) {
            params.setRequireUserAction(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.setInstallReason(PackageManager.INSTALL_REASON_USER)
        }

        return@withContext packageInstaller.createSession(params)
    }

    suspend fun getSessionInfo(sessionId: Int) = withContext(Dispatchers.IO) {
        return@withContext packageInstaller.getSessionInfo(sessionId)
    }

    fun isSessionOwned(sessionInfo: SessionInfo?): Boolean {
        sessionInfo?.let {
            return it.installerPackageName == context.applicationInfo.packageName
        }

        return false
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun writeAndCommitSession(sessionId: Int, apkInputStream: InputStream) {
        withContext(Dispatchers.IO) {
            val session = packageInstaller.openSession(sessionId)

            session.openWrite("package", 0, -1).use { destination ->
                apkInputStream.copyTo(destination)
            }

            val statusIntent = Intent(INSTALL_INTENT_NAME).apply {
                setPackage(context.packageName)
            }

            val statusPendingIntent = PendingIntent.getBroadcast(context, 0, statusIntent, 0)
            session.commit(statusPendingIntent.intentSender)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getInstallerPackageName(packageName: String): String? {
        return withContext(Dispatchers.IO) {
            return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                packageManager.getInstallerPackageName(packageName)
            }
        }
    }

    suspend fun getInstalledApps(): List<String> {
        val appInstallerPackage = context.packageName

        return withContext(Dispatchers.IO) {
            return@withContext packageManager.getInstalledApplications(0)
                .mapNotNull {
                    if (getInstallerPackageName(it.packageName) == appInstallerPackage) {
                        it.packageName
                    } else {
                        null
                    }
                }
        }
    }

    suspend fun getInstalledPackageMap(): Map<String, PackageInfo> {
        val appInstallerPackage = context.packageName

        return withContext(Dispatchers.IO) {
            return@withContext packageManager.getInstalledPackages(0)
                .mapNotNull {
                    if (getInstallerPackageName(it.packageName) == appInstallerPackage) {
                        it.packageName to it
                    } else {
                        null
                    }
                }
                .toMap()
        }
    }
}