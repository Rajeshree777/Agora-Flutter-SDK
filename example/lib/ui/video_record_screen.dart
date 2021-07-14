/*
 * Author: Saurabh-Detharia (saurabh.detharia@bacancy.com)
 * Date: 17-02-21
 * Brief Description: Video recording
 *  Notes:
 *  - It requires camera and path_provider
 */

import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui';

import 'package:agora_rtc_engine/rtc_engine.dart';
import 'package:camera/camera.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_ffmpeg/flutter_ffmpeg.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/svg.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:soda_app/helper/enums.dart';
import 'package:soda_app/model/model_short_video.dart';
import 'package:soda_app/source/color_assets.dart';
import 'package:soda_app/source/image_assets.dart';
import 'package:soda_app/source/string_assets.dart';
import 'package:soda_app/source/styles.dart';
import 'package:soda_app/ui/home_screen.dart';
import 'package:soda_app/utils/common_widgets.dart';
import 'package:soda_app/utils/constants.dart';
import 'package:soda_app/utils/constants_methods.dart';
import 'package:soda_app/utils/print_log.dart';
import 'package:soda_app/utils/screen_util.dart';
import 'package:soda_app/widget/effect_selector_view.dart';
import 'package:video_thumbnail/video_thumbnail.dart';

import 'package:agora_rtc_engine/rtc_banuba_view.dart' as RtcBanubaVuew;

class VideoRecordScreen extends StatefulWidget {
  final Function updateUI;

  VideoRecordScreen({Key key, this.updateUI}) : super(key: key);

  @override
  _VideoRecordScreenState createState() => _VideoRecordScreenState();
}

class _VideoRecordScreenState extends State<VideoRecordScreen> with TickerProviderStateMixin, WidgetsBindingObserver {
  bool _isRecording = false; // To check if recording is ongoing or not
  double _videoSpeed = 1; //To determine the speed of video
  double _totalProgressBarWidth = 0.0; // Total width of progressbar
  double _secondWiseProgressBarWidth = 0.0; // Width to show 1 recording second
  int _startSnippetPosition = 0; // Indicate snippet starting second
  int _progressValue = 0; // To set the progress
  int _totalRecordingDuration = 30; // Max duration of recording (in seconds)
  int _remainingRecordingDuration = 0; // Calculate remaining recording time
  FlutterFFmpeg _flutterFFmpeg = FlutterFFmpeg();
  Timer _recordingTimer; // To set timer for video recording
  Timer _delayTimer; // To set timer before video recording
  Flash _flash; // To set the flash
  Camera _camera; // To check the view of camera
  Audio _audio; // To enable / disable the audio
  Delay _delay; // To set delay before recording

  // For camera
  List cameras;
  CameraController _cameraController; // To handle camera

  List<SingleSnippetModel> _listSnippetModel = []; // List of seconds
  List<VideoModel> _listVideoModel = [];
  bool _showDelayText = false, isPlayBackSpeedChanged = false;

  String _delayText = ""; // List of snippets
  bool flagForCircle = false;
  List<VideoModel> scaledPath = [];
  bool isMergingInProgress = false; // To check merging is in progress

  // Animation for recording button
  AnimationController _recordingButtonAnimationController;

  // Animation for timer text
  AnimationController _textAnimationController;
  Animation<double> _textAnimation;

  // Parameters for progress bar
  double _videoDuration = 0.0;
  Timer _progressTimer;
  double _previousDuration = 0.0;
  List<double> _previousDurationList = [];
  AnimationController _progressAnimation;
  String mergedVideo = "";
  bool updatedRecording = false;
  List<VideoModel> _newlyRecordedVideos = [];
  bool isFirstTime = true;
  bool _isSpeedOptionsVisible = false;
  bool _showPreview = false;
  bool _isFlashVisible = true;
  bool isDialogShowing = false, showEffectList = false;
  String selectedEffect = "", filePath;

