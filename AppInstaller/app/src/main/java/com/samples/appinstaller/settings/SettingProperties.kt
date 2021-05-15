package com.samples.appinstaller.settings

import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod

fun getAutoUpdateScheduleInMinutes(value: AutoUpdateSchedule): Int {
    return when(value) {
        AutoUpdateSchedule.UNRECOGNIZED,
        AutoUpdateSchedule.MANUAL -> -1
        AutoUpdateSchedule.EVERY_15_MINUTES -> 1
        AutoUpdateSchedule.EVERY_30_MINUTES -> 10
        AutoUpdateSchedule.EVERY_60_MINUTES -> 60
    }
}

fun getUpdateAvailabilityPeriodQuantity(value: UpdateAvailabilityPeriod): Int {
    return when(value) {
        UpdateAvailabilityPeriod.UNRECOGNIZED,
        UpdateAvailabilityPeriod.NONE -> -1
        UpdateAvailabilityPeriod.AFTER_30_SECONDS -> 30
        UpdateAvailabilityPeriod.AFTER_5_MINUTES -> 5
        UpdateAvailabilityPeriod.AFTER_15_MINUTES -> 5
        UpdateAvailabilityPeriod.AFTER_30_MINUTES -> 10
        UpdateAvailabilityPeriod.AFTER_60_MINUTES -> 60
    }
}