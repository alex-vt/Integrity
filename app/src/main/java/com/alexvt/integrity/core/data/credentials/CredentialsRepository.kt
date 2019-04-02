/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.data.credentials

import com.alexvt.integrity.core.data.Clearable
import com.alexvt.integrity.lib.core.data.metadata.Credentials
import io.reactivex.Single

/**
 * Manager of repository of credentials
 */
interface CredentialsRepository : Clearable {

    /**
     * Adds credentials which are stored separately.
     */
    fun addCredentials(credentials: Credentials)

    /**
     * Gets credentials by the provided title. If none, returns empty ones.
     */
    fun getCredentialsBlocking(title: String?): Credentials

    /**
     * Gets credentials by the provided title, async.
     */
    fun getCredentialsSingle(title: String?): Single<Credentials>

    fun removeCredentials(title: String)

    /**
     * Deletes all credentials from database
     */
    override fun clear()
}