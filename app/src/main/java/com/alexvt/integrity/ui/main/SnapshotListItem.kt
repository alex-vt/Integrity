/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.main

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import com.alexvt.integrity.R
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.bumptech.glide.Glide
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.snapshot_list_item.view.*

class SnapshotListItem(
        private val snapshot: Snapshot,
        private val titleHighlightRange: IntRange? = null,
        private val relatedSnapshotCount: Int,
        private val context: Context,
        private val showMoreButton: Boolean,
        private val settingsRepository: SettingsRepository,
        private val dataFolderManager: DataFolderManager,
        private val onClickListener: (Long, String) -> Unit,
        private val onLongClickListener: ((Long, String, Boolean) -> Unit)? = null,
        private val onClickMoreListener: ((Long, String, Boolean) -> Unit)? = null
        ) : Item() {
    override fun getLayout() = R.layout.snapshot_list_item

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if (other !is SnapshotListItem) return false
        if (snapshot.artifactId != other.snapshot.artifactId) return false
        if (snapshot.date != other.snapshot.date) return false
        if (snapshot.status != other.snapshot.status) return false
        if (relatedSnapshotCount != other.relatedSnapshotCount) return false
        if (showMoreButton != other.showMoreButton) return false
        return true
    }

    override fun bind(holder: ViewHolder, position: Int) {
        holder.itemView.tvTitle.text = getHighlightedSpannable(snapshot.title, titleHighlightRange)
        holder.itemView.tvDate.text = when {
            snapshot.status == SnapshotStatus.COMPLETE -> snapshot.date
            snapshot.status == SnapshotStatus.INCOMPLETE -> snapshot.date + " (incomplete)"
            snapshot.status == SnapshotStatus.IN_PROGRESS -> snapshot.date + " (downloading)"
            else -> "blueprint only"
        }

        val isMultipleSnapshots = relatedSnapshotCount > 1

        holder.itemView.bMore.visibility = if (showMoreButton) View.VISIBLE else View.GONE
        holder.itemView.bMore.text = if (isMultipleSnapshots) "${relatedSnapshotCount - 1} more" else "Add more"

        holder.itemView.bMore.setOnClickListener {
            onClickMoreListener?.invoke(snapshot.artifactId, snapshot.date, isMultipleSnapshots)
        }
        holder.itemView.setOnClickListener {
            onClickListener.invoke(snapshot.artifactId, snapshot.date)
        }
        holder.itemView.setOnLongClickListener {
            onLongClickListener?.invoke(snapshot.artifactId, snapshot.date, isMultipleSnapshots)
            false
        }

        // todo pass path with item
        val snapshotPreviewPath = dataFolderManager.getSnapshotPreviewPath(
                settingsRepository.get().dataFolderPath, snapshot.artifactId, snapshot.date)
        if (!dataFolderManager.fileExists(snapshotPreviewPath)) {
            holder.itemView.ivPreview.setImageDrawable(IconicsDrawable(context)
                    .icon(CommunityMaterial.Icon2.cmd_view_grid)
                    .colorRes(R.color.colorBlueprint)
                    .sizeDp(36))
        } else {
            Glide.with(context)
                    .asBitmap()
                    .load(snapshotPreviewPath)
                    .into(holder.itemView.ivPreview)
        }
    }

    private fun getHighlightedSpannable(text: String, highlightRange: IntRange? = null): Spannable {
        val spannableString = SpannableString(text)
        if (highlightRange != null) {
            spannableString.setSpan(ForegroundColorSpan(Color.BLUE), highlightRange.first,
                    highlightRange.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannableString
    }
}