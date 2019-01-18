/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.lib.databinding.ActivityDataTypeBinding
import kotlin.collections.ArrayList


abstract class DataTypeActivity : AppCompatActivity() {

    private val TAG = DataTypeActivity::class.java.simpleName
    private lateinit var binding : ActivityDataTypeBinding

    // snapshot metadata to view/save related data // todo move to repository, keep state synced
    private lateinit var snapshot: SnapshotMetadata


    // Data type methods for implementation

    abstract fun getTypeName(): String

    abstract fun getTypeMetadataNewInstance(): TypeMetadata

    abstract fun inflateContentView(context: Context): ViewDataBinding

    abstract fun inflateControlsView(context: Context): ViewDataBinding

    abstract fun inflateFilterView(context: Context): ViewDataBinding

    abstract fun fillInTypeOptions(snapshot: SnapshotMetadata, isEditable: Boolean)

    abstract fun snapshotViewModeAction(snapshot: SnapshotMetadata)

    abstract fun snapshotCreateModeAction(snapshot: SnapshotMetadata): SnapshotMetadata

    abstract fun contentCanGoBack(): Boolean

    abstract fun contentGoBack()

    abstract fun checkSnapshot(snapshot: SnapshotMetadata): Pair<SnapshotMetadata, Boolean>


    // Data type API methods

