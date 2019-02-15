/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.util.IntentUtil
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotlinx.android.synthetic.main.activity_settings.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.community_material_typeface_library.CommunityMaterial


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        // Custom icon support // todo improve
        // see https://github.com/mikepenz/Android-Iconics/issues/270#issuecomment-296468885
        val bottomMenuItemColorStates = ColorStateList(arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
        ), intArrayOf(
                getColor(R.color.colorPrimaryDark),
                getColor(R.color.colorPrimary),
                getColor(R.color.colorPrimaryLight))
        )
        bnView.itemTextColor = bottomMenuItemColorStates
        bnView.itemIconTintList = bottomMenuItemColorStates
        bnView.menu.getItem(0).icon = IconicsDrawable(this.applicationContext)
                .icon(CommunityMaterial.Icon.cmd_brush)
        bnView.menu.getItem(1).icon = IconicsDrawable(this.applicationContext)
                .icon(CommunityMaterial.Icon2.cmd_settings)
        bnView.menu.getItem(2).icon = IconicsDrawable(this.applicationContext)
                .icon(CommunityMaterial.Icon.cmd_database)
        bnView.menu.getItem(3).icon = IconicsDrawable(this.applicationContext)
                .icon(CommunityMaterial.Icon.cmd_bell_ring)
        bnView.menu.getItem(4).icon = IconicsDrawable(this.applicationContext)
                .icon(CommunityMaterial.Icon2.cmd_puzzle)

        bnView.setOnNavigationItemSelectedListener { item ->
            toolbar.subtitle = item.title
            when (item.itemId) {
                R.id.action_appearance -> {
                    // todo
                }
                R.id.action_behavior -> {
                    // todo
                }
                R.id.action_notifications -> {
                    // todo
                }
                R.id.action_extensions -> {
                    // todo
                }
            }
            true
        }
        bnView.selectedItemId = when {
            IntentUtil.getViewExtensions(intent) -> R.id.action_extensions
            else -> R.id.action_appearance
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_restore -> {
            // todo
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}
