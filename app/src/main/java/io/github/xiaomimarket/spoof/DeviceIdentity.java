package io.github.xiaomimarket.spoof;

import java.util.Map;

/**
 * 小米应用商店在本进程内应看到的设备身份。
 * <p>
 * 这组值在 REDMI K90 Pro Max（25102RKBEC / myron）上验证过：
 * 把商店可见的厂商、品牌、设备代号和型号改成这些值后，
 * 商店按机型对下载管理器的限速会解除。本模块从不写入全局 {@code getprop}。
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
