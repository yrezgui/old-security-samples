package com.samples.appinstaller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samples.appinstaller.databinding.FragmentInstallerBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InstallerFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var binding: FragmentInstallerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInstallerBinding.inflate(inflater, container, false)
        setupUi(binding)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        togglePermissionSection()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500L)
            viewModel.loadLibrary()
        }
    }

    private fun setupUi(binding: FragmentInstallerBinding) {
        binding.showRationaleButton.setOnClickListener { showRationale() }

        val adapter = InstallerRecyclerViewAdapter(
            list = emptyList(),
            timestamp = System.currentTimeMillis(),
            updateAvailabilityPeriod = viewModel.appSettings.value?.updateAvailabilityPeriod
                ?: AppSettings.getDefaultInstance().updateAvailabilityPeriod,
            listeners = object : LibraryEntryActionListeners {
                override fun openApp(appId: String) {
                    viewModel.openApp(appId)
                }

                override fun installApp(appId: String, appName: String) {
                    viewModel.installApp(appId, appName)
                }

                override fun upgradeApp(appId: String) {
                    TODO("Not yet implemented")
                }

                override fun uninstallApp(appId: String) {
                    viewModel.uninstallApp(appId)
                }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.library.observe(viewLifecycleOwner) {
            adapter.updateData(it.values.toList())
        }
    }

    private fun togglePermissionSection() {
        if (viewModel.isPermissionGranted) {
            binding.permissionSection.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        } else {
            binding.permissionSection.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
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

interface LibraryEntryActionListeners {
    fun openApp(appId: String)
    fun installApp(appId: String, appName: String)
    fun upgradeApp(appId: String)
    fun uninstallApp(appId: String)
}