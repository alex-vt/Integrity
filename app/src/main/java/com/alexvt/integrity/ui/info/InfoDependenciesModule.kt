/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.info

import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.ui.ViewModelFactory
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector


@Module
abstract class InfoDependenciesModule {

    @ContributesAndroidInjector(modules = [HelpViewModelFactoryModule::class])
    abstract fun bindHelpInfoActivity(): HelpInfoActivity

    @ContributesAndroidInjector(modules = [HelpViewModelFactoryModule::class])
    abstract fun bindHelpInfoFragment(): HelpInfoSettingsFragment

    @Module
    class HelpViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: HelpInfoViewModel): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @ContributesAndroidInjector
    abstract fun bindLegalInfoActivity(): LegalInfoActivity

    @ContributesAndroidInjector
    abstract fun bindLegalInfoFragment(): LegalInfoSettingsFragment


}

