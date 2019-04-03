/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.common

import androidx.lifecycle.ViewModel
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.lib.core.util.ColorUtil
import com.alexvt.integrity.lib.core.util.ThemeColors
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class RxAutoDisposeThemedViewModel : ViewModel() {

    private val disposeOnClear = CompositeDisposable()

    private var taggedDisposables: Map<String, Disposable> = emptyMap()

    override fun onCleared() {
        super.onCleared()
        disposeOnClear.clear()
        taggedDisposables = emptyMap()
    }

    fun Disposable.untilCleared() = untilClearedOrUpdated(null)

    fun Disposable.untilClearedOrUpdated(tag: String?) {
        if (tag != null) {
            if (taggedDisposables.containsKey(tag)) {
                taggedDisposables[tag]!!.dispose()
                taggedDisposables = taggedDisposables.minus(tag)
            }
            taggedDisposables = taggedDisposables.plus(tag to this)
        }
        disposeOnClear.add(this)
    }


    abstract val settingsRepository: SettingsRepository

    fun getFont() = settingsRepository.get().textFont

    fun computeColorPrimary() = ColorUtil.getColorPrimary(getThemeColors())

    fun computeColorPrimaryDark() = ColorUtil.getColorPrimaryDark(getThemeColors())

    fun computeTextColorPrimary() = ColorUtil.getTextColorPrimary(getThemeColors())

    fun computeTextColorSecondary() = ColorUtil.getTextColorSecondary(getThemeColors())

    fun computeColorAccent() = ColorUtil.getColorAccent(getThemeColors())

    fun computeColorBackground() = ColorUtil.getColorBackground(getThemeColors())

    fun computeColorBackgroundSecondary() = ColorUtil.getColorBackgroundSecondary(getThemeColors())

    fun computeColorBackgroundBleached() = ColorUtil.getColorBackgroundBleached(getThemeColors())

    fun getThemeColors() = with(settingsRepository.get()) {
        ThemeColors(colorBackground, colorPrimary, colorAccent)
    }
}
