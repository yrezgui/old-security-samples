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

@ExperimentalCoroutinesApi
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = getApplication()

    val autoUpdateSchedule = context.appSettings.data.mapLatest { settings ->
        when (settings.autoUpdateSchedule) {
            AutoUpdateSchedule.MANUAL -> context.getString(R.string.auto_update_disabled)
            else -> {
                val quantity = getAutoUpdateScheduleInMinutes(settings.autoUpdateSchedule)

                context.resources.getQuantityString(
                    R.plurals.auto_update_schedule,
                    quantity,
                    quantity
                )
            }
        }
    }.asLiveData()

    val updateAvailabilityPeriod = context.appSettings.data.mapLatest { settings ->
        when (settings.updateAvailabilityPeriod) {
            UpdateAvailabilityPeriod.NONE -> context.getString(R.string.update_availability_disabled)
            else -> {
                val quantity =
                    getUpdateAvailabilityPeriodInMinutes(settings.updateAvailabilityPeriod)

                context.resources.getQuantityString(
                    R.plurals.update_availability_period,
                    quantity,
                    quantity
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