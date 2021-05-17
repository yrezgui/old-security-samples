package com.samples.appinstaller.library

import android.app.PendingIntent
import android.content.Context
import androidx.navigation.NavDeepLinkBuilder
import com.samples.appinstaller.R

fun createLibraryNavigationLink(context: Context): PendingIntent {
    return NavDeepLinkBuilder(context)
        .setGraph(R.navigation.mobile_navigation)
        .setDestination(R.id.navigation_library)
        .createPendingIntent()
}