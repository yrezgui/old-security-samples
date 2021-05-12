package com.samples.appinstaller.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInstaller
import android.util.Log

class UninstallUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val extras = intent.extras!!
        val status = extras.getInt(PackageInstaller.EXTRA_STATUS)
        val message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)

        Log.d("UninstallReceiver", "status: $status // message: $message")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?
                confirmIntent?.flags = FLAG_ACTIVITY_NEW_TASK

                context?.startActivity(confirmIntent)
            }
            PackageInstaller.STATUS_SUCCESS -> { }
            else -> { }
        }
    }
}