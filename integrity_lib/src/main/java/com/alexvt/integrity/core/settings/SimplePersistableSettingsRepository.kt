/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.settings

import android.content.Context
import com.alexvt.integrity.core.filesystem.local.LocalFolderLocation
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.lib.Tag

/**
 * In-memory settings storage persisting to SharedPreferences.
 */
object SimplePersistableSettingsRepository : SettingsRepository {

    private lateinit var integrityAppSettings: IntegrityAppSettings

    private const val PREFERENCES_NAME = "IntegrityAppSettings"
    private const val PREFERENCE_KEY = "IntegrityAppSettings"

    private fun getSharedPreferences(context: Context)
            = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)


    /**
     * Prepares database for use.
     */
    override fun init(context: Context) {
        if (::integrityAppSettings.isInitialized) {
            return
        }
        val logJson = getSharedPreferences(context).getString(PREFERENCE_KEY, null)
        if (logJson != null) {
            integrityAppSettings = JsonSerializerUtil.fromJson(logJson, IntegrityAppSettings::class.java)
        }
        if (!::integrityAppSettings.isInitialized) {
            integrityAppSettings = getDefaultSettings()
            saveChanges(context)
        }
    }

    override fun set(context: Context, integrityAppSettings: IntegrityAppSettings) {
        if (!this::integrityAppSettings.isInitialized) init(context)
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

    /**
     * Persists settings to JSON in SharedPreferences.
     *
     * Should be called after every metadata modification.
     */
    @Synchronized private fun saveChanges(context: Context) {
        val settingsJson = JsonSerializerUtil.toJson(integrityAppSettings)
        getSharedPreferences(context).edit().putString(PREFERENCE_KEY, settingsJson).commit() // todo listener
    }
}