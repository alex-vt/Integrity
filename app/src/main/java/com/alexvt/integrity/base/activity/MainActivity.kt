/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.base.adapter.SnapshotRecyclerAdapter
import com.alexvt.integrity.base.adapter.SearchResultRecyclerAdapter
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.search.SearchUtil
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.util.IntentUtil
import com.leinardi.android.speeddial.SpeedDialActionItem
import io.reactivex.android.schedulers.AndroidSchedulers

import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.LayoutInflaterCompat
import androidx.databinding.DataBindingUtil
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.expandable.expandableItem
import co.zsmb.materialdrawerkt.draweritems.toggleable.toggleItem
import com.alexvt.integrity.databinding.DrawerHeaderBinding
import com.alexvt.integrity.util.SpeedDialCompatUtil
import com.mikepenz.iconics.context.IconicsLayoutInflater2
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import java.util.*
import co.zsmb.materialdrawerkt.draweritems.badge
import com.alexvt.integrity.BuildConfig
import com.alexvt.integrity.core.util.FontUtil
import com.alexvt.integrity.core.util.ThemeUtil
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.jakewharton.rxbinding3.appcompat.queryTextChanges


class MainActivity : CyaneaAppCompatActivity() {

    data class Inputs(val filteredArtifactId: Long?,
                      val searchText: String,
                      val runningJobsExpanded: Boolean,
                      val scheduledJobsExpanded: Boolean)

    private lateinit var drawer: Drawer

    private var inputs: Inputs = Inputs(null, "", true, true)

    private fun onInputsUpdate(newInputs: Inputs) {
        inputs = newInputs
        refreshSnapshotList(inputs.filteredArtifactId)
        search(inputs.searchText, inputs.filteredArtifactId)
        updateFilterView(inputs.filteredArtifactId)
        updateAddButton(inputs.filteredArtifactId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LayoutInflaterCompat.setFactory2(layoutInflater, IconicsLayoutInflater2(delegate))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        bindDrawer()
        bindAddButton()
        bindSnapshotList()
        bindSnapshotList()
        bindFilter()
        bindSearch()

        onInputsUpdate(inputs)

        FontUtil.setFont(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // todo fix and remove
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ThemeUtil.getColorPrimaryDark()
    }

    private fun bindDrawer() {
        drawer = drawer {
            hasStableIds = true
            toolbar = this@MainActivity.toolbar
            selectedItem = -1
            // Header is updatable
            expandableItem("Running jobs") {
                identifier = 1
                selectable = false
                onClick { _ ->
                    userExpandJobs(false)
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_playlist_play
                iconTintingEnabled = true
                textColor = ThemeUtil.getTextColorPrimary().toLong()
                iconColor = ThemeUtil.getTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity)
            } // updatable
            expandableItem("Up next") {
                identifier = 2
                selectable = false
                onClick { _ ->
                    userExpandJobs(true)
                    false
                }
                iicon = CommunityMaterial.Icon.cmd_calendar_clock
                iconTintingEnabled = true
                textColor = ThemeUtil.getTextColorPrimary().toLong()
                iconColor = ThemeUtil.getTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity)
            } // updatable
            divider { identifier = 3 }
            primaryItem("Archives & Storage") {
                identifier = 4
                selectable = false
                onClick { _ ->
                    viewFolderLocations() // todo also show data cache folder
                    false
                }
                iicon = CommunityMaterial.Icon.cmd_archive
                iconTintingEnabled = true
                textColor = ThemeUtil.getTextColorPrimary().toLong()
                iconColor = ThemeUtil.getTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity)
            }
            primaryItem("Tags") {
                identifier = 5
                selectable = false
                onClick { _ ->
                    viewTags()
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_tag_multiple
                iconTintingEnabled = true
                textColor = ThemeUtil.getTextColorPrimary().toLong()
                iconColor = ThemeUtil.getTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity)
            }
            primaryItem("Log") {
                identifier = 6
            } // updatable
            divider { identifier = 7 }
            primaryItem("Extensions") {
                selectable = false
                onClick { _ ->
                    viewSettings(true)
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_puzzle
                iconTintingEnabled = true
                textColor = ThemeUtil.getTextColorPrimary().toLong()
                iconColor = ThemeUtil.getTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity)
            }
            divider { identifier = 8 }
            primaryItem("Restore...") {
                identifier = 9
                selectable = false
                enabled = false
                onClick { _ ->
                    // todo show options for snapshots (including app settings) recovery from archives
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_history
                iconTintingEnabled = true
                textColor = ThemeUtil.getTextColorPrimary().toLong()
                iconColor = ThemeUtil.getTextColorSecondary().toLong()
                disabledTextColor = ThemeUtil.getTextColorSecondary().toLong()
                disabledIconColor = ThemeUtil.getTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity)
            }
            footer {
                toggleItem("Offline mode") {
                    selectable = false
                    enabled = false
                    onToggled {
                        // todo (also show notification)
                    }
                    textColor = ThemeUtil.getTextColorPrimary().toLong()
                    iconColor = ThemeUtil.getTextColorSecondary().toLong()
                    disabledTextColor = ThemeUtil.getTextColorSecondary().toLong()
                    disabledIconColor = ThemeUtil.getTextColorSecondary().toLong()
                }
                divider {}
                primaryItem("Settings") {
                    selectable = false
                    onClick { _ ->
                        viewSettings()
                        false
                    }
                    textColor = ThemeUtil.getTextColorPrimary().toLong()
                }
                primaryItem("Help") {
                    selectable = false
                    onClick { _ ->
                        // todo
                        false
                    }
                    badge("version ${BuildConfig.VERSION_NAME}") {
                        textColor = ThemeUtil.getTextColorSecondary().toLong()
                        colorRes = R.color.colorNone
                    }
                    textColor = ThemeUtil.getTextColorPrimary().toLong()
                }
                divider {}
                primaryItem("Legal") {
                    selectable = false
                    onClick { _ ->
                        // todo
                        false
                    }
                    textColor = ThemeUtil.getTextColorPrimary().toLong()
                }
            }
            actionBarDrawerToggleAnimated = true
            actionBarDrawerToggleEnabled = true
        }
        drawer.recyclerView.scrollBarDefaultDelayBeforeFade = 3000 // todo don't show when updating but keeping size
        drawer.slider.setBackgroundColor(ThemeUtil.getColorBackground())
        drawer.stickyFooter.setBackgroundColor(ThemeUtil.getColorBackgroundSecondary())
        cyanea.tinter.tint(drawer.slider)
        cyanea.tinter.tint(drawer.stickyFooter)
    }

