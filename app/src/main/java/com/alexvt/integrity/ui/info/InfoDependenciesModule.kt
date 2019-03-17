/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.info

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.alexvt.integrity.ui.ViewModelFactory
import com.alexvt.integrity.R
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named
import javax.inject.Provider


@Module
abstract class InfoDependenciesModule {

    @ContributesAndroidInjector(modules = [HelpViewModelFactoryModule::class])
    abstract fun bindHelpInfoActivity(): HelpInfoActivity

    @ContributesAndroidInjector(modules = [HelpViewModelFactoryModule::class])
    abstract fun bindHelpInfoFragment(): HelpInfoSettingsFragment

    @Module
    class HelpViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<HelpInfoViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @ContributesAndroidInjector(modules = [LegalViewModelFactoryModule::class, ResourcesModule::class])
    abstract fun bindLegalInfoActivity(): LegalInfoActivity

    @ContributesAndroidInjector(modules = [LegalViewModelFactoryModule::class, ResourcesModule::class])
    abstract fun bindLegalInfoFragment(): LegalInfoSettingsFragment

    @Module
    class LegalViewModelFactoryModule {
        @Provides
        fun providesVmFactory(vm: Provider<LegalInfoViewModel>): ViewModelProvider.Factory = ViewModelFactory(vm)
    }

    @Module
    class ResourcesModule {
        @Provides
        @Named("termsTitle")
        fun providesTermsTitle(context: Context) = "Terms & Conditions" // todo from resources

        @Provides
        @Named("termsText")
        fun providesTermsText(context: Context) = getTextFromRawResource(context, R.raw.license)

        @Provides
        @Named("privacyPolicyTitle")
        fun providesPrivacyPolicyTitle(context: Context) = "Privacy Policy" // todo from resources

        @Provides
        @Named("privacyPolicyText")
        fun providesPrivacyPolicyText(context: Context) = getTextFromRawResource(context, R.raw.privacy)

        private fun getTextFromRawResource(context: Context, resId: Int): String {
            val inputStream = context.resources.openRawResource(resId)
            val bytes = ByteArray(inputStream.available())
            inputStream.read(bytes)
            return String(bytes)
        }
    }
}

