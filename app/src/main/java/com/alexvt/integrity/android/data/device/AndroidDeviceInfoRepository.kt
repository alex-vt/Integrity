/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.android.data.device

import android.content.Context
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.content.Context.BATTERY_SERVICE
import com.alexvt.integrity.core.data.device.DeviceInfoRepository
import javax.inject.Inject


class AndroidDeviceInfoRepository @Inject constructor(
        private val context: Context
) : DeviceInfoRepository {

    override fun isOnWifi(): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        return if (wifiManager != null && wifiManager.isWifiEnabled) {
            wifiManager.connectionInfo.networkId != -1
        } else {
            false
        }
    }

    override fun isBatteryChargeMoreThan(minPercent: Int): Boolean {
        val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager?
        val batteryPercent = batteryManager!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return batteryPercent > minPercent
    }

}