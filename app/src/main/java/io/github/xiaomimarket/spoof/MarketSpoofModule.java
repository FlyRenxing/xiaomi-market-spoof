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
 * LSPosed 现代 API 102 入口。作用域仅限 {@code com.xiaomi.market}。
 * <p>
 * 模块一加载进商店进程就会安装钩子，后续商店代码读取 {@link Build}
 * 或 {@code SystemProperties} 时，会看到伪装后的小米 18 Pro Max 身份，
 * 从而避开按机型对下载管理器的限速。
 */
public final class MarketSpoofModule extends XposedModule {
    static final String TAG = "MarketSpoof";

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        if (param.isSystemServer()) {
            log(Log.WARN, TAG, "加载到 system_server，正在脱离");
            detach();
            return;
        }
        log(Log.INFO, TAG, "onModuleLoaded 进程=" + param.getProcessName()
                + " 框架=" + getFrameworkName()
                + " " + getFrameworkVersion()
                + " api=" + getApiVersion());
        applyBuildFields();
        installPropertyHooks();
    }

    @Override
    public void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        if (!DeviceIdentity.MARKET_PACKAGE.equals(param.getPackageName())) {
            log(Log.INFO, TAG, "忽略包名 " + param.getPackageName());
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
                + " 伪装=" + DeviceIdentity.describe());
        applyBuildFields();
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        log(Log.INFO, TAG, "onHotReloading");
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        log(Log.INFO, TAG, "onHotReloaded 进程=" + param.getProcessName()
                + " 旧钩子数量=" + param.getOldHookHandles().size());
        param.getOldHookHandles().forEach(XposedInterface.HookHandle::unhook);
        applyBuildFields();
        installPropertyHooks();
    }

    private void installPropertyHooks() {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            hookStringGetter(systemProperties, "get", String.class);
            hookStringGetter(systemProperties, "get", String.class, String.class);
            log(Log.INFO, TAG, "SystemProperties 钩子已安装，伪装=" + DeviceIdentity.describe());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "挂钩 SystemProperties 失败", t);
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
        log(Log.INFO, TAG, "Build 字段现在 MODEL=" + Build.MODEL
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
            log(Log.INFO, TAG, "miui.os.Build 字段已更新");
        } catch (ClassNotFoundException ignored) {
            // 纯 AOSP 进程没有 MIUI 辅助类。
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
            log(Log.ERROR, TAG, "设置 " + owner.getName() + "." + name + " 失败", t);
        }
    }

    private static void clearFinalFlag(Field field) {
        try {
            Field accessFlags = Field.class.getDeclaredField("accessFlags");
            accessFlags.setAccessible(true);
            accessFlags.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (NoSuchFieldException ignored) {
            // ART 可能不暴露这个 Java 层字段；Field.set 仍有可能成功。
        } catch (IllegalAccessException ignored) {
            // 尽力而为。主要路径仍是 SystemProperties 钩子。
        }
    }
}
