package com.banuba.sdk.internal.gl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.banuba.sdk.internal.utils.Logger;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static com.banuba.sdk.internal.Constants.FLIP_HORIZONTALLY;
import static com.banuba.sdk.internal.Constants.FLIP_VERTICALLY;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class GlUtils {
    public static final PrecisionEnum PRECISION = PrecisionEnum.highp;

    public static final int MATRIX_SIZE = 16;
    public static final int FLOAT_SIZE = 4;
    public static final int SHORT_SIZE = 2;
    public static final int INT_SIZE = 4;

    public static final int OFFSET_ZERO = 0;
    public static final int COUNT_ONE = 1;
    public static final int DEPTH_NEAR = -100;
    public static final int DEPTH_FAR = 100;

    public static final int VERTEX_PER_FACE = 3;
    public static final int COORDS_PER_VERTEX = 3;
    public static final int COORDS_UV_PER_TEXTURE = 2;

    public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * FLOAT_SIZE;
    public static final int TEXTURE_STRIDE = COORDS_UV_PER_TEXTURE * FLOAT_SIZE;

    public static final int GL_DEFAULT_RENDER_BUFFER = 0;
    public static final int GL_DEFAULT_TEXTURE_ID = 0;
    public static final int GL_DEFAULT_PROGRAM_ID = 0;

    public static final int GL_TEXTURE0 = 0;
    public static final int GL_TEXTURE1 = 1;
    public static final int GL_TEXTURE2 = 2;
    public static final int GL_TEXTURE3 = 3;
    public static final int GL_TEXTURE4 = 4;

    public static final float MAX_COLOR_FLOAT_VALUE = 255.0f;

    public static final float DEFAULT_ALPHA = 1.0f;

    private static final int GL_ES_20_INT = 0x20000;

    private static final int MAX_VARYING_VECTORS = 0x8DFC;

    private static final float[] IDENTITY_MATRIX;

    private static final BitmapFactory.Options DEFAULT_BITMAP_OPTIONS;

    private static final boolean DETAILED_LOG = false;

    private static final int SIZEOF_FLOAT = 4;

    static {
        IDENTITY_MATRIX = new float[MATRIX_SIZE];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);

        DEFAULT_BITMAP_OPTIONS = new BitmapFactory.Options();
        DEFAULT_BITMAP_OPTIONS.inPreferredConfig = Bitmap.Config.ARGB_8888;
        DEFAULT_BITMAP_OPTIONS.inScaled = false;
        DEFAULT_BITMAP_OPTIONS.inPremultiplied = false;
        DEFAULT_BITMAP_OPTIONS.inMutable = false;
    }

    private enum PrecisionEnum { mediump,
                                 highp }

    private GlUtils() {
    }

    public static void copyMatrix(@NonNull float[] src, @NonNull float[] dest) {
        System.arraycopy(src, OFFSET_ZERO, dest, OFFSET_ZERO, MATRIX_SIZE);
    }

    public static float[] getIdentityMatrix() {
        return IDENTITY_MATRIX;
    }

    public static float[] getNewIdentityMatrix() {
        float temp[] = new float[MATRIX_SIZE];
        System.arraycopy(IDENTITY_MATRIX, OFFSET_ZERO, temp, OFFSET_ZERO, MATRIX_SIZE);
        return temp;
    }

    public static FloatBuffer initFloatBuffer(float[] inFloatArray) {
        FloatBuffer res = ByteBuffer.allocateDirect(inFloatArray.length * FLOAT_SIZE)
                              .order(ByteOrder.nativeOrder())
                              .asFloatBuffer();
        res.put(inFloatArray).position(0);
        return res;
    }

    public static void initColorArray(float[] colorArray, int iColor) {
        colorArray[0] = ((float) Color.red(iColor)) / MAX_COLOR_FLOAT_VALUE;
        colorArray[1] = ((float) Color.green(iColor)) / MAX_COLOR_FLOAT_VALUE;
        colorArray[2] = ((float) Color.blue(iColor)) / MAX_COLOR_FLOAT_VALUE;
        colorArray[3] = ((float) Color.alpha(iColor)) / MAX_COLOR_FLOAT_VALUE;
    }

    public static boolean detectOpenGLES20(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        return (info.reqGlEsVersion >= GL_ES_20_INT);
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlErrorNoException("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Logger.e(
                "Could not compile shader %1$d: %2$s",
                shaderType,
                GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }


    /*public static int loadProgramFromAssets(@NonNull String vertexShaderPath, @NonNull String
    fragShaderPath) { return loadProgram(FileUtils.readFromAssets(vertexShaderPath),
    FileUtils.readFromAssets(fragShaderPath));
    }*/

    public static int loadProgram(@NonNull String vertShaderSrc, @NonNull String fragShaderSrc) {
        int vertexShader;
        int fragmentShader;
        int programObject;
        int[] linked = new int[1];

        // Load the vertex/fragment shaders
        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertShaderSrc);
        if (vertexShader == 0) {
            return 0;
        }

        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragShaderSrc);
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader);
            return 0;
        }

        // Create the program object
        programObject = GLES20.glCreateProgram();

        if (programObject == 0) {
            return 0;
        }

        GLES20.glAttachShader(programObject, vertexShader);
        GLES20.glAttachShader(programObject, fragmentShader);

        // Link the program
        GLES20.glLinkProgram(programObject);

        // Check the link status
        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Logger.e("Error linking program: %s", GLES20.glGetProgramInfoLog(programObject));
            GLES20.glDeleteProgram(programObject);
            return 0;
        }

        // Free up no longer needed shader resources
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return programObject;
    }

    public static int loadTextureFromFile(@NonNull File file) {
        final Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), DEFAULT_BITMAP_OPTIONS);
        if (bitmap != null) {
            return loadTextureInternal(bitmap, false);
        }
        return 0;
    }

    public static int loadTextureFromBitmap(@NonNull Bitmap bitmap) {
        return loadTextureInternal(bitmap, false);
    }

    public static int loadTextureFromRawRes(@RawRes int resID, Context context) {
        final Bitmap bitmap = BitmapFactory.decodeStream(
            context.getResources().openRawResource(resID), null, DEFAULT_BITMAP_OPTIONS);
        return loadTextureInternal(bitmap, true);
    }

    public static int createTextureFromDrawable(Drawable drawable, int width, int height) {
        final Bitmap bitmap =
            Bitmap.createBitmap(width, height, DEFAULT_BITMAP_OPTIONS.inPreferredConfig);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return loadTextureInternal(bitmap, true);
    }

    private static int loadTextureInternal(@NonNull Bitmap bitmap, boolean throwException) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE); // Set U Wrapping
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE); // Set V Wrapping

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            //GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (throwException && textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static int
    loadTextureLuminance(@NonNull ByteBuffer buffer, int width, int height, int format) {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE); // Set U Wrapping
            GLES20.glTexParameterf(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE); // Set V Wrapping

            // Load the bitmap into the bound texture.
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                format,
                width,
                height,
                0,
                format,
                GLES20.GL_UNSIGNED_BYTE,
                buffer);
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static int createExternalTextureObject() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGlErrorNoException("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
        checkGlErrorNoException("glBindTexture " + texId);

        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGlErrorNoException("glTexParameter");

        return texId;
    }

    public static void setupBlend() {
        try {
            // based on http://www.andersriggelsen.dk/glblendfunc.php
            GLES20.glBlendFuncSeparate(
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA,
                GLES20.GL_SRC_ALPHA,
                GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glBlendEquationSeparate(GLES20.GL_FUNC_ADD, GLES20.GL_FUNC_ADD);
            // int srcRGB = GL_SRC_ALPHA
            // int dstRGB = GL_ONE_MINUS_SRC_ALPHA
            // int srcAlpha = GL_ONE
            // int dstAlpha = GL_ONE_MINUS_SRC_ALPHA
            // http://learningwebgl.com/blog/?p=859
        } catch (Exception e) {
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /*public static void saveGLScreen(int width, int height, Context context) {
        byte[] bytes = new byte[width * height * 4];
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        GLES20.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        final String filename = context.getExternalFilesDir(null) + File.separator +
    DateUtils.getTimeBasedFileName("frame", "png"); try { FileOutputStream out = new
    FileOutputStream(filename); bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); out.close();
        } catch (Exception e) {
            Logger.wtf(e);
        }
        bitmap.recycle();
    }*/

    public static void printGlInfo() {
        int[] array = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, array, 0);
        Logger.i(" Open GL Max Texture Size = " + array[0]);

        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, array, 0);
        Logger.i("INFO", " Open GL Max Texture Units = " + array[0]);

        GLES20.glGetIntegerv(MAX_VARYING_VECTORS, array, 0);
        Logger.i(
            "INFO",
            " Open GL Max Varing Vectors(vec4) = " + array[0] + " or " + 4 * array[0] + " floats ");
    }

    public static int setupVertexBuffer(float vertexArray[]) {
        final FloatBuffer vertexBuffer = createFloatBuffer(vertexArray);

        final int vbo[] = new int[1];
        GLES20.glGenBuffers(1, vbo, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexArray.length * FLOAT_SIZE,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        return vbo[0];
    }


    public static int[] setupVertexTextureBuffers(float vertexArray[], float textureArray[]) {
        final FloatBuffer vertexBuffer = createFloatBuffer(vertexArray);
        final FloatBuffer textureBuffer = createFloatBuffer(textureArray);

        final int vbo[] = new int[2];
        GLES20.glGenBuffers(2, vbo, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexArray.length * FLOAT_SIZE,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[1]);
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            textureArray.length * FLOAT_SIZE,
            textureBuffer,
            GLES20.GL_STATIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    public static void multiplyMM(float[] result, float[] lhs, float[] rhs) {
        Matrix.multiplyMM(result, OFFSET_ZERO, lhs, OFFSET_ZERO, rhs, OFFSET_ZERO);
    }

    public static void
    multiplyMM4(float[] result, float[] mat1, float[] mat2, float[] mat3, float[] mat4) {
        float[] res12 = new float[MATRIX_SIZE];
        float[] res123 = new float[MATRIX_SIZE];

        Matrix.multiplyMM(res12, OFFSET_ZERO, mat1, OFFSET_ZERO, mat2, OFFSET_ZERO);
        Matrix.multiplyMM(res123, OFFSET_ZERO, res12, OFFSET_ZERO, mat3, OFFSET_ZERO);
        Matrix.multiplyMM(result, OFFSET_ZERO, res123, OFFSET_ZERO, mat4, OFFSET_ZERO);
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error) + " (" + GLU.gluErrorString(error) + ")";
            Logger.e(msg);
            throw new RuntimeException(msg);
        }
    }

    public static void checkGlErrorNoException(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error) + " (" + GLU.gluErrorString(error) + ")";
            Logger.e(msg);
        }
    }

    public static void setupSampler(int samplerIndex, int location, int texture, boolean external) {
        int glTexture = GLES20.GL_TEXTURE0;
        int glUniformX = GL_TEXTURE0;

        switch (samplerIndex) {
            case 1:
                glTexture = GLES20.GL_TEXTURE1;
                glUniformX = GL_TEXTURE1;
                break;
            case 2:
                glTexture = GLES20.GL_TEXTURE2;
                glUniformX = GL_TEXTURE2;
                break;
            case 3:
                glTexture = GLES20.GL_TEXTURE3;
                glUniformX = GL_TEXTURE3;
                break;
            case 4:
                glTexture = GLES20.GL_TEXTURE4;
                glUniformX = GL_TEXTURE4;
                break;
        }

        final int target = external ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES : GLES20.GL_TEXTURE_2D;

        GLES20.glActiveTexture(glTexture);
        GLES20.glBindTexture(target, texture);
        GLES20.glUniform1i(location, glUniformX);
    }


    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        checkGlErrorNoException("glCreateProgram");
        if (program == 0) {
            Logger.e("Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlErrorNoException("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
        checkGlErrorNoException("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Logger.e("Could not link program: %s", GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }


    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     * <p/>
     * Throws a RuntimeException if the location is invalid.
     */
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Creates a texture from raw data.
     *
     * @param data   Image data, in a "direct" ByteBuffer.
     * @param width  Texture width, in pixels (not bytes).
     * @param height Texture height, in pixels.
     * @param format Image data format (use constant appropriate for glTexImage2D(), e.g. GL_RGBA).
     * @return Handle to texture.
     */
    public static int createImageTexture(ByteBuffer data, int width, int height, int format) {
        int[] textureHandles = new int[1];
        int textureHandle;

        GLES20.glGenTextures(1, textureHandles, 0);
        textureHandle = textureHandles[0];
        checkGlErrorNoException("glGenTextures");

        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        checkGlErrorNoException("loadImageTexture");

        // Load the data from the buffer into the texture handle.
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            /*level*/ 0,
            format,
            width,
            height,
            /*border*/ 0,
            format,
            GLES20.GL_UNSIGNED_BYTE,
            data);
        checkGlErrorNoException("loadImageTexture");

        return textureHandle;
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Writes GL version info to the log.
     */
    public static void logVersionInfo() {
        Logger.i(
            "vendor: %1$s, renderer: %2$s, version: %3$s",
            GLES20.glGetString(GLES20.GL_VENDOR),
            GLES20.glGetString(GLES20.GL_RENDERER),
            GLES20.glGetString(GLES20.GL_VERSION));

        if (DETAILED_LOG) {
            int[] values = new int[1];
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
            int majorVersion = values[0];
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
            int minorVersion = values[0];
            if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
                Logger.i("iversion: %1$d.%2$d", majorVersion, minorVersion);
            }
        }
    }

    private static final float TEXTURE_CENTER = 0.5f;

    public static void calculateCameraMatrix(float[] matrix, float angle, int flip) {
        float rotate[] = new float[MATRIX_SIZE];
        float transPos[] = new float[MATRIX_SIZE];
        float transNeg[] = new float[MATRIX_SIZE];
        float temp[] = new float[MATRIX_SIZE];
        float temp2[] = new float[MATRIX_SIZE];
        float scale[] = new float[MATRIX_SIZE];

        Matrix.setIdentityM(scale, 0);
        if (flip == FLIP_HORIZONTALLY) {
            Matrix.scaleM(scale, 0, -1, 1, 1);
        } else if (flip == FLIP_VERTICALLY) {
            Matrix.scaleM(scale, 0, 1, -1, 1);
        }

        Matrix.setIdentityM(transPos, 0);
        Matrix.setIdentityM(transNeg, 0);
        Matrix.setIdentityM(rotate, 0);

        Matrix.translateM(transPos, 0, TEXTURE_CENTER, TEXTURE_CENTER, 0);
        Matrix.translateM(transNeg, 0, -TEXTURE_CENTER, -TEXTURE_CENTER, 0);

        Matrix.setRotateM(rotate, 0, angle, 0, 0, 1);

        Matrix.multiplyMM(temp, 0, transPos, 0, rotate, 0);
        Matrix.multiplyMM(temp2, 0, temp, 0, scale, 0);
        Matrix.multiplyMM(matrix, 0, temp2, 0, transNeg, 0);
    }
}
