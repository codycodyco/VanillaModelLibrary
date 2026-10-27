package dev.cody.vanillamobs.assets;

/** A delivery that must not be installed. The previous install is left untouched. */
public final class AssetException extends Exception {

    public AssetException(String message) {
        super(message);
    }

    public AssetException(String message, Throwable cause) {
        super(message, cause);
    }
}
