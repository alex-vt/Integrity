/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import android.content.Context
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil

/**
 * Stores presets simply in Java objects and persists them to Android SharedPreferences
 * as JSON string.
 */
object SimplePersistablePresetRepository : PresetRepository {

    /**
     * A collection of presets: folder locations
     */
    private data class PresetCollection(val folderLocationSet: LinkedHashSet<FolderLocation>) {
        constructor() : this(linkedSetOf())
    }

    private lateinit var allPresets: PresetCollection


    /**
     * Prepares database for use
     */
    override fun init(context: Context) {
        val presetsJson = PreferencesUtil.getPresetsJson(IntegrityCore.context)
        val presetsFromJson = JsonSerializerUtil.fromJson(presetsJson, PresetCollection::class.java)
        if (presetsFromJson != null) {
            allPresets = presetsFromJson
        } else {
            allPresets = PresetCollection(linkedSetOf())
        }
    }

    /**
     * Adds a unique folder location.
     */
    override fun addFolderLocation(folderLocation: FolderLocation) {
        allPresets.folderLocationSet.add(folderLocation)
        persistAll()
    }

    /**
     * Gets all unique folder location
     */
    override fun getAllFolderLocations(): List<FolderLocation> {
        return ArrayList(allPresets.folderLocationSet)
    }

    /**
     * Deletes all folder locations from database
     */
    override fun clear() {
        allPresets.folderLocationSet.clear()
        persistAll()
    }

    /**
     * Persists presets to JSON in SharedPreferences.
     *
     * Should be called after every presets modification.
     */
    @Synchronized private fun persistAll() {
        val presetsJson = JsonSerializerUtil.toJson(allPresets)!!
        PreferencesUtil.setPresetsJson(IntegrityCore.context, presetsJson)
    }
}