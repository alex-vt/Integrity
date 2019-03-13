/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.info

import android.os.Bundle
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
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
    lateinit var integrityCore: IntegrityCore

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        toolbar.title = "Legal"
        // todo other implementation
        supportFragmentManager.beginTransaction()
                .replace(R.id.flInfoSettingsSection, LegalInfoSettingsFragment())
                .commit()

        FontUtil.setFont(this, toolbar, integrityCore.getFont())
        // todo set fonts in settings better
        GlobalScope.launch(Dispatchers.Main) {
            FontUtil.setFont(this@LegalInfoActivity, integrityCore.getFont())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}
