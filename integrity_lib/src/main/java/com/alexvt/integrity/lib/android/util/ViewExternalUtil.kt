/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.lib.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object ViewExternalUtil {

    fun viewLinkExternal(context: Context, link: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(link) // todo security
        context.startActivity(intent)
    }

    fun viewAppInfo(context: Context, packageName: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        context.startActivity(intent)
    }

}