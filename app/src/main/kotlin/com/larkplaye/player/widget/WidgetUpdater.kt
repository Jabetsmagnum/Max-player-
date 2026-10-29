package com.larkplaye.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.larkplaye.player.MainActivity
import com.larkplaye.player.R
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.playback.PlaybackService

object WidgetUpdater {
    fun update(context: Context, state: PlayerState) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, PlayerWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        ids.forEach { id ->
            manager.updateAppWidget(id, buildViews(context, state))
        }
    }

    private fun buildViews(context: Context, state: PlayerState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_player)
        val track = state.current
        views.setTextViewText(
            R.id.widget_title,
            track?.title ?: context.getString(R.string.app_name),
        )
        views.setTextViewText(
            R.id.widget_artist,
            track?.displayArtist ?: context.getString(R.string.widget_description),
        )
        views.setTextViewText(
            R.id.widget_toggle,
            if (state.isPlaying) "❚❚" else "▶",
        )

        val openIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_art_frame, openIntent)
        views.setOnClickPendingIntent(R.id.widget_title, openIntent)
        views.setOnClickPendingIntent(R.id.widget_artist, openIntent)

        views.setOnClickPendingIntent(
            R.id.widget_toggle,
            servicePending(context, PlaybackService.ACTION_WIDGET_TOGGLE, 11),
        )
        views.setOnClickPendingIntent(
            R.id.widget_next,
            servicePending(context, PlaybackService.ACTION_WIDGET_NEXT, 12),
        )
        views.setOnClickPendingIntent(
            R.id.widget_prev,
            servicePending(context, PlaybackService.ACTION_WIDGET_PREVIOUS, 13),
        )
        return views
    }

    private fun servicePending(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
