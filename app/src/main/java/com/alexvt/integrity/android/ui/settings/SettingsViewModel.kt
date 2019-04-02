/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.settings

import androidx.lifecycle.MutableLiveData
import com.alexvt.integrity.core.operations.search.SortingUtil
import com.alexvt.integrity.core.data.settings.IntegrityAppSettings
import com.alexvt.integrity.core.data.settings.ListViewMode
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.data.types.DataTypeRepository
import com.alexvt.integrity.lib.core.operations.filesystem.FilesystemManager
import com.alexvt.integrity.lib.android.util.FontUtil
import com.alexvt.integrity.lib.android.util.ThemeUtil
import com.alexvt.integrity.android.ui.common.RxAutoDisposeThemedViewModel
import com.alexvt.integrity.android.ui.common.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

data class InputState(
        val tabId: Int
)

data class NavigationEvent(
        val goBack: Boolean = false,
        val applyTheme: Boolean = false,
        val viewAppInfo: Boolean = false,
        val targetPackage: String = "",
        val targetClass: String = ""
)

class SettingsViewModel @Inject constructor(
        @Named("initialTabId") val initialTabId: Int,
        @Named("colorsBackground") val colorsBackground: IntArray,
        @Named("colorsPalette") val colorsPalette: IntArray,
        @Named("packageName") val packageName: String,
        override val settingsRepository: SettingsRepository,
        private val filesystemManager: FilesystemManager,
        private val dataTypeRepository: DataTypeRepository, // todo listen to changes
        @Named("destinationsScreenClass") val destinationsScreenClass: String,
        @Named("recoveryScreenClass") val recoveryScreenClass: String,
        @Named("legalInfoScreenClass") val legalInfoScreenClass: String
        ) : RxAutoDisposeThemedViewModel() {

    val settingsData = MutableLiveData<IntegrityAppSettings>()
    val navigationEventData = SingleLiveEvent<NavigationEvent>()
    val inputStateData = MutableLiveData<InputState>()

    init {
        settingsData.value = settingsRepository.get()
        settingsRepository.addChangesListener(this.toString()) {
            val themeChanged = settingsData.value!!.textFont != it.textFont
                    || settingsData.value!!.colorBackground != it.colorBackground
                    || settingsData.value!!.colorPrimary != it.colorPrimary
                    || settingsData.value!!.colorAccent != it.colorAccent
            settingsData.value = it
            if (themeChanged) {
                navigationEventData.value = NavigationEvent(applyTheme = true)
            }
        }
        inputStateData.value = InputState(tabId = initialTabId)
    }

    override fun onCleared() {
        settingsRepository.removeChangesListener(this.toString())
        super.onCleared()
    }

    // user actions

    fun requestViewTabById(tabId: Int) {
        if (inputStateData.value!!.tabId != tabId) {
            inputStateData.value = inputStateData.value!!.copy(tabId = tabId)
        }
    }

    fun saveColorBackground(intColor: Int) {
        settingsRepository.set(settingsRepository.get().copy(colorBackground = ThemeUtil.getHexColor(intColor)))
        navigateApplyTheme()
    }

    fun saveColorPrimary(intColor: Int) {
        settingsRepository.set(settingsRepository.get().copy(colorPrimary = ThemeUtil.getHexColor(intColor)))
        navigateApplyTheme()
    }

    fun saveColorAccent(intColor: Int) {
        settingsRepository.set(settingsRepository.get().copy(colorAccent = ThemeUtil.getHexColor(intColor)))
        navigateApplyTheme()
    }

    fun saveFont(index: Int) {
        val fontName = getAllFontNames()[index]
        val fontChanged = (fontName != settingsRepository.get().textFont)
        settingsRepository.set(settingsRepository.get().copy(textFont = fontName))
        if (fontChanged) navigateApplyTheme()
    }

    private fun navigateApplyTheme() {
        navigationEventData.value = NavigationEvent(applyTheme = true)
    }

    fun getAllFontNames() = listOf("Default").plus(FontUtil.getNames())

    fun getCurrentFontIndex() = Math.max(
            getAllFontNames().indexOf(settingsRepository.get().textFont),
            0
    ) // default 0

    fun getAllSnapshotListViewTypeNames()
            = listOf(ListViewMode.LIST, ListViewMode.CARDS, ListViewMode.BIG_CARDS)

    fun getCurrentSnapshotListViewTypeIndex() = Math.max(
            getAllSnapshotListViewTypeNames().indexOf(settingsRepository.get().snapshotListViewMode),
            0
    ) // default 0

    fun saveSnapshotListViewType(index: Int) {
        val snapshotListViewTypeName = getAllSnapshotListViewTypeNames()[index]
        settingsRepository.set(settingsRepository.get()
                .copy(snapshotListViewMode = snapshotListViewTypeName))
    }

    fun toggleScheduledJobsEnabled() {
        settingsRepository.set(settingsRepository.get()
                .copy(jobsEnableScheduled = !settingsRepository.get().jobsEnableScheduled))
    }

    fun toggleScheduledJobsExpand() {
        settingsRepository.set(settingsRepository.get()
                .copy(jobsExpandScheduled = !settingsRepository.get().jobsExpandScheduled))
    }

    fun toggleRunningJobsExpand() {
        settingsRepository.set(settingsRepository.get()
                .copy(jobsExpandRunning = !settingsRepository.get().jobsExpandRunning))
    }

    fun toggleSearchFaster() {
        settingsRepository.set(settingsRepository.get()
                .copy(fasterSearchInputs = !settingsRepository.get().fasterSearchInputs))
    }

    // todo names from resources
    fun getAllSortingMethods() = SortingUtil.getSortingMethodNameMap().values.toList()

    fun getCurrentSortingMethodIndex() = SortingUtil.getSortingMethodNameMap().keys.toList()
            .indexOf(settingsRepository.get().sortingMethod)

    fun saveSortingMethod(index: Int) {
        val sortingMethod = SortingUtil.getSortingMethodNameMap().keys.toList()[index]
        settingsRepository.set(settingsRepository.get().copy(sortingMethod = sortingMethod))
    }


    val pathPrefix = filesystemManager.getRootFolder() + "/"

    fun getCurrentDataFolderRelativePath() = settingsRepository.get().dataFolderPath
            .removePrefix(pathPrefix)

    fun saveDataFolderPath(relativePath: String) {
        settingsRepository.set(settingsRepository.get()
                .copy(dataFolderPath = pathPrefix + relativePath))
    }

    fun toggleErrorNotificationsEnabled() {
        settingsRepository.set(settingsRepository.get()
                .copy(notificationShowErrors = !settingsRepository.get().notificationShowErrors))
    }

    fun toggleScheduledJobsDisabledNotification() {
        settingsRepository.set(settingsRepository.get()
                .copy(notificationShowDisabledScheduled = !settingsRepository.get()
                        .notificationShowDisabledScheduled))
    }


    fun viewDestinations() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = destinationsScreenClass)
    }

    fun viewRecovery() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = recoveryScreenClass)
    }

    fun viewLegal() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = legalInfoScreenClass)
    }


    fun getNonIncludedExtensions() = dataTypeRepository.getAllDataTypes()
            .filter { it.packageName != packageName } // except the built in types

    fun viewAppInfo(packageName: String) {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                viewAppInfo = true)
    }


    fun pressBackButton() {
        navigationEventData.value = NavigationEvent(goBack = true)
    }

}
