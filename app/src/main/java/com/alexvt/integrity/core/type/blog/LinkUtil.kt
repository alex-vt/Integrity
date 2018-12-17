/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import org.jsoup.Jsoup
import java.net.URL

object LinkUtil {

    fun ccsSelectLinksInSameDomain(html: String, cssSelector: String,
                                   pageUrl: String): Map<String, String>
            = Jsoup.parse(html).select(cssSelector.trim() + " a[href]")
            // dropping duplicates before putting to map to preserve first
            .distinctBy { it.attr("abs:href").trimEnd('/') }
            .associate { it.attr("abs:href").trimEnd('/') to it.cssSelector() }
            .filter { it.key.isNotEmpty() }
            .filter { it.key.contains(getDomainName(pageUrl)) }
            .filterNot { it.key == pageUrl.trimEnd('/') }

    fun getMatchedLinks(html: String, partOfLink: String): Set<String>
            = Jsoup.parse(html).select("a[href]")
            .map { it.attr("abs:href").trimEnd('/') }
            .filter { it.contains(partOfLink) }
            .toSet()

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

    private fun getDomainName(url: String) = if (URL(url).host.startsWith("www.")) {
        URL(url).host.replaceFirst("www.", "")
    } else {
        URL(url).host
    }
}