    protected fun isSnapshotViewMode(): Boolean {
        if (snapshotDataExists()) {
            val snapshot = IntegrityCore.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)
            if (snapshot.status != SnapshotStatus.BLUEPRINT) {
                return true
            }
        }
        return false
    }

    protected fun isSnapshotCreateMode(): Boolean {
        if (snapshotDataExists()) {
            val snapshot = IntegrityCore.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)
            if (snapshot.status == SnapshotStatus.BLUEPRINT) {
                return true
            }
        }
        return false
    }

    protected fun isArtifactCreateMode() = !snapshotDataExists()

    protected fun setTitleInControls(title: String) {
        binding.etName.setText(title)
    }

    protected fun closeFilterDrawer() {
        binding.dlAllContent.closeDrawers()
    }


    // activity lifecycle (not exposed)

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data_type)

        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        binding.content.addView(inflateContentView(this).root)
        binding.controls.addView(inflateControlsView(this).root)
        binding.filter.addView(inflateFilterView(this).root)

        if (isSnapshotViewMode()) {
            snapshot = IntegrityCore.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)

            // Incomplete snapshot can be completed, apart from creating a new blueprint from it
            if (snapshot.status == SnapshotStatus.INCOMPLETE) {
                supportActionBar!!.title = "Incomplete ${getTypeName()} Type Snapshot"
                setContinueSavingButtonVisible(true)
            } else {
                supportActionBar!!.title = "Viewing ${getTypeName()} Type Snapshot"
                setContinueSavingButtonVisible(false)
            }

            fillInCommonOptions(snapshot, isEditable = false)
            fillInTypeOptions(snapshot, isEditable = false)

            snapshotViewModeAction(snapshot)

        } else if (isSnapshotCreateMode()) {
            snapshot = IntegrityCore.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)

            supportActionBar!!.title = "Creating new ${getTypeName()} Type Snapshot"

            fillInCommonOptions(snapshot, isEditable = true)
            fillInTypeOptions(snapshot, isEditable = true)

            snapshot = snapshotCreateModeAction(snapshot)

        } else {
            snapshot = SnapshotMetadata(
                    artifactId = System.currentTimeMillis(),
                    title = "${getTypeName()} Type Artifact",
                    dataTypeSpecificMetadata = getTypeMetadataNewInstance()
            )

            supportActionBar!!.title = "Creating new ${getTypeName()} Type Artifact"

            fillInCommonOptions(snapshot, isEditable = true)
            fillInTypeOptions(snapshot, isEditable = true)
        }
    }

    final override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_data_view, menu)
        return true
    }

    final override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_options -> {
                binding.dlAllContent.openDrawer(Gravity.RIGHT)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    final override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        return true
    }

    final override fun onBackPressed() {
        if (contentCanGoBack()) {
            contentGoBack()
        } else {
            super.onBackPressed()
        }
    }

    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (IntentUtil.getSnapshot(data) != null) {
            snapshot = snapshot.copy(
                    archiveFolderLocations = IntentUtil.getSnapshot(data)!!.archiveFolderLocations
            )
            updateFolderLocationSelectionInViews(IntentUtil.getFolderLocationNames(data))
        }
    }


    // private helper methods

    private fun snapshotDataExists() = IntentUtil.getSnapshot(intent) != null

    private fun setContinueSavingButtonVisible(visible: Boolean) {
        binding.bContinueSaving.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun fillInCommonOptions(snapshot: SnapshotMetadata, isEditable: Boolean) {
        binding.etName.isEnabled = isEditable
        binding.etName.append(snapshot.title)
        binding.etDescription.isEnabled = isEditable
        binding.etDescription.append(snapshot.description)

        updateFolderLocationSelectionInViews(IntentUtil.getFolderLocationNames(intent))
        binding.bArchiveLocation.isEnabled = isEditable
        binding.bArchiveLocation.setOnClickListener { openFolderLocationList(selectMode = true) }

        binding.bManageArchiveLocations.isEnabled = isEditable
        binding.bManageArchiveLocations.setOnClickListener { openFolderLocationList(selectMode = false) }

        binding.tvDownloadSchedule.text = getDownloadScheduleText(snapshot.downloadSchedule)
        binding.bDownloadSchedule.isEnabled = isEditable
        binding.bDownloadSchedule.setOnClickListener { askSetDownloadSchedule() }

        binding.sDownloadOnWifi.isEnabled = isEditable
        binding.sDownloadOnWifi.isChecked = snapshot.downloadSchedule.allowOnWifi
        binding.sDownloadOnMobileData.isEnabled = isEditable
        binding.sDownloadOnMobileData.isChecked = snapshot.downloadSchedule.allowOnMobileData

        supportActionBar!!.subtitle = snapshot.title

        binding.bSaveBlueprint.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.BLUEPRINT) }
        binding.bSave.visibility = if (isEditable) View.VISIBLE else View.GONE
        binding.bSave.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.IN_PROGRESS) }
        binding.bContinueSaving.setOnClickListener { checkAndReturnSnapshot(SnapshotStatus.INCOMPLETE) }
    }

    private fun openFolderLocationList(selectMode: Boolean) {
        val intent = Intent()
        intent.component = ComponentName("com.alexvt.integrity",
                "com.alexvt.integrity.base.activity.FolderLocationsActivity") // todo resolve
        IntentUtil.putSelectMode(intent, selectMode)
        IntentUtil.putSnapshot(intent, IntegrityCore.fromTypeSpecificMetadata(this, snapshot))
        startActivityForResult(intent, 0)
    }

    private fun updateFolderLocationSelectionInViews(folderLocationTexts: Array<String>) {
        binding.tvArchiveLocations.text = folderLocationTexts.joinToString(separator = ", ")
    }

    // todo improve this option
    private fun getDownloadScheduleText(downloadSchedule: DownloadSchedule)
            = "Every " + downloadSchedule.periodSeconds + " s"

    private val downloadScheduleOptionMap = mapOf(
            Pair("Never", 0L),
            Pair("Every minute", 60L),
            Pair("Every hour", 60 * 60L),
            Pair("Every day", 24 * 60 * 60L),
            Pair("Every week", 7 * 24 * 60 * 60L),
            Pair("Every month", 30 * 7 * 24 * 60 * 60L)
    )

    private fun askSetDownloadSchedule() {
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

    private fun updateDownloadScheduleInViews(optionText: String) {
        binding.tvDownloadSchedule.text = optionText
    }

    private fun checkAndReturnSnapshot(status: String) {
        if (checkSnapshotCommon(status)) {
            val snapshotCheckResult = checkSnapshot(snapshot)
            val snapshotToReturn = snapshotCheckResult.first
            val isSnapshotValid = snapshotCheckResult.second
            if (isSnapshotValid) {
                returnSnapshot(snapshotToReturn)
            }
        }
    }

    private fun checkSnapshotCommon(status: String): Boolean {
        if (binding.etName.text.trim().isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (snapshot.archiveFolderLocations.isEmpty()) {
            Toast.makeText(this, "Please add location where to save archive", Toast.LENGTH_SHORT).show()
            return false
        }
        val timestamp = System.currentTimeMillis()
        // for new artifact, generating artifactId
        if (snapshot.artifactId < 1) {
            snapshot = snapshot.copy(artifactId = timestamp)
        }
        snapshot = snapshot.copy(
                title = binding.etName.text.toString(),
                description = binding.etDescription.text.toString(),
                downloadSchedule = snapshot.downloadSchedule.copy( // period is already set
                        allowOnWifi = binding.sDownloadOnWifi.isChecked,
                        allowOnMobileData = binding.sDownloadOnMobileData.isChecked
                ),
                status = status
        )
        return true
    }

    private fun returnSnapshot(snapshot: SnapshotMetadata) {
        val intent = Intent()
        IntentUtil.putSnapshot(intent, IntegrityCore.fromTypeSpecificMetadata(this, snapshot))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
