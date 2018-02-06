package xyz.juniverse.babylistener.ui

import android.opengl.GLSurfaceView
import android.os.Bundle
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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class WebRtcTestActivity : AppCompatActivity(), PeerConnection.Observer {

    private lateinit var db: FirebaseDatabase
    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel
    private val audioConstraint = MediaConstraints()
    private val videoConstraint = MediaConstraints()

    private val WEBRTC_DB_REF = "webrtc"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_rtc_test)

        gl_surface.setRenderer(object : GLSurfaceView.Renderer {
            override fun onDrawFrame(p0: GL10?) {
//                console.i("onDrawFrame")
            }

            override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
                console.i("onSurfaceChanged")
            }

            override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
                console.i("onSurfaceCreated")
            }
        })

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
//            call()
        } else if (button.id == R.id.answer_button) {
            caller = false
            initializeP2P()
            answer()
        } else if (button.id == R.id.send_button) {
            dataChannel.send(DataChannel.Buffer(ByteBuffer.wrap("hello".toByteArray()), false))
        }
//        textFeeder.sendEmptyMessageDelayed(1, 150)
    }

    private fun initializeP2P() {
        val who = if (caller) "_caller" else "_answerer"
        if (PeerConnectionFactory.initializeAndroidGlobals(baseContext, true, true, false, null)) {
            val factory = PeerConnectionFactory()

            val camNumber = VideoCapturerAndroid.getDeviceCount()
            val capturer = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice())
            val videoSource = factory.createVideoSource(capturer, videoConstraint)
            val videoTrack = factory.createVideoTrack("video_track_id" + who, videoSource)

            val audioSource = factory.createAudioSource(audioConstraint)
            val audioTrack = factory.createAudioTrack("audio_track_id" + who, audioSource)

            val mediaStream = factory.createLocalMediaStream("media_stream_id" + who)
            mediaStream.addTrack(audioTrack)
            mediaStream.addTrack(videoTrack)

            VideoRendererGui.setView(gl_surface, null)

            val remoteRenderer = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false)
            val localRenderer = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true)

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
        console.d("listening...")
    }

    private val firebaseDbListener = object: ValueEventListener {
        override fun onCancelled(error: DatabaseError?) {
            console.d("onCancelled", error)
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            console.d("value event onDataChange", snapshot, snapshot.key)
            val type = snapshot.child("type").value.toString()
            val description = snapshot.child("description").value.toString()
            var remoteDescription: SessionDescription? = null
            if (type.toUpperCase() == "OFFER") {
                remoteDescription = SessionDescription(SessionDescription.Type.OFFER, description)
            } else if (type.toUpperCase() == "ANSWER") {
                remoteDescription = SessionDescription(SessionDescription.Type.ANSWER, description)
            }
//            val remoteDescription = snapshot.getValue(SessionDescription::class.java)

            if (remoteDescription?.type == SessionDescription.Type.OFFER && !caller) {
                console.d("setting remote description for answerer")
                peerConnection.setRemoteDescription(sdpObserver, remoteDescription)
                peerConnection.createAnswer(sdpObserver, audioConstraint)
            }
            if (remoteDescription?.type == SessionDescription.Type.ANSWER && caller) {
                console.d("setting remote description for caller")
                peerConnection.setRemoteDescription(sdpObserver, remoteDescription)

                // 결과 받았음. 지워..
                db.getReference(WEBRTC_DB_REF).removeValue()
            }
        }
    }

    private val sdpObserver = object : SdpObserver {
        override fun onSetFailure(p0: String?) {
            console.d("onSetFailure", p0)
        }

        override fun onSetSuccess() {
            console.d("onSetSuccess")
        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            console.d("onCreateSuccess", p0, p0?.type)
            peerConnection.setLocalDescription(this, p0)
            if (p0?.type == SessionDescription.Type.OFFER)
                db.getReference(WEBRTC_DB_REF).child("user_id").setValue(p0)
            else if (p0?.type == SessionDescription.Type.ANSWER)
                db.getReference(WEBRTC_DB_REF).child("user_id").setValue(p0)
        }

        override fun onCreateFailure(p0: String?) {
            console.d("onCreateFailure", p0)
        }
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        console.d("onIceGatheringChange", p0)
        if (p0 == PeerConnection.IceGatheringState.COMPLETE) {
//            initDataChannel()
        }
    }

    override fun onAddStream(p0: MediaStream?) {
        console.d("onAddStream", p0)
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        console.d("onIceCandidate", p0)
        peerConnection.addIceCandidate(p0)
    }

    override fun onDataChannel(p0: DataChannel?) {
        console.d("onDataChannel", p0)
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        console.d("onSignalingChange", p0)
    }

    override fun onRemoveStream(p0: MediaStream?) {
        console.d("onRemoveStream", p0)
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        console.d("onIceConnectionChange", p0)
    }

    override fun onRenegotiationNeeded() {
        console.d("onRenegotiationNeeded")
    }


    fun initDataChannel() {
        val dInint = DataChannel.Init()
        dInint.id = 1
        console.d("createDataChannel")
        dataChannel = peerConnection.createDataChannel("baby", dInint)
        dataChannel.registerObserver(object: DataChannel.Observer {
            override fun onMessage(p0: DataChannel.Buffer?) {
                console.d("onMessage", p0)
            }

            override fun onStateChange() {
                console.d("onStateChange")
            }
        })

    }
}
