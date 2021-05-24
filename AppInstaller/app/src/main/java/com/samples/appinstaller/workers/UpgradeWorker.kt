package com.samples.appinstaller.workers

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.apps.AppManager
import com.samples.appinstaller.settings.appSettings
import com.samples.appinstaller.settings.toDuration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

const val UPGRADE_WORKER_TAG = "upgrade"

class UpgradeWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val appManager = AppManager(applicationContext)

    override suspend fun doWork(): Result = coroutineScope {
        Log.d("UpgradeWorker", "Work is happening in the background")

        val settings = applicationContext.appSettings.data.firstOrNull()
            ?: return@coroutineScope Result.failure()

        val updateAvailabilityPeriod = settings.updateAvailabilityPeriod

        appManager
            .getInstalledPackageMap()
            .filterValues { isUpdateAvailable(updateAvailabilityPeriod, it.lastUpdateTime) }
            .forEach { (_, packageInfo) ->
                run {
                    this.launch {
                        installApp(applicationContext, packageInfo)
                        Log.d("Upgrade task", "${packageInfo.packageName} DONE")
                    }
                }
            }

        Result.success()
    }

    /**
     * Check if the installed time of a package is older than the UpdateAvailabilityPeriod setting
     */
    private fun isUpdateAvailable(
        updateAvailabilityPeriod: UpdateAvailabilityPeriod,
        lastUpdateTime: Long
    ): Boolean {
        if (updateAvailabilityPeriod == UpdateAvailabilityPeriod.NONE) {
            return false
        }

        val delay = updateAvailabilityPeriod.toDuration().toMillis()
        return (lastUpdateTime + delay) < System.currentTimeMillis()
    }

    /**
     * Install app by creating an install session and write the app's apk in it.
     *
     * TODO: This method should be a WorkManager worker running in the foreground
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun installApp(context: Context, packageInfo: PackageInfo) {
        val sessionInfo = appManager.getCurrentInstallSession(packageInfo.packageName)
            ?: appManager.getSessionInfo(
                appManager.createInstallSession(
                    appName = context.packageManager.getApplicationLabel(packageInfo.applicationInfo)
                        .toString(),
                    appPackage = packageInfo.packageName
                )
            )
            ?: return

        appManager.writeAndCommitSession(
            sessionId = sessionInfo.sessionId,
            apkInputStream = context.assets.open("${packageInfo.packageName}.apk"),
            isUpgrade = false
        )
    }
}
