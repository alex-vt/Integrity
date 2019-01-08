/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.util.Log
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Serializable object to and from JSON string
 */
object JsonSerializerUtil {

    private val TAG = JsonSerializerUtil::class.java.simpleName

    private lateinit var objectMapper: ObjectMapper

    private fun getMapper(): ObjectMapper {
        if (!this::objectMapper.isInitialized) {
            objectMapper = jacksonObjectMapper()
            objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                    JsonTypeInfo.As.WRAPPER_OBJECT)
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
            // performance optimization: see https://github.com/FasterXML/jackson-module-kotlin/issues/69
            objectMapper.configure(MapperFeature.USE_ANNOTATIONS, false)
            // allowing to ignore properties when Jackson annotations are disabled. Also improves init time
            objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
        }
        return objectMapper
    }

    fun toJson(`object`: Any): String? {
        try {
            val timestamp = System.currentTimeMillis()
            val returnValue = getMapper().writeValueAsString(`object`)
            Log.d(TAG, "Serialization took " + (System.currentTimeMillis() - timestamp) + " millis")
            return returnValue
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to serialize to JSON", e)
            return null
        }
    }

    fun <T> fromJson(jsonString: String?, type: Class<T>): T? {
        try {
            val timestamp = System.currentTimeMillis()
            val returnValue = getMapper().readValue(jsonString, type)
            Log.d(TAG, "Deserialization took " + (System.currentTimeMillis() - timestamp) + " millis")
            return returnValue
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to deserialize to " + type.simpleName, e)
            return null
        }

    }
}
