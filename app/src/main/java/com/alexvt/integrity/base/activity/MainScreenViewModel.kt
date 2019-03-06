/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.base.activity

import android.content.ComponentName
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.BuildConfig
import com.alexvt.integrity.core.IntegrityCore
import com.alexvt.integrity.core.job.RunningJobManager
import com.alexvt.integrity.core.job.ScheduledJobManager
import com.alexvt.integrity.core.search.SearchResult
import com.alexvt.integrity.core.search.SearchUtil
import com.alexvt.integrity.core.search.SortingUtil
import com.alexvt.integrity.core.settings.IntegrityAppSettings
import com.alexvt.integrity.core.settings.SortingMethod
import com.alexvt.integrity.core.util.ThrottledFunction
import com.alexvt.integrity.core.util.ThemeColors
import com.alexvt.integrity.core.util.ThemeUtil
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.SnapshotStatus
import com.alexvt.integrity.util.SingleLiveEvent


class MainScreenViewModelFactory(
        val packageName: String,
        val folderLocationsScreenClass: String,
        val tagsScreenClass: String,
        val logScreenClass: String,
        val settingsClass: String,
        val recoveryScreenClass: String,
        val helpInfoScreenClass: String,
        val legalInfoScreenClass: String
) : ViewModelProvider.Factory {
    // Pass type parameter to instance if needed for initial state
    override fun <T : ViewModel> create(modelClass: Class<T>)
            = MainScreenViewModel(packageName, folderLocationsScreenClass, tagsScreenClass,
            logScreenClass, settingsClass, recoveryScreenClass, helpInfoScreenClass,
            legalInfoScreenClass) as T
}

/**
 * The minimal set of data about screen state that comes from user input,
 * but cannot be obtained from repositories,
 * and together with data from repositories can be rendered in UI.
 */
data class MainScreenInputState(
        // bottom sheet controls
        val searchViewText: String,
        val filteredArtifactId: Long?,

        // dialogs view model
        val runningDownloadViewArtifactId: Long?,
        val viewingSortingTypeOptions: Boolean,
        val jobProgressTitle: String,
        val jobProgressMessage: String,
        val jobProgressArtifactId: Long?,
        val jobProgressDate: String?
)

data class NavigationEvent(
        val targetPackage: String,
        val targetClass: String,
        val goBack: Boolean = false,

        // bundled data to attack when goBack is false
        val bundledArtifactId: Long? = null,
        val bundledDate: String? = null,
        val bundledSnapshot: Snapshot? = null,
        val bundledDates: List<String>? = null,
        val bundledDataFolderName: String? = null,
        val bundledFontName: String? = null,
        val bundledColorBackground: String? = null,
        val bundledColorPrimary: String? = null,
        val bundledColorAccent: String? = null,
        val bundledOptionExtensions: Boolean? = null
)

