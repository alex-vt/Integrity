/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.tags

import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.lib.android.util.IntentUtil
import com.alexvt.integrity.android.ui.ViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named
import javax.inject.Provider


@Module
abstract class TagsDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class])
    abstract fun bindActivity(): TagsActivity

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<TagsViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @Module
    class IntentModule {
        @Provides
        @Named("selectModeTags")
        fun providesSelectMode(activity: TagsActivity) = IntentUtil.isSelectMode(activity.intent)

        @Provides
        @Named("snapshotWithInitialTags")
        fun providesSnapshot(activity: TagsActivity) = IntentUtil.getSnapshot(activity.intent)
    }
}

