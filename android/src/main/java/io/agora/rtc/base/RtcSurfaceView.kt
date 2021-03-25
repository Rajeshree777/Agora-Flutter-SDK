package io.agora.rtc.base

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import com.banuba.sdk.entity.RecordedVideoInfo
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.manager.BanubaSdkTouchListener
import com.banuba.sdk.manager.IEventCallback
import com.banuba.sdk.types.Data
import io.agora.agora_rtc_engine.AgoraRtcEnginePlugin
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.AgoraVideoFrame
import io.agora.rtc.video.VideoCanvas
import java.lang.ref.WeakReference

class RtcSurfaceView(
  context: Context,
  val rtcEnginePlugin: AgoraRtcEnginePlugin
) : FrameLayout(context) {
  private var surface: SurfaceView
  private var canvas: VideoCanvas
  private var isMediaOverlay = false
  private var onTop = false
  private val MASK_NAME = "HeadphoneMusic"
  private var channel: WeakReference<RtcChannel>? = null

  init {
    try {
      BanubaSdkManager.initialize(context,
        AgoraRtcEnginePlugin.BANUBA_CLIENT_TOKEN
      )
      surface = RtcEngine.CreateRendererView(context)
      print("init Block called")
    } catch (e: UnsatisfiedLinkError) {
      throw RuntimeException("Please init RtcEngine first!")
    }


    canvas = VideoCanvas(surface)
    configureSdkManager()


    addView(surface)
  }

  private val banubaSdkManager by lazy(LazyThreadSafetyMode.NONE) {
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
    val banubaTouchListener = BanubaSdkTouchListener(context, banubaSdkManager.effectPlayer)

    banubaSdkManager.effectManager.loadAsync(maskUri.toString())
    banubaSdkManager.setCallback(banubaSdkEventCallback)
    banubaSdkManager.attachSurface(surface)
    // banubaSdkManager.attachSurface(view)
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
    //surface.pushExternalVideoFrame(videoFrame)
    rtcEnginePlugin.engine()?.pushExternalVideoFrame(videoFrame)


  }

  private fun getEngine(): RtcEngine? {
    return rtcEnginePlugin.engine()
  }


  fun setZOrderMediaOverlay(isMediaOverlay: Boolean) {
    this.isMediaOverlay = isMediaOverlay
    try {
      removeView(surface)
      surface.setZOrderMediaOverlay(isMediaOverlay)
      addView(surface)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setZOrderOnTop(onTop: Boolean) {
    this.onTop = onTop
    try {
      removeView(surface)
      surface.setZOrderOnTop(onTop)
      addView(surface)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun setData(engine: RtcEngine, channel: RtcChannel?, uid: Int) {
    this.channel = if (channel != null) WeakReference(channel) else null
    canvas.channelId = this.channel?.get()?.channelId()
    canvas.uid = uid
    setupVideoCanvas(engine)
  }

  fun resetVideoCanvas(engine: RtcEngine) {
    val canvas = VideoCanvas(null, canvas.renderMode, canvas.channelId, canvas.uid, canvas.mirrorMode)
    if (canvas.uid == 0) {
      engine.setupLocalVideo(canvas)
    } else {
      engine.setupRemoteVideo(canvas)
    }
  }

  private fun setupVideoCanvas(engine: RtcEngine) {
    removeAllViews()
    surface = RtcEngine.CreateRendererView(context.applicationContext)
    surface.setZOrderMediaOverlay(isMediaOverlay)
    surface.setZOrderOnTop(onTop)
    addView(surface)
    surface.layout(0, 0, width, height)
    canvas.view = surface
    if (canvas.uid == 0) {
      engine.setupLocalVideo(canvas)
    } else {
      engine.setupRemoteVideo(canvas)
    }
  }

  fun setRenderMode(engine: RtcEngine, @Annotations.AgoraVideoRenderMode renderMode: Int) {
    canvas.renderMode = renderMode
    setupRenderMode(engine)
  }

  fun setMirrorMode(engine: RtcEngine, @Annotations.AgoraVideoMirrorMode mirrorMode: Int) {
    canvas.mirrorMode = mirrorMode
    setupRenderMode(engine)
  }

  private fun setupRenderMode(engine: RtcEngine) {
    if (canvas.uid == 0) {
      engine.setLocalRenderMode(canvas.renderMode, canvas.mirrorMode)
    } else {
      channel?.get()?.let {
        it.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
        return@setupRenderMode
      }
      engine.setRemoteRenderMode(canvas.uid, canvas.renderMode, canvas.mirrorMode)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width: Int = MeasureSpec.getSize(widthMeasureSpec)
    val height: Int = MeasureSpec.getSize(heightMeasureSpec)
    surface.layout(0, 0, width, height)
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }


}
