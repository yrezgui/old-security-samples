package com.samples.appinstaller.appDetails

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.samples.appinstaller.BuildConfig
import com.samples.appinstaller.R
import com.samples.appinstaller.appManager.AppStatus
import com.samples.appinstaller.appManager.getStatusFailureName
import com.samples.appinstaller.databinding.FragmentAppDetailsBinding

const val SEND_INSTALL_UPDATES_PERMISSION = "com.samples.appinstaller.permission.SEND_INSTALLER_UPDATES"

const val INSTALL_INTENT_NAME = "appinstaller_install_status"
const val UNINSTALL_INTENT_NAME = "appinstaller_uninstall_status"

class AppDetailsFragment : Fragment() {
    private val viewModel: AppDetailsViewModel by viewModels()
    private val args: AppDetailsFragmentArgs by navArgs()
    private lateinit var binding: FragmentAppDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.loadApp(args.packageId)

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_app_details,
            container,
            false
        )
        binding.viewmodel = viewModel
        binding.permissionHandler = InstallPermissionHandler()
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        context?.packageManager?.packageInstaller?.registerSessionCallback(sessionCallback)
    }

    override fun onStop() {
        super.onStop()
        context?.packageManager?.packageInstaller?.unregisterSessionCallback(sessionCallback)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAppStatus()
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
                .setAction("Dismiss") {}
                .show()
        }
    }

    /**
     * Monitor install sessions progress
     */
    private val sessionCallback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {}

        override fun onBadgingChanged(sessionId: Int) {}

        override fun onActiveChanged(sessionId: Int, active: Boolean) {}

        override fun onProgressChanged(sessionId: Int, progress: Float) {}

        override fun onFinished(sessionId: Int, success: Boolean) {
            Log.d("SessionCallback", "onFinished sessionId: $sessionId, success: $success")
            if(viewModel.currentInstallSessionId.value == sessionId) {
                viewModel.clearInstallSessionId()
            }
        }
    }

    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val extras = intent.extras!!
            val status = extras.getInt(PackageInstaller.EXTRA_STATUS)
            val message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)

            Log.d("InstallReceiver", "status: $status // message: $message")

            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    viewModel.setAppStatus(AppStatus.INSTALLING)
                    val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?
                    startActivity(confirmIntent)
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    viewModel.clearInstallSessionId()
                    viewModel.setAppStatus(AppStatus.INSTALLED)
                }
                PackageInstaller.STATUS_FAILURE,
                PackageInstaller.STATUS_FAILURE_ABORTED,
                PackageInstaller.STATUS_FAILURE_BLOCKED,
                PackageInstaller.STATUS_FAILURE_CONFLICT,
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                PackageInstaller.STATUS_FAILURE_INVALID,
                PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    Log.d("InstallReceiver FAILURE", "status: $status (${getStatusFailureName(status)}) // message: $message")

                    viewModel.clearInstallSessionId()
                    viewModel.setAppStatus(AppStatus.UNINSTALLED)
                    showSnackbar("Error happened during installation: ${getStatusFailureName(status)}")
                }
            }
        }
    }

    private val uninstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val extras = intent.extras!!
            val status = extras.getInt(PackageInstaller.EXTRA_STATUS)
            val message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)

            Log.d("UninstallReceiver", "status: $status // message: $message")

            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?
                    startActivity(confirmIntent)
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    viewModel.setAppStatus(AppStatus.UNINSTALLED)
                }
                else -> {
                    showSnackbar("Error happened during uninstallation")
                }
            }
        }
    }

    class InstallPermissionHandler {
        fun showRationale(view: View) {
            val context = view.context

            MaterialAlertDialogBuilder(context)
                .setTitle("Authorization")
                .setMessage("App Installer needs this special permission to be install other apps")
                .setPositiveButton("Ok") { _, _ -> request(view) }
                .setNegativeButton("Learn More") { _, _ -> openLearnMoreLink(context) }
                .show()
        }

        private fun openLearnMoreLink(context: Context) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data =
                    Uri.parse("https://developer.android.com/reference/kotlin/android/content/pm/PackageInstaller")
            }

            context.startActivity(intent)
        }

        private fun request(view: View) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            }

            view.findFragment<AppDetailsFragment>().startActivityForResult(intent, 1000)
        }
    }
}