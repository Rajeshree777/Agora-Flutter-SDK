import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/services.dart';

import 'src/enums.dart';
import 'src/rtc_render_view.dart';

/// SurfaceView.
class SurfaceView extends RtcSurfaceView {
  /// Constructs a [SurfaceView]
  SurfaceView({
    Key? key,
    required int uid,
    String? channelId,
    renderMode = VideoRenderMode.Hidden,
    mirrorMode = VideoMirrorMode.Auto,
    zOrderOnTop = false,
    zOrderMediaOverlay = false,
    PlatformViewCreatedCallback? onPlatformViewCreated,
    Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers,
    int totalJoinedUser,
  })  : assert(uid != 0),
        super(
            key: key,
            uid: uid,
            zOrderMediaOverlay: zOrderMediaOverlay,
            zOrderOnTop: zOrderOnTop,
            renderMode: renderMode,
            channelId: channelId,
            mirrorMode: mirrorMode,
            gestureRecognizers: gestureRecognizers,
            onPlatformViewCreated: onPlatformViewCreated,
            totalJoinedUser: totalJoinedUser);
}

/// TextureView.
class TextureView extends RtcTextureView {
  /// Constructs a [TextureView]
  TextureView({
    Key? key,
    required int uid,
    String? channelId,
    renderMode = VideoRenderMode.Hidden,
    mirrorMode = VideoMirrorMode.Auto,
    PlatformViewCreatedCallback? onPlatformViewCreated,
    Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers,
  })  : assert(uid != 0),
        super(
          key: key,
          uid: uid,
          channelId: channelId,
          renderMode: renderMode,
          mirrorMode: mirrorMode,
          onPlatformViewCreated: onPlatformViewCreated,
          gestureRecognizers: gestureRecognizers,
        );
}
