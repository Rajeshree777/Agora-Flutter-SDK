package com.banuba.sdk.manager;

import androidx.annotation.Nullable;

import com.banuba.sdk.camera.Facing;

/**
 * Encapsulates info about Banuba SDK Manager ({@link BanubaSdkManager}) Configuration.
 */
final public class BanubaSdkManagerConfiguration {
    private final boolean mIsMirrored;
    private final Facing mFacing;
    @Nullable
    private final IResolutionController mResolutionController;
    @Nullable
    private final IFpsController mFpsController;
    @Nullable
    private final IAutoRotationHandler mAutoRotationHandler;

    private BanubaSdkManagerConfiguration(Builder builder) {
        this.mIsMirrored = builder.mIsMirrored;
        this.mFacing = builder.mFacing;
        this.mResolutionController = builder.mResolutionController;
        this.mFpsController = builder.mFpsController;
        this.mAutoRotationHandler = builder.mAutoRotationHandler;
    }

    /**
     * Return is camera should be mirrored value.
     *
     * @return image from camera should be mirrored.
     */
    public boolean isFacingMirrored() {
        return mIsMirrored;
    }

    /**
     * Get camera facing.
     *
     * @return camera {@link Facing} value.
     */
    public Facing getFacing() {
        return mFacing;
    }

    /**
     * Get resolution controlller.
     *
     * @return {@link IResolutionController}.
     */
    @Nullable
    public IResolutionController getResolutionController() {
        return mResolutionController;
    }

    /**
     * Get FPS controller.
     *
     * @return {@link IFpsController}.
     */
    @Nullable
    public IFpsController getFpsController() {
        return mFpsController;
    }

    /**
     * Get auto rotation handler.
     *
     * @return {@link IAutoRotationHandler}.
     */
    @Nullable
    public IAutoRotationHandler getAutoRotationHandler() {
        return mAutoRotationHandler;
    }

    /**
     * Create new {@link Builder} instance.
     *
     * @return {@link Builder} instance.
     */
    public static Builder newInstance() {
        return new Builder();
    }

    /**
     * Create new {@link Builder} instance.
     *
     * @param defaultFacing Default {@link Facing} value.
     * @param isFacingMirrored Is mirroring required value.
     * @return {@link Builder} instance.
     */
    public static Builder newInstance(Facing defaultFacing, boolean isFacingMirrored) {
        return new Builder(defaultFacing, isFacingMirrored);
    }

    public static class Builder {
        private boolean mIsMirrored = true;
        private Facing mFacing = Facing.FRONT;
        @Nullable
        private IResolutionController mResolutionController;
        @Nullable
        private IFpsController mFpsController;
        @Nullable
        private IAutoRotationHandler mAutoRotationHandler;

        private Builder() {
        }

        private Builder(Facing defaultFacing, boolean isFacingMirrored) {
            this.mFacing = defaultFacing;
            this.mIsMirrored = isFacingMirrored;
        }

        /**
         * Set is mirroring required.
         *
         * @param isFacingMirrored If mirroring required value.
         * @return {@link Builder} instance.
         */
        public Builder setFacingMirrored(boolean isFacingMirrored) {
            this.mIsMirrored = isFacingMirrored;
            return this;
        }

        /**
         * Set camere {@link Facing}.
         * 
         * @param facing Camera {@link Facing} value.
         * @return {@link Builder} instance.
         */
        public Builder setFacing(Facing facing) {
            this.mFacing = facing;
            return this;
        }

        /**
         * Set {@link IResolutionController}.
         *
         * @param controller {@link IResolutionController} value.
         * @return {@link Builder} instance.
         */
        public Builder setResolutionController(@Nullable IResolutionController controller) {
            this.mResolutionController = controller;
            return this;
        }

        /**
         * Set {@link IFpsController}.
         *
         * @param controller {@link IFpsController} value.
         * @return {@link Builder} instance.
         */
        public Builder setFpsController(@Nullable IFpsController controller) {
            this.mFpsController = controller;
            return this;
        }

        /**
         * Set {@link IAutoRotationHandler}.
         *
         * @param handler {@link IAutoRotationHandler} value.
         * @return {@link Builder} instance.
         */
        public Builder setAutoRotationHandler(@Nullable IAutoRotationHandler handler) {
            this.mAutoRotationHandler = handler;
            return this;
        }

        /**
         * Build {@link BanubaSdkManagerConfiguration} object.
         *
         * @return {@link BanubaSdkManagerConfiguration} object.
         */
        public BanubaSdkManagerConfiguration build() {
            return new BanubaSdkManagerConfiguration(this);
        }
    }
}
