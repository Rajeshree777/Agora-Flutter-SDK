import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'enum_converter.dart';
import 'enums.dart';
import 'rtc_channel.dart';
import 'rtc_engine.dart';

final Map<int, MethodChannel> _channels = {};

/// Use SurfaceView in Android.
///
/// Use [UIView](https://developer.apple.com/documentation/uikit/uiview) in iOS.
class BanubaSurfaceView extends StatefulWidget {
  /// User ID.
  final int? uid;

  /// The unique channel name for the AgoraRTC session in the string format. The string length must be less than 64 bytes. Supported character scopes are:
  /// - All lowercase English letters: a to z.
  /// - All uppercase English letters: A to Z.
  /// - All numeric characters: 0 to 9.
  /// - The space character.
  /// - Punctuation characters and other symbols, including: "!", "#", "$", "%", "&", "(", ")", "+", "-", ":", ";", "<", "=", ".", ">", "?", "@", "\[", "\]", "^", "_", " {", "}", "|", "~", ",".
  ///
  /// **Note**
  /// - The default value is the empty string "". Use the default value if the user joins the channel using the [RtcEngine.joinChannel] method in the [RtcEngine] class.
  /// - If the user joins the channel using the [RtcChannel.joinChannel] method in the [RtcChannel] class, set this parameter as the channelId of the [RtcChannel] object.
  final String? channelId;

  /// The rendering mode of the video view.
  final VideoRenderMode renderMode;

  /// The video mirror mode.
  final VideoMirrorMode mirrorMode;

  /// Control whether the surface view's surface is placed on top of its window.
  ///
  /// See [TargetPlatform.android].
  final bool? zOrderOnTop;

  /// Control whether the surface view's surface is placed on top of another regular surface view in the window (but still behind the window itself).
  ///
  /// See [TargetPlatform.android].
  final bool? zOrderMediaOverlay;

  /// Callback signature for when a platform view was created.
  ///
  /// `id` is the platform view's unique identifier.
  final PlatformViewCreatedCallback? onPlatformViewCreated;

  /// Which gestures should be consumed by the web view.
  ///
  /// It is possible for other gesture recognizers to be competing with the web view on pointer
  /// events, e.g if the web view is inside a [ListView] the [ListView] will want to handle
  /// vertical drags. The web view will claim gestures that are recognized by any of the
  /// recognizers on this list.
  ///
  /// When this set is empty or null, the web view will only handle pointer events for gestures that
  /// were not claimed by any other gesture recognizer.
  final Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers;

  final String? effectName;

  final int? surfaceHeight;
  final int? surfaceWidth;

  final bool isFrontCamera;

  /// Constructs a [BanubaSurfaceView]
  BanubaSurfaceView({
    Key? key,
    @required this.uid,
    required this.channelId,
    this.renderMode = VideoRenderMode.Hidden,
    this.mirrorMode = VideoMirrorMode.Auto,
    this.zOrderOnTop = false,
    this.zOrderMediaOverlay = false,
    this.onPlatformViewCreated,
    this.gestureRecognizers,
    this.effectName,
    this.surfaceHeight,
    this.surfaceWidth,
    this.isFrontCamera = true,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _BanubaSurfaceViewState();
  }
}

class _BanubaSurfaceViewState extends State<BanubaSurfaceView> {
  int? _id;
  int? _renderMode;
  int? _mirrorMode;

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return GestureDetector(
        behavior: HitTestBehavior.opaque,
        child: AndroidView(
          viewType: 'BanubaSurfaceView',
          onPlatformViewCreated: onPlatformViewCreated,
          hitTestBehavior: PlatformViewHitTestBehavior.transparent,
          creationParams: {
            'data': {
              'uid': widget.uid,
              'channelId': widget.channelId,
              "effectName": widget.effectName,
            },
            'renderMode': _renderMode,
            'mirrorMode': _mirrorMode,
            'zOrderOnTop': widget.zOrderOnTop,
            'zOrderMediaOverlay': widget.zOrderMediaOverlay,
          },
          creationParamsCodec: const StandardMessageCodec(),
          gestureRecognizers: widget.gestureRecognizers,
        ),
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return GestureDetector(
        behavior: HitTestBehavior.opaque,
        child: UiKitView(
          viewType: 'BanubaSurfaceView',
          onPlatformViewCreated: onPlatformViewCreated,
          hitTestBehavior: PlatformViewHitTestBehavior.transparent,
          creationParams: {
            'data': {
              'uid': widget.uid,
              'channelId': widget.channelId,
              'effectName': widget.effectName,
              'surfaceHeight': widget.surfaceHeight,
              'surfaceWidth': widget.surfaceWidth,
              'isFrontCamera': widget.isFrontCamera,
            },
            'renderMode': _renderMode,
            'mirrorMode': _mirrorMode,
          },
          creationParamsCodec: const StandardMessageCodec(),
          gestureRecognizers: widget.gestureRecognizers,
        ),
      );
    }
    return Text('$defaultTargetPlatform is not yet supported by the plugin');
  }

