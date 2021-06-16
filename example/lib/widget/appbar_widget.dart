import 'package:flutter/material.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:soda_app/source/color_assets.dart';
import 'package:soda_app/source/styles.dart';
import 'package:soda_app/utils/screen_util.dart';

/// common Custom App bar Widget for setting App bar inside Scaffold
class AppBarWidget extends StatefulWidget implements PreferredSizeWidget {

  final AppBar appBar = AppBar();
  @override
  Size get preferredSize {
    return new Size.fromHeight(appBar.preferredSize.height);
  }

  final Function onBackPressed, onTitleClick, onIconClick;
  final String title;
  final String rightText;
  final Widget titleTrailingIcon;
  final bool isIconVisible;
  final bool isTextVisible;
  final IconData iconLeftName;
  final String iconName;
  final bool updateData;

  AppBarWidget(
      {Key key,
        this.onBackPressed,
        this.title,
        this.titleTrailingIcon,
        this.onTitleClick,
        this.onIconClick,
        this.isIconVisible = false,
        this.iconName = "",
        this.isTextVisible = false,
        this.rightText = "",
        this.iconLeftName,
        this.updateData = false})
      : super(key: key);

  @override
  _AppBarWidgetState createState() => _AppBarWidgetState();
}

class _AppBarWidgetState extends State<AppBarWidget> {
  @override
  Widget build(BuildContext context) {
    return AppBar(
      backgroundColor: Colors.white,
      toolbarHeight: 1100.0,
      centerTitle: true,
      elevation: 1.0,

      leading: InkWell(
        onTap: widget.onBackPressed,
        child: Icon(
          widget.iconLeftName,
          color: Colors.black,
          size: ScreenUtil().setWidth(28.0),
        ),
      ),
      title: InkWell(
        onTap: widget.onTitleClick,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(widget.title, style: Styles.textStyle(
                color: ColorAssets.themeColorBlack,
                fontWeight: FontWeight.w800,
                fontSize: ScreenUtil().setSp(18.0)
            ),),
            (widget.titleTrailingIcon != null) ? SizedBox(width: Constant.size4) : SizedBox.shrink(),
            (widget.titleTrailingIcon != null) ? widget.titleTrailingIcon : SizedBox.shrink(),
          ],
        ),
      ),

      actions: [
        Visibility(
          visible: widget.isIconVisible,
          child: widget.isTextVisible
              ? Container(
                margin: EdgeInsets.only(right: Constant.size20,
                    top: Constant.size8, bottom: Constant.size8),
                child: GestureDetector(
            onTap: widget.onIconClick,
            child: Container(
                alignment: Alignment.center,
                padding: EdgeInsets.symmetric(horizontal: Constant.size12),
                decoration: BoxDecoration(
                    color: ColorAssets.themeColorDarkDeepPink,
                    borderRadius: BorderRadius.circular(Constant.size8)
                ),
                child: Text(
                  widget.rightText,
                  textAlign: TextAlign.center,
                  style: Styles.titleTextStyleWhilte,
                ),
            ),
          ),
              )
              : IgnorePointer(
            ignoring: !widget.updateData,
            child: InkWell(
              onTap: widget.onIconClick,
              child: Container(
                  alignment: Alignment.centerRight,
                  margin: EdgeInsets.only(right: Constant.size16),
                  child: SvgPicture.asset(
                    widget.iconName,
                    height: Constant.size32,
                    width: Constant.size32,
                    color: widget.updateData ? null : Colors.transparent,
                  )),
            ),
          ),
        )

      ],

    );
  }
}