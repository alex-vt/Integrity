/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.data.settings

import com.alexvt.integrity.lib.core.data.metadata.FolderLocation
import com.alexvt.integrity.lib.core.data.metadata.Tag
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
        val snapshotListViewMode: String = ListViewMode.CARDS,
        val jobsEnableScheduled: Boolean = true,
        val jobsExpandRunning: Boolean = true,
        val jobsExpandScheduled: Boolean = true,
        val sortingMethod: String = SortingMethod.NEW_FIRST,
        val fasterSearchInputs: Boolean = true,
        val dataFolderPath: String = "Integrity",
        val dataTags: List<Tag> = emptyList(),
        val dataFolderLocations: List<FolderLocation> = emptyList(),
        val notificationShowErrors: Boolean = true,
        val notificationShowDisabledScheduled: Boolean = true
) : Serializable

object ListViewMode {
    const val LIST = "list"
    const val CARDS = "cards"
    const val BIG_CARDS = "big_cards"
}

/**
 * Sorting method setting can be changed directly, or in dual controls. In the latter case they are:
 * by time (ascending/descending), by name (ascending/descending), random (shuffle again).
 */
object SortingMethod {
    const val NEW_FIRST = "new_first"
    const val OLD_FIRST = "old_first"
    const val A_Z = "a_z"
    const val Z_A = "z_a"
    const val RANDOM = "random"
}