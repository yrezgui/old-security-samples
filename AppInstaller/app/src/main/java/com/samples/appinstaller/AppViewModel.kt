/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.appinstaller

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_REPLACING
import android.content.pm.PackageInstaller
import android.util.Log
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.apps.AppPackage
import com.samples.appinstaller.apps.AppRepository
import com.samples.appinstaller.apps.AppStatus
import com.samples.appinstaller.apps.SampleStoreDB
import com.samples.appinstaller.settings.appSettings
import com.samples.appinstaller.settings.toDuration
import com.samples.appinstaller.workers.EXTRA_PACKAGE_NAME_KEY
import com.samples.appinstaller.workers.INSTALL_INTENT_NAME
import com.samples.appinstaller.workers.UNINSTALL_INTENT_NAME
import com.samples.appinstaller.workers.UPGRADE_INTENT_NAME
import com.samples.appinstaller.workers.UPGRADE_WORKER_TAG
import com.samples.appinstaller.workers.UpgradeWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

typealias LibraryEntries = Map<String, AppPackage>

const val DOWNLOADING_DELAY = 3000L

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val appRepository: AppRepository by lazy { AppRepository(context) }

    val isPermissionGranted: Boolean
        get() = appRepository.canRequestPackageInstalls()

    val appSettings: LiveData<AppSettings> = context.appSettings.data.asLiveData()

    private val _library = MutableLiveData<LibraryEntries>(emptyMap())
    val library: LiveData<LibraryEntries> = _library

    private val syncChannel = Channel<SyncAction>()

    val autoUpdateSchedule =
        context.appSettings.data.mapLatest { it.autoUpdateSchedule }.asLiveData()

    val updateAvailabilityPeriod =
        context.appSettings.data.mapLatest { it.updateAvailabilityPeriod }.asLiveData()

    init {
        viewModelScope.launch {
            loadLibrary()
            syncChannel.consumeEach(::syncLibrary)
        }
    }

    suspend fun loadLibrary() {
        val updatedLibrary = appRepository.getInstalledPackageMap()
            .filterKeys { SampleStoreDB.containsKey(it) }
            .mapValues {
                SampleStoreDB[it.key]!!.copy(
                    status = AppStatus.INSTALLED,
                    lastUpdateTime = it.value.lastUpdateTime
                )
            }

        _library.value = SampleStoreDB + updatedLibrary
    }

    /**
     * Sync app library and check if apps have been installed since last sync
     */
    private fun syncLibrary(action: SyncAction) {
        val library = library.value ?: return
        val app = library[action.appId] ?: return

        val updatedLibrary = mapOf(
            when (action.type) {
                SyncType.INSTALLING -> app.id to app.copy(status = AppStatus.INSTALLING)
                SyncType.INSTALL_SUCCESS -> app.id to app.copy(
                    status = AppStatus.INSTALLED,
                    lastUpdateTime = System.currentTimeMillis()
                )
                SyncType.INSTALL_FAILURE -> app.id to app.copy(status = AppStatus.UNINSTALLED)
                SyncType.UNINSTALL_SUCCESS -> app.id to app.copy(status = AppStatus.UNINSTALLED)
                SyncType.UNINSTALL_FAILURE -> app.id to app.copy(status = AppStatus.INSTALLED)
            }
        )

        _library.value = library + updatedLibrary
    }

    fun openApp(appId: String) {
        appRepository.openApp(appId)
    }

    fun uninstallApp(appId: String) {
        appRepository.uninstallApp(appId)
    }

    /**
     * Install app by creating an install session and write the app's apk in it.
     *
     * TODO: This method should be a WorkManager worker running in the foreground
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun installApp(appId: String, appName: String) {
        viewModelScope.launch {
            val sessionInfo = appRepository.getCurrentInstallSession(appId)
                ?: appRepository.getSessionInfo(appRepository.createInstallSession(appName, appId))
                ?: return@launch

            // We're updating the library entry to show a progress bar
            syncChannel.send(SyncAction(SyncType.INSTALLING, appId))

            // We fake a delay to show active work. This would be replaced by real APK download
            delay(DOWNLOADING_DELAY)

            appRepository.writeAndCommitSession(
                sessionId = sessionInfo.sessionId,
                apkInputStream = context.assets.open("$appId.apk"),
                isUpgrade = false
            )
        }
    }

    /**
     * Upgrade app by creating an install session with a different intent filter from the normal
     * install flow and write the app's apk in it
     *
     * TODO: This method should be a WorkManager worker running in the foreground
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun upgradeApp(appId: String, appName: String) {
        viewModelScope.launch {
            val sessionInfo = appRepository.getCurrentInstallSession(appId)
                ?: appRepository.getSessionInfo(appRepository.createInstallSession(appName, appId))
                ?: return@launch

            // We're updating the library entry to show a progress bar
            syncChannel.send(SyncAction(SyncType.INSTALLING, appId))

            // We fake a delay to show active work. This would be replaced by real APK download
            delay(DOWNLOADING_DELAY)

            appRepository.writeAndCommitSession(
                sessionId = sessionInfo.sessionId,
                apkInputStream = context.assets.open("$appId.apk"),
                isUpgrade = true
            )
        }
    }

    /**
     * Setter for the [AutoUpdateSchedule] setting
     */
    fun setAutoUpdateSchedule(value: Int) {
        viewModelScope.launch {
            val autoUpdateSchedule = context.appSettings
                .updateData { currentSettings ->
                    currentSettings.toBuilder().setAutoUpdateScheduleValue(value).build()
                }
                .autoUpdateSchedule

            // Cancel previous periodic task
            WorkManager.getInstance(context).cancelAllWorkByTag(UPGRADE_WORKER_TAG)

            if (autoUpdateSchedule != AutoUpdateSchedule.MANUAL) {
                // Schedule new one based on schedule
                PeriodicWorkRequestBuilder<UpgradeWorker>(autoUpdateSchedule.toDuration())
                    .addTag(UPGRADE_WORKER_TAG)
                    .build()
            }
        }
    }

    /**
     * Setter for the [UpdateAvailabilityPeriod] setting
     */
    fun setUpdateAvailabilityPeriod(value: Int) {
        viewModelScope.launch {
            context.appSettings.updateData { currentSettings ->
                currentSettings.toBuilder()
                    .setUpdateAvailabilityPeriodValue(value)
                    .build()
            }
        }
    }

    fun triggerAutoUpdating(@Suppress("UNUSED_PARAMETER") view: View) {
        val upgradeWorkRequest = OneTimeWorkRequestBuilder<UpgradeWorker>().build()
        WorkManager.getInstance(context).enqueue(upgradeWorkRequest)
    }

    /**
     * Monitor install sessions progress
     */
    val packageInstallCallback = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action

            Log.d("packageInstallCallback", "action: $action")

            viewModelScope.launch {
                when (action) {
                    // Install broadcast triggered by the system
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val packageName = intent.data?.schemeSpecificPart ?: return@launch
                        // We ignore the broadcast if the app isn't part of our store
                        SampleStoreDB[packageName] ?: return@launch
                        syncChannel.send(SyncAction(SyncType.INSTALL_SUCCESS, packageName))
                    }
                    // Uninstall broadcast triggered by the system
                    // Helps us to maintain up to date library in case the user uninstalls one of
                    // our app from the system settings
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!intent.getBooleanExtra(EXTRA_REPLACING, false)) {
                            val packageName = intent.data?.schemeSpecificPart ?: return@launch
                            // We ignore the broadcast if the app isn't part of our store
                            SampleStoreDB[packageName] ?: return@launch
                            syncChannel.send(SyncAction(SyncType.UNINSTALL_SUCCESS, packageName))
                        }
                    }
                    // Broadcast triggered by our app
                    INSTALL_INTENT_NAME,
                    UPGRADE_INTENT_NAME,
                    UNINSTALL_INTENT_NAME -> {
                        val status =
                            intent.extras?.getInt(PackageInstaller.EXTRA_STATUS) ?: return@launch
                        val packageName = intent.extras?.getString(EXTRA_PACKAGE_NAME_KEY)
                            ?: intent.data?.schemeSpecificPart
                            ?: return@launch
                        onInternalBroadcast(status, packageName)
                    }
                }
            }
        }

        suspend fun onInternalBroadcast(status: Int, packageName: String) {
            when (status) {
                // We monitor user cancellation or system failure of these actions within our app
                PackageInstaller.STATUS_FAILURE,
                PackageInstaller.STATUS_FAILURE_ABORTED,
                PackageInstaller.STATUS_FAILURE_BLOCKED,
                PackageInstaller.STATUS_FAILURE_CONFLICT,
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                PackageInstaller.STATUS_FAILURE_INVALID,
                PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    syncChannel.send(SyncAction(SyncType.INSTALL_FAILURE, packageName))
                }
                else -> {
                }
            }
        }
    }
}

enum class SyncType {
    INSTALLING, INSTALL_SUCCESS, INSTALL_FAILURE, UNINSTALL_SUCCESS, UNINSTALL_FAILURE
}

data class SyncAction(val type: SyncType, val appId: String)
