/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.log

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.android.util.ThemedActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_log_view.*
import javax.inject.Inject

class LogViewActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[LogViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        bindToolbar()
        bindContent()
        bindNavigation()
    }

    private fun bindToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private fun bindContent() {
        rvLogList.adapter = LogRecyclerAdapter(ArrayList(), this)
        vm.logData.observe(this, Observer {
            (rvLogList.adapter as LogRecyclerAdapter).setItems(it)
        })
    }

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            }
        })
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
                    vm.clickDeleteAllLog()
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        vm.pressBackButton()
    }
}
