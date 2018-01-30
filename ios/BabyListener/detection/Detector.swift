//
//  Detector.swift
//  BabyListener
//
//  Created by 김준성 on 15/01/2018.
//  Copyright © 2018 김준성. All rights reserved.
//

import Foundation
import AVFoundation

public enum DetectorState: String {
    case none
    case stopped
    case preparing
    case running
    case paused
}

public protocol DetectorDelegate : NSObjectProtocol {
    func onStateChanged(_ state:DetectorState)
    func onDetecting(_ samples:UnsafeMutableBufferPointer<Int16>)
    func onDetected()
}

class Detector: NSObject, AVCaptureAudioDataOutputSampleBufferDelegate {

    private let samplingRate = 8000
    private let numChannels = 1
    private let hitCounts = 10
    
    private let audioSession = AVCaptureSession()
    
    private var audioConnection: AVCaptureConnection!
    private var queue: DispatchQueue!
    private var delegate: DetectorDelegate!
    private var state: DetectorState = .none
    private var sensitivity: Int64 = 0
    
    private var initialized: Bool = false
    
    private func initRecording() -> Bool {

        if initialized { return true }

        guard let audioDevice = AVCaptureDevice.default(for: .audio) else { return false }
        
        let audioIn: AVCaptureDeviceInput
        do {
            audioIn = try AVCaptureDeviceInput(device: audioDevice)
        } catch {
            return false
        }

        guard audioSession.canAddInput(audioIn) else { return false }
        audioSession.addInput(audioIn)
        

        let audioOut = AVCaptureAudioDataOutput()
        queue = DispatchQueue(label: "xyz.juniverse.babylistener_queue")
        audioOut.setSampleBufferDelegate(self, queue: queue)
//        audioOut.setSampleBufferDelegate(self, queue: DispatchQueue.main)
        
        guard audioSession.canAddOutput(audioOut) else { return false }
        audioSession.addOutput(audioOut)
        
        audioConnection = audioOut.connection(with: .audio)
        
        initialized = true
        
        return true
    }
    
    func setDetectorDelegate(_ delegate: DetectorDelegate) {
        self.delegate = delegate
    }
    
    func setSensitivity(_ sensitivity: Int) {
        self.sensitivity = Int64(sensitivity)
        print("sensitivity? \(sensitivity)")
    }
    
    func start() -> Bool {
        guard initRecording() else {
            return false
        }
        
        if !audioSession.isRunning {
            queue.async {
                self.audioSession.startRunning()
                self.state = .running
                self.delegate.onStateChanged(self.state)
            }
            return true
        }
        return false
    }
    
    func stop() {
        if audioSession.isRunning {
            audioSession.stopRunning()
            self.state = .stopped
            self.delegate.onStateChanged(self.state)
        }
    }
    
    func pause() {
        self.stop()
    }
    
//    func captureOutput(_ captureOutput: AVCaptureOutput, didOutputSampleBuffer sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
//        print("capturing something???")
//    }
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        
//        something(sampleBuffer)
        
        var buffer: CMBlockBuffer? = nil
        var audioBufferList = AudioBufferList(mNumberBuffers: 1,
                                              mBuffers: AudioBuffer(mNumberChannels: 1, mDataByteSize: 0, mData: nil))
        CMSampleBufferGetAudioBufferListWithRetainedBlockBuffer(
            sampleBuffer,
            nil,
            &audioBufferList,
            MemoryLayout<AudioBufferList>.size,
            nil,
            nil,
            UInt32(kCMSampleBufferFlag_AudioBufferList_Assure16ByteAlignment),
            &buffer
        )
        
        let abl = UnsafeMutableAudioBufferListPointer(&audioBufferList)
//        var max:Int64 = 0
//        var min:Int64 = 0
//        var count:Int = 0
        for buff in abl {
            let samples = UnsafeMutableBufferPointer<Int16>(start: UnsafeMutablePointer(OpaquePointer(buff.mData)),
                                                            count: Int(buff.mDataByteSize)/MemoryLayout<Int16>.size)
            
            /*
            if detect(samples) {
                self.delegate.onDetected()
                return
            }
            */
            self.delegate.onDetecting(samples)
        }
    }
    
    private func detect(_ samples: UnsafeMutableBufferPointer<Int16>) -> Bool {
        var count: Int = 0
        for sample in samples {
            let s = abs(Int64(sample))
            if s > sensitivity {
                print("loud s? \(s)")
                count += 1
            }
            
            if count > hitCounts {
                return true
            }
        }
        return false
    }
    
    private func something(_ sampleBuffer: CMSampleBuffer) {
        var buffer: CMBlockBuffer? = nil
        var audioBufferList = AudioBufferList(mNumberBuffers: 1,
                                              mBuffers: AudioBuffer(mNumberChannels: 1, mDataByteSize: 0, mData: nil))
        CMSampleBufferGetAudioBufferListWithRetainedBlockBuffer(
            sampleBuffer,
            nil,
            &audioBufferList,
            MemoryLayout<AudioBufferList>.size,
            nil,
            nil,
            UInt32(kCMSampleBufferFlag_AudioBufferList_Assure16ByteAlignment),
            &buffer
        )
        let abl = UnsafeMutableAudioBufferListPointer(&audioBufferList)
        var sum:Int64 = 0
        var count:Int = 0
        var bufs:Int = 0
        for buff in abl {
            let samples = UnsafeMutableBufferPointer<Int16>(start: UnsafeMutablePointer(OpaquePointer(buff.mData)),
                                                            count: Int(buff.mDataByteSize)/MemoryLayout<Int16>.size)
            for sample in samples {
                let s = Int64(sample)
                sum = (sum + s*s)
                count += 1
            }
            bufs += 1
        }
        print( "found \(count) samples in \(bufs) buffers, RMS is \(sqrt(Float(sum)/Float(count)))" )
    }
}
