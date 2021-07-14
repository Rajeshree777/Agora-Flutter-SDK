import 'package:fluttertoast/fluttertoast.dart';
import 'package:soda_app/source/color_assets.dart';

void showToast(String text) {
  Fluttertoast.showToast(
      msg: text,
      toastLength: Toast.LENGTH_SHORT,
      gravity: ToastGravity.BOTTOM,
      timeInSecForIosWeb: 2,
      backgroundColor: ColorAssets.themeColorBlack,
      textColor: ColorAssets.themeColorWhite,
      fontSize: 16.0,
  );
}
