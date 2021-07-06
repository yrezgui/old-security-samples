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
import android.content.Intent.ACTION_PACKAGE_ADDED
import android.content.Intent.ACTION_PACKAGE_REMOVED
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavDeepLinkBuilder
import androidx.room.Room
import com.samples.appinstaller.AppDatabase
import com.samples.appinstaller.AppInstallerApplication
import com.samples.appinstaller.DATABASE_NAME
import com.samples.appinstaller.NotificationRepository
import com.samples.appinstaller.R
import com.samples.appinstaller.SyncEvent
import com.samples.appinstaller.SyncEventType
import com.samples.appinstaller.apps.SampleStoreDB
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

const val INSTALL_INTENT_NAME = "appinstaller_install_status"
const val INSTALL_CHANNEL_ID = "install"
const val INSTALL_NOTIFICATION_ID = 1
const val INSTALL_NOTIFICATION_TAG = "install"

const val UNINSTALL_INTENT_NAME = "appinstaller_uninstall_status"

const val UPGRADE_INTENT_NAME = "appinstaller_upgrade_status"
const val UPGRADE_CHANNEL_ID = "upgrade"
const val UPGRADE_NOTIFICATION_ID = 3
const val UPGRADE_NOTIFICATION_TAG = "upgrade_single_app"

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
            val appContext = context.applicationContext as AppInstallerApplication

            when (action) {
                ACTION_PACKAGE_ADDED -> onPackageAddedBroadcast(appContext, intent)
                ACTION_PACKAGE_REMOVED -> onPackageRemovedBroadcast(appContext, intent)
                INSTALL_INTENT_NAME -> onInstallBroadcast(appContext, status, extras)
                UNINSTALL_INTENT_NAME -> onUninstallBroadcast(appContext, status, extras)
                UPGRADE_INTENT_NAME -> onUpgradeBroadcast(appContext, status, extras)
            }
        }
    }

    /**
     * Update library UI when a added package broadcast is sent by the system
     */
    private fun onPackageAddedBroadcast(appContext: AppInstallerApplication, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        // We ignore the broadcast if the app isn't part of our store
        SampleStoreDB[packageName] ?: return

        GlobalScope.launch {
            // We send a sync event to update our library UI
            appContext.emitSyncEvent(SyncEvent(SyncEventType.INSTALL_SUCCESS, packageName))
        }
    }

    /**
     * Update library UI when a removed package broadcast is sent by the system
     *
     * Helps us to maintain up to date library in case the user uninstalls one of our apps from the
     * system settings
     */
    private fun onPackageRemovedBroadcast(appContext: AppInstallerApplication, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        // We ignore the broadcast if the app isn't part of our store
        SampleStoreDB[packageName] ?: return

        GlobalScope.launch {
            // We send a sync event to update our library UI
            appContext.emitSyncEvent(SyncEvent(SyncEventType.UNINSTALL_SUCCESS, packageName))
        }
    }

    private fun onInstallBroadcast(appContext: AppInstallerApplication, status: Int, extras: Bundle) {
        val sessionId = extras.getInt(PackageInstaller.EXTRA_SESSION_ID)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?

                Log.d("InstallReceiver", "STATUS_PENDING_USER_ACTION")

                // Our app is currently used by the user, so we can show the install dialog
                if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    confirmIntent?.flags = FLAG_ACTIVITY_NEW_TASK
                    appContext.startActivity(confirmIntent)
                }
                // In this case, our app is paused or closed, so it's better to a show an install
                // notification first rather than showing the install dialog which would be
                // confusing as it will appear without explanation
                else {
                    if (confirmIntent != null) {
                        showInstallNotification(
                            appContext = appContext,
                            sessionId = sessionId,
                            confirmIntent = confirmIntent
                        )
                    }
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                val packageName = extras.getString(PackageInstaller.EXTRA_PACKAGE_NAME) ?: return

                GlobalScope.launch {
                    // We remove the saved session ID from our database as the installation is done
                    removeSessionFromDB(appContext, packageName)
                }
            }

            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val packageName = extras.getString(PackageInstaller.EXTRA_PACKAGE_NAME) ?: return

                GlobalScope.launch {
                    // We remove the saved session ID from our database as the install has failed
                    removeSessionFromDB(appContext, packageName)

                    // We send a sync event to update our library UI
                    appContext.emitSyncEvent(SyncEvent(SyncEventType.INSTALL_FAILURE, packageName))
                }
            }

            else -> {
            }
        }
    }

    /**
     * Create notification for the user to trigger the install action as it requires explicit
     * interaction
     */
    private fun showInstallNotification(appContext: AppInstallerApplication, sessionId: Int, confirmIntent: Intent) {
        val notificationRepository = NotificationRepository(appContext)
        val sessionInfo =
            appContext.packageManager.packageInstaller.getSessionInfo(sessionId) ?: return
        val existingNotifications =
            notificationRepository.getActiveNotificationsByTag(INSTALL_NOTIFICATION_TAG)

        createNotificationChannel(
            context = appContext,
            id = INSTALL_CHANNEL_ID,
            name = appContext.getString(R.string.install_notification_channel)
        )

        if (existingNotifications.isNotEmpty()) {
            val pendingIntent = NavDeepLinkBuilder(appContext)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.navigation_library)
                .createPendingIntent()

            notificationRepository.notify(
                id = INSTALL_NOTIFICATION_ID,
                tag = INSTALL_NOTIFICATION_TAG,
                title = appContext.getString(R.string.multiple_install_notification_title),
                description = appContext.getString(R.string.multiple_install_notification_description),
                channel = INSTALL_CHANNEL_ID,
                contentIntent = pendingIntent
            )
        } else {
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                1,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            notificationRepository.notify(
                id = INSTALL_NOTIFICATION_ID,
                tag = INSTALL_NOTIFICATION_TAG,
                title = appContext.getString(R.string.install_notification_title),
                description = appContext.getString(
                    R.string.install_notification_description,
                    sessionInfo.appLabel.toString()
                ),
                channel = INSTALL_CHANNEL_ID,
                contentIntent = pendingIntent
            )
        }
    }

    private fun onUninstallBroadcast(appContext: AppInstallerApplication, status: Int, extras: Bundle) {
        when(status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?

                confirmIntent?.let {
                    Log.d("UninstallReceiver", "STATUS_PENDING_USER_ACTION")

                    // Our app is currently used by the user, so we can show the uninstall dialog
                    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        confirmIntent.flags = FLAG_ACTIVITY_NEW_TASK
                        appContext.startActivity(confirmIntent)
                    }
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                val packageName = extras.getString(PackageInstaller.EXTRA_PACKAGE_NAME) ?: return

                GlobalScope.launch {
                    // We remove any saved session ID linked with this package name from our
                    // database as the app has been uninstalled and the session isn't useful anymore
                    removeSessionFromDB(appContext, packageName)
                }
            }

            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val packageName = extras.getString(PackageInstaller.EXTRA_PACKAGE_NAME) ?: return

                GlobalScope.launch {
                    // We remove the saved session ID from our database as the uninstall has failed
                    removeSessionFromDB(appContext, packageName)

                    // We send a sync event to update our library UI
                    appContext.emitSyncEvent(SyncEvent(SyncEventType.INSTALL_FAILURE, packageName))
                }
            }

            else -> {}
        }
    }

    private fun onUpgradeBroadcast(appContext: AppInstallerApplication, status: Int, extras: Bundle) {
        val sessionId = extras.getInt(PackageInstaller.EXTRA_SESSION_ID)

        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) return

        val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?

        // Our app is currently used by the user, so we can show the install dialog
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            confirmIntent?.flags = FLAG_ACTIVITY_NEW_TASK
            appContext.startActivity(confirmIntent)
        }
        // In this case, our app is paused or closed, so it's better to a show an upgrade
        // notification first rather than showing the upgrade dialog which would be
        // confusing as it will appear without explanation
        else {
            if (confirmIntent != null) {
                showUpgradeNotification(
                    context = appContext,
                    sessionId = sessionId,
                    confirmIntent = confirmIntent
                )
            }
        }
    }

    private suspend fun removeSessionFromDB(context: Context, packageName: String) {
        val appDB = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()

        // We remove the saved session ID from our database as it's not needed anymore
        appDB.sessionDao().deleteByPackageName(packageName)
    }

    /**
     * Create notification for the user to trigger the upgrade action as it requires explicit
     * interaction
     */
    private fun showUpgradeNotification(context: Context, sessionId: Int, confirmIntent: Intent) {
        createNotificationChannel(
            context = context,
            id = UPGRADE_CHANNEL_ID,
            name = context.getString(R.string.upgrade_notification_channel)
        )

        val notificationRepository = NotificationRepository(context)
        val sessionInfo =
            context.packageManager.packageInstaller.getSessionInfo(sessionId) ?: return
        val existingNotifications =
            notificationRepository.getActiveNotificationsByTag(UPGRADE_NOTIFICATION_TAG)

        if (existingNotifications.isNotEmpty()) {
            val pendingIntent = NavDeepLinkBuilder(context)
                .setGraph(R.navigation.mobile_navigation)
                .setDestination(R.id.navigation_library)
                .createPendingIntent()

            notificationRepository.notify(
                id = UPGRADE_NOTIFICATION_ID,
                tag = UPGRADE_NOTIFICATION_TAG,
                title = context.getString(R.string.multiple_upgrades_notification_title),
                description = context.getString(R.string.multiple_upgrades_notification_description),
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
                tag = UPGRADE_NOTIFICATION_TAG,
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
     * Create a notification channel
     */
    private fun createNotificationChannel(context: Context, id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationRepository(context)

            notificationManager.createChannel(
                id = id,
                name = name,
                description = ""
            )
        }
    }
}
