package com.samples.appinstaller

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.appManager.AppManager
import com.samples.appinstaller.appManager.AppPackage
import com.samples.appinstaller.appManager.SampleStoreDB
import com.samples.appinstaller.settings.appSettings
import com.samples.appinstaller.settings.toTemporalAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/**
 * Timer to check sync app list
 */
const val SYNC_TIMER = 3000L

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val appManager: AppManager by lazy { AppManager(context) }

    private val _appSettings = MutableLiveData(AppSettings.getDefaultInstance())
    private val appSettings: LiveData<AppSettings> = _appSettings

    private val _store = MutableLiveData(SampleStoreDB)
    val store: LiveData<Map<String, AppPackage>> = _store

    private val _library = MutableLiveData<Map<String, AppPackage>>(emptyMap())
    val library: LiveData<Map<String, AppPackage>> = _library

    private var syncAppListJob: Job = Job()

    init {
        viewModelScope.launch {
            _appSettings.postValue(context.appSettings.data.first())
            startSyncAppListJob()

            withContext(Dispatchers.IO) {
                context.appSettings.data.collect {
                    _appSettings.postValue(it)
                    startSyncAppListJob()
                }
            }
        }
    }

    /**
     * Start sync app list job app after cancelling existing one first
     */
    private fun startSyncAppListJob() {
        viewModelScope.launch {
            syncAppListJob.cancel()
            syncAppListJob = viewModelScope.launch {
                while (isActive) {
                    syncAppList()
                    delay(SYNC_TIMER)
                }
            }
        }
    }

    /**
     * Sync app list that updates store & library listing to check if they're installed and have
     * updates
     */
    private fun syncAppList() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val settings = appSettings.value ?: return@withContext
                val packages = appManager.getInstalledPackageMap()

                var storeListing = _store.value
                    ?.mapValues { it.value.copy(isInstalled = packages.containsKey(it.key)) }
                    ?: return@withContext

                Log.d("syncAppList", settings.updateAvailabilityPeriod.toString())

                if (settings.updateAvailabilityPeriod != UpdateAvailabilityPeriod.NONE) {
                    storeListing = storeListing.mapValues {
                        if (it.value.isInstalled) {
                            it.value.copy(
                                isUpdateAvailable = checkIfUpdateIsAvailable(
                                    it.value,
                                    packages[it.key]!!
                                )
                            )
                        } else {
                            it.value
                        }
                    }
                }

                _store.postValue(storeListing)
                _library.postValue(storeListing.filterValues { it.isInstalled })
            }
        }
    }

    /**
     * Sync app list that updates store & library listing to check if they're installed and have
     * updates
     */
    private fun checkIfUpdateIsAvailable(app: AppPackage, packageInfo: PackageInfo): Boolean {
        val now = Instant.now()

        val settings = appSettings.value ?: return false
        val updateAvailabilityPeriod = settings.updateAvailabilityPeriod.toTemporalAmount()

        if (!app.isInstalled) {
            return false
        }

        val between = Duration.between(Instant.ofEpochMilli(packageInfo.lastUpdateTime), now)

        val after = now.isAfter(
            Instant.ofEpochMilli(packageInfo.lastUpdateTime).plus(updateAvailabilityPeriod)
        )

        Log.d("syncAppList", "${app.id}: ${between.toMillis() / 1000}s  After: $after")

        return now.isAfter(
            Instant.ofEpochMilli(packageInfo.lastUpdateTime).plus(updateAvailabilityPeriod)
        )
    }

    /**
     * Setter for the [AutoUpdateSchedule] setting
     */
    fun setAutoUpdateSchedule(value: Int) {
        viewModelScope.launch {
            context.appSettings.updateData { currentSettings ->
                currentSettings.toBuilder()
                    .setAutoUpdateScheduleValue(value)
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
}