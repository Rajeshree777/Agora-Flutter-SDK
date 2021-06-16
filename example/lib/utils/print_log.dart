/*
 * Author: Vaidehee Vala (vaidehee.vala@bacancy.com)
 * Date: 18-02-21
 * Brief Description:this class is used to print debug logs, set shouldPrintLog=false if you no longer want to print logs
 */
import 'dart:core';
import 'dart:developer';
bool shouldPrintLog = true;

printLog(value) {
  if (shouldPrintLog == true) {
    log("$value");
  }
}

printLogTag(tag, value) {
  if (shouldPrintLog == true) {
    log("$tag $value");
  }
}
