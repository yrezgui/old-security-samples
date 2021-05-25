package com.samples.appinstaller.settings

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import com.samples.appinstaller.R

@BindingAdapter("autoUpdateSchedule")
fun setAutoUpdateScheduleValueLabel(textView: TextView, autoUpdateSchedule: AutoUpdateSchedule) {
    textView.text = when (autoUpdateSchedule) {
        AutoUpdateSchedule.MANUAL -> textView.context.getString(R.string.auto_update_disabled)
        else -> textView.context.resources.getString(
            R.string.auto_update_schedule,
            autoUpdateSchedule.toDuration().toMinutes()
        )
    }
}

@BindingAdapter("updateAvailabilityPeriod")
fun setUpdateAvailabilityPeriodValueLabel(
    textView: TextView,
    updateAvailabilityPeriod: UpdateAvailabilityPeriod
) {
    textView.text = when (updateAvailabilityPeriod) {
        UpdateAvailabilityPeriod.NONE -> {
            textView.context.getString(R.string.update_availability_disabled)
        }
        UpdateAvailabilityPeriod.AFTER_30_SECONDS -> {
            textView.context.resources.getString(
                R.string.update_availability_seconds_period,
                updateAvailabilityPeriod.toDuration().seconds
            )
        }
        else -> {
            textView.context.resources.getString(
                R.string.update_availability_minutes_period,
                updateAvailabilityPeriod.toDuration().toMinutes()
            )
        }
    }
}