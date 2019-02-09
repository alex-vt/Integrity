/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Shared preferences util.
 */
object PreferencesUtil {

    private val FULL_METADATA = "FULL_METADATA"

    fun getFullMetadataJson(context: Context)
            = getSharedPreferences(context).getString(FULL_METADATA, null)

    fun setFullMetadataJson(context: Context, value: String) {
        getPreferencesEditor(context).putString(FULL_METADATA, value).commit()
    }


    private val LOG = "LOG"

    fun getLogJson(context: Context)
            = getSharedPreferences(context).getString(LOG, null)

    fun setLogJson(context: Context, value: String) {
        getPreferencesEditor(context).putString(LOG, value).commit()
    }


    private val FOLDER_LOCATIONS = "FOLDER_LOCATIONS"

    fun getFolderLocationsJson(context: Context)
            = getSharedPreferences(context).getString(FOLDER_LOCATIONS, null)

    fun setFolderLocationsJson(context: Context, value: String) {
        getPreferencesEditor(context).putString(FOLDER_LOCATIONS, value).commit()
    }


    private val TAGS = "TAGS"

    fun getTagsJson(context: Context)
            = getSharedPreferences(context).getString(TAGS, null)

    fun setTagsJson(context: Context, value: String) {
        getPreferencesEditor(context).putString(TAGS, value).commit()
    }


    private val DATA_CHUNKS = "DATA_CHUNKS"

    fun getDataChunksJson(context: Context)
            = getSharedPreferences(context).getString(DATA_CHUNKS, null)

    fun setDataChunksJson(context: Context, value: String) {
        getPreferencesEditor(context).putString(DATA_CHUNKS, value).commit()
    }


    private val PREFERENCES_NAME = "SHARED_PREFERENCES"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private fun getPreferencesEditor(context: Context): SharedPreferences.Editor {
        return getSharedPreferences(context).edit()
    }

}