/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.alexvt.integrity.ui.UiDependenciesModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap


@Module
abstract class MainScreenDependenciesModule {

    @Binds
    @IntoMap
    @UiDependenciesModule.ViewModelKey(MainScreenViewModel::class)
    abstract fun bindViewModel(viewModel: MainScreenViewModel): ViewModel

    @ContributesAndroidInjector(modules = [InjectViewModelModule::class])
    abstract fun bindActivity(): MainActivity

    @Module
    class InjectViewModelModule {
        @Provides
        fun provideViewModel(factory: ViewModelProvider.Factory, target: MainActivity)
                = ViewModelProviders.of(target, factory).get(MainScreenViewModel::class.java)
    }
}

