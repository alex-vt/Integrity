/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.settings.IntegrityAppSettings
import com.alexvt.integrity.core.util.ThemeColors
import com.alexvt.integrity.core.util.ThemeUtil
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.ui.util.SingleLiveEvent


class DestinationsViewModelFactory(
        val packageName: String,
        val isSelectMode: Boolean,
        val snapshotWithInitialDestination: Snapshot?
) : ViewModelProvider.Factory {
    // Pass type parameter to instance if needed for initial state
    override fun <T : ViewModel> create(modelClass: Class<T>)
            = DestinationsViewModel(packageName, isSelectMode, snapshotWithInitialDestination) as T
}

/**
 * The minimal set of data about screen state that comes from user input,
 * but cannot be obtained from repositories,
 * and together with data from repositories can be rendered in UI.
 */
data class DestinationsInputState(
        val selectedDestinations: List<FolderLocation>
)

data class NavigationEvent(
        val targetPackage: String,
        val targetClass: String,
        val goBack: Boolean = false,
        val returnData: Boolean = false,

        // bundled data to attach when goBack is false
        val bundledSnapshot: Snapshot? = null,
        val bundledTitle: String? = null
)

class DestinationsViewModel(
        private val packageName: String,
        private val isSelectMode: Boolean,
        private val snapshotWithInitialDestination: Snapshot?
        ) : ViewModel() {

    private val inputStateData = MutableLiveData<DestinationsInputState>()
    private val settingsData = MutableLiveData<IntegrityAppSettings>()

    // depends on  inputStateData, settingsData, snapshotWithInitialDestination
    val destinationListData = MutableLiveData<List<Pair<FolderLocation, Boolean>>>()

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()

    init {
        // input state  starts with destinations from possible snapshot pre-selected
        inputStateData.value = DestinationsInputState(
                selectedDestinations = getDestinationsFromEditedSnapshot()
        )

        // settings (contain preset destinations)  are listened to in their repository
        settingsData.value = IntegrityCore.settingsRepository.get()
        IntegrityCore.settingsRepository.addChangesListener(this.toString()) {
            settingsData.value = it
            updateDestinationList()
        }

        updateDestinationList()
    }

    private fun getDestinationsFromEditedSnapshot()
            = snapshotWithInitialDestination?.archiveFolderLocations?.toList() ?: emptyList()

    private fun getUserPreSetDestinations() = settingsData.value!!.dataFolderLocations.toList()

    private fun updateDestinationList() {
        val listedDestinations = linkedSetOf<FolderLocation>()
                .plus(getDestinationsFromEditedSnapshot())
                .plus(getUserPreSetDestinations())
        val selectedDestinations = inputStateData.value!!.selectedDestinations

        val listedDestinationsWithSelection = listedDestinations.map {
            Pair(it, selectedDestinations.contains(it))
        }
        destinationListData.value = listedDestinationsWithSelection
    }

    private fun updateInputState(inputState: DestinationsInputState) {
        inputStateData.value = inputState
        updateDestinationList()
    }


    fun isSelectMode() = isSelectMode

    fun getDestinationNames() = IntegrityCore.getDestinationNames()

    fun getFont() = settingsData.value!!.textFont

    fun computeColorPrimaryDark() = ThemeUtil.getColorPrimaryDark(getThemeColors())

    fun computeTextColorPrimary() = ThemeUtil.getTextColorPrimary(getThemeColors())

    fun computeTextColorSecondary() = ThemeUtil.getTextColorSecondary(getThemeColors())

    fun computeColorAccent() = ThemeUtil.getColorAccent(getThemeColors())

    fun computeColorBackground() = ThemeUtil.getColorBackground(getThemeColors())

    fun computeColorBackgroundSecondary() = ThemeUtil.getColorBackgroundSecondary(getThemeColors())

    fun computeColorBackgroundBleached() = ThemeUtil.getColorBackgroundBleached(getThemeColors())

    fun getThemeColors() = with(settingsData.value!!) {
        ThemeColors(colorBackground, colorPrimary, colorAccent)
    }


    // content user actions

    fun clickDestination(destination: FolderLocation) =
            if (isSelectMode()) toggleDestination(destination) else viewDestination(destination)

    private fun toggleDestination(destination: FolderLocation) {
        val selectedDestinations = with (inputStateData.value!!.selectedDestinations) {
            if (contains(destination)) minus(destination) else plus(destination)
        }
        updateInputState(inputStateData.value!!.copy(selectedDestinations = selectedDestinations))
    }

    private fun viewDestination(destination: FolderLocation) {
        val component = IntegrityCore.getDestinationComponent(destination.title)
        navigationEventData.value = NavigationEvent(targetPackage = component.packageName,
                targetClass = component.className, bundledTitle = destination.title)
    }

    fun removeDestination(title: String) {
        IntegrityCore.settingsRepository.removeFolderLocation(IntegrityCore.context, title)
        IntegrityCore.credentialsRepository.removeCredentials(IntegrityCore.context, title)
    }

    fun clickDone() {
        navigationEventData.value = NavigationEvent("", "",
                returnData = true,
                bundledSnapshot = snapshotWithInitialDestination?.copy(
                        archiveFolderLocations = ArrayList(inputStateData.value!!.selectedDestinations)
                ))
    }


    // floating button user actions

    fun clickFloatingSubButton(index: Int) {
        val destinationClass = IntegrityCore.getDestinationClasses()[index]
        val viewComponent = IntegrityCore.getDestinationUtil(destinationClass)
                .getViewMainActivityComponent()
        navigationEventData.value = NavigationEvent(viewComponent.packageName,
                viewComponent.className)
    }


    // menus user action

    fun clickViewOptionsIcon() {
        // todo
    }

    fun pressBackButton() {
        navigationEventData.value = NavigationEvent("", "", goBack = true)
    }

}
