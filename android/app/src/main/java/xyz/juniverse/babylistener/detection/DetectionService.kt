package xyz.juniverse.babylistener.detection

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import xyz.juniverse.babylistener.R
import xyz.juniverse.babylistener.ui.CallModeActivity
import xyz.juniverse.babylistener.etc.*
import xyz.juniverse.babylistener.firebase.Report


class DetectionService : Service() {
    companion object {
        val ACTION_STATUS = "detection.service.action.status"
        val ACTION_STATUS_REQ = "detection.service.action.status.req"

        val KEY_SENSITIVITY = "detection.service.key.sensitivity"
        val KEY_NUMBER = "detection.service.key.number"
        val KEY_RUNNING_STATUS = "detection.service.key.status"
    }

//    class ServiceBinder constructor(service: DetectionService) : Binder() {
//        var serviceInstance: DetectionService? = null
//
//        init {
//            serviceInstance = service
//        }
//    }

    override fun onBind(intent: Intent): IBinder? {
//        console.d("onBind", detector)
//        return ServiceBinder(this)
        return null
    }

    private val requestListener = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STATUS_REQ) {
                console.d("received ACTION_STATUS_REQ")
                val reqIntent = Intent(ACTION_STATUS)
                reqIntent.putExtra(KEY_RUNNING_STATUS, detector.state)
                sendBroadcast(reqIntent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(requestListener, IntentFilter(ACTION_STATUS_REQ))

        audioUtil = AudioUtil(baseContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId)
        console.d("onStartCommand")
        if (intent != null)
            startDetection(intent.extras.getInt(KEY_SENSITIVITY), intent.extras.getString(KEY_NUMBER))

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(requestListener)
        stopDetection()
    }

    private var detector = Detector()
    private lateinit var audioUtil: AudioUtil

    init {
        detector.setDetectorInterface(
                onStateChanged = { state ->
                    console.d("sending status", state)
                    val intent = Intent(ACTION_STATUS)
                    intent.putExtra(KEY_RUNNING_STATUS, state)
                    sendBroadcast(intent)

                    // stop was not expected!!! notify user....
                    if (state == Detector.State.STOPPED) {
                        val byUser = Pref.getBool(Pref.stoppedByUser)
                        if (!byUser)
                            SmsManager.getDefault().sendTextMessage(targetNumber, null, getString(R.string.sms_warning_unexpected_stop), null, null)
                        Report.event(baseContext, Report.E.DETECTION_STOP, arrayOf(Pair("by_user", byUser.toString())))
                    }
                },
                onDetecting = { _ ->
                },
                onDetected = {
                    // in call mode
                    inManualCallMode = true
                    makeCall(baseContext, targetNumber)
                })
    }

    private var targetNumber: String = ""
    private var inManualCallMode: Boolean = false

    private fun startDetection(sensitivity: Int, number: String) {
        targetNumber = number
        inManualCallMode = false
        detector.sensitivity = sensitivity
        detector.start()

        showNotification()
        listenForCallState(true)
        audioUtil.setToMute()

        // saving detector's sensitivity NOT the UI sensitivity
        Report.event(baseContext, Report.E.DETECTION_START, arrayOf(Pair("sensitivity", detector.sensitivity.toString())))
    }

    private fun stopDetection() {
        detector.stop()
        hideNotification()
        audioUtil.restore()
        listenForCallState(false)
    }

    private val phoneStateListener = object: PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                console.d("is idle. should resume")
                // restarting... remove warning.
                inManualCallMode = false
                if (detector.resume() && Pref.getBool(Pref.smsAllNoti))
                    SmsManager.getDefault().sendTextMessage(targetNumber, null, getString(R.string.sms_warning_resumed), null, null)
            } else {
                console.d("is NOT idle!!!")
                if (!inManualCallMode) {
                    // detector didn't make the call... incoming call!!! pause it for now...
                    detector.pause()
                    if (Pref.getBool(Pref.smsAllNoti))
                        SmsManager.getDefault().sendTextMessage(targetNumber, null, getString(R.string.sms_warning_paused_by_call), null, null)
                }
            }
        }
    }

    private fun listenForCallState(listen: Boolean) {
        val telephonyManager : TelephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val flag = if (listen) PhoneStateListener.LISTEN_CALL_STATE else PhoneStateListener.LISTEN_NONE
        telephonyManager.listen(phoneStateListener, flag)
    }

    private val notificationId = 84721
    private fun showNotification() {
        val builder = NotificationCompat.Builder(baseContext, "babylistener")
        builder.setSmallIcon(R.mipmap.ic_launcher).setContentTitle(getString(R.string.notification_title)).setContentText(getString(R.string.notification_msg))

        val intent = Intent(baseContext, CallModeActivity::class.java)
//        intent.putExtra(BabyListener.KEY_STOP_DETECT, true)
        builder.setContentIntent(PendingIntent.getActivity(baseContext, 0, intent, 0))
        builder.setOngoing(true)

        val notificationManager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private fun hideNotification() {
        val notificationManager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

}
