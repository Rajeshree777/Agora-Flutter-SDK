/*
 * Author: Rajeshree Savaliya (rajeshree.savaliya@bacancy.com)
 * Date: 22-02-21
 * Brief Description: Video Dating Screen with Agora
 * Notes:
 * - It contains agora_rtc_engine, wakelock
 */

import 'dart:async';
import 'dart:io';
import 'dart:math';
import 'dart:ui';
import 'package:agora_rtc_engine/rtc_engine.dart';
import 'package:agora_rtc_engine/rtc_local_view.dart' as RtcLocalView;
import 'package:agora_rtc_engine/rtc_remote_view.dart' as RtcRemoteView;
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/svg.dart';
import 'package:provider/provider.dart';
import 'package:soda_app/providers/video_dating_screen_state.dart';
import 'package:soda_app/source/string_assets.dart';
import 'package:soda_app/source/styles.dart';
import 'package:soda_app/utils/constants.dart';
import 'package:soda_app/widget/effect_selector_view.dart';
import 'package:wakelock/wakelock.dart';
import '../source/color_assets.dart';
import '../source/image_assets.dart';
import '../utils/api_utils.dart';
import '../utils/common_widgets.dart';
import '../utils/print_log.dart';
import '../utils/screen_util.dart';

enum UserType { CALLER, RECEIVER }

class VideoDatingScreen extends StatefulWidget {
  // non-modifiable channel name of the page
  final String channelName;

  // non-modifiable client role of the page
  final ClientRole role;

  // Connected User info
  final String me, gameId, agoraToken, selectedEffect, callerName;
  final UserType userType;
  final bool cameraOn, isMuted, isFrontCamera, isFromNotification;

  // Creates a call page with given channel name.
  const VideoDatingScreen(
      {Key key,
      this.channelName,
      this.role,
      this.me,
      this.gameId,
      this.agoraToken,
      this.userType,
      this.isMuted,
      this.isFrontCamera = true,
      this.cameraOn = true,
      this.isFromNotification,
      this.callerName,
      this.selectedEffect})
      : super(key: key);

  @override
  _VideoDatingScreenState createState() => _VideoDatingScreenState();
}

class _VideoDatingScreenState extends State<VideoDatingScreen> with TickerProviderStateMixin {
  final _users = <AgoraUserModel>[];
  final _infoStrings = <String>[];
  bool muted = false;
  RtcEngine _engine;
  bool localVideoRendered = true;
  AnimationController animationController;
  bool gameViewOpen = false, isBottomSheetOpen = false;

  bool isCalled = false, showEffectList = false, memberNotAvailable = false;
  int callerId;
  String selectedEffect;
  bool completed = false;
  List<String> questionsList = [];
  Animation offsetAnimation;
  LocalCameraProvider _localCameraProvider;
  RemoteCameraProvider _remoteCameraProvider;

  @override
  void dispose() {
    // clear users
    _users.clear();
    // destroy sdk
    animationController.dispose();
    offsetAnimation.removeListener(() {});
    _engine.leaveChannel();
    _engine.destroy();
    Wakelock.disable();
    super.dispose();
  }

  @override
  void initState() {
    SystemChrome.setEnabledSystemUIOverlays([]);
    animationController = AnimationController(
      vsync: this,
      duration: Duration(seconds: 2),
    );
    offsetAnimation = Tween<Offset>(
      begin: Offset(1.5, 0.0),
      end: const Offset(0.0, 0.0),
    ).animate(CurvedAnimation(parent: animationController, curve: Curves.linearToEaseOut));
    animationController.forward();
    animationController.addStatusListener((AnimationStatus status) {
      if (status == AnimationStatus.completed) {
        print('Completed');
      } else {
        print('Going');
      }
    });

    // animationController.forward();
    super.initState();
    // To keep the screen on:
    Wakelock.enable();
    // checkIfGameViewIsOpen();
    // initialize agora sdk
    selectedEffect = widget.selectedEffect;
    initialize();
  }

  @override
  void didChangeDependencies() {
    if (!isCalled) {
      isCalled = true;
      _localCameraProvider = Provider.of<LocalCameraProvider>(context);
      _remoteCameraProvider = Provider.of<RemoteCameraProvider>(context);
      super.didChangeDependencies();
    }
  }

