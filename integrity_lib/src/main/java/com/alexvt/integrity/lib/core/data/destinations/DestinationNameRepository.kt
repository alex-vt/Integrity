/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.data.destinations

import com.alexvt.integrity.lib.core.data.metadata.FolderLocation

interface DestinationNameRepository {

    fun getDestinationName(destinationClass: Class<out FolderLocation>): String

}