    private fun userExpandJobs(isScheduledJobs: Boolean) {
        val sectionId = if (isScheduledJobs) 2L else 1L
        val isExpandable = drawer.getDrawerItem(sectionId).subItems.isNotEmpty()
        if (!isExpandable) {
            return
        }
        val isExpanded = drawer.getDrawerItem(sectionId).isExpanded
        onInputsUpdate(if (isScheduledJobs) {
            inputs.copy(scheduledJobsExpanded = isExpanded)
        } else {
            inputs.copy(runningJobsExpanded = isExpanded)
        })
    }

    private fun updateErrorViewsOnDrawer(unreadErrorCount: Int) {
        // Header
        val headerBinding: DrawerHeaderBinding = DataBindingUtil.inflate(LayoutInflater.from(
                this@MainActivity), R.layout.drawer_header, null, false)
        FontUtil.setFont(this, headerBinding.rlView)
        headerBinding.tvTitle.text = if (unreadErrorCount == 0) {
            "App is working normally"
        } else {
            "There are errors."
        }
        headerBinding.bViewLog.visibility = if (unreadErrorCount == 0) View.GONE else View.VISIBLE
        headerBinding.bViewLog.setOnClickListener {
            viewLog()
            drawer.closeDrawer()
        }
        drawer.header = headerBinding.rlView
        headerBinding.rlView.setBackgroundColor(ThemeUtil.getColorBackgroundSecondary())
        headerBinding.tvTitle.setTextColor(ThemeUtil.getTextColorPrimary())
        headerBinding.bViewLog.setTextColor(ThemeUtil.getTextColorSecondary())

        // Log with error badge
        val badgeText = if (unreadErrorCount == 0) "" else "Errors: $unreadErrorCount"
        val badgeColor = if (unreadErrorCount == 0) {
            ThemeUtil.getColorBackground()
        } else {
            getColor(R.color.colorError)
        }
        drawer.updateItem(PrimaryDrawerItem()
                .withIdentifier(6)
                .withName("Log")
                .withSelectable(false)
                .withOnDrawerItemClickListener { view, position, drawerItem -> run {
                    viewLog()
                    false
                } }
                .withTextColor(ThemeUtil.getTextColorPrimary())
                .withIconColor(ThemeUtil.getTextColorSecondary())
                .withIcon(CommunityMaterial.Icon2.cmd_text)
                .withIconTintingEnabled(true)
                .withTypeface(FontUtil.getTypeface(this))
                .withBadge(badgeText)
                .withBadgeStyle(BadgeStyle()
                        .withColor(badgeColor)
                        .withTextColorRes(R.color.colorWhite)
                        .withPaddingLeftRightDp(8)
                        .withCornersDp(12)
                ))
    }

