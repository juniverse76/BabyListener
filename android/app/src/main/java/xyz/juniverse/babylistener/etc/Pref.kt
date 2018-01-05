package xyz.juniverse.babylistener.etc

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import xyz.juniverse.babylistener.detection.Detector

/**
 * Created by juniverse on 04/12/2017.
 */
class Pref(/*private val context: Context*/) {
    companion object {
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

        val appMode = "key.app.mode"
        val partnerId = "key.partner.id"






        private lateinit var p: SharedPreferences
        val edit
            get() = p.edit()

        private val longDefaults = HashMap<String, Long>()
        private val intDefaults = HashMap<String, Int>()
        private val stringDefaults = HashMap<String, String>()
        private val boolDefaults = HashMap<String, Boolean>()

        init {
            longDefaults.put(maxPauseTime, 60 * 1000)
            intDefaults.put(sensitivity, Detector.defaultValue)
            boolDefaults.put(init, true)
        }

        fun setContext(context: Context) {
            p = PreferenceManager.getDefaultSharedPreferences(context)
        }

        fun getLong(key: String): Long = p.getLong(key, longDefaults[key] ?: 0)
        fun getInt(key: String): Int = p.getInt(key, intDefaults[key] ?: 0)
        fun getString(key: String): String? = p.getString(key, stringDefaults[key])
        fun getBool(key: String): Boolean = p.getBoolean(key, boolDefaults[key] ?: false)

        fun putString(key: String, value: String) = p.edit().putString(key, value).apply()
        fun putBool(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()

        fun reset() = p.edit().clear().apply()
        fun remove(key: String) = p.edit().remove(key).apply()
    }
}