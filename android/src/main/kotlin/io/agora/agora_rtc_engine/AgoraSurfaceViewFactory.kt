package io.agora.agora_rtc_engine

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.View
import com.banuba.sdk.manager.BanubaSdkManager
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.base.RtcSurfaceView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaMethod

class AgoraSurfaceViewFactory(
  private val messenger: BinaryMessenger,
  private val rtcEnginePlugin: AgoraRtcEnginePlugin,
  private val rtcChannelPlugin: AgoraRtcChannelPlugin
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
  override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
    return AgoraSurfaceView(context.applicationContext, messenger, viewId, args as? Map<*, *>, rtcEnginePlugin, rtcChannelPlugin)
  }
}

class AgoraSurfaceView(
  context: Context,
  messenger: BinaryMessenger,
  viewId: Int,
  args: Map<*, *>?,
  private val rtcEnginePlugin: AgoraRtcEnginePlugin,
  private val rtcChannelPlugin: AgoraRtcChannelPlugin
) : PlatformView, MethodChannel.MethodCallHandler {
  private val view = RtcSurfaceView(context)
  private val channel = MethodChannel(messenger, "agora_rtc_engine/surface_view_$viewId")

  private val banubaSdkManager by lazy(LazyThreadSafetyMode.NONE) {
    BanubaSdkManager(context)
  }

  private val maskUri by lazy(LazyThreadSafetyMode.NONE) {
    Uri.parse(BanubaSdkManager.getResourcesBase())
      .buildUpon()
      .appendPath("effects")
      .appendPath("UnluckyWitch")
      .build()
  }

  private fun configureSdkManager() {
    /*val banubaTouchListener = BanubaSdkTouchListener(this, banubaSdkManager.effectPlayer)
    localSurfaceView.setOnTouchListener(banubaTouchListener)*/
    banubaSdkManager.effectManager.loadAsync(maskUri.toString())
  }
  init {
    BanubaSdkManager.initialize(context,
      "rXyaDKtsn260PUlL+DwHJDRKfEC8pkAjGbiOTzWJyEaMqlrVtV87NGPBUPLRGelwp9IcJVPjeeYVAiDzShXDW7UsioCXOLTGR4VoKK+Ljg4e1qwndj3t3maiwEeT6eOwCU3tZMQA+sgK0l/eZ2RPBg7giVAXGrMYjsT/8siO91AtKiNiUHnjABrJKAaFR+Y+Rfd4aaJvs8fWP5msgWbBOFYyFhupQ++pVkGD02qZq0j+DO88yc2gAIBz1+4+nkLN+EsOnt5tw/DeocpdyOPPkohFzjZzwA0D0f79qsVRngQccF37+bNkYqVowwvrm+YFBxHXSaL67nAU5KvOJFLyZb4evIfGzVVCeGlWjFXP8jAiMXoT1G2xmlZYgrUHIGhiWI0/owGiq2Prmv+6MU9XB7wj5IK3DMGuCwZ4rqlWfe41nFJv6UbfnpPDcF5zINiMfwuC+7lbHdHtH5g2jnpIxdywkEln4dy78mgPdbRPrQvwiWIsGiXwdjkikZxf6ARWFHgHV7Bn63S8B63oxS2NSduCIllHdhowcdJZCzWjCkP3AcTmuAz0rD3BynPCrwgEOL58g3GT+w7ZbNX8JDVE9mZVKORCRp7iMmNsR6Uxy/egcm5ylaV1QfBS6xaxK0gZLimBsvc5fFbEzb448VECxqOXpq9S5kt2S50gvKJA6OENB3wUXnoTbytkS0fIF1+UH6XvG6u1wfNPDJ4CKetTEP0pfYC0HdYStBxgbe5xIkI2/fjxQNTyRX8AynR70FFZVZhqbLklpkffQWvTmMCfVTAij9xJth+znvL4W5IG165L45mAmC2JstuCCGLR3+EXL8+2IOIANOrCfC+sMoMJqwuI/y5A9REqGWBYCqZBkkVYdz88Jcw1ri243ewq9nJzD1f+IxpXqMFLLKrpgEs3CWJs0ax4rUz1OQLpGQ1dnkgdVts3CJ57EigLrKCoP3K7mU9pptW8+Kk+tdPgjR4Eb2RtulIGHcOz6+uW7UQfrtd/6ceGgPd1YUTklda815UNQhAnHDuWXRMybQLFaPgtAP4r+cY49IEyuAoL6cRz8owxxb6RpAU1XN9TYWkZfez7eYFD2PrGt16pvPLMDBZFK4YmgmpDjSzPiHh4c11zz+poQHOA+DcLsBoh3RRAHCClKsdCVEpzb5ygwv242AXteBA3A9DImRpRlibqiqsN3hFGhUJF4UcJqPyRk3VJLfeO4dTLc5Ac2jQojpOrp4Gl8+1decC+8FuT2+e1X6U4tOb0WHNuNxBzTFVSSee+kjEIAJZAif5mmfYvTrg2XDbEBOcV4a/pKrXrChUTU/3HFldUtuRuMIKcmHDSfRftuAT+jxTIDvSMq5uwTO09gUHyXfdWPuIjTbqiaL3KCdrHL3OulEDgEqbddobHZYz1tpQnibdgzFYN2IXCvHTjz8YjeNgoEK6jZvYqeE0xFBT3DdLrqznQkJWrQ6R5XYbA1kVgC4JMKv78Oan+R2a58PXuF7XMahOj5k65tq4THj5FguDCcFB0xSXZGlf++c4l3mxlAv2mbouM4737ecIbj4/ksAJX35rx+swOs72kL3LdgFF4lktUx2WXySp9Ap2WRb2v4oUTsacF1TA8MQBCZzuYjDFAr+kcpNpHBUeErPxuCJFHs+HHyrKJocco2vmY2QPqt8Ha80AzB0g17pMcV41n6h65WU7PPVXN8VOv9iaO9hMl3scyd8ImX/t0OUjzlfElL/mFbsKAmXtTJLfEKO4MnaH57EBgV16GodTupVG7cAY04Rv4/wS8NwRKveePz9TTnMbmgPEJNqYIEoEGjOlcagsElVfTP6P0LS9TUS69O/m0MClRRF/peDHjBeNo3PQnQQ8yK/uRSO8iyGPS9e511kE4e6lemdCFWEwqypAkzLBb6lDbsg=="
    )
    args?.let { map ->
      (map["data"] as? Map<*, *>)?.let { setData(it) }
      (map["renderMode"] as? Number)?.let { setRenderMode(it.toInt()) }
      (map["mirrorMode"] as? Number)?.let { setMirrorMode(it.toInt()) }
      (map["zOrderOnTop"] as? Boolean)?.let { setZOrderOnTop(it) }
      (map["zOrderMediaOverlay"] as? Boolean)?.let { setZOrderMediaOverlay(it) }
    }
    channel.setMethodCallHandler(this)
  }

  override fun getView(): View {
    return view
  }

  override fun dispose() {
    channel.setMethodCallHandler(null)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    this::class.declaredMemberFunctions.find { it.name == call.method }?.let { function ->
      function.javaMethod?.let { method ->
        val parameters = mutableListOf<Any?>()
        function.parameters.forEach { parameter ->
          val map = call.arguments<Map<*, *>>()
          if (map.containsKey(parameter.name)) {
            parameters.add(map[parameter.name])
          }
        }
        try {
          method.invoke(this, *parameters.toTypedArray())
          return@onMethodCall
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
    result.notImplemented()
  }

  private fun setData(data: Map<*, *>) {
    val channel = (data["channelId"] as? String)?.let { getChannel(it) }
    getEngine()?.let {

      view.setData(it, channel, (data["uid"] as Number).toInt())
      banubaSdkManager.attachSurface(view as SurfaceView)
      configureSdkManager()
    }
  }

  private fun setRenderMode(renderMode: Int) {
    getEngine()?.let { view.setRenderMode(it, renderMode) }
  }

  private fun setMirrorMode(mirrorMode: Int) {
    getEngine()?.let { view.setMirrorMode(it, mirrorMode) }
  }

  private fun setZOrderOnTop(onTop: Boolean) {
    view.setZOrderOnTop(onTop)
  }

  private fun setZOrderMediaOverlay(isMediaOverlay: Boolean) {
    view.setZOrderMediaOverlay(isMediaOverlay)
  }

  private fun getEngine(): RtcEngine? {
    return rtcEnginePlugin.engine()
  }

  private fun getChannel(channelId: String): RtcChannel? {
    return rtcChannelPlugin.channel(channelId)
  }
}
