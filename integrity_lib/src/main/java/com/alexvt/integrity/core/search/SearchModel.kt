/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

data class DataChunk(val artifactId: Long = 0,
                     val date: String = "",
                     val text: String = "",
                     val links: ArrayList<Pair<String, String>> = arrayListOf()
)

data class DataChunks(val dataChunkList: ArrayList<DataChunk> = arrayListOf())