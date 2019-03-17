/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.util.FontUtil
import com.alexvt.integrity.lib.util.ThemeUtil
import com.alexvt.integrity.lib.util.ThemedActivity
import com.alexvt.integrity.lib.util.ViewExternalUtil
import com.jakewharton.rxbinding3.material.itemSelections
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotlinx.android.synthetic.main.activity_settings.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import dagger.android.AndroidInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class SettingsActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[SettingsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindToolbar()
        bindBottomNavigationIcons()
        bindBottomNavigation()
        bindNavigation()
    }

    private fun bindToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private fun bindBottomNavigation() {
        FontUtil.setFont(this, vm.getFont())
        // todo set fonts better
        supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentResumed(fm, f)
                        GlobalScope.launch(Dispatchers.Main) {
                            if (f.view != null) {
                                FontUtil.setFont(this@SettingsActivity, f.view!!,
                                        vm.getFont())
                            }
                        }
                    }
                }, true)

        bnView.itemSelections()
                .debounce(20, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { vm.requestViewTabById(it.itemId) }
        vm.inputStateData.observe(this, Observer {
            showTab(bnView.menu.findItem(it.tabId))
        })
    }

    private fun showTab(item: MenuItem) {
        android.util.Log.v("requestViewTabById", "$item")
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
        bnView.selectedItemId = item.itemId
    }

    private fun bindBottomNavigationIcons() {
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

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            } else if (it.viewAppInfo) {
                ViewExternalUtil.viewAppInfo(this, it.targetPackage)
            } else if (it.applyTheme) {
                ThemeUtil.applyThemeAndRecreate(this, vm.getThemeColors())
            } else with(it) {
                val intent = Intent()
                intent.component = ComponentName(targetPackage, targetClass)
                this@SettingsActivity.startActivity(intent)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_restore -> {
            vm.viewRecovery()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
