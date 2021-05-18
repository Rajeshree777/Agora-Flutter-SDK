//
//  RtcSurfaceView.swift
//  RCTAgora
//
//  Created by LXH on 2020/4/15.
//  Copyright © 2020 Syan. All rights reserved.
//

import Foundation
import UIKit
import AgoraRtcKit

class RtcSurfaceView: UIView {
    private var surface: UIView
    private var canvas: AgoraRtcVideoCanvas
    private weak var channel: AgoraRtcChannel?
    
    private var effectName : String?
    private var totalJoinedUser: Int = 1
   
    init(frame: CGRect, _ uid: UInt, _ totalJoinedUser : UInt, _ isFrontCamera: Bool) {
        let height = UIScreen.main.bounds.size.height / CGFloat(totalJoinedUser)
        print("init total \(totalJoinedUser) === uid \(uid)")
        
        surface = UIView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: height)))        
        canvas = AgoraRtcVideoCanvas()
        canvas.view = surface
        super.init(frame: CGRect(origin: CGPoint.zero, size: CGSize(width: UIScreen.main.bounds.size.width, height: height)))
        addSubview(surface)
        addObserver(self, forKeyPath: observerForKeyPath(), options: .new, context: nil)
        
        self.totalJoinedUser = Int(totalJoinedUser)        
     }
    
    func observerForKeyPath() -> String {
        return "frame"
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        canvas.view = nil
        removeObserver(self, forKeyPath: observerForKeyPath(), context: nil)
    }
    
    func setData(_ engine: AgoraRtcEngineKit, _ channel: AgoraRtcChannel?, _ uid: UInt, _ effectName: String?, _ newJoinList: UInt) {
        self.channel = channel
        canvas.channelId = channel?.getId()
        canvas.uid = uid
        print("Effect \(effectName) uid = \(uid) total \(totalJoinedUser) === new \(newJoinList)")
        
        self.effectName = effectName
        
        self.totalJoinedUser = Int(newJoinList)
        
        setupVideoCanvas(engine)        
    }
       
    private func setupVideoCanvas(_ engine: AgoraRtcEngineKit) {
        let height = UIScreen.main.bounds.size.height / CGFloat(totalJoinedUser)

             // Remote User
            subviews.forEach {
                $0.removeFromSuperview()
            }
            surface = UIView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: height)))
            addSubview(surface)
            canvas.view = surface
        print("Setup Video Canvas Agora \(canvas.uid ) \(self.frame.size) === \(surface.frame)")
        
        if canvas.uid == 0 {
            engine.setupLocalVideo(canvas)
        } else {
            engine.setupRemoteVideo(canvas)
        }
    }
    
    func setRenderMode(_ engine: AgoraRtcEngineKit, _ renderMode: UInt) {
        print("Render Mode \(renderMode)")
        canvas.renderMode = AgoraVideoRenderMode(rawValue: renderMode)!
        setupRenderMode(engine)
    }
    
    func setMirrorMode(_ engine: AgoraRtcEngineKit, _ mirrorMode: UInt) {
        print("Mirror Mode \(mirrorMode)")
        canvas.mirrorMode = AgoraVideoMirrorMode(rawValue: mirrorMode)!
        setupRenderMode(engine)
    }
    
    private func setupRenderMode(_ engine: AgoraRtcEngineKit) {
        if canvas.uid == 0 {
            engine.setLocalRenderMode(canvas.renderMode, mirrorMode: canvas.mirrorMode)
        } else {
            if let `channel` = channel {
                channel.setRemoteRenderMode(canvas.uid, renderMode: canvas.renderMode, mirrorMode: canvas.mirrorMode)
            } else {
                engine.setRemoteRenderMode(canvas.uid, renderMode: canvas.renderMode, mirrorMode: canvas.mirrorMode)
            }
        }
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey: Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == observerForKeyPath() {
            if let rect = change?[.newKey] as? CGRect {
                print("Frame auto change \(canvas.uid)")
                surface.frame = CGRect(origin: CGPoint(x: 0, y: 0), size: rect.size)
            }
        }
    }
}
extension Notification.Name {
    static let effectChangeNotification = Notification.Name("effectChangeNotification")
    static let cameraModeChangeNotification = Notification.Name("cameraModeChangeNotification")
    static let flashModeChangeNotification = Notification.Name("flashModeChangeNotification")
    static let videoRecodingChangeNotification = Notification.Name("videoRecodingChangeNotification")
    static let audioChangeNotification = Notification.Name("audioChangeNotification")
    static let cameraPauseModeChangeNotification = Notification.Name("cameraPauseModeChangeNotification")
}
