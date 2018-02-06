//
//  ViewController.swift
//  BabyListener
//
//  Created by 김준성 on 02/01/2018.
//  Copyright © 2018 김준성. All rights reserved.
//

import UIKit
import WebRTC
import Firebase

class ViewController: UIViewController, DetectorDelegate, RTCPeerConnectionDelegate {

    let WEBRTC_TABLE_NAME = "webrtc"
    private let factory = RTCPeerConnectionFactory()
    let audioContraint = RTCMediaConstraints.init(mandatoryConstraints: ["OfferToReceiveAudio": "true"], optionalConstraints: nil)
    let configuration = RTCConfiguration()
    var peerConnection: RTCPeerConnection
//    private let dbRef = Database.database().reference(withPath: "webrtc")
//    private var audioContraint: RTCMediaConstraints? = nil
    
//    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
//    }
    
    required init?(coder aDecoder: NSCoder) {
        configuration.iceServers = [RTCIceServer.init(urlStrings: ["stun:stun.l.google.com:19302"])]
        peerConnection = factory.peerConnection(with: configuration, constraints: audioContraint, delegate: nil)

        super.init(coder: aDecoder)
    }
    
    let detector = Detector()

    @IBOutlet weak var Label: UILabel!
    @IBOutlet weak var CanvasView: UIImageView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
//        detector.setDetectorDelegate(self)
//        detector.setSensitivity(2000)
//        initWebRTC()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    private var running: Bool = false
    @IBAction func onStart(_ sender: UIButton) {
        initWebRTC()
//        createOffer()
//        print("onStart")
//        if running {
//            detector.start()
//        } else {
//            detector.stop()
//        }
//        running = !running
//            UIApplication.shared.isIdleTimerDisabled = true     // [juniverse] when in detection, disable idle timer
    }
    
    func onStateChanged(_ state: DetectorState) {
        print("onStateChanged \(state)")
        DispatchQueue.main.async {
            self.Label.text = state.rawValue
        }
    }
    
    func onDetecting(_ samples: UnsafeMutableBufferPointer<Int16>) {
        DispatchQueue.main.async {
        print("onDetecting");
        let width = self.CanvasView.bounds.width
        let height = self.CanvasView.bounds.height
        let center_y = height / 2
        let unit_width = width / CGFloat(samples.count)
        let unit_height = height / 10000
        var index = 0
            
            UIGraphicsBeginImageContext(self.CanvasView.frame.size)
            let context = UIGraphicsGetCurrentContext()
            self.CanvasView.image?.draw(in: CGRect(x: 0, y: 0, width: width, height: height))
            context?.clear(CGRect(x: 0, y: 0, width: width, height: height))
            context?.setFillColor(red: 1, green: 0, blue: 0, alpha: 1)
        for sample in samples {
            let s = CGFloat(sample)
            let rect = CGRect(x: CGFloat(index) * unit_width, y: center_y, width: unit_width, height: unit_height * s)
//            context?.move(to: CGPoint(x: index * unit_width, y: center_y))
//            context?.addRect(rect)
            context?.fill(rect)

            index += 1
        }
            self.CanvasView.image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
        }
    }
    
