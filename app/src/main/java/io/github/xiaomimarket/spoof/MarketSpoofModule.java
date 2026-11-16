package io.github.xiaomimarket.spoof;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/**
 * LSPosed modern API 102 entry. Scope is {@code com.xiaomi.market} only.
 * <p>
 * Hooks are installed as soon as the module is loaded into a GetApps process
 * so later store code that reads {@link Build} or {@code SystemProperties}
 * sees the spoofed Xiaomi 18 Pro Max identity.
 */
public final class MarketSpoofModule extends XposedModule {
    static final String TAG = "MarketSpoof";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        if (param.isSystemServer()) {
            log(Log.WARN, TAG, "loaded in system_server, detaching");
            detach();
            return;
        }
        log(Log.INFO, TAG, "onModuleLoaded process=" + param.getProcessName()
                + " framework=" + getFrameworkName()
                + " " + getFrameworkVersion()
                + " api=" + getApiVersion());
        applyBuildFields();
        installPropertyHooks();
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!DeviceIdentity.MARKET_PACKAGE.equals(param.getPackageName())) {
            log(Log.INFO, TAG, "ignoring package " + param.getPackageName());
            return;
        }
        log(Log.INFO, TAG, "onPackageLoaded " + param.getPackageName()
                + " first=" + param.isFirstPackage());
        applyBuildFields();
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!DeviceIdentity.MARKET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        log(Log.INFO, TAG, "onPackageReady classloader=" + param.getClassLoader()
                + " spoof=" + DeviceIdentity.describe());
        applyBuildFields();
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        log(Log.INFO, TAG, "onHotReloading");
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        log(Log.INFO, TAG, "onHotReloaded process=" + param.getProcessName()
                + " oldHooks=" + param.getOldHookHandles().size());
        param.getOldHookHandles().forEach(XposedInterface.HookHandle::unhook);
        applyBuildFields();
        installPropertyHooks();
    }

    private void installPropertyHooks() {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            hookStringGetter(systemProperties, "get", String.class);
            hookStringGetter(systemProperties, "get", String.class, String.class);
            log(Log.INFO, TAG, "SystemProperties hooks installed, spoof=" + DeviceIdentity.describe());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook SystemProperties", t);
        }
    }

    private void hookStringGetter(Class<?> owner, String name, Class<?>... parameterTypes) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameterTypes);
        String id = method.toGenericString();
        hook(method)
                .setId(id)
                .setPriority(XposedInterface.PRIORITY_HIGHEST)
                .intercept(chain -> {
                    String spoofed = DeviceIdentity.spoofedOrNull((String) chain.getArg(0));
                    if (spoofed != null) {
                        return spoofed;
                    }
                    return chain.proceed();
                });
    }

    private void applyBuildFields() {
        setStaticField(Build.class, "BRAND", DeviceIdentity.BRAND);
        setStaticField(Build.class, "MANUFACTURER", DeviceIdentity.MANUFACTURER);
        setStaticField(Build.class, "DEVICE", DeviceIdentity.DEVICE);
        setStaticField(Build.class, "PRODUCT", DeviceIdentity.PRODUCT);
        setStaticField(Build.class, "MODEL", DeviceIdentity.MODEL);
        applyMiuiBuildFields();
        log(Log.INFO, TAG, "Build fields now MODEL=" + Build.MODEL
                + " DEVICE=" + Build.DEVICE
                + " BRAND=" + Build.BRAND
                + " MANUFACTURER=" + Build.MANUFACTURER
                + " PRODUCT=" + Build.PRODUCT);
    }

    private void applyMiuiBuildFields() {
        try {
            Class<?> miuiBuild = Class.forName("miui.os.Build");
            setStaticField(miuiBuild, "DEVICE", DeviceIdentity.DEVICE);
            setStaticField(miuiBuild, "PRODUCT", DeviceIdentity.PRODUCT);
            setStaticField(miuiBuild, "MODEL", DeviceIdentity.MODEL);
            log(Log.INFO, TAG, "miui.os.Build fields updated");
        } catch (ClassNotFoundException ignored) {
            // AOSP-only processes do not ship MIUI helpers.
        }
    }

    private void setStaticField(Class<?> owner, String name, String value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            if (Modifier.isFinal(field.getModifiers())) {
                clearFinalFlag(field);
            }
            field.set(null, value);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to set " + owner.getName() + "." + name, t);
        }
    }

    private static void clearFinalFlag(Field field) {
        try {
            Field accessFlags = Field.class.getDeclaredField("accessFlags");
            accessFlags.setAccessible(true);
            accessFlags.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (NoSuchFieldException ignored) {
            // ART may not expose this Java-level field; Field.set can still succeed.
        } catch (IllegalAccessException ignored) {
            // Best-effort only. SystemProperties hooks remain the primary path.
        }
    }
}
