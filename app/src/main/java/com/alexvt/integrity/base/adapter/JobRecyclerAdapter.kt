/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexvt.integrity.R
import com.alexvt.integrity.base.activity.MainActivity
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.SnapshotMetadata
import kotlinx.android.synthetic.main.job_list_item.view.*

class JobRecyclerAdapter(val items: ArrayList<Pair<SnapshotMetadata, Boolean>>,
                         val mainActivity: MainActivity)
    : RecyclerView.Adapter<JobViewHolder>() {

    /**
     * Sets new running jobs or scheduled jobs. Running jobs are on top
     */
    fun setItems(newItems: List<SnapshotMetadata>, isRunning: Boolean) {
        items.removeIf { it.second == isRunning }
        items.addAll(getInsertionIndex(isRunning), newItems.associate { it to isRunning }.toList())
        items.sortByDescending { isRunning }
        notifyDataSetChanged()
    }

    private fun getInsertionIndex(isRunning: Boolean) = if (isRunning) 0 else items.size

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        return JobViewHolder(LayoutInflater.from(mainActivity).inflate(R.layout.job_list_item,
                parent, false))
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val snapshotMetadata = items[position].first
        val isRunning = items[position].second

        holder.tvTitle?.text = snapshotMetadata.title

        if (isRunning) {
            holder.tvStatus?.text = "Running"
        } else {
            val timeRemainingMillis = IntegrityCore.getNextJobRunTimestamp(snapshotMetadata) -
                    System.currentTimeMillis()
            holder.tvStatus?.text = if (timeRemainingMillis <= 0) {
                "Should start now"
            } else {
                "Starting in ${timeRemainingMillis / 1000} s"
            }
        }

        holder.bShow?.text = if (isRunning) "Show" else "Change"

        holder.bShow.setOnClickListener {
            if (isRunning) {
                IntegrityCore.showRunningJobProgressDialog(mainActivity,
                        snapshotMetadata.artifactId, snapshotMetadata.date)
            } else {
                // snapshot creation screen allows changing schedule setting and saving as blueprint
                mainActivity.startActivity(IntegrityCore.getSnapshotCreateIntent(mainActivity,
                        snapshotMetadata.artifactId))
            }
        }
    }
}


class JobViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val tvTitle = view.tvTitle
    val tvStatus = view.tvStatus
    val bShow = view.bShow
}