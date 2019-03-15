/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations

import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.lib.util.IntentUtil
import com.alexvt.integrity.ui.ViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named


@Module
abstract class DestinationsDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class])
    abstract fun bindActivity(): DestinationsActivity

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: DestinationsViewModel): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @Module
    class IntentModule {
        @Provides
        @Named("selectMode")
        fun providesSelectMode(activity: DestinationsActivity) = IntentUtil.isSelectMode(activity.intent)

        @Provides
        @Named("snapshotWithInitialDestination")
        fun providesSnapshot(activity: DestinationsActivity) = IntentUtil.getSnapshot(activity.intent)
    }
}

