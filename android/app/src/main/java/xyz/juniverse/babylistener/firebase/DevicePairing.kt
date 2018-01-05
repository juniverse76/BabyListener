package xyz.juniverse.babylistener.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import xyz.juniverse.babylistener.etc.Pref
import xyz.juniverse.babylistener.etc.console
import java.util.*

/**
 * Created by juniverse on 03/01/2018.
 *
 * todo
 * 'code' : {
 *      'reqId' : '$uid'
 *      'time' : 123456
 *      'ackId' : '$uid'
 * }
 */
class DevicePairing {
    companion object {
        private var _instance: DevicePairing? = null
        fun getInstance(): DevicePairing {
            if (_instance == null)
                _instance = DevicePairing()
            return _instance!!
        }

        val SUCCESS = 0
        val UNKNOWN_CODE = -1
        val TIMEOUT = -2
    }

    private data class PairingData(val reqId: String = "", val time: Long = 0, var ackId: String? = null)


//    private val maxWaitTime: Long = 3 * 60 * 1000      // 3 minutes
    private val maxWaitTime: Long = 30 * 1000      // 30 seconds
    private val pairDbRef
        get() = FirebaseDatabase.getInstance().getReference(DB.Table.pair)



    fun registerTest() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        console.d("I am ", currentUser.uid)
        pairDbRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError?) {
                console.d("value event onCancelled", error)
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                console.d("value event onDataChange", snapshot)
                val key = "999999"
                if (!snapshot.children.any { it.key == key }) {
                    console.d("adding pairing request..")
                    pairDbRef.child(key).setValue(PairingData(currentUser.uid, Date().time))
                    pairDbRef.removeEventListener(this)
                } else {
                    console.d("key already in... cannot write")
                }
            }
        })
    }




    fun registerPairRequest(onResult: (String?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return onResult(null)

        console.d("making request")

        pairDbRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError?) {
                console.d("onCancelled", error)
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                console.d("value event onDataChange", snapshot)
                var code: String
                do {
                    code = (Random().nextInt(899999) + 100000).toString()
                } while (snapshot.children.any { it.key == code })

                console.d("adding pairing request..")
                pairDbRef.child(code).setValue(PairingData(currentUser.uid, Date().time))

                onResult(code)
            }
        })
    }


    fun cancelWaitingForAck(code: String?) {
        if (code == null) return

        console.d("cancelWaitingForAck")
        cancelTimer?.cancel()
        pairDbRef.child(code).removeEventListener(acknowledgeListener)
        pairDbRef.child(code).removeValue()
        acknowledgeListener = null
    }

    // wait for that code to change. if false, timeout
    private var acknowledgeListener: ValueEventListener? = null
    private var cancelTimer: Timer? = null
    fun waitForAcknowledge(code: String, onResult: (Boolean) -> Unit) {
        if (acknowledgeListener != null) return

        val codeDatRef = pairDbRef.child(code)
        acknowledgeListener = object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                // just in case
                console.d("onCancelled", databaseError)
                codeDatRef.removeEventListener(this)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                console.d("onDataChange", dataSnapshot)

                val pairingData = dataSnapshot.getValue(PairingData::class.java)
                if (pairingData == null) {
                    // this is when it's canceled.. though this probably won't happen
                    cancelWaitingForAck(code)
                    return
                }

                val ackId = pairingData.ackId ?: return

                console.d("answered!!! key?", dataSnapshot.key)
                console.d("pairingData?", pairingData)

                // save
                savePartnerId(ackId)

                // remove listener
                cancelWaitingForAck(code)

                onResult(true)
            }
        }
        codeDatRef.addValueEventListener(acknowledgeListener)

        cancelTimer = Timer()
        cancelTimer?.schedule(object: TimerTask() {
            override fun run() {
                cancelWaitingForAck(code)
                onResult(false)
            }
        }, maxWaitTime)
    }






    // ============ for ackId ==============
    fun acknowledgePairing(code: String, complete: (Int) -> Unit): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false

        pairDbRef.child(code).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError?) {
                console.e("acknowledge error?", error)
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                val pairingData = snapshot.getValue(PairingData::class.java)
                if (pairingData == null) {
                    // todo possibly removed by timeout
                    complete(UNKNOWN_CODE)
                    return
                }

                if (Date().time - pairingData.time > maxWaitTime) {
                    complete(TIMEOUT)
                    return
                }

                pairingData.ackId = currentUser.uid
                pairDbRef.child(code).setValue(pairingData)

                complete(SUCCESS)

                savePartnerId(pairingData.reqId)
            }
        })

        return true
    }


    fun unpairDevice() {
        // todo notify partner to unpair
    }


    private fun savePartnerId(partnerId: String) {
        // todo after math
        console.d("I am", FirebaseAuth.getInstance().currentUser?.uid, "and my partner is", partnerId)
//        Pref.putString(Pref.partnerId, partnerId)
    }

    fun checkPartnerId(result: (Boolean) -> Unit) {
        val partnerId = Pref.getString(Pref.partnerId) ?: return result(false)

        // todo need to send some msg to acknowledge
        result(true)
    }
}