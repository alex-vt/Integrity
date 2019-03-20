/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.settings

import android.content.Context
import com.alexvt.integrity.lib.destinations.local.LocalFolderLocation
import com.alexvt.integrity.lib.util.JsonSerializerUtil
import com.alexvt.integrity.lib.metadata.FolderLocation
import com.alexvt.integrity.lib.metadata.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * In-memory settings storage persisting to SharedPreferences.
 */
class SimplePersistableSettingsRepository(private val context: Context) : SettingsRepository {

    private var integrityAppSettings: IntegrityAppSettings

    // Storage name for the JSON string in SharedPreferences
    private val TAG = "app_settings"
    private val preferencesName = "persisted_$TAG"
    private val preferenceKey = "${TAG}_json"

    init {
        val logJson = readJsonFromStorage(context)
        if (logJson != null) {
            integrityAppSettings = JsonSerializerUtil.fromJson(logJson, IntegrityAppSettings::class.java)
        } else {
            integrityAppSettings = getDefaultSettings()
            saveChanges(context)
        }
    }

    override fun clear() {
        integrityAppSettings = getDefaultSettings()
        saveChanges(context)
    }

    private var changesListenerMap: Map<String, ((IntegrityAppSettings) -> Unit)> = mapOf()

    override fun addChangesListener(tag: String, changesListener: (IntegrityAppSettings) -> Unit) {
        changesListenerMap = changesListenerMap.plus(Pair(tag, changesListener))
        invokeChangesListeners()
    }

    override fun removeChangesListener(tag: String) {
        changesListenerMap = changesListenerMap.minus(tag)
    }

    private fun invokeChangesListeners() {
        GlobalScope.launch (Dispatchers.Main) {
            changesListenerMap.forEach {
                it.value.invoke(integrityAppSettings)
            }
        }
    }

    override fun set(integrityAppSettings: IntegrityAppSettings) {
        this.integrityAppSettings = integrityAppSettings
        saveChanges(context)
    }

    override fun get(): IntegrityAppSettings {
        return integrityAppSettings
    }

    override fun resetToDefault() {
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

    override fun addTag(tag: Tag) {
        val settings = get()
        settings.dataTags.add(tag)
        set(settings)
    }

    override fun removeTag(name: String) {
        val settings = get()
        settings.dataTags.removeIf { it.text == name }
        set(settings)
    }

    override fun getAllTags() = get().dataTags.reversed() // newest first

    override fun clearTags() {
        val settings = get()
        settings.dataTags.clear()
        set(settings)
    }


    override fun addFolderLocation(folderLocation: FolderLocation): String {
        val settings = get()
        settings.dataFolderLocations.add(folderLocation)
        set(settings)

        return folderLocation.title
    }

    override fun getAllFolderLocations(): List<FolderLocation> {
        return get().dataFolderLocations
    }

    override fun removeFolderLocation(title: String) {
        val settings = get()
        settings.dataFolderLocations.removeIf { it.title == title }
        set(settings)
    }

    override fun clearFolderLocations() {
        val settings = get()
        settings.dataFolderLocations.clear()
        set(settings)
    }


    /**
     * Persists settings to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges(context: Context) {
        invokeChangesListeners()
        val settingsJson = JsonSerializerUtil.toJson(integrityAppSettings)
        persistJsonToStorage(context, settingsJson) // todo listener
    }


    // Storage for the JSON string in SharedPreferences

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}