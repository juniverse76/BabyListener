package xyz.juniverse.babylistener.ui

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.*
import android.provider.ContactsContract
import android.provider.Settings
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.*
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import xyz.juniverse.babylistener.ContactAdapter
import xyz.juniverse.babylistener.QuestionsActivity
import xyz.juniverse.babylistener.R
import xyz.juniverse.babylistener.SettingsActivity
import xyz.juniverse.babylistener.detection.DetectionService
import xyz.juniverse.babylistener.detection.Detector
import xyz.juniverse.babylistener.firebase.Ad
import xyz.juniverse.babylistener.etc.Pref
import xyz.juniverse.babylistener.firebase.Report
import xyz.juniverse.babylistener.etc.console
import java.util.*


class CallModeActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

    private var sensitivityValue = 0
    private lateinit var mInterstitialAd: InterstitialAd
    private var adShown = false
    private var detectionWaiting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        console.enable = BuildConfig.DEBUG
//        console.TAG = "blblblbl"

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        action_button.setOnClickListener { _ ->
            toggleDetection()
        }

        tester.setOnClickListener { _ ->
//            SmsManager.getDefault().sendTextMessage("01041610024", null, getString(R.string.sms_warning_paused_by_call), null, null)
            toggleTestRun()
        }

        target_number.setOnFocusChangeListener {_, focused ->
            if (focused && checkContactPermission()) {
                readContacts()
            }
        }

        charge_warning.visibility = View.GONE

        sensitivity.setOnSeekBarChangeListener(this)
        sensitivity.max = Detector.range.last - Detector.range.first
        waver_plotter.setMaxValue(Detector.valueRange.last)

        setupInitValues()

        if (!initialMessage())
            if (checkNecessaryPermissions()) {
                initializeAd()
                checkTutorial()
            }

        layout_loading.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter(DetectionService.ACTION_STATUS)
        registerReceiver(detectorServiceStatusListener, filter)

        sendBroadcast(Intent(DetectionService.ACTION_STATUS_REQ))
    }

    override fun onPause() {
        super.onPause()
        if (plugListening) {
            unregisterReceiver(powerPlugListener)
            plugListening = false
        }
        unregisterReceiver(detectorServiceStatusListener)
        stopTestDetector()

        saveKeyValues()
    }

    // 이건 안하는거야!! 실행중에도 화면은 나갈 수 있도록 해주는 거임...
    override fun onBackPressed() {
        if (detectionStatus == Detector.State.RUNNING)
            toggleDetection()
        else
            super.onBackPressed()
    }

    private var inTutorial = false
    private fun checkTutorial() {
//        if (true) return

        if (!Pref.getBool(Pref.hideTutorial)) {
            tutorial.visibility = View.VISIBLE
            // view 추가할 때는 text도 추가해야 함!!!
            val views = arrayOf(target_number, sensitivity_wrapper, tester, action_button, action_button, charge_warning, tutorial_helper_setting)
            val texts = resources.getStringArray(R.array.tutorial_texts)

            tutorial.addTargets(views, texts)
            tutorial.startTutorial(
                    {index ->
                        when (index) {
                            4 -> inDetectMode(true)
                            5 -> {
                                charge_warning.visibility = View.VISIBLE
                                (charge_warning.drawable as AnimatedVectorDrawable).start()
                            }
                            6 -> {
                                inDetectMode(false)
                                charge_warning.visibility = View.GONE
                            }
                        }
                    },
                    {
                        Pref.putBool(Pref.hideTutorial, true)
                        inTutorial = false
                        if (mInterstitialAd.isLoaded && !adShown) {
                            mInterstitialAd.show()
                            adShown = true
                            Report.event(baseContext, Report.E.AD_SHOW)
                        }
                    })

            inTutorial = true
        }
    }

    private fun initializeAd() {
        console.d("initializeAd")
        MobileAds.initialize(baseContext, Ad.appId)         // juniverse.android@gmail.com
        mInterstitialAd = InterstitialAd(baseContext)
        mInterstitialAd.adUnitId = Ad.gateAdUnitId
        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (!inTutorial && !adShown) {
                    mInterstitialAd.show()
                    adShown = true
                    adWaiter?.removeMessages(1)
                    Report.event(baseContext, Report.E.AD_SHOW)
                }
            }

            override fun onAdClosed() {
                console.d("onAdClosed")
                if (detectionWaiting)
                    toggleDetection()
            }
        }
        mInterstitialAd.loadAd(AdRequest.Builder().build())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(baseContext, SettingsActivity::class.java))
                true
            }
            R.id.action_question -> {
                startActivity(Intent(baseContext, QuestionsActivity::class.java))
                true
            }
            R.id.action_about -> {
                showAboutMessage(false)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupInitValues() {
        target_number.setText(Pref.getString(Pref.phoneNumber))
        sensitivityValue = Pref.getInt(Pref.sensitivity)
        sensitivity.progress = sensitivityValue
        sensitivity_label.text = getString(R.string.prompt_sound_sensitivity, sensitivityValue + Detector.range.first)
    }

    private fun saveKeyValues() {
        with(Pref.edit, {
            putString(Pref.phoneNumber, target_number.text.toString())
            putInt(Pref.sensitivity, sensitivityValue)
            apply()
        })
    }

    private fun initialMessage(): Boolean {
        if (Pref.getBool(Pref.init)) {
            showAboutMessage(true)
            return true
        }
        return false
    }

    private fun showAboutMessage(initial: Boolean) {
        val builder = AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_msg)
                .setCancelable(!initial)
                .setPositiveButton(android.R.string.ok, {_, _ ->
                    if (initial) {
                        Pref.putBool(Pref.init, false)
                        checkNecessaryPermissions()
                    }
                })
        if (initial)
            builder.setNegativeButton(android.R.string.no, {_, _ ->
                finish()
            })
        builder.show()
    }


    // SeekBar Event listener =======================================
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        sensitivityValue = progress
        testDetector.sensitivity = sensitivityValue
        waver_plotter.setUnit(testDetector.sensitivity)
        sensitivity_label.text = getString(R.string.prompt_sound_sensitivity, sensitivityValue + Detector.range.first)
    }

    override fun onStartTrackingTouch(p0: SeekBar?) { }
    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        saveKeyValues()
    }



    // detection service control =======================================
    private var adWaiter: Handler? = null

    private fun toggleDetection() {
        if (detectionStatus != Detector.State.RUNNING) {
            if (!adShown) {
                // 광고보고 올께요...
                if (!(mInterstitialAd.isLoading || mInterstitialAd.isLoaded))
                    initializeAd()
                detectionWaiting = true

                layout_loading.visibility = View.VISIBLE
                (image_loading.drawable as AnimatedVectorDrawable).start()

                // send timer to start any way
                if (adWaiter == null)
                    adWaiter = object: Handler(mainLooper) {
                        override fun handleMessage(msg: Message) {
                            detectionWaiting = false
                            // 광고 나왔다 치고
                            adShown = true
                            toggleDetection()
                            console.d("starting anyway")
                        }
                    }
                adWaiter?.sendEmptyMessageDelayed(1, 3000)
                return
            }

            adWaiter?.removeMessages(1)

            layout_loading.visibility = View.GONE

            val number = target_number.text.toString()
            if (number.isEmpty()) {
                Toast.makeText(baseContext, R.string.error_no_target_number, Toast.LENGTH_SHORT).show()
                target_number.requestFocus()
                return
            }

            saveKeyValues()

            Pref.putBool(Pref.stoppedByUser, false)

            waver_plotter.clear()

            console.d("starting service")
            val intent = Intent(baseContext, DetectionService::class.java)
            intent.putExtra(DetectionService.KEY_SENSITIVITY, sensitivityValue)
            intent.putExtra(DetectionService.KEY_NUMBER, number)
            startService(intent)

        } else {
            console.d("stopping service")
            val intent = Intent(baseContext, DetectionService::class.java)

            // todo need to notify it's stopped by the user...
            Pref.putBool(Pref.stoppedByUser, true)

            stopService(intent)
        }
    }

    private var detectionStatus: Detector.State = Detector.State.NONE
    private val detectorServiceStatusListener = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DetectionService.ACTION_STATUS) {
                detectionStatus = intent.getSerializableExtra(DetectionService.KEY_RUNNING_STATUS) as Detector.State
                inDetectMode(detectionStatus == Detector.State.RUNNING)
            }
        }
    }

    private fun changeAllTextColor(view: View, color: Int) {
        if (view is TextView)
            view.setTextColor(color)
        else if (view is ViewGroup) {
            var index = 0
            while (index < view.childCount) {
                changeAllTextColor(view.getChildAt(index), color)
                index++
            }
        }
    }

    private fun inDetectMode(isDetecting: Boolean?) {
        val detecting = isDetecting ?: false

        var gravity = Gravity.END or Gravity.BOTTOM
        var res = R.drawable.ic_hearing
        var bgColor = ContextCompat.getColor(baseContext, R.color.colorBgLight)
        var textColor = ContextCompat.getColor(baseContext, R.color.colorTextLight)
        var btnColor = ContextCompat.getColor(baseContext, R.color.colorAccent)
        var modeText = getString(R.string.prompt_click_to_test)
        if (detecting) {
            gravity = Gravity.START or Gravity.BOTTOM
            res = R.drawable.ic_close
            bgColor = ContextCompat.getColor(baseContext, R.color.colorBgDark)
            textColor = ContextCompat.getColor(baseContext, R.color.colorTextDark)
            btnColor = Color.RED
            modeText = getString(R.string.prompt_in_detect_mode)
        }

        val clp = action_button.layoutParams as CoordinatorLayout.LayoutParams
        clp.gravity = gravity
        action_button.layoutParams = clp
        action_button.setImageResource(res)

        // do manual color change....
        activity_main.setBackgroundColor(bgColor)
        changeAllTextColor(layout_view, textColor)

        target_number.isEnabled = !detecting
        sensitivity.isEnabled = !detecting

//        test_helper.visibility = if (detecting) View.INVISIBLE else View.VISIBLE
        test_helper.text = modeText

        action_button.backgroundTintList = ColorStateList.valueOf(btnColor)

        runTestDetectAnimation(detecting)
        checkBatterStatus(detecting)
    }

    private fun checkBatterStatus(detecting: Boolean) {
        if (detecting) {
            if (!isCharging()) {
                charge_warning.visibility = View.VISIBLE
                (charge_warning.drawable as AnimatedVectorDrawable).start()

                val plugFilter = IntentFilter()
                plugFilter.addAction(Intent.ACTION_POWER_CONNECTED)
                plugFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)
                registerReceiver(powerPlugListener, plugFilter)
                plugListening = true
            }
        } else {
            (charge_warning.drawable as AnimatedVectorDrawable).stop()
            charge_warning.visibility = View.GONE
            if (plugListening)
                unregisterReceiver(powerPlugListener)
            plugListening = false
        }
    }

    private fun isCharging(): Boolean {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, filter)

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
    }

    private var plugListening = false
    private val powerPlugListener = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isCharging()) {
                (charge_warning.drawable as AnimatedVectorDrawable).stop()
                charge_warning.visibility = View.GONE
            } else {
                charge_warning.visibility = View.VISIBLE
                (charge_warning.drawable as AnimatedVectorDrawable).start()
            }
        }
    }



    // ==================== TEST RUN =============================================
    private var testDetector = Detector()
    private fun toggleTestRun() {
        console.d("toggleTestRun", testDetector.isRunning)
        if (testDetector.isRunning)
            stopTestDetector()
        else
            startTestDetector()
    }

    private fun startTestDetector() {
        testDetector.sensitivity = sensitivityValue
        waver_plotter.clear()
        testDetector.setDetectorInterface(
                onStateChanged = { state ->
                    console.d("state changed", state)
                },
                onDetecting = { level ->
//                    console.d("detecting level", level)
                    waver_plotter.setValues(level)
                },
                onDetected = {
                    runOnUiThread(testDetected)
                })
        testDetector.start()
        waver_plotter.setUnit(testDetector.sensitivity)

        console.d("sensitivity?", testDetector.sensitivity)

        // show test detection ui
        runTestDetectAnimation(true)
        test_helper.setText(R.string.prompt_click_to_stop)
    }

    private fun runTestDetectAnimation(run: Boolean) {
        if (run) {
            wave1.visibility = View.VISIBLE
            wave2.visibility = View.VISIBLE
            dot.setImageResource(R.drawable.ic_detecting)
            (dot.drawable as AnimatedVectorDrawable).start()
            (wave1.drawable as AnimatedVectorDrawable).start()
            wave2.postDelayed( {
                (wave2.drawable as AnimatedVectorDrawable).start()
            }, 1500)
        } else {
            (dot.drawable as AnimatedVectorDrawable).stop()
            (wave1.drawable as AnimatedVectorDrawable).stop()
            (wave2.drawable as AnimatedVectorDrawable).stop()
            wave1.visibility = View.GONE
            wave2.visibility = View.GONE
        }
    }

    private fun stopTestDetector() {
        testDetector.stop()
        runTestDetectAnimation(false)
        test_helper.setText(R.string.prompt_click_to_test)
    }

    private val testDetected = Runnable {
        console.d("detected!!!!")
        stopTestDetector()
        dot.setImageResource(R.drawable.ic_detected)
        (dot.drawable as AnimatedVectorDrawable).start()
        wave1.visibility = View.GONE
        wave2.visibility = View.GONE
    }




    // ========================== Contacts =====================================
    private val reqCodeContactPermission = 4782
    private val reqCodePermissionNecessaries = 4783
    private val reqCodeDNDPermission = 4784
    class Contact(val name: String, val number: String)

    private fun checkContactPermission() : Boolean{
        if (ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), reqCodeContactPermission)
            return false
        }
        return true
    }

    private var allContacts: ArrayList<Contact>? = null

    private fun readContacts() {
        if (allContacts != null)
            return

        val contacts = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC")

        val columnName = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val columnNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val columnHasNumber = contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER)
        allContacts = ArrayList()
        while (contacts.moveToNext()) {
            if (contacts.getInt(columnHasNumber) != 1)
                continue
            allContacts?.add(Contact(contacts.getString(columnName), contacts.getString(columnNumber)))
        }

        contacts.close()
        setupContact()
    }

    private fun setupContact() {
        target_number.threshold = 1
        if (allContacts != null)
            target_number.setAdapter(ContactAdapter(baseContext, allContacts!!))
    }



    private fun checkNecessaryPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(baseContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE), reqCodePermissionNecessaries)
            return false
        }

        return askForDNDPermission()
    }

    private fun askForDNDPermission(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted && !Pref.getBool(Pref.DNDWarned)) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.need_dnd_permission_title)
                    .setMessage(R.string.need_dnd_permission_msg)
                    .setPositiveButton(android.R.string.ok, {_, _ ->
                        startActivityForResult(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), reqCodeDNDPermission)
                    })
                    .setNegativeButton(android.R.string.cancel, {_, _ ->
                        noDNDPermissionWarning()
                    })
                    .show()

            return Pref.getBool(Pref.DNDWarned)
        }
        return true
    }

    private fun recheckDNDPermission() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted && !Pref.getBool(Pref.DNDWarned)) {
            noDNDPermissionWarning()
        } else {
            initializeAd()
            checkTutorial()
        }
    }

    private fun noDNDPermissionWarning() {
        AlertDialog.Builder(this)
                .setTitle(R.string.without_dnd_permission_title)
                .setMessage(R.string.without_dnd_permission_msg)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener {
                    Pref.putBool(Pref.DNDWarned, true)
                    initializeAd()
                    checkTutorial()
                }
                .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == reqCodeContactPermission) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // cannot acquire contact. input number directly
                target_number.setHint(R.string.prompt_hint_number_only)
                target_number.inputType = InputType.TYPE_CLASS_PHONE
            } else {
                readContacts()
            }
        } else if (requestCode == reqCodePermissionNecessaries) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
                // sorry. cannot use.
                finish()
            } else {
                // all clear... check DND
                askForDNDPermission()
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == reqCodeDNDPermission) {
            recheckDNDPermission()
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }
}
