import 'dart:io';
import 'dart:ui';
import 'package:agora_rtc_engine/rtc_engine.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:camera/camera.dart';
import 'package:soda_app/source/color_assets.dart';
import 'package:soda_app/source/styles.dart';
import 'package:soda_app/ui/video_dating_screen.dart';
import 'package:soda_app/utils/common_widgets.dart';
import 'package:soda_app/utils/constants.dart';
import 'package:soda_app/utils/print_log.dart';
import 'package:soda_app/utils/scale_page_route_animation.dart';
import 'package:soda_app/utils/screen_util.dart';
import 'package:soda_app/widget/effect_selector_view.dart';
import '../source/string_assets.dart';
import '../widget/appbar_widget.dart';
import 'package:flutter_svg/svg.dart';
import 'package:soda_app/source/image_assets.dart';
import 'package:agora_rtc_engine/rtc_banuba_view.dart' as RtcBanubaView;

class CallScreen extends StatefulWidget {
  @required
  final bool isStartCall;
  @required
  final String token, channel, videoCallUid;
  final String callerName, receiverName;

  CallScreen(
      {this.isStartCall = false,
      this.token,
      this.channel,
      this.videoCallUid,
      this.callerName,
      this.receiverName});

  @override
  _CallScreenState createState() => _CallScreenState();
}

class _CallScreenState extends State<CallScreen> {
  CameraController cameraController;
  List cameras = List();
  bool isMuted = false;
  bool isCameraOn = true;
  bool isFrontCamera = true;
  int cameraIndex =
      1; // TODOD: Banuba 0 - Front & 1 - Back, In Native Camera 0 - Back & 1 - Front
  bool isInitialized = true;
  var scale;
  GlobalKey cameraWidgetKey = GlobalKey();
  double screenWidth;
  double screenHeight;
  RenderBox renderBoxRed;
  bool isCallInitialized = false, showEffectList = false;
  var navBack;
  RtcBanubaEngine _engine;

  String selectedEffect = "";

  @override
  void initState() {
    SystemChrome.setEnabledSystemUIOverlays(SystemUiOverlay.values);
    SystemChrome.setSystemUIOverlayStyle(SystemUiOverlayStyle(
      statusBarColor: ColorAssets.themeColorWhite,
    ));
    super.initState();
    _checkCameraAvailability();
    // initializeCamera();
  }

  // Initialize Agora and throw error message if anything is wrong
  Future<void> initializeBanuba() async {
    await _initAgoraRtcEngine();
  }

  // Create agora sdk instance and initialize
  Future<void> _initAgoraRtcEngine() async {
    _engine = await RtcBanubaEngine.createWithConfig(RtcBanubaEngineConfig());
    setState(() {
      // _camera = Camera.FRONT_CAMERA;
    });
  }

  var banubaWidgetSize = Size.zero;

  @override
  Widget build(BuildContext context) {
    screenWidth = MediaQuery.of(context).size.width;
    screenHeight = MediaQuery.of(context).size.height;
    Constant.setScreenAwareConstant(context);
    return Scaffold(
      key: cameraWidgetKey,
      appBar: AppBarWidget(
        iconLeftName: widget.isStartCall ? Icons.arrow_back_rounded : null,
        title: widget.isStartCall
            ? StringAssets.startVideoCall
            : StringAssets.joinVideoCall,
        onBackPressed: () {
          Navigator.pop(context);
        },
      ),
      body: Column(
        children: [
          // Camera Container
          Expanded(
            flex: 5,
            child: (Platform.isIOS && Constants.IS_BANUBA_ON)

                /// Banuba Camera
                ? (Container(
                    width: MediaQuery.of(context).size.width,
                    child: (isCameraOn)
                        ? (_engine != null)
                            ? _banubaCameraWidget()
                            : Container()
                        : _cameraTurnedOffWidget(),
                  ))
                : isInitialized
                    ? Container(
                        width: MediaQuery.of(context).size.width,
                        decoration: BoxDecoration(),
                        // show Camera Preview if camera is on
                        child: isCameraOn
                            ? _cameraPreviewWidget()
                            : _cameraTurnedOffWidget())
                    : Container(),
          ),
          widget.isStartCall ? buildStartCallWidget() : buildJoinCallWidget(),
        ],
      ),
    );
  }

