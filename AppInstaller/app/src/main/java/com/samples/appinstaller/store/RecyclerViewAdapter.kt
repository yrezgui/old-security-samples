package com.samples.appinstaller.store

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.samples.appinstaller.R
import com.samples.appinstaller.data.AppPackage

class RecyclerViewAdapter(items: List<AppPackage>, pm: PackageManager) :
    RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    private val mValues: List<AppPackage> = items
    private val packageManager: PackageManager = pm

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.store_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
//        holder.mIdView.setText(mValues.get(position).id);
//        holder.mIcon.setImageResource(mValues[position].icon)
//        holder.mContentView.setText(mValues[position].title)
//        holder.mSubtitle.setText(mValues[position].developer)
//        holder.mUpdateAvailable.visibility = if (holder.mItem.isUpdateAvailable(packageManager)) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val mView: View = view

        //public final TextView mIdView;
        val mContentView: TextView
        val mSubtitle: TextView
        val mIcon: ImageView
        val mUpdateAvailable: TextView
        var mItem: AppPackage? = null
        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }

        override fun onClick(v: View) {
            Log.i("EJC", "onClick: $v")
//            val bundle = Bundle()
//            bundle.putInt("app_id", mItem.id)
//            Navigation.findNavController(v)
//                .navigate(R.id.action_navigation_games_to_scrollingFragment, bundle)
        }

        init {
            mView.setOnClickListener(this)
            //mIdView = (TextView) view.findViewById(R.id.item_number);
            mContentView = view.findViewById(R.id.content)
            mSubtitle = view.findViewById(R.id.subtitle)
            mIcon = view.findViewById(R.id.icon)
            mUpdateAvailable = view.findViewById(R.id.update_available)
        }
    }

}