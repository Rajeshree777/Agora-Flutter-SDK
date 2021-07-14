import 'dart:io';

import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';
import 'package:flutter/material.dart';

class VideoApp extends StatefulWidget {
  VideoApp(this.videopath);

  final videopath;

  @override
  _VideoAppState createState() => _VideoAppState();
}

class _VideoAppState extends State<VideoApp> {
  VideoPlayerController _controller;

  @override
  void initState() {
    super.initState();
    _controller = VideoPlayerController.file(File(widget.videopath))
      ..initialize().then((_) {
        // Ensure the first frame is shown after the video is initialized, even before the play button has been pressed.
        _controller.play();
        _controller.setLooping(true);
        setState(() {});
      });
  }

  @override
  Widget build(BuildContext context) {
    SystemChrome.setEnabledSystemUIOverlays([SystemUiOverlay.bottom]);
    return Scaffold(
      body: Stack(
        children: [
          Container(
            color: Colors.black,
            child: _controller.value.isInitialized
                ? Align(
                    alignment: Alignment.topCenter,
                    child: AspectRatio(
                      child: VideoPlayer(_controller),
                      aspectRatio: _controller.value.aspectRatio,
                    ),
                  )
                : Container(),
          ),
          Positioned(
            left: 11,
            child: InkWell(
              onTap: (){
                Navigator.pop(context);
              },
              child: Icon(
                  Icons.arrow_back,
                  color: Colors.black.withOpacity(0.4),
                  size: 35,
                ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          setState(() {
            _controller.value.isPlaying ? _controller.pause() : _controller.play();
          });
        },
        child: Icon(
          _controller.value.isPlaying ? Icons.pause : Icons.play_arrow,
        ),
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
    _controller.dispose();
  }
}
