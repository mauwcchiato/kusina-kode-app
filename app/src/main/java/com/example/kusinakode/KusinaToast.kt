package com.example.kusinakode

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * In-app toast that wears the Kusina Kode mark instead of the Android robot.
 *
 * Custom toast views only draw while the app is in the foreground, which is
 * when these messages fire (shop, board, settings).
 */
object KusinaToast {

    fun show(context: Context, message: String, long: Boolean = false) {
        val ctx = context.applicationContext
        val density = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.toast_kusina_bg)
            setPadding(dp(14), dp(10), dp(16), dp(10))
            elevation = 8f * density
        }
        val logo = ImageView(ctx).apply {
            setImageResource(R.drawable.kk_logo)
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                marginEnd = dp(10)
            }
        }
        val text = TextView(ctx).apply {
            this.text = message
            setTextColor(Color.parseColor("#3E2723"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            maxWidth = dp(260)
        }
        row.addView(logo)
        row.addView(text)

        Toast(ctx).apply {
            duration = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            view = row
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(72))
            show()
        }
    }
}
