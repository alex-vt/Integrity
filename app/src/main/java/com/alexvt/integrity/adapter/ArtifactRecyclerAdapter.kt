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
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.activity.MainActivity
import com.alexvt.integrity.core.SnapshotMetadata
import kotlinx.android.synthetic.main.artifact_list_item.view.*

class ArtifactRecyclerAdapter(val items: ArrayList<SnapshotMetadata>, val mainActivity: MainActivity)
    : RecyclerView.Adapter<ArtifactViewHolder>() {

    fun setItems(newItems: List<SnapshotMetadata>) {
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

        holder.tvTitle?.text = items.get(position).title + "\nat " + items.get(position).date
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