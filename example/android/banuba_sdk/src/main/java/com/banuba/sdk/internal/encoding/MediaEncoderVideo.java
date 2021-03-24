/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.banuba.sdk.internal.encoding;

import android.media.MediaCodec;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Surface;

import com.banuba.sdk.internal.utils.Logger;

import java.io.IOException;


class MediaEncoderVideo extends MediaEncoderBase {
    private Surface mInputSurface;

    MediaEncoderVideo(
        @NonNull MediaMuxerWrapper muxerWrapper,
        @Nullable EncoderSync encoderSync,
        int width,
        int height) {
        super(muxerWrapper, encoderSync, width, height);
    }

    @Override
    protected boolean isVideoEncoder() {
        return true;
    }

    @Override
    protected boolean isAudioEncoder() {
        return false;
    }

    @Override
    public void prepare() {
        try {
            mEncoder = MediaCodec.createEncoderByType(RecordingParams.getVideoMIME());
        } catch (IOException e) {
            Logger.e(e.getMessage());
        }

        mEncoder.configure(
            RecordingParams.getVideoFormat(width, height),
            null,
            null,
            MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();
    }

    void drainEncoder(boolean endOfStream) {
        super.drainEncoder(endOfStream);
        if (mEncoderSync != null) {
            mEncoderSync.setVideoEncoded();
        }
    }


    Surface getInputSurface() {
        return mInputSurface;
    }

    @Override
    public void close() {
        final Surface surface = mInputSurface;
        if (surface != null) {
            surface.release();
        }
        super.close();
    }
}
