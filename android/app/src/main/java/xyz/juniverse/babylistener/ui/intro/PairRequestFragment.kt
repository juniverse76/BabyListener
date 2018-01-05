package xyz.juniverse.babylistener.ui.intro

import android.support.v7.app.AlertDialog
import android.view.View
import kotlinx.android.synthetic.main.frag_pair_request.view.*
import xyz.juniverse.babylistener.R
import xyz.juniverse.babylistener.etc.console
import xyz.juniverse.babylistener.firebase.DevicePairing

/**
 * Created by juniverse on 04/01/2018.
 */
class PairRequestFragment : IntroFragment() {

    override fun onResume() {
        super.onResume()
        view?.text_status?.visibility = View.INVISIBLE
        makeRequest()
    }

    private val devicePairing = DevicePairing()
    private var pairReqCode: String? = null
    private fun makeRequest() {
        devicePairing.registerPairRequest { code ->
            if (code == null) {
                console.e("not logged in")
                return@registerPairRequest
            }

            pairReqCode = code
            view?.text_pair_code?.text = code
            view?.text_status?.visibility = View.VISIBLE

            devicePairing.waitForAcknowledge(code, { result ->
                console.d("acknowledge result?", result)
                if (result) {
                    // todo set as 'pair mode'
//                    (activity as GateActivity).checkModeAndStart()
                    startFragment(create(R.layout.frag_pair_mode_final))
                }
            })

            // todo start timer...
        }
    }

    override fun onBackPressed(): Boolean {
        if (pairReqCode != null) {
            // todo
            AlertDialog.Builder(activity)
                    .setTitle("REALLY?")
                    .setMessage("cancel?")
                    .setPositiveButton(android.R.string.yes, {_, _ ->
                        devicePairing.cancelWaitingForAck(pairReqCode)
                        pairReqCode = null
                        activity.onBackPressed()
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show()
            return true
        }
        return super.onBackPressed()
    }
}