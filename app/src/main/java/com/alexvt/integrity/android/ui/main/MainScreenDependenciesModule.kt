/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.main

import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.android.ui.ViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Provider


@Module
abstract class MainScreenDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class])
    abstract fun bindActivity(): MainActivity

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<MainScreenViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }
}
