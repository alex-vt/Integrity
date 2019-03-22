/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.credentials

import android.content.Context
import com.alexvt.integrity.lib.metadata.EmptyCredentials
import com.alexvt.integrity.lib.util.JsonSerializerUtil
import com.alexvt.integrity.lib.metadata.Credentials
import io.reactivex.Single

/**
 * Stores credentials simply in Java objects
 * and persists them to Android SharedPreferences as JSON string.
 * todo secure credentials
 */
class SimplePersistableCredentialsRepository(
        private val context: Context
) : CredentialsRepository {

    /**
     * A collection of folder locations and credentials
     */
    private data class CredentialsSet(
            val items: LinkedHashSet<Credentials>
            = linkedSetOf()
    )

    private var credentialsSet: CredentialsSet

    // Storage name for the JSON string in SharedPreferences
    private val TAG = "credentials"
    private val preferencesName = "persisted_$TAG"
    private val preferenceKey = "${TAG}_json"

    init {
        val credentialsSetJson = readJsonFromStorage(context)
        if (credentialsSetJson != null) {
            credentialsSet = JsonSerializerUtil.fromJson(credentialsSetJson, CredentialsSet::class.java)
        } else {
            credentialsSet = CredentialsSet()
            persistCredentials(context)
        }
    }

    override fun addCredentials(credentials: Credentials) {
        credentialsSet.items.add(credentials)
        persistCredentials(context)
    }

    override fun getCredentialsBlocking(title: String?): Credentials
            = credentialsSet.items
            .firstOrNull { it.title == title }
            ?: EmptyCredentials()

    override fun getCredentialsSingle(title: String?): Single<Credentials>
            = Single.just(getCredentialsBlocking(title))

    override fun removeCredentials(title: String) {
        credentialsSet.items.removeIf { it.title == title }
        persistCredentials(context)
    }

    override fun clear() {
        credentialsSet.items.clear()
        persistCredentials(context)
    }

    /**
     * Persists credentials to JSON in SharedPreferences.
     *
     * Should be called after every presets modification.
     */
    @Synchronized private fun persistCredentials(context: Context) {
        val presetsJson = JsonSerializerUtil.toJson(credentialsSet)
        persistJsonToStorage(context, presetsJson)
    }


    // Storage for the JSON string in SharedPreferences

    private fun readJsonFromStorage(context: Context)
            = getSharedPreferences(context).getString(preferenceKey, null)

    private fun persistJsonToStorage(context: Context, value: String)
            = getSharedPreferences(context).edit().putString(preferenceKey, value).commit()

    private fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}