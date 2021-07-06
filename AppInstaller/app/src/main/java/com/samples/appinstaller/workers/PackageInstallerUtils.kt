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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.os.BuildCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object PackageInstallerUtils {
    /**
     * Create install session with the ability to skip user explicit consent if the system allows it
     */
    suspend fun createInstallSession(
        context: Context,
        packageName: String,
        packageLabel: String
    ): Int {
        return withContext(Dispatchers.IO) {
            val params = SessionParams(SessionParams.MODE_FULL_INSTALL)
                .apply {
                    setAppPackageName(packageName)
                    setAppLabel(packageLabel)
                }

            if (BuildCompat.isAtLeastS()) {
                params.setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.setInstallReason(PackageManager.INSTALL_REASON_USER)
            }

            @Suppress("BlockingMethodInNonBlockingContext")
            context.packageManager.packageInstaller.createSession(params)
        }
    }

    /**
     * Save APK in the provided session
     */
    suspend fun writeSession(context: Context, sessionId: Int, apkInputStream: InputStream) {
        withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            val session = context.packageManager.packageInstaller.openSession(sessionId)

            @Suppress("BlockingMethodInNonBlockingContext")
            session.openWrite("package", 0, -1).use { destination ->
                apkInputStream.copyTo(destination)
            }
        }
    }

    /**
     * Commit APK and seal the install session
     */
    suspend fun commitSession(context: Context, sessionId: Int, intent: Intent) {
        withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            val session = context.packageManager.packageInstaller.openSession(sessionId)

            val statusPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            session.commit(statusPendingIntent.intentSender)
        }
    }

    /**
     * Uninstall app
     */
    fun uninstallApp(context: Context, packageName: String) {
        val statusIntent = Intent(UNINSTALL_INTENT_NAME).apply {
            `package` = context.packageName
        }

        val statusPendingIntent = PendingIntent.getBroadcast(context, 0, statusIntent, 0)
        context.packageManager.packageInstaller.uninstall(
            packageName,
            statusPendingIntent.intentSender
        )
    }
}