  Widget _banubaCameraWidget() {
    return Center(
      child: Container(
        padding: EdgeInsets.only(
            top: Constant.size36,
            left: Constant.size40,
            right: Constant.size40,
            bottom: Constant.size18),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(Constant.size20),
          child: Stack(
            fit: StackFit.expand,
            children: [
              MeasureSize(
                onChange: (size) {
                  setState(() {
                    banubaWidgetSize = size;
                  });
                  print('Size $size');
                },
                child: banubaWidgetSize.width == 0
                    ? Container(
                        child: Center(
                          child: CircularProgressIndicator(),
                        ),
                      )
                    : RtcBanubaView.SurfaceView(
                        effectName: selectedEffect,
                        renderMode: VideoRenderMode.FILL,
                        surfaceHeight: banubaWidgetSize.height.toInt(),
                        surfaceWidth: banubaWidgetSize.width.toInt(),
                      ),
              ),
              Align(
                  alignment: Alignment.bottomCenter,
                  child: Container(
                      width: double.infinity, child: buildCameraControls()))
            ],
          ),
        ),
      ),
    );
  }

  // camera controls view
  Widget buildCameraControls() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        /// AR effect selector view
        _buildFilterSelector(),

        /// Camera Controlls view
        Container(
          margin: EdgeInsets.symmetric(
              horizontal: Constant.size36, vertical: Constant.size16),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(Constant.size9),
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
              child: Container(
                height: Constant.size50,
                padding: EdgeInsets.symmetric(horizontal: Constant.size5),
                decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(Constant.size9),
                    color: Colors.black38.withOpacity(0.3)),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    if (Platform.isIOS && Constants.IS_BANUBA_ON)
                      Flexible(
                        flex: 1,
                        child: InkWell(
                            key: Key("videoEffectKey"),
                            onTap: () {
                              setState(() {
                                showEffectList = !showEffectList;
                              });
                            },
                            child: Padding(
                              padding: EdgeInsets.all(Constant.size5),
                              child: SvgPicture.asset(
                                ImageAssets.videoRecordingEffects,
                                color: Colors.white,
                                height: Constant.size22,
                                width: Constant.size22,
                              ),
                            )),
                      ),
                    Flexible(
                      flex: 1,
                      child: InkWell(
                        key: Key("micKey"),
                        onTap: () {
                          setState(() {
                            isMuted = !isMuted;
                          });
                        },
                        child: Padding(
                          padding: EdgeInsets.all(Constant.size5),
                          child: SvgPicture.asset(
                            isMuted
                                ? ImageAssets.micOffIcon
                                : ImageAssets.micOnIcon,
                            color: Colors.white,
                            height: Constant.size20,
                            width: Constant.size20,
                          ),
                        ),
                      ),
                    ),
                    Flexible(
                      flex: 1,
                      child: InkWell(
                        key: Key("disableCameraKey"),
                        onTap: () {
                          setState(() {
                            isCameraOn = !isCameraOn;

                            if (!isCameraOn) {
                              if (Platform.isIOS && Constants.IS_BANUBA_ON) {
                                printLog("Camera on/off");
                                _engine?.cameraPauseStop(true);
                                // _engine?.destroyBanubaCamera();
                                // _engine = null;
                              }
                            } else {
                              if (Platform.isIOS && Constants.IS_BANUBA_ON) {
                                _engine?.cameraPauseStop(false);
                              } else {
                                _checkCameraAvailability();
                              }
                            }
                          });
                        },
                        child: Padding(
                          padding: EdgeInsets.all(Constant.size5),
                          child: SvgPicture.asset(
                            !isCameraOn
                                ? ImageAssets.cameraOffIcon
                                : ImageAssets.cameraOnIcon,
                            height: Constant.size18,
                            width: Constant.size18,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    ),
                    Flexible(
                      flex: 1,
                      child: InkWell(
                        key: Key("switchCameraKey"),
                        onTap: () {
                          if (Platform.isIOS && Constants.IS_BANUBA_ON) {
                            _engine.switchCamera();
                            if (cameraIndex == 0) {
                              cameraIndex = 1;
                            } else {
                              cameraIndex = 0;
                            }
                          } else {
                            if (cameraIndex == 0) {
                              setState(() {
                                cameraIndex = 1;
                                _initCameraController();
                                //cameraController.initialize();
                              });
                            } else {
                              setState(() {
                                cameraIndex = 0;
                                _initCameraController();
                                //initializeCamera();
                                //cameraController.initialize();
                              });
                            }
                          }
                        },
                        child: Padding(
                          padding: EdgeInsets.all(Constant.size5),
                          child: SvgPicture.asset(
                            ImageAssets.switchCameraIcon,
                            color: Colors.white,
                            height: Constant.size20,
                            width: Constant.size20,
                          ),
                        ),
                      ),
                    )
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  // waiting to join user widget
  Widget buildWaitingToJoinCallWidget() {
    return Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(Constant.size9),
        ),
        margin: EdgeInsets.only(
            left: Constant.size25,
            right: Constant.size25,
            bottom: Constant.size36),
        width: double.infinity,
        child: GestureDetector(
          onTap: () async {},
          child: Container(
            height: Constant.size50,
            width: double.infinity,
            alignment: Alignment.center,
            //padding: EdgeInsets.all(Constant.size16),
            decoration: BoxDecoration(
              color: ColorAssets.themeColorMagenta,
              borderRadius: BorderRadius.circular(Constant.size12),
            ),
            child: Container(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Container(
                      height: Constant.size24,
                      width: Constant.size24,
                      padding: EdgeInsets.all(5.0),
                      decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: ColorAssets.themeColorRed,
                          boxShadow: [
                            BoxShadow(
                                color: Colors.black.withOpacity(0.3),
                                blurRadius: 5.0)
                          ]),
                      child: SvgPicture.asset(
                        ImageAssets.endCallIcon,
                        color: Colors.white,
                        height: Constant.size10,
                        width: Constant.size10,
                      )),
                  SizedBox(
                    width: Constant.size10,
                  ),
                  Text(
                    "Waiting to Join Rechard...",
                    style: Styles.startCallStyle,
                  ),
                ],
              ),
            ),
          ),
        ));
  }

  // Start call widget
  Widget buildStartCallWidget() {
    return Container(
      margin: EdgeInsets.only(
          left: Constant.size25,
          right: Constant.size25,
          bottom: Constant.size48,
          top: Constant.size18),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          Expanded(
              child: Text(
            "Call @${widget.receiverName}... ",
            style: TextStyle(
                fontFamily: StringAssets.fontFamilyPoppins,
                fontSize: FontSize.s14),
          )),
          GestureDetector(
            onTap: () async {
                    navBack =
                        await Navigator.of(context).pushReplacement(new ScalePageRoute(
                      VideoDatingScreen(
                        agoraToken: widget.token,
                        channelName: widget.channel,
                        role: ClientRole.Broadcaster,
                        userType: UserType.CALLER,
                        cameraOn: isCameraOn,
                        isFrontCamera: (cameraIndex == 0) ? false : true,
                        isMuted: isMuted,
                        isFromNotification: false,
                        selectedEffect: selectedEffect,
                        // toUserId: widget.fromId,
                        // me: "O",
                        // gameId:'${widget.fromId}-${PreferenceHelper.getUserData().id.toString()}'
                      ),
                    ));
                    if (navBack != null) {
                      if (isCameraOn) _checkCameraAvailability();
                    }

              /* Navigator.of(context).push(new ScalePageRoute(VideoDatingScreen(
                agoraToken: widget.token,
                channelName: widget.channel,
                role: ClientRole.Broadcaster,
                videoCallId: widget.videoCallUid,
                userType: UserType.CALLER,
                cameraOn: isCameraOn,
                isFrontCamera: (cameraIndex == 1)? true : false,
                isMuted: isMuted,
              ),));*/
            },
            child: Container(
              height: Constant.size50,
              alignment: Alignment.center,
              padding: EdgeInsets.symmetric(horizontal: Constant.size20),
              decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(9),
                  color: ColorAssets.colorJoinCall),
              child: Text(
                StringAssets.startCall,
                style: Styles.joinCallStyle,
              ),
            ),
          )
        ],
      ),
    );
  }

  // join call widget
  Widget buildJoinCallWidget() {
    return Flexible(
      child: Align(
        alignment: Alignment.bottomCenter,
        child: Row(
          children: [
            /// end call button
            Expanded(
              child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(Constant.size9),
                  ),
                  margin: EdgeInsets.only(
                      left: Constant.size25, bottom: Constant.size48),
                  width: double.infinity,
                  child: GestureDetector(
                    onTap: () {
                        Navigator.pop(context);
                    },
                    child: Container(
                      height: Constant.size50,
                      width: double.infinity,
                      alignment: Alignment.center,
                      //padding: EdgeInsets.all(Constant.size16),
                      decoration: BoxDecoration(
                        color: ColorAssets.themeColorRed,
                        borderRadius: BorderRadius.circular(Constant.size12),
                      ),
                      child: Container(
                        child: Text(
                          StringAssets.decline,
                          style: Styles.startCallStyle,
                        ),
                      ),
                    ),
                  )),
            ),
            SizedBox(width: ScreenUtil().setWidth(24)),

            /// join call button
            Expanded(
              child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(Constant.size9),
                  ),
                  margin: EdgeInsets.only(
                      right: Constant.size25, bottom: Constant.size48),
                  width: double.infinity,
                  child: GestureDetector(
                    onTap: () {
                      if (Platform.isIOS && Constants.IS_BANUBA_ON) {
                        printLog("push to join call");
                        _engine?.cameraPauseStop(true);
                        _engine?.destroyBanubaCamera();
                        _engine = null;
                      }
                      Navigator.of(context)
                          .pushReplacement(new ScalePageRoute(
                        VideoDatingScreen(
                          agoraToken: widget.token,
                          channelName: widget.channel,
                          role: ClientRole.Broadcaster,
                          userType: UserType.RECEIVER,
                          cameraOn: isCameraOn,
                          isFrontCamera: (cameraIndex == 0) ? false : true,
                          isMuted: isMuted,
                          isFromNotification: false,
                          selectedEffect: selectedEffect,
                        ),
                      ))
                          .then((navBack) {
                        if (navBack != null) {
                          if (isCameraOn) _checkCameraAvailability();
                        }
                      });
                    },
                    child: Container(
                      height: Constant.size50,
                      width: double.infinity,
                      alignment: Alignment.center,
                      //padding: EdgeInsets.all(Constant.size16),
                      decoration: BoxDecoration(
                        color: ColorAssets.themeColorLightGreen,
                        borderRadius: BorderRadius.circular(Constant.size12),
                      ),
                      child: Container(
                        child: Text(
                          StringAssets.joinCall,
                          style: Styles.startCallStyle,
                        ),
                      ),
                    ),
                  )),
            ),
          ],
        ),
      ),
    );
  }

  Widget _cameraPreviewWidget() {
    // If the controller not initialised or permission not given
    if (cameraController == null || !cameraController.value.isInitialized) {
      return Center(
        child: Container(
          margin: EdgeInsets.only(
              top: Constant.size36,
              left: Constant.size40,
              right: Constant.size40,
              bottom: Constant.size18),
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(Constant.size20),
              color: Colors.black,
            ),
          ),
        ),
      );
    } else {
      return Center(
        child: Container(
          width: double.infinity,
          padding: EdgeInsets.only(
              top: Constant.size36,
              left: Constant.size40,
              right: Constant.size40,
              bottom: Constant.size18),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(Constant.size20),
            child: (cameraController != null &&
                    cameraController.value.isInitialized)
                ? CameraPreview(
                    cameraController,
                    child: Align(
                        alignment: Alignment.bottomCenter,
                        child: Container(
                            width: double.infinity,
                            child: buildCameraControls())),
                  )
                : Container(),
          ),
        ),
      );
    }
  }

  Widget _cameraTurnedOffWidget() {
    return Center(
      child: Container(
        margin: EdgeInsets.only(
            top: Constant.size36,
            left: Constant.size40,
            right: Constant.size40,
            bottom: Constant.size18),
        child: Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(Constant.size20),
            color: Colors.black,
          ),
          child: Stack(
            children: [
              Center(
                child: Text(
                  StringAssets.cameraDisableMsg,
                  style: Styles.joinCallStyle,
                ),
              ),
              Align(
                  alignment: Alignment.bottomCenter,
                  child: Container(
                      width: double.infinity, child: buildCameraControls())),
            ],
          ),
        ),
      ),
    );
  }

  initializeCamera() async {
    availableCameras().then((value) {
      setState(() async {
        if (cameras.isNotEmpty) {
          cameras.clear();
        }
        cameras = value;
        cameraController =
            CameraController(cameras[cameraIndex], ResolutionPreset.max);
        cameraController.initialize().then((value) {
          setState(() {});
        });
        isInitialized = true;
      });
    });
  }

  @override
  void didChangeDependencies() {
    // TODO: implement didChangeDependencies
    super.didChangeDependencies();
    print(' did Changed ');
    // initializeCamera();
  }

  @override
  void didUpdateWidget(covariant CallScreen oldWidget) {
    // TODO: implement didUpdateWidget
    super.didUpdateWidget(oldWidget);
    print(" did Updated ");
    // initializeCamera();
  }

  Future _initCameraController() async {
    // if (cameraController != null) {
    //   await cameraController.dispose();
    // }

    CameraDescription cameraDescription = cameras[cameraIndex];

    cameraController = CameraController(
        cameraDescription, ResolutionPreset.high,
        enableAudio: isMuted);

    cameraController.addListener(() {
      if (mounted) {
        setState(() {});
      }

      if (cameraController.value.hasError) {
        printLog('Camera error ${cameraController.value.errorDescription}');
      }
    });

    try {
      await cameraController.initialize();
      setState(() {
        isInitialized = true;
      });
    } on CameraException catch (e) {
      printLog("init error $e");
    }
    if (mounted) {
      setState(() {});
    }
  }

  _checkCameraAvailability() {
    availableCameras().then((availableCameras) async {
      cameras = availableCameras;
      if (cameras.length > 0) {
        if (Platform.isIOS && Constants.IS_BANUBA_ON) {
          initializeBanuba();
        } else {
          _initCameraController().then((void v) {});
        }
      } else {
        //TODO: Show alert for no camera
        printLog("No camera available");
      }
    }).catchError((err) {
      printLog('Error: $err.code\nError Message: $err.message');
    });
  }

  @override
  void dispose() {
    if (Platform.isIOS && Constants.IS_BANUBA_ON) {
      _engine?.cameraPauseStop(true);
      _engine?.destroyBanubaCamera();
    } else {
      if (cameraController != null) cameraController.dispose();
    }

    super.dispose();
  }

  // AR effects selector
  Widget _buildFilterSelector() {
    return Visibility(
      visible: showEffectList,
      child: Align(
        alignment: Alignment.bottomCenter,
        child: FilterSelector(
          onFilterChanged: onEffectChanged,
          filters: effectNames,
        ),
      ),
    );
  }

  List<String> effectNames = [
    "",
    "HeadphoneMusic",
    "BeautyBokeh",
    "BeautyEffectsGrayscale",
    "BGVideoBeach",
    "Emoji",
    "HeartsLut",
    "Polaroid",
    "PrideParade",
    "RainbowBeauty",
    "WhiteCat"
  ];

  void onEffectChanged(String effectName) {
    selectedEffect = effectName;
    _engine.onEffectSelected(effectName);
  }

  Future<dynamic> showRemoteUserStatusDialog(BuildContext context,
      {String code, String message}) {
    return showDialog(
        barrierDismissible: false,
        context: context,
        builder: (context) {
          return Dialog(
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(Constant.size24)),
            //this right here
            child: IntrinsicHeight(
              child: Container(
                padding: EdgeInsets.all(Constant.size16),
                decoration: Styles.containerWithShadowBox,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Padding(
                      padding: EdgeInsets.all(Constant.size10),
                      child: Text(
                        message,
                        style: TextStyle(
                          color: ColorAssets.themeColorDarkGrey,
                          fontSize: FontSize.s16,
                          fontFamily: StringAssets.fontFamilyPoppins,
                        ),
                      ),
                    ),
                    SizedBox(height: Constant.size18),
                    CommonWidgets().buttonView(StringAssets.ok, onClick: () {
                        Navigator.pop(context, true);
                    })
                  ],
                ),
              ),
            ),
          );
        });
  }
}

typedef void OnWidgetSizeChange(Size size);

class MeasureSizeRenderObject extends RenderProxyBox {
  Size oldSize;
  final OnWidgetSizeChange onChange;

  MeasureSizeRenderObject(this.onChange);

  @override
  void performLayout() {
    super.performLayout();

    Size newSize = child.size;
    if (oldSize == newSize) return;

    oldSize = newSize;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      onChange(newSize);
    });
  }
}

class MeasureSize extends SingleChildRenderObjectWidget {
  final OnWidgetSizeChange onChange;

  const MeasureSize({
    Key key,
    @required this.onChange,
    @required Widget child,
  }) : super(key: key, child: child);

  @override
  RenderObject createRenderObject(BuildContext context) {
    return MeasureSizeRenderObject(onChange);
  }
}
