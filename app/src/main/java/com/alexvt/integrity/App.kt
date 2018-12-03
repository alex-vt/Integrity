/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity

import android.util.Log
import com.alexvt.integrity.core.*
import com.alexvt.integrity.core.filesystem.LocalFolderLocation
import com.alexvt.integrity.core.filesystem.LocalFolderLocationUtil
import com.alexvt.integrity.core.filesystem.samba.SambaFolderLocation
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

        // todo add in app
        //IntegrityCore.presetRepository.clear()
        IntegrityCore.presetRepository.addFolderLocation(LocalFolderLocation("/storage/emulated/0/Integrity"))
        //IntegrityCore.presetRepository.addFolderLocation(SambaFolderLocation("user", "password", "smb://path"))

        Log.d("initialized", "Initialized")
    }
}