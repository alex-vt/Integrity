/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.destinations

import com.alexvt.integrity.lib.metadata.FolderLocation

object DestinationNameUtilResolver {

    /**
     * See https://stackoverflow.com/a/41103379
     */
    fun <F: FolderLocation> getDestinationNameUtil(dataArchiveLocationClass: Class<F>): DestinationNameUtil<F> {
        val utilClassName = dataArchiveLocationClass.name
                .replace("FolderLocation", "DestinationNameUtil")
        return Class.forName(utilClassName).kotlin.objectInstance as DestinationNameUtil<F>
    }

    fun getDestinationNames(archiveFolderLocations: List<FolderLocation>) = archiveFolderLocations
            .map { getFolderLocationName(it) }
            .toTypedArray()

    fun getFolderLocationName(folderLocation: FolderLocation) = folderLocation.title +
            " (" + getDestinationNameUtil(folderLocation.javaClass).getFolderLocationLabel() + "): " +
            getDestinationNameUtil(folderLocation.javaClass).getFolderLocationDescription(folderLocation)


}