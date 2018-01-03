package xyz.juniverse.babylistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import xyz.juniverse.babylistener.etc.Pref
import xyz.juniverse.babylistener.etc.console
import xyz.juniverse.babylistener.etc.makeCall

/**
 * Created by juniverse on 11/12/2017.
 */
class CommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        console.d("got sms broadcast", intent.action)

        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val cmd = Pref(context).getString(Pref.smsCallCmd) ?: return

        val pdus = intent.extras?.get("pdus") as Array<*>
        val msgs = Array<SmsMessage>(pdus.size, {i -> SmsMessage.createFromPdu(pdus[i] as ByteArray) })
        for (msg in msgs) {
            if (msg.messageBody.contains(cmd)) {
                // make call.
                makeCall(context, msg.originatingAddress)
                break
            }
        }
    }
}