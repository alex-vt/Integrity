/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.util

import android.content.Intent
import com.alexvt.integrity.lib.log.LogEntry
import com.alexvt.integrity.lib.metadata.Snapshot

/**
 * Puts and gets data fro intents
 */
object IntentUtil {

    private const val recreate = "recreate"
    private const val refresh = "refresh"
    private const val artifactId = "artifactId"
    private const val date = "date"
    private const val dates = "dates"
    private const val message = "message"
    private const val downloaded = "downloaded"
    private const val snapshot = "snapshot"
    private const val selectMode = "selectMode"
    private const val logEntry = "logEntry"
    private const val viewExtensions = "viewExtensions"
    private const val issueDescription = "issueDescription"
    private const val fontName = "fontName"
    private const val colorBackground = "colorBackground"
    private const val colorPrimary = "colorPrimary"
    private const val colorAccent = "colorAccent"
    private const val dataFolderName = "dataFolderName"
    private const val title = "title"


    fun withRecreate(downloaded: Boolean): Intent {
        val intent = Intent()
        intent.putExtra(recreate, downloaded)
        return intent
    }

    fun isRecreate(intent: Intent?) = intent?.getBooleanExtra(recreate, false) ?: false

    fun withRefresh(downloaded: Boolean): Intent {
        val intent = Intent()
        intent.putExtra(refresh, downloaded)
        return intent
    }

    fun isRefresh(intent: Intent?) = intent?.getBooleanExtra(refresh, false) ?: false

    fun putArtifactId(intent: Intent?, artifactId: Long?): Intent {
        if (artifactId != null) intent!!.putExtra(IntentUtil.artifactId, artifactId)
        return intent!!
    }

    fun getArtifactId(intent: Intent?) = intent!!.getLongExtra(artifactId, -1)

    fun putDate(intent: Intent?, date: String?): Intent {
        if (date != null) intent!!.putExtra(IntentUtil.date, date)
        return intent!!
    }

    fun getDate(intent: Intent?) = intent!!.getStringExtra(date)!!

    fun putDates(intent: Intent?, dates: List<String>?): Intent {
        if (dates != null) {
            intent!!.putExtra(IntentUtil.dates, dates.toTypedArray())
        }
        return intent!!
    }

    fun getDates(intent: Intent?) = intent!!.getStringArrayExtra(dates)!!


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

    fun putSnapshot(intent: Intent?, snapshot: Snapshot?): Intent {
        if (snapshot != null) {
            intent!!.putExtra(IntentUtil.snapshot, JsonSerializerUtil.toJson(snapshot))
        }
        return intent!!
    }

    fun getSnapshot(intent: Intent?) = if (intent?.hasExtra(snapshot) == true) {
        JsonSerializerUtil.fromJson(intent.getStringExtra(snapshot), Snapshot::class.java)
    } else {
        null
    }

    fun putSelectMode(intent: Intent?, selectMode: Boolean?): Intent {
        if (selectMode != null) intent!!.putExtra(IntentUtil.selectMode, selectMode)
        return intent!!
    }

    fun isSelectMode(intent: Intent?) = intent!!.getBooleanExtra(selectMode, false)


    fun withLogEntry(logEntry: LogEntry): Intent {
        val intent = Intent()
        intent.putExtra(IntentUtil.logEntry, logEntry)
        return intent
    }

    fun getLogEntry(intent: Intent?) = intent!!.getSerializableExtra(logEntry) as LogEntry

    fun putViewExtensions(intent: Intent?, value: Boolean?): Intent {
        if (value != null) intent!!.putExtra(viewExtensions, value)
        return intent!!
    }

    fun getViewExtensions(intent: Intent?) = intent?.getBooleanExtra(viewExtensions, false)
            ?: false


    fun withIssueDescription(issueDescription: String): Intent {
        val intent = Intent()
        intent.putExtra(IntentUtil.issueDescription, issueDescription)
        return intent
    }

    fun getIssueDescription(intent: Intent?) = intent?.getStringExtra(issueDescription)


    fun putFontName(intent: Intent?, value: String?): Intent {
        if (value != null) intent!!.putExtra(fontName, value)
        return intent!!
    }

    fun getFontName(intent: Intent?) = intent!!.getStringExtra(fontName)!!

    fun putColorBackground(intent: Intent?, value: String?): Intent {
        if (value != null) intent!!.putExtra(colorBackground, value)
        return intent!!
    }

    fun getColorBackground(intent: Intent?) = intent!!.getStringExtra(colorBackground)!!

    fun putColorPrimary(intent: Intent?, value: String?): Intent {
        if (value != null) intent!!.putExtra(colorPrimary, value)
        return intent!!
    }

    fun getColorPrimary(intent: Intent?) = intent!!.getStringExtra(colorPrimary)!!

    fun putColorAccent(intent: Intent?, value: String?): Intent {
        if (value != null) intent!!.putExtra(colorAccent, value)
        return intent!!
    }

    fun getColorAccent(intent: Intent?) = intent!!.getStringExtra(colorAccent)!!

    fun putDataFolderName(intent: Intent?, value: String?): Intent {
        if (value != null) intent!!.putExtra(dataFolderName, value)
        return intent!!
    }

    fun getDataFolderName(intent: Intent?) = intent!!.getStringExtra(dataFolderName)!!

    fun putTitle(intent: Intent?, value: String?): Intent {
        if (value != null) intent!!.putExtra(title, value)
        return intent!!
    }

    fun getTitle(intent: Intent?) = intent?.getStringExtra(title)
}