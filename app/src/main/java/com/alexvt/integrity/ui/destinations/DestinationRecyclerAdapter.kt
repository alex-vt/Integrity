/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.IntegrityEx
import kotlinx.android.synthetic.main.folder_location_list_item.view.*

class DestinationRecyclerAdapter(private val items: ArrayList<Pair<FolderLocation, Boolean>>,
                                 private val isSelectMode: Boolean,
                                 private val destinationsActivity: DestinationsActivity,
                                 private val onItemClickListener: (FolderLocation) -> Unit,
                                 private val onItemLongClickListener: (FolderLocation) -> Unit)
    : RecyclerView.Adapter<DestinationViewHolder>() {

    fun setItems(newItems: List<Pair<FolderLocation, Boolean>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder {
        return DestinationViewHolder(LayoutInflater.from(destinationsActivity)
                .inflate(R.layout.folder_location_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
        val folderLocation = items[position].first
        val isSelected = items[position].second
        holder.tvName?.text = IntegrityEx.getFolderLocationName(folderLocation)

        if (isSelectMode) {
            if (isSelected) {
                holder.tvSelected.visibility = View.VISIBLE
            } else {
                holder.tvSelected.visibility = View.INVISIBLE
            }
        } else {
            holder.tvSelected.visibility = View.GONE
        }

        holder.llItem.setOnClickListener { onItemClickListener.invoke(folderLocation) }

        holder.llItem.setOnLongClickListener {
            onItemLongClickListener.invoke(folderLocation)
            false
        }
    }
}

class DestinationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val llItem = view.llItem
    val tvName = view.tvName
    val tvSelected = view.tvSelected
}