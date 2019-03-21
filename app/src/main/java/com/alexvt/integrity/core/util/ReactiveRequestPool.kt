/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

/**
 * Manages reactive requests,
 * each of which has a Flowable that emits certain data computed by inputToOutput function
 * when emitForInput with inputData is called.
 *
 *
 * Example of usage:
 *
 * 1. Create a request pool for your input type:
 * val reactiveRequestPool = ReactiveRequestPool<List<String>>()
 *
 * 2. Declare flowables with input to output functions:
 * val textCountFlowable: Flowable<Int> = reactiveRequestPool.add { it.size }
 *
 * 3. When you want to update your input data, call like this:
 * reactiveRequestPool.emitForInput(listOf("one", "two"))
 *
 * 4. Subscribers of textCountFlowable will get the corresponding output data updates.
 */

class ReactiveRequestPool<I> {

    inner class ReactiveRequest<O>(
            val publishProcessor: PublishProcessor<O> = PublishProcessor.create(),
            private val inputToOutput: (I) -> O
    ) {
        fun emitForInput(inputData: I) {
            android.util.Log.v("ReactiveRequestPool", "Emitting result of: $inputToOutput")
            publishProcessor.onNext(inputToOutput.invoke(inputData))
        }
    }

    private var reactiveRequests: List<ReactiveRequest<out Any?>> = emptyList()

    fun <O> add(inputToOutput: (I) -> O): Flowable<O> {
        cleanupOldRequests()
        val reactiveRequest: ReactiveRequest<O> = ReactiveRequest(PublishProcessor.create(), inputToOutput)
        reactiveRequests = reactiveRequests.plus(reactiveRequest)
        return reactiveRequest.publishProcessor
    }

    fun emitForInput(inputData: I) {
        cleanupOldRequests()
        android.util.Log.v("ReactiveRequestPool", "Subscribers: ${reactiveRequests.size}")
        reactiveRequests.forEach { it.emitForInput(inputData) }
    }

    private fun cleanupOldRequests() {
        reactiveRequests = reactiveRequests.filter { it.publishProcessor.hasSubscribers() }
    }
}