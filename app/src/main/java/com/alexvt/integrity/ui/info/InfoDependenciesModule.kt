/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.info

import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class InfoDependenciesModule {

    @ContributesAndroidInjector
    abstract fun bindHelpInfoActivity(): HelpInfoActivity

    @ContributesAndroidInjector
    abstract fun bindHelpInfoFragment(): HelpInfoSettingsFragment

    @ContributesAndroidInjector
    abstract fun bindLegalInfoActivity(): LegalInfoActivity

    @ContributesAndroidInjector
    abstract fun bindLegalInfoFragment(): LegalInfoSettingsFragment


}

