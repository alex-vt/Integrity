/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.recovery

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.util.FontUtil
import com.alexvt.integrity.core.util.Initializable
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.util.ThemedActivity
import com.alexvt.integrity.lib.util.ViewExternalUtil
import com.alexvt.integrity.lib.util.IntentUtil
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_recovery.*
import javax.inject.Inject


class RecoveryActivity : ThemedActivity() {
    @Inject
    lateinit var integrityCore: IntegrityCore
    @Inject
    lateinit var dataFolderManager: DataFolderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        FontUtil.setFont(this, integrityCore.getFont())

        val issueDescription = IntentUtil.getIssueDescription(intent)
        tvIssueDescription.text = if (issueDescription.isNotBlank()) {
            "Issue description:\n $issueDescription"
        } else {
            ""
        }

        bClearData.setOnClickListener { askSelectClearData() }
        bClearSnapshots.setOnClickListener { askClearSnapshots() }
        bOpenAppInfo.setOnClickListener { ViewExternalUtil.viewAppInfo(this, packageName) }
        bRestartApp.setOnClickListener { AppRestartUtil.restartApp(this) }
    }

    private fun askSelectClearData() {
        val namedRepositories = listOf(
                integrityCore.metadataRepository to "Snapshots metadata",
                integrityCore.logRepository to "Log",
                integrityCore.settingsRepository to "App settings",
                integrityCore.credentialsRepository to "Credentials",
                integrityCore.searchIndexRepository to "Search index"
        )
        MaterialDialog(this)
                .title(text = "Select data to clear")
                .positiveButton(text = "Clear selected")
                .listItemsMultiChoice(items = namedRepositories.map { it.second }) { _, indices, _ ->
                    askClearData(namedRepositories.filterIndexed { index, _ -> index in indices })
                }.show()
    }

    private fun askClearData(namedRepositoriesToClear: List<Pair<Initializable, String>>) {
        val listedNames = namedRepositoriesToClear.map { it.second }.joinToString(separator = ", ")
        MaterialDialog(this)
                .title(text = "Are you sure?")
                .message(text = "About to clear $listedNames")
                .negativeButton(text = "Cancel")
                .positiveButton(text = "Clear now") {
                    namedRepositoriesToClear.map { it.first }
                            .forEach { it.init(true) }
                }.show()
    }

    private fun askClearSnapshots() {
        val dataFolderName = integrityCore.getDataFolderName()
        MaterialDialog(this)
                .title(text = "Are you sure?")
                .message(text = "About to delete folder from storage: \n\uD83D\uDCF1/" +
                        "$dataFolderName \nwith all downloaded snapshots")
                .negativeButton(text = "Cancel")
                .positiveButton(text = "Delete now") {
                    dataFolderManager.deleteFolder(dataFolderName)
                }.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}
