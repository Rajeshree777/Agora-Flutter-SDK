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
    private var banubaSdkManager = BanubaSdkManager()
    
    internal let banubaClientToken = "CJOTaz7ChzHGe97okh0KcDRKfEC8pkAjGbiOTzWJyEaMqlrVtV87NGPBUPLRGelwp9IcJVPjeeYVAiDzShXDW7UsioCXOLTGR4VoKK+Ljg4e1qwndj3t2WaiwEeT6eOwCU3tZMQA5cZRzlvNSl5EDEj4gVIVGa4Wwcb+w/KF/RY1IztsFnz7JiDCIkCdTOF6FPkuaL9vq5bOcZ+rkS/Pd0M4IBykArCuBg7S02qSr1HVH/w635y4YZYetsYeoHL93ms+6Ktkw52/rsxMyfnSmZtQyCoFzgNg0NHbnuttoTA7Wki3seNJaqJE9CymwqojJxzYXL/8/1kg8uaQY13fJPlbjInQyFZzeG5dpV2jkDBMclsZ0HS5nVdJuJcfRAhiNsAZtSe0kVjVgpvaMSEaIaoU+La/DsK/BAZwta56fN8/o3sAiUaxzr7GbHplFNCOfBqN+7FAGv3sLpIJpxUoxbPgtl5y/f+vyGkDWr5+pzTZ5gIscm7GazIVibJb9wAGWXhuDJBc71GuI7HZzxKkJruCSwJnXBoEedBaGjqjAljwL8TDpAfvpxP12AG7r2FfGJR8t3mR+B/WbN3nIxlFx2xqAf88MoDiWzhMTeJuy577UmRUiolqS+dj/SukJUoMaXeB2IcnDCLEp84m/FkP06GAub5Y9l16TZRXxqIqmP8AD3EBXG0MfiF0XUvCaiqUdebKH4mz7vViBq8IFsI8cP1DPqWwLNYzpQBmY+h5OUUa/Mn7f/2dJX9qiVF/4F9jUKJhWLEnpVbQQWPIn+6fcCwplNdngg3B5/KSC7UZ04FcyIyBiyuYr83TV2K6mMgWJNmSNcZFfeqmPw6/CJQ9owmL7iFA/QotN2B9Fq1amWtsZU1FJah2jz6Cytgi9HFiAFf2OB17qfBBE4OG4EtTSkN/67VCqGb0FgL3BV0Cnixed8gNDr9uDi1H5KDMfFS+s05ZpsKt9K8SqOLqsjdrD2QJ+XQDIMyF7OuOy18upOhWhqeG4LRXZEfhityK2aUNVQErGheLbBkNRG2laJhmK/841dsO+JAjshEMpIZzkscN34+MgRk+R9R9VXtrBOybMr1Z6+b3vWGA05LMbF15MaU9uGxPwWXP6DNFYXpe8PoHEkvDp342xxoa92UhcSG0IdNVRkt6f5u8yPO/tm+QZSULKeWp2kp6ugOk1cosyRdgmgUOrkoDufKPujAITNee7vzkJfhb4TMhn5mwjcb9tMFZdcGdvQLF/MOiWI8wpOC+ICNXPwRzewQKAtqwlT0iHbZvqL0+0ssgc40fGliUOfEPxrjkD6DhD0RrA8bVHVdUtuRuMayhv0HgYz3FgSnPszTwIcSxvKGEb4JdgT64ZuROPvRDE/eGUKD2P+7tBU7Y8D37Oo3zXZH6Qq/Q4NFqtP0liHED1anjtmT9/to+fsskDeLOKsExcEwhFy3QSp29kTvX3tzdYqZ3VLDXkwQnMZNVKsP2L5zlUXH0tbPIJJSHKV+F5U6hoNhNVThQjPuUAwBFxy/IPUHp/5l7mSswRf6kfZG85Kblcd8G2YHq8xZd1Yvx6coHpIe+Mm3OyxMu0R4Tw2WT3ypwCZmST6e0svZfuYUJwgoQHkwLGjSXnzV2pfhK7ZtVC1a+nfZfOrgG66zT9L+Cvds/2NmXlF2tnOybuj0xA1810pQYTLxW9y+YeC+MbVbh3hTcpyWb4w8nydYeGcwoG+9tBV3ej/U+HsiYSMqfv1ltY9aqbLBWpKPz2kJ3fFHK2bDE9QXGZxg8/RrC4kjTeh5OmdnykJPSg9vXhOIUMLs5KKke6IlcKkgNnVvBPq3Ya2YFXCygJtSYOyBoZ32/NWflCe5Q78tsAkMdJeeQaMJyjx7G5fZj0Bd7K4VbnuqcZwthhb4q1bBj1i3yvYk2cgUmndi6UEG7yQ0d9Dbe2xxOM8gMW63zLUdLv4CRAyXw4PAyuIb0zWtbNDE7ekayAHIQOjQm+MScXyJUTif1bP+F"
    
    // override init(frame: CGRect) {
    //     //        surface = UIView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: frame.size))
    //     //        canvas = AgoraRtcVideoCanvas()
    //     //        canvas.view = surface
    //     //        super.init(frame: frame)
    //     //        addSubview(surface)
    //     //        addObserver(self, forKeyPath: observerForKeyPath(), options: .new, context: nil)
        
    //     //         new 1
    //     print("Frame init Banuba \(frame)")
    //     BanubaSdkManager.deinitialize()
    //     BanubaSdkManager.initialize(
    //         resourcePath: [Bundle.main.bundlePath + "/effects"], clientTokenString: banubaClientToken        )
    //     //        _ = banubaSdkManager.loadEffect("HeadphoneMusic")
        
    //     surface = EffectPlayerView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: UIScreen.main.bounds.size))
    //     surface.layoutIfNeeded()
    //     banubaSdkManager.setup(configuration: EffectPlayerConfiguration(renderMode: .video))
    //     banubaSdkManager.setRenderTarget(layer: surface.layer as! CAEAGLLayer, playerConfiguration: nil)
        
    //     //        banubaSdkManager.input.startCamera()
        
    //     canvas = AgoraRtcVideoCanvas()
    //     canvas.view = surface
    //     super.init(frame: CGRect(origin: CGPoint.zero, size: UIScreen.main.bounds.size))
    //     addSubview(surface)
    //     addObserver(self, forKeyPath: observerForKeyPath(), options: .new, context: nil)
        
    //     //        banubaSdkManager.effectPlayer?.setEffectVolume(0)
    //     //        banubaSdkManager.effectPlayer?.playbackPlay()
    //     //        banubaSdkManager.startEffectPlayer()
        
    //     /// new
    //     //        surface = EffectPlayerView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: frame.size))
    //     //        surface.layoutIfNeeded()
    //     //        banubaSdkManager.setup(configuration: EffectPlayerConfiguration(renderMode: .video))
    //     //        banubaSdkManager.setRenderTarget(layer: surface.layer as! CAEAGLLayer, playerConfiguration: nil)
    //     //
    //     //        banubaSdkManager.input.startCamera()
    //     //        _ = banubaSdkManager.loadEffect("UnluckyWitch")
    //     //
    //     //        //        self.subviews.forEach {
    //     //        //            $0.removeFromSuperview()
    //     //        //        }
    //     //        print("Init Banuba Surfaceview")
    //     //        canvas = AgoraRtcVideoCanvas()
    //     //        canvas.uid = 0
    //     //        canvas.renderMode = .hidden
    //     //        canvas.view = surface
    //     //
    //     //        super.init(frame: frame)
    //     //        addSubview(surface)
    //     //        addObserver(self, forKeyPath: observerForKeyPath(), options: .new, context: nil)
    // }

    init(frame: CGRect, _ uid: UInt) {                        
        if (uid == 0) {
//            BanubaSdkManager.deinitialize()
//            BanubaSdkManager.initialize(
//                resourcePath: [Bundle.main.bundlePath + "/effects"], clientTokenString: banubaClientToken)
            
            surface = EffectPlayerView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height)))
            //surface.layoutIfNeeded()
            banubaSdkManager.setup(configuration: EffectPlayerConfiguration(renderMode: .video))
            banubaSdkManager.setRenderTarget(layer: surface.layer as! CAEAGLLayer, contentMode : RenderContentMode.resize, playerConfiguration: nil)
        }
        else {
            surface = UIView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height)))
        }
        
        canvas = AgoraRtcVideoCanvas()
        canvas.view = surface
        super.init(frame: CGRect(origin: CGPoint.zero, size: CGSize(width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height)))
        addSubview(surface)
        addObserver(self, forKeyPath: observerForKeyPath(), options: .new, context: nil)
        
        if (uid == 0) {
            NotificationCenter.default.addObserver(self, selector: #selector(onEffectChange), name: .effectChangeNotification, object: nil)
        }
     }
    
    @objc func onEffectChange(notification: Notification) {
//        print("On select effect myFunction \(notification.object) ==== \(banubaSdkManager.currentEffect())" );
       banubaSdkManager.stopEffectPlayer()
       
        if let effectName = notification.object as? String {
            _ = banubaSdkManager.loadEffect(effectName)
        }        
        banubaSdkManager.startEffectPlayer()
    }
    
    func observerForKeyPath() -> String {
        return "frame"
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    deinit {
        if (canvas.uid == 0) {
            NotificationCenter.default.removeObserver(self, name: .effectChangeNotification, object: nil)
        }
        canvas.view = nil
        removeObserver(self, forKeyPath: observerForKeyPath(), context: nil)
    }
    
    func setData(_ engine: AgoraRtcEngineKit, _ channel: AgoraRtcChannel?, _ uid: UInt) {
        self.channel = channel
        canvas.channelId = channel?.getId()
        canvas.uid = uid

        setupVideoCanvas(engine)
        if (uid == 0)
        {
           banubaSdkManager.output?.startForwardingFrames(handler: { (pixelBuffer) -> Void in
               self.pushPixelBufferIntoAgoraKit(pixelBuffer: pixelBuffer, engine)
           })
        }
    }
    
    override func layoutSubviews() {
        if (canvas.uid == 0) {
//            banubaSdkManager.effectPlayer?.setEffectVolume(0)
            banubaSdkManager.input.startCamera()
//           _ = banubaSdkManager.loadEffect("HeadphoneMusic")
           banubaSdkManager.startEffectPlayer()
        }
    }
   
    private func setupVideoCanvas(_ engine: AgoraRtcEngineKit) {
        if canvas.uid != 0 {
             // Remote User
            subviews.forEach {
                $0.removeFromSuperview()
            }
            surface = UIView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height)))
            addSubview(surface)
            canvas.view = surface
        }
        else {
            surface.frame = CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height))
            canvas.view = surface
        }
        print("Setup Video Canvas Banuba \(canvas.uid ) \(self.frame.size) === \(surface.frame)")
        
        if canvas.uid == 0 {
            engine.setupLocalVideo(canvas)
            
            engine.setExternalVideoSource(true, useTexture: false, pushMode: true)
        } else {
            engine.setupRemoteVideo(canvas)
        }
        engine.enableVideo()
    }
    
    func setRenderMode(_ engine: AgoraRtcEngineKit, _ renderMode: UInt) {
        canvas.renderMode = AgoraVideoRenderMode(rawValue: renderMode)!
        setupRenderMode(engine)
    }
    
    func setMirrorMode(_ engine: AgoraRtcEngineKit, _ mirrorMode: UInt) {
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
                surface.frame = CGRect(origin: CGPoint(x: 0, y: 0), size: rect.size)
            }
        }
    }
    
    private func pushPixelBufferIntoAgoraKit(pixelBuffer: CVPixelBuffer, _ engine: AgoraRtcEngineKit) {
        let videoFrame = AgoraVideoFrame()
        //Video format = 12 means iOS texture (CVPixelBufferRef)
        videoFrame.format = 12
        videoFrame.time = CMTimeMakeWithSeconds(NSDate().timeIntervalSince1970, preferredTimescale: 1000)
        videoFrame.textureBuf = pixelBuffer
        videoFrame.rotation = 180
        engine.pushExternalVideoFrame(videoFrame)
    }
}
extension Notification.Name {
    static let effectChangeNotification = Notification.Name("effectChangeNotification")
}
