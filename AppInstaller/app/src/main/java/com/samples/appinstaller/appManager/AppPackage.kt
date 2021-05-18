package com.samples.appinstaller.appManager

import androidx.annotation.DrawableRes

data class AppPackage(
    val id: String,
    val name: String,
    val company: String,
    @DrawableRes val icon: Int,
    val isInstalled: Boolean = false,
    val isUpdateAvailable: Boolean = false
)

data class InstalledApp(
    val app: AppPackage,
    val isUpdateAvailable: Boolean = false
)