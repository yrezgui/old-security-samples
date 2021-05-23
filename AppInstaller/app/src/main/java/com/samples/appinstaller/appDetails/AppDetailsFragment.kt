package com.samples.appinstaller.appDetails

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.BuildConfig
import com.samples.appinstaller.appManager.AppPackage
import com.samples.appinstaller.appManager.AppStatus
import com.samples.appinstaller.databinding.FragmentAppDetailsBinding


class AppDetailsFragment : Fragment() {
    private val appViewModel: AppViewModel by activityViewModels()
    private val args: AppDetailsFragmentArgs by navArgs()
    private lateinit var binding: FragmentAppDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        appViewModel.selectApp(args.packageId)

        binding = FragmentAppDetailsBinding.inflate(inflater, container, false)
        setupUi()

        appViewModel.selectedApp.observe(viewLifecycleOwner) {
            it?.let { onStateChange(it) }
        }

        return binding.root
    }

    private fun setupUi() {
        binding.showRationaleButton.setOnClickListener { showRationale() }
        binding.installAppButton.setOnClickListener { appViewModel.installApp() }
        binding.uninstallAppButton.setOnClickListener { appViewModel.uninstallApp() }
        binding.openAppButton.setOnClickListener { appViewModel.openApp() }
    }

    private fun onStateChange(app: AppPackage?) {
        if (app == null) {
            binding.mainLayout.visibility = View.INVISIBLE
            return
        }

        binding.icon.setImageResource(app.icon)
        binding.appName.text = app.name
        binding.company.text = app.company
        binding.appStatus.text = app.status.toString()

        if (!appViewModel.isPermissionGranted) {
            binding.showRationaleButton.visibility = View.VISIBLE
            binding.installAppButton.visibility = View.GONE
            binding.installingSection.visibility = View.GONE
            binding.installProgressBar.visibility = View.GONE
            binding.installedSection.visibility = View.GONE

        } else {
            binding.showRationaleButton.visibility = View.GONE
            binding.installAppButton.visibility =
                if (app.status === AppStatus.UNINSTALLED) View.VISIBLE else View.GONE
            binding.installingSection.visibility =
                if (app.status === AppStatus.INSTALLING) View.VISIBLE else View.GONE
            binding.installProgressBar.visibility =
                if (app.status === AppStatus.INSTALLING) View.VISIBLE else View.GONE
            binding.installedSection.visibility =
                if (app.status === AppStatus.INSTALLED) View.VISIBLE else View.GONE

        }
    }

    private fun showRationale() {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle("Authorization")
                .setMessage("App Installer needs this special permission to be install other apps")
                .setPositiveButton("Ok") { _, _ -> requestPermission() }
                .setNegativeButton("Learn More") { _, _ -> openLearnMoreLink() }
                .show()
        }
    }

    private fun openLearnMoreLink() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data =
                Uri.parse("https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller")
        }

        startActivity(intent)
    }

    private fun requestPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        }

        startActivityForResult(intent, 1000)
    }
}