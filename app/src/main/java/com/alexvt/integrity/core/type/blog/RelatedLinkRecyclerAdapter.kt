/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import kotlinx.android.synthetic.main.matchable_link_list_item.view.*

class RelatedLinkRecyclerAdapter(val items: ArrayList<MatchableLink>, val activity: BlogTypeActivity)
    : RecyclerView.Adapter<RelatedLinkViewHolder>() {

    fun setItems(newItems: List<MatchableLink>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedLinkViewHolder {
        return RelatedLinkViewHolder(LayoutInflater.from(activity)
                .inflate(R.layout.matchable_link_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: RelatedLinkViewHolder, position: Int) {
        holder.itemView.tvUrl.text = items[position].url
        holder.itemView.tvCssSelector.text = items[position].cssSelector
        holder.itemView.tvMatches.visibility =
                if (items[position].isMatched) View.VISIBLE else View.INVISIBLE
    }
}

data class MatchableLink(
        val url: String,
        val cssSelector: String,
        val isMatched: Boolean
)

class RelatedLinkViewHolder(view: View) : RecyclerView.ViewHolder(view)