import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:soda_app/incoming_call_screen.dart';

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

    return MaterialApp(
     home: IncomingCallScreen(),);
  }
}