    /**
     * Updates the jobs header and job list.
     */
    private fun updateJobsInDrawer(jobListItems: List<SecondaryDrawerItem>,
                                   sectionId: Long, title: String, titlePlaceholder: String,
                                   isExpanded: Boolean) {
        val sectionPosition = drawer.getPosition(sectionId)

        val countSuffix = if (jobListItems.isEmpty()) "" else "(${jobListItems.size})"
        val visibleTitle = if (jobListItems.isEmpty()) titlePlaceholder else "$title $countSuffix"
        val arrowColor = if (jobListItems.isEmpty()) {
            ThemeUtil.getColorBackground()
        } else {
            ThemeUtil.getColorAccent()
        }

        val jobsExpandableItem = drawer.getDrawerItem(sectionId) as ExpandableDrawerItem

        val nullSubItems = jobsExpandableItem.subItems == null

        val updatedExpandableItem
                = (if (nullSubItems) jobsExpandableItem.withSubItems() else jobsExpandableItem)
                .withName(visibleTitle)
                .withArrowColor(arrowColor)
        drawer.updateItem(updatedExpandableItem)

        val subItems = (drawer.getDrawerItem(sectionId) as ExpandableDrawerItem).subItems
        subItems.clear()
        subItems.addAll(jobListItems)
        drawer.adapter.notifyAdapterDataSetChanged()

        if (isExpanded) {
            drawer.expandableExtension.expand(sectionPosition)
        }
    }

    private fun getScheduledJobDrawerItem(artifactId: Long): SecondaryDrawerItem {
        val snapshot = IntegrityCore.metadataRepository.getLatestSnapshotMetadata(artifactId)
        val timeRemainingMillis = IntegrityCore.getNextJobRunTimestamp(snapshot) -
                System.currentTimeMillis()
        val timeText = if (timeRemainingMillis <= 0) {
            "Should start now"
        } else {
            "Starting in ${timeRemainingMillis / 1000} s"
        }
        return SecondaryDrawerItem()
                .withName(snapshot.title)
                .withDescription(timeText)
                .withIdentifier(artifactId)
                .withLevel(2)
                .withSelectable(false)
                .withTextColor(ThemeUtil.getTextColorSecondary())
                .withDescriptionTextColor(ThemeUtil.getTextColorSecondary())
                .withTypeface(FontUtil.getTypeface(this))
                .withOnDrawerItemClickListener { _, _, _ ->
                    IntegrityCore.openCreateNewSnapshot(this, snapshot.artifactId)
                    false
                }
    }

    private fun getRunningJobDrawerItem(artifactId: Long, date: String): SecondaryDrawerItem {
        val snapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(artifactId, date)
        return SecondaryDrawerItem()
                .withName(snapshot.title)
                .withDescription("Running")
                .withIdentifier(artifactId - 1_000_000_000L) // todo distinguish better
                .withLevel(2)
                .withSelectable(false)
                .withTypeface(FontUtil.getTypeface(this))
                .withOnDrawerItemClickListener { _, _, _ ->
                    IntegrityCore.openViewSnapshotOrShowProgress(this, snapshot.artifactId,
                            snapshot.date)
                    false
                }
    }

    private fun bindSnapshotList() {
        rvSnapshotList.adapter = SnapshotRecyclerAdapter(ArrayList(), this)
    }

    private fun bindAddButton() {
        SpeedDialCompatUtil.setStayOnExpand(sdAdd)
    }

