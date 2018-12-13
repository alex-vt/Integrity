/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type.blog

import com.alexvt.integrity.core.TypeMetadata

/**
 * Metadata of blog type: a web page and (optionally) related pages it links to.
 *
 * Related pages are considered the linked ones on the page defined by URL (main page),
 * from the same domain.
 *
 * relatedLinksPattern is a CSS selector string to choose links by from the main page HTML.
 * When it's empty, related pages are not saved.
 */
data class BlogTypeMetadata(
        val url: String = "",
        val loadImages: Boolean = true,
        val desktopSite: Boolean = false,
        val paginationUsed: Boolean = false,
        val pagination: Pagination = IndexedPagination(),
        val relatedPageLinksUsed: Boolean = false,
        val relatedPageLinksPattern: String = "",// CSS selector, or null when saving only this page
        val loadIntervalMillis: Long = 1000
) : TypeMetadata()

/**
 * Pagination scheme describing a set of blog pages.
 */
abstract class Pagination

/**
 * Pagination with page number.
 *
 * For example, for url = http://example.com/,
 * pagination with path = page/, startIndex = 1, step = 1 and limit = 3
 * yields pages http://example.com/page/1, http://example.com/page/2 and http://example.com/page/3.
 */
data class IndexedPagination(
        val path: String = "page/",
        val startIndex: Int = 1,
        val step: Int = 1,
        val limit: Int = 3 // requires attention to prevent missing or excessive pages
): Pagination()

/**
 * Pagination with links to next page. Finds link to next page by pattern url + pathPrefix.
 *
 * For example, for url = http://example.com/,
 * pagination with pathPrefix = after= and limit = 3
 * finds link http://example.com/after=AA on page http://example.com/,
 * then finds link http://example.com/after=BB on page http://example.com/after=AA,
 * yields pages http://example.com/, http://example.com/after=AA and http://example.com/after=BB.
 */
data class LinkedPagination(
        val pathPrefix: String = "after=",
        val limit: Int = 3 // requires attention to prevent missing or excessive pages
): Pagination()
