package com.samples.appinstaller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.samples.appinstaller.AppSettings
import com.samples.appinstaller.AppViewModel
import com.samples.appinstaller.BuildConfig
import com.samples.appinstaller.R
import com.samples.appinstaller.databinding.FragmentLibraryBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {
    private val viewModel: AppViewModel by activityViewModels()
    private lateinit var binding: FragmentLibraryBinding
    private lateinit var adapter: LibraryRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLibraryBinding.inflate(inflater, container, false)
        setupUi(binding)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        togglePermissionSection()
        viewLifecycleOwner.lifecycleScope.launch {
            // TODO: Remove this delay once we sync active install sessions
            // FIXME: Remove automatic sync
            delay(500L)
            viewModel.loadLibrary()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.loadLibrary()
                    adapter.updateTimestamp(System.currentTimeMillis())
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUi(binding: FragmentLibraryBinding) {
        binding.showRationaleButton.setOnClickListener { showRationale() }

        adapter = LibraryRecyclerViewAdapter(
            list = emptyList(),
            currentTimestamp = System.currentTimeMillis(),
            updateAvailabilityPeriod = viewModel.appSettings.value?.updateAvailabilityPeriod
                ?: AppSettings.getDefaultInstance().updateAvailabilityPeriod,
            listeners = object : LibraryEntryActionListeners {
                override fun openApp(appId: String) {
                    viewModel.openApp(appId)
                }

                override fun installApp(appId: String, appName: String) {
                    viewModel.installApp(appId, appName)
                }

                override fun cancelInstallApp(appId: String) {
                    Snackbar.make(
                        binding.root,
                        R.string.feature_not_implemented,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                override fun upgradeApp(appId: String, appName: String) {
                    viewModel.upgradeApp(appId, appName)
                }

                override fun uninstallApp(appId: String) {
                    viewModel.uninstallApp(appId)
                }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.library.observe(viewLifecycleOwner) {
            adapter.updateList(it.values.toList())
        }
        viewModel.appSettings.observe(viewLifecycleOwner) {
            adapter.updateSettings(it.updateAvailabilityPeriod)
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
                .setTitle(R.string.authorization_dialog_title)
                .setMessage(R.string.authorization_dialog_description)
                .setPositiveButton(R.string.authorization_dialog_confirm_label) { _, _ ->
                    requestPermission()
                }
                .setNegativeButton(R.string.authorization_dialog_learn_more_label) { _, _ ->
                    openLearnMoreLink()
                }
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
    fun cancelInstallApp(appId: String)
    fun upgradeApp(appId: String, appName: String)
    fun uninstallApp(appId: String)
}