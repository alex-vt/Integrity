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
import com.alexvt.integrity.base.activity.MainActivity
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotStatus
import kotlinx.android.synthetic.main.artifact_list_item.view.*

class ArtifactRecyclerAdapter(val items: ArrayList<Snapshot>, val mainActivity: MainActivity)
    : RecyclerView.Adapter<ArtifactViewHolder>() {

    fun setItems(newItems: List<Snapshot>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtifactViewHolder {
        return ArtifactViewHolder(LayoutInflater.from(mainActivity).inflate(R.layout.artifact_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ArtifactViewHolder, position: Int) {
        val artifactId = items.get(position).artifactId

        holder.tvTitle?.text = items.get(position).title + "\n" +
                if (items.get(position).status == SnapshotStatus.BLUEPRINT) {
                    "blueprint only"
                } else if (items.get(position).status == SnapshotStatus.COMPLETE) {
                    "at "  + items.get(position).date
                } else if (items.get(position).status == SnapshotStatus.IN_PROGRESS) {
                    "at "  + items.get(position).date + " (downloading)"
                } else {
                    "at "  + items.get(position).date + " (incomplete)"
                }
        holder.tvTitle.setOnClickListener {mainActivity.viewArtifact(artifactId)}

        holder.tvTitle.setOnLongClickListener {
            mainActivity.askRemoveArtifact(artifactId)
            false
        }
    }
}


class ArtifactViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val tvTitle = view.tvTitle
}