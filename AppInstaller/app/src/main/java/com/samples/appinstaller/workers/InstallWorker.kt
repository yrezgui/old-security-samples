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

import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.samples.appinstaller.AppDatabase
import com.samples.appinstaller.AppInstallerApplication
import com.samples.appinstaller.DATABASE_NAME
import com.samples.appinstaller.R
import com.samples.appinstaller.SyncEvent
import com.samples.appinstaller.SyncEventType
import com.samples.appinstaller.apps.InstallSession
import com.samples.appinstaller.apps.SampleStoreDB
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class InstallWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORKER_TAG = "install"
        const val PACKAGE_ID = "package_id"
        const val PACKAGE_NAME = "package_name"
        const val PACKAGE_LABEL = "package_label"
    }

    override suspend fun doWork(): Result = coroutineScope {
        val appContext = applicationContext as AppInstallerApplication

        val packageId = inputData.getInt(PACKAGE_ID, -1)
        if (packageId == -1) {
            return@coroutineScope Result.failure()
        }

        val packageName =
            inputData.getString(PACKAGE_NAME) ?: return@coroutineScope Result.failure()
        val packageLabel =
            inputData.getString(PACKAGE_LABEL) ?: return@coroutineScope Result.failure()

        // Verify the package name exists in our app database
        if (!SampleStoreDB.containsKey(packageName)) {
            return@coroutineScope Result.failure()
        }

        // We send a sync event to update our library UI
        appContext.emitSyncEvent(SyncEvent(SyncEventType.INSTALLING, packageName))

        // Set worker as a foreground service
        setForeground(createForegroundInfo(packageId, packageLabel))
        val appDB = Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME).build()

        // We fake a delay to show active work. This would be replaced by real APK download
        delay(3000L)

        val sessionId = PackageInstallerUtils.createInstallSession(
            appContext,
            packageName,
            packageLabel
        )

        @Suppress("BlockingMethodInNonBlockingContext")
        PackageInstallerUtils.writeSession(
            context = appContext,
            sessionId = sessionId,
            apkInputStream = appContext.assets.open("$packageName.apk")
        )

        appDB.sessionDao().insert(InstallSession(packageName, sessionId))

        PackageInstallerUtils.commitSession(
            context = appContext,
            sessionId = sessionId,
            intent = Intent(INSTALL_INTENT_NAME).apply {
                setPackage(appContext.packageName)
            }
        )

        Result.success()
    }

    /**
     * Creates an instance of ForegroundInfo which can be used to update the ongoing notification.
     */
    private fun createForegroundInfo(notificationId: Int, packageLabel: String): ForegroundInfo {
        val id = WORKER_TAG
        val title =
            applicationContext.getString(R.string.installing_notification_title, packageLabel)
        val cancel = applicationContext.getString(R.string.installing_notification_cancel_label)

        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = applicationContext.getString(R.string.install_notification_channel)
            val channel = NotificationChannel(id, channelName, IMPORTANCE_LOW).apply {
                this.description = "description"
            }

            NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(notificationId, notification)
    }
}
