package io.github.xiaomimarket.spoof;

import java.util.Map;

/**
 * Device identity that Xiaomi GetApps should observe inside its own process.
 * <p>
 * The combination was verified on a REDMI K90 Pro Max (25102RKBEC / myron):
 * switching the store-visible manufacturer/brand, device and model to these
 * values lifted GetApps download throttling. This module never writes global
 * {@code getprop} values.
 */
final class DeviceIdentity {
    static final String BRAND = "Xiaomi";
    static final String MANUFACTURER = "Xiaomi";
    static final String DEVICE = "madrid";
    static final String PRODUCT = "madrid";
    static final String MODEL = "2611FPNFAG";
    static final String MARKET_PACKAGE = "com.xiaomi.market";

    private static final Map<String, String> EXACT_PROPERTIES = Map.of(
            "ro.product.brand", BRAND,
            "ro.product.manufacturer", MANUFACTURER,
            "ro.product.device", DEVICE,
            "ro.product.name", PRODUCT,
            "ro.product.model", MODEL,
            "ro.build.product", PRODUCT,
            "ro.product.mod_device", DEVICE
    );

    private DeviceIdentity() {
    }

    static String describe() {
        return MANUFACTURER + " / " + DEVICE + " / " + MODEL;
    }

    static String spoofedOrNull(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String exact = EXACT_PROPERTIES.get(key);
        if (exact != null) {
            return exact;
        }
        if (!key.startsWith("ro.product.")) {
            return null;
        }
        int lastDot = key.lastIndexOf('.');
        String leaf = key.substring(lastDot + 1);
        return switch (leaf) {
            case "brand" -> BRAND;
            case "manufacturer" -> MANUFACTURER;
            case "device" -> DEVICE;
            case "name" -> PRODUCT;
            case "model" -> MODEL;
            default -> null;
        };
    }
}
