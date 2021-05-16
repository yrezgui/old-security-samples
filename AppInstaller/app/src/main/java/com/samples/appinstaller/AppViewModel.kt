package com.samples.appinstaller

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.samples.appinstaller.appManager.AppManager

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val appManager: AppManager by lazy { AppManager(context) }
}