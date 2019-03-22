/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor

/**
 * Manages reactive requests,
 * each of which has a Flowable that emits certain data computed by dataSource function
 * when emitCurrentData with is called.
 *
 *
 * Example of usage:
 *
 * 1. Create a request pool for your input type:
 * val reactiveRequestPool = ReactiveRequestPool()
 *
 * 2. Declare flowables with dataSource functions:
 * val listSizeFlowable: Flowable<Int> = reactiveRequestPool.add { myMutableList.size }
 *
 * 3. When you want to emit current data from the dataSource, call like this:
 * reactiveRequestPool.emitCurrentDataAll()
 *
 * 4. Subscribers of listSize will get the corresponding data updates.
 * Note: when subscribing to a flowable, it will emit the dataSource result as the first value.
 */

class ReactiveRequestPool {

    inner class ReactiveRequest<D>(
            val publishProcessor: BehaviorProcessor<D> = BehaviorProcessor.create(),
            private val dataSource: () -> D
    ) {
        fun emitCurrentData() {
            android.util.Log.v("ReactiveRequestPool", "Emitting result of: $dataSource")
            publishProcessor.onNext(dataSource.invoke())
        }
    }

    private var reactiveRequests: List<ReactiveRequest<out Any?>> = emptyList()

    fun <O> add(outputSource: () -> O): Flowable<O> {
        cleanupUnsubscribed()
        val reactiveRequest: ReactiveRequest<O> = ReactiveRequest(BehaviorProcessor.create(), outputSource)
        reactiveRequests = reactiveRequests.plus(reactiveRequest)
        reactiveRequest.emitCurrentData() // seeding the new flowable with the first value.
        android.util.Log.v("ReactiveRequestPool", "Added request, total: ${reactiveRequests.size}")
        return reactiveRequest.publishProcessor
    }

    fun emitCurrentDataAll() {
        cleanupUnsubscribed()
        reactiveRequests.forEach { it.emitCurrentData() }
        android.util.Log.v("ReactiveRequestPool", "Emitted for all requests, total: ${reactiveRequests.size}")
    }

    private fun cleanupUnsubscribed() {
        reactiveRequests = reactiveRequests.filter { it.publishProcessor.hasSubscribers() }
    }
}