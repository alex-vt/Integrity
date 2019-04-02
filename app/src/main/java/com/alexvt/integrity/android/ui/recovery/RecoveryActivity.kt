/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.recovery

import android.os.Bundle
import android.text.TextUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.alexvt.integrity.R
import com.alexvt.integrity.android.util.AppRestartUtil
import com.alexvt.integrity.lib.android.util.FontUtil
import com.alexvt.integrity.lib.android.util.ThemedActivity
import com.alexvt.integrity.lib.android.util.ViewExternalUtil
import com.alexvt.integrity.lib.android.util.IntentUtil
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_recovery.*
import javax.inject.Inject


class RecoveryActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[RecoveryViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery)

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
        FontUtil.setFont(this, vm.getFont())

        val issueDescription = IntentUtil.getIssueDescription(intent)
        tvIssueDescription.text = if (!TextUtils.isEmpty(issueDescription)) {
            "Issue description:\n $issueDescription"
        } else {
            ""
        }

        bClearData.setOnClickListener { askSelectClearData() }
        bClearSnapshots.setOnClickListener { askClearSnapshots() }
        bOpenAppInfo.setOnClickListener { vm.viewAppInfo() }
        bRestartApp.setOnClickListener { vm.clickRestartApp() }
    }

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            } else if (it.viewAppInfo) {
                ViewExternalUtil.viewAppInfo(this, packageName)
            } else if (it.restartApp) {
                AppRestartUtil.restart(this)
            }
        })
    }

    private fun askSelectClearData() {
        MaterialDialog(this)
                .title(text = "Select data to clear")
                .positiveButton(text = "Clear selected")
                .listItemsMultiChoice(items = vm.getRepositoryNames()) { _, indices, _ ->
                    askClearData(indices)
                }.show()
    }

    private fun askClearData(indices: IntArray) {
        MaterialDialog(this)
                .title(text = "Are you sure?")
                .message(text = "About to clear ${vm.getRepositoryNamesAt(indices)}")
                .negativeButton(text = "Cancel")
                .positiveButton(text = "Clear now") {
                    vm.clickClearRepositoriesAt(indices)
                }.show()
    }

    private fun askClearSnapshots() {
        MaterialDialog(this)
                .title(text = "Are you sure?")
                .message(text = "Confirm to delete folder from storage: \n\uD83D\uDCF1/" +
                        "${vm.getSnapshotsFolderName()}\nwith all downloaded snapshots")
                .negativeButton(text = "Cancel")
                .positiveButton(text = "Delete now") {
                    vm.clickDeleteSnapshotsFolder()
                }.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        vm.pressBackButton()
    }
}