  animationRepeat() {
    setState(() {
      animationController = AnimationController(
        vsync: this,
        duration: Duration(milliseconds: 7000),
      );
      offsetAnimation = Tween<Offset>(
        begin: Offset(2.0, 0.0),
        end: const Offset(0.0, 0.0),
      ).animate(CurvedAnimation(parent: animationController, curve: Curves.fastLinearToSlowEaseIn));
    });
    animationController.forward();
  }

  // Initialize Agora and throw error message if anything is wrong
  Future<void> initialize() async {
    try {
      if (ApiUtils.APP_ID.isEmpty) {
        setState(() {
          _infoStrings.add(
            'APP_ID missing, please provide your APP_ID in settings.dart',
          );
          _infoStrings.add('Agora Engine is not starting');
        });
        return;
      }

      await _initAgoraRtcEngine();
      _addAgoraEventHandlers();
      await _engine.enableWebSdkInteroperability(true);
      // _engine.setExternalVideoSource();  // TODO: replace this method to every where with effect changing method
      print("Token ${widget.agoraToken} === ${widget.channelName}");
      await _engine.joinChannel(
          widget.agoraToken,
          widget.channelName,
          // "0063a7d00163f6d41d6b533653369d4ce0eIACxsc6VIezdTALvc2rIsnN158SlYnwwv/TFwsP4LenjkXPFBqYAAAAAEADr9jhXGReRYAEAAQAYF5Fg",
          // "calling",
          null,
          0);
    } finally {
      print("Finally banuba init in videocall");
      if (widget.isMuted) {
        _onToggleMute();
      }
      if (!widget.isFrontCamera) {
        print("switch called on init ${widget.isFrontCamera}");
        _engine.switchCamera(isFrontCamera: widget.isFrontCamera);
      }
      if (!widget.cameraOn) {
        _onEnableDisableCamera(widget.cameraOn);
      }
    }
  }

  // Create agora sdk instance and initialize
  Future<void> _initAgoraRtcEngine() async {
    _engine = await RtcEngine.create(ApiUtils.APP_ID);
    VideoEncoderConfiguration configuration = VideoEncoderConfiguration();
    configuration.dimensions = VideoDimensions(width: 360, height: 640);
    configuration.minFrameRate = VideoFrameRate.Fps7;
    configuration.frameRate = VideoFrameRate.Fps15;
    await _engine.setVideoEncoderConfiguration(configuration);
    await _engine.enableVideo();
    await _engine.setChannelProfile(ChannelProfile.LiveBroadcasting);
    await _engine.setClientRole(widget.role);
  }

