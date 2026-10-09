package com.pahlavan.gamebooster

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class FloatingWidgetService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (bubble != null) return START_STICKY
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = "PGX"; textSize = 14f; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(0,121,107)); gravity = Gravity.CENTER
            setPadding(16,12,16,12)
        }
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 180 }
        var downX = 0; var downY = 0; var startX = 0; var startY = 0
        view.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> { downX=event.rawX.toInt(); downY=event.rawY.toInt(); startX=params.x; startY=params.y; true }
                MotionEvent.ACTION_MOVE -> { params.x=startX+event.rawX.toInt()-downX; params.y=startY+event.rawY.toInt()-downY; try { wm.updateViewLayout(view,params) } catch (_: Exception) {}; true }
                MotionEvent.ACTION_UP -> { if (kotlin.math.abs(event.rawX.toInt()-downX)<8 && kotlin.math.abs(event.rawY.toInt()-downY)<8) Toast.makeText(this,"Pahlavan Booster X • برای پایش کامل برنامه را باز کن",Toast.LENGTH_SHORT).show(); true }
                else -> true
            }
        }
        try { wm.addView(view, params); bubble=view } catch (_: Exception) { stopSelf() }
        return START_STICKY
    }
    override fun onDestroy() { bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }; bubble=null; super.onDestroy() }
}
