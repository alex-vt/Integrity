/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import android.content.Context
import com.alexvt.integrity.core.EmptyLocationCredentials
import com.alexvt.integrity.core.FolderLocation
import com.alexvt.integrity.core.FolderLocationCredentials
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil

/**
 * Stores folder locations and credentials simply in Java objects
 * and persists them to Android SharedPreferences as JSON string.
 * todo secure credentials
 */
object SimplePersistableFolderLocationRepository : FolderLocationRepository {

    /**
     * A collection of folder locations and credentials
     */
    private data class FolderLocations(
            val folderLocationSet: LinkedHashSet<FolderLocation> = linkedSetOf(),
            val folderLocationCredentialsSet: LinkedHashSet<FolderLocationCredentials>
            = linkedSetOf()
    )

    private lateinit var folderLocations: FolderLocations


    /**
     * Prepares database for use
     */
    override fun init(context: Context) {
        val folderLocations = PreferencesUtil.getPresetsJson(IntegrityCore.context)
        this.folderLocations = JsonSerializerUtil
                .fromJson(folderLocations, FolderLocations::class.java) ?: FolderLocations()
    }

    override fun addFolderLocation(folderLocation: FolderLocation): String {
        folderLocations.folderLocationSet.add(folderLocation)
        persistAll()
        return folderLocation.title
    }

    override fun addFolderLocationCredentials(folderLocationCredentials: FolderLocationCredentials) {
        folderLocations.folderLocationCredentialsSet.add(folderLocationCredentials)
        persistAll()
    }

    override fun getAllFolderLocations(): List<FolderLocation> {
        return ArrayList(folderLocations.folderLocationSet)
    }

    override fun getCredentials(folderLocation: FolderLocation): FolderLocationCredentials
            = folderLocations.folderLocationCredentialsSet
            .firstOrNull { it.title == folderLocation.title }
            ?: EmptyLocationCredentials()

    override fun clear() {
        folderLocations.folderLocationSet.clear()
        folderLocations.folderLocationCredentialsSet.clear()
        persistAll()
    }

    /**
     * Persists presets to JSON in SharedPreferences.
     *
     * Should be called after every presets modification.
     */
    @Synchronized private fun persistAll() {
        val presetsJson = JsonSerializerUtil.toJson(folderLocations)!!
        PreferencesUtil.setPresetsJson(IntegrityCore.context, presetsJson)
    }
}