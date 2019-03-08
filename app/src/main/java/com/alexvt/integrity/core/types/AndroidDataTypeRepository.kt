/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.types

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.alexvt.integrity.lib.types.DataTypeServiceResolver

class AndroidDataTypeRepository(private val context: Context) : DataTypeRepository {

    override fun getDataType(dataTypeClassName: String) =
            getDataTypeViewerComponents().first {
                dataTypeClassName.substringAfterLast(".").removeSuffix("TypeMetadata") ==
                        getViewerTitle(it.className)
            }.let { getDataType(it) }

    override fun getAllDataTypes() = getDataTypeViewerComponents()
            .map { getDataType(it) }

    private fun getDataType(component: ComponentName) = with (component) {
        DataType(getViewerTitle(className), packageName, className,
                DataTypeServiceResolver.getDataTypeServiceInfo(context, packageName).name)
    }

    // todo title from resources
    private fun getViewerTitle(viewerClassName: String)
            = viewerClassName.substringAfterLast(".").removeSuffix("TypeActivity")

    /**
     * Gets list of component names of available data types sorted by simple name.
     */
    private fun getDataTypeViewerComponents() =
            context.packageManager.queryIntentActivities(
                    Intent("com.alexvt.integrity.ACTION_VIEW"), 0)
                    .filter { it?.activityInfo != null }
                    .map { it.activityInfo }
                    .map { ComponentName(it.packageName, it.name) }
                    .sortedBy { it.className.substringAfterLast(".") } // sorted by simple name
}
