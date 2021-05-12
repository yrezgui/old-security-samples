package com.samples.appinstaller.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samples.appinstaller.R
import com.samples.appinstaller.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private val viewModel: SettingsViewModel by viewModels()
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
        binding.viewmodel = viewModel
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
                    view.findFragment<SettingsFragment>().viewModel.setAutoUpdateSchedule(which)
                }
                .show()
        }

        fun showUpdateAvailabilityPeriod(view: View) {
            val context = view.context

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.update_availability_period_label))
                .setItems(context.resources.getStringArray(R.array.update_availability_period)) { _, which ->
                    view.findFragment<SettingsFragment>().viewModel.setUpdateAvailabilityPeriod(which)
                }
                .show()
        }
    }
}