/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.main

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
import com.alexvt.integrity.core.settings.ListViewMode
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.core.types.DataTypeRepository
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.alexvt.integrity.lib.util.ThemeUtil
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

    private fun getIvPreview(holder: ViewHolder) = holder.itemView.findViewById<ImageView>(R.id.ivPreview)

    private fun getTvTitle(holder: ViewHolder) = holder.itemView.findViewById<TextView>(R.id.tvTitle)

    private fun getTvDate(holder: ViewHolder) = holder.itemView.findViewById<TextView>(R.id.tvDate)

    private fun getTvType(holder: ViewHolder) = holder.itemView.findViewById<TextView>(R.id.tvType)

    private fun getBmore(holder: ViewHolder) = holder.itemView.findViewById<Button>(R.id.bMore)


    override fun bind(holder: ViewHolder, position: Int) {
        getTvType(holder).text = dataTypeRepository.getDataType(snapshot.dataTypeClassName).title
        getTvType(holder).background.setColorFilter(ThemeUtil.getIntColor(snapshot.themeColor),
                PorterDuff.Mode.DARKEN)

        getTvTitle(holder).text = getHighlightedSpannable(snapshot.title, titleHighlightRange)
        getTvDate(holder).text = when {
            snapshot.status == SnapshotStatus.COMPLETE -> snapshot.date
            snapshot.status == SnapshotStatus.INCOMPLETE -> snapshot.date + " (incomplete)"
            snapshot.status == SnapshotStatus.IN_PROGRESS -> snapshot.date + " (downloading)"
            else -> "blueprint only"
        }

        val isMultipleSnapshots = relatedSnapshotCount > 1

        getBmore(holder).visibility = if (showMoreButton) View.VISIBLE else View.GONE
        getBmore(holder).text = if (isMultipleSnapshots) "${relatedSnapshotCount - 1} more" else "Add more"

        getBmore(holder).setOnClickListener {
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
            getIvPreview(holder).setImageDrawable(IconicsDrawable(context)
                    .icon(CommunityMaterial.Icon2.cmd_view_grid)
                    .colorRes(R.color.colorBlueprint)
                    .sizeDp(36))
        } else {
            Glide.with(context)
                    .asBitmap()
                    .load(snapshotPreviewPath)
                    .into(getIvPreview(holder))
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