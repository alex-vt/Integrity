/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.blog.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import kotlinx.android.synthetic.main.matchable_link_list_item.view.*

class OfflineLinkRecyclerAdapter(val items: ArrayList<OfflineLink>, val activity: BlogTypeActivity)
    : RecyclerView.Adapter<OfflineLinkViewHolder>() {

    fun setItems(newItems: List<OfflineLink>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineLinkViewHolder {
        return OfflineLinkViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.offline_link_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: OfflineLinkViewHolder, position: Int) {
        holder.itemView.tvUrl.text = items[position].url
        holder.itemView.setOnClickListener { activity.goToOfflinePageDirectly(items[position].archiveUrl) }
    }
}

data class OfflineLink(
        val url: String,
        val archiveUrl: String
)

class OfflineLinkViewHolder(view: View) : RecyclerView.ViewHolder(view)