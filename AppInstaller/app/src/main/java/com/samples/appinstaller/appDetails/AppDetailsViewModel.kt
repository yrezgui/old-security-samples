package com.samples.appinstaller.appDetails

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.appManager.AppManager
import com.samples.appinstaller.appManager.AppPackage
import com.samples.appinstaller.appManager.AppStatus
import com.samples.appinstaller.appManager.SampleStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val appManager = AppManager(context)
    lateinit var selectedApp: AppPackage

    private val _appStatus: MutableLiveData<AppStatus> = MutableLiveData(AppStatus.UNKNOWN)
    val appStatus: LiveData<AppStatus> = _appStatus


    private val _currentInstallSessionId: MutableLiveData<Int?> = MutableLiveData(null)
    val currentInstallSessionId: LiveData<Int?> = _currentInstallSessionId

    fun clearInstallSessionId() {
        _currentInstallSessionId.value = null
    }

    fun checkAppStatus() {
        if(currentInstallSessionId.value == null) {
            _appStatus.value = appManager.checkAppStatus(selectedApp.id)
            Log.d("AppSTATUS", _appStatus.value.toString())
        }
    }

    fun setAppStatus(appStatus: AppStatus) {
        Log.d("SET AppSTATUS", appStatus.toString())
        _appStatus.value = appStatus
    }

    fun loadApp(packageId: String) {
        selectedApp = SampleStore.find { it.id == packageId }!!
        checkAppStatus()
    }

    fun uninstallApp(@Suppress("UNUSED_PARAMETER") view: View) {
        appManager.uninstallApp(selectedApp.id)
    }

    fun installApp(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModelScope.launch {
            // Simulate downloading an APK
            _appStatus.value = AppStatus.DOWNLOADING
            delay(3000L)

            val sessionId = appManager.createInstallSession()
            _currentInstallSessionId.value = sessionId

            _appStatus.value = AppStatus.INSTALLING
            appManager.installApp(sessionId, context.assets.open("${selectedApp.id}.apk"))

            _appStatus.value = AppStatus.INSTALLED
        }
    }

    fun openApp(@Suppress("UNUSED_PARAMETER") view: View) {
        appManager.openApp(selectedApp.id)
    }
}