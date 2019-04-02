/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.settings

import androidx.lifecycle.Observer
import androidx.preference.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.alexvt.integrity.core.data.settings.IntegrityAppSettings
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener

abstract class SettingsFragment : PreferenceFragmentCompat() {

    protected abstract val vm: SettingsViewModel

    protected fun bindToggleSetting(key: String, settingSelector: (IntegrityAppSettings) -> Boolean,
                                    toggleAction: () -> Unit) {
        val togglePreference: TwoStatePreference = findPreference(key)
        vm.settingsData.observe(this, Observer {
            togglePreference.isChecked = settingSelector.invoke(it)
        })
        togglePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            toggleAction.invoke()
            true
        }
    }

    protected fun bindSelectionSetting(key: String, title: String, items: List<String>,
                                       initialSelection: Int,
                                       settingSelector: (IntegrityAppSettings) -> String,
                                       selectionAction: (Int) -> Unit) {
        val preference: Preference = findPreference(key)
        vm.settingsData.observe(this, Observer {
            preference.summary = settingSelector.invoke(it)
        })
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialDialog(context!!)
                    .title(text = title)
                    .listItemsSingleChoice(items = items,
                            initialSelection = initialSelection) { _, index, _ ->
                        selectionAction.invoke(index)
                    }.show()
            true
        }
    }

    protected fun bindTextSetting(key: String, title: String, hint: String,
                                  visibleSettingSelector: (IntegrityAppSettings) -> String,
                                  visibleSelectionAction: (String) -> Unit) {
        val preference: Preference = findPreference(key)
        vm.settingsData.observe(this, Observer {
            preference.summary = visibleSettingSelector.invoke(it)
        })
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            MaterialDialog(context!!).show {
                title(text = title)
                input(hint = hint, prefill = visibleSettingSelector.invoke(vm.settingsData.value!!)) {
                    _, text -> visibleSelectionAction.invoke(text.toString())
                }
                positiveButton(text = "Change")
            }
            true
        }
    }

    protected fun bindSimpleActionSetting(key: String, action: () -> Unit) {
        val preference: Preference = findPreference(key)
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            action.invoke()
            true
        }
    }

    protected fun addSimpleActionSetting(categoryKey: String, title: String, summary: String,
                                            action: () -> Unit) {
        val category: PreferenceCategory = findPreference(categoryKey)
        val preference = Preference(context)
        preference.title = title
        preference.summary = summary

        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            action.invoke()
            true
        }

        category.addPreference(preference)
    }

    protected fun bindColorSetting(key: String, initialColor: Int, colorPalette: IntArray,
                                   selectionAction: (Int) -> Unit) {
        val preference: Preference = findPreference(key)
        // todo icon
        preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showColorPickerDialog(initialColor, colorPalette) {
                selectionAction.invoke(it)
            }
            true
        }
    }

    private fun showColorPickerDialog(selectedColorInt: Int, colorsInt: IntArray,
                                        colorSelectAction: (Int) -> Unit) {
        val dialog = ColorPickerDialog.newBuilder()
                .setAllowCustom(false)
                .setShowColorShades(false)
                .setColor(selectedColorInt)
                .setPresets(colorsInt)
                .create()
        dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
            override fun onColorSelected(dialogId: Int, color: Int) {
                colorSelectAction.invoke(color)
            }

            override fun onDialogDismissed(dialogId: Int) { }
        })
        dialog.show(fragmentManager!!, "ColorPickerDialog")
    }
}