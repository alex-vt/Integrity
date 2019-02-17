/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.settings

import com.alexvt.integrity.lib.FolderLocation
import com.alexvt.integrity.lib.Tag
import java.io.Serializable

/**
 * Setting changeable on the settings screen.
 *
 * Snapshots, logs, credentials and computable data aren't included.
 */
data class IntegrityAppSettings(
        val lastChangeDate: String = "",
        val colorBackground: String = "#EEEEEE",
        val colorPrimary: String = "#008577",
        val colorAccent: String = "#EE0077",
        val textFont: String = "",
        val textScalePercent: Float = 100f,
        val menuExpandJobsRunning: Boolean = true,
        val menuExpandJobsScheduled: Boolean = true,
        val dataFolderPath: String = "Integrity",
        val dataTags: ArrayList<Tag> = arrayListOf(),
        val dataFolderLocations: ArrayList<FolderLocation> = arrayListOf(),
        val notificationShowErrors: Boolean = true
) : Serializable
