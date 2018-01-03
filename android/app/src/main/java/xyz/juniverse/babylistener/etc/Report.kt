package xyz.juniverse.babylistener.etc

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import xyz.juniverse.babylistener.BuildConfig

/**
 * Created by juniverse on 11/12/2017.
 *
 * Firebase event 리포트
 */
class Report {
    class E {
        companion object {
            val DETECTION_START = "bl_detection_start"
            val DETECTION_STOP = "bl_detection_stop"
            val AD_SHOW = "bl_add_shown"
        }
    }

    class P {
        companion object {
            val STATUS = "status"
        }
    }

    companion object {
        fun event(context: Context, event: String, params: Array<Pair<String, String>>? = null) {
            val bundle = Bundle()
            if (params != null)
                for (pair in params)
                    bundle.putString(pair.first, pair.second)

            if (!BuildConfig.DEBUG)
                FirebaseAnalytics.getInstance(context).logEvent(event, bundle)
            else
                console.i("sending event:", event, bundle)
        }

        fun event(context: Context, event: String, key: String, value: String) {
            event(context, event, arrayOf(Pair(key, value)))
        }
    }
}