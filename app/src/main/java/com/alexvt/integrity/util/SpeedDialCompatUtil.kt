/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.alexvt.integrity.util

import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import com.leinardi.android.speeddial.SpeedDialView

object SpeedDialCompatUtil {

    /**
     * Enforces SpeedDialView anchored to other view's top to stay in place on expand click.
     *
     * See https://github.com/leinardi/FloatingActionButtonSpeedDial/issues/57 (outdated solution)
     * todo fix or replace the lib instead
     */
    fun setStayOnExpand(sd: SpeedDialView) {
        sd.setOnChangeListener(object: SpeedDialView.OnChangeListener {
            override fun onMainActionSelected(): Boolean = false

            override fun onToggleChanged(isOpen: Boolean) = onToggleChanged(sd, isOpen)
        })
    }

    private fun onToggleChanged(sd: SpeedDialView, isOpen: Boolean) {
        val params = sd.layoutParams as CoordinatorLayout.LayoutParams
        // we're not anchored to the top, no need to change translation
        if (params.anchorGravity and Gravity.TOP != Gravity.TOP)  {
            return
        }

        if (isOpen) {
            logViewY("opening", sd)
            val nonExpandedHeight = sd.height

            sd.doOnNextLayout {
                logViewY("opening-nextLayout", sd)
                val expandedHeight = sd.height
                // Warning: multiplier obtained experimentally. Todo test
                val diff = (expandedHeight - nonExpandedHeight) / 4f
                sd.translationY = diff
            }
        } else {
            logViewY("closing", sd)
            // when menu is closed, reset the translation
            sd.doOnNextLayout {
                logViewY("closing-nextLayout", sd)
                sd.translationY = 0f
            }
        }
    }

    private fun logViewY(tag: String, view: View) {
        android.util.Log.v(SpeedDialCompatUtil::class.java.simpleName,
                "action=$tag, top=${view.top}, height=${view.height}, " +
                        "bottom=${view.bottom}, translationY=${view.translationY}")
    }


}