/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.test.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import com.alexvt.integrity.android.ui.main.MainScreenInputState
import com.alexvt.integrity.android.ui.main.MainScreenViewModel
import com.alexvt.integrity.core.data.log.LogRepository
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.data.settings.IntegrityAppSettings
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.data.types.DataTypeRepository
import com.alexvt.integrity.core.operations.jobs.ScheduledJobManager
import com.alexvt.integrity.core.operations.search.SearchManager
import com.alexvt.integrity.core.operations.snapshots.SnapshotOperationManager
import com.alexvt.integrity.lib.core.data.metadata.Snapshot
import com.alexvt.integrity.lib.core.data.search.SearchRequest
import com.alexvt.integrity.lib.core.data.search.SnapshotSearchResult
import com.alexvt.integrity.lib.core.data.search.TextSearchResult
import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import org.junit.After
import org.junit.Assert.*


@RunWith(JUnit4::class)
class MainScreenViewModelTest {

    // Multithreading & lifecycle
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    val testRxScheduler = Schedulers.trampoline()
    @Mock lateinit var lifecycleOwner: LifecycleOwner
    val lifecycleRegistry by lazy { LifecycleRegistry(lifecycleOwner) }

    // LiveData test values
    val initialInputState = MainScreenInputState(
            searchViewText = "",
            filteredArtifactId = null,
            searchShowOnePerArtifact = true,
            runningDownloadViewArtifactId = null,
            viewingSortingTypeOptions = false,
            jobProgressArtifactId = null,
            jobProgressDate = null,
            jobProgressTitle = "",
            jobProgressMessage = ""
    )
    val basicTextSearchResult = TextSearchResult(
            snapshotTitle = "Text Result",
            artifactId = 1L,
            date = "now",
            truncatedText = "truncated text",
            highlightRange = 0..4,
            relevantLinkOrNull = null
    )
    val basicSnapshot = Snapshot()

    // Data mocks for ViewModel constructor
    val metadataRepository = mock<MetadataRepository> {
        on { getAllArtifactLatestMetadataFlowable() } doAnswer { Flowable.fromArray(listOf(basicSnapshot)) }
        on { getArtifactMetadataFlowable(any()) } doAnswer { Flowable.fromArray(listOf(basicSnapshot)) }
        on { getArtifactMetadataBlocking(any()) } doAnswer { emptyList() }
    }
    val settingsRepository = mock<SettingsRepository> {
        on { get() } doAnswer { IntegrityAppSettings() }
    }.apply {
        whenever(this.addChangesListener(any(), any())).thenAnswer {
            val argument = it.arguments[1]
            val completion = argument as ((settings: IntegrityAppSettings) -> Unit)
            completion.invoke(IntegrityAppSettings())
        }
    }
    val logRepository = mock<LogRepository> {
        on { getUnreadErrorsFlowable(any()) } doAnswer { Flowable.fromArray(emptyList()) }
    }
    @Mock lateinit var dataTypeRepository: DataTypeRepository

    // Operation mocks for ViewModel constructor
    val searchManager = mock<SearchManager> {
        on { searchText(any()) } doAnswer {
            val searchRequest = it.arguments[0] as SearchRequest
            val searchResult = if (searchRequest.text.isEmpty()) {
                emptyList()
            } else {
                listOf(basicTextSearchResult)
            }
            Single.just(searchResult)
        }
        on { searchSnapshotTitles(any()) } doAnswer { Single.just(emptyList()) }
    }
    @Mock lateinit var dataFolderManager: DataFolderManager
    @Mock lateinit var snapshotOperationManager: SnapshotOperationManager
    @Mock lateinit var scheduledJobManager: ScheduledJobManager

    // UI related test values for ViewModel constructor
    var uiScheduler: Scheduler = testRxScheduler
    var packageName: String = "com.alexvt.integrity.test"
    var versionName: String = "0.01-test"
    var destinationsScreenClass: String = "destinationsScreenClass"
    var tagsScreenClass: String = "tagsScreenClass"
    var logScreenClass: String = "logScreenClass"
    var settingsScreenClass: String = "settingsScreenClass"
    var recoveryScreenClass: String = "recoveryScreenClass"
    var helpInfoScreenClass: String = "helpInfoScreenClass"
    var legalInfoScreenClass: String = "legalInfoScreenClass"

    // LiveData observer mocks
    @Mock lateinit var inputStateObserver: Observer<MainScreenInputState>
    @Mock lateinit var logErrorCountObserver: Observer<Int>
    @Mock lateinit var settingsObserver: Observer<IntegrityAppSettings>
    @Mock lateinit var snapshotsObserver: Observer<List<Pair<Snapshot, Int>>>
    @Mock lateinit var runningJobSnapshotsObserver: Observer<List<Snapshot>>
    @Mock lateinit var scheduledJobSnapshotsObserver: Observer<List<Pair<Snapshot, Long>>>
    @Mock lateinit var typeNameObserver: Observer<List<String>>
    @Mock lateinit var snapshotSearchResultObserver: Observer<List<SnapshotSearchResult>>
    @Mock lateinit var textSearchResultObserver: Observer<List<TextSearchResult>>

