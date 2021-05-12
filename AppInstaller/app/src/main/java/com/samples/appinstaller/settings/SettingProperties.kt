package com.samples.appinstaller.settings

import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod

fun getAutoUpdateScheduleInMinutes(value: AutoUpdateSchedule): Int {
    return when(value) {
        AutoUpdateSchedule.UNRECOGNIZED,
        AutoUpdateSchedule.MANUAL -> -1
        AutoUpdateSchedule.EVERY_1_MINUTE -> 1
        AutoUpdateSchedule.EVERY_5_MINUTES -> 5
        AutoUpdateSchedule.EVERY_10_MINUTES -> 10
        AutoUpdateSchedule.EVERY_60_MINUTES -> 60
    }
}

fun getUpdateAvailabilityPeriodInMinutes(value: UpdateAvailabilityPeriod): Int {
    return when(value) {
        UpdateAvailabilityPeriod.UNRECOGNIZED,
        UpdateAvailabilityPeriod.NONE -> -1
        UpdateAvailabilityPeriod.AFTER_1_MINUTE -> 1
        UpdateAvailabilityPeriod.AFTER_5_MINUTES -> 5
        UpdateAvailabilityPeriod.AFTER_10_MINUTES -> 10
        UpdateAvailabilityPeriod.AFTER_60_MINUTES -> 60
    }
}