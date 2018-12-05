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
        val paginationUsed: Boolean = false,
        val pagination: Pagination = Pagination(),
        val relatedPageLinksUsed: Boolean = false,
        val relatedPageLinksPattern: String = "",// CSS selector, or null when saving only this page
        val loadIntervalMillis: Long = 1000
) : TypeMetadata()

/**
 * Pagination scheme describing a typical sequential set of blog pages.
 *
 * For example, for url = http://example.com/,
 * pagination with path = page/, startIndex = 1, step = 1 and limit = 3
 * yields pages http://example.com/page/1, http://example.com/page/2 and http://example.com/page/3.
 */
data class Pagination(
        val path: String = "page/",
        val startIndex: Int = 1,
        val step: Int = 1,
        val limit: Int = 3 // requires attention to prevent missing or excessive pages
)

