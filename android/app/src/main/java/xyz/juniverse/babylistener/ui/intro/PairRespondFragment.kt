package xyz.juniverse.babylistener.ui.intro

import android.view.View
import xyz.juniverse.babylistener.R
import kotlinx.android.synthetic.main.frag_pair_respond.view.*
import xyz.juniverse.babylistener.etc.console
import xyz.juniverse.babylistener.firebase.DevicePairing

/**
 * Created by juniverse on 04/01/2018.
 */
class PairRespondFragment : IntroFragment() {

    override fun onClick(view: View) {
        if (view.id == R.id.acknowledge) {
            acknowledge()
        } else
            super.onClick(view)
    }

    private val devicePairing = DevicePairing()
    private fun acknowledge() {
        val pairCode = view?.text_pair_code?.text?.toString() ?: return

        // todo start loading...
        val acknowledging = devicePairing.acknowledgePairing(pairCode, { result ->
            console.d("result???", result)
            // todo stop loading...
            if (result == DevicePairing.SUCCESS) {
                console.d("success!!!!")
                // todo clear all back stack...
                // todo set as 'pair mode'
//                (activity as GateActivity).checkModeAndStart()
                startFragment(create(R.layout.frag_pair_mode_final))
            } else {
                console.e("failed...")
                // todo notify user
            }
        })

        if (!acknowledging) {
            console.e("not logged in???")
        }
    }
}