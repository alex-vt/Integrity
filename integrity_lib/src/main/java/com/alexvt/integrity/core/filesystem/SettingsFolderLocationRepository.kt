/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.filesystem

import android.content.Context
import com.alexvt.integrity.lib.EmptyLocationCredentials
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.FolderLocationCredentials
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.settings.SimplePersistableSettingsRepository
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.core.util.PreferencesUtil

/**
 * Stores folder locations in app settings.
 *
 * Stores folder credentials simply in Java objects
 * and persists them to Android SharedPreferences as JSON string.
 * todo secure credentials
 */
object SettingsFolderLocationRepository : FolderLocationRepository {

    /**
     * A collection of folder locations and credentials
     */
    private data class Credentials(
            val folderLocationCredentialsSet: LinkedHashSet<FolderLocationCredentials>
            = linkedSetOf()
    )

    private lateinit var credentials: Credentials


    /**
     * Prepares database for use
     */
    override fun init(context: Context) {
        val folderLocationsJson = PreferencesUtil.getFolderLocationsJson(context)
        if (folderLocationsJson != null) {
            credentials = JsonSerializerUtil.fromJson(folderLocationsJson, Credentials::class.java)
        }
        if (!::credentials.isInitialized) {
            credentials = Credentials()
        }
    }

    override fun addFolderLocation(folderLocation: FolderLocation): String {
        val settings = SimplePersistableSettingsRepository.get()
        settings.dataFolderLocations.add(folderLocation)
        SimplePersistableSettingsRepository.set(IntegrityCore.context, settings)

        return folderLocation.title
    }

    override fun addFolderLocationCredentials(folderLocationCredentials: FolderLocationCredentials) {
        credentials.folderLocationCredentialsSet.add(folderLocationCredentials)
        persistCredentials()
    }

    override fun getAllFolderLocations(): List<FolderLocation> {
        return SimplePersistableSettingsRepository.get().dataFolderLocations
    }

    override fun getCredentials(folderLocation: FolderLocation): FolderLocationCredentials
            = credentials.folderLocationCredentialsSet
            .firstOrNull { it.title == folderLocation.title }
            ?: EmptyLocationCredentials()

    override fun removeFolderLocationAndCredentials(title: String) {
        val settings = SimplePersistableSettingsRepository.get()
        settings.dataFolderLocations.removeIf { it.title == title }
        SimplePersistableSettingsRepository.set(IntegrityCore.context, settings)

        credentials.folderLocationCredentialsSet.removeIf { it.title == title }
        persistCredentials()
    }

    override fun clear() {
        val settings = SimplePersistableSettingsRepository.get()
        settings.dataFolderLocations.clear()
        SimplePersistableSettingsRepository.set(IntegrityCore.context, settings)

        credentials.folderLocationCredentialsSet.clear()
        persistCredentials()
    }

    /**
     * Persists presets to JSON in SharedPreferences.
     *
     * Should be called after every presets modification.
     */
    @Synchronized private fun persistCredentials() {
        val presetsJson = JsonSerializerUtil.toJson(credentials)
        PreferencesUtil.setFolderLocationsJson(IntegrityCore.context, presetsJson)
    }
}