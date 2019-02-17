/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.tags

import android.content.Context
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.settings.SimplePersistableSettingsRepository
import com.alexvt.integrity.lib.Tag

/**
 * Stores tags in app settings.
 */
object SettingsTagRepository : TagRepository {

    /**
     * Prepares database for use
     */
    override fun init(context: Context) {}

    override fun addTag(tag: Tag) {
        val settings = SimplePersistableSettingsRepository.get()
        settings.dataTags.add(tag)
        SimplePersistableSettingsRepository.set(IntegrityCore.context, settings)
    }

    override fun removeTag(name: String) {
        val settings = SimplePersistableSettingsRepository.get()
        settings.dataTags.removeIf { it.text == name }
        SimplePersistableSettingsRepository.set(IntegrityCore.context, settings)
    }

    override fun getAllTags() = SimplePersistableSettingsRepository.get().dataTags.reversed() // newest first

    override fun clear() {
        val settings = SimplePersistableSettingsRepository.get()
        settings.dataTags.clear()
        SimplePersistableSettingsRepository.set(IntegrityCore.context, settings)
    }
}