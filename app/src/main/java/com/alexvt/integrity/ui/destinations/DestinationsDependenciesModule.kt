/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.destinations

import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class DestinationsDependenciesModule {

    @ContributesAndroidInjector
    abstract fun bindActivity(): DestinationsActivity

}

