package io.agora.agora_rtc_engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import com.banuba.sdk.entity.RecordedVideoInfo
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.manager.BanubaSdkTouchListener
import com.banuba.sdk.manager.IEventCallback
import com.banuba.sdk.types.Data
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.base.RtcSurfaceView
import io.agora.rtc.video.AgoraVideoFrame
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
  private val MASK_NAME = "HeadphoneMusic"
  private val view = RtcSurfaceView(context)
  private val channel = MethodChannel(messenger, "agora_rtc_engine/surface_view_$viewId")

  init {
    BanubaSdkManager.initialize(context,
      AgoraRtcEnginePlugin.BANUBA_CLIENT_TOKEN
    )
    args?.let { map ->
      (map["data"] as? Map<*, *>)?.let { setData(it) }
      (map["renderMode"] as? Number)?.let { setRenderMode(it.toInt()) }
      (map["mirrorMode"] as? Number)?.let { setMirrorMode(it.toInt()) }
      (map["zOrderOnTop"] as? Boolean)?.let { setZOrderOnTop(it) }
      (map["zOrderMediaOverlay"] as? Boolean)?.let { setZOrderMediaOverlay(it) }
    }
    channel.setMethodCallHandler(this)
    configureSdkManager()

  }

  override fun getView(): View {
    banubaSdkManager.attachSurface(view)
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


  val banubaSdkManager by lazy(LazyThreadSafetyMode.NONE) {
    BanubaSdkManager(context)

  }

  private val banubaSdkEventCallback = object : IEventCallback {
    override fun onCameraOpenError(error: Throwable) {

    }

    override fun onCameraStatus(opened: Boolean) {

    }

    override fun onScreenshotReady(photo: Bitmap) {

    }

    override fun onHQPhotoReady(photo: Bitmap) {

    }

    override fun onVideoRecordingFinished(videoInfo: RecordedVideoInfo) {

    }

    override fun onVideoRecordingStatusChange(started: Boolean) {

    }

    override fun onImageProcessed(processedBitmap: Bitmap) {

    }

    override fun onFrameRendered(data: Data, width: Int, height: Int) {
      Log.e("Agora", "AgoraFrameData  ${data.data}")
      pushCustomFrame(data, width, height)
    }

  }
  private fun configureSdkManager() {
    val banubaTouchListener = BanubaSdkTouchListener(view.context, banubaSdkManager.effectPlayer)

    banubaSdkManager.effectManager.loadAsync(maskUri.toString())
    banubaSdkManager.setCallback(banubaSdkEventCallback)
    banubaSdkManager.attachSurface(view)
  }
  private val maskUri by lazy(LazyThreadSafetyMode.NONE) {
    Uri.parse(BanubaSdkManager.getResourcesBase())
      .buildUpon()
      .appendPath("effects")
      .appendPath(MASK_NAME)
      .build()


  }
  private fun pushCustomFrame(rawData: Data, width: Int, height: Int) {
    val pixelData = ByteArray(rawData.data.remaining())
    rawData.data.get(pixelData)
    rawData.close()
    val videoFrame = AgoraVideoFrame().apply {
      timeStamp = System.currentTimeMillis()
      format = AgoraVideoFrame.FORMAT_RGBA
      this.height = height
      stride = width
      buf = pixelData
    }
    getEngine()?.pushExternalVideoFrame(videoFrame)
  }
}
