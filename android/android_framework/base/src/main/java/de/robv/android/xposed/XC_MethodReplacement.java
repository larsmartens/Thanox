package de.robv.android.xposed;

public abstract class XC_MethodReplacement extends XC_MethodHook {
    @Override
    protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {
        param.setResult(replaceHookedMethod(param));
    }

    @Override
    protected final void afterHookedMethod(MethodHookParam param) {
    }

    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;
}
