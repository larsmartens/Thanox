package github.tornaco.android.thanos.services.xposed.compat;

import io.github.libxposed.api.XposedInterface;

public final class RuntimeHolder {
    private static volatile XposedInterface xposed;

    private RuntimeHolder() {
    }

    public static void setXposed(XposedInterface xposedInterface) {
        xposed = xposedInterface;
    }

    public static XposedInterface getXposed() {
        return xposed;
    }

    public static XposedInterface requireXposed() {
        XposedInterface value = xposed;
        if (value == null) {
            throw new IllegalStateException("Xposed runtime not initialized");
        }
        return value;
    }
}
