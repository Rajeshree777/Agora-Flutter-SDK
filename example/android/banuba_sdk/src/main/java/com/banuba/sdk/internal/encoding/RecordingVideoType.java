package com.banuba.sdk.internal.encoding;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Specifies type of video, that is recorded.
 */
@Retention(CLASS)
@StringDef({RecordingVideoType.DEFAULT_VIDEO_TYPE})
public @interface RecordingVideoType {
    /**
     * Default video type with no custom overlay (raw stream + effect overlay).
     */
    public String DEFAULT_VIDEO_TYPE = "default";
};