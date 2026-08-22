package com.example.dynamicisland.overlay

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * Computes where the Island should sit at the physical top of the display,
 * accounting for the device's actual cutout (punch hole / notch / none).
 *
 * IMPORTANT: this intentionally does NOT add the status bar height as an offset.
 * The overlay window itself is placed with gravity=TOP and a minimal top margin so it
 * occupies the same physical region as the system status bar, per spec. Whether Android
 * actually lets the overlay's pixels draw above the status bar's own icons varies by
 * OEM/version — where the OS clips us we fall back to the topmost offset it does allow.
 */
class IslandPositionManager(private val context: Context) {

    data class Placement(
        val topOffsetPx: Int,
        val recommendedWidthPx: Int,
        val cutoutCenterX: Int?
    )

    fun computePlacement(): Placement {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics: DisplayMetrics = context.resources.displayMetrics

        var topOffset = (4 * metrics.density).toInt() // minimal offset, not statusBarHeight
        var cutoutCenterX: Int? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bounds: Rect = wm.currentWindowMetrics.bounds
            val insets = wm.currentWindowMetrics.windowInsets
            val cutout = insets.displayCutout
            if (cutout != null) {
                val boundingRects = cutout.boundingRects
                if (boundingRects.isNotEmpty()) {
                    val r = boundingRects[0]
                    cutoutCenterX = r.centerX()
                    // Sit just at/around the cutout's own top offset rather than pushing
                    // further down — keeps us at the true physical top.
                    topOffset = minOf(topOffset, r.top.coerceAtLeast(0))
                }
            }
            val width = bounds.width()
            return Placement(
                topOffsetPx = topOffset,
                recommendedWidthPx = (width * 0.34f).toInt().coerceAtLeast((120 * metrics.density).toInt()),
                cutoutCenterX = cutoutCenterX
            )
        }

        return Placement(
            topOffsetPx = topOffset,
            recommendedWidthPx = (140 * metrics.density).toInt(),
            cutoutCenterX = null
        )
    }
}
