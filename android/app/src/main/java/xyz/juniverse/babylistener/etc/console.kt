package xyz.juniverse.babylistener.etc

import android.util.Log
import android.widget.TextView

/**
 * Created by juniverse on 22/11/2017.
 *
 * logger in Kotlin
 */
class console {
    companion object {
        var TAG: String = "console"

        var enable: Boolean = false

        fun d(vararg args: Any?) {
            if (!enable) return
            Log.d(TAG, makeLogText(*args))
        }

        fun d(textView: TextView, vararg args: Any?) {
            if (!enable) return
            val log = makeLogText(*args)
            Log.d(TAG, log)
            textView.post({
                textView.text = String.format("%s\n%s", textView.text, log)
            })
        }

        fun i(vararg args: Any?) {
            if (!enable) return
            Log.i(TAG, makeLogText(*args))
        }

        fun w(vararg args: Any?) {
            if (!enable) return
            Log.w(TAG, makeLogText(*args))
        }

        fun e(vararg args: Any?) {
            Log.e(TAG, makeLogText(*args))
        }

        private fun makeLogText(vararg args: Any?): String {
            val builder = StringBuilder()
            for (arg in args)
                builder.append(arg).append(' ')

            val trace = Thread.currentThread().stackTrace[4]
            return builder.append(String.format("(%s:%d)", trace.fileName, trace.lineNumber)).toString()
        }
    }
}