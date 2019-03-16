/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.info

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.util.FontUtil
import com.alexvt.integrity.lib.util.ThemedActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject


class LegalInfoActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[LegalInfoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        bindToolbar()
        bindContent()
        bindNavigation()
    }

    private fun bindToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        toolbar.title = "Legal"
    }

    private fun bindContent() {
        // todo other implementation
        supportFragmentManager.beginTransaction()
                .replace(R.id.flInfoSettingsSection, LegalInfoSettingsFragment())
                .commit()

        FontUtil.setFont(this, toolbar, vm.getFont())
        // todo set fonts in settings better
        GlobalScope.launch(Dispatchers.Main) {
            FontUtil.setFont(this@LegalInfoActivity, vm.getFont())
        }
    }

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            } else with(it) {
                MaterialDialog(this@LegalInfoActivity)
                        .title(text = dialogTitle)
                        .message(text = dialogText) // todo put online
                        .positiveButton(text = "OK")
                        .show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        vm.pressBackButton()
        return true
    }

    override fun onBackPressed() {
        vm.pressBackButton()
    }
}
