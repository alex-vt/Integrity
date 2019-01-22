/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity

import android.app.Application
import com.alexvt.integrity.core.*
import com.alexvt.integrity.core.filesystem.local.LocalFolderLocation
import com.alexvt.integrity.core.filesystem.local.LocalLocationUtil
import com.alexvt.integrity.core.filesystem.samba.SambaFolderLocation
import com.alexvt.integrity.core.filesystem.samba.SambaLocationUtil
import com.alexvt.integrity.lib.IntegrityEx
import com.alexvt.integrity.lib.Log

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        IntegrityEx.handleUncaughtExceptions(this)

        // Integrity core: initializing for file locations // todo locate dynamically as services
        IntegrityCore.init(this)
        IntegrityCore.registerFileLocationUtil(LocalFolderLocation::class.java, LocalLocationUtil())
        IntegrityCore.registerFileLocationUtil(SambaFolderLocation::class.java, SambaLocationUtil())

        //IntegrityCore.metadataRepository.clear()
        //IntegrityCore.logRepository.clear()

        /*
        IntegrityCore.folderLocationRepository.clear()
        IntegrityCore.folderLocationRepository.addFolderLocation(
                LocalFolderLocation("Device storage", "/storage/emulated/0/Integrity"))
        */

        Log(this).what("IntegrityCore initialized")
                .where("onCreate").log()
    }
}