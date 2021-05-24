package com.samples.appinstaller.apps

import androidx.annotation.DrawableRes

data class AppPackage(
    val id: String,
    val name: String,
    val company: String,
    @DrawableRes val icon: Int,
    val isUpdateAvailable: Boolean = false,
    val status: AppStatus = AppStatus.UNINSTALLED,
    val lastUpdateTime: Long = -1
)

enum class AppStatus {
    UNINSTALLED, INSTALLED, INSTALLING, UPGRADING
}