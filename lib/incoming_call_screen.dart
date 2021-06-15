import 'package:agora_rtc_engine/rtc_engine.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:soda_app/source/string_assets.dart';
import 'package:soda_app/ui/call_screen.dart';
import 'package:soda_app/utils/common_widgets.dart';
import 'package:soda_app/utils/screen_util.dart';

class IncomingCallScreen extends StatefulWidget {

  @override
  _IncomingCallScreenState createState() => _IncomingCallScreenState();
}

class _IncomingCallScreenState extends State<IncomingCallScreen> {
  @override
  Widget build(BuildContext context) {
    Constant.setScreenAwareConstant(context);
    return Scaffold(
        body: Stack(
          fit: StackFit.expand,
          children: [
            // Image
            Image.asset(
              "assets/images/full_image.png",
              fit: BoxFit.cover,
            ),
            // Black Layer
            DecoratedBox(
              decoration: BoxDecoration(color: Colors.black.withOpacity(0.3)),
            ),
            Padding(
              padding: EdgeInsets.all(Constant.size20),
              child: SafeArea(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      "ABC",
                      style: Theme.of(context).textTheme.headline3.copyWith(color: Colors.white),
                    ),
                    SizedBox(height: Constant.size10),
                    Text(
                      StringAssets.incoming.toUpperCase(),
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.6),
                      ),
                    ),
                    Spacer(),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        RoundedButton(
                          press: () {
                            _onCallEnd(context);
                          },
                          color: Colors.red,
                          iconColor: Colors.white,
                          iconSrc: "assets/icons/call_end.svg",
                        ),
                        new RotationTransition(
                          turns: new AlwaysStoppedAnimation(220 / 360),
                          child: RoundedButton(
                            press: () {
                              // call receive button
                              onJoin();
                            },
                            color: Colors.green,
                            iconColor: Colors.white,
                            iconSrc: "assets/icons/call_end.svg",
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ));
  }

  void _onCallEnd(BuildContext context) {
    Navigator.pop(context);
  }

  Future<void> onJoin() async {
    await _handleCameraAndMic(Permission.camera);
    await _handleCameraAndMic(Permission.microphone);
    // push video page with given channel name
    Future.delayed(Duration.zero,() async {
      await Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => CallScreen(
            token: "0063a7d00163f6d41d6b533653369d4ce0eIAAUJlEU7+tZlOLl4bKVd5o/ta3Y5EvvYohWWgGPK/GaOHPFBqYAAAAAEAARnS3HuwzKYAEAAQC7DMpg",
            channel: 'calling',
            isStartCall: false,
            ),
        ),
      );
    });
  }

  Future<void> _handleCameraAndMic(Permission permission) async {
    final status = await permission.request();
    print(status);
  }
}
