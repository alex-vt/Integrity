/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.destinations

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.android.util.FontUtil
import com.alexvt.integrity.lib.android.util.ThemedActivity
import com.alexvt.integrity.lib.android.util.IntentUtil
import com.alexvt.integrity.android.ui.common.SpeedDialUtil
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_folder_locations.*
import javax.inject.Inject

class DestinationsActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[DestinationsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_locations)

        bindToolbar()
        bindDestinationList()
        bindFloatingButton()
        bindDoneButton()
        bindNavigation()
    }

    private fun bindToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private fun bindDestinationList() {
        rvFolderLocationList.adapter = DestinationRecyclerAdapter(ArrayList(),
                vm.isSelectMode(), this,
                vm.destinationUtilManager, // todo pass names directly
                { vm.clickDestination(it) },
                { askRemoveDestination(it.title) })
        vm.destinationListData.observe(this, Observer {
            (rvFolderLocationList.adapter as DestinationRecyclerAdapter).setItems(it)
        })
    }

    private fun bindFloatingButton() {
        // Destinations on the floating sub-buttons are static
        vm.getDestinationNames().forEachIndexed { index, name -> sdAdd.addActionItem(
                SpeedDialUtil.getActionItem(this, index, CommunityMaterial.Icon2.cmd_plus,
                        name, vm.getThemeColors()))
        }
        sdAdd.setOnActionSelectedListener { speedDialActionItem ->
            vm.clickFloatingSubButton(speedDialActionItem.id)
            false
        }
        FontUtil.setFont(this, sdAdd, vm.getFont())
    }

    private fun bindDoneButton() {
        bDone.visibility = if (vm.isSelectMode()) View.VISIBLE else View.GONE
        bDone.setOnClickListener { vm.clickDone() }
    }

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            } else if (it.returnData) {
                val returnIntent = Intent()
                IntentUtil.putSnapshot(returnIntent, it.bundledSnapshot)
                IntentUtil.putTitle(returnIntent, it.bundledTitle)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            } else {
                val intent = Intent()
                intent.component = ComponentName(it.targetPackage, it.targetClass)
                IntentUtil.putSnapshot(intent, it.bundledSnapshot)
                IntentUtil.putTitle(intent, it.bundledTitle)
                startActivityForResult(intent, 0)
            }
        })
    }

    private fun askRemoveDestination(title: String) {
        MaterialDialog(this)
                .title(text = "Remove $title from saved destinations?\nArchives there will remain.")
                .positiveButton(text = "Remove") {
                    vm.removeDestination(title)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        vm.pressBackButton()
        return true
    }

    override fun onBackPressed() = vm.pressBackButton()
}
