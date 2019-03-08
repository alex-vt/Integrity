/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.types

/**
 * Snapshot data type in Integrity app.
 */
data class DataType(val title: String, val packageName: String, val viewerClass: String,
                    val downloaderClass: String)