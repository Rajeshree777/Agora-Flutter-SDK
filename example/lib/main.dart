import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:soda_app/providers/video_dating_screen_state.dart';
import 'package:soda_app/ui/home_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MultiProvider(
        providers: [
          ChangeNotifierProvider(create: (context) => LocalCameraProvider()),
          ChangeNotifierProvider(create: (context) => RemoteCameraProvider()),
        ],
        child: MaterialApp(
          home: HomeScreen(),
        ));
  }
}
