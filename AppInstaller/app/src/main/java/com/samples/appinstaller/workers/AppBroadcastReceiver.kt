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
import com.samples.appinstaller.R
import com.samples.appinstaller.appManager.AppManager
import com.samples.appinstaller.notificationManager.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val EXTRA_SESSION_ID_KEY = "android.content.pm.extra.SESSION_ID"
const val EXTRA_PACKAGE_NAME_KEY = "android.content.pm.extra.PACKAGE_NAME"

const val INSTALL_CHANNEL_ID = "install"
const val UNINSTALL_CHANNEL_ID = "uninstall"
const val APP_NOTIFICATION_ID = 1

const val INSTALL_INTENT_NAME = "appinstaller_install_status"
const val UNINSTALL_INTENT_NAME = "appinstaller_uninstall_status"
const val UPGRADE_INTENT_NAME = "appinstaller_upgrade_status"

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
            }
        }
    }

    private fun handleInstallBroadcast(context: Context, status: Int, extras: Bundle) {

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
                            sessionId = extras.getInt(EXTRA_SESSION_ID_KEY),
                            confirmIntent = confirmIntent
                        )
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
            }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
            }
        }
    }

    /**
     * Create notification for the user to trigger the install action as it requires explicit
     * interaction
     */
    private fun showInstallNotification(context: Context, sessionId: Int, confirmIntent: Intent) {
        createInstallNotificationChannel(context)

        context.packageManager.packageInstaller.getSessionInfo(sessionId)?.let { sessionInfo ->
            val notificationManager = NotificationManager(context)

            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            notificationManager.createAndShowNotification(
                id = APP_NOTIFICATION_ID,
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
            val notificationManager = NotificationManager(context)
            val name = context.getString(R.string.install_notification_channel)

            notificationManager.createChannel(
                id = UNINSTALL_CHANNEL_ID,
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
                                AppManager(context).getPackageInfo(packageId)?.let { packageInfo ->
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
            PackageInstaller.STATUS_SUCCESS -> {
            }
            else -> {
            }
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
        val notificationManager = NotificationManager(context)

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        withContext(Dispatchers.IO) {
            notificationManager.createAndShowNotification(
                id = APP_NOTIFICATION_ID,
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
            val notificationManager = NotificationManager(context)
            val name = context.getString(R.string.uninstall_notification_channel)

            notificationManager.createChannel(
                id = UNINSTALL_CHANNEL_ID,
                name = name,
                description = ""
            )
        }
    }
}