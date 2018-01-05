package xyz.juniverse.babylistener.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import xyz.juniverse.babylistener.R
import xyz.juniverse.babylistener.firebase.DevicePairing

class PairModeActivity : AppCompatActivity() {

    private val devicePairing = DevicePairing()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair_mode)

        devicePairing.checkPartnerId { result ->
            if (!result) {
                // todo uh oh.. partner doesn't exist... must re pair
            }
        }
    }
}
