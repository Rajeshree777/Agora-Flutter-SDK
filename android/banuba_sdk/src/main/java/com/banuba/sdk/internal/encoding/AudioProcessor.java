package com.banuba.sdk.internal.encoding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ShortBuffer;

public final class AudioProcessor {
    static void processRecordsAudio(
        int recordsCount,
        @NonNull SparseArray<float[]> speedData,
        @NonNull SparseArray<ByteBuffer> audioData) {
        for (int w = 0; w < recordsCount; w++) {
            final ByteBuffer byteAudioOld = audioData.get(w);
            byteAudioOld.rewind();
            final CharBuffer audioOld = byteAudioOld.asCharBuffer();
            final ByteBuffer byteAudioNew = ByteBuffer.allocateDirect(2 * byteAudioOld.capacity())
                                                .order(ByteOrder.nativeOrder());
            final CharBuffer audioNew = byteAudioNew.asCharBuffer();

            audioData.put(w, byteAudioNew);

            final float[] speeds = speedData.get(w);

            audioOld.rewind();
            audioNew.rewind();

            processBuffer(audioOld, audioNew, speeds);
            byteAudioNew.rewind();
        }
    }

    @Nullable
    static ByteBuffer
    processCommonAudio(@NonNull float[] speeds, @Nullable ByteBuffer byteAudioOld) {
        if (byteAudioOld == null) {
            return null;
        }

        byteAudioOld.rewind();
        final CharBuffer audioOld = byteAudioOld.asCharBuffer();

        final ByteBuffer byteAudioNew =
            ByteBuffer.allocateDirect(2 * byteAudioOld.capacity()).order(ByteOrder.nativeOrder());
        final CharBuffer audioNew = byteAudioNew.asCharBuffer();

        audioOld.rewind();
        audioNew.rewind();

        processBuffer(audioOld, audioNew, speeds);

        byteAudioNew.rewind();

        return byteAudioNew;
    }


    private static void
    processBuffer(@NonNull CharBuffer audioOld, @NonNull CharBuffer audioNew, float[] speeds) {
        for (int s = 0; s < speeds.length; s++) {
            final int audioBlockLength = RecordingParams.getAudio1msBlockLength(s);
            final float currentSpeedValue = speeds[s];
            if (currentSpeedValue < 0.9f) {
                final int sampleRepeatCount = Math.round(1.0f / currentSpeedValue);
                for (int a = 0; a < audioBlockLength; a++) {
                    final char a1 = audioOld.get();
                    for (int r = 0; r < sampleRepeatCount; r++) {
                        audioNew.put(a1);
                    }
                }
            } else if (currentSpeedValue > 1.1f) {
                final int sampleDilationValue = Math.round(currentSpeedValue);
                for (int a = 0; a < audioBlockLength; a++) {
                    char a1 = audioOld.get();
                    if (a % sampleDilationValue == 0) {
                        audioNew.put(a1);
                    }
                }
            } else {
                for (int a = 0; a < audioBlockLength; a++) {
                    final char a1 = audioOld.get();
                    audioNew.put(a1);
                }
            }
        }
    }


    @NonNull
    public static byte[] processArrays(@NonNull byte[] audioOld, float speed) {
        if (speed >= 0.95f && speed <= 1.05f) {
            return audioOld;
        }

        final int inputSamples = audioOld.length / RecordingParams.getAudioFormatBytes();
        final double speedK = 1.0 / speed;
        final int newSize = (int) (audioOld.length * speedK);
        final int newSizeBlocked = newSize - newSize % 2;
        final byte[] result = new byte[newSizeBlocked];

        final ShortBuffer input =
            ByteBuffer.wrap(audioOld).order(ByteOrder.nativeOrder()).asShortBuffer();
        final ShortBuffer output =
            ByteBuffer.wrap(result).order(ByteOrder.nativeOrder()).asShortBuffer();

        if (speed < 0.9f) {
            final int sampleRepeatCount = Math.round(1.0f / speed);
            for (int a = 0; a < inputSamples; a++) {
                final short a1 = input.get();
                for (int r = 0; r < sampleRepeatCount; r++) {
                    output.put(a1);
                }
            }

        } else if (speed > 1.1f) {
            final int sampleDilationValue = Math.round(speed);
            for (int a = 0; a < inputSamples; a++) {
                final short a1 = input.get();
                if (a % sampleDilationValue == 0) {
                    output.put(a1);
                }
            }
        } else {
            for (int a = 0; a < inputSamples; a++) {
                final short a1 = input.get();
                output.put(a1);
            }
        }

        return result;
    }

    @NonNull
    static ByteBuffer convertStereoToMono(@NonNull ByteBuffer bInput, @NonNull ByteBuffer bOutput) {
        final CharBuffer input = bInput.order(ByteOrder.nativeOrder()).asCharBuffer();
        final CharBuffer output = bOutput.order(ByteOrder.nativeOrder()).asCharBuffer();

        final int size = input.remaining() / 2;
        for (int i = 0; i < size; i++) {
            output.put(input.get());
            input.get(); // Ignore right
        }

        bInput.rewind();
        bOutput.rewind();
        return bOutput;
    }
}
