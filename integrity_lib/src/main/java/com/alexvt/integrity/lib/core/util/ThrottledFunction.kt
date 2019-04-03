/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.core.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule

/**
 * Holder for function invoked on request after the previous request "cools off".
 * Cooling off strategy depends on request type: ThrottleLatest or Debounce type in ReactiveX.
 */
class ThrottledFunction(private val coolingOffPeriodMillis: Long,
                        private val function: () -> Unit) {
    private var timer: TimerTask = Timer().schedule(0) {}

    /**
     * Requests debounced (Debounce in ReactiveX) function invocation.
     */
    fun requestDebounced() {
        delayInvocation(coolingOffPeriodMillis)
    }

    /**
     * Requests throttled-latest (ThrottleLatest in ReactiveX) function invocation.
     */
    fun requestThrottledLatest() {
        delayInvocation(getRemainingCoolOffTime())
    }


    private fun getRemainingCoolOffTime(): Long {
        val coolingOffEndTime = timer.scheduledExecutionTime()
        val timeNow = System.currentTimeMillis()
        val timeUntilCoolingOff = coolingOffEndTime - timeNow
        return if (timeUntilCoolingOff > 0) timeUntilCoolingOff else 0
    }

    private fun delayInvocation(delayMillis: Long) {
        timer.cancel()
        if (delayMillis == 0L) {
            invokeAndCoolOff()
        } else {
            timer = Timer().schedule(delayMillis) {
                invokeAndCoolOff()
            }
        }
    }

    private fun invokeAndCoolOff() {
        invoke()
        timer.cancel()
        timer = Timer().schedule(coolingOffPeriodMillis) {}
    }

    private fun invoke() {
        GlobalScope.launch (Dispatchers.Main) {
            function.invoke()
        }
    }
}