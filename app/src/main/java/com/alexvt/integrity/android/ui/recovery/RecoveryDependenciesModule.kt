/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.recovery

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.lib.android.util.IntentUtil
import com.alexvt.integrity.android.ui.ViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named
import javax.inject.Provider


@Module
abstract class RecoveryDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class,
        ResourcesModule::class])
    abstract fun bindActivity(): RecoveryActivity

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<RecoveryViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @Module
    class IntentModule {
        @Provides
        @Named("recoveryIssue")
        fun providesIssue(activity: RecoveryActivity) = IntentUtil.getIssueDescription(activity.intent)
    }

    @Module
    class ResourcesModule {
        @Provides
        @Named("metadataRepositoryName")
        fun providesMetadataRepositoryName(context: Context) = "Snapshots metadata" // todo from string

        @Provides
        @Named("logRepositoryName")
        fun providesLogRepositoryName(context: Context) = "Log" // todo from string

        @Provides
        @Named("settingsRepositoryName")
        fun providesSettingsRepositoryName(context: Context) = "App settings" // todo from string

        @Provides
        @Named("credentialsRepositoryName")
        fun providesCredentialsRepositoryName(context: Context) = "Credentials" // todo from string

        @Provides
        @Named("searchIndexRepositoryName")
        fun providesSearchIndexRepositoryName(context: Context) = "Search index" // todo from string
    }
}

