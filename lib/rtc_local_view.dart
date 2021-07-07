import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'src/enums.dart';
import 'src/rtc_render_view.dart';

/// SurfaceView.
class SurfaceView extends RtcSurfaceView {
  /// Constructs a [SurfaceView]
  SurfaceView({
    Key? key,
    String? channelId,
    renderMode = VideoRenderMode.Hidden,
    mirrorMode = VideoMirrorMode.Auto,
    zOrderOnTop = false,
    zOrderMediaOverlay = false,
    PlatformViewCreatedCallback? onPlatformViewCreated,
    Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers,
    String effectName,
    int totalJoinedUser,
    bool isFrontCamera = true,
  }) : super(
            key: key,
            zOrderMediaOverlay: zOrderMediaOverlay,
            zOrderOnTop: zOrderOnTop,
            renderMode: renderMode,
            channelId: channelId,
            mirrorMode: mirrorMode,
            gestureRecognizers: gestureRecognizers,
            onPlatformViewCreated: onPlatformViewCreated,
            uid: 0,
            effectName: effectName,
            totalJoinedUser: totalJoinedUser,
            isFrontCamera: isFrontCamera);
}

/// TextureView.
class TextureView extends RtcTextureView {
  /// Constructs a [TextureView]
  TextureView({
    Key? key,
    String? channelId,
    renderMode = VideoRenderMode.Hidden,
    mirrorMode = VideoMirrorMode.Auto,
    PlatformViewCreatedCallback? onPlatformViewCreated,
    Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers,
  }) : super(
          key: key,
          uid: 0,
          channelId: channelId,
          renderMode: renderMode,
          mirrorMode: mirrorMode,
          onPlatformViewCreated: onPlatformViewCreated,
          gestureRecognizers: gestureRecognizers,
        );
}
