/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.banuba.sdk.internal.utils;

import android.util.Log;

import java.util.Locale;
import java.util.MissingFormatArgumentException;

/**
 * Logging helper class. <br/>
 * It uses standard Log.* methods, but it is aware from NullPointerException
 * in case of nullable "msg". <br/>
 */
public final class Logger {
    private static final String NULLABLE_ARGUMENT = "Argument is NULL!";
    private static final String TAG = "banuba_sdk";

    /**
     * No need to instantiate. <br/>
     * <p/>
     * Throw an {@link Exception} explicitly if granting access from Reflection.
     */
    private Logger() {
        throw new AssertionError();
    }

    public static void v(String format, Object... args) {
        Log.v(TAG, buildMessage(format, args));
    }

    public static void d(String format, Object... args) {
        Log.d(TAG, buildMessage(format, args));
    }

    public static void i(String format, Object... args) {
        Log.i(TAG, buildMessage(format, args));
    }

    public static void w(String format, Object... args) {
        Log.w(TAG, buildMessage(format, args));
    }

    public static void w(Throwable tr, String format, Object... args) {
        Log.w(TAG, buildMessage(format, args), tr);
    }

    public static void e(String format, Object... args) {
        Log.e(TAG, buildMessage(format, args));
    }

    public static void e(Throwable tr, String format, Object... args) {
        Log.e(TAG, buildMessage(format, args), tr);
    }

    public static void wtf(String format, Object... args) {
        Log.wtf(TAG, buildMessage(format, args));
    }

    public static void wtf(Throwable tr, String format, Object... args) {
        Log.wtf(TAG, buildMessage(format, args), tr);
    }

    public static void wtf(Throwable tr) {
        Log.wtf(TAG, tr);
    }

    /**
     * Formats the caller's provided message and prepends useful info like
     * calling thread ID and method name.
     *
     * @param format Argument to prepare special message to be shown at dev's console.<br/>
     *               In case argument is <code>null</code>: developer will be notified with a
     * special message.<br/>
     */
    private static String buildMessage(String format, Object... args) {
        String msg;
        try {
            msg = (args == null) ? format
                                 : (format == null) ? NULLABLE_ARGUMENT
                                                    : String.format(Locale.US, format, args);
        } catch (MissingFormatArgumentException ex) {
            msg = "";
        }
        if (msg == null) {
            msg = "";
        }
        StackTraceElement[] trace = new Throwable().fillInStackTrace().getStackTrace();

        String caller = "<unknown>";
        // Walk up the stack looking for the first caller outside of VolleyLog.
        // It will be at least two frames up, so start there.
        for (int i = 2, traceLength = trace.length; i < traceLength; i++) {
            Class<?> clazz = trace[i].getClass();
            if (!clazz.equals(Logger.class)) {
                String callingClass = trace[i].getClassName();
                callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1);
                callingClass = callingClass.substring(callingClass.lastIndexOf('$') + 1);

                caller = callingClass + "." + trace[i].getMethodName();
                break;
            }
        }
        return String.format(Locale.US, "[%d] %s: %s", Thread.currentThread().getId(), caller, msg);
    }
}
