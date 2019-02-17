/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.preference.*
import com.alexvt.integrity.R
import com.alexvt.integrity.core.util.ThemeUtil
import com.alexvt.integrity.lib.util.IntentUtil
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotlinx.android.synthetic.main.activity_settings.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity


class SettingsActivity : CyaneaAppCompatActivity() {

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

        bnView.setOnNavigationItemSelectedListener { item ->
            toolbar.subtitle = item.title
            val sectionFragment = when (item.itemId) {
                R.id.action_appearance -> AppearanceSettingsFragment()
                R.id.action_behavior -> BehaviorSettingsFragment()
                R.id.action_data -> DataSettingsFragment()
                R.id.action_notifications -> NotificationSettingsFragment()
                else -> ExtensionSettingsFragment()
            }
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.flSettingSection, sectionFragment)
                    .commit()
            true
        }
        bnView.selectedItemId = when {
            IntentUtil.getViewExtensions(intent) -> R.id.action_extensions
            else -> R.id.action_appearance
        }
    }

    class AppearanceSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_appearance, rootKey)

            val prefColorBackground: Preference = findPreference("appearance_color_background")
            val prefColorPrimary: Preference = findPreference("appearance_color_main")
            val prefColorAccent: Preference = findPreference("appearance_color_accent")

            prefColorBackground.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val dialog = ColorPickerDialog.newBuilder()
                        .setAllowCustom(false)
                        .setShowColorShades(false)
                        .setColor(ThemeUtil.getColorBackground())
                        .setPresets(resources.getIntArray(R.array.colorsBackground))
                        .create()
                dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                    override fun onColorSelected(dialogId: Int, color: Int) {
                        ThemeUtil.saveColorBackground(context!!, color)
                        (activity as SettingsActivity).applyTheme()
                    }

                    override fun onDialogDismissed(dialogId: Int) { }
                })
                dialog.show(fragmentManager!!, "ColorPickerDialog")
                true
            }
            prefColorPrimary.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val dialog = ColorPickerDialog.newBuilder()
                        .setAllowCustom(false)
                        .setShowColorShades(false)
                        .setColor(ThemeUtil.getColorPrimary())
                        .setPresets(resources.getIntArray(R.array.colorsPrimary))
                        .create()
                dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                    override fun onColorSelected(dialogId: Int, color: Int) {
                        ThemeUtil.saveColorPrimary(context!!, color)
                        (activity as SettingsActivity).applyTheme()
                    }

                    override fun onDialogDismissed(dialogId: Int) { }
                })
                dialog.show(fragmentManager!!, "ColorPickerDialog")
                true
            }
            prefColorAccent.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val dialog = ColorPickerDialog.newBuilder()
                        .setAllowCustom(false)
                        .setShowColorShades(false)
                        .setColor(ThemeUtil.getColorAccent())
                        .setPresets(resources.getIntArray(R.array.colorsPrimary))
                        .create()
                dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                    override fun onColorSelected(dialogId: Int, color: Int) {
                        ThemeUtil.saveColorAccent(context!!, color)
                        (activity as SettingsActivity).applyTheme()
                    }

                    override fun onDialogDismissed(dialogId: Int) { }
                })
                dialog.show(fragmentManager!!, "ColorPickerDialog")
                true
            }
            //applyTheme() // todo propagate to all UI
        }

    }

    companion object {

    }
    private fun applyTheme() {
        cyanea.edit {
            primary(ThemeUtil.getColorPrimary())
            primaryDark(ThemeUtil.getColorPrimaryDark())
            accent(ThemeUtil.getColorAccent())
            shouldTintNavBar(true)
            shouldTintStatusBar(true)
            navigationBar(ThemeUtil.getColorPrimaryDark())
            background(ThemeUtil.getColorBackground())
        }.recreate(this)
    }

    class BehaviorSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_behavior, rootKey)
        }
    }

    class DataSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_data, rootKey)
        }
    }

    class NotificationSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_notifications, rootKey)
        }
    }

    class ExtensionSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_extensions, rootKey)
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
