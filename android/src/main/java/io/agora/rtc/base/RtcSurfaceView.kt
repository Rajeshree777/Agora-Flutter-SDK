package io.agora.rtc.base

import android.content.Context
import android.view.SurfaceView
import android.widget.FrameLayout
import io.agora.rtc.RtcChannel
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import java.lang.ref.WeakReference
import com.banuba.sdk.manager.BanubaSdkManager
import android.util.Log

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.banuba.sdk.entity.RecordedVideoInfo
import com.banuba.sdk.manager.BanubaSdkTouchListener
import com.banuba.sdk.manager.IEventCallback
import com.banuba.sdk.types.Data
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.video.AgoraVideoFrame

class RtcSurfaceView(
  context: Context
) : FrameLayout(context) {
  private var surface: SurfaceView
  private var canvas: VideoCanvas
  private var isMediaOverlay = false
  private var onTop = false
  private var channel: WeakReference<RtcChannel>? = null

  companion object {
    const val BANUBA_CLIENT_TOKEN = "rXyaDKtsn260PUlL+DwHJDRKfEC8pkAjGbiOTzWJyEaMqlrVtV87NGPBUPLRGelwp9IcJVPjeeYVAiDzShXDW7UsioCXOLTGR4VoKK+Ljg4e1qwndj3t3maiwEeT6eOwCU3tZMQA+sgK0l/eZ2RPBg7giVAXGrMYjsT/8siO91AtKiNiUHnjABrJKAaFR+Y+Rfd4aaJvs8fWP5msgWbBOFYyFhupQ++pVkGD02qZq0j+DO88yc2gAIBz1+4+nkLN+EsOnt5tw/DeocpdyOPPkohFzjZzwA0D0f79qsVRngQccF37+bNkYqVowwvrm+YFBxHXSaL67nAU5KvOJFLyZb4evIfGzVVCeGlWjFXP8jAiMXoT1G2xmlZYgrUHIGhiWI0/owGiq2Prmv+6MU9XB7wj5IK3DMGuCwZ4rqlWfe41nFJv6UbfnpPDcF5zINiMfwuC+7lbHdHtH5g2jnpIxdywkEln4dy78mgPdbRPrQvwiWIsGiXwdjkikZxf6ARWFHgHV7Bn63S8B63oxS2NSduCIllHdhowcdJZCzWjCkP3AcTmuAz0rD3BynPCrwgEOL58g3GT+w7ZbNX8JDVE9mZVKORCRp7iMmNsR6Uxy/egcm5ylaV1QfBS6xaxK0gZLimBsvc5fFbEzb448VECxqOXpq9S5kt2S50gvKJA6OENB3wUXnoTbytkS0fIF1+UH6XvG6u1wfNPDJ4CKetTEP0pfYC0HdYStBxgbe5xIkI2/fjxQNTyRX8AynR70FFZVZhqbLklpkffQWvTmMCfVTAij9xJth+znvL4W5IG165L45mAmC2JstuCCGLR3+EXL8+2IOIANOrCfC+sMoMJqwuI/y5A9REqGWBYCqZBkkVYdz88Jcw1ri243ewq9nJzD1f+IxpXqMFLLKrpgEs3CWJs0ax4rUz1OQLpGQ1dnkgdVts3CJ57EigLrKCoP3K7mU9pptW8+Kk+tdPgjR4Eb2RtulIGHcOz6+uW7UQfrtd/6ceGgPd1YUTklda815UNQhAnHDuWXRMybQLFaPgtAP4r+cY49IEyuAoL6cRz8owxxb6RpAU1XN9TYWkZfez7eYFD2PrGt16pvPLMDBZFK4YmgmpDjSzPiHh4c11zz+poQHOA+DcLsBoh3RRAHCClKsdCVEpzb5ygwv242AXteBA3A9DImRpRlibqiqsN3hFGhUJF4UcJqPyRk3VJLfeO4dTLc5Ac2jQojpOrp4Gl8+1decC+8FuT2+e1X6U4tOb0WHNuNxBzTFVSSee+kjEIAJZAif5mmfYvTrg2XDbEBOcV4a/pKrXrChUTU/3HFldUtuRuMIKcmHDSfRftuAT+jxTIDvSMq5uwTO09gUHyXfdWPuIjTbqiaL3KCdrHL3OulEDgEqbddobHZYz1tpQnibdgzFYN2IXCvHTjz8YjeNgoEK6jZvYqeE0xFBT3DdLrqznQkJWrQ6R5XYbA1kVgC4JMKv78Oan+R2a58PXuF7XMahOj5k65tq4THj5FguDCcFB0xSXZGlf++c4l3mxlAv2mbouM4737ecIbj4/ksAJX35rx+swOs72kL3LdgFF4lktUx2WXySp9Ap2WRb2v4oUTsacF1TA8MQBCZzuYjDFAr+kcpNpHBUeErPxuCJFHs+HHyrKJocco2vmY2QPqt8Ha80AzB0g17pMcV41n6h65WU7PPVXN8VOv9iaO9hMl3scyd8ImX/t0OUjzlfElL/mFbsKAmXtTJLfEKO4MnaH57EBgV16GodTupVG7cAY04Rv4/wS8NwRKveePz9TTnMbmgPEJNqYIEoEGjOlcagsElVfTP6P0LS9TUS69O/m0MClRRF/peDHjBeNo3PQnQQ8yK/uRSO8iyGPS9e511kE4e6lemdCFWEwqypAkzLBb6lDbsg=="

    private const val MASK_NAME = "HeadphoneMusic"
  }

  private val banubaSdkManager by lazy(LazyThreadSafetyMode.NONE) {
    BanubaSdkManager(context.applicationContext)
  }

  private val maskUri by lazy(LazyThreadSafetyMode.NONE) {
    Uri.parse(BanubaSdkManager.getResourcesBase())
      .buildUpon()
      .appendPath("effects")
      .appendPath(MASK_NAME)
      .build()
  }

  private val agoraRtc: RtcEngine by lazy(LazyThreadSafetyMode.NONE) {
    RtcEngine.create(
      context,
      "8ed92bbde13744af882fdd8963e824c4",
      null
    )
  }

  init {
    try {
      Log.e("logggggggg","loggggggggg1")
      BanubaSdkManager.initialize(context.applicationContext,
        BANUBA_CLIENT_TOKEN
      )
      configureSdkManager()
      surface = RtcEngine.CreateRendererView(context)
      //surface = setupRemoteVideo(0)
      //canvas = VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, 0)
    //  configureRtcEngine()
      Log.e("logggggggg","loggggggggg2")
      
      banubaSdkManager.attachSurface(surface)
      banubaSdkManager.openCamera()
      
      Log.e("logggggggg","loggggggggg3")
    } catch (e: UnsatisfiedLinkError) {
      throw RuntimeException("Please init RtcEngine first!")
    }

//    surface = setupRemoteVideo(0)
    removeAllViews()
    canvas = VideoCanvas(surface)
    addView(surface)

    banubaSdkManager.effectPlayer.setEffectVolume(0F)
    banubaSdkManager.effectPlayer.playbackPlay()
    banubaSdkManager.startForwardingFrames()

    Log.e("logggggggg","loggggggggg4")
  }

  private fun configureSdkManager() {
    banubaSdkManager.effectManager.loadAsync(maskUri.toString())
  }

  private fun configureRtcEngine() {
    agoraRtc.setExternalVideoSource(true, false, true)
    agoraRtc.enableVideo()
  }

  private fun setupRemoteVideo(uid: Int): SurfaceView {
    surface = RtcEngine.CreateRendererView(context)
//    canvas = VideoCanvas(surface, VideoCanvas.RENDER_MODE_HIDDEN, uid)
    agoraRtc.setupRemoteVideo(canvas)
    return surface
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
//    removeAllViews()
//    surface = RtcEngine.CreateRendererView(context.applicationContext)
//    surface.setZOrderMediaOverlay(isMediaOverlay)
//    surface.setZOrderOnTop(onTop)
//    addView(surface)
    surface.layout(0, 0, width, height)
    canvas.view = surface
    if (canvas.uid == 0) {
      engine.setupLocalVideo(canvas)
    } else {
      engine.setupRemoteVideo(canvas)
    }

    engine.setExternalVideoSource(true, false, true)
    engine.enableVideo()
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
