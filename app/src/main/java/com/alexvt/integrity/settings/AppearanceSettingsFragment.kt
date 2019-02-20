/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.settings

import android.app.Activity
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.FontUtil
import com.alexvt.integrity.core.util.ThemeUtil
import com.alexvt.integrity.lib.util.IntentUtil
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity

class AppearanceSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_appearance, rootKey)

        val prefColorBackground: Preference = findPreference("appearance_color_background")
        val prefColorPrimary: Preference = findPreference("appearance_color_main")
        val prefColorAccent: Preference = findPreference("appearance_color_accent")
        val prefTextFont: Preference = findPreference("appearance_text_font")

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
                    ThemeUtil.applyThemeAndRecreate(activity as CyaneaAppCompatActivity)
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
                    ThemeUtil.applyThemeAndRecreate(activity as CyaneaAppCompatActivity)
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
                    ThemeUtil.applyThemeAndRecreate(activity as CyaneaAppCompatActivity)
                }

                override fun onDialogDismissed(dialogId: Int) { }
            })
            dialog.show(fragmentManager!!, "ColorPickerDialog")
            true
        }

        val currentFontName = IntegrityCore.settingsRepository.get().textFont
        prefTextFont.summary = currentFontName
        prefTextFont.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val fontNames = listOf("Default").plus(FontUtil.getNames())
            val currentFontName = IntegrityCore.settingsRepository.get().textFont
            val currentFontIndex = Math.max(fontNames.indexOf(currentFontName), 0) // default 0
            MaterialDialog(context!!)
                    .title(text = "Select font")
                    .listItemsSingleChoice(items = fontNames,
                            initialSelection = currentFontIndex) { dialog, index, text ->
                        val selectedFontName = fontNames[index]
                        if (selectedFontName != currentFontName) {
                            FontUtil.saveFont(context!!, selectedFontName)
                            prefTextFont.summary = selectedFontName
                            FontUtil.setFont(activity!!)
                            activity!!.setResult(Activity.RESULT_OK, IntentUtil.withRecreate(true))
                        }
                    }.show()
            true
        }
    }
}