/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.settings

import android.content.Context
import com.alexvt.integrity.core.filesystem.local.LocalFolderLocation
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.Tag

/**
 * In-memory settings storage persisting to SharedPreferences.
 */
object SimplePersistableSettingsRepository : SettingsRepository {

    private lateinit var integrityAppSettings: IntegrityAppSettings

    /**
     * Prepares database for use.
     */
    override fun init(context: Context, clear: Boolean) {
        if (!clear) {
            val logJson = readJsonFromStorage(context)
            if (logJson != null) {
                integrityAppSettings = JsonSerializerUtil.fromJson(logJson, IntegrityAppSettings::class.java)
            }
        }
        if (clear || !::integrityAppSettings.isInitialized) {
            integrityAppSettings = getDefaultSettings()
            saveChanges(context)
        }
    }

    override fun set(context: Context, integrityAppSettings: IntegrityAppSettings) {
        this.integrityAppSettings = integrityAppSettings
        saveChanges(context)
    }

    override fun get(): IntegrityAppSettings {
        return integrityAppSettings
    }

    override fun resetToDefault(context: Context) {
        this.integrityAppSettings = getDefaultSettings()
        saveChanges(context)
    }

    private fun getDefaultSettings() = IntegrityAppSettings(
            dataFolderPath = "Integrity",
            dataFolderLocations = arrayListOf(LocalFolderLocation(
                    title = "Integrity archives on device",
                    folderPath = "IntegrityArchives"
            )),
            dataTags = arrayListOf(
                    Tag(text = "Favorite", color = "#FFCC00"),
                    Tag(text = "High value", color = "#33AAFF")
            )
    )

    override fun addTag(context: Context, tag: Tag) {
        val settings = get()
        settings.dataTags.add(tag)
        set(context, settings)
    }

    override fun removeTag(context: Context, name: String) {
        val settings = get()
        settings.dataTags.removeIf { it.text == name }
        set(context, settings)
    }

    override fun getAllTags() = get().dataTags.reversed() // newest first

    override fun clearTags(context: Context) {
        val settings = get()
        settings.dataTags.clear()
        SimplePersistableSettingsRepository.set(context, settings)
    }


    override fun addFolderLocation(context: Context, folderLocation: FolderLocation): String {
        val settings = get()
        settings.dataFolderLocations.add(folderLocation)
        set(context, settings)

        return folderLocation.title
    }

    override fun getAllFolderLocations(): List<FolderLocation> {
        return get().dataFolderLocations
    }

    override fun removeFolderLocation(context: Context, title: String) {
        val settings = get()
        settings.dataFolderLocations.removeIf { it.title == title }
        set(context, settings)
    }

    override fun clearFolderLocations(context: Context) {
        val settings = get()
        settings.dataFolderLocations.clear()
        set(context, settings)
    }


    /**
     * Persists settings to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges(context: Context) {
        val settingsJson = JsonSerializerUtil.toJson(integrityAppSettings)
        persistJsonToStorage(context, settingsJson) // todo listener
    }


    // Storage for the JSON string in SharedPreferences

    private const val TAG = "app_settings"

    private const val preferencesName = "persisted_$TAG"
    private const val preferenceKey = "${TAG}_json"

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}