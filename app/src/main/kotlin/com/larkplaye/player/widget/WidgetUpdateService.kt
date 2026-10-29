package com.larkplaye.player.widget

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.larkplaye.player.playback.PlaybackBridge

class WidgetUpdateService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        WidgetUpdater.update(this, PlaybackBridge.playerState.value)
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
