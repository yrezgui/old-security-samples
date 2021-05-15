package com.samples.appinstaller.library

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.appManager.AppManager
import com.samples.appinstaller.appManager.AppPackage
import com.samples.appinstaller.appManager.SampleStore
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val appManager: AppManager by lazy { AppManager(context) }

    private val _installedApps = MutableLiveData<List<AppPackage>>(emptyList())
    val installedApps: LiveData<List<AppPackage>> = _installedApps

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = fetchInstalledApps()
        }
    }

    private suspend fun fetchInstalledApps(): List<AppPackage> {
        return appManager.getInstalledApps().mapNotNull { SampleStore.find { appPackage -> it == appPackage.id } }
    }
}