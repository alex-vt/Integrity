/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type

import android.webkit.WebView
import com.alexvt.integrity.core.TypeMetadata

data class BlogTypeMetadata(
        val urls: ArrayList<String>
) : TypeMetadata() {
    constructor() : this(arrayListOf())
}

