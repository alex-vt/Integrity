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
import com.alexvt.integrity.base.activity.ArtifactViewActivity
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotStatus
import com.alexvt.integrity.lib.util.DataCacheFolderUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.artifact_list_item.view.*

class SnapshotRecyclerAdapter(val items: ArrayList<Snapshot>, val artifactViewActivity: ArtifactViewActivity)
    : RecyclerView.Adapter<SnapshotViewHolder>() {

    fun setItems(newItems: List<Snapshot>) {
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

        holder.view.tvTitle?.text = items.get(position).title + " snapshot\nat " +
                items.get(position).date +
                if (items.get(position).status == SnapshotStatus.BLUEPRINT) {
                    " (blueprint)"
                } else if (items.get(position).status == SnapshotStatus.IN_PROGRESS) {
                    " (downloading)"
                } else if (items.get(position).status == SnapshotStatus.COMPLETE) {
                    ""
                } else {
                    " (incomplete)"
                }
        holder.view.setOnClickListener {
            IntegrityCore.openViewSnapshotOrShowProgress(artifactViewActivity, artifactId, date)
        }

        val snapshotPreviewPath = IntegrityEx.getSnapshotPreviewPath(artifactViewActivity,
                items[position].artifactId, items[position].date)
        if (!DataCacheFolderUtil.fileExists(artifactViewActivity, snapshotPreviewPath)) {
            Glide.with(artifactViewActivity)
                    .asBitmap()
                    .load(R.drawable.baseline_waves_black_36)
                    .into(holder.view.ivPreview)
        } else {
            Glide.with(artifactViewActivity)
                    .asBitmap()
                    .load(snapshotPreviewPath)
                    .apply(RequestOptions().error(R.drawable.baseline_waves_black_36))
                    .into(holder.view.ivPreview)
        }
    }
}

class SnapshotViewHolder (val view: View) : RecyclerView.ViewHolder(view)