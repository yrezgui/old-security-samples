package com.samples.appinstaller.data

import androidx.annotation.DrawableRes

data class AppPackage(
    val id: String,
    val name: String,
    val company: String,
    @DrawableRes val icon: Int
)
