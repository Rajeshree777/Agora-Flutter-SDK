package com.banuba.sdk.internal.utils;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import androidx.annotation.NonNull;

public final class MovieDataExtractor {
    private MovieDataExtractor() {
    }


    public static long extractDuration(@NonNull String filename, long fallBackValue) {
        MediaExtractor extractor = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(filename);
            final int trackIndex = selectTrack(extractor, "video/");
            if (trackIndex == -1) {
                throw new IllegalArgumentException("No tracks found for mime type VIDEO");
            }

            extractor.selectTrack(trackIndex);
            final MediaFormat format = extractor.getTrackFormat(trackIndex);

            if (format.containsKey(MediaFormat.KEY_DURATION)) {
                final long keyDurationMicroSec = format.getLong(MediaFormat.KEY_DURATION);
                return Math.round((double) keyDurationMicroSec / 1000.0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (extractor != null) {
                extractor.release();
            }
        }

        return fallBackValue;
    }

    public static int extractAudioSampleRate(@NonNull String filename, int fallBackValue) {
        MediaExtractor extractor = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(filename);
            final int trackIndex = selectTrack(extractor, "audio/");
            if (trackIndex == -1) {
                throw new IllegalArgumentException("No tracks found for mime type AUDIO");
            }

            extractor.selectTrack(trackIndex);
            final MediaFormat format = extractor.getTrackFormat(trackIndex);

            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                return format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (extractor != null) {
                extractor.release();
            }
        }

        return fallBackValue;
    }


    public static int selectTrack(@NonNull MediaExtractor extractor, @NonNull String mimePrefix) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimePrefix)) {
                return i;
            }
        }

        return 0;
    }
}
