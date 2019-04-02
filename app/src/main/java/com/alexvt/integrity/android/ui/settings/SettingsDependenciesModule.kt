/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.android.ui.ViewModelFactory
import com.alexvt.integrity.R
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named
import javax.inject.Provider


@Module
abstract class SettingsDependenciesModule {

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindActivity(): SettingsActivity

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindAppearanceFragment(): AppearanceSettingsFragment

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindBehaviorFragment(): BehaviorSettingsFragment

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindDataFragment(): DataSettingsFragment

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindNotificationFragment(): NotificationSettingsFragment

    @ContributesAndroidInjector(modules = [ViewModelFactoryModule::class, IntentModule::class, ResourcesModule::class])
    abstract fun bindExtensionFragment(): ExtensionSettingsFragment

    @Module
    class ViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<SettingsViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @Module
    class IntentModule {
        @Provides
        @Named("initialTabId")
        fun providesInitialTabId()
                = R.id.action_appearance // todo provide from intent
    }

    @Module
    class ResourcesModule {
        @Provides
        @Named("colorsBackground")
        fun providesColorsBackground(context: Context) = context.resources.getIntArray(R.array.colorsBackground)

        @Provides
        @Named("colorsPalette")
        fun providesColorsPalette(context: Context) = context.resources.getIntArray(R.array.colorsPrimary)
    }
}

