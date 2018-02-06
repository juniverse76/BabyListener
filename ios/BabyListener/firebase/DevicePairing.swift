//
//  DevicePairing.swift
//  BabyListener
//
//  Created by 김준성 on 05/01/2018.
//  Copyright © 2018 김준성. All rights reserved.
//

import Foundation
import Firebase

class DevicePairing {
    static let SUCCESS = 0
    static let UNKNOWN_CODE = -1
    static let TIMEOUT = -2
    
    let PAIR_TABLE_NAME = "pairing"
    let maxWaitingTime = 3 * 60 * 1000
    
    func registerPairRequest(onResult: @escaping (_ result: String?) -> Void) {
        guard let userId = Auth.auth().currentUser?.uid else {
            print("not logged in")
            onResult(nil)
            return
        }
        
        print("making request")
        let pairDbRef = Database.database().reference(withPath: PAIR_TABLE_NAME)
        pairDbRef.observeSingleEvent(of: DataEventType.value, with: {(snapshot) in
            var code: String
            var found: Bool
            repeat {
                found = false
                code = String(arc4random_uniform(900000) + 100000)
                print("checking code \(code)")
                let enumerator = snapshot.children
                while let pairReq = enumerator.nextObject() as? DataSnapshot {
                    if pairReq.key == code {
                        found = true
                        break
                    }
                }
            } while (found)
            
            let pairData = ["reqId": userId,
                            "time": UInt(NSDate().timeIntervalSince1970 * 1000)] as [String : Any]
//            let pairData = PairingData(reqId: userId, time: UInt(CACurrentMediaTime() * 1000), ackId: nil)
            print("adding pairing request \(pairData)")
            pairDbRef.child(code).setValue(pairData)
           
            onResult(code)
        })
    }
    
    var acknowledgeObserverHandle: UInt = 0
    func waitForAcknowledge(code: String, onResult: @escaping (_ result: Bool) -> Void) {
        if acknowledgeObserverHandle != 0 { return }
        
        let codeDataRef = Database.database().reference(withPath: PAIR_TABLE_NAME).child(code)
            acknowledgeObserverHandle = codeDataRef.observe(DataEventType.value, with: {(snapshot) in
//                print("compiling???")
                guard let pairingData = snapshot.value as? [String : Any] else {
                    self.cancelWaitingForAck(code)
                    return
                }
                
                guard let ackId = pairingData["ackId"] as? String else {
                    return
                }
                
                print("answered!!! \(ackId)")
                self.savePartnerId(partnerId: ackId)
                
                self.cancelWaitingForAck(code)
                
                onResult(true)
                })
        
        print("acknowledgeObserverHandle? \(acknowledgeObserverHandle)")
        // todo timer
    }
    
    func cancelWaitingForAck(_ code: String) {
        // todo
     
        let codeDataRef = Database.database().reference(withPath: PAIR_TABLE_NAME).child(code)
        codeDataRef.removeObserver(withHandle: acknowledgeObserverHandle)
        codeDataRef.removeValue()
        acknowledgeObserverHandle = 0
    }
    
    func acknowledgePairing(code: String, onResult: @escaping (_ result: Int) -> Void) -> Bool {
        guard let userId = Auth.auth().currentUser?.uid else {
            print("not logged in")
            return false
        }
        
        let codeDataRef = Database.database().reference(withPath: PAIR_TABLE_NAME).child(code)
        codeDataRef.observeSingleEvent(of: DataEventType.value, with: {(snapshot) in
            guard let pairingData = snapshot.value as? [String : Any] else {
                onResult(DevicePairing.UNKNOWN_CODE)
                return
            }
            
            let now = UInt(NSDate().timeIntervalSince1970 * 1000)
            if now - (pairingData["time"] as! UInt) > self.maxWaitingTime {
                onResult(DevicePairing.TIMEOUT)
                return
            }
            
            let updateData = ["reqId": pairingData["reqId"] as! String,
                            "time": pairingData["time"] as! UInt,
                            "ackId": userId] as [String : Any]
            codeDataRef.updateChildValues(updateData)
            
            onResult(DevicePairing.SUCCESS)
            
            self.savePartnerId(partnerId: pairingData["reqId"] as! String)
        })
        return true
    }
    
    func unpairDevice() {
        // todo
    }
    
    func savePartnerId(partnerId: String) {
        // todo
        let userId = Auth.auth().currentUser?.uid
        print("I am \(userId) and my parnter is \(partnerId)")
    }
    
    func checkPartnerId(onResult: (_ result: Bool) -> Void) {
        // todo
    }
}
