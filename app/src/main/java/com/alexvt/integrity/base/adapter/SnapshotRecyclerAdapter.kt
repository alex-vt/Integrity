/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.base.activity.MainActivity
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotStatus
import com.alexvt.integrity.lib.util.DataCacheFolderUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.snapshot_list_item.view.*

class SnapshotRecyclerAdapter(val items: ArrayList<Pair<Snapshot, Int>>,
                              private val mainActivity: MainActivity)
    : RecyclerView.Adapter<SnapshotViewHolder>() {

    private var showMoreButton: Boolean = false

    fun setItems(newItems: List<Pair<Snapshot, Int>>, showMoreButton: Boolean) {
        items.clear()
        items.addAll(newItems)
        this.showMoreButton = showMoreButton
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SnapshotViewHolder(
            LayoutInflater.from(mainActivity).inflate(R.layout.snapshot_list_item, parent, false))

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

        holder.view.bMore.visibility = if (showMoreButton) View.VISIBLE else View.GONE
        holder.view.bMore.text = if (snapshotCount == 1) "Add more" else "${snapshotCount - 1} more"
        holder.view.bMore.setOnClickListener {
            if (snapshotCount == 1) {
                mainActivity.addSnapshot(snapshot.artifactId)
            } else {
                mainActivity.filterArtifact(snapshot.artifactId)
            }
        }

        holder.view.setOnClickListener { mainActivity.viewSnapshot(snapshot) }
        holder.view.setOnLongClickListener {
            mainActivity.askRemoveArtifact(snapshot.artifactId) // todo popup menu instead
            false
        }

        val snapshotPreviewPath = IntegrityEx.getSnapshotPreviewPath(mainActivity,
                snapshot.artifactId, snapshot.date)
        if (!DataCacheFolderUtil.fileExists(mainActivity, snapshotPreviewPath)) {
            holder.view.ivPreview.setImageDrawable(IconicsDrawable(mainActivity)
                    .icon(CommunityMaterial.Icon2.cmd_view_grid)
                    .colorRes(R.color.colorBlueprint)
                    .sizeDp(36))
        } else {
            Glide.with(mainActivity)
                    .asBitmap()
                    .load(snapshotPreviewPath)
                    .into(holder.view.ivPreview)
        }
    }
}


class SnapshotViewHolder (val view: View) : RecyclerView.ViewHolder(view)