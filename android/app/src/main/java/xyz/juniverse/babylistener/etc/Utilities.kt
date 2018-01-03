package xyz.juniverse.babylistener.etc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v4.app.ActivityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.juniverse.babylistener.BuildConfig

/**
 * Created by juniverse on 24/11/2017.
 */


fun ViewGroup.inflate(layoutRes: Int, attachToRoot: Boolean = false): View  = LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)


// 아래 함수를 사용하려면 밑에 줄이 gradle에 있어야 함..
// compile "org.jetbrains.kotlin:kotlin-reflect:1.2.0"
fun getNames(view: View?, space: String) {
    if (view == null) return

    if (view.id > 0)
        console.d(space, view::class.qualifiedName, view.resources.getResourceName(view.id))
    else
        console.d(space, view::class.qualifiedName, "'no id'")

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            getNames(view.getChildAt(i), space + "  ")
        }
    }
}

fun makeCall(context: Context, number: String) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
        return

    val targetNumber = "tel://" + number
    val intent = Intent(Intent.ACTION_CALL)
    intent.data = Uri.parse(targetNumber)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    if (!BuildConfig.DEBUG)
        context.startActivity(intent)
    else
        console.d("making call:", targetNumber)
}