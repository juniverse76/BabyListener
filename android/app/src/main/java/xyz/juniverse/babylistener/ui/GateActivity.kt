package xyz.juniverse.babylistener.ui

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.auth.FirebaseAuth
import xyz.juniverse.babylistener.BuildConfig
import xyz.juniverse.babylistener.R
import xyz.juniverse.babylistener.etc.Pref
import xyz.juniverse.babylistener.etc.console
import com.google.android.gms.common.GoogleApiAvailability
import xyz.juniverse.babylistener.firebase.DevicePairing
import xyz.juniverse.babylistener.ui.intro.IntroFragment
import java.text.SimpleDateFormat
import java.util.*


/**
 * todo
 * - initialize firebase utilities
 * - welcome ui
 *   . intro (what this app does) -> fragment_welcom
 *   . select app mode (can switch later)
 *     * pair mode -> request pairing (or accept pairing)
 *     * call mode -> nothing
 *   . final info of each mode
 *   . redo 'check app mode'
 */
class GateActivity : AppCompatActivity() {
    private val appModePair = "pair.mode"
    private val appModeCall = "call.mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()

        if (checkPlayServices())
            startAppEntry()
    }

    /**
     * todo initialize...
     * . firebase
     * . console
     */
    private fun initialize() {
        console.enable = BuildConfig.DEBUG

        Pref.setContext(applicationContext)

        // anonymous login
//        console.d("requesting signInAnonymously")
//        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener { task ->
//            console.d("signInAnonymously", task.isSuccessful)
//            login_status.text = "login successful : " + task.isSuccessful
//            if (task.isSuccessful) {
//            } else {
                // todo what to do???
//            }
//        }
    }

    /**
     * - check app mode
     *   . pair mode -> continue with pair mode activity
     *   . call mode -> continue with call mode activity
     *   . none of the above -> welcome ui
     */
    private fun startAppEntry() {
        // should login here...
        FirebaseAuth.getInstance().signInAnonymously()

        when (Pref.getString(Pref.appMode)) {
            null -> showWelcomeUi()
            appModePair -> startPairModeActivity()
            appModeCall -> startCallModeActivity()
        }
    }

    // need to check google play service first
    private val REQ_GOOGLE_PLAY_SERVICES = 1919
    private fun checkPlayServices(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, REQ_GOOGLE_PLAY_SERVICES).show()
            }
            return false
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_GOOGLE_PLAY_SERVICES) {
            if (resultCode == Activity.RESULT_OK)
                startAppEntry()
            else
                finish()
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    /*
* - welcome ui
*   . intro (what this app does) -> frag_welcome
*   . select app mode (can switch later) -> frag_select_mode
*     * pair mode -> request pairing (or accept pairing) -> frag_select_pair_method
*     * call mode -> nothing
*   . final info of each mode
*   . redo 'check app mode'
*/
    private fun showWelcomeUi() {
        setContentView(R.layout.activity_intro)

        supportFragmentManager.beginTransaction().add(R.id.fragment_holder, IntroFragment.create(R.layout.frag_welcome), IntroFragment.TAG).commit()

//        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//        val date = dateFormatter.parse("2018-01-05 11:48:10")
//        val timer = Timer()
//        timer.schedule(object: TimerTask() {
//            override fun run() {
//                DevicePairing.getInstance().registerTest()
//            }
//        }, date)

    }

    override fun onBackPressed() {
        val frag = supportFragmentManager.findFragmentByTag(IntroFragment.TAG) as? IntroFragment
        // none or didn't process back button
        if (frag == null || !frag.onBackPressed())
            super.onBackPressed()
    }

    private fun startPairModeActivity() {
    }

    private fun startCallModeActivity() {
    }

}
