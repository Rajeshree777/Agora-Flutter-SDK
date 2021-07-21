//
//  RtcSurfaceView.swift
//  RCTAgora
//
//  Created by LXH on 2020/4/15.
//  Copyright © 2020 Syan. All rights reserved.
//

import AgoraRtcKit
import Foundation
import UIKit

class RtcSurfaceView: UIView {
    private var surface: UIView
    private var canvas: AgoraRtcVideoCanvas
    private weak var channel: AgoraRtcChannel?
    private var banubaSdkManager = BanubaSdkManager()

    private var effectName : String?
    private var totalJoinedUser: Int = 1
    private var config = EffectPlayerConfiguration(renderMode: .video)

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

    init(frame: CGRect, _ uid: UInt, _ totalJoinedUser : UInt, _ isFrontCamera: Bool) {
        let height = UIScreen.main.bounds.size.height / CGFloat(totalJoinedUser)
        print("init total \(totalJoinedUser) === uid \(uid)")

        if (uid == 0) {
            if (!isFrontCamera) {
                print("Surface Back camera init")
                config = EffectPlayerConfiguration(renderMode: .video, isFrontCam: isFrontCamera)
            }
//            BanubaSdkManager.deinitialize()
//            BanubaSdkManager.initialize(
//                resourcePath: [Bundle.main.bundlePath + "/effects"], clientTokenString: banubaClientToken)

            surface = EffectPlayerView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: height)))
            //surface.layoutIfNeeded()
            banubaSdkManager.setup(configuration: config)
            banubaSdkManager.setRenderTarget(layer: surface.layer as! CAEAGLLayer, contentMode : RenderContentMode.resizeAspectFill, playerConfiguration: nil)
        }
        else {
            surface = UIView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: height)))
        }

        canvas = AgoraRtcVideoCanvas()
        canvas.view = surface
        super.init(frame: CGRect(origin: CGPoint.zero, size: CGSize(width: UIScreen.main.bounds.size.width, height: height)))
        addSubview(surface)
        addObserver(self, forKeyPath: observerForKeyPath(), options: .new, context: nil)

        self.totalJoinedUser = Int(totalJoinedUser)
        if (uid == 0) {
//            setUpRenderSize()
//            setUpRenderTarget()
            NotificationCenter.default.addObserver(self, selector: #selector(onEffectChange), name: .effectChangeNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(onCameraModeChange), name: .cameraModeChangeNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(destroyBanubaEffect), name: .destroyBanubaEffectNotification, object: nil)
            NotificationCenter.default.addObserver(self, selector: #selector(cameraPauseModeChange), name: .cameraPauseModeChangeNotification, object: nil)
        }
     }

      @objc func cameraPauseModeChange(notification: Notification) {
               print("cameraPauseModeChangeNotification \(notification.object)")
               if let isPause = notification.object as? Bool {
                   if (isPause) {
                       banubaSdkManager.input.stopCamera()
     //                   banubaSdkManager.destroyEffectPlayer()
                       banubaSdkManager.stopEffectPlayer()
                   }
                   else {
                       banubaSdkManager.input.startCamera()
                       banubaSdkManager.startEffectPlayer()
                   }
               }
           }

    @objc func onEffectChange(notification: Notification) {
//        print("On select effect myFunction \(notification.object) ==== \(banubaSdkManager.currentEffect())" );
    //    banubaSdkManager.stopEffectPlayer()

        if let effectName = notification.object as? String {
            self.effectName = effectName
            _ = banubaSdkManager.loadEffect(effectName)
        }
   //     banubaSdkManager.startEffectPlayer()
    }

    @objc func onCameraModeChange(notification: Notification) {

        let cameraMode = banubaSdkManager.input.currentCameraSessionType
        var newCameraMode : CameraSessionType
        if (cameraMode == .FrontCameraVideoSession) {
            newCameraMode = .BackCameraVideoSession
        } else if (cameraMode == .BackCameraVideoSession) {
            newCameraMode = .FrontCameraVideoSession
        } else if (cameraMode == .FrontCameraPhotoSession) {
            newCameraMode = .BackCameraPhotoSession
        } else {
            newCameraMode = .FrontCameraPhotoSession
        }
        banubaSdkManager.input.switchCamera(to: newCameraMode) {
            print("Camera Switched")
        }
    }

    @objc func destroyBanubaEffect(notification: Notification) {
        print("Banuba input camera stop & destory effect")
        banubaSdkManager.input.stopCamera()
        banubaSdkManager.destroyEffectPlayer()
        banubaSdkManager.stopEffectPlayer()
    }

    func observerForKeyPath() -> String {
        return "frame"
    }

    @available(*, unavailable)
    required init?(coder _: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        if (canvas.uid == 0) {
            NotificationCenter.default.removeObserver(self, name: .effectChangeNotification, object: nil)
            NotificationCenter.default.removeObserver(self, name: .cameraModeChangeNotification, object: nil)
            NotificationCenter.default.removeObserver(self, name: .destroyBanubaEffectNotification, object: nil)
            NotificationCenter.default.removeObserver(self, name: .cameraPauseModeChangeNotification, object: nil)
        }
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
        if (uid == 0)
        {
           banubaSdkManager.output?.startForwardingFrames(handler: { (pixelBuffer) -> Void in
               self.pushPixelBufferIntoAgoraKit(pixelBuffer: pixelBuffer, engine)
           })
        }
    }

    override func layoutSubviews() {
        if (canvas.uid == 0) {
            banubaSdkManager.effectPlayer?.setEffectVolume(0)
            banubaSdkManager.input.startCamera()
            if (self.effectName != nil) {
                _ = banubaSdkManager.loadEffect(self.effectName ?? "")
            }
           banubaSdkManager.startEffectPlayer()
        }
    }

    private func setupVideoCanvas(_ engine: AgoraRtcEngineKit) {
        let height = UIScreen.main.bounds.size.height / CGFloat(totalJoinedUser)

        if canvas.uid != 0 {
             // Remote User
            subviews.forEach {
                $0.removeFromSuperview()
            }
            surface = UIView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: height)))
            addSubview(surface)
            canvas.view = surface
        }
        else {
            surface.frame = CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: UIScreen.main.bounds.size.width, height: height))

