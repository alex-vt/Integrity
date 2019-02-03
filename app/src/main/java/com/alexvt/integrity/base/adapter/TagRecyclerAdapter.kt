/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.base.activity.TagsActivity
import com.alexvt.integrity.lib.Tag
import kotlinx.android.synthetic.main.folder_location_list_item.view.*

class TagRecyclerAdapter(val items: ArrayList<Pair<Tag, Boolean>>,
                         val tagsActivity: TagsActivity) : RecyclerView.Adapter<TagViewHolder>() {

    fun setItems(newItems: List<Pair<Tag, Boolean>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder(LayoutInflater.from(tagsActivity)
                .inflate(R.layout.tag_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = items[position].first
        val isSelected = items[position].second
        holder.tvName.text = tag.text
        holder.tvName.setBackgroundColor(Color.parseColor(tag.color)) // todo make text black or white

        if (tagsActivity.isSelectMode()) {
            if (isSelected) {
                holder.tvSelected.visibility = View.VISIBLE
            } else {
                holder.tvSelected.visibility = View.INVISIBLE
            }
        } else {
            holder.tvSelected.visibility = View.GONE
        }

        holder.llItem.setOnClickListener { _ ->
            if (tagsActivity.isSelectMode()) {
                tagsActivity.toggleSelection(tag)
            } else {
                tagsActivity.editTag(tag, tag.text)
            }
        }

        holder.llItem.setOnLongClickListener {
            tagsActivity.askRemoveTag(tag)
            false
        }
    }
}

class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val llItem = view.llItem
    val tvName = view.tvName
    val tvSelected = view.tvSelected
}