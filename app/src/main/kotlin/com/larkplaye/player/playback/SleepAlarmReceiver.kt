package com.larkplaye.player.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.larkplaye.player.core.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SleepAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PlaybackService.ACTION_SLEEP_ALARM) return
        val commands = PlaybackBridge.commands
        if (commands != null) {
            commands.pausePlayback()
            commands.cancelSleepTimer()
        } else {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { Prefs(context.applicationContext).setSleepTimerEnd(0L) }
                pendingResult.finish()
            }
        }
    }
}
