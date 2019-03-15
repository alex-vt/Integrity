/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations.samba

import androidx.lifecycle.MutableLiveData
import com.alexvt.integrity.core.credentials.CredentialsRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.destinations.samba.SambaFolderLocation
import com.alexvt.integrity.lib.destinations.samba.SambaFolderLocationCredentials
import com.alexvt.integrity.ui.ThemedViewModel
import com.alexvt.integrity.ui.util.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

data class NavigationEvent(
        val goBack: Boolean = false,
        val inputError: InputError? = null
)

enum class InputError {
    EMPTY_NAME, EMPTY_PATH, EMPTY_USER, EMPTY_PASSWORD, ALREADY_EXISTS
}

data class InputState(
        val title: String,
        val path: String,
        val user: String,
        val password: String
)

class SambaDestinationViewModel @Inject constructor(
        override val settingsRepository: SettingsRepository,
        val credentialsRepository: CredentialsRepository,
        @Named("editedSambaDestinationTitle") val editedDestinationTitle: String?,
        @Named("defaultSambaDestinationTitle") val defaultTitle: String
        ) : ThemedViewModel() {
    private val pathPrefix = "smb://"

    val inputStateData = MutableLiveData<InputState>()

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()

    init {
        // input state  pre-filled depending on if in edit mode
        inputStateData.value = if (isEditMode()) {
            val folderLocation = settingsRepository.getAllFolderLocations()
                    .first { it.title == editedDestinationTitle } as SambaFolderLocation
            val credentials = credentialsRepository.getCredentials(editedDestinationTitle!!)
                    as SambaFolderLocationCredentials
            InputState(folderLocation.title, folderLocation.fullPath.removePrefix(pathPrefix),
                    credentials.user, password = "") // user has to input it again
        } else {
            InputState(defaultTitle, path = "", user = "", password = "")
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

    fun onNewUser(user: String) {
        updateInputState(inputStateData.value!!.copy(user = user))
    }

    fun onNewPassword(password: String) {
        updateInputState(inputStateData.value!!.copy(password = password))
    }

    fun clickSave() {
        if (checkInputs()) saveFolderLocationAndCredentials()
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

        updateInputState(inputStateData.value!!.copy(user = inputStateData.value!!.user.trim()))
        if (inputStateData.value!!.user.isEmpty()) {
            navigationEventData.value = NavigationEvent(inputError = InputError.EMPTY_USER)
            return false
        }
        if (inputStateData.value!!.password.isEmpty()) {
            navigationEventData.value = NavigationEvent(inputError = InputError.EMPTY_PASSWORD)
            return false
        }

        // when creating new location, it must have unique title
        val titleAlreadyExists = settingsRepository.getAllFolderLocations()
                .any { it.title == inputStateData.value!!.title }
        if (!isEditMode() && titleAlreadyExists) {
            navigationEventData.value = NavigationEvent(inputError = InputError.ALREADY_EXISTS)
            return false
        }
        return true
    }

    private fun saveFolderLocationAndCredentials() {
        val folderLocation = with(inputStateData.value!!) {
            SambaFolderLocation(title, pathPrefix + path)
        }
        val credentials = with(inputStateData.value!!) {
            SambaFolderLocationCredentials(title, user, password)
        }
        // the old ones are removed first
        settingsRepository.removeFolderLocation(folderLocation.title)
        credentialsRepository.removeCredentials(credentials.title)
        settingsRepository.addFolderLocation(folderLocation)
        credentialsRepository.addCredentials(credentials)
        navigationEventData.value = NavigationEvent(goBack = true)
    }

    fun pressBackButton() {
        navigationEventData.value = NavigationEvent(goBack = true)
    }

}
