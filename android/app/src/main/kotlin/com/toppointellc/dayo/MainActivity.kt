package com.toppointellc.dayo

import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

import android.content.Context

import java.util.HashMap
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.Intent

import java.io.File

import android.net.Uri

import android.os.Build
import android.widget.Toast

class MainActivity: FlutterActivity() {

    private val CHANNEL = "com.soda.demo/permission"

    companion object {
        var flutterEngineInstance: FlutterEngine? = null
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        flutterEngineInstance = flutterEngine

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            // Note: this method is invoked on the main thread.
            call, result ->
            if (call.method == "checkAvailability") {
                val packageNameURI : String? = call.argument("packageNameURI")
//                val videoUrl : String? = call.argument("video")
                checkAvailability(packageNameURI, result)
            } else if (call.method == "launchUrl") {
                val packageNameURI : String? = call.argument("packageNameURI")
                val videoUrl : String? = call.argument("video")
                launchApp(packageNameURI, videoUrl)
            } else {
                result.notImplemented()
            }
        }
    }

    private fun checkAvailability(uri: String?, result: Result) {
        if (uri != null && uri.length > 0) {
            val info: PackageInfo? = getAppPackageInfo(uri)
            if (info != null) {
                result.success(true)
            }else
            {
                result.success(false)
            }

        }
        else{
            result.error("", "App not found $uri", null)
        }

    }


    private fun getAppPackageInfo(uri: String): PackageInfo? {
        val ctx: Context = activity.getApplicationContext()
        val pm: PackageManager = ctx.getPackageManager()
        try {
            val pi: PackageInfo = pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES)
            android.util.Log.d("Package", "" + (pi != null))
            return pi
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
        }
        return null
    }

    private fun convertPackageInfoToJson(info: PackageInfo): String {
//        val map: MutableMap<String, Any> = HashMap<String, Any>()
//        map.put("app_name", info.applicationInfo.loadLabel(registrar.context().getPackageManager()).toString())
//        map.put("package_name", info.packageName)
//        map.put("version_code", info.versionCode.toString())
//        map.put("version_name", info.versionName)
        return info.packageName
    }

    private fun launchApp(uri: String?, videoUrl: String?) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_TEXT, videoUrl)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        shareIntent.setPackage(uri) //Instagram App package
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}
