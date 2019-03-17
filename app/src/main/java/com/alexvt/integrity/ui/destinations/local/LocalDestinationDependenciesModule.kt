/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations.local

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.ui.ViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton


@Module
abstract class LocalDestinationDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindActivity(): LocalDestinationActivity

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<LocalDestinationViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @Module
    class IntentModule {
        @Provides
        @Named("editedLocalDestinationTitle")
        fun providesEditedTitle(activity: LocalDestinationActivity) = IntentUtil.getTitle(activity.intent)
    }

    @Module
    class ResourcesModule {
        @Provides
        @Named("defaultLocalDestinationTitle")
        fun providesDefaultDestinationTitle(context: Context) = "Folder on device" // todo from string
    }
}

