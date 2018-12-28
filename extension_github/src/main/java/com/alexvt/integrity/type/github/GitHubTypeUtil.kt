/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

import android.util.Log
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.alexvt.integrity.core.type.DataTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext


class GitHubTypeUtil: DataTypeUtil<GitHubTypeMetadata> {

    override fun getTypeScreenName(): String = "Website (Blog) Pages"

    override fun getOperationMainActivityClass() = GitHubTypeActivity::class.java

    override suspend fun downloadData(artifactId: Long, date: String,
                                      metadata: GitHubTypeMetadata,
                                      jobContext: CoroutineContext): String {
        Log.d("GitHubTypeUtil", "downloadData start")
        val snapshotPath = DataCacheFolderUtil.ensureSnapshotFolder(artifactId, date)

        runBlocking(Dispatchers.Main) {
            // todo
        }

        Log.d("GitHubTypeUtil", "downloadData end")
        return snapshotPath
    }
}
