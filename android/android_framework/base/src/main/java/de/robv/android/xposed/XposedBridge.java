package de.robv.android.xposed;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import github.tornaco.android.thanos.services.xposed.compat.Api101Bridge;
import io.github.libxposed.api.XposedInterface;

public final class XposedBridge {
    public static final ClassLoader BOOTCLASSLOADER = ClassLoader.getSystemClassLoader();

    private XposedBridge() {
    }

    public static void log(String text) {
        Api101Bridge.log(Log.INFO, "XposedBridge", text, null);
    }

    public static void log(Throwable t) {
        Api101Bridge.log(Log.ERROR, "XposedBridge", Log.getStackTraceString(t), t);
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        return Api101Bridge.hookMember(hookMethod, callback);
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new LinkedHashSet<>();
        for (Method method : hookClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                method.setAccessible(true);
                unhooks.add(hookMethod(method, callback));
            }
        }
        return unhooks;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new LinkedHashSet<>();
        for (Constructor<?> constructor : hookClass.getDeclaredConstructors()) {
            constructor.setAccessible(true);
            unhooks.add(hookMethod(constructor, callback));
        }
        return unhooks;
    }
}
