/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.jsoup.select.Selector

object LinkUtil {

    fun getCssSelectedLinkMap(html: String, cssSelector: String,
                              currentPageUrl: String): Map<String, String>
            = Jsoup.parse(html).select(cssSelector.trim() + " a[href]")
            .distinctBy { it.attr("abs:href") } // dropping duplicates before putting to map to preserve first
            .associate { it.attr("abs:href") to it.cssSelector() }
            .filter { it.key.isNotEmpty() }
            .filter { it.key.startsWith(currentPageUrl) }
            .filter { it.key != currentPageUrl }
            .filter { !it.key.contains("#") }

    fun getShortFormUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url.replaceFirst("http://", "").replaceFirst("https://", "")
        }
        return url
    }

    fun getFullFormUrl(url: String): String {
        return if (!url.startsWith("https://") && !url.startsWith("http://")) {
            "http://" + url
        } else {
            url
        }
    }

}