/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.type

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.alexvt.integrity.lib.Snapshot
import com.alexvt.integrity.lib.util.IntentUtil

/**
 * Resolves and sends broadcasts for actions on snapshot data according to snapshot data type.
 */
object BroadcastUtil {

    fun sendBroadcast(context: Context, dataFolderName: String, snapshot: Snapshot, action: String) {
        val broadcastInfo = getBroadcastReceiverInfo(context, action, snapshot.dataTypePackageName)
        val componentName = ComponentName(broadcastInfo.activityInfo.packageName,
                broadcastInfo.activityInfo.name)

        val intent = Intent(action).apply {
            IntentUtil.putDataFolderName(this, dataFolderName)
            IntentUtil.putSnapshot(this, snapshot)
            component = componentName
        }

        context.sendBroadcast(intent)
    }

    private fun getBroadcastReceiverInfo(context: Context, action: String, packageName: String) =
            getBroadcastReceiverInfoList(context, action)
                    .first { it.activityInfo.packageName == packageName }

    private fun getBroadcastReceiverInfoList(context: Context, action: String): List<ResolveInfo> {
        val infoList = context.packageManager.queryBroadcastReceivers(Intent(action), 0)
        android.util.Log.v("BroadcastUtil", "getBroadcastReceiverInfoList: $infoList")
        return infoList
    }

}