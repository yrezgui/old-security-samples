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
