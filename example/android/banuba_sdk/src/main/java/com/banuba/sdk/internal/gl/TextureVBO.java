// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.gl;

import android.opengl.GLES20;

import static com.banuba.sdk.internal.gl.GlUtils.COORDS_PER_VERTEX;
import static com.banuba.sdk.internal.gl.GlUtils.COORDS_UV_PER_TEXTURE;
import static com.banuba.sdk.internal.gl.GlUtils.COUNT_ONE;
import static com.banuba.sdk.internal.gl.GlUtils.GL_DEFAULT_PROGRAM_ID;
import static com.banuba.sdk.internal.gl.GlUtils.OFFSET_ZERO;
import static com.banuba.sdk.internal.gl.GlUtils.TEXTURE_STRIDE;
import static com.banuba.sdk.internal.gl.GlUtils.VERTEX_STRIDE;

@SuppressWarnings("WeakerAccess")
public class  TextureVBO implements TextureVBODrawable {
    private static final String SHADER_VEC = " "
                                             + "  uniform mat4 u_MVPMatrix;                     \n"
                                             + "  uniform mat4 uTexMatrix;                      \n"
                                             + "  attribute vec4 a_position;                    \n"
                                             + "  attribute vec2 a_texCoord;                    \n"
                                             + "  varying vec2 v_texCoord;                      \n"
                                             + "  void main()                                   \n"
                                             + "  {                                             \n"
                                             + "     gl_Position = u_MVPMatrix * a_position;    \n"
                                             + "     vec4 texCoord = vec4(a_texCoord, 0.0, 1.0);\n"
                                             + "     v_texCoord = (uTexMatrix * texCoord).xy;   \n"
                                             + "  }                                             \n";

    private static final String SHADER_FRAG =
        " "
        + "  precision mediump float;                             \n"
        + "  varying vec2 v_texCoord;                             \n"
        + "  uniform sampler2D s_baseMap;                         \n"
        + "  uniform float u_alpha;                               \n"
        + "  void main()                                          \n"
        + "  {                                                    \n"
        + "     vec4 color = texture2D(s_baseMap, v_texCoord);    \n"
        + "     gl_FragColor = vec4(color.rgb, color.a * u_alpha);\n"
        + "  }                                                    \n";

    private final int mProgramHandle;
    private final int mAttributePosition;
    private final int mAttributeTextureCoord;
    private final int mUniformMVPMatrix;
    private final int mUniformSampler;
    private final int mUniformTextureMatrix;
    private final int mUniformAlpha;
    private final boolean mExternalTexture;

    public TextureVBO(boolean externalTexture) {
        final String fragment = externalTexture
                                    ? "#extension GL_OES_EGL_image_external : require\n"
                                          + SHADER_FRAG.replace("sampler2D", "samplerExternalOES")
                                    : SHADER_FRAG;

        mExternalTexture = externalTexture;

        mProgramHandle = GlUtils.loadProgram(SHADER_VEC, fragment);

        // Vertex shader
        mAttributePosition = GLES20.glGetAttribLocation(mProgramHandle, "a_position");
        mAttributeTextureCoord = GLES20.glGetAttribLocation(mProgramHandle, "a_texCoord");

        mUniformMVPMatrix = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mUniformTextureMatrix = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");

        // Fragment Shader
        mUniformSampler = GLES20.glGetUniformLocation(mProgramHandle, "s_baseMap");
        mUniformAlpha = GLES20.glGetUniformLocation(mProgramHandle, "u_alpha");
    }


    @Override
    public void draw(
        int vertexCount,
        float[] matrixMVP,
        float[] matrixTexture,
        int textureID,
        int vertexBufferId,
        int textureBufferId,
        float alpha) {
        GLES20.glUseProgram(mProgramHandle);

        // Vertex Shader Buffers
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        GLES20.glVertexAttribPointer(
            mAttributePosition,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE,
            OFFSET_ZERO);
        GLES20.glEnableVertexAttribArray(mAttributePosition);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureBufferId);
        GLES20.glVertexAttribPointer(
            mAttributeTextureCoord,
            COORDS_UV_PER_TEXTURE,
            GLES20.GL_FLOAT,
            false,
            TEXTURE_STRIDE,
            OFFSET_ZERO);
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoord);

        // Vertex Shader - Uniforms
        GLES20.glUniformMatrix4fv(mUniformMVPMatrix, COUNT_ONE, false, matrixMVP, OFFSET_ZERO);
        GLES20.glUniformMatrix4fv(
            mUniformTextureMatrix, COUNT_ONE, false, matrixTexture, OFFSET_ZERO);

        // Fragment Shader - Texture
        GlUtils.setupSampler(0, mUniformSampler, textureID, mExternalTexture);

        // Fragment Shader - Alpha
        GLES20.glUniform1f(mUniformAlpha, alpha);

        // Drawing
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, OFFSET_ZERO, vertexCount);

        // Clearing
        GLES20.glDisableVertexAttribArray(mAttributePosition);
        GLES20.glDisableVertexAttribArray(mAttributeTextureCoord);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glUseProgram(GL_DEFAULT_PROGRAM_ID);
    }

    @Override
    public void close() {
        GLES20.glDeleteProgram(mProgramHandle);
    }
}
