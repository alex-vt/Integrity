/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core

import com.alexvt.integrity.core.metadata.MetadataRepository
import com.alexvt.integrity.core.metadata.SimplePersistableMetadataRepository
import com.alexvt.integrity.ui.destinations.DestinationsActivity
import com.alexvt.integrity.ui.info.HelpInfoActivity
import com.alexvt.integrity.ui.info.LegalInfoActivity
import com.alexvt.integrity.ui.log.LogViewActivity
import com.alexvt.integrity.ui.recovery.RecoveryActivity
import com.alexvt.integrity.ui.settings.SettingsActivity
import com.alexvt.integrity.ui.tags.TagsActivity
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class IntegrityCoreDependenciesModule {
    @Provides
    @Singleton
    @Named("packageName")
    fun providePackageName() = "com.alexvt.integrity"

    @Provides
    @Singleton
    fun provideMetadataRepository(): MetadataRepository = SimplePersistableMetadataRepository(@Inject context)

    @Provides
    @Singleton
    fun provideSearchIndexRepository() = IntegrityCore.searchIndexRepository

    @Provides
    @Singleton
    fun provideSettingsRepository() = IntegrityCore.settingsRepository

    @Provides
    @Singleton
    fun provideLogRepository() = IntegrityCore.logRepository

    @Provides
    @Singleton
    fun provideDataTypeRepository() = IntegrityCore.dataTypeRepository

    @Provides
    @Singleton
    fun provideSnapshotOperationManager() = IntegrityCore.snapshotOperationManager

    @Provides
    @Singleton
    fun provideScheduledJobManager() = IntegrityCore.scheduledJobManager

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