  // Add agora event handlers
  void _addAgoraEventHandlers() {
    _engine.setEventHandler(RtcEngineEventHandler(error: (code) {
      setState(() {
        final info = 'onError: $code';
        _infoStrings.add(info);
        memberNotAvailable = true;
      });
    }, joinChannelSuccess: (channel, uid, elapsed) {
      setState(() {
        print("caller id called: $uid");
        callerId = uid;
        final info = 'onJoinChannel: $channel, uid: $uid';
        _infoStrings.add(info);
      });
    }, leaveChannel: (stats) {
      setState(() {
        _infoStrings.add('onLeaveChannel');
        _users.clear();
      });
    }, userJoined: (uid, elapsed) {
      printLog("Call Answered");
      setState(() {
        localVideoRendered = true;

        memberNotAvailable = false;
        print("receiver id : $uid");
        final info = 'userJoined: $uid';
        _infoStrings.add(info);
        _users.add(AgoraUserModel(uid: uid, videoRender: false));
      });
    }, userOffline: (uid, elapsed) async {
      setState(() {
        printLog("Call Ended");
        final info = 'userOffline: $uid';
        _infoStrings.add(info);
        _users.remove(_users.firstWhere((element) => element.uid == uid));
        // _users.remove(uid);
        memberNotAvailable = true;
      });
    }, firstRemoteVideoFrame: (uid, width, height, elapsed) {
      setState(() {
        final info = 'firstRemoteVideo: $uid ${width}x $height';
        _infoStrings.add(info);
      });
      var remoteUser = _users.firstWhere((element) => element.uid == uid, orElse: () => null);
      if (remoteUser != null) {
        remoteUser.videoRender = true;
      }
    }, connectionLost: () {
      printLog("Connection Lost");
      _infoStrings.add("Connection Lost");
    }, connectionStateChanged: (connectionStatusType, connectionChangesReason) {
      if (ConnectionStateType.Disconnected == connectionStatusType) {
        printLog("Disconnected");
        _infoStrings.add("Disconnected");
      } else if (ConnectionStateType.Failed == connectionStatusType) {
        printLog("Failed");
        _infoStrings.add("Failed");
      }
    }, localVideoStateChanged: (LocalVideoStreamState localVideoStreamState, LocalVideoStreamError videoStreamError) {
      // TODO: Not Required in Agora VideoCall with banuba
      // if (localVideoStreamState == LocalVideoStreamState.Stopped) {
      //   printLog("local cam disabled");
      //   setState(() {
      //     cameraEnable = false;
      //   });
      // } else if (localVideoStreamState == LocalVideoStreamState.Capturing) {
      //   printLog("local cam enabled");
      //   setState(() {
      //     cameraEnable = true;
      //   });
      // }
    }, remoteVideoStateChanged: (int value, VideoRemoteState videoStats, VideoRemoteStateReason reason, int i) {
      if (reason == VideoRemoteStateReason.RemoteMuted) {
        printLog("remote cam disabled");
        _remoteCameraProvider.enableRCamera(false);
      } else if (reason == VideoRemoteStateReason.RemoteUnmuted) {
        printLog("remote cam enabled");
        _remoteCameraProvider.enableRCamera(true);
      }
    }, firstLocalVideoFrame: (int i, int j, int k) {
      printLog("Local frame rendered $localVideoRendered");
      // TODO: Not Required in Agora VideoCall with banuba

      // setState(() {
      //   localVideoRendered = true;
      // });
    }, firstLocalVideoFramePublished: (int i) {
      printLog("Local firstLocalVideoFramePublished $localVideoRendered === $i");
    }));
  }

  // Helper function to get list of native views
  List<Widget> _getRenderViews() {
    final List<Widget> list = [];
    if (widget.role == ClientRole.Broadcaster) {
      list.add(Consumer<LocalCameraProvider>(
          builder: (context, localCameraProvider, child) {
            return Stack(
              children: [
                (Platform.isIOS && Constants.IS_BANUBA_ON)
                    ? RtcLocalView.SurfaceView(
                  effectName: selectedEffect,
                  totalJoinedUser: max(_users.length + 1, 1),
                  // mirrorMode: VideoMirrorMode.Disabled,
                  isFrontCamera: widget.isFrontCamera,
                )
                    : RtcLocalView.SurfaceView(
                  totalJoinedUser: max(_users.length + 1, 1),
                  isFrontCamera: widget.isFrontCamera,
                ),
                if (!localVideoRendered)
                  Container(
                      color: ColorAssets.themeColorDarkGrey,
                      child: Center(
                        child: Text(
                          "Loading",
                          style: TextStyle(
                            color: ColorAssets.themeColorWhite,
                            fontSize: FontSize.s18,
                            fontWeight: FontWeight.bold,
                            fontFamily: StringAssets.fontFamilyPoppins,
                          ),
                        ),
                      )),
                localCameraProvider.cameraEnable ? Container() : _cameraTurnedOffWidget()
              ],
            );
          },
      ));
    }
    _users.forEach((AgoraUserModel uinfo) => list.add(Consumer<RemoteCameraProvider>(
        builder: (context, remoteCameraProvider, child) {
          return Stack(
            children: [
              (Platform.isIOS && Constants.IS_BANUBA_ON)
                  ? RtcRemoteView.SurfaceView(
                uid: uinfo.uid,
                totalJoinedUser: _users.length,
                mirrorMode: VideoMirrorMode.Enabled,
              )
                  : RtcRemoteView.SurfaceView(
                uid: uinfo.uid,
                totalJoinedUser: _users.length,
              ),
              if (!uinfo.videoRender)
                Container(
                    color: ColorAssets.themeColorDarkGrey,
                    child: Center(
                      child: Text(
                        "",
                        style: TextStyle(
                          color: ColorAssets.themeColorWhite,
                          fontSize: FontSize.s18,
                          fontWeight: FontWeight.bold,
                          fontFamily: StringAssets.fontFamilyPoppins,
                        ),
                      ),
                    )),
              remoteCameraProvider.remoteCamEnable ? Container() : _cameraTurnedOffWidget()
            ],
          );
        },
    )));
    return list;
  }

