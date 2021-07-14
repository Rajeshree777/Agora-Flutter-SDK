/*
 * Author: Saurabh-Detharia (saurabh.detharia@bacancy.com)
 * Date: 23-02-21
 * Brief Description: Model class for short video creation
 *  Notes:
 *
 */

import 'dart:typed_data';

class VideoModel {
  int duration;
  int snippetDuration;
  int startDuration;
  String videoPath;

  num trimStartDuration;
  num trimEndDuration;
  List<Uint8List> thumbList;

  VideoModel(
      {this.duration,
        this.startDuration,
        this.snippetDuration,
        this.videoPath,
        this.thumbList,
        creationDate,
        captions,
        this.trimStartDuration = 0,
        this.trimEndDuration = 0});

  Map<String, dynamic> toJson() {
    var map = new Map<String, dynamic>();
    map["duration"] = duration;
    map["startDuration"] = startDuration;
    map["snippetDuration"] = snippetDuration;
    map["videoPath"] = videoPath;
    map["thumbList"] = thumbList;
    trimStartDuration = 0;
    trimEndDuration = duration;
    return map;
  }

  factory VideoModel.fromJson(Map<String, dynamic> json) => VideoModel(
      duration: json["duration"],
      startDuration: json["startDuration"],
      snippetDuration: json["snippetDuration"],
      videoPath: json["videoPath"],
      thumbList: json["thumbList"]);
}

class SingleSnippetModel {
  int duration;
  bool isRecorded;
  bool isSnippetEnd;

  SingleSnippetModel(this.duration, this.isRecorded, this.isSnippetEnd);
}