class MainScreenViewModel(
        // for navigation
        private val packageName: String,
        private val folderLocationsScreenClass: String,
        private val tagsScreenClass: String,
        private val logScreenClass: String,
        private val settingsScreenClass: String,
        private val recoveryScreenClass: String,
        private val helpInfoScreenClass: String,
        private val legalInfoScreenClass: String
        ) : ViewModel() {

    // primary
    val inputStateData = MutableLiveData<MainScreenInputState>()
    val settingsData = MutableLiveData<IntegrityAppSettings>()
    val runningJobIdsData = MutableLiveData<List<Snapshot>>()
    val scheduledJobIdsData = MutableLiveData<List<Pair<Snapshot, Long>>>()
    val logErrorCountData = MutableLiveData<Int>()
    val versionNameData = MutableLiveData<String>()
    val typeComponentNameData = MutableLiveData<List<ComponentName>>()

    // depends on primary
    val searchResultsData = MutableLiveData<List<SearchResult>>()
    val snapshotsData = MutableLiveData<List<Pair<Snapshot, Int>>>()

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()

    init {
        // input state  starts as default
        inputStateData.value = MainScreenInputState(searchViewText = "", filteredArtifactId = null,
                runningDownloadViewArtifactId = null, viewingSortingTypeOptions = false,
                jobProgressArtifactId = null, jobProgressDate = null,
                jobProgressTitle = "", jobProgressMessage = "")

        // settings, jobs, log error count  are listened to in their repositories
        settingsData.value = IntegrityCore.settingsRepository.get()
        IntegrityCore.settingsRepository.addChangesListener(this.toString()) {
            settingsData.value = it
            updateContentData() // snapshots, search results  depend on  settings
        }
        runningJobIdsData.value = emptyList()
        RunningJobManager.addJobListListener(this.toString()) {
            runningJobIdsData.value = it.map {
                IntegrityCore.metadataRepository.getSnapshotMetadata(it.first, it.second)
            }
        }
        scheduledJobIdsData.value = emptyList()
        ScheduledJobManager.addScheduledJobsListener(this.toString()) {
            scheduledJobIdsData.value = it.map {
                IntegrityCore.metadataRepository.getSnapshotMetadata(it.first, it.second)
            }.map {
                it to IntegrityCore.getNextJobRunTimestamp(it) - System.currentTimeMillis()
            }
        }
        logErrorCountData.value = IntegrityCore.logRepository.getUnreadErrors().count()
        IntegrityCore.logRepository.addChangesListener(this.toString()) {
            logErrorCountData.value = IntegrityCore.logRepository.getUnreadErrors().count()
        }

        // version name, snapshot type component names  are static
        versionNameData.value = BuildConfig.VERSION_NAME
        typeComponentNameData.value = IntegrityCore.getTypeNames()  // todo listen to changes

        // snapshots, search results initial values
        snapshotsData.value = fetchSnapshots()
        searchResultsData.value = fetchSearchResults()
    }

    private fun updateInputState(inputState: MainScreenInputState) {
        inputStateData.value = inputState
        updateContentData() // snapshots, search results  depend on  inputStateData
    }


    private val updateContentCoolingOffMillis = 500L
    private val contentDataWithCoolingOff = ThrottledFunction(updateContentCoolingOffMillis) {
        snapshotsData.value = fetchSnapshots()
        searchResultsData.value = fetchSearchResults()
    }

    private fun updateContentData() {
        val isFasterMethod = settingsData.value!!.fasterSearchInputs
        with (contentDataWithCoolingOff) {
            if (isFasterMethod) requestThrottledLatest() else requestDebounced()
        }
    }

    private fun fetchSearchResults(): List<SearchResult> {
        val searchText = inputStateData.value!!.searchViewText
        val filteredArtifactId = inputStateData.value!!.filteredArtifactId
        val sortingMethod = settingsData.value!!.sortingMethod
        return SortingUtil.sortSearchResults(
                SearchUtil.searchText(searchText, filteredArtifactId), sortingMethod)
    }

    private fun fetchSnapshots(): List<Pair<Snapshot, Int>> {
        val filteredArtifactId = inputStateData.value!!.filteredArtifactId
        val snapshots = SortingUtil.sortSnapshots(when (filteredArtifactId) {
            null -> IntegrityCore.metadataRepository.getAllArtifactLatestMetadata(true)
            else -> IntegrityCore.metadataRepository.getArtifactMetadata(filteredArtifactId)
        }.snapshots, getSortingMethod())
        return snapshots.map { Pair(it, getSnapshotCount(it.artifactId)) }.toList()
    }

    private fun getSortingMethod(): String {
        val isSearching = inputStateData.value!!.searchViewText.isNotBlank()
        val isArtifactFiltered = inputStateData.value!!.filteredArtifactId != null
        return if (isSearching || isArtifactFiltered) {
            settingsData.value!!.sortingMethod
        } else {
            SortingMethod.NEW_FIRST
        }
    }

    private fun getSnapshotCount(artifactId: Long) = IntegrityCore.metadataRepository
            .getArtifactMetadata(artifactId).snapshots.count()





    fun computeScreenTitle(): String {
        val isSearching = inputStateData.value!!.searchViewText.isNotBlank()

        return if (isSearching) "Search" else "Snapshots"
    }

    fun computeScreenSubTitle(): String {
        val isSearching = inputStateData.value!!.searchViewText.isNotBlank()
        val isArtifactFiltered = inputStateData.value!!.filteredArtifactId != null

        val latestSnapshotTitle = snapshotsData.value!!.firstOrNull()?.first?.title ?: ""

        val scopeTitle = if (isSearching) when {
            isArtifactFiltered -> "in $latestSnapshotTitle"
            else -> "In all"
        } else when {
            isArtifactFiltered -> "of $latestSnapshotTitle"
            else -> "Recent in all"
        }
        val sortingTitleSuffix = if (isSearching || isArtifactFiltered) {
            "  · " + SortingUtil.getSortingMethodNameMap()[settingsData.value!!.sortingMethod]
        } else {
            "" // sorting not applied for default view
        }
        return "$scopeTitle$sortingTitleSuffix"
    }

    fun getFont() = settingsData.value!!.textFont

    fun computeColorPrimaryDark() = ThemeUtil.getColorPrimaryDark(getThemeColors())

    fun computeTextColorPrimary() = ThemeUtil.getTextColorPrimary(getThemeColors())

    fun computeTextColorSecondary() = ThemeUtil.getTextColorSecondary(getThemeColors())

    fun computeColorAccent() = ThemeUtil.getColorAccent(getThemeColors())

    fun computeColorBackground() = ThemeUtil.getColorBackground(getThemeColors())

    fun computeColorBackgroundSecondary() = ThemeUtil.getColorBackgroundSecondary(getThemeColors())

    fun computeColorBackgroundBleached() = ThemeUtil.getColorBackgroundBleached(getThemeColors())

    fun computeArtifactFilterTitle(): String {
        val filteredArtifactId = inputStateData.value!!.filteredArtifactId
        val title = if (filteredArtifactId != null) {
            IntegrityCore.metadataRepository.getLatestSnapshotMetadata(filteredArtifactId).title
        } else {
            ""
        }
        val isSearching = inputStateData.value!!.searchViewText.isNotBlank()
        val prefix = if (isSearching) "in: " else ""
        return "$prefix$title"
    }

    fun getThemeColors() = with(settingsData.value!!) {
        ThemeColors(colorBackground, colorPrimary, colorAccent)
    }

    fun getSortingTypeNames() = SortingUtil.getSortingTypeNames()

    fun getSortingTypeNameIndex() = SortingUtil
            .getSortingTypeNameIndex(settingsData.value!!.sortingMethod)


    // content user actions

    fun viewSnapshot(artifactId: Long, date: String) {
        viewRunningJobOrOpenSnapshot(artifactId, date)
    }

    private fun viewRunningJobOrOpenSnapshot(artifactId: Long, date: String) {
        val snapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(artifactId, date)
        if (snapshot.status == SnapshotStatus.IN_PROGRESS) {
            viewRunningJobDialog(artifactId, date)
            return
        }
        // todo ensure snapshot data presence in folder first
        val activityInfo = IntegrityCore.getDataTypeActivityInfo(snapshot.dataTypeClassName)
        navigationEventData.value = NavigationEvent(
                targetPackage = activityInfo.packageName,
                targetClass = activityInfo.name,
                bundledSnapshot = snapshot,
                bundledDates = IntegrityCore.getCompleteSnapshotDatesOrNull(artifactId),
                bundledFontName = IntegrityCore.getFont(),
                bundledColorBackground = IntegrityCore.getColorBackground(),
                bundledColorPrimary = IntegrityCore.getColorPrimary(),
                bundledColorAccent = IntegrityCore.getColorAccent(),
                bundledDataFolderName = IntegrityCore.getDataFolderName()
                )
    }

    private fun viewRunningJobDialog(artifactId: Long, date: String) {
        val snapshot = IntegrityCore.metadataRepository.getSnapshotMetadata(artifactId, date)
        if (snapshot.status == SnapshotStatus.IN_PROGRESS) {
            updateInputState(inputStateData.value!!.copy(jobProgressArtifactId = artifactId,
                    jobProgressDate = date, jobProgressTitle = snapshot.title))
            IntegrityCore.subscribeToJobProgress(snapshot.artifactId, snapshot.date) {
                if (it.result != null) {
                    hideRunningJobDialog() // done
                } else if (it.progressMessage != null) {
                    // continuing
                    updateInputState(inputStateData.value!!.copy(
                            jobProgressMessage = it.progressMessage!!))
                }
            }
        }
    }

    private fun hideRunningJobDialog() {
        updateInputState(inputStateData.value!!.copy(jobProgressArtifactId = null,
                jobProgressDate = null, jobProgressTitle = "", jobProgressMessage = ""))
    }

    fun viewMoreOfArtifact(artifactId: Long) {
        updateInputState(inputStateData.value!!.copy(filteredArtifactId = artifactId))
    }

    fun setSearchText(searchText: String) {
        updateInputState(inputStateData.value!!.copy(searchViewText = searchText.trim()))
    }

    fun clickArtifactFilteringClose() {
        updateInputState(inputStateData.value!!.copy(filteredArtifactId = null))
    }

    fun selectSortingTypeOption(index: Int) {
        val newSortingMethod = SortingUtil.changeSortingType(settingsData.value!!.sortingMethod,
                index)
        IntegrityCore.settingsRepository.set(IntegrityCore.context, IntegrityCore.settingsRepository
                .get().copy(sortingMethod = newSortingMethod))
    }

    fun clickSortingDirectionButton() {
        val newSortingMethod = SortingUtil.revertSortingDirection(settingsData.value!!.sortingMethod)
        IntegrityCore.settingsRepository.set(IntegrityCore.context, IntegrityCore.settingsRepository
                .get().copy(sortingMethod = newSortingMethod))
    }

    fun removeArtifact(artifactId: Long)
            = IntegrityCore.removeArtifact(artifactId, false)

    fun removeSnapshot(artifactId: Long, date: String)
            = IntegrityCore.removeSnapshot(artifactId, date, false)

    fun addSnapshot(artifactId: Long) {
        openAddSnapshotOfArtifact(artifactId)
    }


    // floating button user actions

    fun clickFloatingButtonOption(index: Int) {
        val artifactId = inputStateData.value!!.filteredArtifactId
        if (artifactId != null) {
            openAddSnapshotOfArtifact(artifactId)
        } else {
            val typeName = IntegrityCore.getTypeNames().toList()[index]
            navigationEventData.value = NavigationEvent(
                    targetPackage = typeName.packageName,
                    targetClass = typeName.className,
                    bundledFontName = IntegrityCore.getFont(),
                    bundledColorBackground = IntegrityCore.getColorBackground(),
                    bundledColorPrimary = IntegrityCore.getColorPrimary(),
                    bundledColorAccent = IntegrityCore.getColorAccent(),
                    bundledDataFolderName = IntegrityCore.getDataFolderName()
            )
        }
    }


    // drawer user actions

    fun expandRunningJobsHeader(isExpanded: Boolean) {
        IntegrityCore.settingsRepository.set(IntegrityCore.context, IntegrityCore.settingsRepository
                .get().copy(jobsExpandRunning = isExpanded))
    }

    fun viewRunningJob(artifactId: Long, date: String) {
        viewRunningJobOrOpenSnapshot(artifactId, date)
    }

    fun expandScheduledJobsHeader(isExpanded: Boolean) {
        IntegrityCore.settingsRepository.set(IntegrityCore.context, IntegrityCore.settingsRepository
                .get().copy(jobsExpandScheduled = isExpanded))
    }

    fun editSnapshot(artifactId: Long) {
        openAddSnapshotOfArtifact(artifactId) // opens new snapshot of existing artifact, as blueprint
    }

    private fun openAddSnapshotOfArtifact(artifactId: Long) {
        val snapshot = IntegrityCore.metadataRepository.getLatestSnapshotMetadata(artifactId)
        val activityInfo = IntegrityCore.getDataTypeActivityInfo(snapshot.dataTypeClassName)
        navigationEventData.value = NavigationEvent(
                targetPackage = activityInfo.packageName,
                targetClass = activityInfo.name,
                bundledSnapshot = snapshot.copy(status = SnapshotStatus.BLUEPRINT), // as blueprint
                bundledFontName = IntegrityCore.getFont(),
                bundledColorBackground = IntegrityCore.getColorBackground(),
                bundledColorPrimary = IntegrityCore.getColorPrimary(),
                bundledColorAccent = IntegrityCore.getColorAccent(),
                bundledDataFolderName = IntegrityCore.getDataFolderName()
        )
    }

    fun viewArchiveLocations() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = folderLocationsScreenClass)
    }

    fun viewTags() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = tagsScreenClass)
    }

    fun viewLog() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = logScreenClass)
    }

    fun viewExtensions() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = settingsScreenClass, bundledOptionExtensions = true)
    }

    fun viewRestore() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = recoveryScreenClass)
    }

    fun setScheduledJobsEnabled(enabled: Boolean) {
        IntegrityCore.settingsRepository.set(IntegrityCore.context, IntegrityCore.settingsRepository
                .get().copy(jobsEnableScheduled = enabled))
    }

    fun viewSettings() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = settingsScreenClass)
    }

    fun viewHelp() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = helpInfoScreenClass)
    }

    fun viewLegal() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = legalInfoScreenClass)
    }


    // dialog

    fun cancelSnapshotCreation() {
        IntegrityCore.cancelSnapshotCreation(inputStateData.value!!.jobProgressArtifactId!!,
                inputStateData.value!!.jobProgressDate!!)
        hideRunningJobDialog()
    }


    // menus user action

    fun clickViewOptionsIcon() {
        // todo
    }

    /**
     * If searching of doing artifact filtering, these should be reset.
     */
    fun pressBackButton() {
        val isSearching = inputStateData.value!!.searchViewText.isNotBlank()
        val isArtifactFiltered = inputStateData.value!!.filteredArtifactId != null
        if (isSearching || isArtifactFiltered) {
            updateInputState(inputStateData.value!!
                    .copy(filteredArtifactId = null, searchViewText = ""))
        } else {
            navigationEventData.value = NavigationEvent("", "", goBack = true)
        }
    }


    // lifecycle actions

    fun snapshotReturned(snapshot: Snapshot) {
        val isSaving = IntegrityCore.saveSnapshot(IntegrityCore.context, snapshot)
        if (isSaving) {
            val snapshotBlueprint = IntegrityCore.metadataRepository
                    .getLatestSnapshotMetadata(snapshot.artifactId)
            viewRunningJobDialog(snapshotBlueprint.artifactId, snapshotBlueprint.date)
        }
    }
}
