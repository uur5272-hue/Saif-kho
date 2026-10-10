package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.example.MainActivity
import com.example.R

class MotoAiOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.example.motoai.action.START_OVERLAY"
        const val ACTION_STOP = "com.example.motoai.action.STOP_OVERLAY"
        var isOverlayRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingButton() {
        if (floatingView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = (56 * resources.displayMetrics.density).toInt()

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        val imageView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xCC0D1220.toInt())
                setStroke((2 * resources.displayMetrics.density).toInt(), 0xFF00E5FF.toInt())
            }
            val padding = (8 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        imageView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(imageView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        // Launch MOTO AI Assistant
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(imageView, params)
            floatingView = imageView
            isOverlayRunning = true
        } catch (e: Exception) {
            isOverlayRunning = false
        }
    }

    override fun onDestroy() {
        isOverlayRunning = false
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {}
            floatingView = null
        }
        super.onDestroy()
    }
}
