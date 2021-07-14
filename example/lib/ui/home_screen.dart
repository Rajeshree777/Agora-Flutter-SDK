import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:soda_app/incoming_call_screen.dart';
import 'package:soda_app/ui/call_screen.dart';
import 'package:soda_app/ui/video_record_screen.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Home'),
      ),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ElevatedButton(
              onPressed: () {
                Navigator.push(
                  context,
                  CupertinoPageRoute(builder: (context) => VideoRecordScreen()),
                );
              },
              child: Text('Video Record'),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.push(
                  context,
                  CupertinoPageRoute(builder: (context) => IncomingCallScreen()),
                );
              },
              child: Text('Video Call'),
            ),
          ],
        ),
      ),
    );
  }
}
