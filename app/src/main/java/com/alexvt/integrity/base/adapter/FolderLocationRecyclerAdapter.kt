/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.base.activity.FolderLocationsActivity
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.core.IntegrityCore
import kotlinx.android.synthetic.main.folder_location_list_item.view.*

class FolderLocationRecyclerAdapter(val items: ArrayList<Pair<FolderLocation, Boolean>>,
                                    val folderLocationsActivity: FolderLocationsActivity)
    : RecyclerView.Adapter<FolderLocationViewHolder>() {

    fun setItems(newItems: List<Pair<FolderLocation, Boolean>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderLocationViewHolder {
        return FolderLocationViewHolder(LayoutInflater.from(folderLocationsActivity)
                .inflate(R.layout.folder_location_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: FolderLocationViewHolder, position: Int) {
        val folderLocation = items.get(position).first
        val isSelected = items.get(position).second
        holder.tvName?.text = IntegrityCore.getFolderLocationName(folderLocation)

        if (folderLocationsActivity.isSelectMode()) {
            if (isSelected) {
                holder.tvSelected.visibility = View.VISIBLE
            } else {
                holder.tvSelected.visibility = View.INVISIBLE
            }
        } else {
            holder.tvSelected.visibility = View.GONE
        }

        holder.llItem.setOnClickListener { _ ->
            if (folderLocationsActivity.isSelectMode()) {
                folderLocationsActivity.toggleSelection(folderLocation)
            } else {
                folderLocationsActivity.viewFolderLocation(folderLocation.title)
            }
        }

        holder.llItem.setOnLongClickListener {
            folderLocationsActivity.askRemoveFolderLocation(folderLocation.title)
            false
        }
    }
}

class FolderLocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val llItem = view.llItem
    val tvName = view.tvName
    val tvSelected = view.tvSelected
}