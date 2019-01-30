/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.content.Context.BATTERY_SERVICE


object DeviceStateUtil {

    fun isOnWifi(context: Context): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        return if (wifiManager != null && wifiManager.isWifiEnabled) {
            wifiManager.connectionInfo.networkId != -1
        } else {
            false
        }
    }

    fun isBatteryChargeMoreThan(context: Context, minPercent: Int): Boolean {
        val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager?
        val batteryPercent = batteryManager!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batteryPercent > minPercent
    }

}