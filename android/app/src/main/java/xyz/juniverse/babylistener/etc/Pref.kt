package xyz.juniverse.babylistener.etc

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import xyz.juniverse.babylistener.detection.Detector

/**
 * Created by juniverse on 04/12/2017.
 */
class Pref(private val context: Context) {
    companion object Key {
        val enableUnexpWarningSMS = "key.enable.unexp.warning"
//        val enableWarningSMS = "key.enable.warning"
        val maxPauseTime = "key.wait.for.call.end"
        val phoneNumber = "key.phone.number"
        val sensitivity = "key.sensitivity"
        val init = "key.init"
        val stoppedByUser = "key.implicit.stop"
        val DNDWarned = "key.dnd.warned"
        val disableJoke = "key.disable.joke"
        val hideTutorial = "key.hide.tutorial"
        val smsCallCmd = "key.sms.call.cmd"
        val lastSmsCallCmd = "key.last.sms.call.cmd"
        val smsAllNoti = "key.sms.all.noti"
    }

    private val p
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    val e
        @SuppressLint("CommitPrefEdits")
        get() = PreferenceManager.getDefaultSharedPreferences(context).edit()

    private val longDefaults = HashMap<String, Long>()
    private val intDefaults = HashMap<String, Int>()
    private val stringDefaults = HashMap<String, String>()
    private val boolDefaults = HashMap<String, Boolean>()

    init {
        longDefaults.put(maxPauseTime, 60 * 1000)

        intDefaults.put(sensitivity, Detector.defaultValue)

        boolDefaults.put(init, true)
    }

    fun reset() {
        e.clear().apply()
    }

    fun remove(key: String) {
        e.remove(key).apply()
    }

    fun getLong(key: String): Long {
        return p.getLong(key, longDefaults[key] ?: 0)
    }

    fun getInt(key: String): Int {
        return p.getInt(key, intDefaults[key] ?: 0)
    }

    fun getString(key: String): String? {
        return p.getString(key, stringDefaults[key])
    }

    fun getBool(key: String): Boolean {
        return p.getBoolean(key, boolDefaults[key] ?: false)
    }

    fun putString(key: String, value: String) {
        e.putString(key, value).apply()
    }

    fun putBool(key: String, value: Boolean) {
        e.putBoolean(key, value).apply()
    }
}