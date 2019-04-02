/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.operations.types.blog

interface WebPageLoader {

    /**
     * Async requests web page to be loaded.
     * Invokes the listener with the web page HTML when loaded.
     */
    fun loadHtml(startUrl: String, urlRedirectMap: Map<String, String>, loadImages: Boolean,
                 desktopSite: Boolean, pageLoadListener: (String) -> Unit)

    /**
     * Loads web page in any thread.
     * Returns the web page HTML when loaded.
     */
    fun getHtml(url: String, loadImages: Boolean, desktopSite: Boolean, archiveSavePath: String?,
                screenshotSavePath: String?, delayMillis: Long): String

}