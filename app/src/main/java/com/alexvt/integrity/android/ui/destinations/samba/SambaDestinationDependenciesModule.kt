/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.destinations.samba

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
abstract class SambaDestinationDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindActivity(): SambaDestinationActivity

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<SambaDestinationViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @Module
    class IntentModule {
        @Provides
        @Named("editedSambaDestinationTitle")
        fun providesEditedTitle(activity: SambaDestinationActivity) = IntentUtil.getTitle(activity.intent)
    }

    @Module
    class ResourcesModule {
        @Provides
        @Named("defaultSambaDestinationTitle")
        fun providesDefaultDestinationTitle(context: Context) = "LAN folder" // todo from string
    }
}