    private fun updateAddButton(artifactId: Long?) {
        sdAdd.clearActionItems()
        if (artifactId != null) {
            sdAdd.addActionItem(SpeedDialActionItem.Builder(0, android.R.drawable.ic_input_add)
                    .setLabel("Create another snapshot")
                    .setFabBackgroundColor(ThemeUtil.getColorAccent())
                    .setFabImageTintColor(getColor(R.color.colorWhite))
                    .setLabelBackgroundColor(ThemeUtil.getTextColorSecondary())
                    .setLabelColor(ThemeUtil.getColorBackground())
                    .create())
            sdAdd.setOnActionSelectedListener { speedDialActionItem ->
                addSnapshot(artifactId)
                false
            }
        } else {
            IntegrityCore.getTypeNames().map {
                // todo names from resources
                it.className.substringAfterLast(".").removeSuffix("TypeActivity")
            }.forEachIndexed {
                index, name -> sdAdd.addActionItem(SpeedDialActionItem
                    .Builder(index, android.R.drawable.ic_input_add)
                    .setLabel(name)
                    .setFabBackgroundColor(ThemeUtil.getColorAccent())
                    .setFabImageTintColor(getColor(R.color.colorWhite))
                    .setLabelBackgroundColor(ThemeUtil.getTextColorSecondary())
                    .setLabelColor(ThemeUtil.getColorBackground())
                    .create())
            }
            sdAdd.setOnActionSelectedListener { speedDialActionItem ->
                val typeName = IntegrityCore.getTypeNames().toList()[speedDialActionItem.id]
                IntegrityCore.openCreateNewArtifact(this, typeName)
                false
            }
        }
        FontUtil.setFont(this, sdAdd)
    }

    private fun bindFilter() {
        ivUnFilterArtifact.setOnClickListener { removeArtifactFilter() }
        llBottomSheet.setBackgroundColor(ThemeUtil.getColorBackgroundSecondary())
        svMain.background.setColorFilter(ThemeUtil.getColorBackgroundBleached(),
                PorterDuff.Mode.DARKEN)
    }

    private fun updateFilterView(artifactId: Long?) {
        if (artifactId != null) {
            tvFilteredArtifactTitle.text = IntegrityCore.metadataRepository
                    .getLatestSnapshotMetadata(artifactId).title
        }
        llFilteredArtifact.visibility = if (artifactId != null) View.VISIBLE else View.GONE
    }

    private fun bindSearch() {
        rvSearchResults.adapter = SearchResultRecyclerAdapter(ArrayList(), this)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as? SearchManager
            if (searchManager != null) {
                svMain.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            }

        fixMicIconBackground(svMain)
        svMain.queryTextChanges()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { onInputsUpdate(inputs.copy(searchText = it.trim().toString())) }
    }

    /**
     * Fixes default SearchView voice search icon background shape and visual artifacts.
     * todo improve and move
     */
    private fun fixMicIconBackground(svMain: SearchView) {
        // Rounded mic icon background that doesn't extend outsize the rounded search view
        val ivMicButton: ImageView? = svMain.findViewById(R.id.search_voice_btn)
        if (ivMicButton != null) {
            ivMicButton.setBackgroundResource(R.drawable.rounded_padded_clickable)
        }
        // Removing the thin gray line visual artifact on the mic icon background
        val llSubmitArea: View? = svMain.findViewById(R.id.submit_area)
        if (llSubmitArea != null) {
            llSubmitArea.setBackgroundColor(getColor(R.color.colorNone))
            llSubmitArea.setPadding(0, 0, 0, 0)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            onInputsUpdate(inputs.copy(searchText = query.trim()))
        }
    }

