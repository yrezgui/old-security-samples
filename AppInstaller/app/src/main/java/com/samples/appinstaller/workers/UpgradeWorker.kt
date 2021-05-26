/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.appinstaller.workers

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.apps.AppRepository
import com.samples.appinstaller.settings.appSettings
import com.samples.appinstaller.settings.toDuration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

const val UPGRADE_WORKER_TAG = "upgrade"

class UpgradeWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val appManager = AppRepository(applicationContext)

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
