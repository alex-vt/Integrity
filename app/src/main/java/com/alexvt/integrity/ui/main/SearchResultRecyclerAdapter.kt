/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.search.NamedLink
import com.alexvt.integrity.lib.search.SearchResult
import com.alexvt.integrity.lib.util.ViewExternalUtil
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.SpannableString
import kotlinx.android.synthetic.main.search_result_list_item.view.*


class SearchResultRecyclerAdapter(val items: ArrayList<SearchResult>, val mainActivity: MainActivity)
    : RecyclerView.Adapter<SearchResultViewHolder>() {

    fun setItems(newItems: List<SearchResult>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SearchResultViewHolder(
            LayoutInflater.from(mainActivity).inflate(R.layout.search_result_list_item, parent, false))

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val searchResult = items[position]
        holder.view.tvSnapshotTitle.text = searchResult.snapshotTitle
        holder.view.tvSnapshotDate.text = searchResult.date

        holder.view.tvText.text = getHighlightedSpannable(searchResult.truncatedText,
                searchResult.highlightRange)

        if (searchResult.relevantLinkOrNull != null) { // todo button to view other links
            holder.view.tvRelevantLink.visibility = View.VISIBLE
            val relevantLink = searchResult.relevantLinkOrNull as NamedLink
            holder.view.tvRelevantLink.text = relevantLink.title
            holder.view.tvRelevantLink.setOnClickListener {
                ViewExternalUtil.viewLinkExternal(mainActivity, relevantLink.link)
            }
        } else {
            holder.view.tvRelevantLink.visibility = View.GONE
        }
    }

    private fun getHighlightedSpannable(text: String, highlightRange: IntRange): Spannable {
        val spannableString = SpannableString(text)
        spannableString.setSpan(ForegroundColorSpan(Color.BLUE), highlightRange.first,
                highlightRange.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }
}


class SearchResultViewHolder (val view: View) : RecyclerView.ViewHolder(view)