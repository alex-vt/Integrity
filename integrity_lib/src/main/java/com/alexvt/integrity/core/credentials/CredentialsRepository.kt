/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.credentials

import android.content.Context
import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.Credentials

/**
 * Manager of repository of credentials
 */
interface CredentialsRepository {
    /**
     * Prepares database for use
     */
    fun init(context: Context, clear: Boolean = false)

    /**
     * Adds credentials which are stored separately.
     */
    fun addCredentials(context: Context, credentials: Credentials)

    /**
     * Gets credentials by the provided title. If none, returns empty ones.
     */
    fun getCredentials(title: String): Credentials

    fun removeCredentials(context: Context, title: String)

    /**
     * Deletes all folder locations and credentials from database
     */
    fun clear(context: Context)
}