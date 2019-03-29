/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.main

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.alexvt.integrity.R
import com.alexvt.integrity.lib.metadata.Snapshot
import io.reactivex.android.schedulers.AndroidSchedulers

import kotlinx.android.synthetic.main.activity_main.*
import android.view.*
import androidx.core.view.LayoutInflaterCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.expandable.expandableItem
import com.alexvt.integrity.databinding.DrawerHeaderBinding
import com.alexvt.integrity.ui.util.SpeedDialUtil
import com.mikepenz.iconics.context.IconicsLayoutInflater2
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.holder.BadgeStyle
import co.zsmb.materialdrawerkt.draweritems.badge
import co.zsmb.materialdrawerkt.draweritems.switchable.switchItem
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.alexvt.integrity.core.search.SortingUtil
import com.alexvt.integrity.lib.util.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.materialdrawer.model.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import dagger.android.AndroidInjection
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : ThemedActivity() {

    @Inject
    lateinit var vmFactory: ViewModelProvider.Factory

    private val vm by lazy {
        ViewModelProviders.of(this, vmFactory)[MainScreenViewModel::class.java]
    }

    private lateinit var drawer: Drawer
    private lateinit var jobProgressDialog: MaterialDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        LayoutInflaterCompat.setFactory2(layoutInflater, IconicsLayoutInflater2(delegate))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindVisualTheme()
        bindToolbar()
        bindDrawer()
        bindSnapshotList()
        bindSearchResults()
        bindFloatingButton()
        bindBottomSheetControls()
        bindJobProgressDialog()
        bindNavigation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // todo fix and remove
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = vm.computeColorPrimaryDark()
    }


    // binding to ViewModel

    private fun bindVisualTheme() {
        FontUtil.setFont(this, vm.getFont())
    }

    private fun bindToolbar() {
        setSupportActionBar(toolbar)

        vm.inputStateData.observe(this, androidx.lifecycle.Observer {
            toolbar.title = vm.computeScreenTitle()
            toolbar.subtitle = vm.computeScreenSubTitle()
        })
        vm.settingsData.observe(this, androidx.lifecycle.Observer {
            toolbar.title = vm.computeScreenTitle()
            toolbar.subtitle = vm.computeScreenSubTitle()
        })
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
                    tryToggleExpandSection(isScheduledJobs = false)
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_playlist_play
                iconTintingEnabled = true
                textColor = vm.computeTextColorPrimary().toLong()
                iconColor = vm.computeTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity, vm.getFont())
            } // updatable
            expandableItem("Up next") {
                identifier = 2
                selectable = false
                onClick { _ ->
                    tryToggleExpandSection(isScheduledJobs = true)
                    false
                }
                iicon = CommunityMaterial.Icon.cmd_calendar_clock
                iconTintingEnabled = true
                textColor = vm.computeTextColorPrimary().toLong()
                iconColor = vm.computeTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity, vm.getFont())
            } // updatable
            divider { identifier = 3 }
            primaryItem("Archives & Storage") {
                identifier = 4
                selectable = false
                onClick { _ ->
                    vm.viewArchiveLocations() // todo also show data cache folder in there
                    false
                }
                iicon = CommunityMaterial.Icon.cmd_archive
                iconTintingEnabled = true
                textColor = vm.computeTextColorPrimary().toLong()
                iconColor = vm.computeTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity, vm.getFont())
            }
            primaryItem("Tags") {
                identifier = 5
                selectable = false
                onClick { _ ->
                    vm.viewTags()
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_tag_multiple
                iconTintingEnabled = true
                textColor = vm.computeTextColorPrimary().toLong()
                iconColor = vm.computeTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity, vm.getFont())
            }
            primaryItem("Log") {
                identifier = 6
            } // updatable
            divider { identifier = 7 }
            primaryItem("Extensions") {
                selectable = false
                onClick { _ ->
                    vm.viewExtensions()
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_puzzle
                iconTintingEnabled = true
                textColor = vm.computeTextColorPrimary().toLong()
                iconColor = vm.computeTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity, vm.getFont())
            }
            divider { identifier = 8 }
            primaryItem("Restore...") {
                identifier = 9
                selectable = false
                onClick { _ ->
                    vm.viewRestore()
                    false
                }
                iicon = CommunityMaterial.Icon2.cmd_history
                iconTintingEnabled = true
                textColor = vm.computeTextColorPrimary().toLong()
                iconColor = vm.computeTextColorSecondary().toLong()
                typeface = FontUtil.getTypeface(this@MainActivity, vm.getFont())
            }
            footer {
                switchItem("Scheduled jobs") {
                    // updatable
                    typeface = FontUtil.getTypeface(this@MainActivity, vm.getFont())
                }
                divider {}
                primaryItem("Settings") {
                    selectable = false
                    onClick { _ ->
                        vm.viewSettings()
                        false
                    }
                    textColor = vm.computeTextColorPrimary().toLong()
                }
                primaryItem("Help") {
                    selectable = false
                    onClick { _ ->
                        vm.viewHelp()
                        false
                    }
                    badge("version ${vm.versionName}") {
                        textColor = vm.computeTextColorSecondary().toLong()
                        colorRes = R.color.colorNone
                    }
                    textColor = vm.computeTextColorPrimary().toLong()
                }
                divider {}
                primaryItem("Legal") {
                    selectable = false
                    onClick { _ ->
                        vm.viewLegal()
                        false
                    }
                    textColor = vm.computeTextColorPrimary().toLong()
                }
            }
            actionBarDrawerToggleAnimated = true
            actionBarDrawerToggleEnabled = true
        }
        drawer.recyclerView.scrollBarDefaultDelayBeforeFade = 3000 // todo don't show when updating but keeping size
        drawer.slider.setBackgroundColor(vm.computeColorBackground())
        drawer.stickyFooter.setBackgroundColor(vm.computeColorBackgroundSecondary())
        ThemeUtil.applyToView(drawer.slider)
        ThemeUtil.applyToView(drawer.stickyFooter)

        vm.logErrorCountData.observe(this, androidx.lifecycle.Observer {
            updateDrawerStatusHeader()
            updateLogBadgeErrorCount()
        })
        vm.settingsData.observe(this, androidx.lifecycle.Observer {
            updateDrawerStatusHeader()
            updateRunningJobsInDrawer()
            updateScheduledJobsInDrawer()
            updateScheduledJobsSwitch()
        })
        vm.runningJobSnapshotsData.observe(this, androidx.lifecycle.Observer {
            updateRunningJobsInDrawer()
        })
        vm.scheduledJobSnapshotsData.observe(this, androidx.lifecycle.Observer {
            updateScheduledJobsInDrawer()
        })
    }

    private fun updateRunningJobsInDrawer() {
        updateJobsInDrawer(vm.runningJobSnapshotsData.value!!.map {
            getRunningJobDrawerItem(it)
        }, 1L, "Running now", "No running jobs")
    }

    private fun updateScheduledJobsInDrawer() {
        updateJobsInDrawer(vm.scheduledJobSnapshotsData.value!!.map {
            getScheduledJobDrawerItem(it.first, it.second)
        }, 2L, "Up next", "No scheduled jobs")
    }

    private fun bindSnapshotList() {
        val snapshotAdapter = GroupAdapter<ViewHolder>()
        val snapshotsSection = Section()
        snapshotAdapter.add(snapshotsSection)
        rvSnapshotList.adapter = snapshotAdapter
        vm.snapshotsData.observe(this, androidx.lifecycle.Observer {
            it.map { SnapshotListItem(
                    snapshot = it.first,
                    relatedSnapshotCount = it.second,
                    context = this,
                    showMoreButton = vm.inputStateData.value!!.filteredArtifactId == null,
                    settingsRepository = vm.settingsRepository,
                    dataFolderManager = vm.dataFolderManager,
                    onClickListener = { artifactId, date ->
                        vm.viewSnapshot(artifactId, date)
                    },
                    onLongClickListener = { artifactId, date, many ->
                        if (many) askRemoveArtifact(artifactId) else askRemoveSnapshot(artifactId, date)
                    },
                    onClickMoreListener = { artifactId, _, many ->
                        if (many) vm.viewMoreOfArtifact(artifactId) else vm.addSnapshot(artifactId)
                    }
            ) }.let { snapshotsSection.update(it) }
        })

        vm.inputStateData.observe(this, androidx.lifecycle.Observer {
            rvSnapshotList.visibility = if (vm.isSearching()) View.GONE else View.VISIBLE
        })
    }

    private fun bindSearchResults() {
        val searchResultAdapter = GroupAdapter<ViewHolder>()
        rvSearchResults.adapter = searchResultAdapter

        val searchSnapshotSection = Section()
        searchResultAdapter.add(searchSnapshotSection)
        vm.snapshotSearchResultsData.observe(this, androidx.lifecycle.Observer {
            it.map { SnapshotListItem(
                    snapshot = it.snapshot,
                    titleHighlightRange = it.titleHighlightRange,
                    relatedSnapshotCount = 1,
                    context = this,
                    showMoreButton = false,
                    settingsRepository = vm.settingsRepository,
                    dataFolderManager = vm.dataFolderManager,
                    onClickListener = { artifactId, date ->
                        vm.viewSnapshot(artifactId, date)
                    }
            ) }.let { searchSnapshotSection.update(it) }
            updateNoResultsPlaceholder()
        })

        val searchTextSection = Section()
        searchResultAdapter.add(searchTextSection)
        vm.textSearchResultsData.observe(this, androidx.lifecycle.Observer {
            it.map { TextSearchResultListItem(
                        textSearchResult = it,
                        context = this,
                        onLinkClickListener = { ViewExternalUtil.viewLinkExternal(this, it) }
            ) }.let { searchTextSection.update(it) }
            updateNoResultsPlaceholder()
        })

        vm.inputStateData.observe(this, androidx.lifecycle.Observer {
            updateNoResultsPlaceholder()
            rvSearchResults.visibility = if (vm.isSearching()) View.VISIBLE else View.GONE
        })
    }

    private fun updateNoResultsPlaceholder() {
        val searchResultsExist = vm.textSearchResultsData.value!!.isNotEmpty()
                || vm.snapshotSearchResultsData.value!!.isNotEmpty()
        tvNoResults.visibility = if (vm.isSearching() && !searchResultsExist) View.VISIBLE else View.GONE
    }

    private fun bindFloatingButton() {
        SpeedDialUtil.setStayOnExpand(sdAdd)
        SpeedDialUtil.setIcons(this, sdAdd, CommunityMaterial.Icon2.cmd_plus,
                CommunityMaterial.Icon.cmd_close)

        vm.inputStateData.observe(this, androidx.lifecycle.Observer {
            sdAdd.visibility = if (vm.isSearching()) View.GONE else View.VISIBLE
            // lazy updating for FloatingSubButtons
            val floatingButtonTag = if (it.filteredArtifactId == null) "Types" else "Type"
            if (floatingButtonTag != sdAdd.tag) {
                sdAdd.tag = floatingButtonTag
                updateFloatingSubButtons()
            }
        })
        vm.typeNameData.observe(this, androidx.lifecycle.Observer {
            updateFloatingSubButtons()
        })
    }

    private fun updateFloatingSubButtons() {
        sdAdd.clearActionItems()
        val isFilteringArtifactId = vm.inputStateData.value!!.filteredArtifactId != null
        if (isFilteringArtifactId) {
            sdAdd.addActionItem(SpeedDialUtil.getActionItem(this, 0,
                    CommunityMaterial.Icon2.cmd_plus, "Create another snapshot", vm.getThemeColors()))
            sdAdd.setOnActionSelectedListener {
                vm.clickFloatingButtonOption(0)
                false
            }
        } else {
            vm.typeNameData.value!!.forEachIndexed {
                index, name -> sdAdd.addActionItem(SpeedDialUtil.getActionItem(this, index,
                    CommunityMaterial.Icon2.cmd_plus, "$name Type", vm.getThemeColors()))
            }
            sdAdd.setOnActionSelectedListener {
                vm.clickFloatingButtonOption(it.id)
                false
            }
        }
        FontUtil.setFont(this, sdAdd, vm.getFont())
    }

    private fun bindBottomSheetControls() {
        llBottomSheet.setBackgroundColor(vm.computeColorBackgroundSecondary())

        // search
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as? SearchManager
        if (searchManager != null) {
            svMain.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }
        SearchViewUtil.fixMicIconBackground(svMain)
        svMain.background.setColorFilter(vm.computeColorBackgroundBleached(), PorterDuff.Mode.DARKEN)
        svMain.queryTextChanges()
                .debounce(20, TimeUnit.MILLISECONDS) // preventing loop between vm and view
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    vm.setSearchText(it.toString())
                }
        vm.settingsData.observe(this, androidx.lifecycle.Observer {
            val sortingMethod = it.sortingMethod
            val sortingTypeIcon: IIcon = when {
                SortingUtil.isByDate(sortingMethod) -> CommunityMaterial.Icon.cmd_folder_clock_outline
                SortingUtil.isByTitle(sortingMethod) -> CommunityMaterial.Icon2.cmd_sort_alphabetical
                else -> CommunityMaterial.Icon2.cmd_shuffle
            }
            iivSortingType.icon = IconicsDrawable(this).icon(sortingTypeIcon)
                    .colorRes(R.color.colorWhite)

            val sortingDirectionIcon: IIcon = when {
                SortingUtil.isDescending(sortingMethod) -> CommunityMaterial.Icon2.cmd_sort_descending
                SortingUtil.isAscending(sortingMethod) -> CommunityMaterial.Icon2.cmd_sort_ascending
                else -> CommunityMaterial.Icon2.cmd_refresh
            }
            iivSortingDirection.icon = IconicsDrawable(this).icon(sortingDirectionIcon)
                    .colorRes(R.color.colorWhite)
        })
        vm.inputStateData.observe(this, androidx.lifecycle.Observer {
            val filterArtifact = it.filteredArtifactId != null

            vm.computeArtifactFilterTitle { tvFilteredArtifactTitle.text }
            llSorting.visibility = if (filterArtifact || vm.isSearching()) View.VISIBLE else View.GONE
            llFilteredArtifact.visibility = if (filterArtifact) View.VISIBLE else View.GONE

            svMain.setQuery(it.searchViewText, false)
        })

        // artifact filtering
        iivUnFilterArtifact.setOnClickListener { vm.clickArtifactFilteringClose() }

        // sorting
        iivSortingType.setOnClickListener {
            MaterialDialog(this).show {
                title(text = "Sorting method")
                listItemsSingleChoice(
                        items = vm.getSortingTypeNames(),
                        initialSelection = vm.getSortingTypeNameIndex()
                ) { _, index, _ ->
                    vm.selectSortingTypeOption(index)
                }
            }
        }
        iivSortingDirection.setOnClickListener {
            vm.clickSortingDirectionButton()
        }
    }

    private fun tryToggleExpandSection(isScheduledJobs: Boolean) {
        val sectionId = if (isScheduledJobs) 2L else 1L
        val isExpandable = drawer.getDrawerItem(sectionId).subItems.isNotEmpty()
        if (!isExpandable) {
            return
        }
        val isExpanded = drawer.getDrawerItem(sectionId).isExpanded

        if (isScheduledJobs) {
            vm.expandScheduledJobsHeader(isExpanded)
        } else {
            vm.expandRunningJobsHeader(isExpanded)
        }
    }

    private fun updateDrawerStatusHeader() {
        val headerBinding: DrawerHeaderBinding = DataBindingUtil.inflate(LayoutInflater.from(
                this), R.layout.drawer_header, null, false)
        FontUtil.setFont(this, headerBinding.rlView, vm.getFont())
        val unreadErrorCount = vm.logErrorCountData.value
        headerBinding.tvTitle.text = if (unreadErrorCount == 0) {
            if (vm.settingsData.value!!.jobsEnableScheduled) {
                "App is working normally"
            } else {
                "Scheduled jobs are disabled"
            }
        } else {
            "There are errors."
        }
        headerBinding.bViewLog.visibility = if (unreadErrorCount == 0) View.GONE else View.VISIBLE
        headerBinding.bViewLog.setOnClickListener {
            vm.viewLog()
        }
        drawer.header = headerBinding.rlView
        headerBinding.rlView.setBackgroundColor(vm.computeColorBackgroundSecondary())
        headerBinding.tvTitle.setTextColor(vm.computeTextColorPrimary())
        headerBinding.bViewLog.setTextColor(vm.computeTextColorSecondary())
    }

    private fun updateLogBadgeErrorCount() {
        val unreadErrorCount = vm.logErrorCountData.value
        val badgeText = if (unreadErrorCount == 0) "" else "Errors: $unreadErrorCount"
        val badgeColor = if (unreadErrorCount == 0) {
            vm.computeColorBackground()
        } else {
            getColor(R.color.colorError)
        }
        drawer.updateItem(PrimaryDrawerItem()
                .withIdentifier(6)
                .withName("Log")
                .withSelectable(false)
                .withOnDrawerItemClickListener { _, _, _ ->
                    vm.viewLog()
                    false
                }
                .withTextColor(vm.computeTextColorPrimary())
                .withIconColor(vm.computeTextColorSecondary())
                .withIcon(CommunityMaterial.Icon2.cmd_text)
                .withIconTintingEnabled(true)
                .withTypeface(FontUtil.getTypeface(this, vm.getFont()))
                .withBadge(badgeText)
                .withBadgeStyle(BadgeStyle()
                        .withColor(badgeColor)
                        .withTextColorRes(R.color.colorWhite)
                        .withPaddingLeftRightDp(8)
                        .withCornersDp(12)
                ))
    }

    private fun updateScheduledJobsSwitch() {
        drawer.updateStickyFooterItemAtPosition(SwitchDrawerItem()
                .withName("Scheduled jobs")
                .withSelectable(false)
                .withChecked(vm.settingsData.value!!.jobsEnableScheduled)
                .withOnCheckedChangeListener { _, _, isChecked ->
                    vm.setScheduledJobsEnabled(isChecked)
                }
                .withTextColor(vm.computeTextColorPrimary()),
                0)
        FontUtil.setFont(this, drawer.stickyFooter, vm.getFont())
    }

    /**
     * Updates the jobs header and job list.
     */
    private fun updateJobsInDrawer(jobListItems: List<SecondaryDrawerItem>,
                                   sectionId: Long, title: String, titlePlaceholder: String) {
        val sectionPosition = drawer.getPosition(sectionId)

        val isExpanded = if (sectionId == 1L) {
            vm.settingsData.value!!.jobsExpandRunning
        } else {
            vm.settingsData.value!!.jobsExpandScheduled
        }

        val countSuffix = if (jobListItems.isEmpty()) "" else "(${jobListItems.size})"
        val visibleTitle = if (jobListItems.isEmpty()) titlePlaceholder else "$title $countSuffix"
        val arrowColor = if (jobListItems.isEmpty()) {
            vm.computeColorBackground()
        } else {
            vm.computeColorAccent()
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

    private fun getScheduledJobDrawerItem(snapshot: Snapshot,
                                          timeRemainingMillis: Long): SecondaryDrawerItem {
        val timeText = if (timeRemainingMillis <= 0) {
            "Should start now"
        } else {
            "Starting in ${timeRemainingMillis / 1000} s"
        }
        return SecondaryDrawerItem()
                .withName(snapshot.title)
                .withDescription(timeText)
                .withIdentifier(snapshot.artifactId)
                .withLevel(2)
                .withSelectable(false)
                .withTextColor(vm.computeTextColorSecondary())
                .withDescriptionTextColor(vm.computeTextColorSecondary())
                .withTypeface(FontUtil.getTypeface(this, vm.getFont()))
                .withOnDrawerItemClickListener { _, _, _ ->
                    vm.editSnapshot(snapshot.artifactId)
                    false
                }
    }

    private fun getRunningJobDrawerItem(snapshot: Snapshot): SecondaryDrawerItem {
        return SecondaryDrawerItem()
                .withName(snapshot.title)
                .withDescription("Running")
                .withIdentifier(snapshot.artifactId - 1_000_000_000L) // todo distinguish better
                .withLevel(2)
                .withSelectable(false)
                .withTypeface(FontUtil.getTypeface(this, vm.getFont()))
                .withOnDrawerItemClickListener { _, _, _ ->
                    vm.viewRunningJob(snapshot.artifactId, snapshot.date)
                    false
                }
    }

    private fun bindNavigation() {
        vm.navigationEventData.observe(this, androidx.lifecycle.Observer {
            if (it.goBack) {
                super.onBackPressed()
            } else if (it.recreate) {
                recreate()
            } else with (it) {
                drawer.closeDrawer()
                val intent = Intent()
                intent.component = ComponentName(targetPackage, targetClass)
                IntentUtil.putArtifactId(intent, bundledArtifactId)
                IntentUtil.putDate(intent, bundledDate)
                IntentUtil.putSnapshot(intent, bundledSnapshot)
                IntentUtil.putDates(intent, bundledDates)
                IntentUtil.putDataFolderName(intent, bundledDataFolderName)
                IntentUtil.putFontName(intent, bundledFontName)
                IntentUtil.putColorBackground(intent, bundledColorBackground)
                IntentUtil.putColorPrimary(intent, bundledColorPrimary)
                IntentUtil.putColorAccent(intent, bundledColorAccent)
                IntentUtil.putViewExtensions(intent, bundledOptionExtensions)
                this@MainActivity.startActivityForResult(intent, 0)
            }
        })
    }

    private fun bindJobProgressDialog() {
        vm.inputStateData.observe(this, androidx.lifecycle.Observer {
            if (it.jobProgressArtifactId != null && it.jobProgressDate != null) {
                if (!::jobProgressDialog.isInitialized || !jobProgressDialog.isShowing) {
                    jobProgressDialog = MaterialDialog(this)
                            .title(text = "Creating snapshot of ${it.jobProgressTitle}")
                            .message(text = "")
                            .cancelable(false)
                            .positiveButton(text = "In background") {
                                it.cancel()
                            }.negativeButton(text = "Stop") {
                                vm.cancelSnapshotCreation()
                                it.cancel()
                            }
                    jobProgressDialog.show()
                }
                jobProgressDialog.message(text = it.jobProgressMessage)
            } else {
                if (::jobProgressDialog.isInitialized && jobProgressDialog.isShowing) {
                    jobProgressDialog.cancel()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            vm.setSearchText(query.trim())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val snapshot = IntentUtil.getSnapshot(data)
        if (snapshot != null) {
            vm.snapshotReturned(snapshot)
        }
    }

    private fun askRemoveArtifact(artifactId: Long) {
        MaterialDialog(this)
                .title(text = "Delete artifact?\nData archives will not be affected.")
                .positiveButton(text = "Delete") {
                    vm.removeArtifact(artifactId)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

    private fun askRemoveSnapshot(artifactId: Long, date: String) {
        MaterialDialog(this)
                .title(text = "Delete snapshot?\nData archive will not be affected.")
                .positiveButton(text = "Delete") {
                    vm.removeSnapshot(artifactId, date)
                }
                .negativeButton(text = "Cancel")
                .show()
    }

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

    override fun onBackPressed() = vm.pressBackButton()
}
