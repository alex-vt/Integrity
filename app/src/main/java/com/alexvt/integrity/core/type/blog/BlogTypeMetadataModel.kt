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
        val url: String,
        val relatedPageLinksPattern: String // CSS selector, or empty when saving only this page
) : TypeMetadata() {
    constructor() : this("", "")
}

