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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavDeepLinkBuilder
import com.samples.appinstaller.NotificationRepository
import com.samples.appinstaller.R
import com.samples.appinstaller.apps.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val EXTRA_SESSION_ID_KEY = "android.content.pm.extra.SESSION_ID"
const val EXTRA_PACKAGE_NAME_KEY = "android.content.pm.extra.PACKAGE_NAME"

const val SEND_INSTALL_UPDATES_PERMISSION =
    "com.samples.appinstaller.permission.SEND_INSTALLER_UPDATES"

const val INSTALL_INTENT_NAME = "appinstaller_install_status"
const val INSTALL_CHANNEL_ID = "install"
const val INSTALL_NOTIFICATION_ID = 1
const val INSTALL_NOTIFICATION_TAG = "install"

const val UNINSTALL_INTENT_NAME = "appinstaller_uninstall_status"
const val UNINSTALL_CHANNEL_ID = "uninstall"
const val UNINSTALL_NOTIFICATION_ID = 2
const val UNINSTALL_NOTIFICATION_TAG = "uninstall"

const val UPGRADE_INTENT_NAME = "appinstaller_upgrade_status"
const val UPGRADE_CHANNEL_ID = "upgrade"
const val UPGRADE_NOTIFICATION_ID = 3
const val UPGRADE_SINGLE_NOTIFICATION_TAG = "upgrade_single_app"

class AppBroadcastReceiver : BroadcastReceiver() {

    /**
     * Receives all app broadcast messages (install, uninstall and upgrade)
     */
    override fun onReceive(context: Context?, intent: Intent) {
        val extras = intent.extras!!
        val action = intent.action
        val status = extras.getInt(PackageInstaller.EXTRA_STATUS)
        val message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)

        Log.d("AppBroadcastReceiver", "action: $action || status: $status || message: $message")

