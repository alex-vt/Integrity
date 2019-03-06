/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.alexvt.integrity.R
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.FontUtil
import com.alexvt.integrity.core.util.ThemeUtil
import com.alexvt.integrity.core.util.ThemedActivity
import com.alexvt.integrity.lib.util.IntentUtil
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener

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
                    .setColor(ThemeUtil.getColorBackground(IntegrityCore.getColors()))
                    .setPresets(resources.getIntArray(R.array.colorsBackground))
                    .create()
            dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onColorSelected(dialogId: Int, color: Int) {
                    saveAndApplyColorBackground(context!!, color)
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
                    .setColor(ThemeUtil.getColorPrimary(IntegrityCore.getColors()))
                    .setPresets(resources.getIntArray(R.array.colorsPrimary))
                    .create()
            dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onColorSelected(dialogId: Int, color: Int) {
                    saveAndApplyColorPrimary(context!!, color)
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
                    .setColor(ThemeUtil.getColorAccent(IntegrityCore.getColors()))
                    .setPresets(resources.getIntArray(R.array.colorsPrimary))
                    .create()
            dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
                override fun onColorSelected(dialogId: Int, color: Int) {
                    saveAndApplyColorAccent(context!!, color)
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
                            saveFont(context!!, selectedFontName)
                            prefTextFont.summary = selectedFontName
                            FontUtil.setFont(activity!!, IntegrityCore.getFont())
                            activity!!.setResult(Activity.RESULT_OK, IntentUtil.withRecreate(true))
                        }
                    }.show()
            true
        }
    }



    fun saveAndApplyColorBackground(context: Context, intColor: Int) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                colorBackground = ThemeUtil.getHexColor(intColor))
        )
        applyColors()
    }

    fun saveAndApplyColorPrimary(context: Context, intColor: Int) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                colorPrimary = ThemeUtil.getHexColor(intColor))
        )
        applyColors()
    }

    fun saveAndApplyColorAccent(context: Context, intColor: Int) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                colorAccent = ThemeUtil.getHexColor(intColor))
        )
        applyColors()
    }

    private fun applyColors() {
        ThemeUtil.applyThemeAndRecreate(activity as ThemedActivity, IntegrityCore.getColors())
    }

    fun saveFont(context: Context, fontName: String) {
        IntegrityCore.settingsRepository.set(context, IntegrityCore.settingsRepository.get().copy(
                textFont = fontName)
        )
    }
}