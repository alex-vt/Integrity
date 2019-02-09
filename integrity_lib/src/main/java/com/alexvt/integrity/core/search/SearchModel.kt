/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.search

data class DataChunk(val artifactId: Long = 0,
                     val date: String = "",
                     val text: String = "",
                     val index: String = "", // unique within the snapshot
                     val links: ArrayList<NamedLink> = arrayListOf()
)

data class DataChunks(val chunks: ArrayList<DataChunk> = arrayListOf())

data class NamedLink(val title: String = "", val link: String = "")

data class SearchResult(val artifactTitle: String = "",
                        val date: String = "",
                        val searchedText: String = "",
                        val viewedText: String = "",
                        val viewedLinks: ArrayList<NamedLink> = arrayListOf()
)