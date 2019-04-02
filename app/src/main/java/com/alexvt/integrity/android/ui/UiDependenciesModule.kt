/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.BuildConfig
import com.alexvt.integrity.android.ui.destinations.DestinationsActivity
import com.alexvt.integrity.android.ui.destinations.DestinationsDependenciesModule
import com.alexvt.integrity.android.ui.destinations.local.LocalDestinationDependenciesModule
import com.alexvt.integrity.android.ui.destinations.samba.SambaDestinationDependenciesModule
import com.alexvt.integrity.android.ui.info.HelpInfoActivity
import com.alexvt.integrity.android.ui.info.InfoDependenciesModule
import com.alexvt.integrity.android.ui.info.LegalInfoActivity
import com.alexvt.integrity.android.ui.log.LogViewActivity
import com.alexvt.integrity.android.ui.log.LogViewDependenciesModule
import com.alexvt.integrity.android.ui.main.MainScreenDependenciesModule
import com.alexvt.integrity.android.ui.recovery.RecoveryActivity
import com.alexvt.integrity.android.ui.recovery.RecoveryDependenciesModule
import com.alexvt.integrity.android.ui.settings.SettingsActivity
import com.alexvt.integrity.android.ui.settings.SettingsDependenciesModule
import com.alexvt.integrity.android.ui.tags.TagsActivity
import com.alexvt.integrity.android.ui.tags.TagsDependenciesModule
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

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
class UiDependenciesModule {
    @Provides
    @Singleton
    @Named("packageName")
    fun providePackageName() = "com.alexvt.integrity"

    @Provides
    @Singleton
    @Named("versionName")
    fun provideVersionName() = BuildConfig.VERSION_NAME

    @Provides
    @Singleton
    @Named("projectLink")
    fun provideProjectLink() = "https://github.com/alex-vt/Integrity/tree/develop" // todo update

    @Provides
    @Singleton
    @Named("destinationsScreenClass")
    fun provideDestinationsScreenClass() = DestinationsActivity::class.java.name

    @Provides
    @Singleton
    @Named("tagsScreenClass")
    fun provideTagsScreenClass() = TagsActivity::class.java.name

    @Provides
    @Singleton
    @Named("logScreenClass")
    fun provideLogScreenClass() = LogViewActivity::class.java.name

    @Provides
    @Singleton
    @Named("settingsScreenClass")
    fun provideSettingsScreenClass() = SettingsActivity::class.java.name

    @Provides
    @Singleton
    @Named("recoveryScreenClass")
    fun provideRecoveryScreenClass() = RecoveryActivity::class.java.name

    @Provides
    @Singleton
    @Named("helpInfoScreenClass")
    fun provideHelpInfoScreenClass() = HelpInfoActivity::class.java.name

    @Provides
    @Singleton
    @Named("legalInfoScreenClass")
    fun provideLegalInfoScreenClass() = LegalInfoActivity::class.java.name
}

class ViewModelFactory<VM : ViewModel>(
        private val viewModel: Provider<VM>
): ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return viewModel.get() as T
    }
}