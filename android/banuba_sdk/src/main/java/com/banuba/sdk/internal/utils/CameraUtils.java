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
package com.banuba.sdk.internal.utils;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.util.Pair;
import android.util.Size;
import android.util.SizeF;

import androidx.annotation.NonNull;

import com.banuba.sdk.internal.camera.PreviewSizeComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static com.banuba.sdk.internal.Constants.ASPECT_H;
import static com.banuba.sdk.internal.Constants.ASPECT_W;
import static com.banuba.sdk.internal.Constants.FALLBACK_PREVIEW_SIZE;

@SuppressWarnings("deprecation")
public final class CameraUtils {
    private static final int DEFAULT_CAMERA_ORIENTATION = 270;

    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final int MAX_PREVIEW_WIDTH_LOW = 640;
    private static final int MAX_PREVIEW_HEIGHT_LOW = 480;

    private static final double DEFAULT_CAMERA_FOV = 55;

    private CameraUtils() {
    }

    public static int getSensorOrientation(@NonNull CameraCharacteristics characteristics) {
        final Integer sensorOrientationRaw =
            characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return sensorOrientationRaw != null
            ? sensorOrientationRaw
            : DEFAULT_CAMERA_ORIENTATION; // Default FRONT Camera orientation
    }

    public static int getLensFacing(@NonNull CameraCharacteristics characteristics) {
        final Integer facingRaw = characteristics.get(CameraCharacteristics.LENS_FACING);
        return facingRaw != null ? facingRaw : LENS_FACING_FRONT;
    }

    public static double getFov(@NonNull CameraCharacteristics characteristics) {
        final float[] focalLengths =
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        final SizeF sensorSize =
            characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        if (sensorSize != null && focalLengths != null && focalLengths.length > 0) {
            return Math.toDegrees(2 * Math.atan(0.5f * sensorSize.getWidth() / focalLengths[0]));
        }
        return DEFAULT_CAMERA_FOV;
    }

    public static Size getPreviewSize(
        @NonNull final CameraCharacteristics characteristics,
        @NonNull final Size maxPreviewSize) {
        final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            final List<Size> availableSizes = new ArrayList<>(10);
            final Size[] supportedSizes = map.getOutputSizes(ImageReader.class);
            for (Size size : supportedSizes) {
                final Pair<Integer, Integer> aspect = DisplayUtils.getAspectRatio(size.getWidth(), size.getHeight());
                if (aspect.first == ASPECT_W && aspect.second == ASPECT_H
                    && size.getWidth() <= maxPreviewSize.getWidth() && size.getHeight() <= maxPreviewSize.getHeight()) {
                    availableSizes.add(size);
                }
            }
            return (availableSizes.isEmpty()) ? FALLBACK_PREVIEW_SIZE : Collections.max(availableSizes, new PreviewSizeComparator());
        }
        return FALLBACK_PREVIEW_SIZE;
    }

    public static Size getHighResPhotoSize(@NonNull CameraCharacteristics characteristics) {
        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            final List<Size> availableSizes = new ArrayList<>();
            final Size[] sizesSurfaceTexture = map.getOutputSizes(ImageFormat.YUV_420_888);
            for (Size size : sizesSurfaceTexture) {
                final Pair<Integer, Integer> aspect =
                    DisplayUtils.getAspectRatio(size.getWidth(), size.getHeight());
                if (aspect.first == ASPECT_W && aspect.second == ASPECT_H) {
                    availableSizes.add(size);
                }
            }
            return (availableSizes.isEmpty()) ? FALLBACK_PREVIEW_SIZE : Collections.max(availableSizes, new PreviewSizeComparator());
        }
        return FALLBACK_PREVIEW_SIZE;
    }

    public static String getCameraInfo(CameraCharacteristics characteristics) {
        final StringBuilder sb = new StringBuilder();

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        sb.append(Build.MANUFACTURER);
        sb.append(" ");
        sb.append(Build.MODEL);
        sb.append("\nCamera 2 API\n");

        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null) {
            sb.append("Facing = ");
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                sb.append("BACK");
            } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                sb.append("FRONT");
            } else if (facing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                sb.append("EXTERNAL");
            } else {
                sb.append("UNKNOWN");
            }
            sb.append("\n");
        } else {
            sb.append("Facing = NO DATA\n");
        }

        sb.append("SensorOrientation = ").append(sensorOrientation).append("\n");

        final Integer support =
            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (support != null) {
            if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                sb.append("Level: LEGACY");
            } else if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) {
                sb.append("Level: HARDWARE_LIMITED");
            } else if (support == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) {
                sb.append("Level: HARDWARE_FULL");
            }
        } else {
            sb.append("Camera2 support level: UNKNOWN");
        }

        sb.append("\n\n");

        // http://andrew.hedges.name/experiments/aspect_ratio/
        if (map != null) {
            final Size[] sizesSurfaceTexture = map.getOutputSizes(SurfaceTexture.class);
            if (sizesSurfaceTexture != null) {
                for (Size size : sizesSurfaceTexture) {
                    final Pair<Integer, Integer> aspect =
                        DisplayUtils.getAspectRatio(size.getWidth(), size.getHeight());
                    sb.append("Size: ")
                        .append(size.getWidth())
                        .append("x")
                        .append(size.getHeight())
                        .append("  ");
                    sb.append(aspect.first).append(":").append(aspect.second).append("\n");
                }
            }
        }

        final float[] focalLengths =
            characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        final SizeF sensorSize =
            characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

        if (sensorSize != null && focalLengths != null) {
            sb.append("\nSensor Size: ");
            sb.append(String.format(Locale.ENGLISH, "%.2f", sensorSize.getWidth()));
            sb.append(" x ");
            sb.append(String.format(Locale.ENGLISH, "%.2f", sensorSize.getHeight()));
            sb.append(" mm\n");


            // http://stackoverflow.com/questions/40511641/how-reliable-is-android-camera2-cameracharacteristics-api-for-sensor-size
            // http://photo.stackexchange.com/questions/54054/calculating-the-field-of-view-for-a-nexus-5

            @SuppressWarnings("MagicNumber")
            final double k = 0.5;
            double w = k * sensorSize.getWidth();
            double h = k * sensorSize.getHeight();

            for (float focalLength : focalLengths) {
                double horizontalAngle = Math.toDegrees(2 * Math.atan(w / focalLength));
                double verticalAngle = Math.toDegrees(2 * Math.atan(h / focalLength));
                sb.append("FOV: H = ");
                sb.append(String.format(Locale.ENGLISH, "%.1f", horizontalAngle));
                sb.append(" V = ");
                sb.append(String.format(Locale.ENGLISH, "%.1f", verticalAngle));
            }
        }

        return sb.toString();
    }
}
