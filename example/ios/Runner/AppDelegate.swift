import UIKit
import Flutter
import MessageUI

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
    
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {

    let controller : FlutterViewController = window?.rootViewController as! FlutterViewController
    let channel = FlutterMethodChannel(name: "com.soda.demo/permission",
                                                binaryMessenger: controller.binaryMessenger)

    channel.setMethodCallHandler({ [self]
        (call: FlutterMethodCall, result: @escaping FlutterResult) -> Void in
            
        var uriSchema = ""
    
        let arguments = call.arguments as? NSDictionary
        uriSchema = (arguments!["packageNameURI"] as? String)!
        
        if (call.method == "checkAvailability") {
            self.checkAvailability(appName: uriSchema, result: result);
        } else if (call.method == "launchUrl") {
            self.launchApp(appName: uriSchema, result: result);
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
}
