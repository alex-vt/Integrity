/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.destinations.local

import androidx.lifecycle.MutableLiveData
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.lib.core.data.destinations.LocalFolderLocation
import com.alexvt.integrity.android.ui.common.RxAutoDisposeThemedViewModel
import com.alexvt.integrity.android.ui.common.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

data class NavigationEvent(
        val goBack: Boolean = false,
        val inputError: InputError? = null
)

enum class InputError {
    EMPTY_NAME, EMPTY_PATH, ALREADY_EXISTS
}

data class InputState(
        val title: String,
        val path: String
)

class LocalDestinationViewModel @Inject constructor(
        override val settingsRepository: SettingsRepository,
        @Named("editedLocalDestinationTitle") val editedDestinationTitle: String?,
        @Named("defaultLocalDestinationTitle") val defaultTitle: String
        ) : RxAutoDisposeThemedViewModel() {

    val inputStateData = MutableLiveData<InputState>()

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()

    init {
        // input state  pre-filled depending on if in edit mode
        inputStateData.value = if (isEditMode()) {
            with(settingsRepository.getAllFolderLocations()
                    .first { it.title == editedDestinationTitle } as LocalFolderLocation) {
                InputState(title, path)
            }
        } else {
            InputState(defaultTitle, "")
        }
    }

    private fun updateInputState(inputState: InputState) {
        inputStateData.value = inputState
    }

    fun isEditMode() = !editedDestinationTitle.isNullOrBlank()


    // user actions

    fun onNewTitle(title: String) {
        updateInputState(inputStateData.value!!.copy(title = title))
    }

    fun onNewPath(path: String) {
        updateInputState(inputStateData.value!!.copy(path = path))
    }

    fun clickSave() {
        if (checkInputs()) saveFolderLocation()
    }

    private fun checkInputs(): Boolean {
        updateInputState(inputStateData.value!!.copy(title = inputStateData.value!!.title.trim()))
        if (inputStateData.value!!.title.isEmpty()) {
            navigationEventData.value = NavigationEvent(inputError = InputError.EMPTY_NAME)
            return false
        }
        updateInputState(inputStateData.value!!.copy(path = inputStateData.value!!.path
                .trim().trim('/')))
        if (inputStateData.value!!.path.isEmpty()) {
            navigationEventData.value = NavigationEvent(inputError = InputError.EMPTY_PATH)
            return false
        }
        // todo check path

        // when creating new location, it must have unique title
        val titleAlreadyExists = settingsRepository.getAllFolderLocations()
                .any { it.title == inputStateData.value!!.title }
        if (!isEditMode() && titleAlreadyExists) {
            navigationEventData.value = NavigationEvent(inputError = InputError.ALREADY_EXISTS)
            return false
        }
        return true
    }

    private fun saveFolderLocation() {
        val folderLocation = with(inputStateData.value!!) {
            LocalFolderLocation(title, path)
        }
        // the old one is removed first
        settingsRepository.removeFolderLocation(folderLocation.title)
        settingsRepository.addFolderLocation(folderLocation)
        navigationEventData.value = NavigationEvent(goBack = true)
    }

    fun pressBackButton() {
        navigationEventData.value = NavigationEvent(goBack = true)
    }

}
