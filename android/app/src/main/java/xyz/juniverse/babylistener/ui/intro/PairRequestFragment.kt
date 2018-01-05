package xyz.juniverse.babylistener.ui.intro

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

    override fun onPause() {
        super.onPause()
        DevicePairing.getInstance().cancelWaitingForAck()
    }

    private fun makeRequest() {
        val code = DevicePairing.getInstance().registerPairRequest { result ->
            console.d("pairing result?", result)
            if (result) {
                // todo clear all back stack...
                startFragment(IntroFragment.create(R.layout.frag_final_intro))
            }
        }

        if (code == null) {
            console.e("not logged in???")
            return
        }

        view?.text_pair_code?.text = code
        view?.text_status?.visibility = View.VISIBLE
    }

}