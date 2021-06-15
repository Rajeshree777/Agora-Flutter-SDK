import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:soda_app/source/color_assets.dart';
import 'package:soda_app/source/string_assets.dart';
import 'package:soda_app/utils/screen_util.dart';

class Styles {
  static TextStyle joinCallStyle =
  TextStyle(color: Colors.white, fontFamily: StringAssets.fontFamilyPoppins, fontSize: FontSize.s14);

  static TextStyle startCallStyle =
  TextStyle(fontSize: Constant.size12, color: Colors.white, fontFamily: StringAssets.fontFamilyPoppins);

  static BoxDecoration containerWithShadowBox = BoxDecoration(
    color: ColorAssets.themeColorWhite,
    borderRadius: BorderRadius.circular(Constant.size24),
    boxShadow: [
      BoxShadow(
        color: Colors.grey.withOpacity(0.6),
        offset: Offset(4.0, 4.0),
        blurRadius: 10.0,
      ),
    ],
  );

  static TextStyle textStyle(
      {Color color = ColorAssets.themeColorBlack, double fontSize, FontWeight fontWeight = FontWeight.normal}) {
    return TextStyle(
        color: color,
        fontSize: fontSize != 0 ? fontSize : ScreenUtil().setSp(12.0),
        fontFamily: StringAssets.fontFamilyPoppins,
        fontWeight: fontWeight);
  }

  static TextStyle titleTextStyleWhilte = TextStyle(
      fontFamily: StringAssets.fontFamilyPoppins,
      color: ColorAssets.themeColorWhite,
      fontSize: FontSize.s16,
      fontWeight: FontWeight.w600);
}
