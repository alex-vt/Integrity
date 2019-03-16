/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.recovery

import com.alexvt.integrity.core.credentials.CredentialsRepository
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.filesystem.FilesystemManager
import com.alexvt.integrity.ui.ThemedViewModel
import com.alexvt.integrity.ui.util.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

data class NavigationEvent(
        val goBack: Boolean = false,
        val viewAppInfo: Boolean = false,
        val restartApp: Boolean = false
)

class RecoveryViewModel @Inject constructor(
        val metadataRepository: MetadataRepository,
        @Named("metadataRepositoryName") val metadataRepositoryName: String,
        val logRepository: LogRepository,
        @Named("logRepositoryName") val logRepositoryName: String,
        override val settingsRepository: SettingsRepository,
        @Named("settingsRepositoryName") val settingsRepositoryName: String,
        val credentialsRepository: CredentialsRepository,
        @Named("credentialsRepositoryName") val credentialsRepositoryName: String,
        val searchIndexRepository: SearchIndexRepository,
        @Named("searchIndexRepositoryName") val searchIndexRepositoryName: String,
        val dataFolderManager: DataFolderManager,
        val filesystemManager: FilesystemManager,
        @Named("recoveryIssue") val recoveryIssue: String?
        ) : ThemedViewModel() {

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()

    private fun getSnapshotsFolderPath() = settingsRepository.get().dataFolderPath

    fun getSnapshotsFolderName() = getSnapshotsFolderPath()
            .removePrefix(filesystemManager.getRootFolder())

    private val namedRepositories = listOf(
            metadataRepository to metadataRepositoryName,
            logRepository to logRepositoryName,
            settingsRepository to settingsRepositoryName,
            credentialsRepository to credentialsRepositoryName,
            searchIndexRepository to searchIndexRepositoryName
    )

    fun getRepositoryNames() = namedRepositories.map { it.second }

    fun getRepositoryNamesAt(indices: IntArray) = namedRepositories
            .filterIndexed { index, _ -> index in indices }
            .joinToString(separator = ", ") { it.second }


    // navigation actions

    fun viewAppInfo() {
        navigationEventData.value = NavigationEvent(viewAppInfo = true)
    }

    fun clickRestartApp() {
        navigationEventData.value = NavigationEvent(restartApp = true)
    }

    fun clickClearRepositoriesAt(indices: IntArray) {
        namedRepositories.filterIndexed { index, _ -> index in indices }
                .map { it.first }
                .forEach { it.init(true) }
    }

    fun clickDeleteSnapshotsFolder() {
        dataFolderManager.deleteFolder(getSnapshotsFolderPath()) // assuming it doesn't change by then
    }

    fun pressBackButton() {
        navigationEventData.value = NavigationEvent(goBack = true)
    }

}
