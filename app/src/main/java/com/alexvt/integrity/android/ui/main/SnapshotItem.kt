/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.main

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.alexvt.integrity.R
import com.alexvt.integrity.core.data.settings.ListViewMode
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.data.types.DataTypeRepository
import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.metadata.SnapshotStatus
import com.alexvt.integrity.lib.android.util.ThemeUtil
import com.bumptech.glide.Glide
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder

/**
 * Snapshot in a list is viewed in a layout depending on settings.
 * Possible layouts all have similar widgets, only styles and positions are different.
 */
class SnapshotItem(
        private val snapshot: Snapshot,
        private val titleHighlightRange: IntRange? = null,
        private val relatedSnapshotCount: Int,
        private val context: Context,
        private val showMoreButton: Boolean,
        private val settingsRepository: SettingsRepository,
        private val dataTypeRepository: DataTypeRepository,
        private val dataFolderManager: DataFolderManager,
        private val onClickListener: (Long, String) -> Unit,
        private val onLongClickListener: ((Long, String, Boolean) -> Unit)? = null,
        private val onClickMoreListener: ((Long, String, Boolean) -> Unit)? = null
) : Item() {

    override fun getLayout() = when (settingsRepository.get().snapshotListViewMode) {
        ListViewMode.LIST -> R.layout.snapshot_list_item
        else -> R.layout.snapshot_card_item // default
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if (other !is SnapshotItem) return false
        if (snapshot.artifactId != other.snapshot.artifactId) return false
        if (snapshot.date != other.snapshot.date) return false
        if (snapshot.status != other.snapshot.status) return false
        if (relatedSnapshotCount != other.relatedSnapshotCount) return false
        if (showMoreButton != other.showMoreButton) return false
        return true
    }

    override fun bind(holder: ViewHolder, position: Int) {
        val ivPreview = holder.itemView.findViewById<ImageView>(R.id.ivPreview)
        val tvTitle = holder.itemView.findViewById<TextView>(R.id.tvTitle)
        val tvDate = holder.itemView.findViewById<TextView>(R.id.tvDate)
        val tvType = holder.itemView.findViewById<TextView>(R.id.tvType)
        val bMore = holder.itemView.findViewById<Button>(R.id.bMore)

        tvType.text = dataTypeRepository.getDataType(snapshot.dataTypeClassName).title
        tvType.background.setColorFilter(ThemeUtil.getIntColor(snapshot.themeColor),
                PorterDuff.Mode.DARKEN)

        tvTitle.text = getHighlightedSpannable(snapshot.title, titleHighlightRange)

        tvDate.text = when {
            snapshot.status == SnapshotStatus.COMPLETE -> snapshot.date
            snapshot.status == SnapshotStatus.INCOMPLETE -> snapshot.date + " (incomplete)"
            snapshot.status == SnapshotStatus.IN_PROGRESS -> snapshot.date + " (downloading)"
            else -> "blueprint only"
        }

        val isMultipleSnapshots = relatedSnapshotCount > 1

        bMore.visibility = if (showMoreButton) View.VISIBLE else View.GONE
        bMore.text = if (isMultipleSnapshots) "${relatedSnapshotCount - 1} more" else "Add more"
        bMore.setOnClickListener {
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
            ivPreview.setImageDrawable(IconicsDrawable(context)
                    .icon(CommunityMaterial.Icon2.cmd_view_grid)
                    .colorRes(R.color.colorBlueprint)
                    .sizeDp(36))
        } else {
            Glide.with(context)
                    .asBitmap()
                    .load(snapshotPreviewPath)
                    .into(ivPreview)
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