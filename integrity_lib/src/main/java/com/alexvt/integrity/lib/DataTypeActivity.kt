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
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItems
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.util.DataCacheFolderUtil
import com.alexvt.integrity.core.util.JsonSerializerUtil
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.lib.databinding.ActivityDataTypeBinding
import com.alexvt.integrity.lib.databinding.ViewColorEditBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


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

    protected fun endPreview() { // todo end more gracefully
        val previewEndDelayMillis = 300L
        Timer().schedule(previewEndDelayMillis) {
            runOnUiThread {
                binding.ivPreview.visibility = View.GONE
                binding.pbLoading.visibility = View.GONE
            }
        }
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

        binding.content.addView(inflateContentView(this).root, 0)
        binding.controls.addView(inflateControlsView(this).root)
        binding.filter.addView(inflateFilterView(this).root)

        if (isSnapshotViewMode()) {
            snapshot = IntegrityCore.toTypeSpecificMetadata(IntentUtil.getSnapshot(intent)!!)

            viewSnapshot(snapshot)
            if (snapshot.status == SnapshotStatus.COMPLETE) {
                showDateSelector(IntentUtil.getDates(intent))
            }

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
                    archiveFolderLocations = IntentUtil.getSnapshot(data)!!.archiveFolderLocations,
                    tags = IntentUtil.getSnapshot(data)!!.tags
            )
            updateFolderLocationSelectionInViews(snapshot.archiveFolderLocations)
            updateTagSelectionInViews(snapshot.tags)
        }
    }


    // private helper methods

    private fun viewSnapshot(snapshot: SnapshotMetadata) {
        // Incomplete snapshot can be completed, apart from creating a new blueprint from it
        if (snapshot.status == SnapshotStatus.INCOMPLETE) {
            supportActionBar!!.title = "Incomplete ${getTypeName()} Type Snapshot"
            setContinueSavingButtonVisible(true)
        } else {
            supportActionBar!!.title = "Viewing ${getTypeName()} Type Snapshot"
            setContinueSavingButtonVisible(false)

            showPreview(snapshot)
        }

        fillInCommonOptions(snapshot, isEditable = false)
        fillInTypeOptions(snapshot, isEditable = false)

        snapshotViewModeAction(snapshot)
    }

    private fun showPreview(snapshot: SnapshotMetadata) {
        binding.ivPreview.visibility = View.VISIBLE
        Glide.with(this)
                .asBitmap()
                .load(IntegrityCore.getSnapshotPreviewPath(applicationContext, snapshot.artifactId,
                        snapshot.date))
                .apply(RequestOptions().dontAnimate())
                .into(binding.ivPreview)
        binding.pbLoading.visibility = View.VISIBLE
    }

    private fun showDateSelector(dates: Array<String>) {
        binding.hpDates.visibility = View.VISIBLE
        binding.hpDates.values = dates
        binding.hpDates.selectedItem = dates.indexOf(snapshot.date)
        binding.hpDates.setOnItemSelectedListener {
            val snapshotFolderName = IntegrityEx.getSnapshotDataFolderPath(applicationContext,
                    snapshot.artifactId, dates[it])
            val snapshotJson = DataCacheFolderUtil.getTextFromFile(this,
                    "$snapshotFolderName/_metadata.json.txt")
            snapshot = JsonSerializerUtil.fromJson(snapshotJson, SnapshotMetadata::class.java)
            viewSnapshot(snapshot)
        }
    }

    private fun snapshotDataExists() = IntentUtil.getSnapshot(intent) != null

    private fun setContinueSavingButtonVisible(visible: Boolean) {
        binding.bContinueSaving.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun fillInCommonOptions(snapshot: SnapshotMetadata, isEditable: Boolean) {
        binding.etName.isEnabled = isEditable
        binding.etName.setText("")
        binding.etName.append(snapshot.title)
        binding.etDescription.isEnabled = isEditable
        binding.etDescription.setText("")
        binding.etDescription.append(snapshot.description)

        updateFolderLocationSelectionInViews(snapshot.archiveFolderLocations)
        binding.bArchiveLocation.isEnabled = isEditable
        binding.bArchiveLocation.setOnClickListener { openFolderLocationList(selectMode = true) }
        binding.bManageArchiveLocations.isEnabled = isEditable
        binding.bManageArchiveLocations.setOnClickListener { openFolderLocationList(selectMode = false) }

        updateTagSelectionInViews(snapshot.tags)
        binding.bTags.isEnabled = isEditable
        binding.bTags.setOnClickListener { openTagList(selectMode = true) }
        binding.bThemeColor.isEnabled = isEditable
        binding.bThemeColor.setOnClickListener { openThemeColorPicker() }

        binding.tvDownloadSchedule.text = getDownloadScheduleText(snapshot.downloadSchedule)
        binding.bDownloadSchedule.isEnabled = isEditable
        binding.bDownloadSchedule.setOnClickListener { askSetDownloadSchedule() }

        binding.sDownloadOnWifiOnly.isEnabled = isEditable
        binding.sDownloadOnWifiOnly.isChecked = snapshot.downloadSchedule.allowOnWifiOnly
        binding.sDownloadOnLowBattery.isEnabled = isEditable
        binding.sDownloadOnLowBattery.isChecked = snapshot.downloadSchedule.allowOnLowBattery

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

    private fun updateFolderLocationSelectionInViews(folderLocations: List<FolderLocation>) {
        binding.tvArchiveLocations.text = IntegrityCore.getFolderLocationNames(folderLocations)
                .joinToString(separator = ", ")
    }

    // todo theme color selection UI

    private fun openTagList(selectMode: Boolean) {
        val intent = Intent()
        intent.component = ComponentName("com.alexvt.integrity",
                "com.alexvt.integrity.base.activity.TagsActivity") // todo resolve
        IntentUtil.putSelectMode(intent, selectMode)
        IntentUtil.putSnapshot(intent, IntegrityCore.fromTypeSpecificMetadata(this, snapshot))
        startActivityForResult(intent, 0)
    }

    private fun updateTagSelectionInViews(tags: List<Tag>) {
        binding.tvTags.text = tags.joinToString(separator = ", ") { it.text }
    }

    private fun openThemeColorPicker() {
        // todo improve color picker
        val colorEditViews: ViewColorEditBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_color_edit, null, false)
        colorEditViews.tbColor1.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        colorEditViews.tbColor2.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        colorEditViews.tbColor3.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        colorEditViews.tbColor4.setOnClickListener { (it.parent as RadioGroup).check(it.id) }
        colorEditViews.toggleGroup.setOnCheckedChangeListener { group, checkedId ->
            for (j in 0 until group.childCount) {
                val view = group.getChildAt(j) as ToggleButton
                view.isChecked = (view.id == checkedId)
            }
        }
        when {
            snapshot.themeColor == "#FFFFFF" -> colorEditViews.tbColor1.isChecked = true
            snapshot.themeColor == "#EE8888" -> colorEditViews.tbColor2.isChecked = true
            snapshot.themeColor == "#88EE88" -> colorEditViews.tbColor3.isChecked = true
            snapshot.themeColor == "#8888EE" -> colorEditViews.tbColor4.isChecked = true
        }
        MaterialDialog(this)
                .customView(view = colorEditViews.toggleGroup)
                .positiveButton(text = "Save") {
                    val color = when {
                        colorEditViews.tbColor2.isChecked -> "#EE8888"
                        colorEditViews.tbColor3.isChecked -> "#88EE88"
                        colorEditViews.tbColor4.isChecked -> "#8888EE"
                        else -> "#FFFFFF" // color 1
                    }
                    snapshot = snapshot.copy(themeColor = color)
                }
                .negativeButton(text = "Cancel")
                .show()
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
                    binding.tvDownloadSchedule.text = text
                }
                .show()
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
                        allowOnWifiOnly = binding.sDownloadOnWifiOnly.isChecked,
                        allowOnLowBattery = binding.sDownloadOnLowBattery.isChecked
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
