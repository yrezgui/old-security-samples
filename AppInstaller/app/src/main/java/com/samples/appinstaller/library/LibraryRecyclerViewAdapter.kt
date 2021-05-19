package com.samples.appinstaller.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.samples.appinstaller.R
import com.samples.appinstaller.appManager.AppPackage

class LibraryRecyclerViewAdapter(private var list: List<AppPackage>) :
    RecyclerView.Adapter<LibraryRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appNameTextView: TextView = view.findViewById(R.id.app_name)
        val companyTextView: TextView = view.findViewById(R.id.company)
        val updateTextView: TextView = view.findViewById(R.id.update_available)
        val iconImageView: ImageView = view.findViewById(R.id.icon)
        var appPackage: AppPackage? = null
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.store_list_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val app = list[position]
        viewHolder.appPackage = app
        viewHolder.appNameTextView.text = app.name
        viewHolder.companyTextView.text = app.company
        viewHolder.iconImageView.setImageResource(app.icon)
        viewHolder.updateTextView.visibility = if (app.isUpdateAvailable) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        viewHolder.itemView.setOnClickListener {
            it.findNavController().navigate(
                LibraryFragmentDirections.actionNavigationLibraryToNavigationAppDetails(
                    appName = app.name,
                    packageId = app.id
                )
            )
        }
    }

    override fun getItemCount() = list.size

    fun updateData(list: List<AppPackage>) {
        this.list = list
        this.notifyDataSetChanged()
    }
}
