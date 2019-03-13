/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.settings

import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class SettingsDependenciesModule {

    @ContributesAndroidInjector
    abstract fun bindActivity(): SettingsActivity


    @ContributesAndroidInjector
    abstract fun bindAppearanceFragment(): AppearanceSettingsFragment

    @ContributesAndroidInjector
    abstract fun bindBehaviorFragment(): BehaviorSettingsFragment

    @ContributesAndroidInjector
    abstract fun bindDataFragment(): DataSettingsFragment

    @ContributesAndroidInjector
    abstract fun bindExtensionFragment(): ExtensionSettingsFragment

    @ContributesAndroidInjector
    abstract fun bindNotificationFragment(): NotificationSettingsFragment
}

