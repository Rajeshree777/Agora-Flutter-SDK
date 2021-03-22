package com.banuba.sdk.manager;

import java.io.File;

/**
 * @addtogroup java
 * @{
 */

/**
 * Encapsulates info about effect.
 */
public class EffectInfo {
    public EffectInfo(String path) {
        String dirname = new File(path).getName();

        // TODO: parse effect meta
        mName = dirname;

        mPath = path;
    }

    /**
     * @return name of the effect.
     */
    public String getName() {
        return mName;
    }

    /**
     * @return path to effect. Pass this value to `EffectPlayer.loadEffect`
     */
    public String getPath() {
        return mPath;
    }

    /**
     * @return path to effect preview image.
     */
    public String previewImage() {
        return new File(mPath, "preview.png").getPath();
    }

    private String mPath;
    private String mName;
}

/** @} */ // endgroup
