/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.data.types

/**
 * Repository of snapshot data types.
 */
interface DataTypeRepository {

    fun getDataType(dataTypeClassName: String): DataType

    /**
     * Gets list of available data types sorted by simple name.
     */
    fun getAllDataTypes(): List<DataType>
}