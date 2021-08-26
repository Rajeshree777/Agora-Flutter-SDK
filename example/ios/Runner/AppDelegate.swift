import UIKit
import Flutter
import MessageUI
import AVFoundation
import Photos

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
    
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {

    let controller : FlutterViewController = window?.rootViewController as! FlutterViewController
    let channel = FlutterMethodChannel(name: "com.toppointellc.dayo/platform_channel",
                                                binaryMessenger: controller.binaryMessenger)

    channel.setMethodCallHandler({ [self]
        (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
            
        var uriSchema = ""
    
        let arguments = call.arguments as? NSDictionary
      
        if (call.method == "checkAvailability") {
            self.checkAvailability(appName: uriSchema, result: result);
        }
        else if(call.method=="slowMotionVideo"){
            self.slowMotionVideo(urlPath:(arguments!["urlPath"] as? String)!, speed: (arguments!["speed"] as? NSNumber)?.floatValue ?? 0,finalPath: (arguments!["writingPath"] as? String)!,result: result)
        }else if(call.method=="timeLapseVideo"){
            self.timeLapseVideo(urlPath:(arguments!["urlPath"] as? String)! , speed: (arguments!["speed"] as? NSNumber)?.floatValue ?? 0,finalPath:(arguments!["writingPath"] as? String)! ,result: result)
        }
        else if(call.method=="Merge"){
            self.doMerge(finalPath: (arguments!["writingPath"] as? String)!, animation: false, arrayUrl: (arguments!["urlPath"] as? [String])!, result: result)
        }
        else if (call.method == "launchUrl") {
            self.launchApp(appName: uriSchema, result: result)
        } else {
            result(FlutterMethodNotImplemented)
            return
        }

    })

    GeneratedPluginRegistrant.register(with: self)
    if #available(iOS 10.0, *) {
      UNUserNotificationCenter.current().delegate = self as? UNUserNotificationCenterDelegate
    }
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
    
    public func checkAvailability (appName: String, result: @escaping FlutterResult) {
        var canOpenUrl = false
        let appScheme = "\(appName)://"
        let appUrl = URL(string: appScheme)
        if appUrl != nil {
            let appScheme = "\(appName)://"
            let appUrl = URL(string: appScheme)

            canOpenUrl = UIApplication.shared.canOpenURL(appUrl! as URL)

            result(canOpenUrl)
        } else {
            result(canOpenUrl)
        }
    }

    public func launchApp (appName: String, result: @escaping FlutterResult) {
        let appScheme = "\(appName)://"
        let appUrl = URL(string: appScheme)

        if UIApplication.shared.canOpenURL(appUrl! as URL) {
            UIApplication.shared.open(appUrl!)
            result(true)
        } else {
            print("App not installed")
            result(false)
        }
    }
    public func doMerge(finalPath:String,animation:Bool,arrayUrl:[String],result: @escaping FlutterResult)
     -> Void {
        var arrayVideos = [AVAsset]()
        //let defaultSize = CGSize(width: 1920, height: 1080)
        for index in 0...arrayUrl.count-1{
            arrayVideos.append(AVAsset(url: URL(fileURLWithPath: arrayUrl[index])))
        }
        var insertTime = CMTime.zero
            var arrayLayerInstructions:[AVMutableVideoCompositionLayerInstruction] = []
            var outputSize = CGSize.init(width: 0, height: 0)
            
            // Determine video output size
            for videoAsset in arrayVideos {
                let videoTrack = videoAsset.tracks(withMediaType: AVMediaType.video).first! as AVAssetTrack

                let assetInfo = orientationFromTransform(transform: videoTrack.preferredTransform)

                var videoSize = videoTrack.naturalSize
                if assetInfo.isPortrait == true {
                    videoSize.width = videoTrack.naturalSize.height
                    videoSize.height = videoTrack.naturalSize.width
                }

                if videoSize.height > outputSize.height {
                    outputSize = videoSize
                }
            }
        
        

            // Silence sound (in case of video has no sound track)
            let silenceURL = Bundle.main.url(forResource: "captain", withExtension: "mp3")
            let silenceAsset = AVAsset(url:silenceURL!)
            let silenceSoundTrack = silenceAsset.tracks(withMediaType: AVMediaType.audio).first
            
            // Init composition
            let mixComposition = AVMutableComposition.init()

            for videoAsset in arrayVideos {
                // Get video track
                guard let videoTrack = videoAsset.tracks(withMediaType: AVMediaType.video).first else { continue }

                // Get audio track
                var audioTrack:AVAssetTrack?
                if videoAsset.tracks(withMediaType: AVMediaType.audio).count > 0 {
                    audioTrack = videoAsset.tracks(withMediaType: AVMediaType.audio).first
                }
                else {
                  audioTrack = silenceSoundTrack
                }

                // Init video & audio composition track
                let videoCompositionTrack = mixComposition.addMutableTrack(withMediaType: AVMediaType.video,
                                                                           preferredTrackID: Int32(kCMPersistentTrackID_Invalid))

                let audioCompositionTrack = mixComposition.addMutableTrack(withMediaType: AVMediaType.audio,
                                                                           preferredTrackID: Int32(kCMPersistentTrackID_Invalid))

                do {
                    let startTime = CMTime.zero
                    let duration = videoAsset.duration

                    // Add video track to video composition at specific time
                    try videoCompositionTrack?.insertTimeRange(CMTimeRangeMake(start: startTime, duration: duration),
                                                               of: videoTrack,
                                                               at: insertTime)

                    // Add audio track to audio composition at specific time
                    if let audioTrack = audioTrack {
                        try audioCompositionTrack?.insertTimeRange(CMTimeRangeMake(start: startTime, duration: duration),
                                                                   of: audioTrack,
                                                                   at: insertTime)
                    }

                    // Add instruction for video track
                    let layerInstruction = videoCompositionInstructionForTrack(track: videoCompositionTrack!,
                                                                               asset: videoAsset,
                                                                               standardSize: outputSize,
                                                                               atTime: insertTime)

                    // Hide video track before changing to new track
                    let endTime = CMTimeAdd(insertTime, duration)

                    if animation {
                        let timeScale = videoAsset.duration.timescale
                        let durationAnimation = CMTime.init(seconds: 1, preferredTimescale: timeScale)

                        layerInstruction.setOpacityRamp(fromStartOpacity: 1.0, toEndOpacity: 0.0, timeRange: CMTimeRange.init(start: endTime, duration: durationAnimation))
                    }
                    else {
                        layerInstruction.setOpacity(0, at: endTime)
                    }

                    arrayLayerInstructions.append(layerInstruction)

                    // Increase the insert time
                    insertTime = CMTimeAdd(insertTime, duration)
                }
                catch {
                    print("Load track error")
                }
            }
            
            // Main video composition instruction
            let mainInstruction = AVMutableVideoCompositionInstruction()
        mainInstruction.timeRange = CMTimeRangeMake(start: CMTime.zero, duration: insertTime)
            mainInstruction.layerInstructions = arrayLayerInstructions
            
            // Main video composition
            let mainComposition = AVMutableVideoComposition()
            mainComposition.instructions = [mainInstruction]
        mainComposition.frameDuration = CMTimeMake(value: 1, timescale: 60)
            mainComposition.renderSize = outputSize
            
            // Export to file
          
            let exportURL = URL.init(fileURLWithPath: finalPath)
            
            // Remove file if existed
//        let fileManager = FileManager()
//        try? fileManager.removeItem(at: exportURL)
            
            // Init exporter
            let exporter = AVAssetExportSession.init(asset: mixComposition, presetName: AVAssetExportPresetHighestQuality)
            exporter?.outputURL = exportURL
            exporter?.outputFileType = AVFileType.mp4
            exporter?.shouldOptimizeForNetworkUse = true
            exporter?.videoComposition = mainComposition
            
            // Do export
        print("Done Data::")
            exporter?.exportAsynchronously(completionHandler: {
              result(true)
            })
            
        }
    func videoCompositionInstructionForTrack(track: AVCompositionTrack, asset: AVAsset, standardSize:CGSize, atTime: CMTime) -> AVMutableVideoCompositionLayerInstruction {
            let instruction = AVMutableVideoCompositionLayerInstruction(assetTrack: track)
            let assetTrack = asset.tracks(withMediaType: AVMediaType.video)[0]
            
            let transform = assetTrack.preferredTransform
            let assetInfo = orientationFromTransform(transform: transform)
            
            var aspectFillRatio:CGFloat = 1
            if assetTrack.naturalSize.height < assetTrack.naturalSize.width {
                aspectFillRatio = standardSize.height / assetTrack.naturalSize.height
            }
            else {
                aspectFillRatio = standardSize.width / assetTrack.naturalSize.width
            }
            
            if assetInfo.isPortrait {
                let scaleFactor = CGAffineTransform(scaleX: aspectFillRatio, y: aspectFillRatio)
                
                let posX = standardSize.width/2 - (assetTrack.naturalSize.height * aspectFillRatio)/2
                let posY = standardSize.height/2 - (assetTrack.naturalSize.width * aspectFillRatio)/2
                let moveFactor = CGAffineTransform(translationX: posX, y: posY)
                
                instruction.setTransform(assetTrack.preferredTransform.concatenating(scaleFactor).concatenating(moveFactor), at: atTime)
                
            } else {
                let scaleFactor = CGAffineTransform(scaleX: aspectFillRatio, y: aspectFillRatio)
                
                let posX = standardSize.width/2 - (assetTrack.naturalSize.width * aspectFillRatio)/2
                let posY = standardSize.height/2 - (assetTrack.naturalSize.height * aspectFillRatio)/2
                let moveFactor = CGAffineTransform(translationX: posX, y: posY)
                
                var concat = assetTrack.preferredTransform.concatenating(scaleFactor).concatenating(moveFactor)
                
                if assetInfo.orientation == .down {
                    let fixUpsideDown = CGAffineTransform(rotationAngle: CGFloat(Double.pi))
                    concat = fixUpsideDown.concatenating(scaleFactor).concatenating(moveFactor)
                }
                
                instruction.setTransform(concat, at: atTime)
            }
            return instruction
        }
    func orientationFromTransform(transform: CGAffineTransform) -> (orientation: UIImage.Orientation, isPortrait: Bool) {
        var assetOrientation = UIImage.Orientation.up
            var isPortrait = false
            if transform.a == 0 && transform.b == 1.0 && transform.c == -1.0 && transform.d == 0 {
                assetOrientation = .right
                isPortrait = true
            } else if transform.a == 0 && transform.b == -1.0 && transform.c == 1.0 && transform.d == 0 {
                assetOrientation = .left
                isPortrait = true
            } else if transform.a == 1.0 && transform.b == 0 && transform.c == 0 && transform.d == 1.0 {
                assetOrientation = .up
            } else if transform.a == -1.0 && transform.b == 0 && transform.c == 0 && transform.d == -1.0 {
                assetOrientation = .down
            }
            return (assetOrientation, isPortrait)
        }
    func timeLapseVideo(urlPath:String,speed:Float,finalPath:String, result: @escaping FlutterResult){
        //Generating Video Assets
       // let videoAsset = AVURLAsset(url:url)
        let url = URL(fileURLWithPath: urlPath)
         let videoAsset = AVURLAsset(url: url)
        //Declaring Composition
        let comp = AVMutableComposition()

        //Getting tracks of video and audio
        let videoAssetSourceTrack = videoAsset.tracks(withMediaType: AVMediaType.video).first! as AVAssetTrack
        // Silence sound (in case of video has no sound track)
        let silenceURL = Bundle.main.url(forResource: "captain", withExtension: "mp3")
        let silenceAsset = AVAsset(url:silenceURL!)
        let silenceSoundTrack = silenceAsset.tracks(withMediaType: AVMediaType.audio).first
        
        var audioAssetSourceTrack:AVAssetTrack?
        if videoAsset.tracks(withMediaType: AVMediaType.audio).count > 0 {
            audioAssetSourceTrack = videoAsset.tracks(withMediaType: AVMediaType.audio).first
        }
        else {
            audioAssetSourceTrack = silenceSoundTrack
        }
    //    let audioAssetSourceTrack = videoAsset.tracks(withMediaType: AVMediaType.audio).first! as AVAssetTrack

        //Making Composition tracks
        let videoCompositionTrack = comp.addMutableTrack(withMediaType: AVMediaType.video, preferredTrackID: kCMPersistentTrackID_Invalid)
        let audioCompositionTrack = comp.addMutableTrack(withMediaType: AVMediaType.audio, preferredTrackID: kCMPersistentTrackID_Invalid)

        do {
            //inserting time range for video from video duration
            try videoCompositionTrack!.insertTimeRange(
                CMTimeRangeMake(start: CMTime.zero, duration: videoAsset.duration),
                of: videoAssetSourceTrack,
                at: CMTime.zero)
            //inserting time range for audio from video duration this is used to sync both duration
            try audioCompositionTrack!.insertTimeRange(
                CMTimeRangeMake(start: CMTime.zero, duration: videoAsset.duration),
                of: audioAssetSourceTrack!,
                at: CMTime.zero)
            //Initializing scaleFactor/Speed preset
            let videoScaleFactor = Int64(speed)
            //Duration
            let videoDuration: CMTime = videoAsset.duration
            
            //Composition to give final ouput of video according to the speed
            videoCompositionTrack!.scaleTimeRange(CMTimeRangeMake(start: CMTime.zero, duration: videoDuration), toDuration: CMTimeMake(value: videoDuration.value * 1/(videoScaleFactor), timescale: videoDuration.timescale))
            //Composition to give final ouput of audio according to the speed
            audioCompositionTrack!.scaleTimeRange(CMTimeRangeMake(start: CMTime.zero, duration: videoDuration), toDuration: CMTimeMake(value: videoDuration.value * 1/(videoScaleFactor), timescale: videoDuration.timescale))
            videoCompositionTrack!.preferredTransform = videoAssetSourceTrack.preferredTransform


            //making output path
            let outputFileURL = URL(fileURLWithPath:finalPath)

            let fileManager = FileManager()
        

            //Exporter configs
            let exporter = AVAssetExportSession(asset:comp, presetName: AVAssetExportPresetHighestQuality)
           // exporter?.presetName = .availableStringEncodings
            exporter?.outputURL = outputFileURL
              exporter?.outputFileType = AVFileType.mp4
            exporter?.shouldOptimizeForNetworkUse = true

            exporter?.exportAsynchronously {
                result(true)
                print("Exporting Data::")
           
            }

            
        }catch { print(error) }

    }
    func slowMotionVideo(urlPath:String,speed:Float,finalPath:String, result: @escaping FlutterResult){
        //Generating Video Assets
       // let videoAsset = AVURLAsset(url:url)
       let url = URL(fileURLWithPath: urlPath)
        let videoAsset = AVURLAsset(url: url)
        //Declaring Composition
        let comp = AVMutableComposition()

        //Getting tracks of video and audio
        let videoAssetSourceTrack = videoAsset.tracks(withMediaType: AVMediaType.video).first! as AVAssetTrack
        // Silence sound (in case of video has no sound track)
        let silenceURL = Bundle.main.url(forResource: "captain", withExtension: "mp3")
        let silenceAsset = AVAsset(url:silenceURL!)
        let silenceSoundTrack = silenceAsset.tracks(withMediaType: AVMediaType.audio).first
        
        var audioAssetSourceTrack:AVAssetTrack?
        if videoAsset.tracks(withMediaType: AVMediaType.audio).count > 0 {
            audioAssetSourceTrack = videoAsset.tracks(withMediaType: AVMediaType.audio).first
        }
        else {
            audioAssetSourceTrack = silenceSoundTrack
        }
    //    let audioAssetSourceTrack = videoAsset.tracks(withMediaType: AVMediaType.audio).first! as AVAssetTrack

        //Making Composition tracks
        let videoCompositionTrack = comp.addMutableTrack(withMediaType: AVMediaType.video, preferredTrackID: kCMPersistentTrackID_Invalid)
        let audioCompositionTrack = comp.addMutableTrack(withMediaType: AVMediaType.audio, preferredTrackID: kCMPersistentTrackID_Invalid)

        do {
            //inserting time range for video from video duration
            try videoCompositionTrack!.insertTimeRange(
                CMTimeRangeMake(start: CMTime.zero, duration: videoAsset.duration),
                of: videoAssetSourceTrack,
                at: CMTime.zero)
            //inserting time range for audio from video duration this is used to sync both duration
            try audioCompositionTrack!.insertTimeRange(
                CMTimeRangeMake(start: CMTime.zero, duration: videoAsset.duration),
                of: audioAssetSourceTrack!,
                at: CMTime.zero)
            //Initializing scaleFactor/Speed preset
            let videoScaleFactor = Int64(speed)
            //Duration
            let videoDuration: CMTime = videoAsset.duration
            
            //Composition to give final ouput of video according to the speed
            videoCompositionTrack!.scaleTimeRange(CMTimeRangeMake(start: CMTime.zero, duration: videoDuration), toDuration: CMTimeMake(value: videoDuration.value * videoScaleFactor, timescale: videoDuration.timescale))
            //Composition to give final ouput of audio according to the speed
            audioCompositionTrack!.scaleTimeRange(CMTimeRangeMake(start: CMTime.zero, duration: videoDuration), toDuration: CMTimeMake(value: videoDuration.value * videoScaleFactor, timescale: videoDuration.timescale))
            videoCompositionTrack!.preferredTransform = videoAssetSourceTrack.preferredTransform


            //making output path
            let outputFileURL = URL(fileURLWithPath: finalPath)

            let fileManager = FileManager()
            try? fileManager.removeItem(at: outputFileURL)

            //Exporter configs
            let exporter = AVAssetExportSession(asset:comp, presetName: AVAssetExportPresetHighestQuality)

            exporter?.outputURL = outputFileURL
              exporter?.outputFileType = AVFileType.mp4
            exporter?.shouldOptimizeForNetworkUse = true

            exporter?.exportAsynchronously {
                result(true)
                print("Exporting Data::")
               
            }

            
        }catch { print(error) }

    }
}
