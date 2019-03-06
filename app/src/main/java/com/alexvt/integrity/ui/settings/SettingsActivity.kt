/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.FontUtil
import com.alexvt.integrity.core.util.ThemedActivity
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.ui.recovery.RecoveryActivity
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotlinx.android.synthetic.main.activity_settings.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class SettingsActivity : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        setBottomNavigationIcons()

        bindBottomNavigation()

        FontUtil.setFont(this, IntegrityCore.getFont())
    }

    private fun setBottomNavigationIcons() {
        // Custom icon support // todo improve
        // see https://github.com/mikepenz/Android-Iconics/issues/270#issuecomment-296468885
        val bottomMenuItemColorStates = ColorStateList(arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
        ), intArrayOf(
                getColor(R.color.colorWhite),
                getColor(R.color.colorLight),
                getColor(R.color.colorShade))
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
    }

    private fun bindBottomNavigation() {
        // todo set fonts better
        supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentResumed(fm, f)
                        GlobalScope.launch(Dispatchers.Main) {
                            if (f.view != null) {
                                FontUtil.setFont(this@SettingsActivity, f.view!!,
                                        IntegrityCore.getFont())
                            }
                        }
                    }
                }, true)

        bnView.setOnNavigationItemSelectedListener { item ->
            toolbar.subtitle = item.title
            val sectionFragment = when (item.itemId) {
                R.id.action_appearance -> AppearanceSettingsFragment()
                R.id.action_behavior -> BehaviorSettingsFragment()
                R.id.action_data -> DataSettingsFragment()
                R.id.action_notifications -> NotificationSettingsFragment()
                else -> ExtensionSettingsFragment()
            }
            supportFragmentManager.beginTransaction()
                    .replace(R.id.flSettingSection, sectionFragment)
                    .commit()
            true
        }
        bnView.selectedItemId = when {
            IntentUtil.getViewExtensions(intent) -> R.id.action_extensions
            else -> R.id.action_appearance
        }
        // todo refresh on settings change
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_restore -> {
            viewRecovery()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun viewRecovery() {
        startActivity(Intent(this, RecoveryActivity::class.java))
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }
}
