/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.test.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.alexvt.integrity.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.alexvt.integrity.android.ui.splash.SplashScreenActivity


@RunWith(AndroidJUnit4::class)
class SplashScreenActivityLaunchTest {

    @get:Rule
    var activityRule = ActivityTestRule(SplashScreenActivity::class.java)

    @Test
    fun reachMainActivity() {
        onView(withId(R.id.svMain)).check(matches(isDisplayed()))
    }
}