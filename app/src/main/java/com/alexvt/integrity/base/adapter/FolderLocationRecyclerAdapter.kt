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
import kotlinx.android.synthetic.main.artifact_list_item.view.*

class FolderLocationRecyclerAdapter(val items: ArrayList<FolderLocation>,
                                    val folderLocationsActivity: FolderLocationsActivity)
    : RecyclerView.Adapter<FolderLocationViewHolder>() {

    fun setItems(newItems: List<FolderLocation>) {
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
        holder.tvTitle?.text = IntegrityCore.getFolderLocationName(items.get(position))
        holder.tvTitle.setOnClickListener { _ -> folderLocationsActivity
                .viewFolderLocation(items.get(position).title) }

        holder.tvTitle.setOnLongClickListener {
            folderLocationsActivity.askRemoveFolderLocation(items.get(position).title)
            false
        }

    }
}

class FolderLocationViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val tvTitle = view.tvTitle
}