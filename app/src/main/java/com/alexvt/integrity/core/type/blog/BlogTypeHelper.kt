/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import com.alexvt.integrity.core.util.DataCacheFolderUtil

internal object BlogTypeHelper {

    fun getAllKnownPaginationLinks(blogMetadata: BlogTypeMetadata)
            = if (blogMetadata.paginationUsed) {
        IntProgression.fromClosedRange(
                blogMetadata.pagination.startIndex,
                blogMetadata.pagination.limit,
                blogMetadata.pagination.step
        ).map { blogMetadata.url + blogMetadata.pagination.path + it }
    } else {
        listOf(blogMetadata.url)
    }

    fun getPreviousPaginationLinks(snapshotPath: String)
            = getLinksFromFile(getPaginationPath(snapshotPath))

    fun getNextPaginationLinks(blogMetadata: BlogTypeMetadata, snapshotPath: String)
            = getAllKnownPaginationLinks(blogMetadata)
            .minus(getPreviousPaginationLinks(snapshotPath))

    fun savePaginationLink(paginationLink: String, snapshotPath: String) {
        addTextsToFile(listOf(paginationLink), getPaginationPath(snapshotPath))
    }

    fun saveRelatedPageLinks(pageRelatedLinks: Collection<String>, snapshotPath: String) {
        addTextsToFile(pageRelatedLinks, getRelatedLinksPath(snapshotPath))
    }


    // file operations

    private fun getPaginationPath(snapshotPath: String) = "$snapshotPath/pagination.txt"

    private fun getRelatedLinksPath(snapshotPath: String) = "$snapshotPath/links.txt"

    private fun addTextsToFile(texts: Collection<String>, fullPath: String)
            = DataCacheFolderUtil.addTextToFile(texts.joinToString("\n"), fullPath)

    private fun getLinksFromFile(fullPath: String)
            = DataCacheFolderUtil.readTextFromFile(fullPath)
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

}