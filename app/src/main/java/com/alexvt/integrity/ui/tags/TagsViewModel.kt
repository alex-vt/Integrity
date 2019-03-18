/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.tags

import androidx.lifecycle.MutableLiveData
import com.alexvt.integrity.core.settings.IntegrityAppSettings
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.Tag
import com.alexvt.integrity.ui.ThemedViewModel
import com.alexvt.integrity.ui.util.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

data class InputState(
        val selectedTags: List<Tag>,
        val editingTag: Boolean = false,
        val editOldTagName: String = "",
        val editedTagColor: String = "",
        val editTagName: String = "",
        val bundledSnapshot: Snapshot? = null
)

enum class InputError {
    EMPTY_NAME, ALREADY_EXISTS
}

data class NavigationEvent(
        val goBack: Boolean = false,

        val returnData: Boolean = false,
        val bundledSnapshot: Snapshot? = null,
        val inputError: InputError? = null
)

class TagsViewModel @Inject constructor(
        override val settingsRepository: SettingsRepository,
        @Named("selectModeTags") val selectMode: Boolean,
        @Named("snapshotWithInitialTags") val snapshotWithInitialTags: Snapshot?
        ) : ThemedViewModel() {

    val inputStateData = MutableLiveData<InputState>()
    private val settingsData = MutableLiveData<IntegrityAppSettings>()

    // depends on  inputStateData, settingsData, snapshotWithInitialTags
    val tagListData = MutableLiveData<List<Pair<Tag, Boolean>>>()

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()

    init {
        // input state  starts with destinations from possible snapshot pre-selected
        inputStateData.value = InputState(
                selectedTags = getTagsFromEditedSnapshot()
        )

        // settings (contain preset destinations)  are listened to in their repository
        settingsData.value = settingsRepository.get()
        settingsRepository.addChangesListener(this.toString()) {
            settingsData.value = it
            updateTagList()
        }

        updateTagList()
    }

    private fun getTagsFromEditedSnapshot()
            = snapshotWithInitialTags?.tags?.toList() ?: emptyList()

    private fun getUserPreSetTags() = settingsData.value!!.dataTags.toList()

    private fun updateTagList() {
        val listedTags = linkedSetOf<Tag>()
                .plus(getTagsFromEditedSnapshot())
                .plus(getUserPreSetTags())
        val selectedTags = inputStateData.value!!.selectedTags

        val listedTagsWithSelection = listedTags.map {
            Pair(it, selectedTags.contains(it))
        }
        tagListData.value = listedTagsWithSelection
    }

    private fun updateInputState(inputState: InputState) {
        inputStateData.value = inputState
        updateTagList()
    }


    fun isSelectMode() = selectMode


    // content user actions

    fun clickTag(tag: Tag) = if (isSelectMode()) toggleTag(tag) else viewEditTag(tag.text, tag.color)

    private fun toggleTag(tag: Tag) {
        val selectedTags = with (inputStateData.value!!.selectedTags) {
            if (contains(tag)) minus(tag) else plus(tag)
        }
        updateInputState(inputStateData.value!!.copy(selectedTags = selectedTags))
    }

    private fun viewEditTag(tagText: String, tagColor: String) {
        updateInputState(inputStateData.value!!.copy(editingTag = true,
                editOldTagName = tagText, editTagName = tagText, editedTagColor = tagColor))
    }

    fun updateEditColor(color: String) {
        updateInputState(inputStateData.value!!.copy(editedTagColor = color))
    }

    fun updateEditTagName(name: String) {
        if (name != inputStateData.value!!.editTagName) {
            updateInputState(inputStateData.value!!.copy(editTagName = name))
        }
    }

    fun clickSave() {
        if (checkInputs()) saveTag()
    }

    private fun checkInputs(): Boolean {
        updateInputState(inputStateData.value!!.copy(editTagName = inputStateData.value!!.editTagName.trim()))
        if (inputStateData.value!!.editTagName.isEmpty()) {
            navigationEventData.value = NavigationEvent(inputError = InputError.EMPTY_NAME)
            return false
        }

        // when creating new tag, it must have unique title
        val titleAlreadyExists = settingsRepository.getAllTags()
                .any { it.text == inputStateData.value!!.editTagName }
        if (!isEditMode() && titleAlreadyExists) {
            navigationEventData.value = NavigationEvent(inputError = InputError.ALREADY_EXISTS)
            return false
        }
        return true
    }

    private fun isEditMode() = inputStateData.value!!.editOldTagName.isNotEmpty()

    private fun saveTag() {
        val tag = with(inputStateData.value!!) {
            Tag(editTagName, editedTagColor)
        }
        // the old tag is removed first
        val oldTagName = if (isEditMode()) inputStateData.value!!.editOldTagName else tag.text
        settingsRepository.removeTag(oldTagName)
        settingsRepository.addTag(tag)
        closeViewTag()
    }

    fun closeViewTag() {
        updateInputState(inputStateData.value!!.copy(editingTag = false, editOldTagName = "",
                editTagName = ""))
    }

    fun removeTag(title: String) {
        settingsRepository.removeTag(title)
    }

    fun clickDone() {
        navigationEventData.value = NavigationEvent(
                returnData = true,
                bundledSnapshot = snapshotWithInitialTags?.copy(
                        tags = ArrayList(inputStateData.value!!.selectedTags)
                ))
    }


    // floating button user actions

    fun clickCreateTag() {
        updateInputState(inputStateData.value!!.copy(editingTag = true, editOldTagName = "",
                editTagName = "", editedTagColor = "#FFFFFF")) // todo const
    }


    // menus user action

    fun pressBackButton() {
        navigationEventData.value = NavigationEvent(goBack = true)
    }

}
