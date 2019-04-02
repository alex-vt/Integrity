/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.types.blog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R

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
        return RelatedLinkViewHolder(DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.matchable_link_list_item, null, false))
    }

    override fun onBindViewHolder(holder: RelatedLinkViewHolder, position: Int) {
        holder.binding.tvUrl.text = items[position].url
        holder.binding.tvCssSelector.text = items[position].cssSelector
        holder.binding.tvMatches.visibility =
                if (items[position].isMatched) View.VISIBLE else View.INVISIBLE
    }
}

data class MatchableLink(
        val url: String,
        val cssSelector: String,
        val isMatched: Boolean
)

class RelatedLinkViewHolder(val binding: com.alexvt.integrity.databinding.MatchableLinkListItemBinding)
    : RecyclerView.ViewHolder(binding.root)