/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.util

import android.content.Context
import android.util.Log
import com.alexvt.integrity.core.util.DataCacheFolderUtil

object WebArchiveFilesUtil {

    fun savePageLinkToIndex(context: Context, pageLink: String, snapshotPath: String, pageIndex: Int) {
        Log.d("WebArchiveFilesUtil", "savePageLinkToIndex: $pageLink")
        DataCacheFolderUtil.addTextToFile(context, getLocalHtmlLink(pageLink, pageIndex, 0),
                getPaginationPath(snapshotPath))
    }

    fun saveLinkToIndex(context: Context, link: String, snapshotPath: String, pageIndex: Int, linkIndex: Int) {
        Log.d("WebArchiveFilesUtil", "saveLinkToIndex: $link")
        DataCacheFolderUtil.addTextToFile(context, getLocalHtmlLink(link, pageIndex, linkIndex),
                getLinksPath(snapshotPath))
    }

    fun getLocalHtmlLink(link: String, pageIndex: Int, linkIndex: Int)
            = "<h1><a href=\"${getArchivePath(pageIndex, linkIndex)}\">\n$link\n</a></h1>\n<br/>"

    /**
     * Web archive is downloaded when its corresponding link exists in index.
     */
    fun webArchiveAlreadyDownloaded(context: Context, link: String, snapshotPath: String)
            = LinkUtil.getLinkTexts(DataCacheFolderUtil.readTextFromFile(context, getLinksPath(snapshotPath)))
            .contains(link)

    fun getArchivePath(pageIndex: Int, linkIndex: Int)
            = "page${pageIndex}_link$linkIndex.mht"

    /**
     * Gets map of links for already persisted pagination-to-web-archives index
     * to the corresponding archive links.
     */
    fun getPageIndexLinkToArchivePathMap(context: Context, snapshotPath: String, archivePathPrefix: String)
            = LinkUtil.getLinkTexts(DataCacheFolderUtil.readTextFromFile(context, getLinksPath(snapshotPath)))
            .zip(LinkUtil.getLinks(DataCacheFolderUtil.readTextFromFile(context, getLinksPath(snapshotPath)))
                    .map { archivePathPrefix + it })
            .toMap()

    /**
     * Gets web archive links for already persisted pagination-to-web-archives index.
     */
    fun getPageIndexArchiveLinks(context: Context, snapshotPath: String)
            = LinkUtil.getLinks(DataCacheFolderUtil.readTextFromFile(context, getPaginationPath(snapshotPath)))

    /**
     * Gets page links for already persisted pagination-to-web-archives index.
     */
    fun getPageIndexLinks(context: Context, snapshotPath: String)
            = LinkUtil.getLinkTexts(DataCacheFolderUtil.readTextFromFile(context, getPaginationPath(snapshotPath)))

    fun getPaginationPath(snapshotPath: String) = "$snapshotPath/index-pages.html"

    fun getLinksPath(snapshotPath: String) = "$snapshotPath/index.html"


}