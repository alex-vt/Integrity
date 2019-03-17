/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.log

import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.ui.ViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Provider


@Module
abstract class LogViewDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class])
    abstract fun bindActivity(): LogViewActivity

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<LogViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

}

