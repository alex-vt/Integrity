/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.ui.info

import com.alexvt.integrity.core.data.settings.SettingsRepository
import com.alexvt.integrity.android.ui.common.RxAutoDisposeThemedViewModel
import com.alexvt.integrity.android.ui.common.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

data class LegalInfoNavigationEvent(
        val goBack: Boolean = false,
        val dialogTitle: String = "",
        val dialogText: String = ""
)

class LegalInfoViewModel @Inject constructor(
        @Named("termsTitle") val termsTitle: String,
        @Named("termsText") val termsText: String,
        @Named("privacyPolicyTitle") val privacyPolicyTitle: String,
        @Named("privacyPolicyText") val privacyPolicyText: String,
        override val settingsRepository: SettingsRepository
        ) : RxAutoDisposeThemedViewModel() {

    // single events
    val navigationEventData = SingleLiveEvent<LegalInfoNavigationEvent>()


    // navigation actions

    fun viewTerms() {
        navigationEventData.value = LegalInfoNavigationEvent(dialogTitle = termsTitle,
                dialogText = termsText)
    }

    fun viewPrivacyPolicy() {
        navigationEventData.value = LegalInfoNavigationEvent(dialogTitle = privacyPolicyTitle,
                dialogText = privacyPolicyText)
    }


    // back

    fun pressBackButton() {
        navigationEventData.value = LegalInfoNavigationEvent(goBack = true)
    }

}
