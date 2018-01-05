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

    private fun acknowledge() {
        val pairCode = view?.text_pair_code?.text?.toString() ?: return

        val acknowledging = DevicePairing.getInstance().acknowledgePairing(pairCode, { result ->
            console.d("result???", result)
            if (result == DevicePairing.SUCCESS) {
                console.d("success!!!!")
                // todo clear all back stack...
                startFragment(IntroFragment.create(R.layout.frag_final_intro))
            }
        })

        if (!acknowledging) {
            console.e("not logged in???")
        }
    }
}