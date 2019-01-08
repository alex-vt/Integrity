/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.alexvt.integrity.core.*
import com.alexvt.integrity.lib.util.IntentUtil
import kotlin.collections.ArrayList


abstract class DataTypeActivity : AppCompatActivity() {

    private val TAG = DataTypeActivity::class.java.simpleName

    // snapshot metadata to view/save related data
    protected lateinit var snapshot: SnapshotMetadata

    protected fun snapshotDataExists() = IntentUtil.getSnapshot(intent) != null

    protected fun snapshotViewMode(): Boolean {
        if (snapshotDataExists()) {
            snapshot = IntegrityEx.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)
            if (snapshot.status != SnapshotStatus.BLUEPRINT) {
                return true
            }
        }
        return false
    }

    protected fun snapshotCreateMode(): Boolean {
        if (snapshotDataExists()) {
            snapshot = IntegrityEx.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)
            if (snapshot.status == SnapshotStatus.BLUEPRINT) {
                return true
            }
        }
        return false
    }

    protected fun artifactCreateMode() = !snapshotDataExists()

    protected fun openFolderLocationList(selectMode: Boolean) {
        val intent = Intent()
        intent.component = ComponentName("com.alexvt.integrity",
                "com.alexvt.integrity.base.activity.FolderLocationsActivity") // todo resolve
        IntentUtil.putSelectMode(intent, selectMode)
        IntentUtil.putSnapshot(intent, IntegrityEx.fromTypeSpecificMetadata(this, snapshot))
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (IntentUtil.getSnapshot(data) != null) {
            snapshot = snapshot.copy(
                    archiveFolderLocations = IntentUtil.getSnapshot(data)!!.archiveFolderLocations
            )
            updateFolderLocationSelectionInViews(IntentUtil.getFolderLocationNames(data))
        }
    }

    abstract fun updateFolderLocationSelectionInViews(folderLocationTexts: Array<String>)

    protected fun getArchiveLocationsText(folderLocations: Collection<FolderLocation>)
            = IntegrityCore.getNamedFolderLocationMap(folderLocations).keys
            .joinToString(", ")

    // todo improve this option
    protected fun getDownloadScheduleText(downloadSchedule: DownloadSchedule)
            = "Every " + downloadSchedule.periodSeconds + " s"

    protected val downloadScheduleOptionMap = mapOf(
            Pair("Never", 0L),
            Pair("Every minute", 60L),
            Pair("Every hour", 60 * 60L),
            Pair("Every day", 24 * 60 * 60L),
            Pair("Every week", 7 * 24 * 60 * 60L),
            Pair("Every month", 30 * 7 * 24 * 60 * 60L)
    )

    protected fun askSetDownloadSchedule() {
        MaterialDialog(this)
                .listItems(items = ArrayList(downloadScheduleOptionMap.keys)) { _, _, text ->
                    snapshot = snapshot.copy(
                            downloadSchedule = snapshot.downloadSchedule
                                    .copy(periodSeconds = downloadScheduleOptionMap[text]!!)
                    )
                    updateDownloadScheduleInViews(text)
                }
                .show()
    }

    abstract fun updateDownloadScheduleInViews(optionText: String)

    protected fun checkAndReturnSnapshot(status: String) {
        if (checkSnapshot(status)) {
            returnSnapshot()
        }
    }

    abstract fun checkSnapshot(status: String): Boolean

    protected fun returnSnapshot() {
        val intent = Intent()
        IntentUtil.putSnapshot(intent, IntegrityEx.fromTypeSpecificMetadata(this, snapshot))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }

}
