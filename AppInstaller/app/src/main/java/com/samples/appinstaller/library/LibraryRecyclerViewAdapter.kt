package com.samples.appinstaller.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samples.appinstaller.AppSettings
import com.samples.appinstaller.LibraryEntryActionListeners
import com.samples.appinstaller.R
import com.samples.appinstaller.apps.AppPackage
import com.samples.appinstaller.apps.AppStatus

class LibraryRecyclerViewAdapter(
    private var list: List<AppPackage>,
    private var timestamp: Long,
    private var updateAvailabilityPeriod: AppSettings.UpdateAvailabilityPeriod,
    private val listeners: LibraryEntryActionListeners
) :
    RecyclerView.Adapter<LibraryRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appNameTextView: TextView = view.findViewById(R.id.app_name)
        val companyTextView: TextView = view.findViewById(R.id.company)
        val statusTextView: TextView = view.findViewById(R.id.status)
        val iconImageView: ImageView = view.findViewById(R.id.icon)
        val installAppButton: Button = view.findViewById(R.id.install_app_button)
        val installingSection: LinearLayout = view.findViewById(R.id.installing_section)
        val installProgressBar: ProgressBar = view.findViewById(R.id.install_progress_bar)
        val installedSection: LinearLayout = view.findViewById(R.id.installed_section)
        val uninstallAppButton: Button = view.findViewById(R.id.uninstall_app_button)
        val openAppButton: Button = view.findViewById(R.id.open_app_button)
        val cancelButton: Button = view.findViewById(R.id.cancel_button)

        var appPackage: AppPackage? = null
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.library_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val app = list[position]
        viewHolder.appPackage = app
        viewHolder.appNameTextView.text = app.name
        viewHolder.companyTextView.text = app.company
        viewHolder.iconImageView.setImageResource(app.icon)
        viewHolder.statusTextView.text = app.status.toString()

        viewHolder.installAppButton.visibility =
            if (app.status === AppStatus.UNINSTALLED) View.VISIBLE else View.GONE
        viewHolder.installingSection.visibility =
            if (app.status === AppStatus.INSTALLING) View.VISIBLE else View.GONE
        viewHolder.installProgressBar.visibility =
            if (app.status === AppStatus.INSTALLING) View.VISIBLE else View.GONE
        viewHolder.installedSection.visibility =
            if (app.status === AppStatus.INSTALLED) View.VISIBLE else View.GONE

        viewHolder.openAppButton.setOnClickListener { listeners.openApp(app.id) }
        viewHolder.installAppButton.setOnClickListener { listeners.installApp(app.id, app.name) }
        viewHolder.uninstallAppButton.setOnClickListener { listeners.uninstallApp(app.id) }
        viewHolder.cancelButton.setOnClickListener { listeners.cancelInstallApp(app.id) }
//        viewHolder.upgradeAppButton.setOnClickListener { listeners.onOpen(app.id) }
    }

    override fun getItemCount() = list.size

    fun updateData(list: List<AppPackage>) {
        this.list = list
        this.notifyDataSetChanged()
    }

    fun updateTimestamp(timestamp: Long) {
        this.notifyDataSetChanged()
    }
}
