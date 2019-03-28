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
import com.alexvt.integrity.lib.search.NamedLink
import com.alexvt.integrity.lib.search.SearchResult
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.search_result_list_item.view.*

class SearchResultListItem(
        private val searchResult: SearchResult,
        private val context: Context,
        private val onLinkClickListener: (String) -> Unit
        ) : Item() {
    override fun getLayout() = R.layout.search_result_list_item

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        if (other !is SearchResultListItem) return false
        if (searchResult.snapshotTitle != other.searchResult.snapshotTitle) return false
        if (searchResult.date != other.searchResult.date) return false
        if (searchResult.truncatedText != other.searchResult.truncatedText) return false
        if (searchResult.highlightRange != other.searchResult.highlightRange) return false
        return true
    }

    override fun bind(holder: ViewHolder, position: Int) {
        holder.itemView.tvSnapshotTitle.text = searchResult.snapshotTitle
        holder.itemView.tvSnapshotDate.text = searchResult.date

        holder.itemView.tvText.text = getHighlightedSpannable(searchResult.truncatedText,
                searchResult.highlightRange)

        if (searchResult.relevantLinkOrNull != null) { // todo button to view other links
            holder.itemView.tvRelevantLink.visibility = View.VISIBLE
            val relevantLink = searchResult.relevantLinkOrNull as NamedLink
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