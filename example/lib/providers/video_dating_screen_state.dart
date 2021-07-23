import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class LocalCameraProvider extends ChangeNotifier {
  bool cameraEnable = true;

  enableCamera(bool enable) {
    cameraEnable = enable;
    notifyListeners();
  }
}


class RemoteCameraProvider extends ChangeNotifier {
  bool remoteCamEnable = true;

  enableRCamera(bool enable) {
    remoteCamEnable = enable;
    notifyListeners();
  }
}