  @override
  void initState() {
    super.initState();
    _renderMode = VideoRenderModeConverter(widget.renderMode).value();
    _mirrorMode = VideoMirrorModeConverter(widget.mirrorMode).value();
  }

  @override
  void didUpdateWidget(BanubaSurfaceView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.uid != widget.uid ||
        oldWidget.channelId != widget.channelId ||
        oldWidget.effectName != widget.effectName ||
        oldWidget.surfaceHeight != widget.surfaceHeight ||
        oldWidget.surfaceWidth != widget.surfaceWidth) {
      setData();
    }
    if (oldWidget.renderMode != widget.renderMode) {
      setRenderMode();
    }
    if (oldWidget.mirrorMode != widget.mirrorMode) {
      setMirrorMode();
    }
    if (defaultTargetPlatform == TargetPlatform.android) {
      if (oldWidget.zOrderOnTop != widget.zOrderOnTop) {
        setZOrderOnTop();
      }
      if (oldWidget.zOrderMediaOverlay != widget.zOrderMediaOverlay) {
        setZOrderMediaOverlay();
      }
    }
  }

  @override
  void dispose() {
    super.dispose();
    _channels.remove(_id);
  }

  void setData() {
    if (widget.uid == null) return;
    _channels[_id]?.invokeMethod('setData', {
      'data': {
        'uid': widget.uid,
        'channelId': widget.channelId,
        'effectName': widget.effectName,
        'surfaceHeight': widget.surfaceHeight,
        'surfaceWidth': widget.surfaceWidth
      }
    });
  }

  void setRenderMode() {
    if (widget.renderMode == null) return;
    _renderMode = VideoRenderModeConverter(widget.renderMode).value();
    _channels[_id]?.invokeMethod('setRenderMode', {'renderMode': _renderMode});
  }

  void setMirrorMode() {
    if (widget.mirrorMode == null) return;
    _mirrorMode = VideoMirrorModeConverter(widget.mirrorMode).value();
    _channels[_id]?.invokeMethod('setMirrorMode', {'mirrorMode': _mirrorMode});
  }

  void setZOrderOnTop() {
    if (widget.zOrderOnTop == null) return;
    _channels[_id]
        ?.invokeMethod('setZOrderOnTop', {'onTop': widget.zOrderOnTop});
  }

  void setZOrderMediaOverlay() {
    if (widget.zOrderMediaOverlay == null) return;
    _channels[_id]?.invokeMethod(
        'setZOrderMediaOverlay', {'isMediaOverlay': widget.zOrderMediaOverlay});
  }

  Future<void> onPlatformViewCreated(int id) async {
    _id = id;
    if (!_channels.containsKey(id)) {
      _channels[id] = MethodChannel('agora_rtc_engine/banuba_surface_view_$id');
    }
    widget.onPlatformViewCreated?.call(id);
  }
}

/// Use TextureView in Android.
/// Not support for iOS.
/// [TargetPlatform.android]
class BanubaTextureView extends StatefulWidget {
  /// User ID.
  final int? uid;

