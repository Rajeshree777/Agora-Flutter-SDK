package com.banuba.sdk.manager;

/**
 * Autorotation handler interface.
 */
@FunctionalInterface
public interface IAutoRotationHandler {
    /**
     * Is autorotation is OFF or not.
     *
     * @return true/false in the case of autorotation is OFF or not.
     */
    boolean isAutoRotationOff();
}
