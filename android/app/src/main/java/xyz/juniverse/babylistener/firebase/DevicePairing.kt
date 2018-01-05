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
 * 'uid' : {
 *      'code' : '4532'
 *      'time' : 123456
 *      'ack' : '$uid'
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

    private data class PairingData(val code: String = "", val time: Long = 0, var ack: String? = null)


    private val maxWaitTime: Long = 3 * 60 * 1000      // 3 minutes
    private val pairDbRef
        get() = FirebaseDatabase.getInstance().getReference(DB.Table.pair)


    fun registerTest() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        console.d("I am ", currentUser.uid)
//        pairDbRef.addChildEventListener(object: ChildEventListener{
//            override fun onCancelled(error: DatabaseError?) {
//                console.d("test onCancelled", error)
//            }
//
//            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                console.d("test onChildMoved", snapshot, previousChildName)
//            }
//
//            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                console.d("test onChildChanged", snapshot, previousChildName)
//            }
//
//            override fun onChildAdded(snapshot: DataSnapshot, childName: String?) {
//                console.d("test onChildAdded", snapshot, childName)
//            }
//
//            override fun onChildRemoved(snapshot: DataSnapshot) {
//                console.d("test onChildRemoved", snapshot)
//            }
//        })

        pairDbRef.addValueEventListener(object: ValueEventListener{
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

//        console.d("adding pairing request..")
//        pairDbRef.child("888888").setValue(PairingData(currentUser.uid, Date().time))
    }

    private var responseListener: ValueEventListener? = null
    fun registerPairRequest(complete: (Boolean) -> Unit): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return null

        console.d("making request")

        // make a 4 digit random code and add request. still possible with same code.. at least use user id as seed.
        var codeOffset = currentUser.uid.hashCode() % 200000
        if (codeOffset < 0)
            codeOffset += 200000
//        console.d("codeOffset?", codeOffset)

        // seed as user id, must be 6 digits, even offset by time
        val code = (Random(System.currentTimeMillis()).nextInt(700000) + 100000 + codeOffset).toString()

        console.d("writing something....", code, currentUser.uid)
        val pairReqRef = pairDbRef.child(currentUser.uid)
        pairReqRef.setValue(PairingData(code, Date().time))

        if (responseListener == null) {
            responseListener = object : ValueEventListener {
                override fun onCancelled(databaseError: DatabaseError) {
                    // just in case
                    console.d("onCancelled", databaseError)
                    pairReqRef.removeEventListener(this)
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    console.d("onDataChange", dataSnapshot)

                    val pairingData = dataSnapshot.getValue(PairingData::class.java)
                    if (pairingData == null) {
                        // this is when it's canceled
                        pairReqRef.removeEventListener(this)
                        complete(false)
                        return
                    }

                    // this isn't error... it's just the result of me adding...
                    val ackId = pairingData.ack ?: return

                    console.d("answered!!! key?", dataSnapshot.key)
                    console.d("pairingData?", pairingData)

                    // save
                    savePartnerId(ackId)

                    // remove listener
                    pairReqRef.removeEventListener(this)

                    // remove registration
                    unregisterPairRequest(currentUser.uid)

                    complete(true)
                }
            }
            pairReqRef.addValueEventListener(responseListener)
        }

        return code
    }

    fun cancelWaitingForAck() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        pairDbRef.child(currentUser.uid).removeEventListener(responseListener)
    }


    private fun waitForRequestAck(uid: String) {
        val codeDatRef = pairDbRef.child(uid)
        codeDatRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                // just in case
                console.d("onCancelled", databaseError)
                codeDatRef.removeEventListener(this)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                console.d("onDataChange", dataSnapshot)

                val pairingData = dataSnapshot.getValue(PairingData::class.java)
                if (pairingData == null) {
                    // this is when it's canceled
                    codeDatRef.removeEventListener(this)
                    return
                }

                val ackId = pairingData.ack ?: return

                console.d("answered!!! key?", dataSnapshot.key)
                console.d("pairingData?", pairingData)

                // save
                savePartnerId(ackId)

                // remove listener
                codeDatRef.removeEventListener(this)

                // remove registration
                unregisterPairRequest(uid)
            }
        })
    }

    fun unregisterPairRequest(uid: String) {
        pairDbRef.child(uid).removeValue()
    }


    // ============ for ack ==============
    fun acknowledgePairing(code: String, complete: (Int) -> Unit): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false

        val query = pairDbRef.orderByChild("code").equalTo(code)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {}
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                console.d("dataSnapshot.exists?", dataSnapshot.exists())
                if (!dataSnapshot.exists()) {
                    complete(UNKNOWN_CODE)
                    return
                }

                console.d("dataSnapshot.children", dataSnapshot.children.count())
                for (pairReq in dataSnapshot.children) {
                    console.d("pairReq:", pairReq)
                    val pairingData = pairReq.getValue(PairingData::class.java)

                    if (pairingData == null) {
                        complete(UNKNOWN_CODE)
                        return
                    }

                    if (Date().time - pairingData.time > maxWaitTime) {
                        complete(TIMEOUT)
                        return
                    }

                    pairingData.ack = currentUser.uid
                    pairDbRef.child(pairReq.key).setValue(pairingData)

                    savePartnerId(pairReq.key)

                    complete(SUCCESS)
                    return
                }
            }
        })

        return true
    }


    fun unpairDevice() {
        // todo notify partner to unpair
    }


    private fun savePartnerId(partnerId: String) {
        // todo after math
        Pref.putString(Pref.partnerId, partnerId)
    }

    fun checkPartnerId(result: (Boolean) -> Unit) {
        val partnerId = Pref.getString(Pref.partnerId) ?: return result(false)

        // todo need to send some msg to acknowledge
        result(true)
    }
}