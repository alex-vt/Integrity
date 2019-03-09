/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.types

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.JobIntentService
import com.alexvt.integrity.lib.metadata.Snapshot
import com.alexvt.integrity.lib.util.IntentUtil

/**
 * Resolves and starts service for downloading snapshot data according to snapshot data type.
 */
object DataTypeServiceResolver {

    fun startDownloadService(context: Context, dataFolderName: String, snapshot: Snapshot) {
        val serviceInfo = getDataTypeServiceInfo(context, snapshot.dataTypePackageName)
        val componentName = ComponentName(serviceInfo.packageName, serviceInfo.name)

        var intent = Intent()
        intent = IntentUtil.putSnapshot(intent, snapshot)
        intent = IntentUtil.putDataFolderName(intent, dataFolderName)
        intent.component = componentName

        JobIntentService.enqueueWork(context, componentName, 100, intent)
        android.util.Log.v("DataTypeServiceResolver", "JobIntentService enqueueWork $intent")
    }

    fun getDataTypeServiceInfo(context: Context, packageName: String) =
            getDataTypeServiceInfoList(context).first { packageName == it.packageName }

    private fun getDataTypeServiceInfoList(context: Context): List<ServiceInfo> {
        val intent = Intent()
        intent.action = "com.alexvt.integrity.SNAPSHOT_DOWNLOAD"
        val serviceInfoList = context.packageManager.queryIntentServices(
                intent, 0)
                .map { it.serviceInfo }
        android.util.Log.v("DataTypeServiceResolver", "getDataTypeServiceInfoList: $serviceInfoList")
        return serviceInfoList
    }

}