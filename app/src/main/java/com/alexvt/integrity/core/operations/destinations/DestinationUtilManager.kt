/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.operations.destinations

import com.alexvt.integrity.core.data.credentials.CredentialsRepository
import com.alexvt.integrity.lib.core.data.destinations.DestinationNameRepository
import com.alexvt.integrity.lib.core.data.destinations.LocalFolderLocation
import com.alexvt.integrity.lib.core.data.destinations.SambaFolderLocation
import com.alexvt.integrity.lib.core.data.metadata.FolderLocation
import com.alexvt.integrity.lib.core.operations.filesystem.FilesystemManager
import com.alexvt.integrity.lib.core.operations.log.LogManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DestinationUtilManager @Inject constructor(
        credentialsRepository: CredentialsRepository,
        filesystemManager: FilesystemManager,
        logManager: LogManager,
        private val destinationNameRepository: DestinationNameRepository
) {

    private val destinationMap = linkedMapOf(
            LocalFolderLocation::class.java to LocalDestinationUtil(filesystemManager),
            SambaFolderLocation::class.java to SambaDestinationUtil(credentialsRepository,
                    filesystemManager, logManager)
    )

    fun getDestinationClasses() = destinationMap.keys.toList()

    fun getDestinationNames() = getDestinationClasses().map { getDestinationName(it) }

    fun getDestinationName(destinationClass: Class<out FolderLocation>)
            = destinationNameRepository.getDestinationName(destinationClass)

    /**
     * See https://stackoverflow.com/a/41103379
     */
    fun <F: FolderLocation> get(dataArchiveLocationClass: Class<F>): DestinationUtil<F> {
        return destinationMap[dataArchiveLocationClass] as DestinationUtil<F>
    }

}