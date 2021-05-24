package com.samples.appinstaller.settings

import com.samples.appinstaller.AppSettings.AutoUpdateSchedule
import com.samples.appinstaller.AppSettings.UpdateAvailabilityPeriod
import java.time.Duration

fun AutoUpdateSchedule.toDuration(): Duration {
    return when (this) {
        AutoUpdateSchedule.UNRECOGNIZED,
        AutoUpdateSchedule.MANUAL -> Duration.ZERO
        AutoUpdateSchedule.EVERY_15_MINUTES -> Duration.ofMinutes(15)
        AutoUpdateSchedule.EVERY_30_MINUTES -> Duration.ofMinutes(30)
        AutoUpdateSchedule.EVERY_60_MINUTES -> Duration.ofMinutes(60)
    }
}

fun UpdateAvailabilityPeriod.toDuration(): Duration {
    return when (this) {
        UpdateAvailabilityPeriod.UNRECOGNIZED,
        UpdateAvailabilityPeriod.NONE -> Duration.ZERO
        UpdateAvailabilityPeriod.AFTER_30_SECONDS -> Duration.ofSeconds(30)
        UpdateAvailabilityPeriod.AFTER_5_MINUTES -> Duration.ofMinutes(5)
        UpdateAvailabilityPeriod.AFTER_15_MINUTES -> Duration.ofMinutes(15)
        UpdateAvailabilityPeriod.AFTER_30_MINUTES -> Duration.ofMinutes(30)
        UpdateAvailabilityPeriod.AFTER_60_MINUTES -> Duration.ofMinutes(60)
    }
}