//            banubaSdkManager.stopEffectPlayer()
//            banubaSdkManager.removeRenderTarget()
//            setUpRenderSize()

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

//    func setUpRenderSize() {
//        print("Size \(surface.frame.size)")
//
//        switch UIApplication.shared.statusBarOrientation {
//        case .portrait:
//            config.orientation = .deg90
//            config.renderSize = surface.frame.size
//            setUpRenderTarget()
//        case .portraitUpsideDown:
//            config.orientation = .deg270
//            config.renderSize = CGSize(width: 720, height: 1280)
//            setUpRenderTarget()
//        case .landscapeLeft:
//            config.orientation = .deg180
//            config.renderSize = CGSize(width: 1280, height: 720)
//            setUpRenderTarget()
//        case .landscapeRight:
//            config.orientation = .deg0
//            config.renderSize = CGSize(width: 1280, height: 720)
//            setUpRenderTarget()
//        default:
//            setUpRenderTarget()
//        }
//    }

//    private func setUpRenderTarget() {
//
////        guard let effectView = self.effectView.layer as? CAEAGLLayer else { return }
////        sdkManager.setRenderTarget(layer: effectView, playerConfiguration: nil)
//        banubaSdkManager.setRenderTarget(layer: surface.layer as! CAEAGLLayer, playerConfiguration: nil)
//
////        sdkManager.startEffectPlayer()
//    }

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
            if let channel = channel {
                channel.setRemoteRenderMode(canvas.uid, renderMode: canvas.renderMode, mirrorMode: canvas.mirrorMode)
            } else {
                engine.setRemoteRenderMode(canvas.uid, renderMode: canvas.renderMode, mirrorMode: canvas.mirrorMode)
            }
        }
    }

    override func observeValue(forKeyPath keyPath: String?, of _: Any?, change: [NSKeyValueChangeKey: Any]?, context _: UnsafeMutableRawPointer?) {
        if keyPath == observerForKeyPath() {
            if let rect = change?[.newKey] as? CGRect {
                print("Frame auto change \(canvas.uid)")
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
    static let cameraModeChangeNotification = Notification.Name("cameraModeChangeNotification")
    static let destroyBanubaEffectNotification = Notification.Name("destroyBanubaEffectNotification")
    static let flashModeChangeNotification = Notification.Name("flashModeChangeNotification")
    static let videoRecodingChangeNotification = Notification.Name("videoRecodingChangeNotification")
    static let audioChangeNotification = Notification.Name("audioChangeNotification")
    static let cameraPauseModeChangeNotification = Notification.Name("cameraPauseModeChangeNotification")
}
