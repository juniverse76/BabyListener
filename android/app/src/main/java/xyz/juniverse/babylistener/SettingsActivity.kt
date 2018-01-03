package xyz.juniverse.babylistener

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.widget.EditText
import xyz.juniverse.babylistener.etc.Pref

class SettingsActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
        fragmentManager.beginTransaction().replace(android.R.id.content, GeneralPreferenceFragment(), "setting").commit()
    }

    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {

        private val reqCodeDNDPermission = 4784
        private val reqCodeRecSMSPermission = 4781
        private val reqCodeSendSMSPermission = 4782
        private val reqCodeSendSMSPermission2 = 4783

//        private fun showDisableConfirmPopup() {
//            val titles = resources.getStringArray(R.array.pref_sure_to_disable_warning_titles)
//            var index = Pref(activity.baseContext).getInt(Pref.disableJoke)
//            if (index >= titles.size)
//                index = Random().nextInt(titles.size)
//            else
//                Pref(activity.baseContext).e.putInt(Pref.disableJoke, index + 1).apply()
//
//            val title = titles[index]
//            val msg = resources.getStringArray(R.array.pref_sure_to_disable_warning_msgs)[index]
//            AlertDialog.Builder(activity)
//                    .setTitle(title)
//                    .setMessage(msg)
//                    .setPositiveButton(android.R.string.ok, { _, _ ->
//                        showDisableConfirmPopup()
//                    })
//                    .setNegativeButton(android.R.string.cancel, null)
//                    .show()
//        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val pref = Pref(activity.baseContext)
//            pref.remove(Pref.smsCallCmd)

            addPreferencesFromResource(R.xml.pref_general)

            // unexpected stop
            val warningOpt = findPreference(Pref.enableUnexpWarningSMS) as SwitchPreference
            warningOpt.setOnPreferenceChangeListener { _, value ->
                if (value as Boolean && ActivityCompat.checkSelfPermission(activity.baseContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.SEND_SMS), reqCodeSendSMSPermission)
                    false
                } else
                    true
            }

            // sms command
            val cmd = pref.getString(Pref.smsCallCmd)
            val cmdOpt = findPreference(Pref.smsCallCmd + ".delegate") as SwitchPreference
            cmdOpt.isChecked = cmd != null
            if (cmdOpt.isChecked)
                cmdOpt.summary = getString(R.string.pref_sms_cmd_desc) + "\n" + getString(R.string.pref_cur_sms_cmd, cmd)
            else
                cmdOpt.summary = getString(R.string.pref_sms_cmd_desc) + "\n" + getString(R.string.pref_no_cur_sms_cmd)

            cmdOpt.setOnPreferenceChangeListener { _, value ->
                val checked = value as Boolean
                if (checked) {
                    // todo fist check permission
                    if (ActivityCompat.checkSelfPermission(activity.baseContext, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECEIVE_SMS), reqCodeRecSMSPermission)
                    } else {
                        showSmsCmdEditPopup()
                    }
                } else {
                    cmdOpt.summary = getString(R.string.pref_sms_cmd_desc) + "\n" + getString(R.string.pref_no_cur_sms_cmd)
                    pref.remove(Pref.smsCallCmd)
                    cmdOpt.isChecked = checked
                }
                false
            }


            // sms notification
            val smsOpt = findPreference(Pref.smsAllNoti) as SwitchPreference
            smsOpt.setOnPreferenceChangeListener { _, value ->
                if (value as Boolean && ActivityCompat.checkSelfPermission(activity.baseContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.SEND_SMS), reqCodeSendSMSPermission2)
                    false
                } else
                    true
            }


            // Do Not Disturb option
            val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
                addPreferencesFromResource(R.xml.pref_dnd_link)
                val dnd = findPreference("link.dnd")
                dnd.setOnPreferenceClickListener {
                    startActivityForResult(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), reqCodeDNDPermission)
                    true
                }
            }

        }

        private fun showSmsCmdEditPopup() {
            val cmdOpt = findPreference(Pref.smsCallCmd + ".delegate") as SwitchPreference
            val lastCmd = Pref(activity.baseContext).getString(Pref.lastSmsCallCmd)
            val input = EditText(activity.baseContext)
            input.text = lastCmd as Editable
            AlertDialog.Builder(activity)
                    .setTitle(R.string.pref_label_sms_cmd)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok, { _, _ ->
                        val code = input.text.trim().toString()
                        if (code.isNotEmpty()) {
                            cmdOpt.summary = getString(R.string.pref_sms_cmd_desc) + "\n" + getString(R.string.pref_cur_sms_cmd, code)
                            Pref(activity.baseContext).putString(Pref.smsCallCmd, code)
                            Pref(activity.baseContext).putString(Pref.lastSmsCallCmd, code)
                            cmdOpt.isChecked = true
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            if (requestCode == reqCodeRecSMSPermission && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                showSmsCmdEditPopup()
            else if (requestCode == reqCodeSendSMSPermission && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val warningOpt = findPreference(Pref.enableUnexpWarningSMS) as SwitchPreference
                warningOpt.isChecked = true
                Pref(activity.baseContext).putBool(Pref.enableUnexpWarningSMS, true)
            } else if (requestCode == reqCodeSendSMSPermission2 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val smsOpt = findPreference(Pref.smsAllNoti) as SwitchPreference
                smsOpt.isChecked = true
                Pref(activity.baseContext).putBool(Pref.smsAllNoti, true)
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == reqCodeDNDPermission) {
                val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                    // remove
                    preferenceScreen.removePreference(findPreference("link.dnd"))
                }
            }
            else
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        val frag = fragmentManager.findFragmentByTag("setting") as? PreferenceFragment
        frag?.onRequestPermissionsResult(requestCode, permissions, grantResults) ?: super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
