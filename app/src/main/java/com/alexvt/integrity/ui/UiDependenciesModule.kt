/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.ui.destinations.DestinationsDependenciesModule
import com.alexvt.integrity.ui.destinations.local.LocalDestinationDependenciesModule
import com.alexvt.integrity.ui.destinations.samba.SambaDestinationDependenciesModule
import com.alexvt.integrity.ui.info.InfoDependenciesModule
import com.alexvt.integrity.ui.log.LogViewDependenciesModule
import com.alexvt.integrity.ui.main.MainScreenDependenciesModule
import com.alexvt.integrity.ui.recovery.RecoveryDependenciesModule
import com.alexvt.integrity.ui.settings.SettingsDependenciesModule
import com.alexvt.integrity.ui.tags.TagsDependenciesModule
import dagger.Module
import dagger.Lazy
import javax.inject.Inject

@Module(includes = [
    MainScreenDependenciesModule::class,
    DestinationsDependenciesModule::class,
    LocalDestinationDependenciesModule::class,
    SambaDestinationDependenciesModule::class,
    InfoDependenciesModule::class,
    LogViewDependenciesModule::class,
    RecoveryDependenciesModule::class,
    SettingsDependenciesModule::class,
    TagsDependenciesModule::class
])
abstract class UiDependenciesModule

class ViewModelFactory<VM : ViewModel>(
        private val viewModel: dagger.Lazy<VM>
) : ViewModelProvider.Factory {

    @Inject
    constructor(viewModel: VM) : this(Lazy { viewModel })

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel.get() as T
    }
}