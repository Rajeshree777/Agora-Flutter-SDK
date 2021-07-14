import 'dart:math';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:soda_app/source/color_assets.dart';
import 'package:soda_app/source/image_assets.dart';
import 'package:soda_app/source/string_assets.dart';
import 'package:soda_app/source/styles.dart';
import 'package:soda_app/utils/screen_util.dart';
import 'screen_util.dart';

class CommonWidgets {
  Widget buttonView(String buttonName, {Function onClick, double width}) {
    return GestureDetector(
      onTap: onClick,
      child: Container(
        width: width == null ? double.infinity : width,
        alignment: Alignment.center,
        padding: EdgeInsets.symmetric(
            horizontal: ScreenUtil().setWidth(24.0),
            vertical: ScreenUtil().setHeight(13.0)),
        decoration: BoxDecoration(
          color: ColorAssets.themeColorDarkDeepPink,
          borderRadius: BorderRadius.circular(Constant.size12),
        ),
        child: Text(
          buttonName,
          style: Styles.textStyle(
            color: ColorAssets.themeColorWhite,
            fontWeight: FontWeight.w600,
            fontSize: ScreenUtil().setSp(16.0),
          ),
        ),
      ),
    );
  }

  /// progress Loader Widget
  Widget progressLoading({String message}) {
    return  Center(
      child: CircularProgressIndicator(),
    );
  }
}

class RoundedButton extends StatelessWidget {
  const RoundedButton({
    Key key,
    this.size = 64,
    @required this.iconSrc,
    this.color = Colors.white,
    this.iconColor = Colors.black,
    @required this.press,
  }) : super(key: key);

  final double size;
  final String iconSrc;
  final Color color, iconColor;
  final VoidCallback press;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: size,
      width: size,
      child: FlatButton(
        padding: EdgeInsets.all(Constant.size18),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(Constant.size100)),
        ),
        color: color,
        onPressed: press,
        child: SvgPicture.asset(iconSrc, color: iconColor),
      ),
    );
  }
}
