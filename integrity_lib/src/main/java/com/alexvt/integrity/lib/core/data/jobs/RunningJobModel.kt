/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.data.jobs

/**
 * A progress of loading or performing other async operation with data or metadata, with a result
 */
data class JobProgress<T>(
        val progressMessage: String? = "",
        val result: T? = null
)
