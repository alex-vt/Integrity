/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.util

import android.content.Intent
import com.alexvt.integrity.core.log.LogEntry
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.core.util.JsonSerializerUtil

/**
 * Puts and gets data fro intents
 */
object IntentUtil {

    private const val artifactId = "artifactId"
    private const val date = "date"
    private const val message = "message"
    private const val downloaded = "downloaded"
    private const val snapshot = "snapshot"
    private const val selectMode = "selectMode"
    private const val folderLocationNames = "folderLocationNames"
    private const val logEntry = "logEntry"

    fun putArtifactId(intent: Intent?, artifactId: Long): Intent {
        intent!!.putExtra(IntentUtil.artifactId, artifactId)
        return intent
    }

    fun getArtifactId(intent: Intent?) = intent!!.getLongExtra(artifactId, -1)

    fun putDate(intent: Intent?, date: String): Intent {
        intent!!.putExtra(IntentUtil.date, date)
        return intent
    }

    fun getDate(intent: Intent?) = intent!!.getStringExtra(date)!!

    fun withMessage(message: String): Intent {
        val intent = Intent()
        intent.putExtra(IntentUtil.message, message)
        return intent
    }

    fun getMessage(intent: Intent?) = intent!!.getStringExtra(message)!!

    fun withDownloaded(downloaded: Boolean): Intent {
        val intent = Intent()
        intent.putExtra(IntentUtil.downloaded, downloaded)
        return intent
    }

    fun isDownloaded(intent: Intent?) = intent!!.getBooleanExtra(downloaded, false)

    fun putSnapshot(intent: Intent?, snapshot: Snapshot): Intent {
        intent!!.putExtra(IntentUtil.snapshot, JsonSerializerUtil.toJson(snapshot))
        return intent
    }

    fun getSnapshot(intent: Intent?) = if (intent?.hasExtra(snapshot) == true) {
        JsonSerializerUtil.fromJson(intent.getStringExtra(snapshot), Snapshot::class.java)
    } else {
        null
    }

    fun putSelectMode(intent: Intent?, selectMode: Boolean): Intent {
        intent!!.putExtra(IntentUtil.selectMode, selectMode)
        return intent
    }

    fun isSelectMode(intent: Intent?) = intent!!.getBooleanExtra(selectMode, false)


    fun putFolderLocationNames(intent: Intent?, folderLocationNames: Array<String>): Intent {
        intent!!.putExtra(IntentUtil.folderLocationNames, folderLocationNames)
        return intent
    }

    fun getFolderLocationNames(intent: Intent?) = intent!!.getStringArrayExtra(folderLocationNames)!!

    fun withLogEntry(logEntry: LogEntry): Intent {
        val intent = Intent()
        intent.putExtra(IntentUtil.logEntry, logEntry)
        return intent
    }

    fun getLogEntry(intent: Intent?) = intent!!.getSerializableExtra(logEntry) as LogEntry

}