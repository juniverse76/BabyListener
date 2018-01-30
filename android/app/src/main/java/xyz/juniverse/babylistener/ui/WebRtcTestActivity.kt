package xyz.juniverse.babylistener.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_web_rtc_test.*
import org.webrtc.*
import xyz.juniverse.babylistener.R
import xyz.juniverse.babylistener.etc.console
import java.nio.ByteBuffer

class WebRtcTestActivity : AppCompatActivity(), PeerConnection.Observer {

    private lateinit var db: FirebaseDatabase
    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel
    private val audioConstraint = MediaConstraints()

    private val WEBRTC_DB_REF = "webrtc"

    // todo text feeder를 넣어서 이벤트 발생 순서를 확인하자...
    private var stopFeed = false
    val textFeeder = object: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            console_view.text = String.format("%s\n.......%d", console_view.text, System.currentTimeMillis())
            scroll_view.scrollTo(0, 20000)

            if (!stopFeed)
                sendEmptyMessageDelayed(1, 150)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_rtc_test)

        db = FirebaseDatabase.getInstance()
    }

    override fun onPause() {
        super.onPause()
        peerConnection.close()
    }

    fun handleButton(button: View) {
        if (button.id == R.id.call_button) {
            caller = true
            initializeP2P()
            call()
        } else if (button.id == R.id.answer_button) {
            caller = false
            initializeP2P()
            answer()
        } else if (button.id == R.id.send_button) {
            dataChannel.send(DataChannel.Buffer(ByteBuffer.wrap("hello".toByteArray()), false))
        }
        textFeeder.sendEmptyMessageDelayed(1, 150)
    }

    private fun initializeP2P() {
        val who = if (caller) "_caller" else "_answerer"
        if (PeerConnectionFactory.initializeAndroidGlobals(baseContext, true, false, false, null)) {
            val factory = PeerConnectionFactory()
            val audioSource = factory.createAudioSource(audioConstraint)
            val audioTrack = factory.createAudioTrack("audio_track_id" + who, audioSource)
            val mediaStream = factory.createLocalMediaStream("audio_stream_id" + who)
            mediaStream.addTrack(audioTrack)

            audioConstraint.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))

            val list: List<PeerConnection.IceServer> = listOf(
                    PeerConnection.IceServer("stun:stun.l.google.com:19302"),
                    PeerConnection.IceServer("stun:stun.services.mozilla.com")
            )
            peerConnection = factory.createPeerConnection(list, audioConstraint, this)

            peerConnection.addStream(mediaStream)

//            initDataChannel()
        }
    }

    private var caller: Boolean = false
    private fun call() {
        peerConnection.createOffer(sdpObserver, audioConstraint)
        db.getReference(WEBRTC_DB_REF).child("user_id").addValueEventListener(firebaseDbListener)
    }

    private fun answer() {
        db.getReference(WEBRTC_DB_REF).child("user_id").addValueEventListener(firebaseDbListener)
        console.d(console_view, "listening...")
    }

    private val firebaseDbListener = object: ValueEventListener {
        override fun onCancelled(error: DatabaseError?) {
            console.d(console_view, "onCancelled", error)
        }

        override fun onDataChange(snapshot: DataSnapshot) {
//            console.d("value event onDataChange", snapshot, snapshot.key)
            val type = snapshot.child("type").value.toString()
            val description = snapshot.child("description").value.toString()
            var remoteDescription: SessionDescription? = null
            if (type == "OFFER") {
                remoteDescription = SessionDescription(SessionDescription.Type.OFFER, description)
            } else if (type == "ANSWER") {
                remoteDescription = SessionDescription(SessionDescription.Type.ANSWER, description)
            }
//            val remoteDescription = snapshot.getValue(SessionDescription::class.java)

            if (remoteDescription?.type == SessionDescription.Type.OFFER && !caller) {
                console.d(console_view, "setting remote description for answerer")
                peerConnection.setRemoteDescription(sdpObserver, remoteDescription)
                peerConnection.createAnswer(sdpObserver, audioConstraint)
            }
            if (remoteDescription?.type == SessionDescription.Type.ANSWER && caller) {
                console.d(console_view, "setting remote description for caller")
                peerConnection.setRemoteDescription(sdpObserver, remoteDescription)

                // 결과 받았음. 지워..
                db.getReference(WEBRTC_DB_REF).removeValue()
            }
        }
    }

    private val sdpObserver = object : SdpObserver {
        override fun onSetFailure(p0: String?) {
            console.d(console_view, "onSetFailure", p0)
        }

        override fun onSetSuccess() {
            console.d(console_view, "onSetSuccess")
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            console.d(console_view, "onCreateSuccess", p0, p0?.type)
            peerConnection.setLocalDescription(this, p0)
            if (p0?.type == SessionDescription.Type.OFFER)
                db.getReference(WEBRTC_DB_REF).child("user_id").setValue(p0)
            else if (p0?.type == SessionDescription.Type.ANSWER)
                db.getReference(WEBRTC_DB_REF).child("user_id").setValue(p0)
        }

        override fun onCreateFailure(p0: String?) {
            console.d(console_view, "onCreateFailure", p0)
        }
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        console.d(console_view, "onIceGatheringChange", p0)
        if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
//            initDataChannel()
            stopFeed = true
        }
    }

    override fun onAddStream(p0: MediaStream?) {
        console.d(console_view, "onAddStream", p0)
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        console.d(console_view, "onIceCandidate", p0)
        peerConnection.addIceCandidate(p0)
    }

    override fun onDataChannel(p0: DataChannel?) {
        console.d(console_view, "onDataChannel", p0)
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        console.d(console_view, "onSignalingChange", p0)
    }

    override fun onRemoveStream(p0: MediaStream?) {
        console.d(console_view, "onRemoveStream", p0)
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        console.d(console_view, "onIceConnectionChange", p0)
    }

    override fun onRenegotiationNeeded() {
        console.d(console_view, "onRenegotiationNeeded")
    }


    fun initDataChannel() {
        val dInint = DataChannel.Init()
        dInint.id = 1
        console.d(console_view, "createDataChannel")
        dataChannel = peerConnection.createDataChannel("baby", dInint)
        dataChannel.registerObserver(object: DataChannel.Observer {
            override fun onMessage(p0: DataChannel.Buffer?) {
                console.d(console_view, "onMessage", p0)
            }

            override fun onStateChange() {
                console.d(console_view, "onStateChange")
            }
        })

    }
}
