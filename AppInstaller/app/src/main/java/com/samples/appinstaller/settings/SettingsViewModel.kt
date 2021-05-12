package com.samples.appinstaller.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.AppSettings
import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

@ExperimentalCoroutinesApi
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    private val settingsStore: DataStore<AppSettings> by lazy {
        DataStoreFactory.create(
            serializer = AppSettingsSerializer
        ) { File(context.filesDir, PROTO_STORE_FILE_NAME) }
    }

    val autoUpdateSchedule = settingsStore.data.mapLatest { settings ->
        when (settings.autoUpdateSchedule) {
            AutoUpdateSchedule.MANUAL -> context.getString(R.string.auto_update_disabled)
            else -> context.resources.getQuantityString(
                R.plurals.auto_update_schedule,
                settings.autoUpdateScheduleValue
            )
        }
    }.asLiveData()

    val updateAvailabilityPeriod = settingsStore.data.mapLatest { settings ->
        when (settings.updateAvailabilityPeriod) {
            UpdateAvailabilityPeriod.NONE -> context.getString(R.string.update_availability_disabled)
            else -> context.resources.getQuantityString(
                R.plurals.auto_update_schedule,
                settings.autoUpdateScheduleValue
            )
        }
    }.asLiveData()

    fun setAutoUpdateSchedule(value: Int) {
        viewModelScope.launch {
            settingsStore.updateData { currentSettings ->
                currentSettings.toBuilder()
                    .setAutoUpdateScheduleValue(value)
                    .build()
            }
        }
    }

    fun setUpdateAvailabilityPeriod(value: Int) {
        viewModelScope.launch {
            settingsStore.updateData { currentSettings ->
                currentSettings.toBuilder()
                    .setUpdateAvailabilityPeriodValue(value)
                    .build()
            }
        }
    }
}