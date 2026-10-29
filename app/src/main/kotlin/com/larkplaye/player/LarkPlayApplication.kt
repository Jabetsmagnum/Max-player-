package com.larkplaye.player

import android.app.Application
import com.larkplaye.player.core.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LarkPlayApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            if (container.mediaStore.hasAudioPermission() || container.mediaStore.hasVideoPermission()) {
                container.mediaStore.refreshIfStale()
            }
        }
    }
}