  /// Initialization
  @override
  void initState() {
    SystemChrome.setEnabledSystemUIOverlays([]);
    _recordingButtonAnimationController = AnimationController(duration: const Duration(milliseconds: 30), vsync: this);

    final CurvedAnimation curve = CurvedAnimation(parent: _recordingButtonAnimationController, curve: Curves.ease);

    _recordingButtonAnimationController = AnimationController(duration: const Duration(seconds: 1), vsync: this);

    _recordingButtonAnimationController.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        _recordingButtonAnimationController.reverse();
      } else if (status == AnimationStatus.dismissed) {
        _recordingButtonAnimationController.forward();
      }
    });

    _textAnimationController = AnimationController(
      duration: const Duration(milliseconds: 500),
      vsync: this,
    )..repeat(reverse: true);

    _textAnimation = Tween<double>(
      // end: ScreenUtil().setSp(190),
      // begin: ScreenUtil().setSp(0),
      end: 190,
      begin: 0,
    ).animate(_textAnimationController);

    _textAnimationController.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        _textAnimationController.reverse();
      } else if (status == AnimationStatus.dismissed) {
        _textAnimationController.forward();
      }
    });

    _textAnimationController.stop();
    _recordingButtonAnimationController.stop();

    _progressAnimation = AnimationController(duration: const Duration(seconds: 30), vsync: this);

    super.initState();
    _audio = Audio.SOUND_ON;
    _delay = Delay.INSTANT;
    _flash = Flash.FLASH_OFF;
    WidgetsBinding.instance.addObserver(this);
  }

  RtcBanubaEngine _engine;

  // Initialize Agora and throw error message if anything is wrong
  Future<void> initializeBanuba() async {
    await _initAgoraRtcEngine();
  }

  // Create agora sdk instance and initialize
  Future<void> _initAgoraRtcEngine() async {
    _engine = await RtcBanubaEngine.createWithConfig(RtcBanubaEngineConfig());
    setState(() {
      _camera = Camera.FRONT_CAMERA;
      _flash = Flash.FLASH_OFF;
    });
  }

  /// To calculate width
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    if (isFirstTime) {
      isFirstTime = false;
      _isFlashVisible = false;
      checkPermission();
    }

    // Remaining duration = Total duration - Recorded duration
    _remainingRecordingDuration = _totalRecordingDuration - _progressValue;

    // Progress bar width = screen width - spaces - close icon
    _totalProgressBarWidth = MediaQuery.of(context).size.width - Constant.size64;

    // width indicating a second = total width / total duration for recording
    _secondWiseProgressBarWidth = _totalProgressBarWidth / _totalRecordingDuration;

    // Create second wise list
    _createProgressBar();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    printLog("called2");
    if (state == AppLifecycleState.resumed && !isDialogShowing) {
      printLog("called");
      checkPermission();
    }
  }

  /// Leaving screen
  @override
  void dispose() {
    _textAnimationController.dispose();
    _recordingButtonAnimationController.dispose();
    WidgetsBinding.instance.removeObserver(this);
    // camera controller is no longer in use, dispose the controller
    if (_flash == Flash.FLASH_ON) _changeFlash();
    if (_isRecording) {
      if (Platform.isIOS && Constants.IS_BANUBA_ON) {
        _engine.stopVideoRecording();
      } else {
        _cameraController.stopVideoRecording();
      }
    }
    if (Platform.isIOS && Constants.IS_BANUBA_ON) {
      if(_engine != null){
      _engine.cameraPauseStop(true);
      _engine.destroyBanubaCamera();
      _engine = null;
      }
    } else {
      if (_cameraController != null) _cameraController.dispose();
    }
    super.dispose();
  }

  /// Screen UI
  @override
  Widget build(BuildContext context) {
    Constant.setScreenAwareConstant(context);
    // ScreenUtil.init(context, width: MediaQuery.of(context).size.width, height: MediaQuery.of(context).size.height);
    // Record video screen
    return WillPopScope(
        onWillPop: _onWillPopScopeCallback,
        child: Scaffold(
          backgroundColor: Colors.black,
          extendBody: true,
          body: GestureDetector(
            onTap: () {
              setState(() {
                if (showEffectList) showEffectList = !showEffectList;
              });
            },
            child: Stack(
              fit: StackFit.expand,

              /// To set toolbar, record button over the camera preview
              children: [
                (Platform.isIOS && Constants.IS_BANUBA_ON)
                    ? ((_engine != null)
                        ? RtcBanubaVuew.SurfaceView(
                            effectName: selectedEffect,
                            mirrorMode: VideoMirrorMode.Disabled,
                          )
                        : Container())
                    :

                    /// Load camera, all other stuff will above the camera preview
                    _cameraController != null
                        ? _cameraController.value.isInitialized
                            ? _cameraPreviewWidget()
                            : Container()
                        : Container(),

                /// Progress bar and other options...
                Padding(
                  padding: EdgeInsets.symmetric(
                      vertical: ScreenUtil().setHeight(10.0), horizontal: ScreenUtil().setWidth(10.0)),
                  child: Column(
                    children: [
                      /// Appbar

                      /// Progress bar
                      SafeArea(
                        bottom: false,
                        child: Padding(
                          padding: EdgeInsets.only(top: ScreenUtil().setHeight(10.0)),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(7.0),
                            child: Container(
                              width: _totalProgressBarWidth,
                              height: ScreenUtil().setHeight(6.0),
                              child: Stack(
                                children: [
                                  LinearProgressIndicator(
                                    backgroundColor: ColorAssets.themeColorWhite.withOpacity(0.6),
                                    minHeight: ScreenUtil().setHeight(6.0),
                                    value: _progressAnimation.value,
                                    valueColor: AlwaysStoppedAnimation<Color>(ColorAssets.themeColorLightOrange),
                                  ),
                                  for (int i = 0; i < _previousDurationList.length; i++)
                                    Positioned(
                                      left: _previousDurationList[i] * _totalProgressBarWidth,
                                      child: SafeArea(
                                        child: Container(
                                          color: Colors.white,
                                          width: ScreenUtil().setWidth(3.0),
                                          height: ScreenUtil().setHeight(6.0),
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),

                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          /// Close button
                          Padding(
                            padding: EdgeInsets.only(
                              left: ScreenUtil().setWidth(18.0),
                              top: ScreenUtil().setHeight(15.0),
                            ),
                            child: InkWell(
                              onTap: () async {
                                if (_listVideoModel.isNotEmpty) {
                                  showCloseDialog();
                                } else {
                                  await stopRecording();
                                  if (widget.updateUI == null) {
                                    Navigator.pushAndRemoveUntil(
                                        context,
                                        CupertinoPageRoute(builder: (BuildContext context) => HomeScreen()),
                                        (Route<dynamic> route) => route is HomeScreen);
                                  } else {
                                    Future.delayed(Duration(milliseconds: 300), () {
                                      Navigator.pushAndRemoveUntil(
                                          context,
                                          CupertinoPageRoute(builder: (BuildContext context) => HomeScreen()),
                                          (Route<dynamic> route) => route is HomeScreen);
                                    });
                                  }
                                }
                              },
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(Constant.size50),
                                child: ClipRect(
                                  child: BackdropFilter(
                                    filter: ImageFilter.blur(sigmaX: 10.0, sigmaY: 10.0),
                                    child: Container(
                                      height: ScreenUtil().setHeight(35.0),
                                      width: ScreenUtil().setWidth(35.0),
                                      color: ColorAssets.themeColorBlack.withOpacity(0.1),
                                      child: Center(
                                        child: SvgPicture.asset(
                                          ImageAssets.videoRecordingClose,
                                          color: ColorAssets.themeColorWhite,
                                          height: ScreenUtil().setHeight(15.0),
                                          width: ScreenUtil().setWidth(15.0),
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),

                          /// Filter options
                          Visibility(
                            visible: !_isRecording && !_showDelayText,
                            child: Container(
                              padding: EdgeInsets.only(
                                right: ScreenUtil().setWidth(20.0),
                                top: ScreenUtil().setHeight(15.0),
                              ),
                              child: _filtersList(),
                            ),
                          )
                        ],
                      ),
                    ],
                  ),
                ),

                Center(
                  child: AnimatedBuilder(
                    animation: _textAnimation,
                    builder: (context, widget) {
                      return Visibility(
                        visible: _showDelayText,
                        child: Text(
                          _delayText,
                          style: Styles.textStyle(
                              color: ColorAssets.themeColorWhite,
                              fontWeight: FontWeight.w600,
                              fontSize: _textAnimation.value),
                        ),
                      );
                    },
                  ),
                ),

                Visibility(
                  visible: isMergingInProgress,
                  child: Center(
                    child: CommonWidgets().progressLoading(message: ''),
                  ),
                ),
                Visibility(
                  visible: !showEffectList,
                  child: Align(
                    alignment: Alignment.bottomCenter,
                    child: Stack(alignment: Alignment.bottomCenter, children: [
                      // To set the gradient background
                      Container(
                        height: ScreenUtil().setHeight(160.0),
                        width: double.infinity,
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [
                              Color.fromRGBO(6, 8, 17, 0.72),
                              Color.fromRGBO(41, 46, 70, 0),
                            ],
                            begin: Alignment.bottomCenter,
                            end: Alignment.topCenter,
                          ),
                        ),
                      ),

                      Column(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Visibility(
                            visible: _isSpeedOptionsVisible && (!_showDelayText && !_isRecording),
                            child: Container(
                              height: ScreenUtil().setHeight(50.0),
                              width: ScreenUtil().setWidth(300.0),
                              margin: EdgeInsets.symmetric(
                                horizontal: ScreenUtil().setWidth(30.0),
                              ),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.all(Radius.circular(20)),
                                color: Colors.black.withOpacity(0.4),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                                children: [
                                  InkWell(
                                    onTap: () => setState(() {
                                      _videoSpeed = 0.3;
                                      isPlayBackSpeedChanged = true;
                                    }),
                                    child:
                                        _videoSpeed == 0.3 ? _whiteSpeedSelector('0.3x') : _normalSpeedSelector('0.3x'),
                                  ),
                                  InkWell(
                                    onTap: () => setState(() {
                                      _videoSpeed = 0.5;
                                      isPlayBackSpeedChanged = true;
                                    }),
                                    child:
                                        _videoSpeed == 0.5 ? _whiteSpeedSelector('0.5x') : _normalSpeedSelector('0.5x'),
                                  ),
                                  InkWell(
                                    onTap: () => setState(() {
                                      _videoSpeed = 1;
                                      isPlayBackSpeedChanged = false;
                                    }),
                                    child: _videoSpeed == 1 ? _whiteSpeedSelector('1x') : _normalSpeedSelector('1x'),
                                  ),
                                  InkWell(
                                    onTap: () => setState(() {
                                      _videoSpeed = 2;
                                      isPlayBackSpeedChanged = true;
                                    }),
                                    child: _videoSpeed == 2 ? _whiteSpeedSelector('2x') : _normalSpeedSelector('2x'),
                                  ),
                                  InkWell(
                                    onTap: () => setState(() {
                                      _videoSpeed = 3;
                                      isPlayBackSpeedChanged = true;
                                    }),
                                    child: _videoSpeed == 3 ? _whiteSpeedSelector('3x') : _normalSpeedSelector('3x'),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          SizedBox(
                            height: ScreenUtil().setHeight(20.0),
                          ),
                          Container(
                            width: double.infinity,
                            child: Stack(
                              alignment: Alignment.center,
                              children: [
                                // Record button
                                IgnorePointer(
                                  ignoring: _showDelayText,
                                  child: InkWell(
                                    onDoubleTap: () {},
                                    onTap: () {
                                      if (_isRecording) {
                                        // Stop recording
                                        stopRecording();
                                      } else {
                                        int _delaySec = getDelay(_delay);
                                        if (_listVideoModel.isNotEmpty &&
                                            _listVideoModel.last.duration == _totalRecordingDuration) {
                                          // showDialog(
                                          //     context: context,
                                          //     barrierDismissible: false,
                                          //     builder: (BuildContext context) => CommonWidgets().customDialog(
                                          //           context: context,
                                          //           message: StringAssets.maximumRecordingLimitDialogTitle,
                                          //           description: StringAssets.maximumRecordingLimit,
                                          //           button1Text: StringAssets.recordingCloseAlertStartOver,
                                          //           button2Text: StringAssets.next,
                                          //           onButton1Click: () {
                                          //             _statOverRecording();
                                          //           },
                                          //           onButton2Click: () {
                                          //             _nextToRecording(context);
                                          //           },
                                          //           onButton3Click: () {
                                          //             Navigator.of(context).pop(true);
                                          //           },
                                          //         ));
                                        } else {
                                          if (_delaySec == 0) {
                                            startRecording();
                                          } else {
                                            setState(() {
                                              _showDelayText = true;
                                              _textAnimationController.forward();
                                              // Future.delayed(
                                              //     Duration(microseconds: 10), () {
                                              //   _textAnimationController.forward();
                                              // });
                                              _delayText = _delaySec.toString();
                                            });
                                            _delayTimer = Timer.periodic(Duration(seconds: 1), (timer) {
                                              setState(() {
                                                _delayText = (_delaySec - timer.tick).toString();
                                              });
                                              if (timer.tick == _delaySec) {
                                                setState(() {
                                                  timer.cancel();
                                                  _delayTimer?.cancel();
                                                  _delayText = "";
                                                  _showDelayText = false;
                                                  _textAnimationController.stop();
                                                });
                                                startRecording();
                                              }
                                            });
                                          }
                                        }
                                      }
                                    },
                                    child: Stack(
                                      alignment: Alignment.center,
                                      children: [
                                        Container(
                                          width: ScreenUtil().setWidth(90.0),
                                          height: ScreenUtil().setWidth(90.0),
                                          padding: EdgeInsets.symmetric(
                                              vertical: ScreenUtil().setHeight(10.0),
                                              horizontal: ScreenUtil().setHeight(10.0)),
                                          decoration: BoxDecoration(
                                            border: Border.all(
                                              width: _isRecording
                                                  ? 5 * _recordingButtonAnimationController.value + 3
                                                  : ScreenUtil().setHeight(6.0),
                                              color: ColorAssets.themeColorRedLight,
                                              style: BorderStyle.solid,
                                            ),
                                            shape: BoxShape.circle,
                                          ),
                                        ),
                                        Container(
                                          width:
                                              _isRecording ? ScreenUtil().setWidth(32.0) : ScreenUtil().setWidth(80.0),
                                          height: _isRecording
                                              ? ScreenUtil().setHeight(32.0)
                                              : ScreenUtil().setHeight(80.0),
                                          decoration: BoxDecoration(
                                            borderRadius: _isRecording
                                                ? BorderRadius.all(
                                                    Radius.circular(5.0),
                                                  )
                                                : null,
                                            color: ColorAssets.themeColorRed,
                                            shape: _isRecording ? BoxShape.rectangle : BoxShape.circle,
                                          ),
                                        )
                                      ],
                                    ),
                                  ),
                                ),

                                SizedBox(
                                  width: ScreenUtil().setWidth(24.0),
                                ),

                                // Delete last clip button
                                Positioned(
                                  left: ScreenUtil().setWidth(260.0),
                                  child: Row(
                                    mainAxisAlignment: MainAxisAlignment.end,
                                    children: [
                                      Visibility(
                                        visible: _listVideoModel != null &&
                                            _listVideoModel.isNotEmpty &&
                                            !_isRecording &&
                                            !_showDelayText,
                                        child: InkWell(
                                          onTap: () => showDeleteLastClipDialog(),
                                          child: Image.asset(
                                            ImageAssets.backspaceImage,
                                            height: ScreenUtil().setHeight(36.0),
                                          ),
                                        ),
                                      ),

                                      SizedBox(
                                        width: ScreenUtil().setWidth(24.0),
                                      ),

                                      // Next button
                                      Visibility(
                                        visible: _listVideoModel != null &&
                                            _listVideoModel.length > 0 &&
                                            (!_isRecording && !_showDelayText) &&
                                            (_listVideoModel.last.duration) >= 5,
                                        child: Container(
                                          child: InkWell(
                                            onTap: () => _nextToRecording(context),
                                            child: SvgPicture.asset(
                                              ImageAssets.recordingNextButton,
                                              height: ScreenUtil().setHeight(30.0),
                                            ),
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ),
                          SizedBox(
                            height: ScreenUtil().setHeight(30.0),
                          )
                          // Record options
                        ],
                      ),
                    ]),
/*
                Align(
                  alignment: Alignment.bottomCenter,
                  child: Stack(alignment: Alignment.center, children: [
                    // To set the gradient background
                    Container(
                      height: Constant.size120,
                      width: double.infinity,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [
                            Color.fromRGBO(6, 8, 17, 0.72),
                            Color.fromRGBO(41, 46, 70, 0),
                          ],
                          begin: Alignment.bottomCenter,
                          end: Alignment.topCenter,
                        ),
                      ),
                    ),

                    // Record button
                    IgnorePointer(
                      ignoring: _showDelayText,
                      child: InkWell(
                        onTap: () {
                          if (_isRecording) {
                            // Stop recording
                            stopRecording();
                          } else {
                            int _delaySec = getDelay(_delay);
                            if (_listVideoModel.isNotEmpty &&
                                _listVideoModel.last.duration == 15) {
                              showDialog(
                                  context: context,
                                  barrierDismissible: false,
                                  builder: (BuildContext context) =>
                                      CommonWidgets().customDialog(
                                        message: StringAssets.blank,
                                        description:
                                        StringAssets.maximumRecordingLimit,
                                        button1Text: StringAssets
                                            .recordingCloseAlertStartOver,
                                        button2Text: StringAssets.next,
                                        onButton1Click: () {
                                          _statOverRecording();
                                        },
                                        onButton2Click: () {
                                          _nextToRecording(context);
                                        },
                                        onButton3Click: () {
                                          Navigator.of(context).pop(true);
                                        },
                                      ));
                            } else {
                              if (_delaySec == 0) {
                                startRecording();
                              } else {
                                setState(() {
                                  _showDelayText = true;
                                  Future.delayed(Duration(microseconds: 10),
                                          () {
                                        _textAnimationController.forward();
                                      });
                                  _delayText = _delaySec.toString();
                                });
                                _delayTimer = Timer.periodic(
                                    Duration(seconds: 1), (timer) {
                                  _textAnimationController
                                      .reverse()
                                      .then((value) {
                                    _textAnimationController.forward();
                                  });
                                  setState(() {
                                    _delayText =
                                        (_delaySec - timer.tick).toString();
                                  });
                                  if (timer.tick == _delaySec) {
                                    setState(() {
                                      timer.cancel();
                                      _delayTimer?.cancel();
                                      _showDelayText = false;
                                      _textAnimationController.stop();
                                    });
                                    Future.delayed(Duration(microseconds: 10),
                                            () {
                                          startRecording();
                                        });
                                  }
                                });
                              }
                            }
                          }
                        },
                        child: Stack(
                          alignment: Alignment.center,
                          children: [
                            AnimatedBuilder(
                              animation: _recordingButtonAnimation,
                              builder: (BuildContext context, Widget child) {
                                return Container(
                                  width: Constant.size72,
                                  height: Constant.size72,
                                  padding: EdgeInsets.all(Constant.size10),
                                  decoration: BoxDecoration(
                                      color: _isRecording
                                          ? _recordingButtonAnimation.value
                                          : ColorAssets.themeColorRedLight,
                                      shape: BoxShape.circle),
                                );
                              },
                            ),
                            Container(
                              width: Constant.size50,
                              height: Constant.size50,
                              decoration: BoxDecoration(
                                  color: ColorAssets.themeColorRed,
                                  shape: BoxShape.circle),
                            )
                          ],
                        ),
                      ),
                    ),

                    // Flip camera button
                    Visibility(
                      visible: !_isRecording,
                      child: Container(
                        margin: EdgeInsets.only(left: Constant.size140),
                        child: InkWell(
                          onTap: () => _onSwitchCamera(),
                          child: SvgPicture.asset(
                            ImageAssets.videoRecordingCameraFlip,
                            color: ColorAssets.themeColorWhite,
                          ),
                        ),
                      ),
                    ),

                    // Next button
                    Visibility(
                      visible: _listVideoModel != null &&
                          _listVideoModel.isNotEmpty &&
                          !_isRecording,
                      child: Container(
                        margin: EdgeInsets.only(left: Constant.size300),
                        child: InkWell(
                          onTap: () => _nextToRecording(context),
                          child: Container(
                            decoration: BoxDecoration(
                              color: ColorAssets.themeColorMagenta,
                              borderRadius: BorderRadius.circular(9.0),
                            ),
                            padding: EdgeInsets.symmetric(
                              vertical: Constant.size14,
                              horizontal: Constant.size28,
                            ),
                            child: Text(
                              StringAssets.next,
                              style: Styles.buttonTextStyle,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ]),
                )
*/
                  ),
                ),

                /// AR effects list view
                Container(
                    alignment: Alignment.bottomCenter,
                    padding: EdgeInsets.symmetric(vertical: ScreenUtil().setHeight(24)),
                    child: _buildFilterSelector())
              ],
            ),
          ),
        ));
  }

  /// To check the availability of flash...
  _changeFlash() {
    if (_flash == Flash.FLASH_OFF) {
      setState(() {
        _flash = Flash.FLASH_ON;
        if (Platform.isIOS && Constants.IS_BANUBA_ON) {
          _engine.setCameraTorchOn(true);
        } else {
          _cameraController.setFlashMode(FlashMode.torch);
        }
      });
    } else {
      setState(() {
        _flash = Flash.FLASH_OFF;
        if (Platform.isIOS && Constants.IS_BANUBA_ON) {
          _engine.setCameraTorchOn(false);
        } else {
          _cameraController.setFlashMode(FlashMode.off);
        }
      });
    }
  }

  /// Check the availability of camera...
  _checkCameraAvailability() {
    availableCameras().then((availableCameras) async {
      cameras = availableCameras;
      if (cameras.length > 0) {
        setState(() {
          _camera = Camera.FRONT_CAMERA;
          _flash = Flash.FLASH_OFF;
        });
        // int selectedCamera = PreferenceHelper.getInt(PrefUtils.SELECTED_CAMERA, 1);
        int selectedCamera = 1;
        if (Platform.isIOS && Constants.IS_BANUBA_ON) {
          initializeBanuba();
        } else {
          if (selectedCamera == 0 || selectedCamera == 1) {
            _initCameraController(Camera.FRONT_CAMERA).then((void v) {
              _camera = Camera.FRONT_CAMERA;
              loadCamera(_camera);
            });
          } else {
            _initCameraController(Camera.BACK_CAMERA).then((void v) {
              _camera = Camera.BACK_CAMERA;
              loadCamera(_camera);
            });
          }
        }
      } else {
        //TODO: Show alert for no camera
        printLog("No camera available");
      }
    }).catchError((err) {
      printLog('Error: $err.code\nError Message: $err.message');
    });
  }

  /// To show the camera preview
  Widget _cameraPreviewWidget() {
    // If the controller not initialised or permission not given
    if (_cameraController == null || !_cameraController.value.isInitialized) {
      return Container();
    } else {
      var camera = _cameraController.value;
      // fetch screen size
      final size = MediaQuery.of(context).size;
      // calculate scale depending on screen and camera ratios
      // this is actually size.aspectRatio / (1 / camera.aspectRatio)
      // because camera preview size is received as landscape
      // but we're calculating for portrait orientation
      var scale = size.aspectRatio * camera.aspectRatio;
      // to prevent scaling down, invert the value
      if (scale < 1) scale = 1 / scale;
      return _showPreview
          ? Positioned(
              height: size.height,
              child: CameraPreview(_cameraController),
            )
          : Container();
    }
  }

  /// Filters list...
  Widget _filtersList() {
    return Column(
      children: [
        /// Flip camera option
        _filterOptionPNG(
            // Check if the device have flash...
            ImageAssets.videoRecordingCameraFlipPNG,
            StringAssets.flip,
            () => _onSwitchCamera()),

        Visibility(
          visible: _camera != Camera.FRONT_CAMERA,
          child: SizedBox(
            height: ScreenUtil().setHeight(15.0),
          ),
        ),

        /// Flash option
        Visibility(
          visible: _camera != Camera.FRONT_CAMERA,
          child: _filterOptionPNG(
              // Check if the device have flash...
              _flash != Flash.FLASH_OFF ? ImageAssets.videoRecordingFlashOnPNG : ImageAssets.videoRecordingFlashOffPNG,
              StringAssets.flash, () {
            setState(() {
              // If the device have flash
              print("Flash::: $_flash Camera:: $_camera");
              if (_camera != Camera.FRONT_CAMERA) {
                // If flash is off and not front camera
                _changeFlash();
              }
            });
          }),
        ),

        SizedBox(
          height: ScreenUtil().setHeight(15.0),
        ),

        // /// Color filters
        // _filterOptionPNG(ImageAssets.videoRecordingColorFilterPNG,
        //     StringAssets.filter, () {}),
        //
        // SizedBox(
        //   height: ScreenUtil().setHeight(10.0),
        // ),

        /// Speed option
        _filterOptionPNG(
            _isSpeedOptionsVisible ? ImageAssets.videoRecordingSpeedOnPNG : ImageAssets.videoRecordingSpeedOffPNG,
            StringAssets.speed, () {
          setState(() {
            _isSpeedOptionsVisible = !_isSpeedOptionsVisible;
          });
        }),

        SizedBox(
          height: ScreenUtil().setHeight(15.0),
        ),

        /// Timer
        _filterOptionPNG(
            _delay == Delay.INSTANT
                ? ImageAssets.videoRecordingCameraTimerInstantPNG
                : _delay == Delay.THREE_SEC
                    ? ImageAssets.videoRecordingCameraTimerThreePNG
                    : ImageAssets.videoRecordingCameraTimerTenPNG,
            StringAssets.timer, () {
          setState(() {
            _delay = setDelay(_delay);
          });
        }),

        SizedBox(
          height: ScreenUtil().setHeight(15.0),
        ),

        /// Beauty effects
        Visibility(
          visible: Platform.isIOS && Constants.IS_BANUBA_ON,
          child: _filterOptionPNG(ImageAssets.videoRecordingBeautyEffectsPNG, StringAssets.effects, () {
            setState(() {
              showEffectList = !showEffectList;
            });
          }),
        ),
      ],
    );
  }

  ///Normal Speed Selector when selected speed
  Widget _normalSpeedSelector(String speed) {
    return Container(
      height: ScreenUtil().setHeight(50.0),
      width: ScreenUtil().setWidth(60.0),
      child: Center(
          child: Text(
        speed,
        style: TextStyle(fontSize: FontSize.s14, color: ColorAssets.themeColorWhite),
      )),
    );
  }

  ///White Speed Selector when selected speed
  Widget _whiteSpeedSelector(String speed) {
    //leftmost container
    if (speed == '0.3x') {
      return Container(
        height: ScreenUtil().setHeight(50.0),
        width: ScreenUtil().setWidth(60.0),
        decoration: BoxDecoration(
          color: ColorAssets.themeColorWhite,
          borderRadius: BorderRadius.only(topLeft: Radius.circular(20), bottomLeft: Radius.circular(20)),
        ),
        child: Center(
            child: Text(
          speed,
          style: TextStyle(fontSize: FontSize.s14, color: ColorAssets.themeColorMagenta),
        )),
      );
    }
    //rightmost container
    else if (speed == '3x') {
      return Container(
        height: ScreenUtil().setHeight(50.0),
        width: ScreenUtil().setWidth(60.0),
        decoration: BoxDecoration(
          color: ColorAssets.themeColorWhite,
          borderRadius: BorderRadius.only(topRight: Radius.circular(20), bottomRight: Radius.circular(20)),
        ),
        child: Center(
            child: Text(
          speed,
          style: TextStyle(fontSize: FontSize.s14, color: ColorAssets.themeColorMagenta),
        )),
      );
    }
    //other one's
    else {
      return Container(
        height: ScreenUtil().setHeight(50.0),
        width: ScreenUtil().setWidth(60.0),
        decoration: BoxDecoration(
          color: ColorAssets.themeColorWhite,
        ),
        child: Center(
            child: Text(
          speed,
          style: TextStyle(fontSize: FontSize.s14, color: ColorAssets.themeColorMagenta),
        )),
      );
    }
  }

  /// Single filter button in png...
  Widget _filterOptionPNG(String image, String text, Function onTap) {
    return InkWell(
      onTap: onTap,
      child: Column(
        children: [
          Image.asset(
            image,
            height: ScreenUtil().setWidth(32.0),
            width: ScreenUtil().setWidth(32.0),
          ),
          Text(
            text,
            style: TextStyle(
              color: ColorAssets.themeColorWhite,
              // fontSize: ScreenUtil().setSp(14.0),
              fontSize: 14.0,
              fontFamily: StringAssets.fontFamilyPoppins,
              fontWeight: FontWeight.normal,
              shadows: [
                Shadow(
                  offset: Offset(0.0, 0.0),
                  blurRadius: 3.0,
                  color: ColorAssets.themeColorBlack.withOpacity(0.5),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Create controller for camera
  Future _initCameraController(Camera camera) async {
    if (_cameraController != null) {
      setState(() {
        _showPreview = false;
      });
      await _cameraController.dispose();
    }

    CameraDescription cameraDescription = camera == Camera.FRONT_CAMERA ? cameras[1] : cameras[0];

    _cameraController =
        CameraController(cameraDescription, ResolutionPreset.high, enableAudio: _audio == Audio.SOUND_ON);
    _cameraController.addListener(() {
      if (mounted) {
        setState(() {});
      }

      if (_cameraController.value.hasError) {
        printLog('Camera error ${_cameraController.value.errorDescription}');
      }
    });

    try {
      await _cameraController.initialize();
      setState(() {
        _showPreview = true;
      });
    } on CameraException catch (e) {
      printLog("init error $e");
    }
    if (mounted) {
      setState(() {});
    }
  }

  /// To switch cameras front and back
  void _onSwitchCamera() {
    if (_camera == Camera.FRONT_CAMERA) {
      setState(() {
        _camera = Camera.BACK_CAMERA;
        // PreferenceHelper.setInt(PrefUtils.SELECTED_CAMERA, 2);
      });
    } else {
      setState(() {
        _camera = Camera.FRONT_CAMERA;
      });
      // PreferenceHelper.setInt(PrefUtils.SELECTED_CAMERA, 1);
    }
    if (Platform.isIOS && Constants.IS_BANUBA_ON) {
      _engine.switchCamera();
      if (_camera == Camera.BACK_CAMERA && _flash == Flash.FLASH_ON) {
        Timer(Duration(seconds: 1), () {
          _engine.setCameraTorchOn(true);
        });
      }
    } else {
      _initCameraController(_camera);
      if (_camera == Camera.BACK_CAMERA && _flash == Flash.FLASH_ON) {
        Timer(Duration(seconds: 1), () {
          print("On Camera Change Flash Change");
          _cameraController.setFlashMode(FlashMode.torch);
        });
      }
    }
  }

  /// To start timer for maximum recording duration
  void startCountDown() {
    double _videoSpeedProgress = _totalRecordingDuration * _videoSpeed;
    //Commented because slowness in progress bar was causing the issue in duration which led to error in playback screen
    // if (_videoSpeedProgress >= 60)
    //   _progressAnimation.duration = Duration(minutes: (_videoSpeedProgress ~/ _totalRecordingDuration));
    // else
    _progressAnimation.duration = Duration(seconds: _videoSpeedProgress.toInt());
    _progressAnimation.forward();
    if (_listSnippetModel.isNotEmpty && _listSnippetModel[0].isRecorded != true) _listSnippetModel[0].isRecorded = true;
    _progressTimer = Timer.periodic(Duration(milliseconds: 55), (period) {
      setState(() {
        _videoDuration = _previousDuration + period.tick * 0.0035;
      });
    });
    _recordingTimer = Timer.periodic(Duration(seconds: 1), (timer) {
      if (_progressAnimation.value >= 1.0) {
        timer.cancel();
        _progressTimer.cancel();
        _recordingTimer?.cancel();
        stopRecording();
      } else {
        if (mounted) {
          _progressValue = (double.parse(_progressAnimation.value.toStringAsPrecision(4)) * 30).round();
          int duration = _progressValue;
          _listSnippetModel[duration].isRecorded = true;
        }
      }
    });
  }

  /// To start recording...
  Future<void> startRecording() async {
    if (!updatedRecording && mergedVideo != "") {
      updatedRecording = true;
    }
    _recordingButtonAnimationController.forward();
    setState(() {
      _isRecording = true;
      // _isSpeedOptionsVisible = false;
    });
    if (Platform.isIOS && Constants.IS_BANUBA_ON) {
      int _time = DateTime.now().millisecondsSinceEpoch;

      final Directory extDir =
          Platform.isAndroid ? await getExternalStorageDirectory() : await getApplicationDocumentsDirectory();
      final String dirPath = '${extDir.path}/Movies/FFmpeg';
      await Directory(dirPath).create(recursive: true);
      filePath = '$dirPath/soda_$_time.mp4';

      // printLog("File Path $filePath");
      _engine.startVideoRecording(filePath).then((value) {});
      // printLog("Recording started");
      startCountDown();
    } else {
      _cameraController.startVideoRecording().then((value) {
        startCountDown(); // Start timer
      });
    }
  }

  /// To stop recording...
  Future<void> stopRecording() async {
    _recordingButtonAnimationController?.stop();
    _recordingTimer?.cancel();
    _progressTimer?.cancel();
    _progressAnimation?.stop();

    try {
      if (_flash == Flash.FLASH_ON) {
        _changeFlash();
      }
      // Previous snippet duration + current snippet duration
      int duration = _progressValue;
      // To set snippet end indicator...
      if (_listSnippetModel[duration].isRecorded) {
        _listSnippetModel[duration].isSnippetEnd = true;
        setState(() {
          _previousDuration = _videoDuration;
          _previousDurationList.add(_progressAnimation.value);
          //_previousDurationList.add(_progressAnimation.value);
        });

        // Recalculate remaining time
        _remainingRecordingDuration = _totalRecordingDuration - duration;

        // Indicate next starting point of snippet...
        _startSnippetPosition = _progressValue;

        // This is stored into cache, copy in device's folder
        File us;
        if (Platform.isIOS && Constants.IS_BANUBA_ON) {
          _engine.stopVideoRecording();
          setState(() {
            _isRecording = false;
            _showDelayText = true;
          });
        } else {
          var tempFile = await _cameraController.stopVideoRecording();
          setState(() {
            _isRecording = false;
            _showDelayText = true;
          });
          us = File(tempFile.path);
        }
        int _time = DateTime.now().millisecondsSinceEpoch;
        final Directory extDir =
            Platform.isAndroid ? await getExternalStorageDirectory() : await getApplicationDocumentsDirectory();
        final String dirPath = '${extDir.path}/Movies/FFmpeg';
        await Directory(dirPath).create(recursive: true);
        if (Platform.isAndroid || !Constants.IS_BANUBA_ON) {
          filePath = '$dirPath/soda_$_time.mp4';
        }
        final String localOutPutPath = '$dirPath/sodaOutput_$_time.mp4';
        if (Platform.isAndroid || !Constants.IS_BANUBA_ON) {
          File outFile = us.copySync(filePath);
          print('file exists::${outFile.exists()}');
        }
        // print('localoutputpath::$localOutPutPath');

        if (isPlayBackSpeedChanged) {
          ///if platform is ios the code is with the Hardware Acceleration
          if (Platform.isIOS) {
            if (_videoSpeed == 0.3) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-y -i '${filePath}' -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -filter:v setpts=3.33*PTS -q:v 0 -q:a 0 -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else if (_videoSpeed == 0.5) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-y -i '${filePath}' -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -filter:v setpts=2.0*PTS -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else if (_videoSpeed == 2) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-i '${filePath}' -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -filter_complex [0:v]setpts=0.5*PTS[v];[0:a]atempo=2[a] -map [v] -map [a] -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else if (_videoSpeed == 3) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-i '${filePath}' -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -filter_complex [0:v]setpts=0.33*PTS[v];[0:a]atempo=3[a] -map [v] -map [a] -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-i '${filePath}' -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -preset ultrafast -filter_complex setpts=1.0*PTS -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      print('flag insert');
                      _showDelayText = false;
                    }),
                  );
            }
          } else {
            ///if platform is android the code is with the normal config
            if (_videoSpeed == 0.3) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-y -i '${filePath}' -vcodec libx264 -crf 20 -preset ultrafast -filter:v setpts=3.33*PTS -q:v 0 -q:a 0 -c:v libx264 -preset ultrafast -threads 2 -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else if (_videoSpeed == 0.5) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-y -i '${filePath}' -vcodec libx264 -crf 20 -preset ultrafast -threads 2 -filter:v setpts=2.0*PTS  -c:v libx264 -preset ultrafast -threads 2 -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else if (_videoSpeed == 2) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-y -i '${filePath}' -vcodec libx264 -crf 20 -preset ultrafast -threads 2 -filter_complex [0:v]setpts=0.5*PTS[v];[0:a]atempo=2[a] -map [v] -map [a] -c:v libx264 -preset ultrafast -threads 2 -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else if (_videoSpeed == 3) {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-y -i '${filePath}' -vcodec libx264 -crf 20 -preset ultrafast -threads 2 -filter_complex [0:v]setpts=0.33*PTS[v];[0:a]atempo=3[a] -map [v] -map [a]  -c:v libx264 -preset ultrafast -threads 2 -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      _showDelayText = false;
                    }),
                  );
            } else {
              // setState(() {
              //   _showDelayText = true;
              // });
              await _flutterFFmpeg
                  .execute(
                      "-i '${filePath}' -vcodec libx264 -crf 20 -preset ultrafast -threads 0 -filter_complex setpts=1.0*PTS  -c:v libx264 -preset ultrafast -threads 0 -r 30 '$localOutPutPath'")
                  .then(
                    (value) => setState(() {
                      print('flag insert');
                      _showDelayText = false;
                    }),
                  );
            }
          }
        } else {
          setState(() {
            _showDelayText = false;
          });
        }
        // Adding snippet to model class
        VideoModel videoModel;
        setState(() {
          videoModel = new VideoModel(
              thumbList: null,
              duration: duration,
              startDuration: _listVideoModel.length == 0 ? 0 : _listVideoModel.last.duration,
              snippetDuration: _progressValue-(_listVideoModel.length == 0 ? 0 : _listVideoModel.last.duration),
              videoPath: isPlayBackSpeedChanged ? localOutPutPath : filePath);
          _listVideoModel.add(videoModel);
          if (updatedRecording) {
            // if user will add new video to existing clips then extra list for that videos will be created
            _newlyRecordedVideos.add(videoModel);
          }
        });
        // List<Uint8List> videoThumb =
        // await generateThumbnail(duration, isPlayBackSpeedChanged ? localOutPutPath : filePath);
        // _listVideoModel[_listVideoModel.length-1].thumbList = videoThumb;
        // _listVideoModel.firstWhere((element) => element.thumbList == videoThumb);
        // if (updatedRecording) {
        //   // if user will add new video to existing clips then extra list for that videos will be created
        //   videoModel.thumbList = videoThumb;
        //   _newlyRecordedVideos.add(videoModel);
        // }

        printLog("newly updated list length : ${_newlyRecordedVideos.length}");

        /// TODO: implement isRecordingVideo() method
      } else if ((Platform.isIOS && Constants.IS_BANUBA_ON)
          ? /*await _engine.isRecodingVideo()*/ _isRecording
          : _cameraController.value.isRecordingVideo) {
        (Platform.isIOS && Constants.IS_BANUBA_ON)
            ? await _engine.stopVideoRecording()
            : _cameraController.stopVideoRecording();
        setState(() {
          _isRecording = false;
        });
      }
      if ((_listVideoModel.last.duration) < 5) {
        showToast(StringAssets.minVideoDuration);
      }
      setState(() {
        _showDelayText = false;
      });
    } catch (e) {
      printLog(e);
      setState(() {
        _isRecording = false;
        _showDelayText = false;
      });
    }
  }

  /// Generate and store Video Thumbs when USer stops video recording
  Future<List<Uint8List>> generateThumbnail(int duration, String _videoPath) async {
    double _eachPart = duration / 10;

    List<Uint8List> _byteList = [];
    for (int i = 1; i <= 10; i++) {
      Uint8List _bytes;
      _bytes = await VideoThumbnail.thumbnailData(
        video: _videoPath,
        timeMs: (_eachPart * i).toInt(),
        quality: 40,
      );
      // Future.delayed(Duration(microseconds: 10));
      print("Video Thumb $i === $_eachPart === ${_bytes != null}");
      if (_bytes != null) _byteList.add(_bytes);
      print("val of bytes is ");
    }
    return _byteList;
  }

  /// Start Over Recording when Start over Option clicked
  // Future<void> _statOverRecording() async {
  //   await DatabaseHelper().deleteDeleteUnDraftData();
  //   Navigator.pop(context, true);
  //
  //   /// TODO: implement isRecordingVideo() method
  //   if (Platform.isIOS && Constants.IS_BANUBA_ON) {
  //     /*if (await _engine.isRecodingVideo()) {
  //       await _engine.stopVideoRecording();
  //     }*/
  //     if (_isRecording) {
  //       await _engine.stopVideoRecording();
  //     }
  //   } else {
  //     if (_cameraController.value.isRecordingVideo) {
  //       await _cameraController?.stopVideoRecording();
  //     }
  //   }
  //   setState(() {
  //     _isRecording = false;
  //     _showDelayText = false;
  //     _startSnippetPosition = 0;
  //     _listSnippetModel.clear();
  //     _listVideoModel.clear();
  //     _recordingTimer?.cancel();
  //     _previousDurationList.clear();
  //     _videoDuration = 0.0;
  //     _previousDuration = 0.0;
  //     _progressAnimation.reset();
  //     scaledPath.clear();
  //     _newlyRecordedVideos.clear();
  //     updatedRecording = false;
  //     mergedVideo = "";
  //     _remainingRecordingDuration = _totalRecordingDuration;
  //   });
  //   _createProgressBar();
  // }

  /// Create list of seconds...
  void _createProgressBar() {
    SingleSnippetModel snippetModel;
    for (int loop = _startSnippetPosition; loop <= _totalRecordingDuration; loop++) {
      snippetModel = new SingleSnippetModel(loop, false, false);
      _listSnippetModel.add(snippetModel);
    }
  }

  _nextToRecording(BuildContext context) async {
    printLog("path: ${_listVideoModel.last.videoPath}");

    ///On Next Option clicked Turn off the flash if its ON while recording
    if (_flash == Flash.FLASH_ON) _changeFlash();

    if (_listVideoModel.length == 1 || (!updatedRecording && mergedVideo != "")) {
      // if no need to merge (no extra changes done in already recorded clips) or only one video is recorded this block will be executed
      // _cameraController.dispose();
      updatedRecording = false;
      _newlyRecordedVideos.clear();
      if (Platform.isIOS && Constants.IS_BANUBA_ON) {
        printLog("leave");
        _engine.cameraPauseStop(true);
        // _engine.destroyBanubaCamera();
        // _engine = null;
      } else {
        // _cameraController.dispose();
      }
      // Navigator.push(
      //     context,
      //     CupertinoPageRoute(
      //         builder: (context) => VideoPlaybackScreen(
      //               outputVideoPath:
      //                   (!updatedRecording && mergedVideo != "") ? mergedVideo : _listVideoModel.last.videoPath,
      //               totalVideoList: _listVideoModel,
      //               onBack: () {
      //                 printLog("play");
      //                 if (Platform.isIOS && Constants.IS_BANUBA_ON) {
      //                   _engine.cameraPauseStop(false);
      //                 }
      //               },
      //             ))).then((value) {
      //   if (Platform.isAndroid || !Constants.IS_BANUBA_ON) _checkCameraAvailability();
      // });
    } else {
      createScaledVideoList();
    }
  }

  createScaledVideoList() async {
    setState(() {
      flagForCircle = true;
      isMergingInProgress = true;
    });
    String status;
    final Directory extDir =
        Platform.isAndroid ? await getExternalStorageDirectory() : await getApplicationDocumentsDirectory();
    final String dirPath = '${extDir.path}';
    final FlutterFFmpeg _flutterFFmpeg = new FlutterFFmpeg();
    String cmdForMerging, localCommand, localOutPutPath;
    scaledPath.clear();

    ///The command of merging will be ran without FPS if the process is unstable then we will again add FPS command
    // if (updatedRecording && mergedVideo != "") {
    //   VideoModel videoModel =
    //       VideoModel(duration: 0, snippetDuration: 0, videoPath: mergedVideo);
    //   scaledPath.add(videoModel);
    //   for (int i = 0; i < _newlyRecordedVideos.length; i++) {
    //     localOutPutPath =
    //         "$dirPath/${DateTime.now().millisecondsSinceEpoch}" + "$i" + ".mp4";
    //     printLog("local otput path $localOutPutPath");
    //     printLog(
    //         " element . video path is ${_newlyRecordedVideos[i].videoPath}");
    //     ///Hardware accelerated command
    //     if(Platform.isIOS){
    //       localCommand =
    //       "-y -i '${_newlyRecordedVideos[i].videoPath}' -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -filter:v fps=fps=30 -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -r 30 '$localOutPutPath'";
    //     }
    //     else{
    //       localCommand =
    //       "-y -i '${_newlyRecordedVideos[i].videoPath}' -vcodec libx264 -crf 18 -preset superfast -threads 2 -filter:v fps=fps=30 -c:v libx264 -preset superfast -threads 2 -r 30 '$localOutPutPath'";
    //     }
    //     await _flutterFFmpeg.execute(localCommand).then((value) async {
    //       setState(() {
    //         VideoModel videoModel = VideoModel(
    //             duration: 0, snippetDuration: 0, videoPath: localOutPutPath);
    //         scaledPath.add(videoModel);
    //         printLog(" path ${videoModel.videoPath}");
    //       });
    //       printLog(" length is ${scaledPath.length}");
    //     });
    //   }
    // } else {
    //   for (int i = 0; i < _listVideoModel.length; i++) {
    //     //i++;
    //     localOutPutPath =
    //         "$dirPath/${DateTime.now().millisecondsSinceEpoch}" + "$i" + ".mp4";
    //     printLog("local otput path $localOutPutPath");
    //     printLog(" element . video path is ${_listVideoModel[i].videoPath}");
    //     ///Hardware accelerated command
    //     if(Platform.isIOS){
    //       localCommand =
    //       "-y -i '${_listVideoModel[i].videoPath}' -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -filter:v fps=fps=30 -c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -r 30 '$localOutPutPath'";
    //     }
    //     else{
    //       localCommand =
    //       "-y -i '${_listVideoModel[i].videoPath}' -vcodec libx264 -crf 18 -preset superfast -threads 2 -filter:v fps=fps=30 -c:v libx264 -preset superfast -threads 2 -r 30 '$localOutPutPath'";
    //       printLog(" lecal command is $localCommand}");
    //     }
    //     await _flutterFFmpeg.execute(localCommand).then((value) async {
    //       setState(() {
    //         VideoModel videoModel = VideoModel(
    //             duration: 0, snippetDuration: 0, videoPath: localOutPutPath);
    //         scaledPath.add(videoModel);
    //         printLog(" path ${videoModel.videoPath}");
    //       });
    //       printLog(" length is ${scaledPath.length}");
    //     });
    //   }
    // }
    // scaledPath.forEach((element) {
    //   printLog(" scaled Path ${element.videoPath}");
    //   printLog(" scaled Path length  ${scaledPath.length}");
    // });
    List<String> commandList = await mergeVideoLoops(_listVideoModel);
    // List<String> commandList = await mergeVideo(_listVideoModel);
    cmdForMerging = commandList[0];
    printLog("cmdForMerging : " + cmdForMerging);
    String outputPath = commandList[1];
    printLog("output path : " + outputPath);
    printLog(cmdForMerging);
    _flutterFFmpeg.execute(cmdForMerging).then((rc) {
      printLog("FFMPEG process exited with rc $rc");
      if (rc == 0) {
        setState(() {
          isMergingInProgress = false;
        });
        status = "Success";
        setState(() {
          flagForCircle = false;
        });
        showToast("Saving Successful");
        mergedVideo = outputPath;
        updatedRecording = false;
        _newlyRecordedVideos.clear();
        if (Platform.isIOS && Constants.IS_BANUBA_ON) {
          printLog("leave");
          _engine.cameraPauseStop(true);
          // _engine.destroyBanubaCamera();
          // _engine = null;
        } else {
          //_cameraController.dispose();
        }

        // Navigator.of(context)
        //     .push(
        //   CupertinoPageRoute(
        //       builder: (context) => VideoPlaybackScreen(
        //             outputVideoPath: outputPath,
        //             totalVideoList: _listVideoModel,
        //             onBack: () {
        //               if (Platform.isIOS && Constants.IS_BANUBA_ON) {
        //                 _engine.cameraPauseStop(false);
        //               }
        //             },
        //           )),
        // )
        //     .then((value) {
        //   // _checkCameraAvailability();
        // });
      } else {
        setState(() {
          flagForCircle = false;
          isMergingInProgress = false;
        });
        status = "Error";
        showToast("error occurred");
      }
      setState(() {
        //filePaths.clear();
      });
    });
    printLog("Scaled path length : ${scaledPath.length}");
  }

  Future<List<String>> mergeVideoLoops(List<VideoModel> videoList) async {
    List<String> commandList = [];
    final Directory extDir =
        Platform.isAndroid ? await getExternalStorageDirectory() : await getApplicationDocumentsDirectory();
    final String dirPath = '${extDir.path}/Movies';
    final outputPath = '$dirPath/${DateTime.now().millisecondsSinceEpoch}_merged.mp4';

    List<String> cmdList = [];
    StringBuffer sb = StringBuffer();

    //cmdList.add("-y");
    if (Platform.isIOS) {
      for (int i = 0; i < videoList.length; i++) {
        cmdList.add("-y -i");
        cmdList.add(videoList[i].videoPath);

        i == 0 ? sb.write("'[") : sb.write("[");
        sb.write(i);
        if (videoList.length >= 2)
          for (int i = 0; i < 1; i++) {
            sb.write(":v");
          }
        sb.write(":0]");

        i == 0 ? sb.write("[") : sb.write("[");
        sb.write(i);
        if (videoList.length >= 2)
          for (int i = 0; i < 1; i++) {
            sb.write(":a");
          }
        sb.write(":0]");
      }
      sb.write(" concat=n=");
      sb.write(videoList.length.toString());
      sb.write(":v=1:a=1[outv][outa]'");
      cmdList.add("-c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0");
      cmdList.add("-filter_complex");
      cmdList.add(sb.toString());
      cmdList.add("-map");
      cmdList.add("'[outv]'");
      cmdList.add("-map");
      cmdList.add("'[outa]'");
      cmdList.add("-c:v h264_videotoolbox -b:v 2200k -b:a 220k -preset ultrafast -threads 0 -r 30");
      cmdList.add("$outputPath");
      sb = StringBuffer();
      cmdList.forEach((element) {
        sb.write(element);
        sb.write(" ");
      });
    }

    ///Normal command for the android
    else {
      for (int i = 0; i < videoList.length; i++) {
        cmdList.add("-y -i");
        cmdList.add(videoList[i].videoPath);

        i == 0 ? sb.write("'[") : sb.write("[");
        sb.write(i);
        if (videoList.length >= 2)
          for (int i = 0; i < 1; i++) {
            sb.write(":v");
          }
        sb.write(":0]");

        i == 0 ? sb.write("[") : sb.write("[");
        sb.write(i);
        if (videoList.length >= 2)
          for (int i = 0; i < 1; i++) {
            sb.write(":a");
          }
        sb.write(":0]");
      }
      sb.write(" concat=n=");
      sb.write(videoList.length.toString());
      sb.write(":v=1:a=1[outv][outa]'");
      cmdList.add("-vcodec libx264");
      cmdList.add("-crf 18");
      cmdList.add("-preset ultrafast -threads 0");
      cmdList.add("-filter_complex");
      cmdList.add(sb.toString());
      cmdList.add("-map");
      cmdList.add("'[outv]'");
      cmdList.add("-map");
      cmdList.add("'[outa]'");
      cmdList.add("-c:v libx264 -preset ultrafast -threads 0 -r 60");
      cmdList.add("$outputPath");
      sb = StringBuffer();
      cmdList.forEach((element) {
        sb.write(element);
        sb.write(" ");
      });
    }

    print(" command is $sb ");
    commandList.add(sb.toString());
    commandList.add(outputPath);
    return commandList;
  }

  void loadCamera(Camera camera) {
    // int selectedCamera = PreferenceHelper.getInt(PrefUtils.SELECTED_CAMERA, 1);
    // if (selectedCamera == 0 || selectedCamera == 1) {
    _initCameraController(camera).then((void v) {});
    // } else {
    //   _initCameraController(Camera.BACK_CAMERA).then((void v) {});
    // }
  }

  Future<void> checkPermission() async {
    var resCam = await Permission.camera.request().isGranted;
    var resMicrophone = await Permission.microphone.request().isGranted;
    if (resCam && resMicrophone) {
      _checkCameraAvailability();
      setState(() {
        _isFlashVisible = true;
      });
    } else if (await Permission.camera.request().isDenied ||
        await Permission.microphone.request().isPermanentlyDenied) {
      openAppSettings();
      // isDialogShowing = true;
      // showDialog(
      //     context: context,
      //     barrierDismissible: false,
      //     builder: (BuildContext context) => SingleButtonDialog(
      //           title: StringAssets.appName,
      //           code: "",
      //           msg: StringAssets.cameraPermissionDescription,
      //           buttonText: StringAssets.openSettings,
      //           callBack: () {
      //             Navigator.pop(context);
      //             openAppSettings();
      //             isDialogShowing = false;
      //           },
      //         ));
    }
  }

  void showDeleteLastClipDialog() {
    // showDialog(
    //     context: context,
    //     builder: (BuildContext context) => CommonWidgets().customDialog(
    //         context: context,
    //         message: StringAssets.recordingDeleteLastClipAlertTitle,
    //         description: StringAssets.recordingDeleteLastClipAlertDesc,
    //         button1Text: StringAssets.recordingDeleteLastClipAlertCancel,
    //         button2Text: StringAssets.recordingDeleteLastClipAlertDelete,
    //
    //         /// Cancel Option: Close the dialogue,
    //         onButton1Click: () {
    //           Navigator.pop(context);
    //         },
    //
    //         ///Delete option: Delete last recorded clip
    //         onButton2Click: () {
    //           setState(() {
    //             mergedVideo = "";
    //             updatedRecording = true;
    //             _previousDurationList.removeLast();
    //             if (_progressAnimation.isAnimating) {
    //               _progressAnimation?.stop();
    //             }
    //             if (scaledPath.length > 0) {
    //               scaledPath.removeLast();
    //             }
    //             _progressAnimation.value = _previousDurationList.isEmpty ? 0 : _previousDurationList.last;
    //             _listVideoModel.removeLast();
    //             if (_listVideoModel.length > 0) {
    //               _remainingRecordingDuration = _totalRecordingDuration - _listVideoModel.last.duration;
    //             } else {
    //               _remainingRecordingDuration = _totalRecordingDuration;
    //             }
    //           });
    //           Navigator.pop(context);
    //         },

            // ///Close the dialog;
            // onButton3Click: () {
            //   Navigator.pop(context, true);
            // }));
  }

  /// To show the dialog on leaving screen...
    void showCloseDialog() {
    // stopRecording();
    // showDialog(
    //     context: context,
    //     builder: (BuildContext context) => CommonWidgets().customDialog(
    //         context: context,
    //         btnKey1: "discard_recording",
    //         btnKey2: "start_over_recording",
    //         btnKey3: "close_dialog",
    //         message: StringAssets.recordingCloseAlertTitle,
    //         description: StringAssets.recordingCloseAlertDesc,
    //         button1Text: StringAssets.recordingCloseAlertDiscard,
    //         button2Text: StringAssets.recordingCloseAlertStartOver,
    //
    //         /// Discard Option Close and Close the screen,
    //         onButton1Click: () async {
    //           await DatabaseHelper().deleteDeleteUnDraftData();
    //           Navigator.pop(context);
    //           Navigator.pop(context);
    //           if (widget.updateUI == null) {
    //             Navigator.pushAndRemoveUntil(
    //                 context,
    //                 CupertinoPageRoute(builder: (BuildContext context) => BottomTabBarScreen()),
    //                 (route) => route.settings.name == StringAssets.bottomTabBarScreen);
    //           } else {
    //             Navigator.pushAndRemoveUntil(
    //                 context,
    //                 CupertinoPageRoute(builder: (BuildContext context) => BottomTabBarScreen()),
    //                 (route) => route.settings.name == StringAssets.bottomTabBarScreen);
    //           }
    //         },
    //
    //         ///Start Over Option Close Dialog and Show Video Screen
    //         onButton2Click: () {
    //           _statOverRecording();
    //         },
    //
    //         ///Close the dialog;
    //         onButton3Click: () {
    //           Navigator.pop(context, true);
    //         }));
  }

  Future<bool> _onWillPopScopeCallback() async {
    if (_listVideoModel.length >= 1)
      showCloseDialog();
    else {
      await stopRecording();
      if (widget.updateUI == null) {
        Navigator.pushAndRemoveUntil(
            context,
            CupertinoPageRoute(builder: (BuildContext context) => HomeScreen()),
            (Route<dynamic> route) => route is HomeScreen);
      } else {
        Future.delayed(Duration(milliseconds: 300), () {
          Navigator.pushAndRemoveUntil(
              context,
              CupertinoPageRoute(builder: (BuildContext context) => HomeScreen()),
              (Route<dynamic> route) => route is HomeScreen);
        });
      }
    }
    return false;
  }

  /// AR effects selector
  Widget _buildFilterSelector() {
    return Visibility(
      visible: showEffectList,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          InkWell(
            onTap: () {
              setState(() {
                showEffectList = false;
              });
            },
            child: SvgPicture.asset(ImageAssets.closeEffectIcons,
                height: ScreenUtil().setHeight(24), width: ScreenUtil().setWidth(24)),
          ),
          SizedBox(height: ScreenUtil().setHeight(2)),
          FilterSelector(
            onFilterChanged: onEffectChanged,
            filters: effectNames,
          ),
          Text(
            selectedEffect,
            style: TextStyle(
                fontSize: ScreenUtil().setSp(14),
                fontFamily: StringAssets.fontFamilyPoppins,
                fontWeight: FontWeight.w500,
                color: Colors.white),
          )
        ],
      ),
    );
  }

  List<String> effectNames = [
    "",
    "HeadphoneMusic",
    "BeautyEffectsGrayscale",
    "BGVideoBeach",
    "Emoji",
    "HeartsLut",
    "Polaroid",
    "PrideParade",
    "BeautyBokeh",
    "RainbowBeauty",
    "WhiteCat"
  ];

  /// To load selected AR effect
  void onEffectChanged(String effectName) {
    setState(() {
      selectedEffect = effectName;
    });
    _engine.onEffectSelected(effectName);
  }
}
