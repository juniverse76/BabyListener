package xyz.juniverse.babylistener.firebase

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceIdService
import com.google.firebase.iid.FirebaseInstanceId
import xyz.juniverse.babylistener.etc.console


/**
 * Created by juniverse on 03/01/2018.
 */

class InstanceIdService : FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        console.d("Refreshed token: " + refreshedToken!!)

        // todo I cannot know the old id....

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.

        sendRegistrationToServer(refreshedToken)
    }

    private fun sendRegistrationToServer(token: String) {
        val pushRef = FirebaseDatabase.getInstance().getReference(DB.Table.push)
        pushRef.child(FirebaseInstanceId.getInstance().id).setValue(token)
    }
}