  // Video view wrapper
  Widget _videoView(view) {
    return Expanded(child: Container(child: view));
  }

  // Video view row wrapper
  Widget _expandedVideoRow(List<Widget> views) {
    final wrappedViews = views.map<Widget>(_videoView).toList();
    return Expanded(
      child: Row(
        children: wrappedViews,
      ),
    );
  }

  // Video layout wrapper
  Widget _viewRows() {
    final views = _getRenderViews();
    switch (views.length) {
      case 1:

        /// Single person view
        return Container(child: Consumer<LocalCameraProvider>(
          builder: (context, localCameraProvider, child) {
            return Stack(
              children: [
                Column(
                  children: <Widget>[
                    _videoView(views[0]),
                  ],
                ),
                localCameraProvider.cameraEnable ? Container() : Column(
                  children: [
                    _videoView(_cameraTurnedOffWidget()),
                  ],
                )
              ],
            );
          },
        ));
      case 2:

        /// Two persons view
        return Container(
            child: Column(
          children: <Widget>[
            // _expandedVideoRow([views[1]]),
            // _expandedVideoRow([views[0]])
            _expandedVideoRow([views[1]]),
            _expandedVideoRow([views[0]])
          ],
        ));
      /* case 3:

        /// Three persons view
        return Container(
            child: Column(
          children: <Widget>[_expandedVideoRow(views.sublist(0, 2)), _expandedVideoRow(views.sublist(2, 3))],
        ));
      case 4:

        /// Four persons view
        return Container(
            child: Column(
          children: <Widget>[_expandedVideoRow(views.sublist(0, 2)), _expandedVideoRow(views.sublist(2, 4))],
        ));*/
      default:
    }
    return Container();
  }

