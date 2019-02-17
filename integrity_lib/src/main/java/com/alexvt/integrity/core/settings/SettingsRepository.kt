/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.settings

import android.content.Context

/**
 * Manager of repository of app settings (singleton).
 */
interface SettingsRepository {

    /**
     * Prepares database for use.
     */
    fun init(context: Context)

    /**
     * Setter.
     */
    fun set(context: Context, integrityAppSettings: IntegrityAppSettings)

    /**
     * Getter.
     */
    fun get(): IntegrityAppSettings

    /**
     * Rewrites settings with the default ones.
     */
    fun resetToDefault(context: Context)
}