/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.base.adapter.LogRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import kotlinx.android.synthetic.main.activity_log_view.*

class LogViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)
        setSupportActionBar(toolbar)

        rvLogList.adapter = LogRecyclerAdapter(ArrayList(), this)
    }

    override fun onStart() {
        super.onStart()
        IntegrityCore.logRepository.addChangesListener(LogViewActivity::class.java.simpleName) {
            refreshLogList()
        }
    }

    override fun onStop() {
        super.onStop()
        IntegrityCore.logRepository.removeChangesListener(LogViewActivity::class.java.simpleName)
    }

    private fun refreshLogList() {
        (rvLogList.adapter as LogRecyclerAdapter).setItems(IntegrityCore
                .logRepository.getRecentEntries(1000))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                askClearLog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun askClearLog() {
        MaterialDialog(this)
                .title(text = "Clear log?")
                .positiveButton(text = "Yes") {
                    IntegrityCore.logRepository.clear()
                }
                .negativeButton(text = "Cancel")
                .show()
    }
}
