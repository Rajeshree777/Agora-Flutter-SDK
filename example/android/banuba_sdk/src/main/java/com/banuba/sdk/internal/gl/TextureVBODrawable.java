// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.gl;

@SuppressWarnings("WeakerAccess")
public interface TextureVBODrawable extends AutoCloseable {
    void draw(
        int count,
        float[] matrixMVP,
        float[] matrixTexture,
        int textureID,
        int vertexBufferId,
        int textureBufferId,
        float alpha);
}
