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
import com.samples.appinstaller.R
import com.samples.appinstaller.apps.AppPackage
import com.samples.appinstaller.apps.AppStatus
import com.samples.appinstaller.settings.toDuration
import java.time.Instant

class LibraryRecyclerViewAdapter(
    private var list: List<AppPackage>,
    private var currentTimestamp: Long,
    private var updateAvailabilityPeriod: AppSettings.UpdateAvailabilityPeriod,
    private val listeners: LibraryEntryActionListeners
) :
    RecyclerView.Adapter<LibraryRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.icon)
        val appNameTextView: TextView = view.findViewById(R.id.app_name)
        val companyTextView: TextView = view.findViewById(R.id.company)
        val statusTextView: TextView = view.findViewById(R.id.status)
        val lastUpdateTimeView: TextView = view.findViewById(R.id.last_updated_time)

        val installAppButton: Button = view.findViewById(R.id.install_app_button)
        val installingSection: LinearLayout = view.findViewById(R.id.installing_section)
        val installProgressBar: ProgressBar = view.findViewById(R.id.install_progress_bar)
        val installedSection: LinearLayout = view.findViewById(R.id.installed_section)
        val uninstallAppButton: Button = view.findViewById(R.id.uninstall_app_button)
        val upgradeAppButton: Button = view.findViewById(R.id.upgrade_app_button)
        val openAppButton: Button = view.findViewById(R.id.open_app_button)
        val cancelButton: Button = view.findViewById(R.id.cancel_button)

        var appPackage: AppPackage? = null
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.library_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    /**
     * Check if the installed time of a package is older than the UpdateAvailabilityPeriod setting
     */
    private fun isUpdateAvailable(lastUpdateTime: Long): Boolean {
        if (lastUpdateTime == -1L || updateAvailabilityPeriod == AppSettings.UpdateAvailabilityPeriod.NONE) {
            return false
        }

        val delay = updateAvailabilityPeriod.toDuration().toMillis()

        return (lastUpdateTime + delay) < currentTimestamp
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val app = list[position]
        val isUpdateAvailable = isUpdateAvailable(app.updatedAt)

        viewHolder.appPackage = app
        viewHolder.iconImageView.setImageResource(app.icon)
        viewHolder.appNameTextView.text = app.name
        viewHolder.companyTextView.text = app.company
        viewHolder.statusTextView.text = if (isUpdateAvailable) {
            viewHolder.itemView.context.getString(R.string.update_available_status)
        } else {
            app.status.toString()
        }

        // We don't display the lastUpdateTime if the app isn't installed
        if (app.updatedAt != -1L) {
            viewHolder.lastUpdateTimeView.text = Instant.ofEpochMilli(app.updatedAt).toString()
        }

        viewHolder.installAppButton.visibility =
            if (app.status === AppStatus.UNINSTALLED) View.VISIBLE else View.GONE

        viewHolder.installingSection.visibility =
            if (app.status === AppStatus.INSTALLING) View.VISIBLE else View.GONE

        viewHolder.installProgressBar.visibility =
            if (app.status === AppStatus.INSTALLING) View.VISIBLE else View.GONE

        viewHolder.installedSection.visibility =
            if (app.status === AppStatus.INSTALLED) View.VISIBLE else View.GONE

        if (isUpdateAvailable) {
            viewHolder.upgradeAppButton.visibility = View.VISIBLE
            viewHolder.uninstallAppButton.visibility = View.GONE
        } else {
            viewHolder.upgradeAppButton.visibility = View.GONE
            viewHolder.uninstallAppButton.visibility = View.VISIBLE
        }

        viewHolder.openAppButton.setOnClickListener { listeners.openApp(app.id) }
        viewHolder.installAppButton.setOnClickListener { listeners.installApp(app.id, app.name) }
        viewHolder.uninstallAppButton.setOnClickListener { listeners.uninstallApp(app.id) }
        viewHolder.cancelButton.setOnClickListener { listeners.cancelInstallApp(app.id) }
        viewHolder.upgradeAppButton.setOnClickListener { listeners.upgradeApp(app.id, app.name) }
    }

    override fun getItemCount() = list.size

    fun updateList(list: List<AppPackage>) {
        this.list = list
        this.notifyDataSetChanged()
    }

    fun updateSettings(updateAvailabilityPeriod: AppSettings.UpdateAvailabilityPeriod) {
        this.updateAvailabilityPeriod = updateAvailabilityPeriod
        this.notifyDataSetChanged()
    }

    fun updateTimestamp(timestamp: Long) {
        this.currentTimestamp = timestamp
        this.notifyDataSetChanged()
    }
}
