package com.banuba.sdk.internal.encoding;


import androidx.annotation.NonNull;

public interface IAudioDataSender { void sendAudioData(@NonNull byte[] buffer, long audioTimeNs); }
