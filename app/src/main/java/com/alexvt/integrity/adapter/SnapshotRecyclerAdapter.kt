/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.activity.ArtifactViewActivity
import com.alexvt.integrity.core.SnapshotMetadata
import kotlinx.android.synthetic.main.artifact_list_item.view.*

class SnapshotRecyclerAdapter(val items: ArrayList<SnapshotMetadata>, val artifactViewActivity: ArtifactViewActivity)
    : RecyclerView.Adapter<SnapshotViewHolder>() {

    fun setItems(newItems: List<SnapshotMetadata>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotViewHolder {
        return SnapshotViewHolder(LayoutInflater.from(artifactViewActivity).inflate(R.layout.artifact_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: SnapshotViewHolder, position: Int) {
        val artifactId = items.get(position).artifactId
        val date = items.get(position).date

        holder.tvTitle?.text = items.get(position).title + " snapshot\nat " + items.get(position).date
        holder.tvTitle.setOnClickListener({view -> artifactViewActivity.previewSnapshot(artifactId, date)})
    }
}

class SnapshotViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val tvTitle = view.tvTitle
}