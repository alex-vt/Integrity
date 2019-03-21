/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations

import android.content.ComponentName
import androidx.lifecycle.MutableLiveData
import com.alexvt.integrity.core.credentials.CredentialsRepository
import com.alexvt.integrity.lib.destinations.DestinationNameUtilResolver
import com.alexvt.integrity.core.destinations.DestinationUtilResolver
import com.alexvt.integrity.core.settings.IntegrityAppSettings
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.metadata.FolderLocation
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.ui.ThemedViewModel
import com.alexvt.integrity.ui.util.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

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

class DestinationsViewModel @Inject constructor(
        override val settingsRepository: SettingsRepository,
        val credentialsRepository: CredentialsRepository,
        @Named("selectMode") val selectMode: Boolean,
        @Named("snapshotWithInitialDestination") val snapshotWithInitialDestination: Snapshot?
        ) : ThemedViewModel() {

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
        settingsData.value = settingsRepository.get()
        settingsRepository.addChangesListener(this.toString()) {
            settingsData.value = it
            updateDestinationList()
        }

        updateDestinationList()
    }

    override fun onCleared() {
        settingsRepository.removeChangesListener(this.toString())
        super.onCleared()
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


    fun isSelectMode() = selectMode

    /**
     * Gets list of archive destination labels.
     */
    fun getDestinationNames() = DestinationUtilResolver.getDestinationClasses().map {
        DestinationNameUtilResolver.getDestinationNameUtil(it).getFolderLocationLabel()
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
        val component = getDestinationComponent(destination.title)
        navigationEventData.value = NavigationEvent(targetPackage = component.packageName,
                targetClass = component.className, bundledTitle = destination.title)
    }

    private fun getDestinationComponent(title: String): ComponentName {
        val folderLocation = settingsRepository.getAllFolderLocations()
                .first { it.title == title }
        return DestinationUtilResolver.get(folderLocation.javaClass)
                .getViewMainActivityComponent()
    }


    fun removeDestination(title: String) {
        settingsRepository.removeFolderLocation(title)
        credentialsRepository.removeCredentials(title)
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
        val destinationClass = DestinationUtilResolver.getDestinationClasses()[index]
        val viewComponent = DestinationUtilResolver.get(destinationClass)
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