  /// The unique channel name for the AgoraRTC session in the string format. The string length must be less than 64 bytes. Supported character scopes are:
  /// - All lowercase English letters: a to z.
  /// - All uppercase English letters: A to Z.
  /// - All numeric characters: 0 to 9.
  /// - The space character.
  /// - Punctuation characters and other symbols, including: "!", "#", "$", "%", "&", "(", ")", "+", "-", ":", ";", "<", "=", ".", ">", "?", "@", "\[", "\]", "^", "_", " {", "}", "|", "~", ",".
  ///
  /// **Note**
  /// - The default value is the empty string "". Use the default value if the user joins the channel using the [RtcEngine.joinChannel] method in the [RtcEngine] class.
  /// - If the user joins the channel using the [RtcChannel.joinChannel] method in the [RtcChannel] class, set this parameter as the channelId of the [RtcChannel] object.
  final String? channelId;

  /// The rendering mode of the video view.
  final VideoRenderMode renderMode;

  /// The video mirror mode.
  final VideoMirrorMode mirrorMode;

  /// Callback signature for when a platform view was created.
  ///
  /// `id` is the platform view's unique identifier.
  final PlatformViewCreatedCallback? onPlatformViewCreated;

  /// Which gestures should be consumed by the web view.
  ///
  /// It is possible for other gesture recognizers to be competing with the web view on pointer
  /// events, e.g if the web view is inside a [ListView] the [ListView] will want to handle
  /// vertical drags. The web view will claim gestures that are recognized by any of the
  /// recognizers on this list.
  ///
  /// When this set is empty or null, the web view will only handle pointer events for gestures that
  /// were not claimed by any other gesture recognizer.
  final Set<Factory<OneSequenceGestureRecognizer>>? gestureRecognizers;

  /// Constructs a [BanubaTextureView]
  BanubaTextureView({
    Key? key,
    @required this.uid,
    this.channelId,
    this.renderMode = VideoRenderMode.Hidden,
    this.mirrorMode = VideoMirrorMode.Auto,
    this.onPlatformViewCreated,
    this.gestureRecognizers,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _BanubaTextureViewState();
  }
}

class _BanubaTextureViewState extends State<BanubaTextureView> {
  int? _id;
  int? _renderMode;
  int? _mirrorMode;

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return GestureDetector(
        behavior: HitTestBehavior.opaque,
        child: AndroidView(
          viewType: 'BanubaSurfaceView',
          onPlatformViewCreated: onPlatformViewCreated,
          hitTestBehavior: PlatformViewHitTestBehavior.transparent,
          creationParams: {
            'data': {'uid': widget.uid, 'channelId': widget.channelId},
            'renderMode': _renderMode,
            'mirrorMode': _mirrorMode,
          },
          creationParamsCodec: const StandardMessageCodec(),
          gestureRecognizers: widget.gestureRecognizers,
        ),
      );
    }
    return Text('$defaultTargetPlatform is not yet supported by the plugin');
  }

  @override
  void initState() {
    super.initState();
    _renderMode = VideoRenderModeConverter(widget.renderMode).value();
    _mirrorMode = VideoMirrorModeConverter(widget.mirrorMode).value();
  }

  @override
  void didUpdateWidget(BanubaTextureView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.uid != widget.uid ||
        oldWidget.channelId != widget.channelId) {
      setData();
    }
    if (oldWidget.renderMode != widget.renderMode) {
      setRenderMode();
    }
    if (oldWidget.mirrorMode != widget.mirrorMode) {
      setMirrorMode();
    }
  }

  @override
  void dispose() {
    super.dispose();
    _channels.remove(_id);
  }

  void setData() {
    if (widget.uid == null) return;
    _channels[_id]?.invokeMethod('setData', {
      'data': {'uid': widget.uid, 'channelId': widget.channelId}
    });
  }

  void setRenderMode() {
    if (widget.renderMode == null) return;
    _renderMode = VideoRenderModeConverter(widget.renderMode).value();
    _channels[_id]?.invokeMethod('setRenderMode', {'renderMode': _renderMode});
  }

  void setMirrorMode() {
    if (widget.mirrorMode == null) return;
    _mirrorMode = VideoMirrorModeConverter(widget.mirrorMode).value();
    _channels[_id]?.invokeMethod('setMirrorMode', {'mirrorMode': _mirrorMode});
  }

  Future<void> onPlatformViewCreated(int id) async {
    _id = id;
    if (!_channels.containsKey(id)) {
      _channels[id] = MethodChannel('agora_rtc_engine/banuba_texture_view_$id');
    }
    widget.onPlatformViewCreated?.call(id);
  }
}