    func onDetected() {
        print("onDetected!!!!");
        DispatchQueue.main.async {
            self.Label.text = "DETECTED!!!"
        }
//        detector.stop()
    }

    
    func initWebRTC() {
//        print(RTCInitializeSSL())
        
//        RTCInitializeSSL()
        print("initWebRTC")
//        let audioContraint = RTCMediaConstraints.init(mandatoryConstraints: ["OfferToReceiveAudio": "true"], optionalConstraints: nil)
//        let configuration = RTCConfiguration()
//        configuration.iceServers = [RTCIceServer.init(urlStrings: ["stun:stun.l.google.com:19302"])]
//        configuration.iceServers = [RTCIceServer.init(urlStrings: ["stun:stun.services.mozilla.com"])]
//        let pc = factory.peerConnection(with: configuration, constraints: audioContraint, delegate: self)
//        self.peerConnection = factory.peerConnection(with: configuration, constraints: audioContraint, delegate: self)
        peerConnection.delegate = self

        let audioSource = factory.audioSource(with: audioContraint)
        let audioTrack = factory.audioTrack(with: audioSource, trackId: "audio_track_id_ios")
        let audioStream = factory.mediaStream(withStreamId: "audio_stream_id_ios")
        audioStream.addAudioTrack(audioTrack)
        peerConnection.add(audioStream)
        
        peerConnection.offer(for: audioContraint, completionHandler: { [weak self] (description, error) in
            let desc: String = (description?.sdp)!
//            print("description? \(desc)")
            print("setting local description")
            self?.peerConnection.setLocalDescription(description!, completionHandler: nil)

            var type: String
            if description?.type == .offer {
                type = "offer"
            } else {
                type = "answer"
            }

            let db = Database.database()
            let signalingRef = db.reference(withPath: (self?.WEBRTC_TABLE_NAME)!).child("user_id")
            let signalData = ["type": type, "description": desc]
            signalingRef.setValue(signalData)
            
//            DispatchQueue.main.async {
                self?.listen()
//            }
        })
        print("initWebRTC done")
        
//        self.peerConnection = pc
    }
    
    func listen() {
//        let codeDataRef = Database.database().reference(withPath: PAIR_TABLE_NAME).child(code)
        let db = Database.database()
        let ref = db.reference(withPath: WEBRTC_TABLE_NAME).child("user_id")
        let handler = ref.observe(DataEventType.value, with: {(snapshot) in
            print("got snapshot")
            guard let signalingData = snapshot.value as? [String : String] else {
                return
            }
            let type: String? = signalingData["type"]
            let desc: String? = signalingData["description"]
//            print("type: \(type)")
//            print("desc: \(desc)")
            
            if (type?.uppercased() == "ANSWER") {
                print("is answer!!! setting protocol!!!")
                let sdp = RTCSessionDescription.init(type: .answer, sdp: desc!)
                DispatchQueue.main.async {
//                    let pc = self.factory.peerConnection(with: self.configuration, constraints: self.audioContraint, delegate: self)
                    self.peerConnection.setRemoteDescription(sdp, completionHandler: {(error) in
//                    pc.setRemoteDescription(sdp, completionHandler: {(error) in
                        print("error??? \(error)")
                    })
                }
//                Database.database().reference(withPath: self.WEBRTC_TABLE_NAME).removeValue()
            }
        })
        print("data set... listening... \(handler)")
    }
    
    func createOffer() {
        print("about to create offer")
//        self.peerConnection?.offer(for: self.audioContraint!, completionHandler: {(description, error) in
//            let desc: String = (description?.sdp)!
//            print("description? \(desc)")
//            self.peerConnection?.setLocalDescription(description!, completionHandler: nil)
//
//            var type: String
//            if description?.type == .offer {
//                type = "offer"
//            } else {
//                type = "answer"
//            }
//
//            let signalingRef = Database.database().reference(withPath: self.WEBRTC_TABLE_NAME).child("uiser_id")
//            let signalData = ["type": type, "description": description?.sdp]
//            signalingRef.setValue(signalData)
//        })
        print("creating offer")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {
        print("peerConnection:didChange:RTCSignalingState")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {
        print("peerConnection:didAdd")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {
        print("peerConnection:didRemove:RTCMediaStream")
    }
    
    func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {
        print("peerConnectionShouldNegotiate")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {
        print("peerConnection:didChange:RTCIceConnectionState")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceGatheringState) {
        print("peerConnection:didChange:RTCIceGatheringState")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
        print("peerConnection:didGenerate")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove candidates: [RTCIceCandidate]) {
        print("peerConnection:didRemove:RTCIceCandidate")
    }
    
    func peerConnection(_ peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {
        print("peerConnection:didOpen")
    }
    
}

