/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.search

import com.alexvt.integrity.core.data.settings.SortingMethod
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.search.SnapshotSearchResult
import com.alexvt.integrity.lib.core.data.search.TextSearchResult
import java.lang.Exception

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
            = getSortingDirection(sortingMethod) == SortingUtil.SortingDirection.ASCENDING

    fun isDescending(sortingMethod: String)
            = getSortingDirection(sortingMethod) == SortingUtil.SortingDirection.DESCENDING

    fun isByDate(sortingMethod: String) = getSortingType(sortingMethod) == SortingUtil.SortingType.DATE

    fun isByTitle(sortingMethod: String) = getSortingType(sortingMethod) == SortingUtil.SortingType.TITLE


    fun changeSortingType(sortingMethod: String, sortingTypeIndex: Int) = synthesizeSortingMethod(
            sortingType = enumValues<SortingType>()[sortingTypeIndex] // default direction
    )

    fun revertSortingDirection(sortingMethod: String) = synthesizeSortingMethod(
            sortingType = getSortingType(sortingMethod),
            sortingDirection = getOppositeDirection(getSortingDirection(sortingMethod))
    )

    // todo sortability compile time verification
    private fun getDate(sortable: Any) = when (sortable) {
        is Snapshot -> sortable.date
        is TextSearchResult -> sortable.date
        is SnapshotSearchResult -> sortable.snapshot.date
        else -> throw Exception("Item $sortable not sortable by date")
    }

    private fun getTitle(sortable: Any) = when (sortable) {
        is Snapshot -> sortable.title
        is TextSearchResult -> sortable.snapshotTitle
        is SnapshotSearchResult -> sortable.snapshot.title
        else -> throw Exception("Item $sortable not sortable by title")
    }

    fun <T: Any> sort(sortableItems: List<T>,
                      sortingMethod: String): List<T> = with (sortableItems) {
        if (isByDate(sortingMethod)) {
            if (isAscending(sortingMethod)) {
                sortedBy { getDate(it) }
            } else {
                sortedByDescending { getDate(it) }
            }
        } else if (isByTitle(sortingMethod)) {
            if (isAscending(sortingMethod)) {
                sortedBy { getTitle(it) }
            } else {
                sortedByDescending { getTitle(it) }
            }
        } else {
            shuffled()
        }
    }

    private fun getSortingType(sortingMethod: String) = when (sortingMethod) {
        SortingMethod.NEW_FIRST, SortingMethod.OLD_FIRST -> SortingUtil.SortingType.DATE
        SortingMethod.A_Z, SortingMethod.Z_A -> SortingUtil.SortingType.TITLE
        else -> SortingUtil.SortingType.RANDOM
    }

    private fun getSortingDirection(sortingMethod: String) = when (sortingMethod) {
        SortingMethod.NEW_FIRST, SortingMethod.Z_A -> SortingUtil.SortingDirection.DESCENDING
        SortingMethod.OLD_FIRST, SortingMethod.A_Z -> SortingUtil.SortingDirection.ASCENDING
        else -> SortingUtil.SortingDirection.RANDOM
    }

    private fun getOppositeDirection(sortingDirection: SortingDirection) = when (sortingDirection) {
        SortingUtil.SortingDirection.DESCENDING -> SortingUtil.SortingDirection.ASCENDING
        SortingUtil.SortingDirection.ASCENDING -> SortingUtil.SortingDirection.DESCENDING
        else -> SortingUtil.SortingDirection.RANDOM
    }

    private fun synthesizeSortingMethod(sortingType: SortingType,
                                        sortingDirection: SortingDirection? = null) = when (sortingType) {
        SortingUtil.SortingType.DATE -> if (sortingDirection == SortingUtil.SortingDirection.ASCENDING) {
            SortingMethod.OLD_FIRST
        } else {
            SortingMethod.NEW_FIRST
        }
        SortingUtil.SortingType.TITLE -> if (sortingDirection == SortingUtil.SortingDirection.DESCENDING) {
            SortingMethod.Z_A
        } else {
            SortingMethod.A_Z
        }
        else -> SortingMethod.RANDOM
    }
}