package xyz.juniverse.babylistener.etc

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import xyz.juniverse.babylistener.R

/**
 * Created by juniverse on 04/12/2017.
 */

class AudioUtil(private val context: Context) {
    private var previousRingerMode: Int = -1
    fun setToMute() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                // warning
                Toast.makeText(context, R.string.warning_not_silent_mode, Toast.LENGTH_LONG).show()
            }
            return
        }

        previousRingerMode = audioManager.ringerMode
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
    }

    fun restore() {
        if (previousRingerMode < 0)
            return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = previousRingerMode
    }
}
