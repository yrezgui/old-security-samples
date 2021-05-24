package com.samples.appinstaller

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.appManager.AppManager
import com.samples.appinstaller.appManager.AppPackage
import com.samples.appinstaller.appManager.AppStatus
import com.samples.appinstaller.appManager.SampleStoreDB
import com.samples.appinstaller.workers.EXTRA_PACKAGE_NAME_KEY
import com.samples.appinstaller.workers.INSTALL_INTENT_NAME
import com.samples.appinstaller.workers.UNINSTALL_INTENT_NAME
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

typealias LibraryEntries = Map<String, AppPackage>

class InstallerViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val appManager: AppManager by lazy { AppManager(context) }

    val isPermissionGranted: Boolean
        get() = appManager.canRequestPackageInstalls()

    private val _appSettings = MutableLiveData(AppSettings.getDefaultInstance())
    val appSettings: LiveData<AppSettings> = _appSettings

    private val _library = MutableLiveData<LibraryEntries>(emptyMap())
    val library: LiveData<LibraryEntries> = _library

    private val syncChannel = Channel<SyncAction>()

    init {
        viewModelScope.launch {
            loadLibrary()
            syncChannel.consumeEach(::syncLibrary)
        }
    }

    suspend fun loadLibrary() {
        val updatedLibrary = appManager.getInstalledPackageMap()
            .filterKeys { SampleStoreDB.containsKey(it) }
            .mapValues {
                SampleStoreDB[it.key]!!.copy(
                    status = AppStatus.INSTALLED,
                    lastUpdateTime = it.value.lastUpdateTime
                )
            }

        _library.value = SampleStoreDB + updatedLibrary
    }

    private fun syncLibrary(action: SyncAction) {
        val library = library.value ?: return
        val app = library[action.appId] ?: return

        val updatedLibrary = mapOf(
            when (action.type) {
                SyncType.INSTALLING -> app.id to app.copy(status = AppStatus.INSTALLING)
                SyncType.INSTALL_SUCCESS -> app.id to app.copy(status = AppStatus.INSTALLED)
                SyncType.UNINSTALL_SUCCESS -> app.id to app.copy(status = AppStatus.UNINSTALLED)
                SyncType.INSTALL_FAILURE -> TODO()
                SyncType.UNINSTALL_FAILURE -> TODO()
            }
        )

        _library.value = library + updatedLibrary
    }


    fun openApp(appId: String) {
        appManager.openApp(appId)
    }

    fun uninstallApp(appId: String) {
        appManager.uninstallApp(appId)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun installApp(appId: String, appName: String) {
        viewModelScope.launch {
            val sessionInfo = appManager.getCurrentInstallSession(appId)
                ?: appManager.getSessionInfo(appManager.createInstallSession(appName, appId))
                ?: return@launch


            syncChannel.send(SyncAction(SyncType.INSTALLING, appId))
            delay(5000L)

            appManager.writeAndCommitSession(
                sessionId = sessionInfo.sessionId,
                apkInputStream = context.assets.open("${appId}.apk")
            )
        }
    }

    val packageInstallCallback = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            val status = intent.extras?.getInt(PackageInstaller.EXTRA_STATUS) ?: return
            val appId = intent.extras?.getString(EXTRA_PACKAGE_NAME_KEY) ?: return

            viewModelScope.launch {
                when (action) {
                    INSTALL_INTENT_NAME -> onInstall(status, appId)
                    UNINSTALL_INTENT_NAME -> onUninstall(status, appId)
                }
            }
        }

        suspend fun onInstall(status: Int, appId: String) {
            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    syncChannel.send(SyncAction(SyncType.INSTALL_SUCCESS, appId))
                }
                PackageInstaller.STATUS_FAILURE,
                PackageInstaller.STATUS_FAILURE_ABORTED,
                PackageInstaller.STATUS_FAILURE_BLOCKED,
                PackageInstaller.STATUS_FAILURE_CONFLICT,
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                PackageInstaller.STATUS_FAILURE_INVALID,
                PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    syncChannel.send(SyncAction(SyncType.INSTALL_FAILURE, appId))
                }
            }
        }

        suspend fun onUninstall(status: Int, appId: String) {
            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    syncChannel.send(SyncAction(SyncType.UNINSTALL_SUCCESS, appId))
                }
                PackageInstaller.STATUS_FAILURE,
                PackageInstaller.STATUS_FAILURE_ABORTED,
                PackageInstaller.STATUS_FAILURE_BLOCKED,
                PackageInstaller.STATUS_FAILURE_CONFLICT,
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                PackageInstaller.STATUS_FAILURE_INVALID,
                PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    syncChannel.send(SyncAction(SyncType.UNINSTALL_FAILURE, appId))
                }
            }
        }
    }
}

enum class SyncType {
    INSTALLING, INSTALL_SUCCESS, INSTALL_FAILURE, UNINSTALL_SUCCESS, UNINSTALL_FAILURE
}

data class SyncAction(val type: SyncType, val appId: String)