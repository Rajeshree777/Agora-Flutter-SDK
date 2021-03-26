package io.agora.rtc.base

import android.content.Context
import android.net.Uri
import android.view.Surface
import android.view.SurfaceView
import android.widget.FrameLayout
import com.banuba.sdk.manager.BanubaSdkManager
import com.banuba.sdk.manager.BanubaSdkTouchListener
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.ref.WeakReference

class BanubaSurfaceView(context: Context,val surface:SurfaceView) : FrameLayout(context) {


  private var canvas: VideoCanvas

  private val MASK_NAME = "HeadphoneMusic"


  init {
    canvas = VideoCanvas(surface)
    configureSdkManager()
    addView(surface)

  }

  val banubaSdkManager by lazy(LazyThreadSafetyMode.NONE) {
    BanubaSdkManager(context)
  }


  val maskUri by lazy(LazyThreadSafetyMode.NONE) {
    Uri.parse(BanubaSdkManager.getResourcesBase())
      .buildUpon()
      .appendPath("effects")
      .appendPath(MASK_NAME)
      .build()
  }

  private fun configureSdkManager() {
    banubaSdkManager.attachSurface(surface)
    banubaSdkManager.openCamera()
    banubaSdkManager.effectManager.loadAsync(maskUri.toString())
    surface.setZOrderMediaOverlay(true)
    banubaSdkManager.effectPlayer.setEffectVolume(0F)
    banubaSdkManager.effectPlayer.playbackPlay()
    banubaSdkManager.startForwardingFrames()
  }


}
