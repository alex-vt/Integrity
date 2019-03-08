/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

import com.alexvt.integrity.core.settings.SortingMethod
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.search.SearchResult

object SortingUtil { // for UI

    private enum class SortingType { DATE, TITLE, RANDOM }
    private enum class SortingDirection { ASCENDING, DESCENDING, RANDOM }

    fun getSortingTypeNames()
            = listOf("By date", "By title", "Random")

    fun getSortingMethodNameMap() = linkedMapOf(
            SortingMethod.NEW_FIRST to "By date ↓",
            SortingMethod.OLD_FIRST to "By date ↑",
            SortingMethod.Z_A to "By title ↓",
            SortingMethod.A_Z to "By title ↑",
            SortingMethod.RANDOM to "Random"
    )

    fun getSortingTypeNameIndex(sortingMethod: String) = enumValues<SortingType>()
            .indexOf(getSortingType(sortingMethod))


    fun isAscending(sortingMethod: String)
            = getSortingDirection(sortingMethod) == SortingDirection.ASCENDING

    fun isDescending(sortingMethod: String)
            = getSortingDirection(sortingMethod) == SortingDirection.DESCENDING

    fun isByDate(sortingMethod: String) = getSortingType(sortingMethod) == SortingType.DATE

    fun isByTitle(sortingMethod: String) = getSortingType(sortingMethod) == SortingType.TITLE


    fun changeSortingType(sortingMethod: String, sortingTypeIndex: Int) = synthesizeSortingMethod(
            sortingType = enumValues<SortingType>()[sortingTypeIndex] // default direction
    )

    fun revertSortingDirection(sortingMethod: String) = synthesizeSortingMethod(
            sortingType = getSortingType(sortingMethod),
            sortingDirection = getOppositeDirection(getSortingDirection(sortingMethod))
    )



    fun sortSnapshots(snapshots: List<Snapshot>,
                      sortingMethod: String) = with (snapshots) {
        if (isByDate(sortingMethod)) {
            if (isAscending(sortingMethod)) {
                sortedBy { it.date }
            } else {
                sortedByDescending { it.date }
            }
        } else if (isByTitle(sortingMethod)) {
            if (isAscending(sortingMethod)) {
                sortedBy { it.title }
            } else {
                sortedByDescending { it.title }
            }
        } else {
            shuffled()
        }
    }

    fun sortSearchResults(searchResults: List<SearchResult>,
                          sortingMethod: String) = with (searchResults) {
        if (isByDate(sortingMethod)) {
            if (isAscending(sortingMethod)) {
                sortedBy { it.date }
            } else {
                sortedByDescending { it.date }
            }
        } else if (isByTitle(sortingMethod)) {
            if (isAscending(sortingMethod)) {
                sortedBy { it.snapshotTitle }
            } else {
                sortedByDescending { it.snapshotTitle }
            }
        } else {
            shuffled()
        }
    }



    private fun getSortingType(sortingMethod: String) = when (sortingMethod) {
        SortingMethod.NEW_FIRST, SortingMethod.OLD_FIRST -> SortingType.DATE
        SortingMethod.A_Z, SortingMethod.Z_A -> SortingType.TITLE
        else -> SortingType.RANDOM
    }

    private fun getSortingDirection(sortingMethod: String) = when (sortingMethod) {
        SortingMethod.NEW_FIRST, SortingMethod.Z_A -> SortingDirection.DESCENDING
        SortingMethod.OLD_FIRST, SortingMethod.A_Z -> SortingDirection.ASCENDING
        else -> SortingDirection.RANDOM
    }

    private fun getOppositeDirection(sortingDirection: SortingDirection) = when (sortingDirection) {
        SortingDirection.DESCENDING -> SortingDirection.ASCENDING
        SortingDirection.ASCENDING -> SortingDirection.DESCENDING
        else -> SortingDirection.RANDOM
    }

    private fun synthesizeSortingMethod(sortingType: SortingType,
                                        sortingDirection: SortingDirection? = null) = when (sortingType) {
        SortingType.DATE -> if (sortingDirection == SortingDirection.ASCENDING) {
            SortingMethod.OLD_FIRST
        } else {
            SortingMethod.NEW_FIRST
        }
        SortingType.TITLE -> if (sortingDirection == SortingDirection.DESCENDING) {
            SortingMethod.Z_A
        } else {
            SortingMethod.A_Z
        }
        else -> SortingMethod.RANDOM
    }
}