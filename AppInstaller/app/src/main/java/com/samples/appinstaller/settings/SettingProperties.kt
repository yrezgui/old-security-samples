package com.samples.appinstaller.settings

import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

@ExperimentalTime
fun AutoUpdateSchedule.toDuration(): Duration {
    return when(this) {
        AutoUpdateSchedule.UNRECOGNIZED,
        AutoUpdateSchedule.MANUAL -> Duration.ZERO
        AutoUpdateSchedule.EVERY_15_MINUTES -> 15.minutes
        AutoUpdateSchedule.EVERY_30_MINUTES -> 10.minutes
        AutoUpdateSchedule.EVERY_60_MINUTES -> 60.minutes
    }
}

@ExperimentalTime
fun UpdateAvailabilityPeriod.toDuration(): Duration {
    return when(this) {
        UpdateAvailabilityPeriod.UNRECOGNIZED,
        UpdateAvailabilityPeriod.NONE -> Duration.ZERO
        UpdateAvailabilityPeriod.AFTER_30_SECONDS -> 30.seconds
        UpdateAvailabilityPeriod.AFTER_5_MINUTES -> 5.minutes
        UpdateAvailabilityPeriod.AFTER_15_MINUTES -> 5.minutes
        UpdateAvailabilityPeriod.AFTER_30_MINUTES -> 10.minutes
        UpdateAvailabilityPeriod.AFTER_60_MINUTES -> 60.minutes
    }
}