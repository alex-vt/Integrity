/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import java.net.URL
import java.util.regex.Pattern

object LinkUtil {

    fun ccsSelectLinks(html: String, cssSelector: String, partOfLink: String,
                       pageUrl: String): Map<String, String>
            = Jsoup.parse(html).select(cssSelector.trim() + " a[href]")
            // dropping duplicates before putting to map to preserve first
            .distinctBy { it.attr("href").trimEnd('/') }
            .associate { getFullUrl(it.attr("href"), pageUrl) to it.cssSelector() }
            .filter { it.key.isNotEmpty() }
            .filter { it.key.contains(partOfLink) }

    fun getMatchedLinks(html: String, partOfLink: String): Set<String>
            = Jsoup.parse(html).select("a[href]")
            .map { it.attr("href").trimEnd('/') }
            .filter { it.contains(partOfLink) }
            .toSet()

    fun getVisibleTextWithLinks(html: String) = Jsoup.clean(html, "", whitelistWithLinks,
            Document.OutputSettings().outline(true))!!
            .replace("<a href=\"", "") // todo extract link as text in a better way
            .replace("<a>", "")
            .replace("</a>", "")
            .replace("<img src=\"", "")
            .replace("<img>", "")
            .replace("</img>", "")
            .replace("\">", "\n")

    private val whitelistWithLinks = Whitelist()
            .addTags("img")
            .addAttributes("img", "src")
            .addProtocols("img", "src", "http", "https")
            .addTags("a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "ftp", "ftps", "http", "https", "mailto")

    val linkRegexString = "(?<=" + Pattern.quote("href=\"") + ")" +
            "[^\"]+" +
            "(?=" + Pattern.quote("\"") + ")"

    fun getLinks(html: String) = linkRegexString.toRegex().findAll(html)
            .map { it.value.trim() }
            .toSet()

    val linkTextRegexString = "(?<=" + Pattern.quote("\">") + ")" +
            "[^\"]+" +
            "(?=" + Pattern.quote("</a>") + ")"

    fun getLinkTexts(html: String) = linkTextRegexString.toRegex().findAll(html)
            .map { it.value.trim() }
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

    private fun getFullUrl(url: String, baseUrl: String) =
            if (url.startsWith("/")) {
                URL(baseUrl).protocol + "://" + URL(baseUrl).host + url.trimEnd('/')
            } else {
                url.trimEnd('/')
            }
}