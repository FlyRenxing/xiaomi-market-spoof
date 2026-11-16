# libxposed 注解只在编译期使用。
-dontwarn io.github.libxposed.annotation.**
# 混淆入口类后，同步改写 java_init.list 中的类名。
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
