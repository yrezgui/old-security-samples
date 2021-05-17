package com.samples.appinstaller.settings

import android.app.Application
import android.content.Context
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    val autoUpdateSchedule = context.appSettings.data.mapLatest { settings ->
        when (settings.autoUpdateSchedule) {
            AutoUpdateSchedule.MANUAL -> context.getString(R.string.auto_update_disabled)
            else -> {
                context.resources.getString(
                    R.string.auto_update_schedule,
                    settings.autoUpdateSchedule.toDuration().inMinutes
                )
            }
        }
    }.asLiveData()

    val updateAvailabilityPeriod = context.appSettings.data.mapLatest { settings ->
        when (settings.updateAvailabilityPeriod) {
            UpdateAvailabilityPeriod.NONE -> context.getString(R.string.update_availability_disabled)
            UpdateAvailabilityPeriod.AFTER_30_SECONDS -> {
                context.resources.getString(
                    R.string.update_availability_seconds_period,
                    settings.updateAvailabilityPeriod.toDuration().inSeconds.toInt()
                )
            }
            else -> {
                context.resources.getString(
                    R.string.update_availability_minutes_period,
                    settings.updateAvailabilityPeriod.toDuration().inMinutes.toInt()
                )
            }
        }
    }.asLiveData()

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

    fun triggerAutoUpdating(view: View) {

    }

    fun triggerAddingUpdates(view: View) {

    }
}