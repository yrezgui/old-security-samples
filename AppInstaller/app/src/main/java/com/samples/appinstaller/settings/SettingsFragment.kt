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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.findFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.R
import com.samples.appinstaller.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private val appViewModel: AppViewModel by activityViewModels()
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_settings,
            container,
            false
        )

        binding.viewModel = appViewModel
        binding.settingsHandler = SettingsHandler()
        binding.lifecycleOwner = this

        return binding.root
    }

    class SettingsHandler {
        fun showAutoUpdateSchedule(view: View) {
            val context = view.context

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.auto_update_schedule_label))
                .setItems(context.resources.getStringArray(R.array.auto_update_schedule)) { _, which ->
                    view.findFragment<SettingsFragment>().appViewModel.setAutoUpdateSchedule(
                        which
                    )
                }
                .show()
        }

        fun showUpdateAvailabilityPeriod(view: View) {
            val context = view.context

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.update_availability_period_label))
                .setItems(context.resources.getStringArray(R.array.update_availability_period)) { _, which ->
                    view.findFragment<SettingsFragment>().appViewModel.setUpdateAvailabilityPeriod(
                        which
                    )
                }
                .show()
        }
    }
}
