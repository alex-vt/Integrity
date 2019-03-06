/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.core.util

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import com.alexvt.integrity.lib.R

object SearchViewUtil {

    /**
     * Fixes default SearchView voice search icon background shape and visual artifacts.
     * todo improve and move
     */
    fun fixMicIconBackground(svMain: SearchView) {
        // Rounded mic icon background that doesn't extend outsize the rounded search view
        val ivMicButton: ImageView? = svMain.findViewById(R.id.search_voice_btn)
        if (ivMicButton != null) {
            ivMicButton.setBackgroundResource(R.drawable.rounded_padded_clickable)
        }
        // Removing the thin gray line visual artifact on the mic icon background
        val llSubmitArea: View? = svMain.findViewById(R.id.submit_area)
        if (llSubmitArea != null) {
            llSubmitArea.setBackgroundColor(Color.TRANSPARENT)
            llSubmitArea.setPadding(0, 0, 0, 0)
        }
    }

}