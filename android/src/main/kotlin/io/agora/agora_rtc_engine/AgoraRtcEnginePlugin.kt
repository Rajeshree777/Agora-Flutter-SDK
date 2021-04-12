package io.agora.agora_rtc_engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.banuba.sdk.entity.RecordedVideoInfo
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.manager.BanubaSdkTouchListener
import com.banuba.sdk.manager.IEventCallback
import com.banuba.sdk.types.Data
import io.agora.rtc.RtcEngine
import io.agora.rtc.base.RtcEngineManager
import io.agora.rtc.video.AgoraVideoFrame
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.platform.PlatformViewRegistry
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaMethod

/** AgoraRtcEnginePlugin */
class AgoraRtcEnginePlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
  private var registrar: Registrar? = null
  private var binding: FlutterPlugin.FlutterPluginBinding? = null
  private lateinit var applicationContext: Context
  private val MASK_NAME = "HeadphoneMusic"

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var methodChannel: MethodChannel
  private lateinit var eventChannel: EventChannel

  private var eventSink: EventChannel.EventSink? = null
  private val manager = RtcEngineManager { methodName, data -> emit(methodName, data) }
  private val handler = Handler(Looper.getMainLooper())
  private val rtcChannelPlugin = AgoraRtcChannelPlugin(this)

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //fsaf
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    const val BANUBA_CLIENT_TOKEN = "rXyaDKtsn260PUlL+DwHJDRKfEC8pkAjGbiOTzWJyEaMqlrVtV87NGPBUPLRGelwp9IcJVPjeeYVAiDzShXDW7UsioCXOLTGR4VoKK+Ljg4e1qwndj3t3maiwEeT6eOwCU3tZMQA+sgK0l/eZ2RPBg7giVAXGrMYjsT/8siO91AtKiNiUHnjABrJKAaFR+Y+Rfd4aaJvs8fWP5msgWbBOFYyFhupQ++pVkGD02qZq0j+DO88yc2gAIBz1+4+nkLN+EsOnt5tw/DeocpdyOPPkohFzjZzwA0D0f79qsVRngQccF37+bNkYqVowwvrm+YFBxHXSaL67nAU5KvOJFLyZb4evIfGzVVCeGlWjFXP8jAiMXoT1G2xmlZYgrUHIGhiWI0/owGiq2Prmv+6MU9XB7wj5IK3DMGuCwZ4rqlWfe41nFJv6UbfnpPDcF5zINiMfwuC+7lbHdHtH5g2jnpIxdywkEln4dy78mgPdbRPrQvwiWIsGiXwdjkikZxf6ARWFHgHV7Bn63S8B63oxS2NSduCIllHdhowcdJZCzWjCkP3AcTmuAz0rD3BynPCrwgEOL58g3GT+w7ZbNX8JDVE9mZVKORCRp7iMmNsR6Uxy/egcm5ylaV1QfBS6xaxK0gZLimBsvc5fFbEzb448VECxqOXpq9S5kt2S50gvKJA6OENB3wUXnoTbytkS0fIF1+UH6XvG6u1wfNPDJ4CKetTEP0pfYC0HdYStBxgbe5xIkI2/fjxQNTyRX8AynR70FFZVZhqbLklpkffQWvTmMCfVTAij9xJth+znvL4W5IG165L45mAmC2JstuCCGLR3+EXL8+2IOIANOrCfC+sMoMJqwuI/y5A9REqGWBYCqZBkkVYdz88Jcw1ri243ewq9nJzD1f+IxpXqMFLLKrpgEs3CWJs0ax4rUz1OQLpGQ1dnkgdVts3CJ57EigLrKCoP3K7mU9pptW8+Kk+tdPgjR4Eb2RtulIGHcOz6+uW7UQfrtd/6ceGgPd1YUTklda815UNQhAnHDuWXRMybQLFaPgtAP4r+cY49IEyuAoL6cRz8owxxb6RpAU1XN9TYWkZfez7eYFD2PrGt16pvPLMDBZFK4YmgmpDjSzPiHh4c11zz+poQHOA+DcLsBoh3RRAHCClKsdCVEpzb5ygwv242AXteBA3A9DImRpRlibqiqsN3hFGhUJF4UcJqPyRk3VJLfeO4dTLc5Ac2jQojpOrp4Gl8+1decC+8FuT2+e1X6U4tOb0WHNuNxBzTFVSSee+kjEIAJZAif5mmfYvTrg2XDbEBOcV4a/pKrXrChUTU/3HFldUtuRuMIKcmHDSfRftuAT+jxTIDvSMq5uwTO09gUHyXfdWPuIjTbqiaL3KCdrHL3OulEDgEqbddobHZYz1tpQnibdgzFYN2IXCvHTjz8YjeNgoEK6jZvYqeE0xFBT3DdLrqznQkJWrQ6R5XYbA1kVgC4JMKv78Oan+R2a58PXuF7XMahOj5k65tq4THj5FguDCcFB0xSXZGlf++c4l3mxlAv2mbouM4737ecIbj4/ksAJX35rx+swOs72kL3LdgFF4lktUx2WXySp9Ap2WRb2v4oUTsacF1TA8MQBCZzuYjDFAr+kcpNpHBUeErPxuCJFHs+HHyrKJocco2vmY2QPqt8Ha80AzB0g17pMcV41n6h65WU7PPVXN8VOv9iaO9hMl3scyd8ImX/t0OUjzlfElL/mFbsKAmXtTJLfEKO4MnaH57EBgV16GodTupVG7cAY04Rv4/wS8NwRKveePz9TTnMbmgPEJNqYIEoEGjOlcagsElVfTP6P0LS9TUS69O/m0MClRRF/peDHjBeNo3PQnQQ8yK/uRSO8iyGPS9e511kE4e6lemdCFWEwqypAkzLBb6lDbsg=="

    @JvmStatic
    fun registerWith(registrar: Registrar) {
      AgoraRtcEnginePlugin().apply {
        this.registrar = registrar
        rtcChannelPlugin.initPlugin(registrar.messenger())
        initPlugin(registrar.context(), registrar.messenger(), registrar.platformViewRegistry())
      }
    }
  }

  private fun initPlugin(context: Context, binaryMessenger: BinaryMessenger, platformViewRegistry: PlatformViewRegistry) {
    applicationContext = context.applicationContext
//    BanubaSdkManager.initialize(applicationContext,
//      BANUBA_CLIENT_TOKEN
//    )
   // configureSdkManager()
    methodChannel = MethodChannel(binaryMessenger, "agora_rtc_engine")
    methodChannel.setMethodCallHandler(this)
    eventChannel = EventChannel(binaryMessenger, "agora_rtc_engine/events")
    eventChannel.setStreamHandler(this)
    platformViewRegistry.registerViewFactory("AgoraSurfaceView", AgoraSurfaceViewFactory(binaryMessenger, this, rtcChannelPlugin))
    platformViewRegistry.registerViewFactory("AgoraTextureView", AgoraTextureViewFactory(binaryMessenger, this, rtcChannelPlugin))
//    banubaSdkManager.attachSurface(platformViewRegistry)
  }

  override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    this.binding = binding
    rtcChannelPlugin.onAttachedToEngine(binding)
    initPlugin(binding.applicationContext, binding.binaryMessenger, binding.platformViewRegistry)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    rtcChannelPlugin.onDetachedFromEngine(binding)
    methodChannel.setMethodCallHandler(null)
    eventChannel.setStreamHandler(null)
    manager.release()
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
    eventSink = events
  }

  override fun onCancel(arguments: Any?) {
    eventSink = null
  }

  private fun emit(methodName: String, data: Map<String, Any?>?) {
    handler.post {
      val event: MutableMap<String, Any?> = mutableMapOf("methodName" to methodName)
      data?.let { event.putAll(it) }
      eventSink?.success(event)
    }
  }

  fun engine(): RtcEngine? {
    return manager.engine
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "getAssetAbsolutePath") {
      getAssetAbsolutePath(call, result)
      return
    }

    manager::class.declaredMemberFunctions.find { it.name == call.method }?.let { function ->
      function.javaMethod?.let { method ->
        try {
          val parameters = mutableListOf<Any?>()
          call.arguments<Map<*, *>>()?.toMutableMap()?.let {
            if (call.method == "create") {
              it["context"] = applicationContext
            }
            parameters.add(it)
          }
          method.invoke(manager, *parameters.toTypedArray(), ResultCallback(result))
          return@onMethodCall
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
    result.notImplemented()
  }

  fun getAssetAbsolutePath(call: MethodCall, result: Result) {
    call.arguments<String>()?.let {
      val assetKey = registrar?.lookupKeyForAsset(it)
        ?: binding?.flutterAssets?.getAssetFilePathByName(it)
      try {
        applicationContext.assets.openFd(assetKey!!).close()
        result.success("/assets/$assetKey")
      } catch (e: Exception) {
        result.error(e.javaClass.simpleName, e.message, e.cause)
      }
      return@getAssetAbsolutePath
    }
    result.error(IllegalArgumentException::class.simpleName, null, null)
  }

//  val banubaSdkManager by lazy(LazyThreadSafetyMode.NONE) {
//    BanubaSdkManager(applicationContext)
//  }
//
//  private fun configureSdkManager() {
//    banubaSdkManager.effectManager.loadAsync(maskUri.toString())
//  }
//
//  private val maskUri by lazy(LazyThreadSafetyMode.NONE) {
//    Uri.parse(BanubaSdkManager.getResourcesBase())
//      .buildUpon()
//      .appendPath("effects")
//      .appendPath(MASK_NAME)
//      .build()
//
//
//  }


}