  /// Info panel to show logs
  Widget _panel() {
    print("InfoStrings : $_infoStrings");
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 48),
      alignment: Alignment.bottomCenter,
      child: FractionallySizedBox(
        heightFactor: 0.5,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 48),
          child: ListView.builder(
            reverse: true,
            itemCount: _infoStrings.length,
            itemBuilder: (BuildContext context, int index) {
              if (_infoStrings.isEmpty) {
                return null;
              }
              return Padding(
                padding: const EdgeInsets.symmetric(
                  vertical: 3,
                  horizontal: 10,
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Flexible(
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          vertical: 2,
                          horizontal: 5,
                        ),
                        decoration: BoxDecoration(
                          color: Colors.yellowAccent,
                          borderRadius: BorderRadius.circular(5),
                        ),
                        child: Text(
                          _infoStrings[index],
                          style: TextStyle(color: Colors.blueGrey),
                        ),
                      ),
                    )
                  ],
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  // To end the call
  Future<void> _onCallEnd(BuildContext context) async {
    // To let the screen turn off again:
    Wakelock.disable();
    //GameState().restart();

    Navigator.pop(context, true);
  }

  // to mute/unMute
  void _onToggleMute() {
    setState(() {
      muted = !muted;
    });
    _engine.muteLocalAudioStream(muted).then((value) {
      setState(() {});
    });
  }

  // to switch camera view
  void _onSwitchCamera() {
    _engine.switchCamera();
  }

  void _onEnableDisableCamera(bool cameraEnable) {
    _localCameraProvider.enableCamera(cameraEnable);
    _engine.enableLocalVideo(cameraEnable);
    printLog("widget.cameraOn : ${cameraEnable}");
  }

  @override
  Widget build(BuildContext context) {
    Constant.setScreenAwareConstant(context);
    return WillPopScope(
      onWillPop: () {
        return Future.value(false);
      },
      child: Scaffold(
        backgroundColor: ColorAssets.themeColorWhite,
        extendBody: true,
        bottomNavigationBar: BottomAppBar(
          elevation: 0,
          color: Colors.transparent,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Stack(
                alignment: Alignment.bottomLeft,
                children: [
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      SizedBox(
                        height: Constant.size12,
                      ),
                      SizedBox(
                        height: Constant.size20,
                      ),
                    ],
                  ),
                ],
              ),

              /// AR Effects list view
              _buildFilterSelector(),

              Stack(
                alignment: Alignment.center,
                children: [
                  /// Black circular rectangle
                  Padding(
                    padding: EdgeInsets.symmetric(vertical: Constant.size40, horizontal: Constant.size24),
                    child: ClipRRect(
                      borderRadius: BorderRadius.all(Radius.circular(Constant.size10)),
                      child: Container(
                        color: ColorAssets.themeColorBlack.withOpacity(0.5),
                        height: Constant.size44,
                        width: MediaQuery.of(context).size.width,
                        child: BackdropFilter(
                          filter: ImageFilter.blur(sigmaY: 30, sigmaX: 30),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceAround,
                            children: [
                              /// Filters button
                              InkResponse(
                                onTap: (Platform.isIOS && Constants.IS_BANUBA_ON)
                                    ? () {
                                        setState(() {
                                          showEffectList = !showEffectList;
                                        });
                                      }
                                    : null,
                                child: SvgPicture.asset(
                                  ImageAssets.beautyIcon,
                                  color: (Platform.isIOS && Constants.IS_BANUBA_ON)
                                      ? Colors.white
                                      : Colors.white.withOpacity(0.5),
                                  height: Constant.size24,
                                  width: Constant.size24,
                                ),
                              ),

                              /// Microphone on/off button
                              InkWell(
                                onTap: _onToggleMute,
                                child: SvgPicture.asset(
                                  muted ? ImageAssets.micOffIcon : ImageAssets.micOnIcon,
                                  color: Colors.white,
                                  height: Constant.size24,
                                  width: Constant.size24,
                                ),
                              ),
                              SizedBox(
                                width: Constant.size64,
                              ),

                              /// Camera on/off button
                              InkWell(
                                onTap: (){
                                  _onEnableDisableCamera(!_localCameraProvider.cameraEnable);
                                },
                                child: SvgPicture.asset(
                                  !_localCameraProvider.cameraEnable
                                      ? ImageAssets.cameraOffIcon
                                      : ImageAssets.cameraOnIcon,
                                  color: Colors.white,
                                  height: Constant.size22,
                                  width: Constant.size24,
                                ),
                              ),

                              /// Camera switch button
                              InkWell(
                                onTap: _onSwitchCamera,
                                child: SvgPicture.asset(
                                  ImageAssets.switchCameraIcon,
                                  color: Colors.white,
                                  height: Constant.size20,
                                  width: Constant.size24,
                                ),
                              ),

                              /// Game button
                              /* InkWell(
                                onTap: () {
                                  */ /*if (!gameViewOpen) {
                        gameViewOpen = true;
                        updateUser(gameViewOpen);
                        if(!isBottomSheetOpen) {
                          _showModalSheet(widget.gameId, widget.toUserId, widget.me);
                        }
                        setState(() {});
                      }else{
                        gameViewOpen = false;
                        updateUser(gameViewOpen);
                        Navigator.pop(context,true);
                        setState(() {});
                      }*/ /*
                                },
                                child: SvgPicture.asset(
                                  ImageAssets.gameIcon,
                                  color: Colors.white,
                                  height: Constant.size20,
                                  width: Constant.size24,
                                ),
                              ),*/
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),

                  /// Call end button
                  IgnorePointer(
                    ignoring: endCallButtonPressed,
                    child: RoundedButton(
                      iconSrc: ImageAssets.endCallIcon,
                      size: Constant.size72,
                      color: Colors.red,
                      press: () {
                        _onCallEnd(context);
                      },
                      iconColor: ColorAssets.themeColorWhite,
                    ),
                  )
                ],
              ),
            ],
          ),
        ),

        /// Video call portion
        body: (_engine != null)
            ? _viewRows()
            : Container(
                color: Colors.black,
              ),
      ),
    );
  }

  // AR effects selector
  Widget _buildFilterSelector() {
    return Visibility(
      visible: showEffectList,
      child: FilterSelector(
        onFilterChanged: onEffectChanged,
        filters: effectNames,
      ),
    );
  }

  List<String> effectNames = [
    "",
    "BeautyBokeh",
    "BGVideoBeach",
    "Polaroid",
    "PrideParade",
    "BeautyEffectsGrayscale",
    "Emoji",
    "HeartsLut",
    "HeadphoneMusic",
    "RainbowBeauty",
    "WhiteCat"
  ];

  void onEffectChanged(String effectName) {
    print('onEffectChanged $effectName');
    selectedEffect = effectName;
    _engine.onEffectSelected(effectName);
  }

  Widget _cameraTurnedOffWidget() {
    return Container(
      color: Colors.black,
      child: Center(
        child: Text(
          StringAssets.cameraDisableMsg,
          style: Styles.joinCallStyle,
        ),
      ),
    );
  }

  bool endCalled = false, endCallButtonPressed = false;

  Future<dynamic> showRemoteUserStatusDialog(BuildContext context, {String code, String message}) {
    return showDialog(
        barrierDismissible: false,
        context: context,
        builder: (context) {
          return Dialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(Constant.size24)),
            //this right here
            child: IntrinsicHeight(
              child: Container(
                padding: EdgeInsets.all(Constant.size16),
                decoration: Styles.containerWithShadowBox,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    IgnorePointer(
                      ignoring: endCalled,
                      child: Padding(
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
                    ),
                    SizedBox(height: Constant.size18),
                    CommonWidgets().buttonView(StringAssets.ok, onClick: () {
                      setState(() {
                        endCalled = true;
                      });
                      Navigator.pop(context, true);
                      Navigator.pop(context, true);
                    })
                  ],
                ),
              ),
            ),
          );
        });
  }

