/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.main

import androidx.lifecycle.MutableLiveData
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.jobs.ScheduledJobManager
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.lib.search.SearchResult
import com.alexvt.integrity.core.search.SearchManager
import com.alexvt.integrity.core.search.SortingUtil
import com.alexvt.integrity.core.settings.IntegrityAppSettings
import com.alexvt.integrity.core.settings.SortingMethod
import com.alexvt.integrity.core.operations.SnapshotOperationManager
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.core.types.DataTypeRepository
import com.alexvt.integrity.lib.IntegrityLib
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.util.ThrottledFunction
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.metadata.SnapshotStatus
import com.alexvt.integrity.ui.ThemedViewModel
import com.alexvt.integrity.ui.util.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

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
        val recreate: Boolean = false,

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

class MainScreenViewModel @Inject constructor(
        // for navigation
        @Named("packageName") val packageName: String,
        @Named("versionName") val versionName: String,
        val metadataRepository: MetadataRepository,
        val searchIndexRepository: SearchIndexRepository,
        override val settingsRepository: SettingsRepository,
        val dataFolderManager: DataFolderManager,
        val logRepository: LogRepository,
        val dataTypeRepository: DataTypeRepository,
        val snapshotOperationManager: SnapshotOperationManager,
        val scheduledJobManager: ScheduledJobManager,
        @Named("destinationsScreenClass") val destinationsScreenClass: String,
        @Named("tagsScreenClass") val tagsScreenClass: String,
        @Named("logScreenClass") val logScreenClass: String,
        @Named("settingsScreenClass") val settingsScreenClass: String,
        @Named("recoveryScreenClass") val recoveryScreenClass: String,
        @Named("helpInfoScreenClass") val helpInfoScreenClass: String,
        @Named("legalInfoScreenClass") val legalInfoScreenClass: String
    ): ThemedViewModel() {

    // primary
    val inputStateData = MutableLiveData<MainScreenInputState>()
    val settingsData = MutableLiveData<IntegrityAppSettings>()
    val runningJobIdsData = MutableLiveData<List<Snapshot>>()
    val scheduledJobIdsData = MutableLiveData<List<Pair<Snapshot, Long>>>()
    val logErrorCountData = MutableLiveData<Int>()
    val typeNameData = MutableLiveData<List<String>>()

    // depends on primary
    val searchResultsData = MutableLiveData<List<SearchResult>>()
    val snapshotsData = MutableLiveData<List<Pair<Snapshot, Int>>>()

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()

    private val searchUtil = SearchManager(metadataRepository, searchIndexRepository)

    init {
        // input state  starts as default
        inputStateData.value = MainScreenInputState(searchViewText = "", filteredArtifactId = null,
                runningDownloadViewArtifactId = null, viewingSortingTypeOptions = false,
                jobProgressArtifactId = null, jobProgressDate = null,
                jobProgressTitle = "", jobProgressMessage = "")

        // settings, jobs, log error count  are listened to in their repositories
        settingsData.value = settingsRepository.get()
        settingsRepository.addChangesListener(this.toString()) {
            val themeChanged = settingsData.value!!.textFont != it.textFont
                    || settingsData.value!!.colorBackground != it.colorBackground
                    || settingsData.value!!.colorPrimary != it.colorPrimary
                    || settingsData.value!!.colorAccent != it.colorAccent
            settingsData.value = it
            updateContentData() // snapshots, search results  depend on  settings
            if (themeChanged) {
                navigationEventData.value = NavigationEvent(targetPackage = "", targetClass = "",
                        recreate = true)
            }
        }
        runningJobIdsData.value = emptyList()
        IntegrityLib.runningJobManager.addJobListListener(this.toString()) {
            runningJobIdsData.value = it.map {
                metadataRepository.getSnapshotMetadata(it.first, it.second)
            }
        }
        scheduledJobIdsData.value = emptyList()
        scheduledJobManager.addScheduledJobsListener(this.toString()) {
            scheduledJobIdsData.value = it.map {
                metadataRepository.getSnapshotMetadata(it.first, it.second)
            }.map {
                it to scheduledJobManager.getNextRunTimestamp(it) - System.currentTimeMillis()
            }
        }
        logRepository.addChangesListener(this.toString()) {
            logRepository.getUnreadErrors {
                logErrorCountData.value = it.count()
            }
        }

        // version name, snapshot type component names  are static
        typeNameData.value = dataTypeRepository.getAllDataTypes().map { it.title } // todo add listener to repo

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
                searchUtil.searchText(searchText, filteredArtifactId), sortingMethod)
    }

    private fun fetchSnapshots(): List<Pair<Snapshot, Int>> {
        val filteredArtifactId = inputStateData.value!!.filteredArtifactId
        val snapshots = SortingUtil.sortSnapshots(when (filteredArtifactId) {
            null -> metadataRepository.getAllArtifactLatestMetadata(true)
            else -> metadataRepository.getArtifactMetadata(filteredArtifactId)
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

    private fun getSnapshotCount(artifactId: Long) = metadataRepository
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

    fun computeArtifactFilterTitle(): String {
        val filteredArtifactId = inputStateData.value!!.filteredArtifactId
        val title = if (filteredArtifactId != null) {
            metadataRepository.getLatestSnapshotMetadata(filteredArtifactId).title
        } else {
            ""
        }
        val isSearching = inputStateData.value!!.searchViewText.isNotBlank()
        val prefix = if (isSearching) "in: " else ""
        return "$prefix$title"
    }

    fun getSortingTypeNames() = SortingUtil.getSortingTypeNames()

    fun getSortingTypeNameIndex() = SortingUtil
            .getSortingTypeNameIndex(settingsData.value!!.sortingMethod)


    // content user actions

    fun viewSnapshot(artifactId: Long, date: String) {
        viewRunningJobOrOpenSnapshot(artifactId, date)
    }

    private fun viewRunningJobOrOpenSnapshot(artifactId: Long, date: String) {
        val snapshot = metadataRepository.getSnapshotMetadata(artifactId, date)
        if (snapshot.status == SnapshotStatus.IN_PROGRESS) {
            viewRunningJobDialog(artifactId, date)
            return
        }
        // todo ensure snapshot data presence in folder first
        val dataType = dataTypeRepository.getDataType(snapshot.dataTypeClassName)
        navigationEventData.value = NavigationEvent(
                targetPackage = dataType.packageName,
                targetClass = dataType.viewerClass,
                bundledSnapshot = snapshot,
                bundledDates = getCompleteSnapshotDatesOrNull(artifactId),
                bundledFontName = settingsData.value!!.textFont,
                bundledColorBackground = settingsData.value!!.colorBackground,
                bundledColorPrimary = settingsData.value!!.colorPrimary,
                bundledColorAccent = settingsData.value!!.colorAccent,
                bundledDataFolderName = settingsData.value!!.dataFolderPath
        )
    }
    
    private fun getCompleteSnapshotDatesOrNull(artifactId: Long) = metadataRepository
            .getArtifactMetadata(artifactId).snapshots
            .filter { it.status == SnapshotStatus.COMPLETE }
            .map { it.date }
            .reversed() // in ascending order
            .ifEmpty { null }


    private fun viewRunningJobDialog(artifactId: Long, date: String) {
        val snapshot = metadataRepository.getSnapshotMetadata(artifactId, date)
        if (snapshot.status == SnapshotStatus.IN_PROGRESS) {
            updateInputState(inputStateData.value!!.copy(jobProgressArtifactId = artifactId,
                    jobProgressDate = date, jobProgressTitle = snapshot.title))
            IntegrityLib.runningJobManager.setJobProgressListener(snapshot) {
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
        settingsRepository.set(settingsRepository
                .get().copy(sortingMethod = newSortingMethod))
    }

    fun clickSortingDirectionButton() {
        val newSortingMethod = SortingUtil.revertSortingDirection(settingsData.value!!.sortingMethod)
        settingsRepository.set(settingsRepository
                .get().copy(sortingMethod = newSortingMethod))
    }

    fun removeArtifact(artifactId: Long)
            = snapshotOperationManager.removeArtifact(artifactId, false)

    fun removeSnapshot(artifactId: Long, date: String)
            = snapshotOperationManager.removeSnapshot(artifactId, date, false)

    fun addSnapshot(artifactId: Long) {
        openAddSnapshotOfArtifact(artifactId)
    }


    // floating button user actions

    fun clickFloatingButtonOption(index: Int) {
        val artifactId = inputStateData.value!!.filteredArtifactId
        if (artifactId != null) {
            openAddSnapshotOfArtifact(artifactId)
        } else {
            val dataType = dataTypeRepository.getAllDataTypes()[index]
            navigationEventData.value = NavigationEvent(
                    targetPackage = dataType.packageName,
                    targetClass = dataType.viewerClass,
                    bundledFontName = settingsData.value!!.textFont,
                    bundledColorBackground = settingsData.value!!.colorBackground,
                    bundledColorPrimary = settingsData.value!!.colorPrimary,
                    bundledColorAccent = settingsData.value!!.colorAccent,
                    bundledDataFolderName = settingsData.value!!.dataFolderPath
            )
        }
    }


    // drawer user actions

    fun expandRunningJobsHeader(isExpanded: Boolean) {
        settingsRepository.set(settingsRepository
                .get().copy(jobsExpandRunning = isExpanded))
    }

    fun viewRunningJob(artifactId: Long, date: String) {
        viewRunningJobOrOpenSnapshot(artifactId, date)
    }

    fun expandScheduledJobsHeader(isExpanded: Boolean) {
        settingsRepository.set(settingsRepository
                .get().copy(jobsExpandScheduled = isExpanded))
    }

    fun editSnapshot(artifactId: Long) {
        openAddSnapshotOfArtifact(artifactId) // opens new snapshot of existing artifact, as blueprint
    }

    private fun openAddSnapshotOfArtifact(artifactId: Long) {
        val snapshot = metadataRepository.getLatestSnapshotMetadata(artifactId)
        val dataType = dataTypeRepository.getDataType(snapshot.dataTypeClassName)
        navigationEventData.value = NavigationEvent(
                targetPackage = dataType.packageName,
                targetClass = dataType.viewerClass,
                bundledSnapshot = snapshot.copy(status = SnapshotStatus.BLUEPRINT), // as blueprint
                bundledFontName = settingsData.value!!.textFont,
                bundledColorBackground = settingsData.value!!.colorBackground,
                bundledColorPrimary = settingsData.value!!.colorPrimary,
                bundledColorAccent = settingsData.value!!.colorAccent,
                bundledDataFolderName = settingsData.value!!.dataFolderPath
        )
    }

    fun viewArchiveLocations() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = destinationsScreenClass)
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
        settingsRepository.set(settingsRepository
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
        snapshotOperationManager.cancelSnapshotCreation(
                inputStateData.value!!.jobProgressArtifactId!!,
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
        val isSaving = snapshotOperationManager.saveSnapshot(snapshot)
        if (isSaving) {
            val snapshotBlueprint = metadataRepository
                    .getLatestSnapshotMetadata(snapshot.artifactId)
            viewRunningJobDialog(snapshotBlueprint.artifactId, snapshotBlueprint.date)
        }
    }
}
