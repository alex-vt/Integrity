/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.credentials

import android.content.Context
import com.alexvt.integrity.lib.EmptyCredentials
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.lib.Credentials

/**
 * Stores credentials simply in Java objects
 * and persists them to Android SharedPreferences as JSON string.
 * todo secure credentials
 */
object SimplePersistableCredentialsRepository : CredentialsRepository {

    /**
     * A collection of folder locations and credentials
     */
    private data class CredentialsSet(
            val items: LinkedHashSet<Credentials>
            = linkedSetOf()
    )

    private lateinit var credentialsSet: CredentialsSet


    /**
     * Prepares database for use
     */
    override fun init(context: Context, clear: Boolean) {
        if (!clear) {
            val credentialsSetJson = readJsonFromStorage(context)
            if (credentialsSetJson != null) {
                credentialsSet = JsonSerializerUtil.fromJson(credentialsSetJson, CredentialsSet::class.java)
            }
        }
        if (clear || !SimplePersistableCredentialsRepository::credentialsSet.isInitialized) {
            credentialsSet = CredentialsSet()
            persistCredentials(context)
        }
    }

    override fun addCredentials(context: Context, credentials: Credentials) {
        credentialsSet.items.add(credentials)
        persistCredentials(context)
    }

    override fun getCredentials(title: String): Credentials
            = credentialsSet.items
            .firstOrNull { it.title == title }
            ?: EmptyCredentials()

    override fun removeCredentials(context: Context, title: String) {
        credentialsSet.items.removeIf { it.title == title }
        persistCredentials(context)
    }

    override fun clear(context: Context) {
        credentialsSet.items.clear()
        persistCredentials(context)
    }

    /**
     * Persists presets to JSON in SharedPreferences.
     *
     * Should be called after every presets modification.
     */
    @Synchronized private fun persistCredentials(context: Context) {
        val presetsJson = JsonSerializerUtil.toJson(credentialsSet)
        persistJsonToStorage(context, presetsJson)
    }


    // Storage for the JSON string in SharedPreferences

    private const val TAG = "credentials"

    private const val preferencesName = "persisted_$TAG"
    private const val preferenceKey = "${TAG}_json"

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}