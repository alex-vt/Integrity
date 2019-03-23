/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import android.content.Context
import com.alexvt.integrity.BuildConfig
import com.alexvt.integrity.core.credentials.CredentialsRepository
import com.alexvt.integrity.core.credentials.SimplePersistableCredentialsRepository
import com.alexvt.integrity.core.jobs.AndroidScheduledJobManager
import com.alexvt.integrity.core.jobs.ScheduledJobManager
import com.alexvt.integrity.core.jobs.ScheduledJobsEnableReceiver
import com.alexvt.integrity.core.log.LogEventReceiver
import com.alexvt.integrity.core.log.LogRepository
import com.alexvt.integrity.core.log.RoomLogRepository
import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.metadata.RoomMetadataRepository
import com.alexvt.integrity.core.metadata.SimplePersistableMetadataRepository
import com.alexvt.integrity.core.operations.AndroidSnapshotOperationManager
import com.alexvt.integrity.core.operations.LogOperationManager
import com.alexvt.integrity.core.operations.SnapshotOperationManager
import com.alexvt.integrity.core.search.RoomSearchIndexRepository
import com.alexvt.integrity.core.search.SearchIndexRepository
import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.core.settings.SimplePersistableSettingsRepository
import com.alexvt.integrity.core.types.AndroidDataTypeRepository
import com.alexvt.integrity.core.types.DataTypeRepository
import com.alexvt.integrity.lib.IntegrityLib
import com.alexvt.integrity.lib.filesystem.AndroidFilesystemManager
import com.alexvt.integrity.lib.filesystem.DataFolderManager
import com.alexvt.integrity.lib.filesystem.FilesystemManager
import com.alexvt.integrity.lib.jobs.RunningJobManager
import com.alexvt.integrity.ui.destinations.DestinationsActivity
import com.alexvt.integrity.ui.info.HelpInfoActivity
import com.alexvt.integrity.ui.info.LegalInfoActivity
import com.alexvt.integrity.ui.log.LogViewActivity
import com.alexvt.integrity.ui.recovery.RecoveryActivity
import com.alexvt.integrity.ui.settings.SettingsActivity
import com.alexvt.integrity.ui.tags.TagsActivity
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [IntegrityCoreDependenciesModule.ActionReceiverModule::class])
class IntegrityCoreDependenciesModule {

    @Module
    abstract class ActionReceiverModule {
        @ContributesAndroidInjector
        abstract fun contributesLogEntryReceiver(): LogEventReceiver.LogEntryReceiver

        @ContributesAndroidInjector
        abstract fun contributesLogReadReceiver(): LogEventReceiver.LogReadReceiver

        @ContributesAndroidInjector
        abstract fun contributesSnapshotProgressReceiver(): AndroidSnapshotOperationManager.SnapshotProgressReceiver

        @ContributesAndroidInjector
        abstract fun contributesScheduledJobsEnableReceiver(): ScheduledJobsEnableReceiver
    }

    @Provides
    @Singleton
    @Named("packageName")
    fun providePackageName() = "com.alexvt.integrity"

    @Provides
    @Singleton
    @Named("versionName")
    fun provideVersionName() = BuildConfig.VERSION_NAME

    @Provides
    @Singleton
    @Named("projectLink")
    fun provideProjectLink() = "https://github.com/alex-vt/Integrity/tree/develop" // todo update


    @Provides
    fun provideUiScheduler(): Scheduler = AndroidSchedulers.mainThread()


    @Provides
    @Singleton
    fun provideMetadataRepository(context: Context): MetadataRepository
            = RoomMetadataRepository(context)

    @Provides
    @Singleton
    fun provideCredentialsRepository(context: Context): CredentialsRepository
            = SimplePersistableCredentialsRepository(context)

    @Provides
    @Singleton
    fun provideSearchIndexRepository(context: Context): SearchIndexRepository
            = RoomSearchIndexRepository(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(context: Context): SettingsRepository
            = SimplePersistableSettingsRepository(context)

    @Provides
    @Singleton
    fun provideLogRepository(context: Context): LogRepository
            = RoomLogRepository(context)

    @Provides
    @Singleton
    fun provideDataTypeRepository(context: Context): DataTypeRepository
            = AndroidDataTypeRepository(context)

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
    fun provideSnapshotOperationManager(
            context: Context,
            metadataRepository: MetadataRepository,
            searchIndexRepository: SearchIndexRepository,
            dataFolderManager: DataFolderManager,
            runningJobManager: RunningJobManager,
            settingsRepository: SettingsRepository
    ): SnapshotOperationManager = AndroidSnapshotOperationManager(context, metadataRepository,
            searchIndexRepository, dataFolderManager, runningJobManager,
            settingsRepository)

    @Provides
    @Singleton
    fun provideScheduledJobManager(
            metadataRepository: MetadataRepository,
            settingsRepository: SettingsRepository,
            snapshotOperationManager: SnapshotOperationManager
    ): ScheduledJobManager = AndroidScheduledJobManager(metadataRepository, settingsRepository,
            snapshotOperationManager)

    @Provides
    @Singleton
    fun provideLogOperationManager(
            logRepository: LogRepository,
            context: Context
    ): LogOperationManager = LogOperationManager(logRepository, context)

    @Provides
    @Singleton
    fun provideRunningJobManager(): RunningJobManager = IntegrityLib.runningJobManager

    @Provides
    @Singleton
    @Named("destinationsScreenClass")
    fun provideDestinationsScreenClass() = DestinationsActivity::class.java.name

    @Provides
    @Singleton
    @Named("tagsScreenClass")
    fun provideTagsScreenClass() = TagsActivity::class.java.name

    @Provides
    @Singleton
    @Named("logScreenClass")
    fun provideLogScreenClass() = LogViewActivity::class.java.name

    @Provides
    @Singleton
    @Named("settingsScreenClass")
    fun provideSettingsScreenClass() = SettingsActivity::class.java.name

    @Provides
    @Singleton
    @Named("recoveryScreenClass")
    fun provideRecoveryScreenClass() = RecoveryActivity::class.java.name

    @Provides
    @Singleton
    @Named("helpInfoScreenClass")
    fun provideHelpInfoScreenClass() = HelpInfoActivity::class.java.name

    @Provides
    @Singleton
    @Named("legalInfoScreenClass")
    fun provideLegalInfoScreenClass() = LegalInfoActivity::class.java.name
}