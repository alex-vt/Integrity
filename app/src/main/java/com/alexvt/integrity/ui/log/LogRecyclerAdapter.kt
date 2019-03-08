/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.log

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.log.LogEntry
import com.alexvt.integrity.lib.log.LogEntryType
import kotlinx.android.synthetic.main.log_list_item.view.*

class LogRecyclerAdapter(val items: ArrayList<LogEntry>, val activity: Activity)
    : RecyclerView.Adapter<LogViewHolder>() {

    fun setItems(newItems: List<LogEntry>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        return LogViewHolder(LayoutInflater.from(activity).inflate(R.layout.log_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val logEntry = items[position]

        holder.view.tvDate.text = logEntry.time
        holder.view.tvType.text = logEntry.type
        val isError = logEntry.type == LogEntryType.ERROR || logEntry.type == LogEntryType.CRASH
        holder.view.tvType.setBackgroundColor(activity
                .getColor(if (isError) R.color.colorError else R.color.colorPrimary))

        holder.view.tvText.text = logEntry.text
        holder.view.tvStackTrace.text = logEntry.stackTraceText
        holder.view.tvData.text = logEntry.data.asSequence().joinToString(separator = "\n")
    }
}

class LogViewHolder (val view: View) : RecyclerView.ViewHolder(view)