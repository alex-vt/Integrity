/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.util.FontUtil
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.bumptech.glide.Glide
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.snapshot_list_item.view.*

class SnapshotRecyclerAdapter(private val items: ArrayList<Pair<Snapshot, Int>>,
                              private val context: Context,
                              private val onClickListener: (Long, String) -> Unit,
                              private val onLongClickListener: (Long, String, Boolean) -> Unit,
                              private val onClickMoreListener: (Long, String, Boolean) -> Unit)
    : RecyclerView.Adapter<SnapshotViewHolder>() {

    private var showMoreButton: Boolean = false

    fun setItems(newItems: List<Pair<Snapshot, Int>>, showMoreButton: Boolean) {
        items.clear()
        items.addAll(newItems)
        this.showMoreButton = showMoreButton
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SnapshotViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.snapshot_list_item, parent, false)
        FontUtil.setFont(context, view, IntegrityCore.getFont())
        return SnapshotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SnapshotViewHolder, position: Int) {
        val snapshot = items[position].first

        holder.view.tvTitle.text = snapshot.title
        holder.view.tvDate.text = when {
            snapshot.status == SnapshotStatus.COMPLETE -> snapshot.date
            snapshot.status == SnapshotStatus.INCOMPLETE -> snapshot.date + " (incomplete)"
            snapshot.status == SnapshotStatus.IN_PROGRESS -> snapshot.date + " (downloading)"
            else -> "blueprint only"
        }

        val snapshotCount = items[position].second
        val isMultipleSnapshots = snapshotCount > 1

        holder.view.bMore.visibility = if (showMoreButton) View.VISIBLE else View.GONE
        holder.view.bMore.text = if (isMultipleSnapshots) "${snapshotCount - 1} more" else "Add more"

        holder.view.bMore.setOnClickListener {
            onClickMoreListener.invoke(snapshot.artifactId, snapshot.date, isMultipleSnapshots)
        }
        holder.view.setOnClickListener {
            onClickListener.invoke(snapshot.artifactId, snapshot.date)
        }
        holder.view.setOnLongClickListener {
            onLongClickListener.invoke(snapshot.artifactId, snapshot.date, isMultipleSnapshots)
            false
        }

        // todo pass path with item, remove IntegrityCore dependency
        val snapshotPreviewPath = IntegrityCore.dataFolderManager.getSnapshotPreviewPath(
                IntegrityCore.getDataFolderName(), snapshot.artifactId, snapshot.date)
        if (!IntegrityCore.dataFolderManager.fileExists(snapshotPreviewPath)) {
            holder.view.ivPreview.setImageDrawable(IconicsDrawable(context)
                    .icon(CommunityMaterial.Icon2.cmd_view_grid)
                    .colorRes(R.color.colorBlueprint)
                    .sizeDp(36))
        } else {
            Glide.with(context)
                    .asBitmap()
                    .load(snapshotPreviewPath)
                    .into(holder.view.ivPreview)
        }
    }
}

class SnapshotViewHolder (val view: View) : RecyclerView.ViewHolder(view)
