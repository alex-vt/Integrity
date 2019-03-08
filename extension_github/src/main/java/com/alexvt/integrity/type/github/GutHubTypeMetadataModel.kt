/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.type.github

import com.alexvt.integrity.lib.metadata.TypeMetadata

/**
 * Metadata of GitHub type: experimental, loading some data by user name from GitHub.
 */
data class GitHubTypeMetadata(
        val userName: String = ""
) : TypeMetadata()