    // Class under test
    lateinit var mainScreenViewModel: MainScreenViewModel

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        // Multithreading settings
        Dispatchers.setMain(Dispatchers.Default)
        RxJavaPlugins.setIoSchedulerHandler { testRxScheduler }
        RxJavaPlugins.setComputationSchedulerHandler { testRxScheduler }
        RxJavaPlugins.setNewThreadSchedulerHandler { testRxScheduler }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { testRxScheduler }
        RxAndroidPlugins.setMainThreadSchedulerHandler { testRxScheduler }
        RxJavaPlugins.setErrorHandler { throwable ->
            print("Error handler reported:\n $throwable")
        }

        whenever(lifecycleOwner.lifecycle).thenReturn(lifecycleRegistry)

        // Class under test
        mainScreenViewModel = MainScreenViewModel(uiScheduler, packageName, versionName,
                metadataRepository, searchManager, settingsRepository, dataFolderManager,
                logRepository, dataTypeRepository, snapshotOperationManager, scheduledJobManager,
                destinationsScreenClass, tagsScreenClass, logScreenClass, settingsScreenClass,
                recoveryScreenClass, helpInfoScreenClass, legalInfoScreenClass)

        mainScreenViewModel.snapshotsData.observe(lifecycleOwner, snapshotsObserver)
        mainScreenViewModel.inputStateData.observe(lifecycleOwner, inputStateObserver)
        mainScreenViewModel.textSearchResultsData.observe(lifecycleOwner, textSearchResultObserver)
        mainScreenViewModel.settingsData.observe(lifecycleOwner, settingsObserver)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    @Test
    fun `MutableLiveData reliability`() {
        mainScreenViewModel.snapshotsData.value = listOf(Pair(basicSnapshot, 1)) // force load 1
        mainScreenViewModel.snapshotsData.value = emptyList() // force clear
        mainScreenViewModel.snapshotsData.value = listOf(Pair(basicSnapshot, 2)) // force load 2

        argumentCaptor<List<Pair<Snapshot, Int>>> {
            verify(snapshotsObserver, after(20)!!.times(5)).onChanged(capture())
            assertEquals(allValues[0], emptyList<Pair<Snapshot, Int>>()) // initial
            assertEquals(allValues[1], listOf(Pair(basicSnapshot, 1))) // force load 1
            assertEquals(allValues[2], emptyList<Pair<Snapshot, Int>>()) // force clear
            assertEquals(allValues[3], listOf(Pair(basicSnapshot, 2))) // force load 2
            assertEquals(allValues[4], listOf(Pair(basicSnapshot, 0))) // loaded automatically
        }
    }

    @Test
    fun `Main screen opened`() {
        verify(settingsObserver).onChanged(IntegrityAppSettings())
        verify(inputStateObserver).onChanged(initialInputState)
        verify(textSearchResultObserver).onChanged(emptyList())
        argumentCaptor<List<Pair<Snapshot, Int>>> {
            verify(snapshotsObserver, after(20)!!.times(2)).onChanged(capture())
            assertEquals(firstValue, emptyList<Pair<Snapshot, Int>>()) // first the list is empty
            assertEquals(lastValue, listOf(Pair(basicSnapshot, 0))) // data loaded
        }
    }

    @Test
    fun `Searching, then clearing search`() {
        val searchDelayExceedingTimeMillis = 510L

        mainScreenViewModel.setSearchText("test search")
        Thread.sleep(searchDelayExceedingTimeMillis)
        mainScreenViewModel.setSearchText("")

        argumentCaptor<List<TextSearchResult>> {
            verify(textSearchResultObserver, after(searchDelayExceedingTimeMillis)!!.times(4))
                    .onChanged(capture())
            assertEquals(firstValue, emptyList<TextSearchResult>()) // no search results at start
            assertEquals(secondValue, listOf(basicTextSearchResult)) // search results
            assertEquals(thirdValue, listOf(basicTextSearchResult)) // search results // todo fix duplicates
            assertEquals(allValues[3], emptyList<TextSearchResult>()) // search cleared
        }

        argumentCaptor<MainScreenInputState> {
            verify(inputStateObserver, times(3)).onChanged(capture())
            assertEquals(firstValue, initialInputState) // no search results at start
            assertEquals(secondValue, initialInputState.copy(searchViewText = "test search")) // no search results at start
            assertEquals(thirdValue, initialInputState) // search results on search
        }
    }



    @After
    fun dispose() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}