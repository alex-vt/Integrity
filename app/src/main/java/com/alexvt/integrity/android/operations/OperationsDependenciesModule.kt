/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.operations

import android.content.Context
import com.alexvt.integrity.android.AutoStartReceiver
import com.alexvt.integrity.android.operations.jobs.AndroidScheduledJobManager
import com.alexvt.integrity.core.operations.jobs.ScheduledJobManager
import com.alexvt.integrity.android.operations.notifications.ScheduledJobsEnableReceiver
import com.alexvt.integrity.android.data.log.LogEventReceiver
import com.alexvt.integrity.android.operations.notifications.AndroidDisabledScheduledJobsNotifier
import com.alexvt.integrity.android.operations.notifications.AndroidErrorNotifier
import com.alexvt.integrity.android.operations.snapshots.AndroidSnapshotOperationManager
import com.alexvt.integrity.android.util.AppOpenReceiver
import com.alexvt.integrity.android.util.AppReopenReceiver
import com.alexvt.integrity.core.data.device.DeviceInfoRepository
import com.alexvt.integrity.core.data.log.LogRepository
import com.alexvt.integrity.core.data.metadata.MetadataRepository
import com.alexvt.integrity.core.operations.log.LogReadMarker
import com.alexvt.integrity.core.operations.snapshots.SnapshotOperationManager
import com.alexvt.integrity.core.data.search.SearchIndexRepository
import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.core.operations.destinations.DestinationUtilManager
import com.alexvt.integrity.core.operations.notifications.DisabledScheduledJobsNotifier
import com.alexvt.integrity.core.operations.notifications.ErrorNotifier
import com.alexvt.integrity.core.operations.snapshots.ArchiveManager
import com.alexvt.integrity.lib.android.data.destinations.AndroidDestinationNameRepository
import com.alexvt.integrity.lib.core.data.jobs.GlobalRunningJobs
import com.alexvt.integrity.lib.android.operations.filesystem.AndroidFilesystemManager
import com.alexvt.integrity.lib.android.operations.log.AndroidLogManager
import com.alexvt.integrity.lib.core.data.destinations.DestinationNameRepository
import com.alexvt.integrity.lib.core.operations.filesystem.DataFolderManager
import com.alexvt.integrity.lib.core.operations.filesystem.FilesystemManager
import com.alexvt.integrity.lib.core.data.jobs.RunningJobRepository
import com.alexvt.integrity.lib.core.operations.log.LogManager
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Singleton

@Module(includes = [OperationsDependenciesModule.ActionReceiverModule::class])
class OperationsDependenciesModule {

    @Module
    abstract class ActionReceiverModule {
        @ContributesAndroidInjector
        abstract fun contributesLogEntryReceiver(): LogEventReceiver.LogEntryReceiver

        @ContributesAndroidInjector
        abstract fun contributesLogReadReceiver(): LogEventReceiver.LogReadReceiver

        @ContributesAndroidInjector
        abstract fun contributesAutoStartReceiver(): AutoStartReceiver

        @ContributesAndroidInjector
        abstract fun contributesAppOpenReceiver(): AppOpenReceiver

        @ContributesAndroidInjector
        abstract fun contributesAppReopenReceiver(): AppReopenReceiver

        @ContributesAndroidInjector
        abstract fun contributesSnapshotProgressReceiver(): AndroidSnapshotOperationManager.SnapshotProgressReceiver

        @ContributesAndroidInjector
        abstract fun contributesScheduledJobsEnableReceiver(): ScheduledJobsEnableReceiver
    }

    @Provides
    fun provideUiScheduler(): Scheduler = AndroidSchedulers.mainThread()

    @Provides
    @Singleton
    fun provideDataFolderManager(filesystemManager: FilesystemManager): DataFolderManager
            = DataFolderManager(filesystemManager)

    @Provides
    @Singleton
    fun provideFilesystemManager(context: Context): FilesystemManager
            = AndroidFilesystemManager(context)

    @Provides
    @Singleton
    fun provideDestinationNameRepository(): DestinationNameRepository
            = AndroidDestinationNameRepository()

    @Provides
    @Singleton
    fun provideSnapshotOperationManager(
            context: Context,
            metadataRepository: MetadataRepository,
            searchIndexRepository: SearchIndexRepository,
            dataFolderManager: DataFolderManager,
            runningJobRepository: RunningJobRepository,
            settingsRepository: SettingsRepository,
            archiveManager: ArchiveManager,
            deviceInfoRepository: DeviceInfoRepository,
            logManager: LogManager,
            destinationUtilManager: DestinationUtilManager
    ): SnapshotOperationManager = AndroidSnapshotOperationManager(context, metadataRepository,
            searchIndexRepository, dataFolderManager, runningJobRepository,
            settingsRepository, archiveManager, deviceInfoRepository, logManager,
            destinationUtilManager)

    @Provides
    @Singleton
    fun provideScheduledJobManager(
            metadataRepository: MetadataRepository,
            settingsRepository: SettingsRepository,
            snapshotOperationManager: SnapshotOperationManager,
            logManager: LogManager
    ): ScheduledJobManager = AndroidScheduledJobManager(metadataRepository, settingsRepository,
            snapshotOperationManager, logManager)

    @Provides
    @Singleton
    fun provideLogOperationManager(
            logRepository: LogRepository,
            errorNotifier: ErrorNotifier
    ): LogReadMarker = LogReadMarker(logRepository, errorNotifier)

    @Provides
    @Singleton
    fun provideErrorNotifier(context: Context): ErrorNotifier
            = AndroidErrorNotifier(context)

    @Provides
    @Singleton
    fun provideDisabledScheduledJobsNotifier(context: Context): DisabledScheduledJobsNotifier
            = AndroidDisabledScheduledJobsNotifier(context)

    @Provides
    @Singleton
    fun provideRunningJobManager(): RunningJobRepository = GlobalRunningJobs.RUNNING_JOB_REPOSITORY

    @Provides
    @Singleton
    fun provideLogManager(context: Context): LogManager = AndroidLogManager(context)
}