/* Future<void> updateUser(bool gameStart) async {
    await FirebaseDatabase.instance
        .reference()
        .child(USERS)
        .child(PreferenceHelper.getUserData().id.toString())
        .update({"gameStart": gameStart});
    await FirebaseDatabase.instance
        .reference()
        .child(USERS)
        .child(widget.toUserId)
        .update({"gameStart": gameStart});
  }

  void _showModalSheet(String gameId, String fromId, me) {
    setState(() {
      isBottomSheetOpen = true;
    });
    showModalBottomSheet(
        context: context,
        barrierColor: Colors.black.withAlpha(1),
        backgroundColor: Colors.transparent,
        isDismissible: false,
        enableDrag: false,
        builder: (builder) {
          return WillPopScope(
            onWillPop: (){
              return;
            },
            child: Opacity(
              opacity: 0.7,
              child: Container(
                decoration: new BoxDecoration(
                  color: Colors.greenAccent,
                  borderRadius: BorderRadius.only(
                      topRight: Radius.circular(Constant.size16),
                      topLeft: Radius.circular(Constant.size16)),
                ),
                height: MediaQuery.of(context).copyWith().size.height * 0.40,
                margin: EdgeInsets.symmetric(
                    horizontal: Constant.size32, vertical: Constant.size32),
                child: Stack(
                  children: [
                    Center(
                      child: Game(
                          title: 'Tic Tac Toe',
                          type: "wifi",
                          me: me,
                          gameId: gameId,
                          withId: fromId),
                    ),
                    Align(
                        alignment: Alignment.topRight,
                        child: InkWell(
                            onTap: () {
                              gameViewOpen = false;
                              updateUser(gameViewOpen);
                              if(isBottomSheetOpen) {
                                setState(() {
                                  isBottomSheetOpen = false;
                                });
                                restart(gameId);
                                Navigator.pop(context,true);
                              }
                            },
                            child: Container(
                                margin: EdgeInsets.only(
                                    right: Constant.size10, top: Constant.size10),
                                child: Icon(
                                  Icons.close,
                                  color: Colors.white,
                                ))))
                  ],
                ),
              ),
            ),
          );
        });
  }
  void restart(gameId) async {
    await FirebaseDatabase.instance
        .reference()
        .child('games')
        .child(gameId)
        .set(null);
  }*/
}

class AgoraUserModel {
  int uid;
  bool videoRender;

  AgoraUserModel({this.uid, this.videoRender = false});

  AgoraUserModel.fromJson(Map<String, dynamic> json) {
    uid = json['uid'];
    videoRender = json['videoRender'] ?? false;
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['uid'] = this.uid;
    data['videoRender'] = this.videoRender;
    return data;
  }
}