        context?.let {
            when (action) {
                INSTALL_INTENT_NAME -> handleInstallBroadcast(context, status, extras)
                UNINSTALL_INTENT_NAME -> handleUninstallBroadcast(context, status, extras)
                UPGRADE_INTENT_NAME -> handleUpgradeBroadcast(context, status, extras)
            }
        }
    }

    private fun handleInstallBroadcast(context: Context, status: Int, extras: Bundle) {

        val sessionId = extras.getInt(EXTRA_SESSION_ID_KEY)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?

                Log.d("InstallReceiver", "STATUS_PENDING_USER_ACTION")

                // Our app is currently used by the user, so we can show the install dialog
                if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    confirmIntent?.flags = FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(confirmIntent)
                }
                // In this case, our app is paused or closed, so it's better to a show an install
                // notification first rather than showing the install dialog which would be
                // confusing as it will appear without explanation
                else {
                    if (confirmIntent != null) {
                        showInstallNotification(
                            context = context,
                            sessionId = sessionId,
                            confirmIntent = confirmIntent
                        )
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> { }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> { }
        }
    }

    /**
     * Create notification for the user to trigger the install action as it requires explicit
     * interaction
     */
    private fun showInstallNotification(context: Context, sessionId: Int, confirmIntent: Intent) {
        val notificationRepository = NotificationRepository(context)
        val sessionInfo =
            context.packageManager.packageInstaller.getSessionInfo(sessionId) ?: return
        val existingNotifications =
            notificationRepository.getActiveNotificationsByTag(UPGRADE_SINGLE_NOTIFICATION_TAG)

        if (existingNotifications.isNotEmpty()) {
            val pendingIntent = NavDeepLinkBuilder(context)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.navigation_library)
                .createPendingIntent()

            notificationRepository.notify(
                id = INSTALL_NOTIFICATION_ID,
                tag = INSTALL_NOTIFICATION_TAG,
                title = context.getString(R.string.multiple_install_notification_title),
                description = context.getString(
                    R.string.multiple_install_notification_description,
                    sessionInfo.appLabel.toString()
                ),
                channel = INSTALL_CHANNEL_ID,
                contentIntent = pendingIntent
            )
        } else {
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            notificationRepository.notify(
                id = INSTALL_NOTIFICATION_ID,
                tag = INSTALL_NOTIFICATION_TAG,
                title = context.getString(R.string.install_notification_title),
                description = context.getString(
                    R.string.install_notification_description,
                    sessionInfo.appLabel.toString()
                ),
                channel = INSTALL_CHANNEL_ID,
                contentIntent = pendingIntent
            )
        }
    }

    /**
     * Create channel for installation notifications
     */
    private fun createInstallNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationRepository(context)
            val name = context.getString(R.string.install_notification_channel)

            notificationManager.createChannel(
                id = INSTALL_CHANNEL_ID,
                name = name,
                description = ""
            )
        }
    }

    private fun handleUninstallBroadcast(context: Context, status: Int, extras: Bundle) {

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?

                confirmIntent?.let {
                    Log.d("UninstallReceiver", "STATUS_PENDING_USER_ACTION")

                    // Our app is currently used by the user, so we can show the uninstall dialog
                    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        confirmIntent.flags = FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(confirmIntent)
                    }
                    // In this case, our app is paused or closed, so it's better to a show an uninstall
                    // notification first rather than showing the install dialog which would be
                    // confusing as it will appear without explanation
                    else {
                        CoroutineScope(Dispatchers.IO).launch {
                            extras.getString(EXTRA_PACKAGE_NAME_KEY)?.let { packageId ->
                                AppRepository(context).getPackageInfo(packageId)
                                    ?.let { packageInfo ->
                                        showUninstallNotification(
                                            context = context,
                                            packageInfo = packageInfo,
                                            confirmIntent = confirmIntent
                                        )
                                    }
                            }
                        }
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> { }
            else -> { }
        }
    }

    /**
     * Create notification for the user to trigger the uninstall action as it requires explicit
     * interaction
     */
    private suspend fun showUninstallNotification(
        context: Context,
        packageInfo: PackageInfo,
        confirmIntent: Intent
    ) {
        createUninstallNotificationChannel(context)
        val notificationManager = NotificationRepository(context)

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        withContext(Dispatchers.IO) {
            notificationManager.notify(
                id = UNINSTALL_NOTIFICATION_ID,
                tag = UNINSTALL_NOTIFICATION_TAG,
                title = context.getString(R.string.uninstall_notification_title),
                description = context.getString(
                    R.string.uninstall_notification_description,
                    context.packageManager.getApplicationLabel(packageInfo.applicationInfo)
                ),
                channel = UNINSTALL_CHANNEL_ID,
                contentIntent = pendingIntent
            )
        }
    }

    /**
     * Create channel for uninstallation notifications
     */
    private fun createUninstallNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationRepository(context)
            val name = context.getString(R.string.uninstall_notification_channel)

            notificationManager.createChannel(
                id = UNINSTALL_CHANNEL_ID,
                name = name,
                description = ""
            )
        }
    }

    private fun handleUpgradeBroadcast(context: Context, status: Int, extras: Bundle) {
        val sessionId = extras.getInt(EXTRA_SESSION_ID_KEY)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?

                // Our app is currently used by the user, so we can show the install dialog
                if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    confirmIntent?.flags = FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(confirmIntent)
                }
                // In this case, our app is paused or closed, so it's better to a show an upgrade
                // notification first rather than showing the upgrade dialog which would be
                // confusing as it will appear without explanation
                else {
                    if (confirmIntent != null) {
                        showUpgradeNotification(
                            context = context,
                            sessionId = sessionId,
                            confirmIntent = confirmIntent
                        )
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> { }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {}
        }
    }

    /**
     * Create notification for the user to trigger the upgrade action as it requires explicit
     * interaction
     */
    private fun showUpgradeNotification(context: Context, sessionId: Int, confirmIntent: Intent) {
        createUpgradeNotificationChannel(context)

        val notificationRepository = NotificationRepository(context)
        val sessionInfo =
            context.packageManager.packageInstaller.getSessionInfo(sessionId) ?: return
        val existingNotifications =
            notificationRepository.getActiveNotificationsByTag(UPGRADE_SINGLE_NOTIFICATION_TAG)

        if (existingNotifications.isNotEmpty()) {
            val pendingIntent = NavDeepLinkBuilder(context)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.navigation_library)
                .createPendingIntent()

            notificationRepository.notify(
                id = UPGRADE_NOTIFICATION_ID,
                tag = UPGRADE_SINGLE_NOTIFICATION_TAG,
                title = context.getString(R.string.multiple_updates_notification_title),
                description = context.getString(
                    R.string.multiple_updates_notification_description,
                    sessionInfo.appLabel.toString()
                ),
                channel = UPGRADE_CHANNEL_ID,
                contentIntent = pendingIntent
            )
        } else {
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            notificationRepository.notify(
                id = UPGRADE_NOTIFICATION_ID,
                tag = UPGRADE_SINGLE_NOTIFICATION_TAG,
                title = context.getString(R.string.upgrade_notification_title),
                description = context.getString(
                    R.string.upgrade_notification_description,
                    sessionInfo.appLabel.toString()
                ),
                channel = UPGRADE_CHANNEL_ID,
                contentIntent = pendingIntent
            )
        }
    }

    /**
     * Create channel for upgrade notifications
     */
    private fun createUpgradeNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationRepository(context)
            val name = context.getString(R.string.install_notification_channel)

            notificationManager.createChannel(
                id = UPGRADE_CHANNEL_ID,
                name = name,
                description = ""
            )
        }
    }
}