    private fun search(searchedText: String, filteredArtifactId: Long?) {
        svMain.setQuery(searchedText, false)
        if (searchedText.isBlank()) {
            toolbar.title = "Snapshots"
            toolbar.subtitle = when {
                filteredArtifactId != null -> "of ${IntegrityCore.metadataRepository
                        .getLatestSnapshotMetadata(filteredArtifactId).title}"
                else -> "Recent in all"
            }
        } else {
            toolbar.title = "Search"
            toolbar.subtitle = when {
                filteredArtifactId != null -> "in ${IntegrityCore.metadataRepository
                        .getLatestSnapshotMetadata(filteredArtifactId).title}"
                else -> "In all"
            }
        }
        if (searchedText.length >= 3) {
            val searchResults = SearchUtil.searchText(searchedText, filteredArtifactId)
            (rvSearchResults.adapter as SearchResultRecyclerAdapter).setItems(searchResults)
            tvNoResults.visibility = if (searchResults.isEmpty()) View.VISIBLE else View.GONE
        } else {
            tvNoResults.visibility = View.GONE
        }
        rvSnapshotList.visibility = if (searchedText.isBlank()) View.VISIBLE else View.GONE
        rvSearchResults.visibility = if (searchedText.isBlank()) View.GONE else View.VISIBLE
        sdAdd.visibility = if (searchedText.isBlank()) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val snapshot = IntentUtil.getSnapshot(data)
        if (snapshot != null) {
            IntegrityCore.saveSnapshot(this, snapshot)
        }
        if (IntentUtil.isRecreate(data)) {
            recreate()
        }
    }

    override fun onStart() {
        super.onStart()
        IntegrityCore.metadataRepository.addChangesListener(this) {
            refreshSnapshotList(inputs.filteredArtifactId)
        }
        IntegrityCore.subscribeToScheduledJobListing(this) {
            updateJobsInDrawer(it.map { getScheduledJobDrawerItem(it.first) },
                    2L, "Up next", "No scheduled jobs",
                    inputs.scheduledJobsExpanded)
        }
        IntegrityCore.subscribeToRunningJobListing(this) {
            updateJobsInDrawer(it.map { getRunningJobDrawerItem(it.first, it.second) },
                    1L, "Running now", "No running jobs",
                    inputs.runningJobsExpanded)
        }
        updateErrorViewsOnDrawer(IntegrityCore.logRepository.getUnreadErrors().count())
    }

    override fun onStop() {
        IntegrityCore.metadataRepository.removeChangesListener(this)
        IntegrityCore.unsubscribeFromScheduledJobListing(this)
        IntegrityCore.unsubscribeFromRunningJobListing(this)
        super.onStop()
    }

    fun askRemoveArtifact(artifactId: Long) {
        MaterialDialog(this)
                .title(text = "Delete artifact?\nData archives will not be affected.")
                .positiveButton(text = "Delete") {
                    IntegrityCore.removeArtifact(artifactId, false)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    private fun askRemoveAll() {
        MaterialDialog(this)
                .title(text = "Delete all artifacts?")
                .positiveButton(text = "Delete") {
                    IntegrityCore.removeAll(false)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    fun addSnapshot(artifactId: Long) = IntegrityCore.openCreateNewSnapshot(this, artifactId)

    private fun refreshSnapshotList(artifactId: Long?) {
        val snapshots = when (artifactId) {
            null -> IntegrityCore.metadataRepository.getAllArtifactLatestMetadata(true)
            else -> IntegrityCore.metadataRepository.getArtifactMetadata(artifactId)
        }.snapshots
        (rvSnapshotList.adapter as SnapshotRecyclerAdapter)
                .setItems(snapshots.map { Pair(it, getSnapshotCount(it.artifactId)) }.toList(),
                        inputs.filteredArtifactId == null)
    }

    private fun getSnapshotCount(artifactId: Long) = IntegrityCore.metadataRepository
            .getArtifactMetadata(artifactId).snapshots.count()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_select_view -> {
            // todo show list view selection (for snapshots or search results)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun viewLog() {
        startActivity(Intent(this, LogViewActivity::class.java))
    }

    private fun viewFolderLocations() {
        startActivity(Intent(this, FolderLocationsActivity::class.java))
    }

    private fun viewTags() {
        startActivity(Intent(this, TagsActivity::class.java))
    }

    private fun viewSettings(viewExtensions: Boolean = false) {
        startActivityForResult(IntentUtil.putViewExtensions(
                Intent(this, SettingsActivity::class.java), viewExtensions), 0)
    }

    fun filterArtifact(artifactId: Long?) {
        onInputsUpdate(inputs.copy(filteredArtifactId = artifactId))
    }

    private fun removeArtifactFilter() = filterArtifact(null)

    fun viewSnapshot(snapshot: Snapshot) {
        IntegrityCore.openViewSnapshotOrShowProgress(this, snapshot.artifactId, snapshot.date)
    }
}
