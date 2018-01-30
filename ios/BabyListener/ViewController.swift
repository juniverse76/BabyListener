//
//  ViewController.swift
//  BabyListener
//
//  Created by 김준성 on 02/01/2018.
//  Copyright © 2018 김준성. All rights reserved.
//

import UIKit
import Firebase
import WebRTC

class ViewController: UIViewController, DetectorDelegate, RTCPeerConnectionDelegate {

    let WEBRTC_TABLE_NAME = "webrtc"
    private var peerConnection: RTCPeerConnection!
    private var audioContraint: RTCMediaConstraints!
    
    let detector = Detector()

    @IBOutlet weak var Label: UILabel!
    @IBOutlet weak var CanvasView: UIImageView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
//        detector.setDetectorDelegate(self)
//        detector.setSensitivity(2000)
        initWebRTC()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    private var running: Bool = false
    @IBAction func onStart(_ sender: UIButton) {
        createOffer()
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
        
        print("initWebRTC")
        let audioContraint = RTCMediaConstraints.init(mandatoryConstraints: ["OfferToReceiveAudio": "true"], optionalConstraints: ["OfferToReceiveAudio": "true"])
        let factory = RTCPeerConnectionFactory.init()
        let configuration = RTCConfiguration()
        configuration.iceServers = [RTCIceServer.init(urlStrings: ["stun:stun.l.google.com:19302"])]
        peerConnection = factory.peerConnection(with: configuration, constraints: audioContraint, delegate: self)

        let audioSource = factory.audioSource(with: audioContraint)
        let audioTrack = factory.audioTrack(with: audioSource, trackId: "audio_track_id_ios")
        let audioStream = factory.mediaStream(withStreamId: "audio_stream_id_ios")
        audioStream.addAudioTrack(audioTrack)
//        peerConnection.add(audioStream)
        
//        peerConnection.offer(for: audioContraint, completionHandler: {(description, error) in
//            let desc: String = (description?.sdp)!
//            print("description? \(desc)")
//            peerConnection.setLocalDescription(description!, completionHandler: nil)
//
//            var type: String
//            if description?.type == .offer {
//                type = "offer"
//            } else {
//                type = "answer"
//            }
//
//            let signalingRef = Database.database().reference(withPath: self.WEBRTC_TABLE_NAME).child("uiser_id")
//            let signalData = ["type": type, "description": description?.description]
//            signalingRef.setValue(signalData)
//        })
        print("initWebRTC done")
    }
    
    func createOffer() {
        print("about to create offer")
        peerConnection.offer(for: audioContraint, completionHandler: {(description, error) in
            let desc: String = (description?.sdp)!
            print("description? \(desc)")
            self.peerConnection.setLocalDescription(description!, completionHandler: nil)
            
            var type: String
            if description?.type == .offer {
                type = "offer"
            } else {
                type = "answer"
            }
            
            let signalingRef = Database.database().reference(withPath: self.WEBRTC_TABLE_NAME).child("uiser_id")
            let signalData = ["type": type, "description": description?.description]
            signalingRef.setValue(signalData)
        })
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

