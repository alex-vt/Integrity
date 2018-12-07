/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity

import android.util.Log
import com.alexvt.integrity.core.*
import com.alexvt.integrity.core.filesystem.local.LocalFolderLocation
import com.alexvt.integrity.core.filesystem.local.LocalFolderLocationUtil
import com.alexvt.integrity.core.filesystem.samba.SambaFolderLocation
import com.alexvt.integrity.core.filesystem.samba.SambaFolderLocationCredentials
import com.alexvt.integrity.core.filesystem.samba.SambaFolderLocationUtil
import com.alexvt.integrity.core.type.blog.BlogTypeUtil
import com.alexvt.integrity.core.type.blog.BlogTypeMetadata

class App : android.app.Application() {

    override fun onCreate() {
        super.onCreate()

        // Integrity core: initializing for file locations and data types
        IntegrityCore.init(this)
        IntegrityCore.registerFileLocationUtil(LocalFolderLocation::class.java, LocalFolderLocationUtil())
        IntegrityCore.registerFileLocationUtil(SambaFolderLocation::class.java, SambaFolderLocationUtil())
        IntegrityCore.registerDataTypeUtil(BlogTypeMetadata::class.java, BlogTypeUtil())
        //IntegrityCore.metadataRepository.clear()

        /*
        // todo add in app
        IntegrityCore.folderLocationRepository.clear()
        IntegrityCore.folderLocationRepository.addFolderLocation(
                LocalFolderLocation("Device storage", "/storage/emulated/0/Integrity"))
        val sambaId = IntegrityCore.folderLocationRepository.addFolderLocation(
                SambaFolderLocation("Samba server", "smb://path"))
        IntegrityCore.folderLocationRepository.addFolderLocationCredentials(
                SambaFolderLocationCredentials(sambaId, "user", "password"))
        */

        Log.d("initialized", "Initialized")
    }
}