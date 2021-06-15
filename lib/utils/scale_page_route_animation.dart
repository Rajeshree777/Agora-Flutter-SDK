import 'package:flutter/cupertino.dart';

class ScalePageRoute extends PageRouteBuilder {
  final Widget widget;

  ScalePageRoute(this.widget)
      : super(
      transitionDuration: Duration(milliseconds: 200),
      transitionsBuilder: (BuildContext context,
          Animation<double> animation,
          Animation<double> secAnimation,
          Widget child) {
        animation = CurvedAnimation(
            parent: animation, curve: Curves.easeInQuad,reverseCurve:Curves.easeInQuad);
        return ScaleTransition(
          scale: animation,
          child: child,
          alignment: Alignment.center,
        );
      },
      pageBuilder: (BuildContext context, Animation<double> animation,
          Animation<double> secAnimation) {
        return widget;
      });
}
