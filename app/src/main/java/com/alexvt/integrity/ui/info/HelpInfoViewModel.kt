/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.ui.info

import com.alexvt.integrity.core.settings.SettingsRepository
import com.alexvt.integrity.ui.ThemedViewModel
import com.alexvt.integrity.ui.util.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

data class NavigationEvent(
        val goBack: Boolean = false,
        val viewLink: String = "",
        val targetPackage: String = "",
        val targetClass: String = ""
)

class HelpInfoViewModel @Inject constructor(
        @Named("packageName") val packageName: String,
        @Named("versionName") val versionName: String,
        @Named("projectLink") val projectLink: String,
        override val settingsRepository: SettingsRepository,
        @Named("settingsScreenClass") val settingsScreenClass: String,
        @Named("recoveryScreenClass") val recoveryScreenClass: String,
        @Named("legalInfoScreenClass") val legalInfoScreenClass: String
        ) : ThemedViewModel() {

    // single events
    val navigationEventData = SingleLiveEvent<NavigationEvent>()


    // navigation actions

    fun viewSettings() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = settingsScreenClass)
    }

    fun viewRecovery() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = recoveryScreenClass)
    }

    fun viewLegal() {
        navigationEventData.value = NavigationEvent(targetPackage = packageName,
                targetClass = legalInfoScreenClass)
    }

    fun viewProjectLink() {
        navigationEventData.value = NavigationEvent(viewLink = projectLink)
    }


    // back

    fun pressBackButton() {
        navigationEventData.value = NavigationEvent(goBack = true)
    }

}
