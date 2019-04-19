/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.test.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.alexvt.integrity.R
import com.alexvt.integrity.android.ui.main.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.alexvt.integrity.test.ui.EspressoViewActionUtil.typeSearchViewText
import com.alexvt.integrity.test.ui.EspressoViewActionUtil.waitFor
import org.hamcrest.core.IsNot.not


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun searchAndThenClear() {
        onView(withId(R.id.llFilters)).check(matches(not((isDisplayed()))))

        onView(withId(R.id.svMain)).perform(click())
        onView(withId(R.id.svMain)).perform(typeSearchViewText("test", false))

        onView(withText("test")).check(matches(withText("test"))) // todo SearchView text matcher
        onView(withId(R.id.llFilters)).check(matches(isDisplayed()))

        val searchDelayExceedingTimeMillis = 510L
        onView(isRoot()).perform(waitFor(searchDelayExceedingTimeMillis))
        onView(withId(R.id.svMain)).perform(typeSearchViewText("", false))

        onView(isRoot()).perform(waitFor(searchDelayExceedingTimeMillis))
        onView(withId(R.id.llFilters)).check(matches(not((isDisplayed()))))
    }
}