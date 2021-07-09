//
//  BanubaSurfaceView.swift

import Foundation
import UIKit

class BanubaSurfaceView: UIView {
    private var surface: EffectPlayerView
    private var banubaSdkManager = BanubaSdkManager()

    private var effectName : String?

    private var config = EffectPlayerConfiguration(renderMode: .video)

    var torceMode: AVCaptureDevice.TorchMode = AVCaptureDevice.TorchMode.off

    init(frame: CGRect, _ uid: UInt, _ isFrontCamera: Bool) {
        //            BanubaSdkManager.deinitialize()
        //            BanubaSdkManager.initialize(
        //                resourcePath: [Bundle.main.bundlePath + "/effects"], clientTokenString: banubaClientToken)

        if (!isFrontCamera) {
            print("Surface Back camera init")
            config = EffectPlayerConfiguration(renderMode: .video, isFrontCam: isFrontCamera)
        }

        surface = EffectPlayerView(frame: CGRect(origin: CGPoint(x: 0, y: 0), size: frame.size.width > 0 ? frame.size : CGSize(width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height)
        ))
        //surface.layoutIfNeeded()
        banubaSdkManager.setup(configuration: config)
        //            banubaSdkManager.setRenderTarget(layer: surface.layer as! CAEAGLLayer, contentMode : RenderContentMode.resizeAspectFill, playerConfiguration: nil)

        surface.contentMode = UIView.ContentMode.scaleAspectFit
        super.init(frame: CGRect(origin: CGPoint.zero, size: CGSize(width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height)))
        addSubview(surface)
        addObserver(self, forKeyPath: observerForKeyPath(), options: .new, context: nil)

        setUpRenderSize()
        surface.effectPlayer = banubaSdkManager.effectPlayer

        NotificationCenter.default.addObserver(self, selector: #selector(onEffectChange), name: .effectChangeNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(onCameraModeChange), name: .cameraModeChangeNotification, object: nil)

        NotificationCenter.default.addObserver(self, selector: #selector(flashModeChange), name: .flashModeChangeNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(videoRecordChange), name: .videoRecodingChangeNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(audioRecordChange), name: .audioChangeNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(destroyBanubaEffectPlayer), name: .destroyBanubaEffectNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(cameraPauseModeChange), name: .cameraPauseModeChangeNotification, object: nil)

//        banubaSdkManager.input.switchCamera(to: .BackCameraVideoSession) {
//            print("Camera Switched")
//            _ = self.banubaSdkManager.input.setTorch(mode: self.torceMode)
//        }
    }

    func setUpRenderSize() {
        switch UIApplication.shared.statusBarOrientation {
        case .portrait:
            config.orientation = .deg90
            config.renderSize = CGSize(width: 720, height: 1280)
            banubaSdkManager.autoRotationEnabled = false
            setUpRenderTarget()
        case .portraitUpsideDown:
            config.orientation = .deg270
            config.renderSize = CGSize(width: 720, height: 1280)
            setUpRenderTarget()
        case .landscapeLeft:
            config.orientation = .deg180
            config.renderSize = CGSize(width: 1280, height: 720)
            setUpRenderTarget()
        case .landscapeRight:
            config.orientation = .deg0
            config.renderSize = CGSize(width: 1280, height: 720)
            setUpRenderTarget()
        default:
            setUpRenderTarget()
        }
    }

    private func setUpRenderTarget() {
        guard let effectView = self.surface.layer as? CAEAGLLayer else { return }
        banubaSdkManager.setRenderTarget(layer: effectView, playerConfiguration: nil)
        banubaSdkManager.startEffectPlayer()
    }

    @objc func onEffectChange(notification: Notification) {
        //        print("On select effect myFunction \(notification.object) ==== \(banubaSdkManager.currentEffect())" );
        banubaSdkManager.stopEffectPlayer()

        if let effectName = notification.object as? String {
            self.effectName = effectName
            _ = banubaSdkManager.loadEffect(effectName)
        }
        banubaSdkManager.startEffectPlayer()
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

    @objc func flashModeChange(notification: Notification) {
        torceMode = banubaSdkManager.input.setTorch(mode: torceMode == .on ? AVCaptureDevice.TorchMode.off : AVCaptureDevice.TorchMode.on)
    }
    var fileNameCount : Int = 1
    @objc func videoRecordChange(notification: Notification) {
        print("Start Stop Video Record")
        if let fileUrl = notification.object as? String {
            if (banubaSdkManager.output?.isRecording ?? false) {
                fileNameCount += 1
                banubaSdkManager.output?.stopVideoCapturing(cancel: false)
                banubaSdkManager.input.stopAudioCapturing()
            }
            else {
                banubaSdkManager.input.startAudioCapturing()

                banubaSdkManager.output?.startVideoCapturing(fileURL: URL(fileURLWithPath: fileUrl), configuration: OutputConfiguration(applyWatermark: false, adjustDeviceOrientation: true, mirrorFrontCamera: false) ,completion: { (status, error) in
                    print("Start Video \(fileUrl) === \(status) === \(String(describing: error))")
                })
            }
        }
    }

    @objc func audioRecordChange(notification: Notification) {
        if let isAudio = notification.object as? Bool {
            if (isAudio) {
                banubaSdkManager.input.startAudioCapturing()
            } else {
                banubaSdkManager.input.stopAudioCapturing()
            }
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

        @objc func destroyBanubaEffectPlayer(notification: Notification) {
          print("destroyBanubaEffectPlayer from surfaceview")
             banubaSdkManager.destroyEffectPlayer()
         }


    func observerForKeyPath() -> String {
        return "frame"
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {

        NotificationCenter.default.removeObserver(self, name: .effectChangeNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: .cameraModeChangeNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: .flashModeChangeNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: .videoRecodingChangeNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: .audioChangeNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: .destroyBanubaEffectNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: .cameraPauseModeChangeNotification, object: nil)

        removeObserver(self, forKeyPath: observerForKeyPath(), context: nil)
    }

    func setData(_ uid: UInt, _ effectName: String?) {
        print("Effect \(effectName)")

        self.effectName = effectName
    }

    override func layoutSubviews() {

        banubaSdkManager.input.startCamera()
        banubaSdkManager.effectPlayer?.setEffectVolume(0)
        if (self.effectName != nil) {
            _ = banubaSdkManager.loadEffect(self.effectName ?? "")
        }
        banubaSdkManager.startEffectPlayer()
    }

    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey: Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == observerForKeyPath() {
            if let rect = change?[.newKey] as? CGRect {
                print("Frame Change")
                surface.frame = CGRect(origin: CGPoint(x: 0, y: 0), size: rect.size)

//                config.renderSize = CGSize(width: 660, height: 841)

            }
        }
    }
}
