/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.main

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.core.data.search.NamedLink
import com.alexvt.integrity.lib.core.data.search.TextSearchResult
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.search_result_list_item.view.*

class TextSearchResultListItem(
        private val textSearchResult: TextSearchResult,
        private val context: Context,
        private val onLinkClickListener: (String) -> Unit
        ) : Item() {
    override fun getLayout() = R.layout.search_result_list_item

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if (other !is TextSearchResultListItem) return false
        if (textSearchResult.snapshotTitle != other.textSearchResult.snapshotTitle) return false
        if (textSearchResult.date != other.textSearchResult.date) return false
        if (textSearchResult.truncatedText != other.textSearchResult.truncatedText) return false
        if (textSearchResult.highlightRange != other.textSearchResult.highlightRange) return false
        return true
    }

    override fun bind(holder: ViewHolder, position: Int) {
        holder.itemView.tvSnapshotTitle.text = textSearchResult.snapshotTitle
        holder.itemView.tvSnapshotDate.text = textSearchResult.date

        holder.itemView.tvText.text = getHighlightedSpannable(textSearchResult.truncatedText,
                textSearchResult.highlightRange)

        if (textSearchResult.relevantLinkOrNull != null) { // todo button to view other links
            holder.itemView.tvRelevantLink.visibility = View.VISIBLE
            val relevantLink = textSearchResult.relevantLinkOrNull as NamedLink
            holder.itemView.tvRelevantLink.text = relevantLink.title
            holder.itemView.tvRelevantLink.setOnClickListener {
                onLinkClickListener.invoke(relevantLink.link)
            }
        } else {
            holder.itemView.tvRelevantLink.visibility = View.GONE
        }
    }

    private fun getHighlightedSpannable(text: String, highlightRange: IntRange): Spannable {
        val spannableString = SpannableString(text)
        spannableString.setSpan(ForegroundColorSpan(Color.BLUE), highlightRange.first,
                highlightRange.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }
}