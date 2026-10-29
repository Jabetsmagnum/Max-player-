package com.larkplaye.player.lyrics

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.larkplaye.player.LarkPlayApplication
import com.larkplaye.player.MainActivity
import com.larkplaye.player.R
import com.larkplaye.player.core.model.ParsedLyrics
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class LyricsOverlayService : android.app.Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private var overlayView: TextView? = null
    private var lastNotificationTrackId: Long? = null
    private var started = false

    private val app: LarkPlayApplication
        get() = application as LarkPlayApplication

    private val lyricsFlow: Flow<ParsedLyrics?> by lazy {
        app.container.playerConnection.state
            .map { it.current }
            .distinctUntilChangedBy { it?.id }
            .flatMapLatest { track ->
                if (track == null) {
                    flowOf(null)
                } else {
                    app.container.lyrics.observe(track.id).onStart {
                        emit(app.container.lyrics.load(track))
                    }
                }
            }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        startForegroundSafely(buildNotification(null, null))
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        addOverlay()
        started = true
        observePlayback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch { runCatching { app.container.prefs.setFloatingLyrics(false) } }
            stopSelf()
            return START_NOT_STICKY
        }
        if (!started && Settings.canDrawOverlays(this)) {
            addOverlay()
            started = true
            observePlayback()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    private fun observePlayback() {
        scope.launch {
            combine(
                app.container.playerConnection.state,
                lyricsFlow,
                app.container.prefs.flow,
            ) { state, lyrics, prefs ->
                OverlayState(state, lyrics, prefs.lyricsFontScale, prefs.lyricsCentered)
            }.collect { overlay ->
                render(overlay)
            }
        }
    }

    private fun render(overlay: OverlayState) {
        val track = overlay.state.current
        val view = overlayView ?: return
        view.post {
            view.textSize = 20f * overlay.fontScale.coerceIn(0.7f, 1.8f)
            view.gravity = if (overlay.centered) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
            val text = buildText(track, overlay.lyrics, overlay.state.positionMs)
            if (view.text != text) view.text = text
        }
        if (track?.id != lastNotificationTrackId) {
            lastNotificationTrackId = track?.id
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(track, overlay.state))
        }
    }

    private fun buildText(track: Track?, lyrics: ParsedLyrics?, positionMs: Long): String {
        if (track == null) return getString(R.string.app_name)
        if (lyrics != null && lyrics.synced && lyrics.lines.isNotEmpty()) {
            val index = lyrics.indexAt(positionMs)
            if (index >= 0) {
                val line = lyrics.lines[index].text
                if (line.isNotBlank()) return line
            }
            val next = lyrics.lines.firstOrNull { it.timeMs > positionMs && it.text.isNotBlank() }
            if (next != null) return next.text
        }
        return "${track.title} — ${track.artist}"
    }

    private fun addOverlay() {
        if (overlayView != null) return
        val view = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            maxWidth = resources.displayMetrics.widthPixels - dp(32)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.argb(210, 12, 13, 18))
                setStroke(dp(1), Color.argb(70, 255, 213, 74))
            }
            text = getString(R.string.floating_lyrics)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(96)
        }
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > dp(6) || kotlin.math.abs(dy) > dp(6)) moved = true
                        val metrics = resources.displayMetrics
                        params.x = (initialX + dx)
                            .coerceIn(0, (metrics.widthPixels - v.width).coerceAtLeast(0))
                        params.y = (initialY + dy)
                            .coerceIn(0, (metrics.heightPixels - v.height).coerceAtLeast(0))
                        runCatching { windowManager.updateViewLayout(view, params) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) openApp()
                        return true
                    }
                }
                return false
            }
        })
        runCatching { windowManager.addView(view, params) }
        overlayView = view
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        runCatching { startActivity(intent) }
    }

    private fun startForegroundSafely(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(track: Track?, state: PlayerState?): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            30,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            31,
            Intent(this, LyricsOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_player)
            .setContentTitle(getString(R.string.floating_lyrics))
            .setContentText(track?.let { "${it.title} — ${it.artist}" } ?: getString(R.string.app_name))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.disable_overlay), stopIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.floating_lyrics),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.floating_lyrics_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        scope.cancel()
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        super.onDestroy()
    }

    private data class OverlayState(
        val state: PlayerState,
        val lyrics: ParsedLyrics?,
        val fontScale: Float,
        val centered: Boolean,
    )

    companion object {
        private const val CHANNEL_ID = "floating_lyrics"
        private const val NOTIFICATION_ID = 2134
        const val ACTION_STOP = "com.larkplaye.player.LYRICS_OVERLAY_STOP"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, LyricsOverlayService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, LyricsOverlayService::class.java))
            }
        }
    }
}
