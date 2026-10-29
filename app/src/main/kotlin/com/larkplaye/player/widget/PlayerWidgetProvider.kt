package com.larkplaye.player.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.larkplaye.player.playback.PlaybackBridge

class PlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        WidgetUpdater.update(context, PlaybackBridge.playerState.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, ACTION_TOGGLE, ACTION_NEXT, ACTION_PREVIOUS -> {
                WidgetUpdater.update(context, PlaybackBridge.playerState.value)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.larkplaye.player.WIDGET_REFRESH"
        const val ACTION_TOGGLE = "com.larkplaye.player.WIDGET_TOGGLE"
        const val ACTION_NEXT = "com.larkplaye.player.WIDGET_NEXT"
        const val ACTION_PREVIOUS = "com.larkplaye.player.WIDGET_PREV"
    }
}
