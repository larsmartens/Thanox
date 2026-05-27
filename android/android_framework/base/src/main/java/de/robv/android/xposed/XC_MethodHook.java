package de.robv.android.xposed;

import java.lang.reflect.Member;

public abstract class XC_MethodHook {
    public static class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        public Object result;
        public Throwable throwable;
        public boolean returnEarly;

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean hasThrowable() {
            return throwable != null;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            if (throwable != null) {
                this.result = null;
                this.returnEarly = true;
            }
        }
    }

    public class Unhook {
        private final Member hookedMethod;
        private final io.github.libxposed.api.XposedInterface.HookHandle handle;

        public Unhook(Member hookedMethod, io.github.libxposed.api.XposedInterface.HookHandle handle) {
            this.hookedMethod = hookedMethod;
            this.handle = handle;
        }

        public Member getHookedMethod() {
            return hookedMethod;
        }

        public void unhook() {
            if (handle != null) {
                handle.unhook();
            }
        }

        @Override
        public String toString() {
            return "Unhook{" + hookedMethod + '}';
        }
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    public final void callBeforeHookedMethod(MethodHookParam param) throws Throwable {
        beforeHookedMethod(param);
    }

    public final void callAfterHookedMethod(MethodHookParam param) throws Throwable {
        afterHookedMethod(param);
    }
}
