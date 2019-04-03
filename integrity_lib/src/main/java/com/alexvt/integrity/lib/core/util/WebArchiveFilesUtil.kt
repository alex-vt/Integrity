/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.util

import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.alexvt.integrity.lib.core.operations.log.Logger

class WebArchiveFilesUtil(
        private val dataFolderManager: DataFolderManager,
        private val logger: Logger
) {

    fun savePageLinkToIndex(pageLink: String, snapshotPath: String, pageIndex: Int) {
        logger.v("savePageLinkToIndex: $pageLink")
        dataFolderManager.addTextToFile(getLocalHtmlLink(pageLink, pageIndex, 0),
                getPaginationPath(snapshotPath))
    }

    fun saveLinkToIndex(link: String, snapshotPath: String, pageIndex: Int, linkIndex: Int) {
        logger.v("saveLinkToIndex: $link")
        dataFolderManager.addTextToFile(getLocalHtmlLink(link, pageIndex, linkIndex),
                getLinksPath(snapshotPath))
    }

    private fun getLocalHtmlLink(link: String, pageIndex: Int, linkIndex: Int)
            = "<h1><a href=\"${getArchivePath(pageIndex, linkIndex)}\">\n$link\n</a></h1>\n<br/>"

    /**
     * Web archive is downloaded when its corresponding link exists in index.
     */
    fun webArchiveAlreadyDownloaded(link: String, snapshotPath: String)
            = LinkUtil.getLinkTexts(dataFolderManager.readTextFromFile(getLinksPath(snapshotPath)))
            .contains(link)

    fun getArchivePath(pageIndex: Int, linkIndex: Int)
            = "page${pageIndex}_link$linkIndex.mht"

    /**
     * Gets map of links for already persisted pagination-to-web-archives index
     * to the corresponding archive links.
     */
    fun getPageIndexLinkToArchivePathMap(snapshotPath: String, archivePathPrefix: String)
            = LinkUtil.getLinkTexts(dataFolderManager.readTextFromFile(getLinksPath(snapshotPath)))
            .zip(LinkUtil.getLinks(dataFolderManager.readTextFromFile(getLinksPath(snapshotPath)))
                    .map { archivePathPrefix + it })
            .toMap()

    /**
     * Gets web archive links for already persisted pagination-to-web-archives index.
     */
    fun getPageIndexArchiveLinks(snapshotPath: String)
            = LinkUtil.getLinks(dataFolderManager.readTextFromFile(getPaginationPath(snapshotPath)))

    /**
     * Gets page links for already persisted pagination-to-web-archives index.
     */
    fun getPageIndexLinks(snapshotPath: String)
            = LinkUtil.getLinkTexts(dataFolderManager.readTextFromFile(getPaginationPath(snapshotPath)))

    private fun getPaginationPath(snapshotPath: String) = "$snapshotPath/index-pages.html"

    private fun getLinksPath(snapshotPath: String) = "$snapshotPath/index.html"


}